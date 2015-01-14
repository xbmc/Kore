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
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostConnectionObserver;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.method.*;
import com.syncedsynapse.kore2.jsonrpc.method.System;
import com.syncedsynapse.kore2.jsonrpc.type.ListType;
import com.syncedsynapse.kore2.jsonrpc.type.PlayerType;
import com.syncedsynapse.kore2.ui.hosts.AddHostActivity;
import com.syncedsynapse.kore2.ui.views.CirclePageIndicator;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.TabsAdapter;
import com.syncedsynapse.kore2.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class RemoteActivity extends HostConnectionActivity
        implements HostConnectionObserver.PlayerEventsObserver,
        NowPlayingFragment.NowPlayingListener,
        SendTextDialogFragment.SendTextDialogListener {
	private static final String TAG = LogUtils.makeLogTag(RemoteActivity.class);

    /**
     * Host manager singleton
     */
    private HostManager hostManager = null;

    /**
     * To register for observing host events
     */
    private HostConnectionObserver hostConnectionObserver;

    private NavigationDrawerFragment navigationDrawerFragment;

    @InjectView(R.id.background_image) ImageView backgroundImage;
    @InjectView(R.id.pager_indicator) CirclePageIndicator pageIndicator;
    @InjectView(R.id.pager) ViewPager viewPager;
    @InjectView(R.id.default_toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set default values for the preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_remote);
        ButterKnife.inject(this);

        hostManager = HostManager.getInstance(this);

        // Check if we have any hosts setup
        if (hostManager.getHostInfo() == null) {
            final Intent intent = new Intent(this, AddHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set up pager and fragments
        TabsAdapter tabsAdapter = new TabsAdapter(this, getSupportFragmentManager())
                .addTab(NowPlayingFragment.class, null, R.string.now_playing, 1)
                .addTab(RemoteFragment.class, null, R.string.remote, 2)
                .addTab(PlaylistFragment.class, null, R.string.playlist, 3);

        viewPager.setAdapter(tabsAdapter);
        pageIndicator.setViewPager(viewPager);
        pageIndicator.setOnPageChangeListener(defaultOnPageChangeListener);

        viewPager.setCurrentItem(1);
        viewPager.setOffscreenPageLimit(2);

        setupActionBar();

//        // Setup system bars and content padding
//        setupSystemBarsColors();
//        // Set the padding of views.
//        // Only set top and right, to allow bottom to overlap in each fragment
//        UIUtils.setPaddingForSystemBars(this, viewPager, true, true, false);
//        UIUtils.setPaddingForSystemBars(this, pageIndicator, true, true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver = hostManager.getHostConnectionObserver();
        hostConnectionObserver.registerPlayerObserver(this);
        // Get last result
        hostConnectionObserver.replyWithLastResult(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        hostConnectionObserver.unregisterPlayerObserver(this);
        hostConnectionObserver = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		if (!navigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen if the drawer is not showing.
			// Otherwise, let the drawer decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.remote, menu);
		}
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
		switch (item.getItemId()) {
            case R.id.action_wake_up:
                UIUtils.sendWolAsync(this, hostManager.getHostInfo());
                return true;
            case R.id.action_quit:
                Application.Quit actionQuit = new Application.Quit();
                // Fire and forget
                actionQuit.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.action_suspend:
                System.Suspend actionSuspend = new System.Suspend();
                // Fire and forget
                actionSuspend.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.action_shutdown:
                System.Shutdown actionShutdown = new System.Shutdown();
                // Fire and forget
                actionShutdown.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.send_text:
                SendTextDialogFragment dialog =
                        SendTextDialogFragment.newInstance(getString(R.string.send_text));
                dialog.show(getSupportFragmentManager(), null);
                return true;
			default:
				break;
		}

		return super.onOptionsItemSelected(item);
    }

    /**
     * Callbacks from Send text dialog
     */
    public void onSendTextFinished(String text, boolean done) {
        Input.SendText action = new Input.SendText(text, done);
        action.execute(hostManager.getConnection(), null, null);
    }

    public void onSendTextCancel() {
        // Nothing to do
    }


    private void setupActionBar() {
        setToolbarTitle(toolbar, 1);
        setSupportActionBar(toolbar);
    }

    private void setToolbarTitle(Toolbar toolbar, int position) {
        if (toolbar != null) {
            switch (position) {
                case 0:
                    toolbar.setTitle(R.string.now_playing);
                    break;
                case 1:
                    toolbar.setTitle(R.string.remote);
                    break;
                case 2:
                    toolbar.setTitle(R.string.playlist);
                    break;
            }
        }
    }

    // Default page change listener, that doesn't scroll images
    ViewPager.OnPageChangeListener defaultOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

        @Override
        public void onPageSelected(int position) {
            setToolbarTitle(toolbar, position);
        }

        @Override
        public void onPageScrollStateChanged(int state) { }
    };

    /**
     * Sets or clear the image background
     * @param url
     */
    private void setImageViewBackground(String url) {
        if (url != null) {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);

            UIUtils.loadImageIntoImageview(hostManager, url, backgroundImage,
                    displaySize.x, displaySize.y / 2);

            final int pixelsPerPage = displaySize.x / 4;

            backgroundImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    backgroundImage.getViewTreeObserver().removeOnPreDrawListener(this);
                    // Position the image
                    int offsetX =  (viewPager.getCurrentItem() - 1) * pixelsPerPage;
                    backgroundImage.scrollTo(offsetX, 0);

                    pageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                            int offsetX = (int) ((position - 1 + positionOffset) * pixelsPerPage);
                            backgroundImage.scrollTo(offsetX, 0);
                        }

                        @Override
                        public void onPageSelected(int position) {
                            setToolbarTitle(toolbar, position);
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) { }
                    });

                    return true;
                }
            });
        } else {
            backgroundImage.setImageDrawable(null);
            pageIndicator.setOnPageChangeListener(defaultOnPageChangeListener);
        }
    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    private String lastImageUrl = null;
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        String imageUrl = (TextUtils.isEmpty(getItemResult.fanart)) ?
                getItemResult.thumbnail : getItemResult.fanart;
        if ((imageUrl != null) && !imageUrl.equals(lastImageUrl)) {
            setImageViewBackground(imageUrl);
        }
        lastImageUrl = imageUrl;
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        playerOnPlay(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnStop() {
        if (lastImageUrl != null) {
            setImageViewBackground(null);
        }
        lastImageUrl = null;
    }

    public void playerNoResultsYet() {
        // Do nothing
    }

    public void playerOnConnectionError(int errorCode, String description) {
        playerOnStop();
    }

    public void systemOnQuit() {
        Toast.makeText(this, R.string.xbmc_quit, Toast.LENGTH_SHORT).show();
        playerOnStop();
    }

    public void inputOnInputRequested(String title, String type, String value) {
        SendTextDialogFragment dialog =
                SendTextDialogFragment.newInstance(title);
        dialog.show(getSupportFragmentManager(), null);
    }

    /**
     * Now playing fragment listener
     */
    public void SwitchToRemotePanel() {
        viewPager.setCurrentItem(1);
    }
}
