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

package org.xbmc.kore.tests.jsonrpc.method;

import android.os.Build;
import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.testutils.tcpserver.MockTcpServer;
import org.xbmc.kore.testutils.tcpserver.handlers.ApplicationHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.JSONConnectionHandlerManager;
import org.xbmc.kore.utils.RoboThreadRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class ApplicationTest {

    private HostConnection hostConnection;
    private MockTcpServer server;
    private ApplicationHandler applicationHandler;
    private JSONConnectionHandlerManager manager;

    @Before
    public void setup() throws Exception {
        applicationHandler = new ApplicationHandler();

        manager = new JSONConnectionHandlerManager();
        manager.addHandler(applicationHandler);

        server = new MockTcpServer(manager);
        server.start();

        HostInfo hostInfo = new HostInfo("TESTHOST", server.getHostName(), HostConnection.PROTOCOL_TCP,
                                         HostInfo.DEFAULT_HTTP_PORT, server.getPort(), null, null, true,
                                         HostInfo.DEFAULT_EVENT_SERVER_PORT,
                                         false);

        hostConnection = new HostConnection(hostInfo);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        hostConnection.disconnect();
    }

    @Test
    public void getPropertiesTest() throws Exception {
        org.xbmc.kore.jsonrpc.method.Application.GetProperties properties =
                new org.xbmc.kore.jsonrpc.method.Application.GetProperties(org.xbmc.kore.jsonrpc.method.Application.GetProperties.MUTED);
        applicationHandler.setMuted(true, false);

        hostConnection.execute(properties, new ApiCallback<ApplicationType.PropertyValue>() {
            @Override
            public void onSuccess(ApplicationType.PropertyValue result) {
                assertNotNull(result);
                assertTrue(result.muted);
                RoboThreadRunner.stop();
            }

            @Override
            public void onError(int errorCode, String description) {
                fail("errorCode="+errorCode+", description="+description);
                RoboThreadRunner.stop();
            }
        }, new Handler());

        assertTrue(RoboThreadRunner.run(10));
    }

    @Test
    public void setMuteTrueTest() throws Exception {
        applicationHandler.setMuted(false, false);
        sendSetMute(true);
        assertTrue(RoboThreadRunner.run(10));
    }

    @Test
    public void setMuteFalseTest() throws Exception {
        applicationHandler.setMuted(true, false);
        sendSetMute(false);
        assertTrue(RoboThreadRunner.run(10));
    }

    @Test
    public void incrementVolumeTest() throws Exception {
        org.xbmc.kore.jsonrpc.method.Application.SetVolume setVolume =
                new org.xbmc.kore.jsonrpc.method.Application.SetVolume(GlobalType.IncrementDecrement.INCREMENT);
        applicationHandler.setVolume(77, false);
        hostConnection.execute(setVolume, new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                assertNotNull(result);
                assertTrue(result == 78);
                RoboThreadRunner.stop();
            }

            @Override
            public void onError(int errorCode, String description) {
                fail("errorCode="+errorCode+", description="+description);
                RoboThreadRunner.stop();
            }
        }, new Handler());

        assertTrue(RoboThreadRunner.run(10));
    }


    @Test
    public void decrementVolumeTest() throws Exception {
        org.xbmc.kore.jsonrpc.method.Application.SetVolume setVolume =
                new org.xbmc.kore.jsonrpc.method.Application.SetVolume(GlobalType.IncrementDecrement.DECREMENT);
        applicationHandler.setVolume(77, false);
        hostConnection.execute(setVolume, new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                assertNotNull(result);
                assertTrue(result == 76);
                RoboThreadRunner.stop();
            }

            @Override
            public void onError(int errorCode, String description) {
                fail("errorCode="+errorCode+", description="+description);
                RoboThreadRunner.stop();
            }
        }, new Handler());

        assertTrue(RoboThreadRunner.run(10));
    }

    @Test
    public void setVolumeTest() throws Exception {
        org.xbmc.kore.jsonrpc.method.Application.SetVolume setVolume =
                new org.xbmc.kore.jsonrpc.method.Application.SetVolume(83);
        applicationHandler.setVolume(77, false);
        hostConnection.execute(setVolume, new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                assertNotNull(result);
                assertTrue(result == 83);
                RoboThreadRunner.stop();
            }

            @Override
            public void onError(int errorCode, String description) {
                fail("errorCode="+errorCode+", description="+description);
                RoboThreadRunner.stop();
            }
        }, new Handler());

        assertTrue(RoboThreadRunner.run(10));
    }

    /**
     * Sends the SetMute method to toggle the mute state
     * @throws InterruptedException
     */
    private void sendSetMute(final boolean expectedMuteState) throws InterruptedException {
        org.xbmc.kore.jsonrpc.method.Application.SetMute mute =
                new org.xbmc.kore.jsonrpc.method.Application.SetMute();

        hostConnection.execute(mute, new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                assertTrue(result == expectedMuteState);
                RoboThreadRunner.stop();
            }

            @Override
            public void onError(int errorCode, String description) {
                fail(description);
                RoboThreadRunner.stop();
            }
        }, new Handler());

    }
}
