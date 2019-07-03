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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;

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
import org.xbmc.kore.testutils.tcpserver.handlers.ApplicationHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.InputHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.JSONConnectionHandlerManager;
import org.xbmc.kore.testutils.tcpserver.handlers.JSONRPCHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.PlaylistHandler;
import org.xbmc.kore.ui.sections.hosts.HostFragmentManualConfiguration;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@Ignore
abstract public class AbstractTestClass<T extends AppCompatActivity> {
    private static final String TAG = LogUtils.makeLogTag(AbstractTestClass.class);

    abstract protected ActivityTestRule<T> getActivityTestRule();

    /**
     * Method that can be used to change the shared preferences.
     * This will be called before each test after clearing the settings
     * in {@link #setUp()}
     */
    abstract protected void setSharedPreferences(Context context);

    private LoaderIdlingResource loaderIdlingResource;
    private ActivityTestRule<T> activityTestRule;
    private static MockTcpServer server;
    private static JSONConnectionHandlerManager manager;
    private static PlayerHandler playerHandler;
    private static ApplicationHandler applicationHandler;
    private static InputHandler inputHandler;
    private static PlaylistHandler playlistHandler;
    private int kodiMajorVersion = HostInfo.DEFAULT_KODI_VERSION_MAJOR;
    private HostInfo hostInfo;

    @BeforeClass
    public static void setupMockTCPServer() throws Throwable {
        playerHandler = new PlayerHandler();
        applicationHandler = new ApplicationHandler();
        inputHandler = new InputHandler();
        playlistHandler = new PlaylistHandler();
        manager = new JSONConnectionHandlerManager();
        manager.addHandler(playerHandler);
        manager.addHandler(applicationHandler);
        manager.addHandler(inputHandler);
        manager.addHandler(new AddonsHandler());
        manager.addHandler(playlistHandler);
        manager.addHandler(new JSONRPCHandler());
        server = new MockTcpServer(manager);
        server.start();
    }

    @Before
    public void setUp() throws Throwable {

        activityTestRule = getActivityTestRule();

        final Context context = activityTestRule.getActivity();
        if (context == null)
            throw new RuntimeException("Could not get context. Maybe activity failed to start?");

        Utils.clearSharedPreferences(context);
        //Prevent drawer from opening when we start a new activity
        Utils.setLearnedAboutDrawerPreference(context, true);
        //Allow each test to change the shared preferences
        setSharedPreferences(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useEventServer = prefs.getBoolean(HostFragmentManualConfiguration.HOST_USE_EVENT_SERVER, false);

        hostInfo = Database.addHost(context, server.getHostName(),
                                    HostConnection.PROTOCOL_TCP, HostInfo.DEFAULT_HTTP_PORT,
                                    server.getPort(), useEventServer, kodiMajorVersion);

        loaderIdlingResource = new LoaderIdlingResource(activityTestRule.getActivity().getSupportLoaderManager());
        IdlingRegistry.getInstance().register(loaderIdlingResource);

        Utils.disableAnimations(context);

        Utils.setupMediaProvider(context);

        Database.fill(hostInfo, context, context.getContentResolver());

        Utils.switchHost(context, activityTestRule.getActivity(), hostInfo);

        //Relaunch the activity for the changes (Host selection, preference changes, and database fill) to take effect
        activityTestRule.finishActivity();
        activityTestRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() throws Exception {
        if ( loaderIdlingResource != null )
            IdlingRegistry.getInstance().unregister(loaderIdlingResource);

        applicationHandler.reset();
        playerHandler.reset();

        Context context = activityTestRule.getActivity();
        Database.flush(context.getContentResolver());
        Utils.enableAnimations(context);
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

    /**
     * Use this to set the major version of Kodi.
     * <br/>
     * NOTE: be sure to call this before {@link #setUp()} is called to have the version correctly
     * set in the database.
     * @param kodiMajorVersion
     */
    protected void setKodiMajorVersion(int kodiMajorVersion) {
        this.kodiMajorVersion = kodiMajorVersion;
    }

    public static JSONConnectionHandlerManager getConnectionHandlerManager() {
        return manager;
    }

    public static PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public static ApplicationHandler getApplicationHandler() {
        return applicationHandler;
    }

    public static InputHandler getInputHandler() {
        return inputHandler;
    }

    public static PlaylistHandler getPlaylistHandler() {
        return playlistHandler;
    }
}
