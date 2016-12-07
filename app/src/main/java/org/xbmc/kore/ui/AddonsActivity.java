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
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

import java.util.List;
import java.util.Map;

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

    private boolean clearSharedElements;

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
            AddonListContainerFragment addonListFragment = new AddonListContainerFragment();

            // Setup animations
            if (Utils.isLollipopOrLater()) {
                //Fade added to prevent shared element from disappearing very shortly at the start of the transition.
                Transition fade = TransitionInflater
                        .from(this)
                        .inflateTransition(android.R.transition.fade);
                addonListFragment.setExitTransition(fade);
                addonListFragment.setReenterTransition(fade);
                addonListFragment.setSharedElementReturnTransition(TransitionInflater.from(
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
                addonListFragment.setExitSharedElementCallback(seCallback);
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

    private boolean drawerIndicatorIsArrow = false;
    private void setupActionBar(String addonTitle) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (addonTitle != null) {
            if (!drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(true);
                drawerIndicatorIsArrow = true;
            }
            actionBar.setTitle(addonTitle);
        } else {
            if (drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(false);
                drawerIndicatorIsArrow = false;
            }
            actionBar.setTitle(R.string.addons);
        }
    }

    /**
     * Callback from list fragment when a addon is selected.
     * Switch fragment in portrait
     * @param vh
     */
    @TargetApi(21)
    public void onAddonSelected(AddonListFragment.ViewHolder vh) {
        selectedAddonId = vh.addonId;
        selectedAddonTitle = vh.addonName;

        // Replace list fragment
        final SharedElementFragment addonDetailsFragment =
            vh.browsable
            ? AddonOverviewFragment.newInstance(vh)
            : AddonDetailsFragment.newInstance(vh)
            ;
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
                    if (addonDetailsFragment.isVisible()) {
                        View sharedView = addonDetailsFragment.getSharedElement();
                        if (sharedView == null) { // shared element not visible
                            clearSharedElements = true;
                        }
                    }
                }
            };
            addonDetailsFragment.setEnterSharedElementCallback(seCallback);

            addonDetailsFragment.setEnterTransition(TransitionInflater
                                                            .from(this)
                                                            .inflateTransition(R.transition.media_details));
            addonDetailsFragment.setReturnTransition(null);

            Transition changeImageTransition = TransitionInflater.from(
                    this).inflateTransition(R.transition.change_image);
            addonDetailsFragment.setSharedElementReturnTransition(changeImageTransition);
            addonDetailsFragment.setSharedElementEnterTransition(changeImageTransition);

            fragTrans.addSharedElement(vh.artView, vh.artView.getTransitionName());
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
