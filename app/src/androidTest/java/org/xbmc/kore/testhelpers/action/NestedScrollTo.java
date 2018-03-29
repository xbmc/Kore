/*
 * Copyright 2018 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testhelpers.action;

import android.graphics.Rect;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.util.HumanReadables;
import android.support.v4.widget.NestedScrollView;
import android.view.View;

import org.hamcrest.Matcher;
import org.xbmc.kore.utils.LogUtils;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;

/**
 * Modified version of {@link android.support.test.espresso.action.ScrollToAction} to support
 * NestedScrollView.
 * TODO Check future versions of {@link android.support.test.espresso.action.ScrollToAction} to see if support for NestedScrollView has been added
 */
public class NestedScrollTo implements ViewAction {
    private final static String TAG = LogUtils.makeLogTag(NestedScrollTo.class);

    @Override
    public Matcher<View> getConstraints() {
        return allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE), isDescendantOfA(anyOf(
                isAssignableFrom(NestedScrollView.class))));
    }

    @Override
    public String getDescription() {
        return "nested scroll to";
    }

    @Override
    public void perform(UiController uiController, View view) {
        if (isDisplayingAtLeast(90).matches(view)) {
            LogUtils.LOGI(TAG, "View is already displayed. Returning.");
            return;
        }
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        if (!view.requestRectangleOnScreen(rect, true /* immediate */)) {
            LogUtils.LOGW(TAG, "Scrolling to view was requested, but none of the parents scrolled.");
        }
        uiController.loopMainThreadUntilIdle();
        if (!isDisplayingAtLeast(90).matches(view)) {
            throw new PerformException.Builder()
                    .withActionDescription(this.getDescription())
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(new RuntimeException(
                            "Scrolling to view was attempted, but the view is not displayed"))
                    .build();
        }
    }
}
