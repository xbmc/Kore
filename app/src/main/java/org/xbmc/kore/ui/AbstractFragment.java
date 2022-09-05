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

package org.xbmc.kore.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.greenrobot.event.EventBus;

public class AbstractFragment extends Fragment {

    private final String BUNDLE_KEY_POSTPONE_TRANSITION = "BUNDLE_KEY_POSTPONE_TRANSITION";

    protected boolean shouldPostponeReenterTransition = false;
    private DataHolder dataHolder;

    /**
     * This is a EventBus message that should be sent by list fragments when their setup is complete, which allows for
     * controlling a reenter shared element transition.
     * If specified by shouldPostponeReenterTransition the list fragment should postpone its enter transition in
     * onViewCreated, and when it is fully setup, launch this Event Bus message, so that the postponed transition
     * is started here.
     * This framework allows for a shared element transition from a list to a details fragment to work, as well as
     * supporting Tabs Fragments. When using a Tabs Fragment, it needs to postponed its reenter transition, and be
     * notified when the *child* list fragment is completly set up, so that the postponed transition starts. With this
     * framework, the *child* list fragment sends this Event Bus message, which is caught and acted upon by the
     * Tabs Fragment (instead of by the child list fragment, as it doesn't have the shouldPostponeReenterTransition set)
     */
    public static class ListFragmentSetupComplete {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.dataHolder = new DataHolder(getArguments());
        if (savedInstanceState != null) {
            shouldPostponeReenterTransition = savedInstanceState.getBoolean(BUNDLE_KEY_POSTPONE_TRANSITION);
        }
    }
    public void onStart() {
        super.onStart();
        EventBus.getDefault()
                .register(this);
    }
    @Override
    public void onStop() {
        EventBus.getDefault()
                .unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(BUNDLE_KEY_POSTPONE_TRANSITION, shouldPostponeReenterTransition);
        super.onSaveInstanceState(outState);
    }

    /**
     * Call this to set a flag indicating to postpone the reenter transition. This is useful in reenter transitions
     * after a pop of the back stack, as it allows control of when the return shared element transition should play.
     * Make sure that a ListFragmentSetupComplete Event Bus message is sent when the transition should run
     * (shared element is loaded and ready)
     */
    public void setPostponeReenterTransition(boolean val) {
        shouldPostponeReenterTransition = val;
    }

    /**
     * Event bus callback when a fragment notifies that it's setup is complete
     * This is used in list fragments, so that we can start a previously postponed reenter transitions
     * @param event Event
     */
    public void onEventMainThread(ListFragmentSetupComplete event) {
        if (shouldPostponeReenterTransition) {
            // We postponed the enter transition, launch it now
            View rootView = requireView();
            rootView.getViewTreeObserver()
                    .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            rootView.getViewTreeObserver().removeOnPreDrawListener(this);
                            startPostponedEnterTransition();
                            return true;
                        }
                    });
            shouldPostponeReenterTransition = false;
        }
    }

    public DataHolder getDataHolder() {
        return dataHolder;
    }

    public static class DataHolder {
        static final String BUNDLE_KEY_ID = "id";
        static final String BUNDLE_KEY_TITLE = "title";
        static final String BUNDLE_KEY_UNDERTITLE = "undertitle";
        static final String BUNDLE_KEY_DESCRIPTION = "description";
        static final String BUNDLE_KEY_DETAILS = "details";
        static final String BUNDLE_KEY_POSTERURL = "poster";
        static final String BUNDLE_KEY_FANARTURL = "fanart";
        static final String BUNDLE_KEY_SQUAREPOSTER = "squareposter";
        static final String BUNDLE_KEY_RATING = "rating";
        static final String BUNDLE_KEY_VOTES = "votes";
        static final String BUNDLE_KEY_SEARCH_TERMS = "searchterms";

        private final Bundle bundle;

        public DataHolder(Bundle bundle) {
            this.bundle = bundle;
        }

        public DataHolder(int itemId) {
            bundle = new Bundle();
            bundle.putInt(BUNDLE_KEY_ID, itemId);
        }

        public void setSquarePoster(boolean squarePoster) {
            bundle.putBoolean(BUNDLE_KEY_SQUAREPOSTER, squarePoster);
        }

        public void setRating(double rating) {
            bundle.putDouble(BUNDLE_KEY_RATING, rating);
        }

        public void setVotes(String votes) {
            bundle.putString(BUNDLE_KEY_VOTES, votes);
        }

        public void setPosterUrl(String posterUrl) {
            bundle.putString(BUNDLE_KEY_POSTERURL, posterUrl);
        }

        public void setTitle(String title) {
            bundle.putString(BUNDLE_KEY_TITLE, title);
        }

        public void setUndertitle(String underTitle) {
            bundle.putString(BUNDLE_KEY_UNDERTITLE, underTitle);
        }

        public void setDescription(String description) {
            bundle.putString(BUNDLE_KEY_DESCRIPTION, description);
        }

        public void setDetails(String details) {
            bundle.putString(BUNDLE_KEY_DETAILS, details);
        }

        public void setFanArtUrl(String fanArtUrl) {
            bundle.putString(BUNDLE_KEY_FANARTURL, fanArtUrl);
        }

        public void setSearchTerms(String searchTerms) {
            bundle.putString(BUNDLE_KEY_SEARCH_TERMS, searchTerms);
        }

        public void setId(int id) {
            bundle.putInt(BUNDLE_KEY_ID, id);
        }

        public boolean getSquarePoster() {
            return bundle.getBoolean(BUNDLE_KEY_SQUAREPOSTER);
        }

        public double getRating() {
            return bundle.getDouble(BUNDLE_KEY_RATING);
        }

        public String getVotes() {
            return bundle.getString(BUNDLE_KEY_VOTES);
        }

        public String getPosterUrl() {
            return bundle.getString(BUNDLE_KEY_POSTERURL);
        }

        public String getTitle() {
            return bundle.getString(BUNDLE_KEY_TITLE);
        }

        public String getUnderTitle() {
            return bundle.getString(BUNDLE_KEY_UNDERTITLE);
        }

        public String getDescription() {
            return bundle.getString(BUNDLE_KEY_DESCRIPTION);
        }

        public String getDetails() {
            return bundle.getString(BUNDLE_KEY_DETAILS);
        }

        public String getFanArtUrl() {
            return bundle.getString(BUNDLE_KEY_FANARTURL);
        }

        public String getSearchTerms() {
            return bundle.getString(BUNDLE_KEY_SEARCH_TERMS);
        }

        public int getId() {
            return bundle.getInt(BUNDLE_KEY_ID);
        }

        public Bundle getBundle() {
            return bundle;
        }
    }
}
