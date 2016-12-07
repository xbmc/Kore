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


import android.support.test.espresso.ViewAction;
import static android.support.test.espresso.action.ViewActions.actionWithAssertions;

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

}
