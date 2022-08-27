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
package org.xbmc.kore.ui.sections.remote;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.ActivityRemoteBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.System;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.service.MediaSessionService;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.ui.generic.SendTextDialogFragment;
import org.xbmc.kore.ui.generic.VolumeControllerDialogFragmentListener;
import org.xbmc.kore.ui.sections.hosts.AddHostActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;
import org.xbmc.kore.utils.UIUtils;

public class RemoteActivity extends BaseActivity
        implements HostConnectionObserver.PlayerEventsObserver,
        NowPlayingFragment.NowPlayingListener,
        SendTextDialogFragment.SendTextDialogListener {
    private static final String TAG = LogUtils.makeLogTag(RemoteActivity.class);

    private static final int NOWPLAYING_FRAGMENT_ID = 1;
    private static final int REMOTE_FRAGMENT_ID = 2;
    private static final int PLAYLIST_FRAGMENT_ID = 3;

    /**
     * Host manager singleton
     */
    private HostManager hostManager = null;

    /**
     * To register for observing host events
     */
    private HostConnectionObserver hostConnectionObserver;

    private NavigationDrawerFragment navigationDrawerFragment;

    private ActivityRemoteBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       // Set default values for the preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        binding = ActivityRemoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hostManager = HostManager.getInstance(this);

        // Check if we have any hosts setup
        if (hostManager.getHostInfo() == null) {
            final Intent intent = new Intent(this, AddHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

       // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        if (navigationDrawerFragment != null)
            navigationDrawerFragment.setUp(R.id.navigation_drawer, findViewById(R.id.drawer_layout));

        // Set up pager and fragments
        TabsAdapter tabsAdapter = new TabsAdapter(this)
                .addTab(NowPlayingFragment.class, null, R.string.now_playing, NOWPLAYING_FRAGMENT_ID)
                .addTab(RemoteFragment.class, null, R.string.remote, REMOTE_FRAGMENT_ID)
                .addTab(PlaylistFragment.class, null, R.string.playlist, PLAYLIST_FRAGMENT_ID);

        binding.pager.setAdapter(tabsAdapter);
        binding.pager.setCurrentItem(1, false);
        binding.pager.setOffscreenPageLimit(2);
        binding.pager.registerOnPageChangeCallback(defaultOnPageChangeCallback);
        binding.pagerIndicator.setViewPager(binding.pager);

        setupActionBar();

        // Periodic Check of Kodi version
        hostManager.checkAndUpdateKodiVersion();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver = hostManager.getHostConnectionObserver();
        hostConnectionObserver.registerPlayerObserver(this);
        // Force a refresh, specifically to update the time elapsed on the fragments
        hostConnectionObserver.refreshWhatsPlaying();
        hostConnectionObserver.refreshPlaylists();

        // Check whether we should keep the remote activity above the lockscreen
        boolean keepAboveLockscreen = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getBoolean(Settings.KEY_PREF_KEEP_REMOTE_ABOVE_LOCKSCREEN,
                    Settings.DEFAULT_KEY_PREF_KEEP_REMOTE_ABOVE_LOCKSCREEN);
        if (keepAboveLockscreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        // Check whether we should keep the screen on
        boolean keepScreenOn = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_KEEP_SCREEN_ON,
                            Settings.DEFAULT_KEY_PREF_KEEP_SCREEN_ON);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hostConnectionObserver != null) hostConnectionObserver.unregisterPlayerObserver(this);
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
        int itemId = item.getItemId();
        if (itemId == R.id.action_wake_up) {
            UIUtils.sendWolAsync(this, hostManager.getHostInfo());
            return true;
        } else if (itemId == R.id.action_quit) {
            Application.Quit actionQuit = new Application.Quit();
            // Fire and forget
            actionQuit.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.action_suspend) {
            System.Suspend actionSuspend = new System.Suspend();
            // Fire and forget
            actionSuspend.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.action_reboot) {
            System.Reboot actionReboot = new System.Reboot();
            // Fire and forget
            actionReboot.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.action_shutdown) {
            System.Shutdown actionShutdown = new System.Shutdown();
            // Fire and forget
            actionShutdown.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.send_text) {
            SendTextDialogFragment dialog =
                    SendTextDialogFragment.newInstance(getString(R.string.send_text));
            dialog.show(getSupportFragmentManager(), null);
            return true;
        } else if (itemId == R.id.toggle_fullscreen) {
            GUI.SetFullscreen actionSetFullscreen = new GUI.SetFullscreen();
//                Input.ExecuteAction actionSetFullscreen = new Input.ExecuteAction(Input.ExecuteAction.TOGGLEFULLSCREEN);
            actionSetFullscreen.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.clean_video_library) {
            VideoLibrary.Clean actionCleanVideo = new VideoLibrary.Clean();
            actionCleanVideo.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.clean_audio_library) {
            AudioLibrary.Clean actionCleanAudio = new AudioLibrary.Clean();
            actionCleanAudio.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.update_video_library) {
            VideoLibrary.Scan actionScanVideo = new VideoLibrary.Scan();
            actionScanVideo.execute(hostManager.getConnection(), null, null);
            return true;
        } else if (itemId == R.id.update_audio_library) {
            AudioLibrary.Scan actionScanAudio = new AudioLibrary.Scan();
            actionScanAudio.execute(hostManager.getConnection(), null, null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Override hardware volume keys and send to Kodi
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            handled = VolumeControllerDialogFragmentListener.handleVolumeKeyEvent(this, event);

            // Show volume change dialog if the event was handled and we are not in
            // first page, which already contains a volume control
            if (handled && (binding.pager.getCurrentItem() != 0)) {
                new VolumeControllerDialogFragmentListener()
                        .show(getSupportFragmentManager(), VolumeControllerDialogFragmentListener.class.getName());
            }
        }
        return handled || super.dispatchKeyEvent(event);
    }

    /**
     * Callbacks from Send text dialog
     */
    public void onSendTextFinished(String text, boolean done) {
        if (TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR.isRtl(text, 0, text.length())) {
            text = new StringBuilder(text).reverse().toString();
        }
        Input.SendText action = new Input.SendText(text, done);
        action.execute(hostManager.getConnection(), null, null);
    }

    public void onSendTextCancel() {
        // Nothing to do
    }


    private void setupActionBar() {
        setToolbarTitle(binding.includeToolbar.defaultToolbar, NOWPLAYING_FRAGMENT_ID);
        setSupportActionBar(binding.includeToolbar.defaultToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
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
    ViewPager2.OnPageChangeCallback defaultOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            setToolbarTitle(binding.includeToolbar.defaultToolbar, position);
        }
    };

    /**
     * Sets or clear the image background
     * @param url Image url
     */
    private void setImageViewBackground(String url) {
        if (url != null) {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);

            UIUtils.loadImageIntoImageview(hostManager, url, binding.backgroundImage,
                    displaySize.x, displaySize.y / 2);

            final int pixelsPerPage = displaySize.x / 4;

            binding.backgroundImage.getViewTreeObserver()
                                   .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    binding.backgroundImage.getViewTreeObserver().removeOnPreDrawListener(this);
                    // Position the image
                    int offsetX =  (binding.pager.getCurrentItem() - 1) * pixelsPerPage;
                    binding.backgroundImage.scrollTo(offsetX, 0);

                    binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                            int offsetX = (int) ((position - 1 + positionOffset) * pixelsPerPage);
                            binding.backgroundImage.scrollTo(offsetX, 0);
                        }

                        @Override
                        public void onPageSelected(int position) {
                            setToolbarTitle(binding.includeToolbar.defaultToolbar, position);
                        }
                    });

                    return true;
                }
            });
        } else {
            binding.backgroundImage.setImageDrawable(null);
            binding.pager.registerOnPageChangeCallback(defaultOnPageChangeCallback);
        }
    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    private String lastImageUrl = null;

    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {

    }

    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        String imageUrl = (TextUtils.isEmpty(getItemResult.fanart)) ?
                getItemResult.thumbnail : getItemResult.fanart;
        if ((imageUrl != null) && !imageUrl.equals(lastImageUrl)) {
            setImageViewBackground(imageUrl);
        }
        lastImageUrl = imageUrl;

        MediaSessionService.startIfNotRunning(this);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        playerOnPlay(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnStop() {
        LogUtils.LOGD(TAG, "Player stopping");
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

    public void observerOnStopObserving() {}

    /**
     * Now playing fragment listener
     */
    public void SwitchToRemotePanel() {
        binding.pager.setCurrentItem(1);
    }
}
