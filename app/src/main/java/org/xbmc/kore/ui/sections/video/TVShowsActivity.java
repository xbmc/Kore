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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.utils.LogUtils;

/**
 * Controls the presentation of TV Shows information (list, details)
 * All the information is presented by specific fragments
 */
public class TVShowsActivity extends BaseMediaActivity
        implements TVShowListFragment.OnTVShowSelectedListener,
                   TVShowProgressFragment.TVShowProgressActionListener,
                   TVShowEpisodeListFragment.OnEpisodeSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(TVShowsActivity.class);

    public static final String TVSHOWID = "tvshow_id";
    public static final String TVSHOWTITLE = "tvshow_title";
    public static final String EPISODEID = "episode_id";
    public static final String SEASON = "season";
    public static final String SEASONTITLE = "season_title";

    private int selectedTVShowId = -1;
    private String selectedTVShowTitle = null;
    private int selectedSeason = -1;
    private String selectedSeasonTitle = null;
    private int selectedEpisodeId = -1;

    @Override
    protected String getActionBarTitle() {
        return (selectedSeasonTitle != null) ? selectedSeasonTitle :
               (selectedTVShowTitle != null) ? selectedTVShowTitle : getString(R.string.tv_shows);
    }

    @Override
    protected Fragment createFragment() {
        return new TVShowListFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedTVShowId = savedInstanceState.getInt(TVSHOWID, -1);
            selectedTVShowTitle = savedInstanceState.getString(TVSHOWTITLE, null);
            selectedEpisodeId = savedInstanceState.getInt(EPISODEID, -1);
            selectedSeason = savedInstanceState.getInt(SEASON, -1);
            selectedSeasonTitle = savedInstanceState.getString(SEASONTITLE, null);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TVSHOWID, selectedTVShowId);
        outState.putString(TVSHOWTITLE, selectedTVShowTitle);
        outState.putInt(EPISODEID, selectedEpisodeId);
        outState.putInt(SEASON, selectedSeason);
        outState.putString(SEASONTITLE, selectedSeasonTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getDrawerIndicatorIsArrow()) {
                    getSupportFragmentManager().popBackStack();
                    updateActionBar();
                    return true;
                }
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        updateActionBar();
        super.onBackPressed();
    }

    /**
     * Callback from tvshows list fragment when a show is selected.
     */
    public void onTVShowSelected(AbstractFragment.DataHolder dataHolder, ImageView sharedImageView) {
        selectedTVShowId = dataHolder.getId();
        selectedTVShowTitle = dataHolder.getTitle();

        // Replace list fragment
        showFragment(TVShowInfoFragment.class, dataHolder.getBundle(), sharedImageView);
        updateActionBar(selectedTVShowTitle, true);
    }

    /**
     * Callback from tvshow details when a season is selected
     * @param tvshowId tv show id
     * @param seasonId season number
     */
    public void onSeasonSelected(int tvshowId, int seasonId, String seasonPoster) {
        selectedSeason = seasonId;

        // Replace fragment
        Bundle args = new Bundle();
        args.putInt(TVShowEpisodeListFragment.TVSHOWID, tvshowId);
        args.putInt(TVShowEpisodeListFragment.TVSHOWSEASON, seasonId);
        args.putString(TVShowEpisodeListFragment.TVSHOWSEASONPOSTERURL, seasonPoster);
        showFragment(TVShowEpisodeListFragment.class, args);

        selectedSeasonTitle = String.format(getString(R.string.season_number), seasonId);
        updateActionBar(selectedSeasonTitle, true);
    }

    /**
     * Callback from tvshow details when a episode is selected
     */
    public void onNextEpisodeSelected(int tvshowId, AbstractInfoFragment.DataHolder dataHolder) {
        selectedEpisodeId = dataHolder.getId();

        // Replace list fragment
        Bundle args = dataHolder.getBundle();
        args.putInt(TVShowEpisodeInfoFragment.BUNDLE_KEY_TVSHOWID, tvshowId);
        showFragment(TVShowEpisodeInfoFragment.class, args);

        updateActionBar(selectedTVShowTitle, true);
    }

    /**
     * Callback from tvshow episodes list when a episode is selected
     */
    public void onEpisodeSelected(int tvshowId, AbstractInfoFragment.DataHolder dataHolder) {
        selectedEpisodeId = dataHolder.getId();

        dataHolder.getBundle().putInt(TVShowEpisodeInfoFragment.BUNDLE_KEY_TVSHOWID, tvshowId);
        showFragment(TVShowEpisodeInfoFragment.class, dataHolder.getBundle());

        updateActionBar(selectedTVShowTitle, true);
    }

    private void updateActionBar() {
        if (selectedEpisodeId != -1) {
            selectedEpisodeId = -1;
            if (selectedSeason != -1)
                updateActionBar(selectedSeasonTitle, true);
            else
                updateActionBar(selectedTVShowTitle, true);
        } else if (selectedSeason != -1) {
            selectedSeason = -1;
            selectedSeasonTitle = null;
            updateActionBar(selectedTVShowTitle, true);
        } else if (selectedTVShowId != -1) {
            selectedTVShowId = -1;
            selectedTVShowTitle = null;
            updateActionBar(getActionBarTitle(), false);
        }
    }

}
