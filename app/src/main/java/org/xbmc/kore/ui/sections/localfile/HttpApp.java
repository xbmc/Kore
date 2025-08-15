package org.xbmc.kore.ui.sections.localfile;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import org.xbmc.kore.utils.LogUtils;

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

    private final Response forbidden = newFixedLengthResponse(Response.Status.FORBIDDEN, "", "");

    @Override
    public Response serve(IHTTPSession session) {

        Map<String, List<String>> params = session.getParameters();
        if (localFileLocationList == null) {
            return forbidden;
        }

        List<String> lstToken = params.get("token");
        if (lstToken == null ||
            lstToken.get(0) == null ||
            !lstToken.get(0).equals(this.token)) {
            return forbidden;
        }

        FileInputStream fis;
        String mimeType = null;
        try {
            if (params.containsKey("number")) {
                int file_number = Integer.parseInt(params.get("number").get(0));

                LocalFileLocation localFileLocation = localFileLocationList.get(file_number);
                fis = new FileInputStream(localFileLocation.fullPath);
                mimeType = localFileLocation.getMimeType();
            } else if (params.containsKey("uri")) {
                int uri_number = Integer.parseInt(params.get("uri").get(0));
                Uri uri = localUriList.get(uri_number);

                try {
                    // ensure that we can read the URI's content, even if the component
                    // that originally provided this permission has died
                    context.grantUriPermission(context.getPackageName(), uri,
                                               Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    LogUtils.LOGE(LogUtils.makeLogTag(HttpApp.class), e.toString());
                    return forbidden;
                }

                fis = (FileInputStream) context.getContentResolver().openInputStream(uri);
            } else {
                return forbidden;
            }
        } catch (FileNotFoundException e) {
            LogUtils.LOGW(LogUtils.makeLogTag(HttpApp.class), e.toString());
            return forbidden;
        }

        return newChunkedResponse(Response.Status.OK, mimeType, fis);
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

        // If the mimeType determined by Andoid is not equal to the one determined by
        // the filename, add an extra extension to make sure Kodi recognizes the file type:
        String mimeType = context.getContentResolver().getType(contentUri);
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extensionFromFilename = mimeTypeMap.getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(fileName));
        if (
                (extensionFromFilename == null) || (!extensionFromFilename.equals(mimeType))
        ) {
            fileName += "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
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
