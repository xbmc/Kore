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

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostConnectionObserver;
import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.ApiCallback;
import com.syncedsynapse.kore2.jsonrpc.ApiMethod;
import com.syncedsynapse.kore2.jsonrpc.method.GUI;
import com.syncedsynapse.kore2.jsonrpc.method.Input;
import com.syncedsynapse.kore2.jsonrpc.method.Player;
import com.syncedsynapse.kore2.jsonrpc.type.GlobalType;
import com.syncedsynapse.kore2.jsonrpc.type.ListType;
import com.syncedsynapse.kore2.jsonrpc.type.PlayerType;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.RepeatListener;
import com.syncedsynapse.kore2.utils.UIUtils;
import com.syncedsynapse.kore2.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Remote view
 */
public class RemoteFragment extends Fragment
        implements HostConnectionObserver.PlayerEventsObserver {
    private static final String TAG = LogUtils.makeLogTag(RemoteFragment.class);

    /**
     * Host manager from which to get info about the current XBMC
     */
    private HostManager hostManager;

    /**
     * Activity to communicate potential actions that change what's playing
     */
    private HostConnectionObserver hostConnectionObserver;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    /**
     * The current active player id
     */
    private int currentActivePlayerId = -1;

    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.media_panel) RelativeLayout mediaPanel;
    @InjectView(R.id.remote) RelativeLayout remotePanel;

    @InjectView(R.id.info_title) TextView infoTitle;
    @InjectView(R.id.info_message) TextView infoMessage;

    @InjectView(R.id.button_bar) LinearLayout buttonBarPanel;

    /**
     * Buttons
     */
    @InjectView(R.id.home) ImageButton homeButton;
    @InjectView(R.id.movies) ImageButton moviesButton;
    @InjectView(R.id.tv_shows) ImageButton tvShowsButton;
    @InjectView(R.id.music) ImageButton musicButton;
    @InjectView(R.id.pictures) ImageButton picturesButton;

    @InjectView(R.id.select) ImageView selectButton;
    @InjectView(R.id.left) ImageView leftButton;
    @InjectView(R.id.right) ImageView rightButton;
    @InjectView(R.id.up) ImageView upButton;
    @InjectView(R.id.down) ImageView downButton;
    @InjectView(R.id.back) ImageView backButton;
    @InjectView(R.id.info) ImageView infoButton;
    @InjectView(R.id.codec_info) ImageView codecInfoButton;
    @InjectView(R.id.osd) ImageView osdButton;

    @InjectView(R.id.art) ImageView thumbnail;
    @InjectView(R.id.title) TextView nowPlayingTitle;
    @InjectView(R.id.details) TextView nowPlayingDetails;

    @InjectView(R.id.play) ImageButton playButton;
    @InjectView(R.id.rewind) ImageButton rewindButton;
    @InjectView(R.id.fast_forward) ImageButton fastForwardButton;

    private Animation buttonInAnim;
    private Animation buttonOutAnim;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();

        buttonInAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.button_in);
        buttonOutAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.button_out);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_remote, container, false);
        ButterKnife.inject(this, root);

        // Setup repeat buttons
        setupRepeatButton(leftButton, new Input.Left());
        setupRepeatButton(rightButton, new Input.Right());
        setupRepeatButton(upButton, new Input.Up());
        setupRepeatButton(downButton, new Input.Down());

        setupNoRepeatButton(selectButton, new Input.Select());

        setupNoRepeatButton(backButton, new Input.Back());
        setupNoRepeatButton(infoButton, new Input.ExecuteAction(Input.ExecuteAction.INFO));
        setupNoRepeatButton(osdButton, new Input.ExecuteAction(Input.ExecuteAction.OSD));
        setupContextualNoRepeatButton(codecInfoButton, new Input.ExecuteAction(Input.ExecuteAction.CODECINFO), new Input.ContextMenu());

//        // Padd main content view to account for bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), root, false, false, true);

        // No clipping
//        root.setClipToPadding(false);

        return root;
    }

//    /**
//     * Select button callback
//     * @param v Clicked view
//     */
//    @OnClick(R.id.select)
//    public void onSelectClicked(View v) {
//        v.startAnimation(buttonInOutAnim);
//        Input.Select action = new Input.Select();
//        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
//    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        // Get last result from host observer, so that we update the UI accordingly
        // One of the PlayerEventsObserver callbacks will be called if there's a result available
        hostConnectionObserver.replyWithLastResult(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        hostConnectionObserver.unregisterPlayerObserver(this);
    }

    private void setupRepeatButton(View button, final ApiMethod<String> action) {
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    }
                }, buttonInAnim, buttonOutAnim));
    }

    private void setupNoRepeatButton(View button, final ApiMethod<String> action) {
        button.setOnTouchListener(new RepeatListener(-1, -1,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    }
                }, buttonInAnim, buttonOutAnim));
    }

    private void setupContextualNoRepeatButton(View button, final ApiMethod<String> actionPlaying, final ApiMethod<String> actionNotPlaying) {
        button.setOnTouchListener(new RepeatListener(-1, -1,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isNowPlaying())
                            actionPlaying.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                        else
                            actionNotPlaying.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    }
                }, buttonInAnim, buttonOutAnim));
    }


    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultActionCallback = ApiMethod.getDefaultActionCallback();

    /**
     * Callbacks for boottoom button bar
     */
    @OnClick(R.id.home)
    public void onHomeClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.HOME);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @OnClick(R.id.movies)
    public void onMoviedClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.VIDEOS, GUI.ActivateWindow.PARAM_MOVIE_TITLES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @OnClick(R.id.tv_shows)
    public void onTvShowsClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.VIDEOS, GUI.ActivateWindow.PARAM_TV_SHOWS_TITLES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @OnClick(R.id.music)
    public void onMusicClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.MUSICLIBRARY);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @OnClick(R.id.pictures)
    public void onPicturesClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.PICTURES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    /**
     * Calllbacks for media control buttons
     */
    @OnClick(R.id.fast_forward)
    public void onFastForwardClicked(View v) {
        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.INCREMENT);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    @OnClick(R.id.rewind)
    public void onRewindClicked(View v) {
        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.DECREMENT);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    @OnClick(R.id.play)
    public void onPlayClicked(View v) {
        Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    /**
     * Callback for methods that change the play speed
     */
    private ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
        @Override
        public void onSucess(Integer result) {
            if (!isAdded()) return;
            UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, result);
        }

        @Override
        public void onError(int errorCode, String description) { }
    };

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnStop() {
        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel, true);
        infoTitle.setText(R.string.nothing_playing);
        infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel, false);
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
            // TODO: check error code
            infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
            infoMessage.setText(null);
        }
    }

    public void playerNoResultsYet() {
        // Initialize info panel
        switchToPanel(R.id.info_panel, false);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
        }
        infoMessage.setText(null);
    }

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {}

        /**
         * Sets whats playing information
         * @param nowPlaying Return from method {@link com.syncedsynapse.kore2.jsonrpc.method.Player.GetItem}
         */
    private void setNowPlayingInfo(ListType.ItemsAll nowPlaying,
                                   PlayerType.PropertyValue properties) {
        String title, underTitle, thumbnailUrl;
        if (nowPlaying.type.equals(ListType.ItemsAll.TYPE_MOVIE)) {
            switchToPanel(R.id.media_panel, true);

            title = nowPlaying.title;
            underTitle = nowPlaying.tagline;
            thumbnailUrl = nowPlaying.thumbnail;
        } else if (nowPlaying.type.equals(ListType.ItemsAll.TYPE_EPISODE)) {
            switchToPanel(R.id.media_panel, true);

            title = nowPlaying.title;
            String season = String.format(getString(R.string.season_episode_abbrev), nowPlaying.season, nowPlaying.episode);
            underTitle = String.format("%s | %s", nowPlaying.showtitle, season);
            thumbnailUrl = nowPlaying.art.poster;
        } else if (nowPlaying.type.equals(ListType.ItemsAll.TYPE_SONG)) {
            switchToPanel(R.id.media_panel, true);

            title = nowPlaying.title;
            underTitle = nowPlaying.displayartist + " | " + nowPlaying.album;
            thumbnailUrl = nowPlaying.thumbnail;
        } else if (nowPlaying.type.equals(ListType.ItemsAll.TYPE_MUSIC_VIDEO)) {
            switchToPanel(R.id.media_panel, true);

            title = nowPlaying.title;
            underTitle = Utils.listStringConcat(nowPlaying.artist, ", ") + " | " + nowPlaying.album;
            thumbnailUrl = nowPlaying.thumbnail;
        } else {
            switchToPanel(R.id.media_panel, true);
            title = nowPlaying.label;
            underTitle = "";
            thumbnailUrl = nowPlaying.thumbnail;
        }

        nowPlayingTitle.setText(title);
        nowPlayingDetails.setText(underTitle);

        UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                thumbnailUrl, title,
                thumbnail, thumbnail.getWidth(), thumbnail.getHeight());
    }

    /**
     * Returns true if media is loaded (playing or paused)
     */
    private boolean isNowPlaying() {
        return mediaPanel.getVisibility() == View.VISIBLE;
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId, boolean showRemote) {
        switch (panelResId) {
            case R.id.info_panel:
                mediaPanel.setVisibility(View.GONE);
                infoPanel.setVisibility(View.VISIBLE);
                break;
            case R.id.media_panel:
                infoPanel.setVisibility(View.GONE);
                mediaPanel.setVisibility(View.VISIBLE);
                break;
        }

        if (showRemote) {
            remotePanel.setVisibility(View.VISIBLE);
            buttonBarPanel.setVisibility(View.VISIBLE);
        } else {
            remotePanel.setVisibility(View.GONE);
            buttonBarPanel.setVisibility(View.GONE);
        }
    }
}
