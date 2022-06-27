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
package org.xbmc.kore.ui.sections.hosts;


import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.utils.LogUtils;

/**
 * Activity that presents the list of registered hosts, allowing the user to change, edit or
 * delete hosts. Also launches add host wizard activity.
 */
public class HostManagerActivity extends BaseActivity {
    private static final String TAG = LogUtils.makeLogTag(HostManagerActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_manager);

        // Set up the drawer.
        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        if (navigationDrawerFragment != null)
            navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        // Action bar
        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        toolbar.setTitle(R.string.xbmc_media_center);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
