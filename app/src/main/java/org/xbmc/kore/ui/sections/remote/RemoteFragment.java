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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.xbmc.kore.R;
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
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.RepeatListener;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

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

    /**
     * The current item type
     */
    private String currentNowPlayingItemType = null;

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
    @InjectView(R.id.context) ImageView contextButton;
    @InjectView(R.id.osd) ImageView osdButton;

    @InjectView(R.id.art) ImageView thumbnail;
    @InjectView(R.id.title) TextView nowPlayingTitle;
    @InjectView(R.id.details) TextView nowPlayingDetails;

    @InjectView(R.id.play) ImageButton playButton;
    @InjectView(R.id.stop) ImageButton stopButton;
    @InjectView(R.id.rewind) ImageButton rewindButton;
    @InjectView(R.id.fast_forward) ImageButton fastForwardButton;

    private Animation buttonInAnim;
    private Animation buttonOutAnim;
    // Touch listener that provides touch feedback
    private View.OnTouchListener feedbackTouchListener;

    // EventServer connection
    private EventServerConnection eventServerConnection = null;

    // Icons for fastForward/Rewind or skipPrevious/skipNext
    int fastForwardIcon, rewindIcon, skipPreviousIcon, skipNextIcon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();

        buttonInAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.button_in);
        buttonOutAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.button_out);

        createEventServerConnection();

        feedbackTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonInAnim.setFillAfter(true);
                        v.startAnimation(buttonInAnim);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.startAnimation(buttonOutAnim);
                        break;
                }
                return false;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_remote, container, false);
        ButterKnife.inject(this, root);

        if (hostManager.getHostInfo().getUseEventServer() &&
            (eventServerConnection != null)) {
            // Setup d-pad to use EventServer
            setupEventServerButton(leftButton, ButtonCodes.REMOTE_LEFT);
            setupEventServerButton(rightButton, ButtonCodes.REMOTE_RIGHT);
            setupEventServerButton(upButton, ButtonCodes.REMOTE_UP);
            setupEventServerButton(downButton, ButtonCodes.REMOTE_DOWN);

            setupEventServerButton(selectButton, ButtonCodes.REMOTE_SELECT);
        } else {
            // Otherwise, use json-rpc
            setupRepeatButton(leftButton, new Input.Left());
            setupRepeatButton(rightButton, new Input.Right());
            setupRepeatButton(upButton, new Input.Up());
            setupRepeatButton(downButton, new Input.Down());

            setupDefaultButton(selectButton, new Input.Select(), null);
        }

        // Other buttons
        setupDefaultButton(backButton, new Input.Back(), null);
        setupDefaultButton(osdButton, new Input.ExecuteAction(Input.ExecuteAction.OSD), null);
        setupDefaultButton(contextButton, new Input.ExecuteAction(Input.ExecuteAction.CONTEXTMENU), null);

        // Info button, v17 uses a different window to display codec info so check version number
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo.getKodiVersionMajor() < 17) {
            setupDefaultButton(infoButton,
                               new Input.ExecuteAction(Input.ExecuteAction.INFO),
                               new Input.ExecuteAction(Input.ExecuteAction.CODECINFO));
        } else {
            setupDefaultButton(infoButton,
                               new Input.ExecuteAction(Input.ExecuteAction.INFO),
                               new Input.ExecuteAction(Input.ExecuteAction.PLAYERPROCESSINFO));
        }

        adjustRemoteButtons();

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

//        // Pad main content view to account for bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), root, false, false, true);

        // No clipping
//        root.setClipToPadding(false);

        return root;
    }

    @TargetApi(21)
    private void adjustRemoteButtons() {
        Resources.Theme theme = getActivity().getTheme();
        TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                R.attr.remoteButtonColorFilter,
                R.attr.contentBackgroundColor});
//                R.attr.remoteBackgroundColorFilter});
        Resources resources = getResources();
        int remoteButtonsColor =  styledAttributes.getColor(styledAttributes.getIndex(0), resources.getColor(R.color.white)),
            remoteBackgroundColor = styledAttributes.getColor(styledAttributes.getIndex(1), resources.getColor(R.color.dark_content_background_dim_70pct));
        styledAttributes.recycle();

        leftButton.setColorFilter(remoteButtonsColor);
        rightButton.setColorFilter(remoteButtonsColor);
        upButton.setColorFilter(remoteButtonsColor);
        downButton.setColorFilter(remoteButtonsColor);

        selectButton.setColorFilter(remoteButtonsColor);
        backButton.setColorFilter(remoteButtonsColor);
        infoButton.setColorFilter(remoteButtonsColor);
        osdButton.setColorFilter(remoteButtonsColor);
        contextButton.setColorFilter(remoteButtonsColor);


        // On ICS the remote background isn't shown as the tinting isn't supported
        //int backgroundResourceId = R.drawable.remote_background_square_black_alpha;
        int backgroundResourceId = R.drawable.remote_background_square_black;
        if (Utils.isLollipopOrLater()) {
            remotePanel.setBackgroundTintList(ColorStateList.valueOf(remoteBackgroundColor));
            remotePanel.setBackgroundResource(backgroundResourceId);
        } else if (Utils.isJellybeanOrLater()) {
            BitmapDrawable background = new BitmapDrawable(getResources(),
                    BitmapFactory.decodeResource(getResources(), backgroundResourceId));
            background.setColorFilter(new PorterDuffColorFilter(remoteBackgroundColor, PorterDuff.Mode.SRC_IN));
            remotePanel.setBackground(background);
        }
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
            createEventServerConnection();
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

    private void createEventServerConnection() {
        eventServerConnection = new EventServerConnection(
                hostManager.getHostInfo(),
                new EventServerConnection.EventServerConnectionCallback() {
                    @Override
                    public void OnConnectResult(boolean success) {
                        if (!success) {
                            LogUtils.LOGD(TAG, "Couldnt setup EventServer, disabling it");
                            eventServerConnection = null;
                        }
                    }
                });
    }

    private void setupRepeatButton(View button, final ApiMethod<String> action) {
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                                                     new View.OnClickListener() {
                                                         @Override
                                                         public void onClick(View v) {
                                                             action.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                                                         }
                                                     }, buttonInAnim, buttonOutAnim, getActivity().getApplicationContext()));
    }

    private void setupDefaultButton(View button,
                                    final ApiMethod<String> clickAction,
                                    final ApiMethod<String> longClickAction) {
        // Set animation
        button.setOnTouchListener(feedbackTouchListener);
        if (clickAction != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtils.handleVibration(getActivity());
                    clickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                }
            });
        }
        if (longClickAction != null) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    UIUtils.handleVibration(getActivity());
                    longClickAction.execute(hostManager.getConnection(), defaultActionCallback, callbackHandler);
                    return true;
                }
            });
        }
    }

    private void setupEventServerButton(View button, final String action) {
        final Packet packet =
                new PacketBUTTON(ButtonCodes.MAP_REMOTE, action, false, true, true, (short)0, (byte)0);
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                                                     new View.OnClickListener() {
                                                         @Override
                                                         public void onClick(View v) {
                                                             if (eventServerConnection != null)
                                                                 eventServerConnection.sendPacket(packet);
                                                         }
                                                     }, buttonInAnim, buttonOutAnim, getActivity().getApplicationContext()));
    }


//    private void setupEventServerButton(View button, final String action) {
//        short amount = 0;
//        byte axis = 0;
//        final Packet packetDown =
//                new PacketBUTTON(ButtonCodes.MAP_REMOTE, action, true, true, false, amount, axis);
//        final Packet packetUp =
//                new PacketBUTTON(ButtonCodes.MAP_REMOTE, action, false, false, false, amount, axis);
//
//        // Set animation and packet
//        button.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        buttonInAnim.setFillAfter(true);
//                        v.startAnimation(buttonInAnim);
//                        eventServerConnection.sendPacket(packetDown);
//                        break;
//                    case MotionEvent.ACTION_UP:
//                    case MotionEvent.ACTION_CANCEL:
//                        v.startAnimation(buttonOutAnim);
//                        eventServerConnection.sendPacket(packetUp);
//                        break;
//                }
//                return false;
//            }
//        });
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Nothing to do
//            }
//        });
//    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultActionCallback = ApiMethod.getDefaultActionCallback();

    /**
     * Callbacks for bottom button bar
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
        currentNowPlayingItemType = getItemResult.type;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getItemResult, getPropertiesResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        currentNowPlayingItemType = getItemResult.type;
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
            remotePanel.setVisibility(View.VISIBLE);
            buttonBarPanel.setVisibility(View.VISIBLE);
        } else {
            remotePanel.setVisibility(View.GONE);
            buttonBarPanel.setVisibility(View.GONE);
        }
    }
}
