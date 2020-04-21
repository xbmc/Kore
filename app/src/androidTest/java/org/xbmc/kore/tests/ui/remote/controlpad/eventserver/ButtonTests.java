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

package org.xbmc.kore.tests.ui.remote.controlpad.eventserver;

import android.content.Context;

import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.eventclient.ButtonCodes;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.testhelpers.TestUtils;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.testutils.eventserver.EventPacket;
import org.xbmc.kore.testutils.eventserver.EventPacketBUTTON;
import org.xbmc.kore.testutils.eventserver.MockEventServer;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertTrue;

public class ButtonTests extends AbstractTestClass<RemoteActivity> {
    private static MockEventServer mockEventServer;

    @Rule
    public ActivityTestRule<RemoteActivity> remoteActivityActivityTestRule =
            new ActivityTestRule<>(RemoteActivity.class);

    @Override
    protected ActivityTestRule<RemoteActivity> getActivityTestRule() {
        return remoteActivityActivityTestRule;
    }

    @Override
    protected void setSharedPreferences(Context context) {
        Utils.setUseEventServerPreference(context, true);
    }

    @BeforeClass
    public static void setupEventServer() {
        mockEventServer = new MockEventServer();
        mockEventServer.setListenPort(HostInfo.DEFAULT_EVENT_SERVER_PORT);
        mockEventServer.start();
    }

    @Override
    public void setUp() throws Throwable {
        setKodiMajorVersion(HostInfo.KODI_V17_KRYPTON);
        super.setUp();
    }

    @After
    public void resetState() {
        mockEventServer.reset();
    }

    @AfterClass
    public static void cleanup() {
        mockEventServer.shutdown();
    }

    @Test
    public void leftControlPadButtonTest() {
        onView(withId(R.id.left)).perform(click());

        testRemoteButton(ButtonCodes.REMOTE_LEFT);
    }

    @Test
    public void rightControlPadButtonTest() {
        onView(withId(R.id.right)).perform(click());

        testRemoteButton(ButtonCodes.REMOTE_RIGHT);
    }

    @Test
    public void upControlPadButtonTest() {
        onView(withId(R.id.up)).perform(click());

        testRemoteButton(ButtonCodes.REMOTE_UP);
    }

    @Test
    public void downControlPadButtonTest() {
        onView(withId(R.id.down)).perform(click());

        testRemoteButton(ButtonCodes.REMOTE_DOWN);
    }

    @Test
    public void selectPadButtonTest() {
        onView(withId(R.id.select)).perform(click());

        testRemoteButton(ButtonCodes.REMOTE_SELECT);
    }

    //The following tests do not use the event server. They're included here
    //to make sure they still work when the event server is enabled.
    @Test
    public void contextControlPadButtonTest() {
        onView(withId(R.id.context)).perform(click());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.CONTEXTMENU);
    }

    @Test
    public void infoControlPadButtonTest() {
        HostManager.getInstance(getActivity()).getHostInfo().setKodiVersionMajor(17);

        onView(withId(R.id.info)).perform(click());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.INFO);
    }

    @Test
    public void infoControlPadButtonLongClickTest() {
        onView(withId(R.id.info)).perform(longClick());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.PLAYERPROCESSINFO);
    }

    @Test
    public void osdControlPadButtonTest() {
        onView(withId(R.id.osd)).perform(click());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.OSD);
    }

    @Test
    public void backControlPadButtonTest() {
        onView(withId(R.id.back)).perform(click());

        TestUtils.testHTTPEvent(Input.Back.METHOD_NAME, null);
    }

    private void testRemoteButton(String buttonName) {
        EventPacket packet = mockEventServer.getEventPacket();
        assertTrue(packet != null);
        assertTrue(packet.getPacketType() == EventPacket.PT_BUTTON);
        assertTrue(((EventPacketBUTTON) packet).getButtonName().contentEquals(buttonName));
        assertTrue(((EventPacketBUTTON) packet).getMapName().contentEquals(ButtonCodes.MAP_REMOTE));
    }
}
