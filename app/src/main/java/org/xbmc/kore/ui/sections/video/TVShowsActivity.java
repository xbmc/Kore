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

import android.annotation.TargetApi;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

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

    @TargetApi(21)
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
     * Switch fragment in portrait
     * @param vh view holder
     */
    @TargetApi(21)
    public void onTVShowSelected(TVShowListFragment.ViewHolder vh) {
        selectedTVShowId = vh.dataHolder.getId();
        selectedTVShowTitle = vh.dataHolder.getTitle();

        // Replace list fragment
        final TVShowInfoFragment tvshowDetailsFragment = new TVShowInfoFragment();
        tvshowDetailsFragment.setDataHolder(vh.dataHolder);
        showFragment(tvshowDetailsFragment, vh.artView, vh.dataHolder);
        updateActionBar(selectedTVShowTitle, true);
    }

    /**
     * Callback from tvshow details when a season is selected
     * @param tvshowId tv show id
     * @param seasonId season number
     */
    public void onSeasonSelected(int tvshowId, int seasonId) {
        selectedSeason = seasonId;

        // Replace fragment
        TVShowEpisodeListFragment fragment =
                TVShowEpisodeListFragment.newInstance(selectedTVShowId, seasonId);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_details_enter, 0, R.anim.fragment_list_popenter, 0)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        selectedSeasonTitle = String.format(getString(R.string.season_number), seasonId);
        updateActionBar(selectedSeasonTitle, true);
    }

    /**
     * Callback from tvshow details when a episode is selected
     */
    @TargetApi(21)
    public void onNextEpisodeSelected(int tvshowId,
                                      AbstractInfoFragment.DataHolder dh) {
        selectedEpisodeId = dh.getId();

        // Replace list fragment
        TVShowEpisodeInfoFragment fragment = new TVShowEpisodeInfoFragment();
        fragment.setDataHolder(dh);
        fragment.setTvshowId(tvshowId);
        startFragment(fragment);
        updateActionBar(selectedTVShowTitle, true);
    }

    /**
     * Callback from tvshow episodes list when a episode is selected
     */
    @TargetApi(21)
    public void onEpisodeSelected(int tvshowId,
                                  TVShowEpisodeListFragment.ViewHolder viewHolder) {
        selectedEpisodeId = viewHolder.dataHolder.getId();
        TVShowEpisodeInfoFragment fragment = new TVShowEpisodeInfoFragment();
        fragment.setDataHolder(viewHolder.dataHolder);
        fragment.setTvshowId(tvshowId);
        startFragment(fragment);
        updateActionBar(selectedTVShowTitle, true);
    }

    @TargetApi(21)
    private void startFragment(AbstractFragment fragment) {
        // Replace list fragment
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            fragment.setEnterTransition(
                    TransitionInflater.from(this).inflateTransition(R.transition.media_details));
            fragment.setReturnTransition(null);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0, R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, fragment)
                 .addToBackStack(null)
                 .commit();
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
