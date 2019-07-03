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
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.hamcrest.Matcher;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.action.ViewActions;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
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
                .perform(click(), typeText(query), closeSoftKeyboard());
        onView(isRoot()).perform(clearFocus());
    }

    /**
     * Clicks on the search menu item and clears the search query by entering the empty string
     * @param activity
     */
    public static void clearSearchQuery(Activity activity) {
        EspressoTestUtils.clickMenuItem(activity, activity.getString(R.string.action_search), R.id.action_search);

        onView(isAssignableFrom(AutoCompleteTextView.class))
                .perform(click(), clearText(), closeSoftKeyboard());
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
     * @param resourceId of adapter view holding the item that should be clicked
     */
    public static void clickAdapterViewItem(int position, int resourceId) {
        onData(anything()).inAdapterView(allOf(withId(resourceId), isDisplayed()))
                          .atPosition(position).perform(click());
    }


    public static void clickRecyclerViewItem(int position, int resourceId) {
        onView(withId(resourceId)).perform(RecyclerViewActions.actionOnItemAtPosition(position, click()));
    }

    public static void clickRecyclerViewItem(String text, int resourceId) {
        ViewInteraction viewInteraction = onView(allOf(withId(resourceId),
                                                       hasDescendant(withText(containsString(text))),
                                                       isDisplayed()));
        viewInteraction.perform(RecyclerViewActions.scrollTo(hasDescendant(withText(containsString(text)))));
        viewInteraction.perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(containsString(text))),
                                                                 click()));
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
     * @param resourceId resource identifier or list view
     */
    public static void checkListMatchesSearchQuery(String query, int listSize, int resourceId) {
        onView(isRoot()).perform(ViewActions.waitForView(resourceId, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return v.isShown();
            }
        }, 10000));

        onView(allOf(withId(resourceId), isDisplayed()))
                .check(matches(Matchers.withOnlyMatchingDataItems(hasDescendant(withText(containsString(query))))));
        checkRecyclerViewListsize(listSize, resourceId);
    }

    /**
     * Checks that the list size matches the given list size
     * @param listSize amount of elements expected in list
     */
    public static void checkRecyclerViewListsize(int listSize, int resourceId) {
        onView(allOf(withId(resourceId), isDisplayed()))
                .check(matches(Matchers.withRecyclerViewSize(listSize)));
    }

    /**
     * Checks if search action view does not exist in the current view hierarchy
     */
    public static void checkSearchMenuCollapsed() {
        onView(isAssignableFrom(AutoCompleteTextView.class)).check(doesNotExist());
    }

    /**
     * Returns the current active activity. Use this when the originally started activity
     * started a new activity and you need the reference to the new activity.
     * @return reference to the current active activity
     */
    public static Activity getActivity() {
        final Activity[] activity = new Activity[1];
        onView(allOf(withId(android.R.id.content), isDisplayed())).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "getting current activity";
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view.getContext() instanceof Activity) {
                    activity[0] = ((Activity)view.getContext());
                }
            }
        });
        return activity[0];
    }

    /**
     * Clicks on tab that contains the text given by stringResourceId.
     * @param stringResourceId text displayed in Tab that should be clicked
     */
    public static void clickTab(int stringResourceId) {
        onView(withId(R.id.pager)).perform(ViewActions.setCurrentViewPagerItem(stringResourceId));
    }

    /**
     * Clicks the album tab in the music activity
     */
    public static void clickAlbumsTab() {
        clickTab(R.string.albums);
    }

    /**
     * Clicks the artists tab in the music activity
     */
    public static void clickArtistsTab() {
        clickTab(R.string.artists);
    }

    /**
     * Clicks the genres tab in the music activity
     */
    public static void clickGenresTab() {
        clickTab(R.string.genres);
    }

    /**
     * Clicks the music videos tab in the music activity
     */
    public static void clickMusicVideosTab() {
        clickTab(R.string.videos);
    }

    /**
     * Selects an item in the list, then presses back and checks the action bar title
     * @param item number (0 is first item) of the item that should be pressed
     * @param listResourceId Resource identifier of the AdapterView
     * @param actionbarTitle title that should be displayed in the action bar after pressing back
     */
    public static void selectListItemPressBackAndCheckActionbarTitle(int item,
                                                                     int listResourceId,
                                                                     String actionbarTitle) {
        EspressoTestUtils.clickRecyclerViewItem(item, listResourceId);
        pressBack();
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(actionbarTitle)));
    }

    /**
     * Selects an item in the list, then presses back and checks the action bar title
     * @param itemText the text the item that must be pressed should contain
     * @param listResourceId Resource identifier of the AdapterView
     * @param actionbarTitle title that should be displayed in the action bar after pressing back
     */
    public static void selectListItemPressBackAndCheckActionbarTitle(String itemText,
                                                                     int listResourceId,
                                                                     String actionbarTitle) {
        EspressoTestUtils.clickRecyclerViewItem(itemText, listResourceId);
        pressBack();
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(containsString(actionbarTitle))));
    }

    /**
     * Selects an item in the list, then rotates the device and checks the action bar title
     * @param itemText the text the item that must be pressed should contain
     * @param listResourceId Resource identifier of the AdapterView
     * @param actionbarTitle title that should be displayed in the action bar after rotating
     */
    public static void selectListItemRotateDeviceAndCheckActionbarTitle(String itemText,
                                                                        int listResourceId,
                                                                        final String actionbarTitle,
                                                                        Activity activity) {
        EspressoTestUtils.clickRecyclerViewItem(itemText, listResourceId);
        EspressoTestUtils.rotateDevice(activity);

        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(containsString(actionbarTitle))));
    }

    /**
     * Selects an item in the list and then checks the action bar title
     * @param itemText the text the item that must be pressed should contain
     * @param listResourceId Resource identifier of the AdapterView
     * @param actionbarTitle title that should be displayed in the action bar after selecting item
     */
    public static void selectListItemAndCheckActionbarTitle(String itemText,
                                                            int listResourceId,
                                                            String actionbarTitle) {
        EspressoTestUtils.clickRecyclerViewItem(itemText, listResourceId);
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(actionbarTitle)));
    }

    /**
     * Waits for 10 seconds till panel has given state.
     *
     * @param panelState desired state of panel
     */
    public static void waitForPanelState(final SlidingUpPanelLayout.PanelState panelState) {
        onView(isRoot()).perform(ViewActions.waitForView(R.id.now_playing_panel, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((SlidingUpPanelLayout) v).getPanelState() == panelState;
            }
        }, 10000));
    }
}
