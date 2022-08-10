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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AbstractFragment extends Fragment {

    private DataHolder dataHolder;

    public void setDataHolder(DataHolder dataHolder) {
        this.dataHolder = dataHolder;
        Bundle bundle = getArguments();
        if (bundle == null) {
            setArguments(dataHolder.getBundle());
        } else {
            bundle.putAll(dataHolder.getBundle());
        }
    }

    public DataHolder getDataHolder() {
        return dataHolder;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( this.dataHolder == null ) {
            this.dataHolder = new DataHolder(-1);
        }

        this.dataHolder.setBundle(getArguments());
    }

    public static class DataHolder {
        static final String POSTER_TRANS_NAME = "POSTER_TRANS_NAME";
        static final String BUNDLE_KEY_ID = "id";
        static final String BUNDLE_KEY_TITLE = "title";
        static final String BUNDLE_KEY_UNDERTITLE = "undertitle";
        static final String BUNDLE_KEY_DESCRIPTION = "description";
        static final String BUNDLE_KEY_DETAILS = "details";
        static final String BUNDLE_KEY_POSTERURL = "poster";
        static final String BUNDLE_KEY_FANARTURL = "fanart";
        static final String BUNDLE_KEY_SQUAREPOSTER = "squareposter";
        static final String BUNDLE_KEY_RATING = "rating";
        static final String BUNDLE_KEY_MAXRATING = "maxrating";
        static final String BUNDLE_KEY_VOTES = "votes";
        static final String BUNDLE_KEY_IMDB_NUMBER = "imdbnumber";

        private Bundle bundle;

        public DataHolder(Bundle bundle) {
            setBundle(bundle);
        }

        public DataHolder(int itemId) {
            bundle = new Bundle();
            bundle.putInt(BUNDLE_KEY_ID, itemId);
        }

        public void setBundle(Bundle bundle) {
            this.bundle = bundle;
        }

        public void setPosterTransitionName(String posterTransitionName) {
            bundle.putString(POSTER_TRANS_NAME, posterTransitionName);
        }

        public void setSquarePoster(boolean squarePoster) {
            bundle.putBoolean(BUNDLE_KEY_SQUAREPOSTER, squarePoster);
        }

        public void setRating(double rating) {
            bundle.putDouble(BUNDLE_KEY_RATING, rating);
        }

        public void setMaxRating(int maxRating) {
            bundle.putInt(BUNDLE_KEY_MAXRATING, maxRating);
        }

        public void setVotes(int votes) {
            bundle.putInt(BUNDLE_KEY_VOTES, votes);
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

        public void setImdbNumber(String imdbNumebr) {
            bundle.putString(BUNDLE_KEY_IMDB_NUMBER, imdbNumebr);
        }

        public void setId(int id) {
            bundle.putInt(BUNDLE_KEY_ID, id);
        }

        public String getPosterTransitionName() {
            return bundle.getString(POSTER_TRANS_NAME);
        }

        public boolean getSquarePoster() {
            return bundle.getBoolean(BUNDLE_KEY_SQUAREPOSTER);
        }

        public double getRating() {
            return bundle.getDouble(BUNDLE_KEY_RATING);
        }

        public int getMaxRating() {
            return bundle.getInt(BUNDLE_KEY_MAXRATING);
        }

        public int getVotes() {
            return bundle.getInt(BUNDLE_KEY_VOTES);
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

        public String getImdbNumber() {
            return bundle.getString(BUNDLE_KEY_IMDB_NUMBER);
        }

        public int getId() {
            return bundle.getInt(BUNDLE_KEY_ID);
        }

        public Bundle getBundle() {
            return bundle;
        }
    }
}
