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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.Settings;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.UIUtils;

/**
 * Presents the Preferences fragment
 */
public class SettingsActivity extends ActionBarActivity{
    private static final String TAG = LogUtils.makeLogTag(SettingsActivity.class);

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(UIUtils.getThemeResourceId(
                prefs.getString(Settings.KEY_PREF_THEME,
                        Settings.DEFAULT_PREF_THEME)));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // Setup action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.settings);

        // Display the fragment as the main content.
        settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, settingsFragment)
                            .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This is kind of an Hack...
     * The settings fragment launches the purchase workflow, which calls
     * startIntentSenderForResult on this activity, which, when finished calls
     * this onActivityResult.
     * Wee need to pass this to the fragment, so it can update itself
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the fragment
        if (!settingsFragment.onPurchaseWorkflowFinish(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            LogUtils.LOGD(TAG, "onActivityResult handled by IABUtil.");
        }
    }
}
