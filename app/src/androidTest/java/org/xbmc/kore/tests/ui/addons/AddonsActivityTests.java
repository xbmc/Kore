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

package org.xbmc.kore.tests.ui.addons;

import android.support.test.rule.ActivityTestRule;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.tests.ui.BaseMediaActivityTests;
import org.xbmc.kore.ui.sections.video.MoviesActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemPressBackAndCheckActionbarTitle;

/**
 * Note: we use MoviesActivity here instead of AddonsActivity. The reason is that we use @Rule
 * to start the activity which is done prior to executing @Before. This results in a deadlock
 * situation.
 *
 * Normal startup procedure would be as follows:
 *
 * 1. Start MockTCPServer {@link AbstractTestClass#setupMockTCPServer()}
 * 2. Start activity {mActivityRule}
 * 3. Espresso waits for activity to become idle before calling {@link AbstractTestClass#setUp()}
 * 4. Add AddonsHandler {@link AbstractTestClass#setUp()}
 *
 * At step 2 the AddonsActivity displays an animated progress indicator while it waits for the
 * MockTCPServer to send the list of addons.
 * This is never send as the {@link org.xbmc.kore.testutils.tcpserver.handlers.AddonsHandler} is
 * added in {@link super#setUp()} which is never started by Espresso as it waits for
 * {@link org.xbmc.kore.ui.sections.addon.AddonsActivity} to become idle.
 */
public class AddonsActivityTests extends BaseMediaActivityTests<MoviesActivity> {

    @Rule
    public ActivityTestRule<MoviesActivity> mActivityRule = new ActivityTestRule<>(
            MoviesActivity.class);

    @Override
    protected ActivityTestRule<MoviesActivity> getActivityTestRule() {
        return mActivityRule;
    }

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
        //Start the AddonsActivity from the MoviesActivity
        Utils.openDrawer(getActivityTestRule());
        EspressoTestUtils.clickAdapterViewItem(8, R.id.navigation_drawer);
    }

    /**
     * Test if action bar title initially displays Addons
     */
    @Test
    public void setActionBarTitleMain() {
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(R.string.addons)));
    }

    /**
     * Test if action bar title is correctly set after selecting a list item
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Result: action bar title should show list item title
     */
    @Test
    public void setActionBarTitle() {
        EspressoTestUtils.selectListItemAndCheckActionbarTitle(0, R.id.list,
                                                               "Dumpert");
    }

    /**
     * Test if action bar title is correctly restored after a configuration change
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Rotate device
     *   3. Result: action bar title should show list item title
     */
    @Test
    public void restoreActionBarTitleOnConfigurationStateChanged() {
        EspressoTestUtils.selectListItemRotateDeviceAndCheckActionbarTitle(0, R.id.list,
                                                                           "Dumpert",
                                                                           getActivity());
    }

    /**
     * Test if action bar title is correctly restored after returning from a movie selection
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: action bar title should show main title
     */
    @Test
    public void restoreActionBarTitleOnReturningFromMovie() {
        selectListItemPressBackAndCheckActionbarTitle(0, R.id.list,
                                                      getActivity().getString(R.string.addons));
    }
}
