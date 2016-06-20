/**
 * Copyright (C) 2014 Subito.it S.r.l (www.subito.it)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.testhelpers.action;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.allOf;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

public class ClearFocus implements ViewAction {

    @Override
    public Matcher<View> getConstraints() {

        return allOf(isDisplayed(), isAssignableFrom(View.class));
    }

    @Override
    public String getDescription() {

        return "Clear focus on the given view";
    }

    @Override
    public void perform(UiController uiController, View view) {

        view.clearFocus();
    }
}

