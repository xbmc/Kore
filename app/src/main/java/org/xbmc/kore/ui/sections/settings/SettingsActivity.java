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
package org.xbmc.kore.ui.sections.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.utils.LogUtils;

/**
 * Presents the Preferences fragment
 */
public class SettingsActivity extends BaseActivity {
    private static final String TAG = LogUtils.makeLogTag(SettingsActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // Setup action bar
        Toolbar toolbar = findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.settings);

        // Display the fragment as the main content.
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag("settings-fragment") == null) {
            fm.beginTransaction()
              .replace(R.id.fragment_container, SettingsFragment.class, null, "settings-fragment")
              .setReorderingAllowed(true)
              .commit();
        }
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
}
