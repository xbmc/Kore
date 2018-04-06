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

package org.xbmc.kore.tests.ui.remote.controlpad.http;

import android.content.Context;
import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.testhelpers.TestUtils;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ButtonTests extends AbstractTestClass<RemoteActivity> {
    @Rule
    public ActivityTestRule<RemoteActivity> remoteActivityActivityTestRule =
            new ActivityTestRule<>(RemoteActivity.class);

    @Override
    protected ActivityTestRule<RemoteActivity> getActivityTestRule() {
        return remoteActivityActivityTestRule;
    }

    @Override
    protected void setSharedPreferences(Context context) {
        Utils.setUseEventServerPreference(context, false);
    }

    @Override
    protected void configureHostInfo(HostInfo hostInfo) {
    }

    @Override
    public void setUp() throws Throwable {
        setKodiMajorVersion(HostInfo.KODI_V17_KRYPTON);
        super.setUp();
    }

    @Test
    public void leftControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.left)).perform(click());

        TestUtils.testHTTPEvent(Input.Left.METHOD_NAME, null);
    }

    @Test
    public void rightControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.right)).perform(click());

        TestUtils.testHTTPEvent(Input.Right.METHOD_NAME, null);
    }

    @Test
    public void upControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.up)).perform(click());

        TestUtils.testHTTPEvent(Input.Up.METHOD_NAME, null);
    }

    @Test
    public void downControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.down)).perform(click());

        TestUtils.testHTTPEvent(Input.Down.METHOD_NAME, null);
    }

    @Test
    public void selectPadButtonTest() throws InterruptedException {
        onView(withId(R.id.select)).perform(click());

        TestUtils.testHTTPEvent(Input.Select.METHOD_NAME, null);
    }

    @Test
    public void contextControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.context)).perform(click());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.CONTEXTMENU);
    }

    @Test
    public void infoControlPadButtonTest() throws InterruptedException {
        HostManager.getInstance(getActivity()).getHostInfo().setKodiVersionMajor(17);

        onView(withId(R.id.info)).perform(click());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.INFO);
    }

    @Test
    public void infoControlPadButtonLongClickTest() throws InterruptedException {
        onView(withId(R.id.info)).perform(longClick());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.PLAYERPROCESSINFO);
    }

    @Test
    public void osdControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.osd)).perform(click());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.OSD);
    }

    @Test
    public void backControlPadButtonTest() throws InterruptedException {
        onView(withId(R.id.back)).perform(click());

        TestUtils.testHTTPEvent(Input.Back.METHOD_NAME, null);
    }
}
