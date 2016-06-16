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

package org.xbmc.kore.tests.ui;

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.testhelpers.LoaderIdlingResource;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.ui.MusicActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RestoreSearchQueryViewPagerTest {

    private final String ARTIST_SEARCH_QUERY = "Ben";
    private final int ARTIST_SEARCH_QUERY_LIST_SIZE = 2;
    private final String ALBUMS_SEARCH_QUERY = "tes";
    private final int ALBUM_SEARCH_QUERY_LIST_SIZE = 3;
    private final int ARTIST_COMPLETE_LIST_SIZE = 224;
    private final int ALBUM_COMPLETE_LIST_SIZE = 231;

    private LoaderIdlingResource loaderIdlingResource;

    @Rule
    public ActivityTestRule<MusicActivity> mActivityRule = new ActivityTestRule<>(
            MusicActivity.class);

    @Before
    public void setUp() throws Throwable {
        Utils.initialize(mActivityRule);
        loaderIdlingResource = new LoaderIdlingResource(mActivityRule.getActivity().getSupportLoaderManager());
        Espresso.registerIdlingResources(loaderIdlingResource);
        Utils.closeDrawer(mActivityRule);
    }

    @After
    public void tearDown() throws Exception {
        Espresso.unregisterIdlingResources(loaderIdlingResource);
    }

    @AfterClass
    public static void cleanup() {
        Utils.cleanup();
    }

    /**
     * Simple test that checks if search query results in expected item(s)
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Result: search query entered at 1. should show in search field and list should match search query
     */
    @Test
    public void simpleSearchTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), ARTIST_SEARCH_QUERY);

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Simple test that checks if search query is restored after device rotate
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Rotate device
     *   3. Result: search query entered at 1. should show in search field and list should match search query
     */
    @Test
    public void simpleRotateTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), ARTIST_SEARCH_QUERY);
        EspressoTestUtils.rotateDevice(mActivityRule.getActivity());

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Test if search query is restored when user returns to list fragment from
     * detail fragment
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Click on list item
     *   3. Press back
     *   4. Result: search query entered at 1. should be restored in search field
     */
    @Test
    public void searchClickBackTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), ARTIST_SEARCH_QUERY);
        EspressoTestUtils.clickAdapterViewItem(0, R.id.list);
        Espresso.pressBack();

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Test if search query is restored when user returns to list fragment from
     * detail fragment when device is rotated while on detail fragment
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Click on list item
     *   3. Rotate device
     *   4. Press back
     *   5. Result: search query entered at 1. should be restored in search field
     */
    @Test
    public void searchClickRotateBackTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), ARTIST_SEARCH_QUERY);
        EspressoTestUtils.clickAdapterViewItem(0, R.id.list);
        EspressoTestUtils.rotateDevice(mActivityRule.getActivity());
        Espresso.pressBack();

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Test if search query is cleared when switching to
     * different tab in the TabAdapter
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Switch to Albums tab
     *   3. Result: search query should be cleared
     */
    @Test
    public void searchSwitchTabTest() {
        Activity activity = mActivityRule.getActivity();

        EspressoTestUtils.enterSearchQuery(activity, ARTIST_SEARCH_QUERY);
        clickAlbumsTab();

        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);
        EspressoTestUtils.checkTextInSearchQuery("");
    }

    /**
     * Tests if search query is still cleared when
     * device is rotated after switching to a different tab
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Switch to Albums tab
     *   3. Rotate device
     *   4. Open search menu item
     *   5. Result: search query should be cleared
     */
    @Test
    public void searchSwitchTabRotateTest() {
        Activity activity = mActivityRule.getActivity();

        EspressoTestUtils.enterSearchQuery(activity, ARTIST_SEARCH_QUERY);
        clickAlbumsTab();
        EspressoTestUtils.rotateDevice(activity);
        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);

        EspressoTestUtils.checkTextInSearchQuery("");
        EspressoTestUtils.checkListMatchesSearchQuery("", ALBUM_COMPLETE_LIST_SIZE, R.id.list);
    }

    /**
     * Tests if search query is restored when returning
     * to the original tab
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Switch to Albums tab
     *   3. Switch to Artists tab
     *   4. Result: search query entered at 1. should show in search field and list should match search query
     */
    @Test
    public void searchSwitchTabReturnTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), ARTIST_SEARCH_QUERY);
        clickAlbumsTab();
        clickArtistsTab();

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }



    /**
     * Tests if search query is restored when returning
     * to the original tab after switching to a different
     * tab and rotating the device
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Switch to Albums tab
     *   3. Rotated device
     *   4. Switch to Artists tab
     *   5. Result: search query entered at 1. should show in search field and list should match search query
     */
    @Test
    public void searchSwitchTabRotateReturnTest() {
        Activity activity = mActivityRule.getActivity();

        EspressoTestUtils.enterSearchQuery(activity, ARTIST_SEARCH_QUERY);
        clickAlbumsTab();
        EspressoTestUtils.rotateDevice(activity);
        clickArtistsTab();

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Tests if search query is still cleared when user clears a previous
     * search query and switches to a different tab and returns to the
     * original tab
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Clear search query
     *   3. Switch to Albums tab
     *   4. Switch to Artists tab
     *   5. Click search menu item
     *   6. Result: search query should be cleared and list should contain all items
     */
    @Test
    public void searchClearSwitchTabSwitchBack() {
        Activity activity = mActivityRule.getActivity();

        EspressoTestUtils.enterSearchQuery(activity, ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.clearSearchQuery(activity);
        clickAlbumsTab();
        clickArtistsTab();
        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);

        EspressoTestUtils.checkTextInSearchQuery("");
        EspressoTestUtils.checkListMatchesSearchQuery("", ARTIST_COMPLETE_LIST_SIZE, R.id.list);
    }

    /**
     * Same test as {@link #searchClearSwitchTabSwitchBack()} but this time clearing performed using X button
     *
     * UI interaction flow tested:
     *   1. Enter search query
     *   2. Clear search query
     *   3. Switch to Albums tab
     *   4. Switch to Artists tab
     *   5. Click search menu item using X button
     *   6. Result: search query should be cleared and list should contain all items
     */
    @Test
    public void searchSwitchTabSwitchBackClearUsingXButtonSwitchTabSwitchBack() {
        Activity activity = mActivityRule.getActivity();

        EspressoTestUtils.enterSearchQuery(activity, ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        clickAlbumsTab();
        clickArtistsTab();
        EspressoTestUtils.clearSearchQueryXButton(activity);
        clickAlbumsTab();
        clickArtistsTab();

        EspressoTestUtils.checkSearchMenuCollapsed();
        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);
        EspressoTestUtils.checkTextInSearchQuery("");
        EspressoTestUtils.checkListMatchesSearchQuery("", ARTIST_COMPLETE_LIST_SIZE, R.id.list);
    }

    /**
     * Tests if search queries for separate tabs are restored correctly
     *
     * UI interaction flow tested:
     *   1. Enter search query artists tab
     *   2. Enter search query albums tab
     *   3. Switch to Artists tab
     *   4. Result: search query entered at 1. should show in search field and list should match search query
     *   5. Switch to Albums tab
     *   6. Result: search query entered at 2. should show in search field and list should match search query
     */
    @Test
    public void searchArtistsSearchAlbumsSwitchArtists() {
        Activity activity = mActivityRule.getActivity();

        EspressoTestUtils.enterSearchQuery(activity, ARTIST_SEARCH_QUERY);
        clickAlbumsTab();
        EspressoTestUtils.enterSearchQuery(activity, ALBUMS_SEARCH_QUERY);
        clickArtistsTab();

        EspressoTestUtils.checkTextInSearchQuery(ARTIST_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ARTIST_SEARCH_QUERY, ARTIST_SEARCH_QUERY_LIST_SIZE, R.id.list);

        clickAlbumsTab();

        EspressoTestUtils.checkTextInSearchQuery(ALBUMS_SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(ALBUMS_SEARCH_QUERY, ALBUM_SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    private void clickAlbumsTab() {
        onView(withId(R.id.pager_tab_strip)).perform(swipeLeft());
        onView(withText(R.string.albums)).perform(click());
    }

    private void clickArtistsTab() {
        onView(withId(R.id.pager_tab_strip)).perform(swipeRight());
        onView(withText(R.string.artists)).perform(click());
    }
}
