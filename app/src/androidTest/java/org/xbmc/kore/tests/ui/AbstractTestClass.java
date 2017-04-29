/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.tests.ui;

import android.content.Intent;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.app.AppCompatActivity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.testhelpers.LoaderIdlingResource;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.testutils.Database;
import org.xbmc.kore.testutils.tcpserver.MockTcpServer;
import org.xbmc.kore.testutils.tcpserver.handlers.AddonsHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.JSONConnectionHandlerManager;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@Ignore
abstract public class AbstractTestClass<T extends AppCompatActivity> {

    abstract protected ActivityTestRule<T> getActivityTestRule();

    private LoaderIdlingResource loaderIdlingResource;
    private ActivityTestRule<T> activityTestRule;
    private static MockTcpServer server;
    private static JSONConnectionHandlerManager manager;
    private AddonsHandler addonsHandler;

    @BeforeClass
    public static void setupMockTCPServer() throws Throwable {
        manager = new JSONConnectionHandlerManager();
        server = new MockTcpServer(manager);
        server.start();
    }

    @Before
    public void setUp() throws Throwable {

        activityTestRule = getActivityTestRule();

        //Note: as the activity is not yet available in @BeforeClass we need
        //      to add the handler here
        if (addonsHandler == null) {
            addonsHandler = new AddonsHandler(activityTestRule.getActivity());
            manager.addHandler(addonsHandler);
        }

        HostInfo hostInfo = Database.addHost(activityTestRule.getActivity(), server.getHostName(),
                                                 HostConnection.PROTOCOL_TCP, HostInfo.DEFAULT_HTTP_PORT,
                                                 server.getPort());

        Utils.initialize(activityTestRule, hostInfo);

        loaderIdlingResource = new LoaderIdlingResource(activityTestRule.getActivity().getSupportLoaderManager());
        Espresso.registerIdlingResources(loaderIdlingResource);

        activityTestRule.launchActivity(new Intent());

        Utils.closeDrawer(activityTestRule);
    }

    @After
    public void tearDown() throws Exception {
        if ( loaderIdlingResource != null )
            Espresso.unregisterIdlingResources(loaderIdlingResource);

        Utils.cleanup();
    }

    @AfterClass
    public static void cleanup() throws IOException {
        server.shutdown();
    }

    protected T getActivity() {
        if (activityTestRule != null) {
            return activityTestRule.getActivity();
        }
        return null;
    }
}
