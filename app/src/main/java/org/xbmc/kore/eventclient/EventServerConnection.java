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
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
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
    private InetAddress hostInetAddress = null;

    // Handler on which packets will be posted, to send them asynchronously
    private Handler commHandler = null;
    private HandlerThread handlerThread = null;

    private PacketPING packetPING = new PacketPING();
    private Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.LOGD(TAG, "Pinging EventServer");
            if (hostInetAddress != null) {
                try {
                    packetPING.send(hostInetAddress, hostInfo.getEventServerPort());
                } catch (IOException exc) {
                    LogUtils.LOGD(TAG, "Got an IOException when sending a PING Packet to Kodi's EventServer");
                }
            }
            commHandler.postDelayed(this, PING_INTERVAL);
        }
    };

    /**
     * Interface to notify users if the connection was successful
     */
    public interface EventServerConnectionCallback {
        void OnConnectResult(boolean success);
    }

    /**
     * Constructor. Starts the thread that keeps the connection alive. Make sure to call quit() when done.
     * @param hostInfo Host to connect to
     */
    public EventServerConnection(final HostInfo hostInfo, final EventServerConnectionCallback callback) {
        this.hostInfo = hostInfo;

        LogUtils.LOGD(TAG, "Starting EventServer Thread");
        // Handler thread that will keep pinging and send the requests to Kodi
        handlerThread = new HandlerThread("EventServerConnection", Process.THREAD_PRIORITY_DEFAULT);
        handlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        commHandler = new Handler(handlerThread.getLooper());

        // Now, get the host InetAddress in the background
        commHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    hostInetAddress = InetAddress.getByName(hostInfo.getAddress());
                } catch (UnknownHostException exc) {
                    LogUtils.LOGD(TAG, "Got an UnknownHostException, disabling EventServer");
                    hostInetAddress = null;
                }
                callback.OnConnectResult(hostInetAddress != null);
            }
        });

        // Start pinging
        commHandler.postDelayed(pingRunnable, PING_INTERVAL);
    }


    /**
     * Stops the HandlerThread that is being used to send packets to Kodi
     */
    public void quit() {
        LogUtils.LOGD(TAG, "Quiting EventServer handler thread");
        quitHandlerThread(handlerThread);
    }

    /**
     * Sends a packet to Kodi's Event Server
     * Only sends the packet if connected, i.e. if quit() has not been not called
     * @param p Packet to send
     */
    public void sendPacket(final Packet p) {
        if (!handlerThread.isAlive() || (hostInetAddress == null)) {
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

    /**
     * Establishes a connection to the EventServer and reports the result
     * @param hostInfo Host to connect to
     * @param callerCallback Callback on which to post the result
     * @param callerHandler Handler on which to post the callback call
     */
    public static void testEventServerConnection(final HostInfo hostInfo,
                                                 final EventServerConnectionCallback callerCallback,
                                                 final Handler callerHandler) {
        final HandlerThread auxThread = new HandlerThread("EventServerConnectionTest", Process.THREAD_PRIORITY_DEFAULT);
        auxThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        final Handler auxHandler = new Handler(auxThread.getLooper());

        auxHandler.post(new Runnable() {
            @Override
            public void run() {
                // Get the InetAddress
                final InetAddress hostInetAddress;
                try {
                    hostInetAddress = InetAddress.getByName(hostInfo.getAddress());
                } catch (UnknownHostException exc) {
                    LogUtils.LOGD(TAG, "Couldn't get host InetAddress");
                    reportTestResult(callerHandler, callerCallback, false);
                    quitHandlerThread(auxThread);
                    return;
                }

                // Send a HELO packet
                Packet p = new PacketHELO(DEVICE_NAME);
                try {
                    p.send(hostInetAddress, hostInfo.getEventServerPort());
                } catch (IOException exc) {
                    LogUtils.LOGD(TAG, "Couldn't send HELO packet to host");
                    reportTestResult(callerHandler, callerCallback, false);
                    quitHandlerThread(auxThread);
                    return;
                }

                // The previous checks don't really test the connection, as this is UDP. Apart from checking if
                // any HostUnreachable ICMP message is returned (which may or may not happen), there's no direct way
                // to check if the messages were delivered, so the solution is to force something to happen on
                // Kodi and them read Kodi's state to check if it was applied.
                // We are going to get the mute status of Kodi via jsonrpc, change it via EventServer and check if
                // it was changed via jsonrpc, reverting it back afterwards
                final HostConnection auxHostConnection = new HostConnection(
                        new HostInfo(hostInfo.getName(), hostInfo.getAddress(),
                                     HostConnection.PROTOCOL_HTTP, hostInfo.getHttpPort(), hostInfo.getTcpPort(),
                                     hostInfo.getUsername(), hostInfo.getPassword(), false, 0));
                final Application.GetProperties action = new Application.GetProperties(Application.GetProperties.MUTED);
                final Packet mutePacket = new PacketBUTTON(ButtonCodes.MAP_REMOTE, ButtonCodes.REMOTE_MUTE,
                                                           false, true, true, (short) 0, (byte) 0);

                // Get the initial mute status
                action.execute(auxHostConnection, new ApiCallback<ApplicationType.PropertyValue>() {
                    @Override
                    public void onSuccess(ApplicationType.PropertyValue result) {
                        final boolean initialMuteStatus = result.muted;
                        // Switch mute status
                        try {
                            mutePacket.send(hostInetAddress, hostInfo.getEventServerPort());
                        } catch (IOException exc) {
                            LogUtils.LOGD(TAG, "Couldn't send first MUTE packet to host");
                            reportTestResult(callerHandler, callerCallback, false);
                            quitHandlerThread(auxThread);
                            return;
                        }

                        // Sleep a while to make sure the previous command was executed
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException exc) {
                            // Ignore
                        }

                        // Now get the new status and compare
                        action.execute(auxHostConnection, new ApiCallback<ApplicationType.PropertyValue>() {
                            @Override
                            public void onSuccess(ApplicationType.PropertyValue result) {
                                // Report result (mute status is different)
                                reportTestResult(callerHandler, callerCallback, initialMuteStatus != result.muted);

                                // Revert mute status
                                try {
                                    mutePacket.send(hostInetAddress, hostInfo.getEventServerPort());
                                } catch (IOException exc) {
                                    LogUtils.LOGD(TAG, "Couldn't revert MUTE status");
                                }
                                quitHandlerThread(auxThread);
                            }

                            @Override
                            public void onError(int errorCode, String description) {
                                LogUtils.LOGD(TAG, "Got an error on Application.GetProperties: " + description);
                                reportTestResult(callerHandler, callerCallback, false);
                                quitHandlerThread(auxThread);
                            }
                        }, auxHandler);
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        LogUtils.LOGD(TAG, "Got an error on Application.GetProperties: " + description);
                        reportTestResult(callerHandler, callerCallback, false);
                        quitHandlerThread(auxThread);
                    }
                }, auxHandler);
            }
        });

    }

    private static void reportTestResult(final Handler callerHandler,
                                         final EventServerConnectionCallback callback,
                                         final boolean result) {
        callerHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.OnConnectResult(result);
            }
        });
    }

    @SuppressLint("NewApi")
    private static void quitHandlerThread(HandlerThread handlerThread) {
        if (Utils.isJellybeanMR2OrLater()) {
            handlerThread.quitSafely();
        } else {
            handlerThread.quit();
        }
    }
}
