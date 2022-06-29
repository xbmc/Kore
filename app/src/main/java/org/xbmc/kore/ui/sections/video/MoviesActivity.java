/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore.ui.sections.video;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.utils.LogUtils;

/**
 * Controls the presentation of Movies information (list, details)
 * All the information is presented by specific fragments
 */
public class MoviesActivity extends BaseMediaActivity
        implements MovieListFragment.OnMovieSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(MoviesActivity.class);

    public static final String MOVIEID = "movie_id";
    public static final String MOVIETITLE = "movie_title";

    private int selectedMovieId = -1;
    private String selectedMovieTitle;

    @Override
    protected String getActionBarTitle() {
        return (selectedMovieTitle != null) ? selectedMovieTitle : getString(R.string.movies);
    }

    @Override
    protected Fragment createFragment() {
        return new MovieListFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedMovieId = savedInstanceState.getInt(MOVIEID, -1);
            selectedMovieTitle = savedInstanceState.getString(MOVIETITLE, null);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MOVIEID, selectedMovieId);
        outState.putString(MOVIETITLE, selectedMovieTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Only respond to this if we are showing the movie details in portrait mode,
                // which can be checked by checking if selected movie != -1, in which case we
                // should go back to the previous fragment, which is the list.
                if (selectedMovieId != -1) {
                    selectedMovieId = -1;
                    selectedMovieTitle = null;
                    updateActionBar(getActionBarTitle(), false);
                    getSupportFragmentManager().popBackStack();
                    return true;
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If we are showing movie details in portrait, clear selected and show action bar
        if (selectedMovieId != -1) {
            selectedMovieId = -1;
            selectedMovieTitle = null;
            updateActionBar(getActionBarTitle(), false);
        }

        super.onBackPressed();
    }

    /**
     * Callback from movielist fragment when a movie is selected.
     * Switch fragment in portrait
     * @param vh ViewHolder holding movie info of item clicked
     */
    public void onMovieSelected(MovieListFragment.ViewHolder vh) {
        selectedMovieTitle = vh.dataHolder.getTitle();
        selectedMovieId = vh.dataHolder.getId();

        final MovieInfoFragment movieInfoFragment = new MovieInfoFragment();
        movieInfoFragment.setDataHolder(vh.dataHolder);

        showFragment(movieInfoFragment, vh.artView, vh.dataHolder);

        updateActionBar(selectedMovieTitle, true);
    }
}
