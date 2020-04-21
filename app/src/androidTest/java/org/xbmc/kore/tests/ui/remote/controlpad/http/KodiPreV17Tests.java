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

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.testhelpers.TestUtils;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class KodiPreV17Tests extends AbstractTestClass<RemoteActivity> {
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
    public void setUp() throws Throwable {
        setKodiMajorVersion(HostInfo.KODI_V16_JARVIS);
        super.setUp();
    }

    @Test
    public void infoControlPadButtonLongClickTest() {
        onView(withId(R.id.info)).perform(longClick());

        TestUtils.testHTTPEvent(Input.ExecuteAction.METHOD_NAME, Input.ExecuteAction.CODECINFO);
    }
}
