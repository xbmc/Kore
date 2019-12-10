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

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.System;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.service.ConnectionObserversManagerService;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.ui.generic.SendTextDialogFragment;
import org.xbmc.kore.ui.generic.VolumeControllerDialogFragmentListener;
import org.xbmc.kore.ui.sections.hosts.AddHostActivity;
import org.xbmc.kore.ui.sections.localfile.HttpApp;
import org.xbmc.kore.ui.views.CirclePageIndicator;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;


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

    private Future<Boolean> pendingShare;

    private Future<Void> awaitingShare;

    @BindView(R.id.background_image) ImageView backgroundImage;
    @BindView(R.id.pager_indicator) CirclePageIndicator pageIndicator;
    @BindView(R.id.pager) ViewPager viewPager;
    @BindView(R.id.default_toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set default values for the preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_remote);
        ButterKnife.bind(this);

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
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set up pager and fragments
        TabsAdapter tabsAdapter = new TabsAdapter(this, getSupportFragmentManager())
                .addTab(NowPlayingFragment.class, null, R.string.now_playing, NOWPLAYING_FRAGMENT_ID)
                .addTab(RemoteFragment.class, null, R.string.remote, REMOTE_FRAGMENT_ID)
                .addTab(PlaylistFragment.class, null, R.string.playlist, PLAYLIST_FRAGMENT_ID);

        viewPager.setAdapter(tabsAdapter);
        pageIndicator.setViewPager(viewPager);
        pageIndicator.setOnPageChangeListener(defaultOnPageChangeListener);

        viewPager.setCurrentItem(1);
        viewPager.setOffscreenPageLimit(2);

        setupActionBar();

        // Periodic Check of Kodi version
        hostManager.checkAndUpdateKodiVersion();

        // If we should start playing something

//        // Setup system bars and content padding
//        setupSystemBarsColors();
//        // Set the padding of views.
//        // Only set top and right, to allow bottom to overlap in each fragment
//        UIUtils.setPaddingForSystemBars(this, viewPager, true, true, false);
//        UIUtils.setPaddingForSystemBars(this, pageIndicator, true, true, false);

        //noinspection unchecked
        pendingShare = (Future<Boolean>) getLastCustomNonConfigurationInstance();
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
        if (awaitingShare != null) awaitingShare.cancel(true);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return pendingShare;
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
     * Override hardware volume keys and send to Kodi
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            handled = VolumeControllerDialogFragmentListener.handleVolumeKeyEvent(this, event);

            // Show volume change dialog if the event was handled and we are not in
            // first page, which already contains a volume control
            if (handled && (viewPager.getCurrentItem() != 0)) {
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
        setToolbarTitle(toolbar, NOWPLAYING_FRAGMENT_ID);
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
     * Provides the thread where the intent will be handled
     */
    private static ExecutorService SHARE_EXECUTOR = null;
    private static ExecutorService getShareExecutor() {
        if (SHARE_EXECUTOR == null) {
            SHARE_EXECUTOR = Executors.newSingleThreadExecutor();
        }
        return SHARE_EXECUTOR;
    }

    /**
     * Handles the intent that started this activity, namely to start playing something on Kodi
     * @param intent Start intent for the activity
     */
    protected void handleStartIntent(Intent intent) {
        handleStartIntent(intent, false);
    }

    protected void handleStartIntent(Intent intent, boolean queue) {
        if (pendingShare != null) {
            awaitShare(queue);
            return;
        }

        final String action = intent.getAction();
        // Check action
        if ((action == null) ||
                !(action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_VIEW)))
            return;

        Uri videoUri;
        if (action.equals(Intent.ACTION_SEND)) {
            // Get the URI, which is stored in Extras
            videoUri = getYouTubeUri(intent.getStringExtra(Intent.EXTRA_TEXT));
        } else {
            videoUri = intent.getData();
        }

        String url;
        if (videoUri == null) {
            url = getShareLocalUri(intent);
        } else {
            url = toPluginUrl(videoUri);
        }

        if (url == null) {
            url = videoUri.toString();
        }

        // If a host was passed from the intent use it
        int hostId = intent.getIntExtra("hostId", 0);
        if (hostId > 0) {
            HostManager hostManager = HostManager.getInstance(this);
            for (HostInfo host : hostManager.getHosts()) {
                if (host.getId() == hostId) {
                    hostManager.switchHost(host);
                    break;
                }
            }
        }

        // Determine which playlist to use
        String intentType = intent.getType();
        int playlistType;
        if (intentType == null) {
            playlistType = PlaylistType.VIDEO_PLAYLISTID;
        } else {
            if (intentType.matches("audio.*")) {
                playlistType = PlaylistType.MUSIC_PLAYLISTID;
            } else if (intentType.matches("video.*")) {
                playlistType = PlaylistType.VIDEO_PLAYLISTID;
            } else if (intentType.matches("image.*")) {
                playlistType = PlaylistType.PICTURE_PLAYLISTID;
            } else {
                // Generic links? Default to video:
                playlistType = PlaylistType.VIDEO_PLAYLISTID;
            }
        }

        String title = getString(R.string.app_name);
        String text = getString(R.string.item_added_to_playlist);
        pendingShare = getShareExecutor().submit(
                new OpenSharedUrl(hostManager.getConnection(), url, title, text, queue, playlistType));

        awaitShare(queue);
        intent.setAction(null);

        // Don't display Kore after sharing content from another app:
        finish();
    }

    private String getShareLocalUri(Intent intent) {
        Uri contentUri = intent.getData();

        if (contentUri == null) {
            Bundle bundle = intent.getExtras();
            contentUri = (Uri) bundle.get(Intent.EXTRA_STREAM);
        }
        if (contentUri == null) {
            return null;
        }

        HttpApp http_app = null;
        try {
            http_app = HttpApp.getInstance(getApplicationContext(), 8080);
        } catch (IOException ioe) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.error_starting_http_server),
                    Toast.LENGTH_LONG).show();
            return null;
        }
        http_app.addUri(contentUri);
        String url = http_app.getLinkToFile();

        return url;
    }

    /**
     * Awaits the completion of the share request in the same background thread
     * where the request is running.
     * <p>
     * This needs to run stuff in the UI thread so the activity reference is
     * inevitable, but unlike the share request this doesn't need to outlive the
     * activity. The resulting future __must__ be cancelled when the activity is
     * paused (it will drop itself when cancelled or finished). This should be called
     * again when the activity is resumed and a {@link #pendingShare} exists.
     */
    private void awaitShare(final boolean queue) {
        awaitingShare = getShareExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    final boolean wasAlreadyPlaying = pendingShare.get();
                    pendingShare = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (wasAlreadyPlaying) {
                                String msg;
                                if (queue) {
                                    msg = getString(R.string.item_added_to_playlist);
                                } else {
                                    msg = getString(R.string.item_sent_to_kodi);
                                }
                                Toast.makeText(RemoteActivity.this,
                                    msg,
                                    Toast.LENGTH_SHORT)
                                    .show();
                            }
                        }
                    });
                } catch (InterruptedException ignored) {
                } catch (ExecutionException ex) {
                    pendingShare = null;
                    final OpenSharedUrl.Error e = (OpenSharedUrl.Error) ex.getCause();
                    LogUtils.LOGE(TAG, "Share failed", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RemoteActivity.this,
                                getString(e.stage, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    awaitingShare = null;
                }
                return null;
            }
        });
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
     * Converts a video url to a Kodi plugin URL.
     *
     * @param playuri some URL
     * @return plugin URL
     */
    private String toPluginUrl(Uri playuri) {
        String host = playuri.getHost();
        if (host.endsWith("youtube.com")) {
            String videoId = playuri.getQueryParameter("v");
            String playlistId = playuri.getQueryParameter("list");
            Uri.Builder pluginUri = new Uri.Builder()
                    .scheme("plugin")
                    .authority("plugin.video.youtube")
                    .path("play/");
            boolean valid = false;
            if (videoId != null) {
                valid = true;
                pluginUri.appendQueryParameter("video_id", videoId);
            }
            if (playlistId != null) {
                valid = true;
                pluginUri.appendQueryParameter("playlist_id", playlistId)
                        .appendQueryParameter("order", "default");
            }
            if (valid) {
                return pluginUri.build().toString();
            }
        } else if (host.endsWith("youtu.be")) {
            return "plugin://plugin.video.youtube/play/?video_id="
                    + playuri.getLastPathSegment();
        } else if (host.endsWith("vimeo.com")) {
            String last = playuri.getLastPathSegment();
            if (last.matches("\\d+")) {
                return "plugin://plugin.video.vimeo/play/?video_id=" + last;
            }
        } else if (host.endsWith("svtplay.se")) {
            Pattern pattern = Pattern.compile(
                    "^(?:https?:\\/\\/)?(?:www\\.)?svtplay\\.se\\/video\\/(\\d+\\/.*)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(playuri.toString());
            if (matcher.matches()) {
                return "plugin://plugin.video.svtplay/?url=%2Fvideo%2F"
                        + URLEncoder.encode(matcher.group(1)) + "&mode=video";
            }
        } else if (host.endsWith("soundcloud.com")) {
            return "plugin://plugin.audio.soundcloud/play/?url="
                    + URLEncoder.encode(playuri.toString());
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

    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {

    }

    @TargetApi(Build.VERSION_CODES.O)
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
                .getBoolean(Settings.KEY_PREF_SHOW_NOTIFICATION,
                            Settings.DEFAULT_PREF_SHOW_NOTIFICATION);
        if (showNotification) {
            // Start service that manages connection observers
            LogUtils.LOGD(TAG, "Starting observer service");
            if (Utils.isOreoOrLater()) {
                startForegroundService(new Intent(this, ConnectionObserversManagerService.class));
            } else {
                startService(new Intent(this, ConnectionObserversManagerService.class));
            }
        }
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
        viewPager.setCurrentItem(1);
    }
}
