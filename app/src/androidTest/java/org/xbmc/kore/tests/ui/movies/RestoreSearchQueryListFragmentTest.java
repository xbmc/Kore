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

package org.xbmc.kore.tests.ui.movies;

import android.content.Context;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.ui.sections.video.MoviesActivity;

@RunWith(AndroidJUnit4.class)
public class RestoreSearchQueryListFragmentTest extends AbstractTestClass<MoviesActivity> {

    private final String SEARCH_QUERY = "Room";
    private final int SEARCH_QUERY_LIST_SIZE = 2;
    private final int COMPLETE_LIST_SIZE = 300;

    @Rule
    public ActivityTestRule<MoviesActivity> mActivityRule = new ActivityTestRule<>(
            MoviesActivity.class);

    @Override
    protected ActivityTestRule<MoviesActivity> getActivityTestRule() {
        return mActivityRule;
    }

    @Override
    protected void setSharedPreferences(Context context) {

    }

    /**
     * Simple test that checks if search query results in expected item(s)
     */
    @Test
    public void simpleSearchTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);

        EspressoTestUtils.checkTextInSearchQuery(SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(SEARCH_QUERY, SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Simple test that checks if search query is restored after device rotate
     */
    @Test
    public void simpleRotateTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.rotateDevice(mActivityRule.getActivity());

        EspressoTestUtils.checkTextInSearchQuery(SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(SEARCH_QUERY, SEARCH_QUERY_LIST_SIZE, R.id.list);
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
        EspressoTestUtils.clearSearchQuery(mActivityRule.getActivity());
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.clickRecyclerViewItem(0, R.id.list);
        Espresso.pressBack();

        EspressoTestUtils.checkTextInSearchQuery(SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(SEARCH_QUERY, SEARCH_QUERY_LIST_SIZE, R.id.list);
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
        EspressoTestUtils.clearSearchQuery(mActivityRule.getActivity());
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.clickRecyclerViewItem(0, R.id.list);
        EspressoTestUtils.rotateDevice(mActivityRule.getActivity());
        Espresso.pressBack();

        EspressoTestUtils.checkTextInSearchQuery(SEARCH_QUERY);
        EspressoTestUtils.checkListMatchesSearchQuery(SEARCH_QUERY, SEARCH_QUERY_LIST_SIZE, R.id.list);
    }

    /**
     * Test if saved search query is cleared when user clears the
     * search query view
     *
     * UI interaction flow tested
     *   1. Enter search query
     *   2. Click on list item
     *   3. Return to list
     *   4. Clear search query
     *   5. Click on list item
     *   6. Return to list
     *   7. Result: search query should be empty and collapsed
     */
    @Test
    public void searchClickBackClearSearchClickBackTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.clickRecyclerViewItem(0, R.id.list);
        Espresso.pressBack();
        EspressoTestUtils.clearSearchQuery(mActivityRule.getActivity());
        EspressoTestUtils.clickRecyclerViewItem(0, R.id.list);
        Espresso.pressBack();

        EspressoTestUtils.checkSearchMenuCollapsed();
    }

    /**
     * Test if after restoring search query the search query is cleared
     * when user presses back again.
     *
     * UI interaction flow tested
     *   1. Enter search query
     *   2. Click on list item
     *   3. Return to list
     *   4. Press back
     *   7. Result: search query should be cleared, collapsed, and list should show everything
     */
    @Test
    public void searchClickBackBackTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.clickRecyclerViewItem(0, R.id.list);
        Espresso.pressBack();
        Espresso.pressBack();

        EspressoTestUtils.checkSearchMenuCollapsed();
        EspressoTestUtils.checkListMatchesSearchQuery("", COMPLETE_LIST_SIZE, R.id.list);
    }

    /**
     * Test if pressing back clears a previous search
     *
     * UI interaction flow tested
     *   1. Enter search query
     *   2. Press back
     *   3. Result: search query should be cleared, collapsed, and list should show everything
     */
    @Test
    public void searchBackTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        Espresso.pressBack();
        EspressoTestUtils.checkSearchMenuCollapsed();
        EspressoTestUtils.checkListMatchesSearchQuery("", COMPLETE_LIST_SIZE, R.id.list);
    }

    /**
     * Test if after restoring the search query pressing home button up clears a previous search
     *
     * UI interaction flow tested
     *   1. Enter search query
     *   2. Click on list item
     *   3. Press back
     *   4. Press home button
     *   5. Result: search query should be cleared, collapsed, and list should show everything
     */
    @Test
    public void searchClickBackUpTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.clickRecyclerViewItem(0, R.id.list);
        Espresso.pressBack();
        EspressoTestUtils.clickToolbarCollapseButton();
        EspressoTestUtils.checkSearchMenuCollapsed();
        EspressoTestUtils.checkListMatchesSearchQuery("", COMPLETE_LIST_SIZE, R.id.list);
    }

    /**
     * Test if pressing home button up clears a previous search
     *
     * UI interaction flow tested
     *   1. Enter search query
     *   2. Press home button
     *   3. Result: search query should be cleared, collapsed, and list should show everything
     */
    @Test
    public void searchUpTest() {
        EspressoTestUtils.enterSearchQuery(mActivityRule.getActivity(), SEARCH_QUERY);
        EspressoTestUtils.clickToolbarCollapseButton();
        EspressoTestUtils.checkSearchMenuCollapsed();
        EspressoTestUtils.checkListMatchesSearchQuery("", COMPLETE_LIST_SIZE, R.id.list);
    }
}
