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
package com.syncedsynapse.kore2.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.Utils;

/**
 * Controls the presentation of TV Shows information (list, details)
 * All the information is presented by specific fragments
 */
public class TVShowsActivity extends BaseActivity
        implements TVShowListFragment.OnTVShowSelectedListener,
        TVShowEpisodeListFragment.OnEpisodeSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(TVShowsActivity.class);

    public static final String TVSHOWID = "tvshow_id";
    public static final String TVSHOWTITLE = "tvshow_title";
    public static final String EPISODEID = "episode_id";

    private int selectedTVShowId = -1;
    private String selectedTVShowTitle = null;
    private int selectedEpisodeId = -1;

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
                tvshowListFragment.setExitTransition(null);
                tvshowListFragment.setReenterTransition(TransitionInflater
                        .from(this)
                        .inflateTransition(android.R.transition.fade));
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, tvshowListFragment)
                    .commit();
        } else {
            selectedTVShowId = savedInstanceState.getInt(TVSHOWID, -1);
            selectedTVShowTitle = savedInstanceState.getString(TVSHOWTITLE, null);
            selectedEpisodeId = savedInstanceState.getInt(EPISODEID, -1);
        }

        setupActionBar(selectedTVShowTitle);

//        // Setup system bars and content padding, allowing averlap with the bottom bar
//        setupSystemBarsColors();
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.fragment_container), true, true, true);
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.drawer_layout), true, true, true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TVSHOWID, selectedTVShowId);
        outState.putString(TVSHOWTITLE, selectedTVShowTitle);
        outState.putInt(EPISODEID, selectedEpisodeId);
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
        } else if (selectedTVShowId != -1) {
            selectedTVShowId = -1;
            selectedTVShowTitle = null;
            setupActionBar(null);
        }
        super.onBackPressed();
    }

    private void setupActionBar(String tvshowTitle) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (tvshowTitle != null) {
            navigationDrawerFragment.setDrawerIndicatorEnabled(false);
            actionBar.setTitle(tvshowTitle);
        } else {
            navigationDrawerFragment.setDrawerIndicatorEnabled(true);
            actionBar.setTitle(R.string.tv_shows);
        }
    }

    /**
     * Callback from tvshows list fragment when a show is selected.
     * Switch fragment in portrait
     * @param tvshowId Selected show
     * @param tvshowTitle Title
     */
    @TargetApi(21)
    public void onTVShowSelected(int tvshowId, String tvshowTitle) {
        selectedTVShowId = tvshowId;
        selectedTVShowTitle = tvshowTitle;

        // Replace list fragment
        TVShowDetailsFragment tvshowDetailsFragment = TVShowDetailsFragment.newInstance(tvshowId);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            tvshowDetailsFragment.setEnterTransition(TransitionInflater
                    .from(this)
                    .inflateTransition(R.transition.media_details));
            tvshowDetailsFragment.setReturnTransition(null);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                    R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, tvshowDetailsFragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(selectedTVShowTitle);
    }

    /**
     * Callback from tvshow episodes list when a episode is selected
     * @param tvshowId Show id of the episode, should be the same as {@link #selectedTVShowId}
     * @param episodeId Episode id
     */
    @TargetApi(21)
    public void onEpisodeSelected(int tvshowId, int episodeId) {
        selectedEpisodeId = episodeId;

        // Replace list fragment
        TVShowEpisodeDetailsFragment fragment =
                TVShowEpisodeDetailsFragment.newInstance(tvshowId, episodeId);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            fragment.setEnterTransition(TransitionInflater
                    .from(this)
                    .inflateTransition(R.transition.media_details));
            fragment.setReturnTransition(null);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                    R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(selectedTVShowTitle);
    }

}
