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

package org.xbmc.kore.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SharedElementCallback;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;

import org.xbmc.kore.R;

import java.util.List;
import java.util.Map;

public class SharedElementTransition {
    private static final String TAG = LogUtils.makeLogTag(SharedElementTransition.class);

    public interface SharedElement {

        /**
         * Returns if the shared element if visible
         * @return true if visible, false otherwise
         */
        boolean isSharedElementVisible();
    }

    private boolean clearSharedElements;

    /**
     * Sets up the transition for the exiting fragment
     * @param fragment
     */
    @TargetApi(21)
    public void setupExitTransition(Context context, Fragment fragment) {
        Transition fade = TransitionInflater
                .from(context)
                .inflateTransition(android.R.transition.fade);
        fragment.setExitTransition(fade);
        fragment.setReenterTransition(fade);

        fragment.setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // Clearing must be done in the reentering fragment
                // as this is called last. Otherwise, the app will crash during transition setup. Not sure, but might
                // be a v4 support package bug.
                if (clearSharedElements) {
                    names.clear();
                    sharedElements.clear();
                    clearSharedElements = false;
                }
            }
        });
    }

    /**
     * Sets up the transition for the entering fragment
     * @param fragmentTransaction
     * @param fragment entering fragment
     * @param sharedElement must have the transition name set
     */
    @TargetApi(21)
    public void setupEnterTransition(Context context,
                                     FragmentTransaction fragmentTransaction,
                                     final Fragment fragment,
                                     View sharedElement) {
        if (!(fragment instanceof SharedElement)) {
            LogUtils.LOGD(TAG, "Enter transition fragment must implement SharedElement interface");
            return;
        }

        android.support.v4.app.SharedElementCallback seCallback = new android.support.v4.app.SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // On returning, onMapSharedElements for the exiting fragment is called before the onMapSharedElements
                // for the reentering fragment. We use this to determine if we are returning and if
                // we should clear the shared element lists. Note that, clearing must be done in the reentering fragment
                // as this is called last. Otherwise, the app will crash during transition setup. Not sure, but might
                // be a v4 support package bug.
                if (fragment.isVisible() && (!((SharedElement) fragment).isSharedElementVisible())) {
                    // shared element not visible
                    clearSharedElements = true;
                }
            }
        };
        fragment.setEnterSharedElementCallback(seCallback);

        fragment.setEnterTransition(TransitionInflater
                                            .from(context)
                                            .inflateTransition(R.transition.media_details));
        fragment.setReturnTransition(null);

        Transition changeImageTransition = TransitionInflater.from(
                context).inflateTransition(R.transition.change_image);
        fragment.setSharedElementReturnTransition(changeImageTransition);
        fragment.setSharedElementEnterTransition(changeImageTransition);

        fragmentTransaction.addSharedElement(sharedElement, sharedElement.getTransitionName());
    }
}
