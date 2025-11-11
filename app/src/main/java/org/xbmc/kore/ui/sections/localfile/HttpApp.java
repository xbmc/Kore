package org.xbmc.kore.ui.sections.localfile;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;

import org.xbmc.kore.utils.LogUtils;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static android.content.Context.WIFI_SERVICE;


public class HttpApp extends NanoHTTPD {

    private HttpApp(Context context, int port) throws IOException {
        super(port);
        this.context = context;
        this.localFileLocationList = new LinkedList<>();
        this.localUriList = new LinkedList<>();
        this.token = generateToken();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    private String generateToken() {
        StringBuilder token = new StringBuilder();

        SecureRandom sr = new SecureRandom();

        int TOKEN_LENGTH = 12;
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int n = sr.nextInt(26*2 + 10);
            if (n < 26) {
                n += 'A';
            } else if (n < 26*2) {
                n += 'a' - 26;
            } else {
                n += '0' - 26*2;
            }
            token.append((char) n);
        }

        return token.toString();
    }

    private final Context context;
    private final LinkedList<LocalFileLocation> localFileLocationList;
    private final LinkedList<Uri> localUriList;
    private int currentIndex;
    private boolean currentIsFile;
    private final String token;
    private record Range(long start, long end) { }

    private final Response forbidden = newFixedLengthResponse(Response.Status.FORBIDDEN, "", "");

    @Override
    public Response serve(IHTTPSession session) {

        Map<String, List<String>> params = session.getParameters();
        Map<String, String> headers = session.getHeaders();

        if (localFileLocationList == null) {
            return forbidden;
        }

        List<String> lstToken = params.get("token");
        if (lstToken == null ||
            lstToken.get(0) == null ||
            !lstToken.get(0).equals(this.token)) {
            return forbidden;
        }

        try {
            if (params.containsKey("number")) {
                return handleFileContent(params.get("number"), headers.get("range"));
            } else if (params.containsKey("uri")) {
                return handleUriContent(params.get("uri"), headers.get("range"));
            } else {
                return forbidden;
            }
        } catch (FileNotFoundException e) {
            LogUtils.LOGW(LogUtils.makeLogTag(HttpApp.class), e.toString());
            return forbidden;
        } catch (IOException e) {
            LogUtils.LOGW(LogUtils.makeLogTag(HttpApp.class), e.toString());
            return forbidden;
        }
    }

    private Response handleFileContent(List<String> param, String rangeHeader) throws FileNotFoundException, IOException {
        int fileNumber = Integer.parseInt(param.get(0));
        LocalFileLocation localFileLocation = localFileLocationList.get(fileNumber);
        File file = new File(localFileLocation.fullPath);

        if (!file.exists() || !file.isFile()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
        }

        String mimeType = localFileLocation.getMimeType();
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return createRangeResponse(pfd, mimeType, rangeHeader);
    }

    private Response handleUriContent(List<String> param, String rangeHeader) throws FileNotFoundException {
        int uri_number = Integer.parseInt(param.get(0));
        Uri uri = localUriList.get(uri_number);
    
        try {
            context.grantUriPermission(context.getPackageName(), uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            LogUtils.LOGE(LogUtils.makeLogTag(HttpApp.class), e.toString());
            return forbidden;
        }
    
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) { 
                throw new IOException("Unable to open ParcelFileDescriptor");
            }
            return createRangeResponse(pfd, "application/octet-stream", rangeHeader);
        } catch (Exception e) {
            LogUtils.LOGW(LogUtils.makeLogTag(HttpApp.class), "Range request failed, using full stream: " + e.getMessage());
            InputStream fallbackStream = context.getContentResolver().openInputStream(uri);
            return newChunkedResponse(Response.Status.OK, "application/octet-stream", fallbackStream);
        }
    }

    private Response createRangeResponse(ParcelFileDescriptor pfd, String mimeType, String rangeHeader) throws IOException {
        AutoCloseInputStream fis = null;
        try {
            long fileSize = pfd.getStatSize();
            Range range = parseRangeHeader(rangeHeader, fileSize);
            if (range == null) {
                pfd.close();
                Response res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                res.addHeader("Content-Range", "bytes */" + fileSize);
                return res;
            }

            fis = new AutoCloseInputStream(pfd);
            fis.getChannel().position(range.start());
            long contentLength = range.end() - range.start() + 1;

            Response response = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, fis, contentLength);
            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("Content-Range", "bytes " + range.start() + "-" + range.end() + "/" + fileSize);
            return response;
        } catch (IOException e) {
            if (fis == null) {
                pfd.close();
            }
            throw e;
        }
    }

    private Range parseRangeHeader(String rangeHeader, long fileSize) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return new Range(0, fileSize - 1);
        }

        try {
            String range = rangeHeader.substring("bytes=".length()).trim();
            long start = 0;
            long end = fileSize - 1;
        
            if (range.startsWith("-")) {
                long suffix = Long.parseLong(range.substring(1));
                start = Math.max(0, fileSize - suffix);
            } else {
                String[] parts = range.split("-", 2);
                start = Long.parseLong(parts[0]);
                if(parts.length > 1 && !parts[1].isEmpty()) {
                end = Math.min(Long.parseLong(parts[1]), fileSize - 1);
                }
            }

            if (start < 0 || start >= fileSize || start > end) {
                return null;
            }

            return new Range(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    public void addLocalFilePath(LocalFileLocation localFileLocation) {
        if (localFileLocationList.contains(localFileLocation)) {
            // Path already exists, get its index:
            currentIndex = localFileLocationList.indexOf(localFileLocation);
        } else {
            this.localFileLocationList.add(localFileLocation);
            currentIndex = localFileLocationList.size() - 1;
        }
        currentIsFile = true;
    }

    public void addUri(Uri uri) {
        if (localUriList.contains(uri)) {
            currentIndex = localUriList.indexOf(uri);
        } else {
            this.localUriList.add(uri);
            currentIndex = localUriList.size() - 1;
        }
        currentIsFile = false;
    }

    private String getIpAddress() throws UnknownHostException {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        byte[] byte_address = BigInteger.valueOf(wm.getConnectionInfo().getIpAddress()).toByteArray();
        // Reverse `byte_address`:
        for (int i = 0; i < byte_address.length/2; i++) {
            byte temp = byte_address[i];
            int j = byte_address.length - i - 1;
            if (j < 0)
                break;
            byte_address[i] = byte_address[j];
            byte_address[j] = temp;
        }
        InetAddress inet_address = InetAddress.getByAddress(byte_address);
        return inet_address.getHostAddress();
    }

    public String getLinkToFile() {
        String ip;
        try {
            ip = getIpAddress();
        } catch (UnknownHostException uhe) {
            return null;
        }
        try {
            if (!isAlive())
                start();
        } catch (IOException ioe) {
            LogUtils.LOGE(LogUtils.makeLogTag(HttpApp.class), ioe.getMessage());
        }
        String path;
        if (currentIsFile) {
            String filename = localFileLocationList.get(currentIndex).fileName;
            path = Uri.encode(filename) + "?number=" + currentIndex;
        } else {
            String filename = getFileNameFromUri(localUriList.get(currentIndex));
            path = Uri.encode(filename) + "?uri=" + currentIndex;
        }
        return "http://" + ip + ":" + getListeningPort() + "/" + path + "&token=" + token;
    }

    private String getFileNameFromUri(Uri contentUri) {
        String fileName = "";
        // Let's parse the Uri to detect the filename:
        if (contentUri.toString().startsWith("content://")) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(contentUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    // Unrolled to prevent error on lint
                    int colIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (colIdx >= 0)
                        fileName = cursor.getString(colIdx);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        return fileName;
    }

    private static HttpApp http_app = null;

    public static HttpApp getInstance(Context context, int port) throws IOException {
        if (http_app == null) {
            synchronized (HttpApp.class) {
                if (http_app == null) {
                    http_app = new HttpApp(context, port);
                }
            }
        }
        return http_app;
    }

}
