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

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.ui.BaseActivity;

/**
 * Edits a host
 */
public class EditHostActivity extends BaseActivity implements
        HostFragmentManualConfiguration.HostManualConfigurationListener {

    private int hostId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_wizard);

        // Only load first fragment if we're starting the activity
        if (savedInstanceState == null) {
            HostFragmentManualConfiguration editFragment = new HostFragmentManualConfiguration();

            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                hostId = extras.getInt(HostFragmentManualConfiguration.HOST_ID);

                HostManager hostManager = HostManager.getInstance(this);
                HostInfo selectedHostInfo = null;
                for (HostInfo hostInfo : hostManager.getHosts()) {
                    if (hostInfo.getId() == hostId) {
                        selectedHostInfo = hostInfo;
                        break;
                    }
                }

                if (selectedHostInfo != null) {
                    Bundle args = new Bundle();
                    args.putString(HostFragmentManualConfiguration.HOST_NAME,
                            selectedHostInfo.getName());
                    args.putString(HostFragmentManualConfiguration.HOST_ADDRESS,
                                   selectedHostInfo.getAddress());
                    args.putInt(HostFragmentManualConfiguration.HOST_HTTP_PORT,
                                selectedHostInfo.getHttpPort());
                    args.putInt(HostFragmentManualConfiguration.HOST_TCP_PORT,
                                selectedHostInfo.getTcpPort());
                    args.putString(HostFragmentManualConfiguration.HOST_USERNAME,
                                   selectedHostInfo.getUsername());
                    args.putString(HostFragmentManualConfiguration.HOST_PASSWORD,
                                   selectedHostInfo.getPassword());
                    args.putInt(HostFragmentManualConfiguration.HOST_PROTOCOL,
                                selectedHostInfo.getProtocol());
                    args.putString(HostFragmentManualConfiguration.HOST_MAC_ADDRESS,
                                   selectedHostInfo.getMacAddress());
                    args.putInt(HostFragmentManualConfiguration.HOST_WOL_PORT,
                                selectedHostInfo.getWolPort());
                    args.putBoolean(HostFragmentManualConfiguration.HOST_USE_EVENT_SERVER,
                                    selectedHostInfo.getUseEventServer());
                    args.putInt(HostFragmentManualConfiguration.HOST_EVENT_SERVER_PORT,
                                    selectedHostInfo.getEventServerPort());
                    editFragment.setArguments(args);
                }
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, editFragment)
                    .commit();
        }
        setupActionBar();

//        // Setup system bars and content padding
//        setupSystemBarsColors();
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.fragment_container), true, true, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.edit_xbmc);
            TypedArray styledAttrs = getTheme().obtainStyledAttributes(new int[] {R.attr.iconHosts});
            actionBar.setIcon(styledAttrs.getResourceId(styledAttrs.getIndex(0), R.drawable.ic_devices_white_24dp));
            styledAttrs.recycle();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Callbacks for the fragment
     */
    public void onHostManualConfigurationNext(HostInfo hostInfo) {
        if (hostId != -1) {
            HostManager hostManager = HostManager.getInstance(this);
            HostInfo newHostInfo = hostManager.editHost(hostId, hostInfo);
            hostManager.switchHost(newHostInfo);
        }

        Intent intent = new Intent(this, HostManagerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void onHostManualConfigurationCancel() {
        finish();
    }
}
