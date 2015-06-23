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
 * Controls the presentation of Addons information (list, details)
 * All the information is presented by specific fragments
 */
public class AddonsActivity extends BaseActivity
        implements AddonListFragment.OnAddonSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(AddonsActivity.class);

    public static final String ADDONID = "addon_id";
    public static final String ADDONTITLE = "addon_title";

    private String selectedAddonId;
    private String selectedAddonTitle;

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
            AddonListFragment addonListFragment = new AddonListFragment();

            // Setup animations
            if (Utils.isLollipopOrLater()) {
                addonListFragment.setExitTransition(null);
                addonListFragment.setReenterTransition(TransitionInflater
                        .from(this)
                        .inflateTransition(android.R.transition.fade));
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, addonListFragment)
                    .commit();
        } else {
            selectedAddonId = savedInstanceState.getString(ADDONID, null);
            selectedAddonTitle = savedInstanceState.getString(ADDONTITLE, null);
        }

        setupActionBar(selectedAddonTitle);
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
        outState.putString(ADDONID, selectedAddonId);
        outState.putString(ADDONTITLE, selectedAddonTitle);
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
                // Only respond to this if we are showing the details in portrait mode,
                // which can be checked by checking if selected movie != -1, in which case we
                // should go back to the previous fragment, which is the list.
                if (selectedAddonId != null) {
                    selectedAddonId = null;
                    selectedAddonTitle = null;
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
        // If we are showing details in portrait, clear selected and show action bar
        if (selectedAddonId != null) {
            selectedAddonId = null;
            selectedAddonTitle = null;
            setupActionBar(null);
        }
        super.onBackPressed();
    }

    private void setupActionBar(String addonTitle) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (addonTitle != null) {
            navigationDrawerFragment.setDrawerIndicatorEnabled(false);
            actionBar.setTitle(addonTitle);
        } else {
            navigationDrawerFragment.setDrawerIndicatorEnabled(true);
            actionBar.setTitle(R.string.addons);
        }
    }

    /**
     * Callback from list fragment when a addon is selected.
     * Switch fragment in portrait
     * @param addonId Addon selected
     * @param addonTitle Title
     */
    @TargetApi(21)
    public void onAddonSelected(String addonId, String addonTitle) {
        selectedAddonId = addonId;
        selectedAddonTitle = addonTitle;

        // Replace list fragment
        AddonDetailsFragment addonDetailsFragment = AddonDetailsFragment.newInstance(addonId);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            addonDetailsFragment.setEnterTransition(TransitionInflater
                    .from(this)
                    .inflateTransition(R.transition.media_details));
            addonDetailsFragment.setReturnTransition(null);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                    R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, addonDetailsFragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(selectedAddonTitle);
    }
}
