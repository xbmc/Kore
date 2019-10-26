package org.xbmc.kore.ui.sections.localfile;


import android.app.Activity;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static android.content.Context.WIFI_SERVICE;


public class HttpApp extends NanoHTTPD {

    public HttpApp(Activity activity, int port) throws IOException {
        super(port);
        this.port = port;
        this.activity = activity;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    static int port;
    static Activity activity;

    String fileName = null;
    String filePath = null;
    String mimeType = null;

    @Override
    public Response serve(IHTTPSession session) {

        Map<String, List<String>> parms = session.getParameters();
        if (filePath == null) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "", "");
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "", "");
        }
        String mimeType = this.mimeType;
        return newChunkedResponse(Response.Status.OK, mimeType, fis);
    }

    public void addLocalFilePath(String filename, String filePath, String mimeType) {
        this.fileName = filename;
        this.filePath = filePath;
        this.mimeType = mimeType;
    }

    private String getIpAddress() throws UnknownHostException {
        WifiManager wm = (WifiManager) activity.getApplicationContext().getSystemService(WIFI_SERVICE);
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
        String ip = inet_address.getHostAddress();
        return ip;
    }

    public String getLinkToFile() {
        String ip = null;
        try {
            ip = getIpAddress();
        } catch (UnknownHostException uhe) {
            return null;
        }
        try {
            if (!isAlive())
                start();
        } catch (IOException ioe) {
            Log.e(ioe.getClass().toString(), ioe.getMessage());
        }
        return "http://" + ip + ":" + String.valueOf(port) + "/" + fileName;
    }

    private static HttpApp http_app = null;

    public static HttpApp getInstance() throws IOException {
        if (http_app == null) {
            http_app = new HttpApp(activity, port);
        }
        return http_app;
    }

    public static void setActivity(Activity activity) {
        HttpApp.activity = activity;
    }

    public static void setPort(int port) {
        HttpApp.port = port;
    }

}
