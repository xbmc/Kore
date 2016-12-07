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

package org.xbmc.kore.testhelpers;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.widget.AutoCompleteTextView;

import org.xbmc.kore.R;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.xbmc.kore.testhelpers.action.ViewActions.clearFocus;

public class EspressoTestUtils {

    public static void rotateDevice(Activity activity) {
        int orientation
                = activity.getResources().getConfiguration().orientation;
        activity.setRequestedOrientation(
                (orientation == Configuration.ORIENTATION_PORTRAIT) ?
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Clicks a menu item regardless if it is in the overflow menu or
     * visible as icon in the action bar
     * @param activity
     * @param name Name of the menu item in the overflow menu
     * @param resourceId Resource identifier of the menu item
     */
    public static void clickMenuItem(Activity activity, String name, int resourceId) {
        try {
            onView(withId(resourceId)).perform(click());
        } catch (NoMatchingViewException e) {
            openActionBarOverflowOrOptionsMenu(activity);
            //Use onData as item might not be visible in the View without scrolling
            onData(allOf(
                    Matchers.withMenuTitle(name)))
                    .perform(click());
        }
    }

    /**
     * Clicks the arrow button in the toolbar when its function is collapsing a view. For instance,
     * collapse the search view in the toolbar.
     */
    public static void clickToolbarCollapseButton() {
        /**
         * The image button in the toolbar used as home/collapse/back button has no ID we can use.
         * In appcompat v7 the arrow button in the toolbar used to collapse a search view has a
         * description "Collapse". We use this to find the button in the view and perform the click
         * action.
         */
        onView(withContentDescription("Collapse")).perform(click());
    }

    /**
     * Clicks on the search menu item and enters the given search query
     * @param activity
     * @param query
     */
    public static void enterSearchQuery(Activity activity, String query) {
        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);

        onView(isAssignableFrom(AutoCompleteTextView.class))
                .perform(click(), typeText(query), clearFocus());

        Espresso.closeSoftKeyboard();
    }

    /**
     * Clicks on the search menu item and clears the search query by entering the empty string
     * @param activity
     */
    public static void clearSearchQuery(Activity activity) {
        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);

        onView(isAssignableFrom(AutoCompleteTextView.class))
                .perform(click(), clearText());

        Espresso.closeSoftKeyboard();
    }

    /**
     * Clears the search query by pressing the X button
     * @param activity
     */
    public static void clearSearchQueryXButton(Activity activity) {
        try {
            onView(withId(R.id.search_close_btn)).perform(click());
        } catch (NoMatchingViewException e) {
            EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);
            onView(withId(R.id.search_close_btn)).perform(click());
        }
        Espresso.closeSoftKeyboard();
    }

    /**
     * Performs a click on an item in an adapter view, such as GridView or ListView
     * @param position
     * @param resourceId
     */
    public static void clickAdapterViewItem(int position, int resourceId) {
        onData(anything()).inAdapterView(allOf(withId(resourceId), isDisplayed()))
                .atPosition(position).perform(click());
    }

    /**
     * Checks that SearchView contains the given text
     * @param query text that SearchView should contain
     */
    public static void checkTextInSearchQuery(String query) {
       onView(isAssignableFrom(AutoCompleteTextView.class)).check(matches(withText(query)));
    }

    /**
     * Checks that the list contains item(s) matching search query
     * @param query text each element must contain
     * @param listSize amount of elements expected in list
     */
    public static void checkListMatchesSearchQuery(String query, int listSize, int resourceId) {
        onView(allOf(withId(resourceId), isDisplayed()))
                .check(matches(Matchers.withOnlyMatchingDataItems(Matchers.withItemContent(containsString(query)))));
        onView(allOf(withId(resourceId), isDisplayed()))
                .check(matches(Matchers.withAdapterSize(listSize)));
    }

    /**
     * Checks if search action view does not exist in the current view hierarchy
     */
    public static void checkSearchMenuCollapsed() {
        onView(isAssignableFrom(AutoCompleteTextView.class)).check(doesNotExist());
    }
}
