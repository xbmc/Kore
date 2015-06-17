/*
 * Copyright 2015 Synced Synapse. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.eventclient;


import android.annotation.SuppressLint;
import android.os.*;
import android.os.Process;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class that establishes and maintains a connection to Kodi's EventServer
 * This class keeps pinging Kodi to keep the connection alive and contains
 * auxiliary methods that allow the sending of packets to Kodi.
 * Make sure to call quit() when done with it, so that it gracefully shuts down
 */
public class EventServerConnection {
    private static final String TAG = LogUtils.makeLogTag(EventServerConnection.class);

    private static final int PING_INTERVAL = 45000; // ms
    private static final String DEVICE_NAME = "Kore Remote";

    /**
     * Host to connect too
     */
    private final HostInfo hostInfo;
    private final InetAddress hostInetAddress;
    private boolean isConnected = false;

    // Handler on which packets will be posted, to send them asynchronously
    private Handler commHandler = null;
    private HandlerThread handlerThread = null;

    private PacketPING packetPING = new PacketPING();
    private Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.LOGD(TAG, "Pinging EventServer");
            try {
                packetPING.send(hostInetAddress, hostInfo.getEventServerPort());
            } catch (IOException exc) {
                LogUtils.LOGD(TAG, "Got an IOException when sending a PING Packet to Kodi's EventServer");
            }
            commHandler.postDelayed(this, PING_INTERVAL);
        }
    };

    /**
     * Constructor. Starts the thread that keeps the connection alive. Make sure to call quit() when done.
     * @param hostInfo Host to connect to
     * @throws UnknownHostException
     */
    public EventServerConnection(final HostInfo hostInfo) throws UnknownHostException{
        this.hostInfo = hostInfo;
        hostInetAddress = InetAddress.getByName(hostInfo.getAddress());

        startEventClient();
    }

    /**
     * Creates the HandlerThread that will be used to post packets, establishes a connection with EventServer
     * (sends HELO packet) and starts the ping thread
     */
    private void startEventClient() {
        LogUtils.LOGD(TAG, "Starting EventServer Thread");
        // Handler thread that will keep pinging and send the requests to Kodi
        handlerThread = new HandlerThread("EventServerConnection", Process.THREAD_PRIORITY_DEFAULT);
        handlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        commHandler = new Handler(handlerThread.getLooper());

        // Send Hello Packet
//        commHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                PacketHELO p;
//                p = new PacketHELO(DEVICE_NAME);
//                try {
//                    p.send(hostInetAddress, hostInfo.getEventServerPort());
//                } catch (IOException exc) {
//                    // We are ignoring this one... Not sure if a good idea, but we're not on the UI thread
//                    LogUtils.LOGD(TAG, "Got an IOException when sending a HELO Packet to Kodi's EventServer");
//                }
//
//                // Start pinging
//                commHandler.postDelayed(pingRunnable, PING_INTERVAL);
//            }
//        });
        // Start pinging
        commHandler.postDelayed(pingRunnable, PING_INTERVAL);

        isConnected = true;
    }

    /**
     * Stops the HandlerThread that is being used to send packets to Kodi
     */
    @SuppressLint("NewApi")
    public void quit() {
        LogUtils.LOGD(TAG, "Quiting EventServer handler thread");
        if (Utils.isJellybeanMR2OrLater()) {
            handlerThread.quitSafely();
        } else {
            handlerThread.quit();
        }
        isConnected = false;
    }

    /**
     * Sends a packet to Kodi's Event Server
     * Only sends the packet if connected, i.e. if quit() has not been not called
     * @param p Packet to send
     */
    public void sendPacket(final Packet p) {
        if (!isConnected) {
            return;
        }

        LogUtils.LOGD(TAG, "Sending Packet");

        commHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    p.send(hostInetAddress, hostInfo.getEventServerPort());
                } catch (IOException exc) {
                    LogUtils.LOGD(TAG, "Got an IOException when sending a packet to Kodi's EventServer");
                }
            }
        });
    }
}
