/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.tests.jsonrpc.notifications;

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.notification.Application;
import org.xbmc.kore.testutils.tcpserver.MockTcpServer;
import org.xbmc.kore.testutils.tcpserver.handlers.ApplicationHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.JSONConnectionHandlerManager;
import org.xbmc.kore.utils.RoboThreadRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class ApplicationTest {

    private HostConnection hostConnection;
    private MockTcpServer server;
    private ApplicationHandler applicationHandler;

    @Before
    public void setup() throws Exception {
        ShadowLog.stream = System.out;

        applicationHandler = new ApplicationHandler();

        JSONConnectionHandlerManager manager = new JSONConnectionHandlerManager();
        manager.addHandler(applicationHandler);

        server = new MockTcpServer(manager);
        server.start();

        HostInfo hostInfo = new HostInfo("TESTHOST", server.getHostName(), HostConnection.PROTOCOL_TCP,
                                         HostInfo.DEFAULT_HTTP_PORT, server.getPort(), null, null, true,
                                         HostInfo.DEFAULT_EVENT_SERVER_PORT, false, false);

        hostConnection = new HostConnection(hostInfo);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        hostConnection.disconnect();
    }

    @Test
    public void onVolumeChanged() throws InterruptedException {
        HostConnection.ApplicationNotificationsObserver observer =
                new HostConnection.ApplicationNotificationsObserver() {
                    @Override
                    public void onVolumeChanged(Application.OnVolumeChanged notification) {
                        RoboThreadRunner.stop();
                        assertTrue(notification.volume == 84);
                    }

                };

        hostConnection.registerApplicationNotificationsObserver(observer, new Handler());

        applicationHandler.setVolume(82, false);
        sendSetVolumeCommand(84);

        assertTrue(RoboThreadRunner.run(10));
    }

    @Test
    public void onVolumeChangedMuted() throws InterruptedException {
        HostConnection.ApplicationNotificationsObserver observer =
                new HostConnection.ApplicationNotificationsObserver() {
                    @Override
                    public void onVolumeChanged(Application.OnVolumeChanged notification) {
                        RoboThreadRunner.stop();
                        assertTrue(notification.muted);
                    }

                };

        hostConnection.registerApplicationNotificationsObserver(observer, new Handler());

        applicationHandler.setMuted(false, false);
        sendToggleMuteCommand();

        assertTrue(RoboThreadRunner.run(10));
    }

    @Test
    public void onVolumeChangedNotMuted() throws InterruptedException {
        HostConnection.ApplicationNotificationsObserver observer =
                new HostConnection.ApplicationNotificationsObserver() {
                    @Override
                    public void onVolumeChanged(Application.OnVolumeChanged notification) {
                        RoboThreadRunner.stop();
                        assertFalse(notification.muted);
                    }

                };

        hostConnection.registerApplicationNotificationsObserver(observer, new Handler());

        applicationHandler.setMuted(true, false);
        sendToggleMuteCommand();

        assertTrue(RoboThreadRunner.run(10));
    }


    private void sendToggleMuteCommand() {
        org.xbmc.kore.jsonrpc.method.Application.SetMute mute =
                new org.xbmc.kore.jsonrpc.method.Application.SetMute();

        mute.execute(hostConnection, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
            }

            @Override
            public void onError(int errorCode, String description) {
                RoboThreadRunner.stop();
                fail("errorCode="+errorCode+", description="+description);
            }
        }, new Handler());
    }

    private void sendSetVolumeCommand(int volume) {
        org.xbmc.kore.jsonrpc.method.Application.SetVolume setVolume =
                new org.xbmc.kore.jsonrpc.method.Application.SetVolume(volume);

        setVolume.execute(hostConnection, new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
            }

            @Override
            public void onError(int errorCode, String description) {
                RoboThreadRunner.stop();
                fail("errorCode="+errorCode+", description="+description);
            }
        }, new Handler());
    }
}
