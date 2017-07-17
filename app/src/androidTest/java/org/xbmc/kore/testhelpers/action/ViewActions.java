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

package org.xbmc.kore.testhelpers.action;


import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.MotionEvents;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.util.HumanReadables;
import android.support.test.espresso.util.TreeIterables;
import android.view.View;
import android.widget.SeekBar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.concurrent.TimeoutException;

import static android.support.test.espresso.action.ViewActions.actionWithAssertions;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;

public final class ViewActions {

    /**
     * Returns an action that clears the focus on the view.
     * <br/>
     * View constraints:
     * <ul>
     * <li>must be displayed on screen</li>
     * </ul>
     */
    public static ViewAction clearFocus() {
        return actionWithAssertions(new ClearFocus());
    }

    public interface CheckStatus {
        boolean check(View v);
    }

    /**
     * ViewAction that waits until view with viewId becomes visible
     * @param viewId Resource identifier of view item that must be checked
     * @param checkStatus called when viewId has been found to check its status. If return value
     *                      is true waitForView will stop, false it will continue until timeout is exceeded
     * @param millis amount of time to wait for view to become visible
     * @return
     */
    public static ViewAction waitForView(final int viewId, final CheckStatus checkStatus, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Searches for view with id: " + viewId + " and tests its status using CheckStatus, using timeout " + millis + " ms.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
                final long endTime = System.currentTimeMillis() + millis;
                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        if (child.getId() == viewId) {
                            if (checkStatus.check(child)) {
                                return;
                            }
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                } while (System.currentTimeMillis() < endTime);

                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    public static ViewAction slideSeekBar(final int progress) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return new TypeSafeMatcher<View>() {
                    @Override
                    protected boolean matchesSafely(View item) {
                        return item instanceof SeekBar;
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("is a SeekBar.");
                    }
                };
            }

            @Override
            public String getDescription() {
                return "Slides seekbar to progress position " + progress;
            }

            @Override
            public void perform(UiController uiController, View view) {
                SeekBar seekBar = (SeekBar) view;

                int[] seekBarPos = {0,0};
                view.getLocationOnScreen(seekBarPos);
                float[] startPos = {seekBarPos[0], seekBarPos[1]};

                MotionEvents.DownResultHolder downResultHolder =
                        MotionEvents.sendDown(uiController, startPos,
                                              Press.PINPOINT.describePrecision());

                while(seekBar.getProgress() < progress) {
                    startPos[0]++;
                    MotionEvents.sendMovement(uiController, downResultHolder.down, startPos);
                    uiController.loopMainThreadForAtLeast(10);
                }

                MotionEvents.sendUp(uiController, downResultHolder.down, startPos);
            }
        };
    }
}
