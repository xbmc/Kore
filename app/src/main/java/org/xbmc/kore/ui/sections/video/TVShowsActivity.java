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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * Controls the presentation of TV Shows information (list, details)
 * All the information is presented by specific fragments
 */
public class TVShowsActivity extends BaseActivity
        implements TVShowListFragment.OnTVShowSelectedListener,
                   TVShowDetailsFragment.TVShowDetailsActionListener,
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

    private boolean clearSharedElements;

    private NavigationDrawerFragment navigationDrawerFragment;

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Request transitions on lollipop
        if (Utils.isLollipopOrLater()) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_media);

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        if (savedInstanceState == null) {
            TVShowListFragment tvshowListFragment = new TVShowListFragment();

            // Setup animations
            if (Utils.isLollipopOrLater()) {
                //Fade added to prevent shared element from disappearing very shortly at the start of the transition.
                Transition fade = TransitionInflater
                        .from(this)
                        .inflateTransition(android.R.transition.fade);
                tvshowListFragment.setExitTransition(fade);
                tvshowListFragment.setReenterTransition(fade);

                tvshowListFragment.setSharedElementReturnTransition(TransitionInflater.from(
                        this).inflateTransition(R.transition.change_image));

                android.support.v4.app.SharedElementCallback seCallback = new android.support.v4.app.SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        if (clearSharedElements) {
                            names.clear();
                            sharedElements.clear();
                            clearSharedElements = false;
                        }
                    }
                };
                tvshowListFragment.setExitSharedElementCallback(seCallback);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, tvshowListFragment)
                    .commit();
        } else {
            selectedTVShowId = savedInstanceState.getInt(TVSHOWID, -1);
            selectedTVShowTitle = savedInstanceState.getString(TVSHOWTITLE, null);
            selectedEpisodeId = savedInstanceState.getInt(EPISODEID, -1);
            selectedSeason = savedInstanceState.getInt(SEASON, -1);
            selectedSeasonTitle = savedInstanceState.getString(SEASONTITLE, null);
        }

        setupActionBar(selectedTVShowTitle);

//        // Setup system bars and content padding, allowing averlap with the bottom bar
//        setupSystemBarsColors();
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.fragment_container), true, true, true);
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.drawer_layout), true, true, true);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TVSHOWID, selectedTVShowId);
        outState.putString(TVSHOWTITLE, selectedTVShowTitle);
        outState.putInt(EPISODEID, selectedEpisodeId);
        outState.putInt(SEASON, selectedSeason);
        outState.putString(SEASONTITLE, selectedSeasonTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!navigationDrawerFragment.isDrawerOpen()) {
//            getMenuInflater().inflate(R.menu.media_info, menu);
//        }
        getMenuInflater().inflate(R.menu.media_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_remote:
                // Starts remote
                Intent launchIntent = new Intent(this, RemoteActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(launchIntent);
                return true;
            case android.R.id.home:
                // Only respond to this if we are showing the episodeor show details in portrait
                // mode, which can be checked by checking if selected movie != -1, in which case we
                // should go back to the previous fragment, which is the list.
                // The default behaviour is handled by the nav drawer (open/close)
                if (selectedEpisodeId != -1) {
                    selectedEpisodeId = -1;
                    getSupportFragmentManager().popBackStack();
                    if (selectedSeason != -1)
                        setupActionBar(selectedSeasonTitle);
                    else
                        setupActionBar(selectedTVShowTitle);
                    return true;
                } else if (selectedSeason != -1) {
                    selectedSeason = -1;
                    getSupportFragmentManager().popBackStack();
                    setupActionBar(selectedTVShowTitle);
                    return true;
                } else if (selectedTVShowId != -1) {
                    selectedTVShowId = -1;
                    selectedTVShowTitle = null;
                    setupActionBar(null);
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
        // If we are showing episode or show details in portrait, clear selected and show action bar
        if (selectedEpisodeId != -1) {
            selectedEpisodeId = -1;
            if (selectedSeason != -1)
                setupActionBar(selectedSeasonTitle);
            else
                setupActionBar(selectedTVShowTitle);
        } else if (selectedSeason != -1) {
            selectedSeason = -1;
            setupActionBar(selectedTVShowTitle);
        } else if (selectedTVShowId != -1) {
            selectedTVShowId = -1;
            selectedTVShowTitle = null;
            setupActionBar(null);
        }
        super.onBackPressed();
    }

    private boolean drawerIndicatorIsArrow = false;
    private void setupActionBar(String title) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (title != null) {
            if (!drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(true);
                drawerIndicatorIsArrow = true;
            }
            actionBar.setTitle(title);
        } else {
            if (drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(false);
                drawerIndicatorIsArrow = false;
            }
            actionBar.setTitle(R.string.tv_shows);
        }
    }

    /**
     * Callback from tvshows list fragment when a show is selected.
     * Switch fragment in portrait
     * @param vh view holder
     */
    @TargetApi(21)
    public void onTVShowSelected(TVShowListFragment.ViewHolder vh) {
        selectedTVShowId = vh.tvshowId;
        selectedTVShowTitle = vh.tvshowTitle;

        // Replace list fragment
        final TVShowDetailsFragment tvshowDetailsFragment = TVShowDetailsFragment.newInstance(vh);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            android.support.v4.app.SharedElementCallback seCallback = new android.support.v4.app.SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    //On returning onMapSharedElements for the exiting fragment is called before the onMapSharedElements
                    // for the reentering fragment. We use this to determine if we are returning and if
                    // we should clear the shared element lists. Note that, clearing must be done in the reentering fragment
                    // as this is called last. Otherwise it the app will crash during transition setup. Not sure, but might
                    // be a v4 support package bug.
                    if (tvshowDetailsFragment.isVisible()) {
                        View sharedView = tvshowDetailsFragment.getSharedElement();
                        if (sharedView == null) { // shared element not visible
                            clearSharedElements = true;
                        }
                    }
                }
            };
            tvshowDetailsFragment.setEnterSharedElementCallback(seCallback);

            tvshowDetailsFragment.setEnterTransition(
                    TransitionInflater.from(this)
                                      .inflateTransition(R.transition.media_details));
            tvshowDetailsFragment.setReturnTransition(null);
            Transition changeImageTransition =
                    TransitionInflater.from(this)
                                      .inflateTransition(R.transition.change_image);
            tvshowDetailsFragment.setSharedElementReturnTransition(changeImageTransition);
            tvshowDetailsFragment.setSharedElementEnterTransition(changeImageTransition);
            fragTrans.addSharedElement(vh.artView, vh.artView.getTransitionName());
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0, R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, tvshowDetailsFragment)
                 .addToBackStack(null)
                 .commit();
        setupActionBar(selectedTVShowTitle);
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
        setupActionBar(selectedSeasonTitle);
    }

    /**
     * Callback from tvshow details when a episode is selected
     * @param episodeId episode id
     */
    @TargetApi(21)
    public void onNextEpisodeSelected(int episodeId) {
        selectedEpisodeId = episodeId;

        // Replace list fragment
        TVShowEpisodeDetailsFragment fragment =
                TVShowEpisodeDetailsFragment.newInstance(selectedTVShowId, selectedEpisodeId);
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
        setupActionBar(selectedTVShowTitle);
    }

    /**
     * Callback from tvshow episodes list when a episode is selected
     * @param vh view holder
     */
    @TargetApi(21)
    public void onEpisodeSelected(TVShowEpisodeListFragment.EpisodeViewHolder vh) {
        selectedEpisodeId = vh.episodeId;

        // Replace list fragment
        TVShowEpisodeDetailsFragment fragment =
                TVShowEpisodeDetailsFragment.newInstance(selectedTVShowId, selectedEpisodeId);
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
        setupActionBar(selectedTVShowTitle);
    }
}
