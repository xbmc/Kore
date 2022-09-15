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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.transition.Transition;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.MaterialElevationScale;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.ActivityGenericMediaBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.service.MediaSessionService;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.ui.generic.VolumeControllerDialogFragmentListener;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

public abstract class BaseMediaActivity
        extends BaseActivity
        implements HostConnectionObserver.ApplicationEventsObserver,
                   HostConnectionObserver.PlayerEventsObserver,
                   AbstractInfoFragment.fabPlayProvider {
    private static final String TAG = LogUtils.makeLogTag(BaseMediaActivity.class);

    private static final String NAVICON_ISARROW = "navstate";
    private static final String ACTIONBAR_TITLE = "actionbartitle";

    public static final String IMAGE_TRANS_NAME = "IMAGE_TRANS_NAME";

    ActivityGenericMediaBinding binding;

    private NavigationDrawerFragment navigationDrawerFragment;

    private boolean drawerIndicatorIsArrow;

    private HostManager hostManager;
    private HostConnectionObserver hostConnectionObserver;

    private boolean showNowPlayingPanel;

    protected abstract String getActionBarTitle();
    protected abstract Fragment createFragment();

    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private final Runnable hidePanelRunnable = new Runnable() {
        @Override
        public void run() {
            binding.nowPlayingPanel.hidePanel();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGenericMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        if (navigationDrawerFragment != null)
            navigationDrawerFragment.setUp(R.id.navigation_drawer, binding.drawerLayout);

        Toolbar toolbar = findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String actionBarTitle;
            boolean naviconIsArrow = false;
            if (savedInstanceState != null) {
                actionBarTitle = savedInstanceState.getString(ACTIONBAR_TITLE);
                naviconIsArrow = savedInstanceState.getBoolean(NAVICON_ISARROW);
            } else {
                actionBarTitle = getActionBarTitle();
            }

            actionBar.setDisplayHomeAsUpEnabled(true);
            updateActionBar(actionBarTitle, naviconIsArrow);
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = createFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, getActionBarTitle())
                    .setReorderingAllowed(true)
                    .commit();
        }

        hostManager = HostManager.getInstance(this);
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    protected void onSaveInstanceState(@NonNull  Bundle outState) {
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

    @Override
    protected void onResume() {
        super.onResume();
        showNowPlayingPanel = PreferenceManager.getDefaultSharedPreferences(this)
                                               .getBoolean(Settings.KEY_PREF_SHOW_NOW_PLAYING_PANEL,
                                                           Settings.DEFAULT_PREF_SHOW_NOW_PLAYING_PANEL);
        if (showNowPlayingPanel) {
            hostConnectionObserver.registerApplicationObserver(this);
            hostConnectionObserver.registerPlayerObserver(this);
            hostConnectionObserver.refreshWhatsPlaying();
            binding.nowPlayingPanel.completeSetup(this, this.getSupportFragmentManager(), binding.fragmentContainer);
        } else {
            //Hide it in case we were displaying the panel and user disabled showing the panel in Settings
            binding.nowPlayingPanel.hidePanel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (showNowPlayingPanel) {
            hostConnectionObserver.unregisterApplicationObserver(this);
            hostConnectionObserver.unregisterPlayerObserver(this);
            binding.nowPlayingPanel.freeResources();
        }
    }

    /**
     * Override hardware volume keys and send to Kodi
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = VolumeControllerDialogFragmentListener.handleVolumeKeyEvent(this, event);
        if (handled) {
            new VolumeControllerDialogFragmentListener()
                    .show(getSupportFragmentManager(), VolumeControllerDialogFragmentListener.class.getName());
        }
        return handled || super.dispatchKeyEvent(event);
    }

    public boolean getDrawerIndicatorIsArrow() {
        return drawerIndicatorIsArrow;
    }

    /**
     * Sets the title and drawer indicator of the toolbar
     * @param title toolbar title
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
        int itemId = item.getItemId();
        if (itemId == R.id.action_show_remote) {
            Intent launchIntent = new Intent(this, RemoteActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launchIntent);
            overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Replaces the fragment with the given one, showing a Shared Element transition based on the image view specified
     * The entering fragment must postpone its enter transition and call startPostponedEnterTransition when it's
     * all setup, so that the shared element transition works
     * For the reenter transition (pop the back stack) to the current fragment, the current fragment must:
     * - postpone its enter transition as it is being asked to by the flag shouldPostponeReenterTransition
     * - launch an Event Bus message ListFragmentSetupComplete when it's fully set up, so that the postponed
     * transition is allowed to run.
     * Notice that the fragment postponing and starting the postponed transition might be different than the one
     * launching the Event Bus message, as is the case if the current fragment is a Tabs Fragment: the Tabs Fragment
     * postpones the transition, a child of his should launch the Event Bus message, which is caught by the Tabs
     * Fragment to start the postponed transition
     * @param fragment Fragment to add
     * @param args Arguments to the fragment
     * @param sharedImageView Image view to use in a Shared Element transition
     */
    protected void showFragment(Class<? extends Fragment> fragment, Bundle args, ImageView sharedImageView) {
        args.putString(IMAGE_TRANS_NAME, sharedImageView.getTransitionName());

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof AbstractFragment) {
            // Postpone reenter transition to allow for shared element loading
            ((AbstractFragment) currentFragment).setPostponeReenterTransition(true);
            Transition exitTransition = new MaterialElevationScale(false),
                    reenterTransition = new MaterialElevationScale(true);
            int exitDuration = getResources().getInteger(R.integer.fragment_exit_animation_duration),
                    reenterDuration = getResources().getInteger(R.integer.fragment_popenter_animation_duration),
                    reenterStartDelay = getResources().getInteger(R.integer.fragment_popenter_start_offset);
            exitTransition.setDuration(exitDuration);
            // Unfortunately we can't do a startDelay on reenter, as the reentering fragment becomes visible
            // during that delay, which ruins the transition
            reenterTransition.setDuration(reenterDuration + reenterStartDelay);
            reenterTransition.setStartDelay(0);
            currentFragment.setExitTransition(exitTransition);
            currentFragment.setReenterTransition(reenterTransition);
        }
        fragmentManager.beginTransaction()
                       .setReorderingAllowed(true)
                       .addSharedElement(sharedImageView, sharedImageView.getTransitionName())
                       .replace(R.id.fragment_container, fragment, args, getActionBarTitle())
                       .addToBackStack(null)
                       .commit();
        binding.topAppBarLayout.setExpanded(true);
    }

    /**
     * Replaces the fragment with the given one, with a generic animation
     * @param fragment Fragment to add
     * @param args Arguments to the fragment
     */
    protected void showFragment(Class<? extends Fragment> fragment, Bundle args) {
        args.putString(IMAGE_TRANS_NAME, null);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof AbstractFragment) {
            // This is to unset previously set transitions with the overloaded showFragment function.
            // This can happen in Tabs Fragment, where fragments on one tab want to use transitions (with the
            // overloaded showFragment), and fragments on another tab want to use the animations provided in here.
            // If an item on the other tab is clicked and the transitions are set on the Tabs Fragment, then the
            // animations set here (on the same Tabs Fragment) won't run, unless we unset the transitions
            currentFragment.setExitTransition(null);
            currentFragment.setReenterTransition(null);
            ((AbstractFragment) currentFragment).setPostponeReenterTransition(false);
        }
        // Replace fragment
        fragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_popenter, R.anim.fragment_popexit)
                .replace(R.id.fragment_container, fragment, args)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onApplicationVolumeChanged(int volume, boolean muted) {
        binding.nowPlayingPanel.setVolumeState(volume, muted);
    }

    @Override
    public void onPlayerPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {
        binding.nowPlayingPanel.setRepeatShuffleState(notificationsData.property.repeatMode,
                                                      notificationsData.property.shuffled,
                                                      notificationsData.property.partymode);
    }

    @Override
    public void onPlayerPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        updateNowPlayingPanel(getActivePlayerResult, getPropertiesResult, getItemResult);
        // Start the MediaSession service
        MediaSessionService.startIfNotRunning(this);
    }

    @Override
    public void onPlayerPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult, PlayerType.PropertyValue getPropertiesResult, ListType.ItemsAll getItemResult) {
        updateNowPlayingPanel(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    @Override
    public void onPlayerStop() {
        // Delay hiding the panel to prevent hiding it when playing the next item in a playlist
        callbackHandler.removeCallbacks(hidePanelRunnable);
        callbackHandler.postDelayed(hidePanelRunnable, 1000);
    }

    @Override
    public void onPlayerConnectionError(int errorCode, String description) {
        binding.nowPlayingPanel.hidePanel();
    }

    @Override
    public void onPlayerNoResultsYet() {}

    @Override
    public void onObserverStopObserving() {
        binding.nowPlayingPanel.hidePanel();
    }

    @Override
    public void onSystemQuit() {
        binding.nowPlayingPanel.hidePanel();
    }

    @Override
    public void onInputRequested(String title, String type, String value) {}

    private void updateNowPlayingPanel(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                       PlayerType.PropertyValue getPropertiesResult,
                                       ListType.ItemsAll getItemResult) {
        String title;
        String poster;
        String details = null;

        callbackHandler.removeCallbacks(hidePanelRunnable);

        binding.nowPlayingPanel.showPanel();

        binding.nowPlayingPanel.setPlaybackState(getActivePlayerResult, getPropertiesResult);

        switch (getItemResult.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                title = getItemResult.title;
                details = getItemResult.tagline;
                poster = TextUtils.isEmpty(getItemResult.art.poster) ? getItemResult.art.fanart
                                                                    : getItemResult.art.poster;
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                title = getItemResult.title;
                String seasonEpisode = String.format(getString(R.string.season_episode_abbrev),
                                                     getItemResult.season, getItemResult.episode);
                details = String.format("%s | %s", getItemResult.showtitle, seasonEpisode);
                poster = TextUtils.isEmpty(getItemResult.art.poster) ? getItemResult.art.fanart
                                                                     : getItemResult.art.poster;
                break;
            case ListType.ItemsAll.TYPE_SONG:
                title = getItemResult.title;
                details = getItemResult.displayartist + " | " + getItemResult.album;
                poster = TextUtils.isEmpty(getItemResult.thumbnail) ? getItemResult.fanart
                                                                    : getItemResult.thumbnail;
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                title = getItemResult.title;
                details = Utils.listStringConcat(getItemResult.artist, ", ") + " | " + getItemResult.album;
                poster = TextUtils.isEmpty(getItemResult.thumbnail) ? getItemResult.fanart
                                                                    : getItemResult.thumbnail;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                title = getItemResult.label;
                details = getItemResult.title;
                poster = TextUtils.isEmpty(getItemResult.thumbnail) ? getItemResult.fanart
                                                                    : getItemResult.thumbnail;
                break;
            default:
                title = getItemResult.label;
                poster = TextUtils.isEmpty(getItemResult.thumbnail) ? getItemResult.fanart
                                                                    : getItemResult.thumbnail;
                break;
        }

        if (title.contentEquals(binding.nowPlayingPanel.getTitle()))
            return; // Still showing same item as previous call

        binding.nowPlayingPanel.setTitle(title);

        if (details != null) {
            binding.nowPlayingPanel.setDetails(details);
        }

        Resources resources = getResources();
        int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_panel_art_width);
        int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_panel_art_heigth);

        UIUtils.loadImageWithCharacterAvatar(this, hostManager, poster, title,
                                             binding.nowPlayingPanel.getPoster(),
                                             posterWidth, posterHeight);

//        // If not video, change aspect ration of poster to a square
//        boolean isVideo = (getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
//                          (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE));
//        binding.nowPlayingPanel.setSquarePoster(!isVideo);
//        UIUtils.loadImageWithCharacterAvatar(this, hostManager, poster, title,
//                                             binding.nowPlayingPanel.getPoster(),
//                                             (isVideo) ? posterWidth : posterHeight, posterHeight);
    }

    @Override
    public FloatingActionButton getFABPlay() {
        return binding.fabPlay;
    }
}
