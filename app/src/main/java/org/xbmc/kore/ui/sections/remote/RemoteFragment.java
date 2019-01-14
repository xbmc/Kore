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

import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.eventclient.ButtonCodes;
import org.xbmc.kore.eventclient.EventServerConnection;
import org.xbmc.kore.eventclient.Packet;
import org.xbmc.kore.eventclient.PacketBUTTON;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.ui.widgets.ControlPad;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;
import butterknife.Unbinder;

/**
 * Remote view
 */
public class RemoteFragment extends Fragment
        implements HostConnectionObserver.PlayerEventsObserver,
                   ControlPad.OnPadButtonsListener {
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

    /**
     * The current item type
     */
    private String currentNowPlayingItemType = null;

    private final Packet leftButtonPacket =
            new PacketBUTTON(ButtonCodes.MAP_REMOTE, ButtonCodes.REMOTE_LEFT, false, true,
                             true, (short)0, (byte)0);
    private final Packet rightButtonPacket =
            new PacketBUTTON(ButtonCodes.MAP_REMOTE, ButtonCodes.REMOTE_RIGHT, false, true,
                             true, (short)0, (byte)0);
    private final Packet upButtonPacket =
            new PacketBUTTON(ButtonCodes.MAP_REMOTE, ButtonCodes.REMOTE_UP, false, true,
                             true, (short)0, (byte)0);
    private final Packet downButtonPacket =
            new PacketBUTTON(ButtonCodes.MAP_REMOTE, ButtonCodes.REMOTE_DOWN, false, true,
                             true, (short)0, (byte)0);
    private final Packet selectButtonPack =
            new PacketBUTTON(ButtonCodes.MAP_REMOTE, ButtonCodes.REMOTE_SELECT, false, true,
                             true, (short)0, (byte)0);

    private final ApiMethod<String> downButtonAction = new Input.Down();
    private final ApiMethod<String> leftButtonAction = new Input.Left();
    private final ApiMethod<String> upButtonAction = new Input.Up();
    private final ApiMethod<String> rightButtonAction = new Input.Right();
    private final ApiMethod<String> selectButtonAction = new Input.Select();
    private final ApiMethod<String> backButtonAction = new Input.Back();
    private final ApiMethod<String> infoButtonAction = new Input.ExecuteAction(Input.ExecuteAction.INFO);
    private final ApiMethod<String> contextButtonAction = new Input.ExecuteAction(Input.ExecuteAction.CONTEXTMENU);
    private final ApiMethod<String> osdButtonAction = new Input.ExecuteAction(Input.ExecuteAction.OSD);

    @BindView(R.id.info_panel) RelativeLayout infoPanel;
    @BindView(R.id.media_panel) RelativeLayout mediaPanel;
    @BindView(R.id.remote) ControlPad controlPad;

    @BindView(R.id.info_title) TextView infoTitle;
    @BindView(R.id.info_message) TextView infoMessage;

    @BindView(R.id.button_bar) LinearLayout buttonBarPanel;

    /**
     * Buttons
     */
    @Nullable @BindView(R.id.home) ImageButton homeButton;
    @Nullable @BindView(R.id.movies) ImageButton moviesButton;
    @Nullable @BindView(R.id.tv_shows) ImageButton tvShowsButton;
    @Nullable @BindView(R.id.music) ImageButton musicButton;
    @Nullable @BindView(R.id.pvr) ImageButton pvrButton;
    @Nullable @BindView(R.id.pictures) ImageButton picturesButton;
    @Nullable @BindView(R.id.videos) ImageButton videosButton;
    //@Nullable @BindView(R.id.favourites) ImageButton favouritesButton;
    @Nullable @BindView(R.id.addons) ImageButton addonsButton;
    @Nullable @BindView(R.id.weather) ImageButton weatherButton;
    @Nullable @BindView(R.id.system) ImageButton systemButton;

    @BindView(R.id.art) ImageView thumbnail;
    @BindView(R.id.title) TextView nowPlayingTitle;
    @BindView(R.id.details) TextView nowPlayingDetails;

    @BindView(R.id.play) ImageButton playButton;
    @BindView(R.id.stop) ImageButton stopButton;
    @BindView(R.id.rewind) ImageButton rewindButton;
    @BindView(R.id.fast_forward) ImageButton fastForwardButton;

    // EventServer connection
    private EventServerConnection eventServerConnection = null;

    // Icons for fastForward/Rewind or skipPrevious/skipNext
    int fastForwardIcon, rewindIcon, skipPreviousIcon, skipNextIcon;

    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();


        eventServerConnection = createEventServerConnection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_remote, container, false);
        unbinder = ButterKnife.bind(this, root);

        controlPad.setOnPadButtonsListener(this);

        HostInfo hostInfo = hostManager.getHostInfo();

        TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(new int[] {
                R.attr.iconFastForward,
                R.attr.iconRewind,
                R.attr.iconNext,
                R.attr.iconPrevious
        });
        fastForwardIcon = styledAttributes.getResourceId(styledAttributes.getIndex(0), R.drawable.ic_fast_forward_white_24dp);
        rewindIcon = styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_fast_rewind_white_24dp);
        skipNextIcon = styledAttributes.getResourceId(styledAttributes.getIndex(2), R.drawable.ic_skip_next_white_24dp);
        skipPreviousIcon = styledAttributes.getResourceId(styledAttributes.getIndex(3), R.drawable.ic_skip_previous_white_24dp);
        styledAttributes.recycle();

        Set<String> shownItems = PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getStringSet(Settings.getRemoteBarItemsPrefKey(hostInfo.getId()),
                        new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.default_values_remote_bar_items))));
        ImageButton[] buttons = {
                homeButton, moviesButton, tvShowsButton, musicButton, pvrButton, picturesButton,
                videosButton, addonsButton, weatherButton, systemButton
        };
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null)
                buttons[i].setVisibility(shownItems.contains(String.valueOf(i)) ? View.VISIBLE : View.GONE);
        }

        nowPlayingTitle.setClickable(true);
        nowPlayingTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { v.setSelected(!v.isSelected()); }
        });

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this, true);
        if (eventServerConnection == null)
            eventServerConnection = createEventServerConnection();
    }

    @Override
    public void onPause() {
        super.onPause();
        hostConnectionObserver.unregisterPlayerObserver(this);
        if (eventServerConnection != null) {
            eventServerConnection.quit();
            eventServerConnection = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    /**
     * Creates a new EventServerConnection if using the event server
     * is enabled in the preferences.
     * @return EventServerConnection or null if usage is disabled
     */
    private EventServerConnection createEventServerConnection() {
        if (! hostManager.getHostInfo().getUseEventServer()) {
            return null;
        }

        return new EventServerConnection(
                hostManager.getHostInfo(),
                new EventServerConnection.EventServerConnectionCallback() {
                    @Override
                    public void OnConnectResult(boolean success) {
                        if (!success) {
                            LogUtils.LOGD(TAG, "Couldn\'t setup EventServer, disabling it");
                            eventServerConnection = null;
                        }
                    }
                });
    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultActionCallback = ApiMethod.getDefaultActionCallback();

    /**
     * Callbacks for bottom button bar
     */
    @Optional
    @OnClick(R.id.home)
    public void onHomeClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.HOME);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.movies)
    public void onMoviedClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.VIDEOS, GUI.ActivateWindow.PARAM_MOVIE_TITLES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.tv_shows)
    public void onTvShowsClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.VIDEOS, GUI.ActivateWindow.PARAM_TV_SHOWS_TITLES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.music)
    public void onMusicClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.MUSIC, GUI.ActivateWindow.PARAM_ROOT);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.pvr)
    public void onRadioClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.TVCHANNELS);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.pictures)
    public void onPicturesClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.PICTURES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.videos)
    public void onVideosClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.VIDEOS, GUI.ActivateWindow.PARAM_ROOT);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    /*@Optional
    @OnClick(R.id.favourites)
    public void onFavouritesClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.FAVOURITES);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }*/

    @Optional
    @OnClick(R.id.addons)
    public void onAddonsClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.ADDONBROWSER);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.weather)
    public void onWeatherClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.WEATHER);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Optional
    @OnClick(R.id.system)
    public void onSystemClicked(View v) {
        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.SETTINGS);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    /**
     * Calllbacks for media control buttons
     */
    @OnClick(R.id.fast_forward)
    public void onFastForwardClicked(View v) {
        if (ListType.ItemsAll.TYPE_SONG.equals(currentNowPlayingItemType)) {
            Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.NEXT);
            action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        } else {
            Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.INCREMENT);
            action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
        }
    }

    @OnClick(R.id.rewind)
    public void onRewindClicked(View v) {
        if (ListType.ItemsAll.TYPE_SONG.equals(currentNowPlayingItemType)) {
            Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.PREVIOUS);
            action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        } else {
            Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.DECREMENT);
            action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
        }
    }

    @OnClick(R.id.play)
    public void onPlayClicked(View v) {
        Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    @OnClick(R.id.stop)
    public void onStopClicked(View v) {
        Player.Stop action = new Player.Stop(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    /**
     * Callback for methods that change the play speed
     */
    private ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
        @Override
        public void onSuccess(Integer result) {
            if (!isAdded()) return;
            UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, result == 1);
        }

        @Override
        public void onError(int errorCode, String description) { }
    };

    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {

    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed == 1);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed == 1);
    }

    public void playerOnStop() {
        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel, true);
        infoTitle.setText(R.string.nothing_playing);
        infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel, true);
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
        switchToPanel(R.id.info_panel, true);
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
    public void observerOnStopObserving() {}

    /**
     * Sets whats playing information
     * @param nowPlaying Return from method {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
     */
    private void setNowPlayingInfo(ListType.ItemsAll nowPlaying,
                                   PlayerType.PropertyValue properties) {
        String title, underTitle, thumbnailUrl;
        int currentRewindIcon, currentFastForwardIcon;

        switch (nowPlaying.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                underTitle = nowPlaying.tagline;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                String season = String.format(getString(R.string.season_episode_abbrev), nowPlaying.season, nowPlaying.episode);
                underTitle = String.format("%s | %s", nowPlaying.showtitle, season);
                thumbnailUrl = nowPlaying.art.poster;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;
                break;
            case ListType.ItemsAll.TYPE_SONG:
                switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                underTitle = nowPlaying.displayartist + " | " + nowPlaying.album;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = skipNextIcon;
                currentRewindIcon = skipPreviousIcon;
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                switchToPanel(R.id.media_panel, true);

                title = nowPlaying.title;
                underTitle = Utils.listStringConcat(nowPlaying.artist, ", ") + " | " + nowPlaying.album;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                switchToPanel(R.id.media_panel, true);

                title = nowPlaying.label;
                underTitle = nowPlaying.title;
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;
                break;
            default:
                switchToPanel(R.id.media_panel, true);
                title = nowPlaying.label;
                underTitle = "";
                thumbnailUrl = nowPlaying.thumbnail;
                currentFastForwardIcon = fastForwardIcon;
                currentRewindIcon = rewindIcon;
                break;
        }

        nowPlayingTitle.setText(title);
        nowPlayingDetails.setText(underTitle);

        fastForwardButton.setImageResource(currentFastForwardIcon);
        rewindButton.setImageResource(currentRewindIcon);
//        // If not video, change aspect ration of poster to a square
//        boolean isVideo = (nowPlaying.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
//                (nowPlaying.type.equals(ListType.ItemsAll.TYPE_EPISODE));
//        if (!isVideo) {
//            ViewGroup.LayoutParams layoutParams = thumbnail.getLayoutParams();
//            layoutParams.width = layoutParams.height;
//            thumbnail.setLayoutParams(layoutParams);
//        }

        UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                thumbnailUrl, title,
                thumbnail, thumbnail.getWidth(), thumbnail.getHeight());
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
            controlPad.setVisibility(View.VISIBLE);
            buttonBarPanel.setVisibility(View.VISIBLE);
        } else {
            controlPad.setVisibility(View.GONE);
            buttonBarPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public void leftButtonClicked() {
        if (eventServerConnection != null) {
            eventServerConnection.sendPacket(leftButtonPacket);
        } else {
            leftButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        }
    }

    @Override
    public void rightButtonClicked() {
        if (eventServerConnection != null) {
            eventServerConnection.sendPacket(rightButtonPacket);
        } else {
            rightButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        }
    }

    @Override
    public void upButtonClicked() {
        if (eventServerConnection != null) {
            eventServerConnection.sendPacket(upButtonPacket);
        } else {
            upButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        }
    }

    @Override
    public void downButtonClicked() {
        if (eventServerConnection != null) {
            eventServerConnection.sendPacket(downButtonPacket);
        } else {
            downButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        }
    }

    @Override
    public void selectButtonClicked() {
        if (eventServerConnection != null) {
            eventServerConnection.sendPacket(selectButtonPack);
        } else {
            selectButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
        }
    }

    @Override
    public void backButtonClicked() {
        backButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Override
    public void infoButtonClicked() {
        infoButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Override
    public boolean infoButtonLongClicked() {
        ApiMethod<String> action;
        HostInfo hostInfo = hostManager.getHostInfo();

        // Info button, v17 uses a different window to display codec info so check version number
        if (hostInfo.isKryptonOrLater()) {
            action = new Input.ExecuteAction(Input.ExecuteAction.PLAYERPROCESSINFO);
        } else {
            action = new Input.ExecuteAction(Input.ExecuteAction.CODECINFO);
        }
        action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);

        return true;
    }

    @Override
    public void contextButtonClicked() {
        contextButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }

    @Override
    public void osdButtonClicked() {
        osdButtonAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
    }
}
