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
package org.xbmc.kore.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Controls the presentation of Live TV/Radio information (list, details)
 * All the information is presented by specific fragments
 */
public class PVRActivity extends BaseActivity
        implements PVRListFragment.OnPVRSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(PVRActivity.class);

    public static final String CHANNELGROUPID = "channel_group_id";
    public static final String CHANNELGROUPTITLE = "channel_group_title";
    public static final String RETURNTOCHANNELGROUP = "return_to_channel_group";

    public static final String CHANNELID = "channel_id";
    public static final String CHANNELTITLE = "channel_title";

    private int selectedChannelGroupId = -1;
    private String selectedChannelGroupTitle = null;
    private boolean returnToChannelGroupList = true;

    private int selectedChannelId = -1;
    private String selectedChannelTitle = null;
    private String selectedChannelType;

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
        navigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        if (savedInstanceState == null) {
            PVRListFragment pvrListFragment = new PVRListFragment();

            // Setup animations
            if (Utils.isLollipopOrLater()) {
                pvrListFragment.setExitTransition(null);
                pvrListFragment.setReenterTransition(TransitionInflater
                        .from(this)
                        .inflateTransition(android.R.transition.fade));
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, pvrListFragment)
                    .commit();
        } else {
            selectedChannelGroupId = savedInstanceState.getInt(CHANNELGROUPID, -1);
            selectedChannelGroupTitle = savedInstanceState.getString(CHANNELGROUPTITLE, null);
            returnToChannelGroupList = savedInstanceState.getBoolean(RETURNTOCHANNELGROUP, true);

            selectedChannelId = savedInstanceState.getInt(CHANNELID, -1);
            selectedChannelTitle = savedInstanceState.getString(CHANNELTITLE, null);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        selectedChannelType = preferences.getString(Settings.KEY_PREF_PVR_LIST_CHANNEL_TYPE, Settings.DEFAULT_PREF_PVR_LIST_CHANNEL_TYPE);

        setupActionBar(selectedChannelType, selectedChannelGroupTitle);
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
        outState.putInt(CHANNELGROUPID, selectedChannelGroupId);
        outState.putString(CHANNELGROUPTITLE, selectedChannelGroupTitle);
        outState.putBoolean(RETURNTOCHANNELGROUP, returnToChannelGroupList);

        outState.putInt(CHANNELID, selectedChannelId);
        outState.putString(CHANNELTITLE, selectedChannelTitle);
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
                // If showing detail view, back up to list
                if (selectedChannelId != -1) {
                    selectedChannelId = -1;
                    selectedChannelTitle = null;
                    setupActionBar(selectedChannelType, null);
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else if (returnToChannelGroupList && (selectedChannelGroupId != -1)) {
                    onBackPressed();
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
        // If we are showing details in portrait, clear selected and show action bar
        if (selectedChannelId != -1) {
            selectedChannelId = -1;
            selectedChannelTitle = null;
            setupActionBar(selectedChannelType, null);
        } else if ((selectedChannelGroupId != -1) && returnToChannelGroupList) {
            selectedChannelGroupId = -1;
            selectedChannelGroupTitle = null;
            setupActionBar(selectedChannelType, null);
            Fragment listFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if ((listFragment != null) &&
                (listFragment instanceof PVRListFragment)) {
                ((PVRListFragment)listFragment).onBackPressed();
            }
            return;
        }
        super.onBackPressed();
    }

    private void setupActionBar(String channelType, String channelTitle) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);

        String title;
        boolean drawerIndicatorEnabled;
        if (channelTitle != null) {
            drawerIndicatorEnabled = !returnToChannelGroupList;
            title = String.format("%s - %s", PVRListFragment.getChannelTypeTitle(this, channelType), channelTitle);
        } else {
            drawerIndicatorEnabled = true;
            title = PVRListFragment.getChannelTypeTitle(this, channelType);
        }
        navigationDrawerFragment.setDrawerIndicatorEnabled(drawerIndicatorEnabled);
        actionBar.setTitle(title);
    }

    /**
     * Callback from list fragment when a channel group is selected.
     * Setup action bar
     * @param channelGroupId Channel group selected
     * @param channelGroupTitle Title
     */
    public void onChannelGroupSelected(int channelGroupId, String channelGroupTitle, boolean canReturnToChannelGroupList) {
        selectedChannelGroupId = channelGroupId;
        selectedChannelGroupTitle = channelGroupTitle;
        this.returnToChannelGroupList = canReturnToChannelGroupList;

        setupActionBar(selectedChannelType, selectedChannelGroupTitle);
    }

    /**
     * Callback from list fragment when the channel guide should be displayed.
     * Setup action bar and repolace list fragment
     * @param channelId Channel selected
     * @param channelTitle Title
     */
    @TargetApi(21)
    public void onChannelGuideSelected(int channelId, String channelTitle) {
        selectedChannelId = channelId;
        selectedChannelTitle = channelTitle;

//        // Replace list fragment
//        PVRDetailsFragment pvrDetailsFragment = PVRDetailsFragment.newInstance(channelId);
//        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();
//
//        // Set up transitions
//        if (Utils.isLollipopOrLater()) {
//            pvrDetailsFragment.setEnterTransition(TransitionInflater.from(this)
//                                                          .inflateTransition(R.transition.media_details));
//            pvrDetailsFragment.setReturnTransition(null);
//        } else {
//            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
//                                                 R.anim.fragment_list_popenter, 0);
//        }
//
//        fragTrans.replace(R.id.fragment_container, pvrDetailsFragment)
//                .addToBackStack(null)
//                .commit();
        setupActionBar(selectedChannelType, selectedChannelTitle);
    }

    /**
     * Callback from list fragment when the channel type is changed
     * @param channelType Channel type selected
     */
    public void onChannelTypeSelected(String channelType) {
        selectedChannelType = channelType;
        setupActionBar(selectedChannelType, selectedChannelTitle);
    }
}
