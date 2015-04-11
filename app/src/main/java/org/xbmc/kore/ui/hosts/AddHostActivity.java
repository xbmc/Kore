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
package org.xbmc.kore.ui.hosts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.RemoteActivity;

/**
 * Add host wizard.
 * Controls the wizard steps fragments.
 */
public class AddHostActivity extends BaseActivity
        implements AddHostFragmentWelcome.AddHostWelcomeListener,
        AddHostFragmentZeroconf.AddHostZeroconfListener,
        HostFragmentManualConfiguration.HostManualConfigurationListener,
        AddHostFragmentFinish.AddHostFinishListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only load first fragment if we're starting the activity
        if (savedInstanceState == null) {
            AddHostFragmentWelcome firstStep = new AddHostFragmentWelcome();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, firstStep)
                    .commit();
        }

//        setupActionBar();

//        // Setup system bars and content padding
//        setupSystemBarsColors();
//        UIUtils.setPaddingForSystemBars(this, findViewById(android.R.id.content), true, true, true);
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.default_toolbar);
        toolbar.setTitle(R.string.add_xbmc);
        setSupportActionBar(toolbar);
    }

    /**
     * Welcome fragment callbacks
     */
    public void onAddHostWelcomeNext() {
        switchToFragment(new AddHostFragmentZeroconf());
    }

    public void onAddHostWelcomeCancel() {
        finish();
    }

    /**
     * Search hosts fragment callbacks
     */
    public void onAddHostZeroconfNoHost() {
        HostFragmentManualConfiguration fragment = new HostFragmentManualConfiguration();
        Bundle args = new Bundle();
        args.putString(HostFragmentManualConfiguration.CANCEL_BUTTON_LABEL_ARG,
                getString(R.string.previous));
        fragment.setArguments(args);
        switchToFragment(fragment);
    }

    public void onAddHostZeroconfFoundHost(HostInfo hostInfo) {
        HostFragmentManualConfiguration fragment = new HostFragmentManualConfiguration();

        Bundle args = new Bundle();
        if (hostInfo != null) {
            args.putString(HostFragmentManualConfiguration.HOST_NAME,
                    hostInfo.getName());
            args.putString(HostFragmentManualConfiguration.HOST_ADDRESS,
                    hostInfo.getAddress());
            args.putInt(HostFragmentManualConfiguration.HOST_HTTP_PORT,
                    hostInfo.getHttpPort());
            args.putInt(HostFragmentManualConfiguration.HOST_TCP_PORT,
                    hostInfo.getTcpPort());
            args.putString(HostFragmentManualConfiguration.HOST_USERNAME,
                    hostInfo.getUsername());
            args.putString(HostFragmentManualConfiguration.HOST_PASSWORD,
                    hostInfo.getPassword());
            args.putInt(HostFragmentManualConfiguration.HOST_PROTOCOL,
                    hostInfo.getProtocol());
            // Ignore Mac Address and Wol Port

            // Send this fragment straight to test
            args.putBoolean(HostFragmentManualConfiguration.GO_STRAIGHT_TO_TEST, true);
            fragment.setArguments(args);
        }
        args.putString(HostFragmentManualConfiguration.CANCEL_BUTTON_LABEL_ARG,
                getString(R.string.previous));
        fragment.setArguments(args);
        switchToFragment(fragment);
    }


    /**
     * Manual configuration/review fragment callbacks
     */
    public void onHostManualConfigurationNext(HostInfo hostInfo) {
        HostManager hostManager = HostManager.getInstance(this);
        HostInfo newHostInfo = hostManager.addHost(hostInfo);
        hostManager.switchHost(newHostInfo);
        switchToFragment(new AddHostFragmentFinish());
    }

    public void onHostManualConfigurationCancel() {
        switchToFragment(new AddHostFragmentZeroconf());
    }

    /**
     * Finish fragment callbacks
     */
    public void onAddHostFinish() {
        Intent intent = new Intent(this, RemoteActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void switchToFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }
}
