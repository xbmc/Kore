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
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Controls the presentation of Live TV/Radio and recordings information (list, details)
 * All the information is presented by specific fragments
 */
public class PVRActivity extends BaseActivity
        implements PVRChannelsListFragment.OnPVRChannelSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(PVRActivity.class);

    public static final String CHANNELID = "channel_id";
    public static final String CHANNELTITLE = "channel_title";

    public static final String CHANNELGROUPID = "channelgroupid";
    public static final String CHANNELGROUPTITLE = "channelgrouptitle";

    private static final String LISTFRAGMENTTAG = "listfragmenttag";

    private int selectedChannelId = -1;
    private String selectedChannelTitle = null;

    private int selectedChannelGroupId = -1;
    private String selectedChannelGroupTitle = null;

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
                    .add(R.id.fragment_container, pvrListFragment, LISTFRAGMENTTAG)
                    .commit();
        } else {
            selectedChannelId = savedInstanceState.getInt(CHANNELID, -1);
            selectedChannelTitle = savedInstanceState.getString(CHANNELTITLE, null);

            selectedChannelGroupId = savedInstanceState.getInt(CHANNELGROUPID, -1);
            selectedChannelGroupTitle = savedInstanceState.getString(CHANNELGROUPTITLE, null);
        }

        setupActionBar(selectedChannelGroupTitle, selectedChannelTitle);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CHANNELID, selectedChannelId);
        outState.putString(CHANNELTITLE, selectedChannelTitle);

        outState.putInt(CHANNELGROUPID, selectedChannelGroupId);
        outState.putString(CHANNELGROUPTITLE, selectedChannelGroupTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                    setupActionBar(selectedChannelGroupTitle, null);
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else if (selectedChannelGroupId != -1) {
                    selectedChannelGroupId = -1;
                    selectedChannelGroupTitle = null;
                    setupActionBar(null, null);

                    PVRListFragment fragment = (PVRListFragment)getSupportFragmentManager().findFragmentByTag(LISTFRAGMENTTAG);
                    if (fragment != null) {
                        fragment.onBackPressed();
                    }
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
        boolean handled = false;
        // If we are showing details in portrait, clear selected and show action bar
        if (selectedChannelId != -1) {
            selectedChannelId = -1;
            selectedChannelTitle = null;
            setupActionBar(selectedChannelGroupTitle, null);
        } else {
            if (selectedChannelGroupId != -1) {
                selectedChannelGroupId = -1;
                selectedChannelGroupTitle = null;
                setupActionBar(null, null);
            }
            PVRListFragment fragment = (PVRListFragment)getSupportFragmentManager().findFragmentByTag(LISTFRAGMENTTAG);
            if (fragment != null) {
                handled = fragment.onBackPressed();
            }
        }
        if (!handled)
            super.onBackPressed();
    }

    private boolean drawerIndicatorIsArrow = false;
    private void setupActionBar(String channelGroupTitle, String channelTitle) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);

        if ((channelTitle != null) || (channelGroupTitle != null)) {
            if (!drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(true);
                drawerIndicatorIsArrow = true;
            }
            actionBar.setTitle(channelTitle == null? channelGroupTitle : channelTitle);
        } else {
            if (drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(false);
                drawerIndicatorIsArrow = false;
            }
            actionBar.setTitle(R.string.pvr);
        }
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

        // Replace list fragment
        PVRChannelEPGListFragment pvrEPGFragment = PVRChannelEPGListFragment.newInstance(channelId);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            pvrEPGFragment.setEnterTransition(
                    TransitionInflater.from(this)
                                      .inflateTransition(R.transition.media_details));
            pvrEPGFragment.setReturnTransition(null);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                                          R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, pvrEPGFragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(selectedChannelGroupTitle, selectedChannelTitle);
    }

    /**
     * Callback from list fragment when a channel group is selected
     * Just setup action bar
     * @param channelGroupId Channel group selected
     * @param channelGroupTitle Title
     */
    public void onChannelGroupSelected(int channelGroupId, String channelGroupTitle) {
        selectedChannelGroupId = channelGroupId;
        selectedChannelGroupTitle = channelGroupTitle;

        setupActionBar(selectedChannelGroupTitle, selectedChannelTitle);
    }
}
