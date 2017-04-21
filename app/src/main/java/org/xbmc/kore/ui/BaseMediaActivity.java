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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedElementTransition;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

public abstract class BaseMediaActivity extends AppCompatActivity {
    private static final String TAG = LogUtils.makeLogTag(BaseMediaActivity.class);

    private static final String NAVICON_ISARROW = "navstate";
    private static final String ACTIONBAR_TITLE = "actionbartitle";

    private NavigationDrawerFragment navigationDrawerFragment;
    private SharedElementTransition sharedElementTransition = new SharedElementTransition();
    private boolean drawerIndicatorIsArrow;

    protected abstract String getActionBarTitle();
    protected abstract Fragment createFragment();

    @Override
    @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        // Request transitions on lollipop
        if (Utils.isLollipopOrLater()) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(UIUtils.getThemeResourceId(
                prefs.getString(Settings.KEY_PREF_THEME, Settings.DEFAULT_PREF_THEME)));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_generic_media);

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            if (savedInstanceState != null) {
                updateActionBar(savedInstanceState.getString(ACTIONBAR_TITLE),
                                savedInstanceState.getBoolean(NAVICON_ISARROW));
            } else {
                updateActionBar(getActionBarTitle(), false);
            }
        }

        String fragmentTitle = getActionBarTitle();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = createFragment();

            if (Utils.isLollipopOrLater()) {
                fragment.setExitTransition(null);
                fragment.setReenterTransition(TransitionInflater
                                                      .from(this)
                                                      .inflateTransition(android.R.transition.fade));
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, fragmentTitle)
                    .commit();
        }

        if (Utils.isLollipopOrLater()) {
            sharedElementTransition.setupExitTransition(this, fragment);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(NAVICON_ISARROW, drawerIndicatorIsArrow);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            CharSequence title = actionBar.getTitle();
            if (title != null) {
                outState.putString(ACTIONBAR_TITLE, title.toString());
            }
        }
    }

    public boolean getDrawerIndicatorIsArrow() {
        return drawerIndicatorIsArrow;
    }

    /**
     * Sets the title and drawer indicator of the toolbar
     * @param title
     * @param showArrowIndicator true if the toolbar should show the back arrow indicator,
     *                               false if it should show the drawer icon
     */
    protected void updateActionBar(String title, boolean showArrowIndicator) {
        if (showArrowIndicator != drawerIndicatorIsArrow) {
            navigationDrawerFragment.animateDrawerToggle(showArrowIndicator);
            drawerIndicatorIsArrow = showArrowIndicator;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(title);
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
                Intent launchIntent = new Intent(this, RemoteActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(launchIntent);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(21)
    protected void showFragment(AbstractFragment fragment, ImageView sharedImageView, AbstractFragment.DataHolder dataHolder) {
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            dataHolder.setPosterTransitionName(sharedImageView.getTransitionName());
            sharedElementTransition.setupEnterTransition(this, fragTrans, fragment, sharedImageView);
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                                          R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, fragment, getActionBarTitle())
                 .addToBackStack(null)
                 .commit();

        dataHolder.getBundle().putBoolean(NAVICON_ISARROW, true);
    }
}
