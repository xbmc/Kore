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

import androidx.loader.app.LoaderManager;
import androidx.test.espresso.IdlingResource;

public class LoaderIdlingResource implements IdlingResource {

    private ResourceCallback mResourceCallback;
    private LoaderManager loaderManager;

    public LoaderIdlingResource(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;
    }

    @Override
    public String getName() {
        return LoaderIdlingResource.class.getName();
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = !loaderManager.hasRunningLoaders();
        if (idle && mResourceCallback != null) {
            mResourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}