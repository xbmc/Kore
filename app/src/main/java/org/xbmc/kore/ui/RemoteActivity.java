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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.eventclient.EventServerConnection;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.method.System;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.service.NotificationService;
import org.xbmc.kore.ui.hosts.AddHostActivity;
import org.xbmc.kore.ui.hosts.AddHostFragmentFinish;
import org.xbmc.kore.ui.views.CirclePageIndicator;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;
import org.xbmc.kore.utils.UIUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class RemoteActivity extends BaseActivity
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

        // If we should start playing something

//        // Setup system bars and content padding
//        setupSystemBarsColors();
//        // Set the padding of views.
//        // Only set top and right, to allow bottom to overlap in each fragment
//        UIUtils.setPaddingForSystemBars(this, viewPager, true, true, false);
//        UIUtils.setPaddingForSystemBars(this, pageIndicator, true, true, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        handleStartIntent(getIntent());
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver = hostManager.getHostConnectionObserver();
        hostConnectionObserver.registerPlayerObserver(this, true);
        // Force a refresh, mainly to update the time elapsed on the fragments
        hostConnectionObserver.forceRefreshResults();

        checkPVREnabledAndSetMenuItems();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hostConnectionObserver != null) hostConnectionObserver.unregisterPlayerObserver(this);
        hostConnectionObserver = null;
    }

    // TODO: Remove this method after deployment of 2.0.0, as it is only needed to
    // facilitate the transition by checking if PVR is enabled and set the side menu
    // items accordingly
    private void checkPVREnabledAndSetMenuItems() {
        if (hostManager.getHostInfo() == null) return;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // Check if PVR is enabled for the current host
        String prefKey = Settings.KEY_PREF_CHECKED_PVR_ENABLED + String.valueOf(hostManager.getHostInfo().getId());
        boolean checkedPVREnabled = sp.getBoolean(prefKey, Settings.DEFAULT_PREF_CHECKED_PVR_ENABLED);
        if (!checkedPVREnabled) {
            AddHostFragmentFinish.checkPVREnabledAndSetMenuItems(this, new Handler());
            sp.edit()
              .putBoolean(prefKey, true)
              .apply();
        }
    }


    /**
     * Override hardware volume keys and send to Kodi
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Check whether we should intercept this
        boolean useVolumeKeys = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_USE_HARDWARE_VOLUME_KEYS,
                        Settings.DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS);
        if (useVolumeKeys) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (action == KeyEvent.ACTION_DOWN) {
                        new Application
                                .SetVolume(GlobalType.IncrementDecrement.INCREMENT)
                                .execute(hostManager.getConnection(), null, null);
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (action == KeyEvent.ACTION_DOWN) {
                        new Application
                                .SetVolume(GlobalType.IncrementDecrement.DECREMENT)
                                .execute(hostManager.getConnection(), null, null);
                    }
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
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
            case R.id.action_reboot:
                System.Reboot actionReboot = new System.Reboot();
                // Fire and forget
                actionReboot.execute(hostManager.getConnection(), null, null);
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
            case R.id.toggle_fullscreen:
                GUI.SetFullscreen actionSetFullscreen = new GUI.SetFullscreen();
//                Input.ExecuteAction actionSetFullscreen = new Input.ExecuteAction(Input.ExecuteAction.TOGGLEFULLSCREEN);
                actionSetFullscreen.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.clean_video_library:
                VideoLibrary.Clean actionCleanVideo = new VideoLibrary.Clean();
                actionCleanVideo.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.clean_audio_library:
                AudioLibrary.Clean actionCleanAudio = new AudioLibrary.Clean();
                actionCleanAudio.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.update_video_library:
                VideoLibrary.Scan actionScanVideo = new VideoLibrary.Scan();
                actionScanVideo.execute(hostManager.getConnection(), null, null);
                return true;
            case R.id.update_audio_library:
                AudioLibrary.Scan actionScanAudio = new AudioLibrary.Scan();
                actionScanAudio.execute(hostManager.getConnection(), null, null);
                return true;
			default:
				break;
		}

		return super.onOptionsItemSelected(item);
    }

    /**
     * Issue commands to update the Audio and Video libraries, sequentially
     */
    private void updateLibraries() {
        final Handler callbackHandler = new Handler();
        VideoLibrary.Scan actionScanVideo = new VideoLibrary.Scan();
        actionScanVideo.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Great, now update the Audio library
                AudioLibrary.Scan actionScanAudio = new AudioLibrary.Scan();
                actionScanAudio.execute(hostManager.getConnection(), null, callbackHandler);
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);
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
        setToolbarTitle(toolbar, 1);
        setSupportActionBar(toolbar);

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

    /**
     * Handles the intent that started this activity, namely to start playing something on Kodi
     * @param intent Start intent for the activity
     */
    private void handleStartIntent(Intent intent) {
        final String action = intent.getAction();
        // Check action
        if ((action == null) ||
                !(action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_VIEW)))
            return;

        Uri videoUri = null;
        if (action.equals(Intent.ACTION_SEND)) {
            // Get the URI, which is stored in Extras
            videoUri = getYouTubeUri(intent.getStringExtra(Intent.EXTRA_TEXT));
            if (videoUri == null) return;
        } else if (action.equals(Intent.ACTION_VIEW)) {
            if (intent.getData() == null) return;
            videoUri = Uri.parse(intent.getData().toString());
        }

        final String videoId = getVideoId(videoUri);
        if (videoId == null) {
            Toast.makeText(RemoteActivity.this,
                    R.string.error_share_video,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final String kodiAddonUrl = "plugin://plugin.video." +
                (videoUri.getHost().endsWith("vimeo.com") ? "vimeo" : "youtube") +
                "/play/?video_id=" + videoId;

        // Check if any video player is active and clear the playlist before queuing if so
        final HostConnection connection = hostManager.getConnection();
        final Handler callbackHandler = new Handler();
        Player.GetActivePlayers getActivePlayers = new Player.GetActivePlayers();
        getActivePlayers.execute(connection, new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result) {
                boolean videoIsPlaying = false;

                for (PlayerType.GetActivePlayersReturnType player : result) {
                    if (player.type.equals(PlayerType.GetActivePlayersReturnType.VIDEO))
                        videoIsPlaying = true;
                }

                if (!videoIsPlaying) {
                    // Clear the playlist
                    clearPlaylistAndQueueFile(kodiAddonUrl, connection, callbackHandler);
                } else {
                    queueFile(kodiAddonUrl, false, connection, callbackHandler);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Couldn't get active player when handling start intent.");
                Toast.makeText(RemoteActivity.this,
                        String.format(getString(R.string.error_get_active_player), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
        intent.setAction(null);

    }

    /**
     * Clears Kodi's playlist, queues the given media file and starts the playlist
     * @param file File to play
     * @param connection Host connection
     * @param callbackHandler Handler to use for posting callbacks
     */
    private void clearPlaylistAndQueueFile(final String file,
                                           final HostConnection connection, final Handler callbackHandler) {
        LogUtils.LOGD(TAG, "Clearing video playlist");
        Playlist.Clear action = new Playlist.Clear(PlaylistType.VIDEO_PLAYLISTID);
        action.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Now queue and start the file
                queueFile(file, true, connection, callbackHandler);
            }

            @Override
            public void onError(int errorCode, String description) {
                Toast.makeText(RemoteActivity.this,
                               String.format(getString(R.string.error_queue_media_file), description),
                               Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    /**
     * Queues the given media file and optionally starts the playlist
     * @param file File to play
     * @param startPlaylist Whether to start playing the playlist after add
     * @param connection Host connection
     * @param callbackHandler Handler to use for posting callbacks
     */
    private void queueFile(final String file, final boolean startPlaylist,
                           final HostConnection connection, final Handler callbackHandler) {
        LogUtils.LOGD(TAG, "Queing file");
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = file;
        Playlist.Add action = new Playlist.Add(PlaylistType.VIDEO_PLAYLISTID, item);
        action.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                if (startPlaylist) {
                    Player.Open action = new Player.Open(Player.Open.TYPE_PLAYLIST, PlaylistType.VIDEO_PLAYLISTID);
                    action.execute(connection, new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                        }

                        @Override
                        public void onError(int errorCode, String description) {
                            Toast.makeText(RemoteActivity.this,
                                    String.format(getString(R.string.error_play_media_file), description),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, callbackHandler);
                }

                // Force a refresh of the playlist fragment
                String tag = "android:switcher:" + viewPager.getId() + ":" + 3;
                PlaylistFragment playlistFragment = (PlaylistFragment)getSupportFragmentManager()
                        .findFragmentByTag(tag);
                if (playlistFragment != null) {
                    playlistFragment.forceRefreshPlaylist();
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                Toast.makeText(RemoteActivity.this,
                        String.format(getString(R.string.error_queue_media_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }


    /**
     * Returns the YouTube Uri that the YouTube app passes in EXTRA_TEXT
     * YouTube sends something like: [Video title]: [YouTube URL] so we need
     * to get the second part
     *
     * @param extraText EXTRA_TEXT passed in the intent
     * @return Uri present in extraText if present
     */
    private Uri getYouTubeUri(String extraText) {
        if (extraText == null) return null;

        for (String word : extraText.split(" ")) {
            if (word.startsWith("http://") || word.startsWith("https://")) {
                try {
                    URL validUri = new URL(word);
                    return Uri.parse(word);
                } catch (MalformedURLException exc) {
                    LogUtils.LOGD(TAG, "Got a malformed URL in an intent: " + word);
                    return null;
                }

            }
        }
        return null;
    }

    /**
     * Returns the youtube/vimeo video ID from its URL
     *
     * @param playuri Youtube/Vimeo URL
     * @return Youtube/Vimeo Video ID
     */
    private String getVideoId(Uri playuri) {
        if (playuri.getHost().endsWith("youtube.com") || playuri.getHost().endsWith("youtu.be") || playuri.getHost().endsWith("vimeo.com")) {
            // We'll need to get the v= parameter from the URL
            final Pattern pattern;
            if (playuri.getHost().endsWith("vimeo.com")) {
                pattern = Pattern.compile("^(?:https?:\\/\\/)?(?:www\\.|player\\.)?vimeo\\.com\\/(?:.*\\/)?(\\d+)(?:\\?.*)?$",
                        Pattern.CASE_INSENSITIVE);
            }
            else {
                pattern = Pattern.compile("^(?:https?:\\/\\/)?(?:www\\.|m\\.)?youtu(?:\\.be\\/|be\\.com\\/watch\\?v=)([\\w-]+)(?:[\\?&].*)?$",
                        Pattern.CASE_INSENSITIVE);
            }
            final Matcher matcher = pattern.matcher(playuri.toString());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
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
     * @param url Image url
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

        // Check whether we should show a notification
        boolean showNotification = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_SHOW_NOTIFICATION, Settings.DEFAULT_PREF_SHOW_NOTIFICATION);
        if (showNotification) {
            // Let's start the notification service
            LogUtils.LOGD(TAG, "Starting notification service");
            startService(new Intent(this, NotificationService.class));
        }
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

    public void observerOnStopObserving() {}

    /**
     * Now playing fragment listener
     */
    public void SwitchToRemotePanel() {
        viewPager.setCurrentItem(1);
    }
}
