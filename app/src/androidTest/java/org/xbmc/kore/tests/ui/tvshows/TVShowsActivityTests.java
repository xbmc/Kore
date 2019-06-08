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

package org.xbmc.kore.tests.ui.tvshows;

import android.content.Context;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.widget.TextView;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.ui.sections.video.TVShowsActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickRecyclerViewItem;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.rotateDevice;
import static org.xbmc.kore.testhelpers.action.ViewActions.nestedScrollTo;

public class TVShowsActivityTests extends AbstractTestClass<TVShowsActivity> {
    private final String TV_SHOW_TITLE = "11.22.63";
    private final String EPISODE_TITLE = "The Rabbit Hole";

    @Rule
    public ActivityTestRule<TVShowsActivity> mActivityRule = new ActivityTestRule<>(
            TVShowsActivity.class);

    @Override
    protected ActivityTestRule<TVShowsActivity> getActivityTestRule() {
        return mActivityRule;
    }

    @Override
    protected void setSharedPreferences(Context context) {

    }

    /**
     * Test if action bar title initially displays TV Shows
     */
    @Test
    public void setActionBarTitleMain() {
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(R.string.tv_shows)));
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
        EspressoTestUtils.selectListItemAndCheckActionbarTitle(TV_SHOW_TITLE, R.id.list, TV_SHOW_TITLE);
    }

    /**
     * Test if action bar title is correctly set after selecting a season
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Click on next episode item
     *   3. Result: action bar title should show next episode title
     */
    @Test
    public void setActionBarTitleOnNextEpisode() {
        clickRecyclerViewItem(1, R.id.list);
        onView( withId(R.id.next_episode_list)).perform( nestedScrollTo(), click());

        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText("3")));
    }

    /**
     * Test if action bar title is correctly set after selecting a season
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Click on season item
     *   3. Result: action bar title should show season title
     */
    @Test
    public void setActionBarTitleOnSeasonList() {
        clickRecyclerViewItem(0, R.id.list);
        onView( withId(R.id.seasons_list)).perform(nestedScrollTo(), click());

        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText("Season 01")));
    }

    /**
     * Test if action bar title is correctly set after selecting an episode from the season list
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Click on season item
     *   3. Click on an episode
     *   4. Result: action bar title should show episode title
     */
    @Test
    public void setActionBarTitleOnSeasonListEpisode() {
        clickRecyclerViewItem(0, R.id.list);
        onView( withId(R.id.seasons_list)).perform( nestedScrollTo(), click());
        EspressoTestUtils.selectListItemAndCheckActionbarTitle(EPISODE_TITLE, R.id.list, TV_SHOW_TITLE);
    }

    /**
     * Test if action bar title is correctly restored after a configuration change
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Rotate device
     *   3. Result: action bar title should show TV show item title
     */
    @Test
    public void restoreActionBarTitleOnConfigurationStateChanged() {
        EspressoTestUtils.selectListItemRotateDeviceAndCheckActionbarTitle(TV_SHOW_TITLE, R.id.list,
                                                                           TV_SHOW_TITLE,
                                                                           mActivityRule.getActivity());
    }

    /**
     * Test if action bar title is correctly restored on season list after a configuration change
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Click on season item
     *   3. Rotate device
     *   4. Result: action bar title should show season title
     */
    @Test
    public void restoreActionBarTitleSeasonListOnConfigurationStateChanged() {
        clickRecyclerViewItem(0, R.id.list);
        onView( withId(R.id.seasons_list)).perform( nestedScrollTo(), click());
        EspressoTestUtils.rotateDevice(mActivityRule.getActivity());

        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText("Season 01")));
    }

    /**
     * Test if action bar title is correctly restored on episode item title after a configuration change
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Click on season item
     *   3. Click on episode item
     *   4. Rotate device
     *   5. Result: action bar title should TV show title
     */
    @Test
    public void restoreActionBarTitleSeasonListEpisodeOnConfigurationStateChanged() {
        clickRecyclerViewItem(0, R.id.list);
        onView( withId(R.id.seasons_list)).perform( nestedScrollTo(), click());
        EspressoTestUtils.selectListItemRotateDeviceAndCheckActionbarTitle(EPISODE_TITLE, R.id.list,
                                                                           TV_SHOW_TITLE,
                                                                           mActivityRule.getActivity());
    }

    /**
     * Test if action bar title is correctly restored on next episode item title after a configuration change
     *
     * UI interaction flow tested:
     *   1. Click on TV Show item
     *   2. Click on next episode item
     *   3. Rotate device
     *   4. Result: action bar title should show season title
     */
    @Test
    public void restoreActionBarTitleNextEpisodeOnConfigurationStateChanged() {
        clickRecyclerViewItem(1, R.id.list);
        onView( withId(R.id.next_episode_list)).perform( nestedScrollTo() );
        onView( withText("You'll See the Sparkle")).perform( click() );
        EspressoTestUtils.rotateDevice(mActivityRule.getActivity());

        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText("3")));
    }

    /**
     * Test if the initial state shows the hamburger icon
     */
    @Test
    public void showHamburgerInInitialState() {
        assertFalse(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is changed to an arrow when selecting a list item
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Result: navigation icon should be an arrow
     */
    @Test
    public void showArrowWhenSelectingListItem() {
        clickRecyclerViewItem(0, R.id.list);

        assertTrue(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is changed to an arrow when selecting a list item
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: navigation icon should be a hamburger
     */
    @Test
    public void showHamburgerWhenSelectingListItemAndReturn() {
        clickRecyclerViewItem(0, R.id.list);
        Espresso.pressBack();

        assertFalse(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is restored to an arrow when selecting a list item
     * and rotating the device
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Rotate device
     *   3. Result: navigation icon should be an arrow
     */
    @Test
    public void restoreArrowOnConfigurationChange() {
        clickRecyclerViewItem(0, R.id.list);
        rotateDevice(getActivity());

        assertTrue(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is restored to an hamburger when selecting a list item
     * and rotating the device and returning to the list
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Rotate device
     *   3. Press back
     *   4. Result: navigation icon should be a hamburger
     */
    @Test
    public void restoreHamburgerOnConfigurationChangeOnReturn() {
        clickRecyclerViewItem(0, R.id.list);
        rotateDevice(getActivity());
        Espresso.pressBack();

        assertTrue(EspressoTestUtils.getActivity() instanceof TVShowsActivity);
        assertFalse(((TVShowsActivity) EspressoTestUtils.getActivity()).getDrawerIndicatorIsArrow());
    }
}
