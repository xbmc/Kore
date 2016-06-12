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

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.RepeatListener;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Now playing view
 */
public class NowPlayingFragment extends Fragment
        implements HostConnectionObserver.PlayerEventsObserver,
        GenericSelectDialog.GenericSelectDialogListener {
    private static final String TAG = LogUtils.makeLogTag(NowPlayingFragment.class);

    /**
     * Interface for this fragment to communicate with the enclosing activity
     */
    public interface NowPlayingListener {
        public void SwitchToRemotePanel();
        public void onShuffleClicked();
    }

    /**
     * Constants for the general select dialog
     */
    private final static int SELECT_AUDIOSTREAM = 0;
    private final static int SELECT_SUBTITLES = 1;

    /**
     * Host manager from which to get info about the current XBMC
     */
    private HostManager hostManager;

    /**
     * Activity to communicate potential actions that change what's playing
     */
    private HostConnectionObserver hostConnectionObserver;

    /**
     * Listener for events on this fragment
     */
    private NowPlayingListener nowPlayingListener;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    /**
     * The current active player id
     */
    private int currentActivePlayerId = -1;

    /**
     * List of available subtitles and audiostremas
     */
    private List<PlayerType.Subtitle> availableSubtitles;
    private List<PlayerType.AudioStream> availableAudioStreams;
    private int currentSubtitleIndex = -1;
    private int currentAudiostreamIndex = -1;

    /**
     * Injectable views
     */
    @InjectView(R.id.play) ImageButton playButton;
    @InjectView(R.id.stop) ImageButton stopButton;
    @InjectView(R.id.previous) ImageButton previousButton;
    @InjectView(R.id.next) ImageButton nextButton;
    @InjectView(R.id.rewind) ImageButton rewindButton;
    @InjectView(R.id.fast_forward) ImageButton fastForwardButton;

    @InjectView(R.id.volume_down) ImageButton volumeDownButton;
    @InjectView(R.id.volume_up) ImageButton volumeUpButton;
    @InjectView(R.id.volume_mute) ImageButton volumeMuteButton;
    @InjectView(R.id.shuffle) ImageButton shuffleButton;
    @InjectView(R.id.repeat) ImageButton repeatButton;
    @InjectView(R.id.overflow) ImageButton overflowButton;

    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.media_panel) ScrollView mediaPanel;

    @InjectView(R.id.info_title) TextView infoTitle;
    @InjectView(R.id.info_message) TextView infoMessage;

    @InjectView(R.id.art) ImageView mediaArt;
    @InjectView(R.id.poster) ImageView mediaPoster;

    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;
    @InjectView(R.id.media_duration) TextView mediaDuration;
    @InjectView(R.id.media_progress) TextView mediaProgress;
    @InjectView(R.id.seek_bar) SeekBar mediaSeekbar;

    @InjectView(R.id.media_details) RelativeLayout mediaDetailsPanel;
    @InjectView(R.id.rating) TextView mediaRating;
    @InjectView(R.id.max_rating) TextView mediaMaxRating;
    @InjectView(R.id.year) TextView mediaYear;
    @InjectView(R.id.genres) TextView mediaGenreSeason;
    @InjectView(R.id.rating_votes) TextView mediaRatingVotes;

    @InjectView(R.id.media_description) TextView mediaDescription;
    @InjectView(R.id.cast_list) GridLayout videoCastList;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Try to cast the enclosing activity to the listener interface
        try {
            nowPlayingListener = (NowPlayingListener)activity;
        } catch (ClassCastException e) {
            nowPlayingListener = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_now_playing, container, false);
        ButterKnife.inject(this, root);

        setupVolumeRepeatButton(volumeDownButton,
                new Application.SetVolume(GlobalType.IncrementDecrement.DECREMENT));
        setupVolumeRepeatButton(volumeUpButton,
                new Application.SetVolume(GlobalType.IncrementDecrement.INCREMENT));

        // Setup dim the fanart when scroll changes
        // Full dim on 4 * iconSize dp
        Resources resources = getActivity().getResources();
        final int pixelsToTransparent  = 4 * resources.getDimensionPixelSize(R.dimen.default_icon_size);
        mediaPanel.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                float y = mediaPanel.getScrollY();
                float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
                mediaArt.setAlpha(newAlpha);
            }
        });

//        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), mediaPanel, false, false, true);
//        mediaPanel.setClipToPadding(false);

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
    }

    @Override
    public void onPause() {
        super.onPause();
        stopNowPlayingInfo();
        hostConnectionObserver.unregisterPlayerObserver(this);
    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();
    private ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();
    private ApiCallback<Boolean> defaultBooleanActionCallback = ApiMethod.getDefaultActionCallback();

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

    private void setupVolumeRepeatButton(View button, final ApiMethod<Integer> action) {
        button.setOnTouchListener(new RepeatListener(UIUtils.initialButtonRepeatInterval, UIUtils.buttonRepeatInterval,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        action.execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
                    }
                }));
    }

    /**
     * Callbacks for bottom button bar
     */
    @OnClick(R.id.play)
    public void onPlayClicked(View v) {
        Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    @OnClick(R.id.stop)
    public void onStopClicked(View v) {
        Player.Stop action = new Player.Stop(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, 0);
    }

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

    @OnClick(R.id.previous)
    public void onPreviousClicked(View v) {
        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.PREVIOUS);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

    @OnClick(R.id.next)
    public void onNextClicked(View v) {
        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.NEXT);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

    /**
     * Calllbacks for media button toolbar
     */
    @OnClick(R.id.volume_mute)
    public void onVolumeMuteClicked(View v) {
        Application.SetMute action = new Application.SetMute();
        action.execute(hostManager.getConnection(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (!isAdded()) return;
                if (result) {
                    Resources.Theme theme = getActivity().getTheme();
                    TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                            R.attr.colorAccent});
                    volumeMuteButton.setColorFilter(
                            styledAttributes.getColor(0,
                                    getActivity().getResources().getColor(R.color.accent_default)));
                    styledAttributes.recycle();
                } else {
                    volumeMuteButton.clearColorFilter();
                }
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);
    }

    @OnClick(R.id.shuffle)
    public void onShuffleClicked(View v) {
        Player.SetShuffle action = new Player.SetShuffle(currentActivePlayerId);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                // Force a refresh
                hostConnectionObserver.forceRefreshResults();
                nowPlayingListener.onShuffleClicked();
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);
    }

    @OnClick(R.id.repeat)
    public void onRepeatClicked(View v) {
        Player.SetRepeat action = new Player.SetRepeat(currentActivePlayerId, PlayerType.Repeat.CYCLE);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                hostConnectionObserver.forceRefreshResults();
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);
    }

    @OnClick(R.id.overflow)
    public void onOverflowClicked(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        popup.inflate(R.menu.video_overflow);
        popup.setOnMenuItemClickListener(overflowMenuClickListener);
        popup.show();
    }


    // Number of explicitly added options for audio and subtitles (to subtract from the
    // number of audiostreams and subtitles returned by Kodi)
    static final int ADDED_AUDIO_OPTIONS = 1;
    static final int ADDED_SUBTITLE_OPTIONS = 3;

    /**
     * Overflow menu
     */
    private PopupMenu.OnMenuItemClickListener overflowMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int selectedItem = -1;
            switch (item.getItemId()) {
                case R.id.audiostreams:
                    // Setup audiostream select dialog
                    String[] audiostreams = new String[(availableAudioStreams != null) ?
                            availableAudioStreams.size() + ADDED_AUDIO_OPTIONS : ADDED_AUDIO_OPTIONS];

                    audiostreams[0] = getString(R.string.audio_sync);

                    if (availableAudioStreams != null) {
                        for (int i = 0; i < availableAudioStreams.size(); i++) {
                            PlayerType.AudioStream current = availableAudioStreams.get(i);
                            audiostreams[i + ADDED_AUDIO_OPTIONS] = TextUtils.isEmpty(current.language) ?
                                    current.name : current.language + " | " + current.name;
                            if (current.index == currentAudiostreamIndex) {
                                selectedItem = i + ADDED_AUDIO_OPTIONS;
                            }
                        }

                        GenericSelectDialog dialog = GenericSelectDialog.newInstance(NowPlayingFragment.this,
                                SELECT_AUDIOSTREAM, getString(R.string.audiostreams), audiostreams, selectedItem);
                        dialog.show(NowPlayingFragment.this.getFragmentManager(), null);
                    }
                    return true;
                case R.id.subtitles:
                    // Setup subtitles select dialog
                    String[] subtitles = new String[(availableSubtitles != null) ?
                            availableSubtitles.size() + ADDED_SUBTITLE_OPTIONS : ADDED_SUBTITLE_OPTIONS];

                    subtitles[0] = getString(R.string.download_subtitle);
                    subtitles[1] = getString(R.string.subtitle_sync);
                    subtitles[2] = getString(R.string.none);

                    if (availableSubtitles != null) {
                        for (int i = 0; i < availableSubtitles.size(); i++) {
                            PlayerType.Subtitle current = availableSubtitles.get(i);
                            subtitles[i + ADDED_SUBTITLE_OPTIONS] = TextUtils.isEmpty(current.language) ?
                                    current.name : current.language + " | " + current.name;
                            if (current.index == currentSubtitleIndex) {
                                selectedItem = i + ADDED_SUBTITLE_OPTIONS;
                            }
                        }
                    }

                    GenericSelectDialog dialog = GenericSelectDialog.newInstance(NowPlayingFragment.this,
                            SELECT_SUBTITLES, getString(R.string.subtitles), subtitles, selectedItem);
                    dialog.show(NowPlayingFragment.this.getFragmentManager(), null);
                    return true;
            }
            return false;
        }
    };

    /**
     * Generic dialog select listener
     * @param token
     * @param which
     */
    public void onDialogSelect(int token, int which) {
        switch (token) {
            case SELECT_AUDIOSTREAM:
                // 0 is to sync audio, other is for a specific audiostream
                switch (which) {
                    case -2:
                        for(int i = 0; i < 5; i++) {
                            Input.ExecuteAction syncAudioDelayAction = new Input.ExecuteAction(Input.ExecuteAction.AUDIODELAYMINUS);
                            syncAudioDelayAction.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        }
                        break;
                    case -1:
                        for(int i = 0; i < 5; i++) {
                            Input.ExecuteAction syncAudioAheadAction = new Input.ExecuteAction(Input.ExecuteAction.AUDIODELAYPLUS);
                            syncAudioAheadAction.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        }
                        break;
                    case 0:
                        Input.ExecuteAction syncAudioAction = new Input.ExecuteAction(Input.ExecuteAction.AUDIODELAY);
                        syncAudioAction.execute(hostManager.getConnection(), new ApiCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                if (!isAdded()) return;
                                // Notify enclosing activity to switch panels
                                nowPlayingListener.SwitchToRemotePanel();
                            }

                            @Override
                            public void onError(int errorCode, String description) { }
                        }, callbackHandler);
                        break;
                    default:
                        Player.SetAudioStream setAudioStream = new Player.SetAudioStream(currentActivePlayerId, which - ADDED_AUDIO_OPTIONS);
                        setAudioStream.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        break;
                }
                break;
            case SELECT_SUBTITLES:
                Player.SetSubtitle setSubtitle;
                // 0 is to download subtitles, 1 is for sync, 2 is for none, other is for a specific subtitle index
                switch (which) {
                    case -2:
                        for(int i = 0; i < 5; i++) {
                            Input.ExecuteAction syncSubtitleDelayAction = new Input.ExecuteAction(Input.ExecuteAction.SUBTITLEDELAYMINUS);
                            syncSubtitleDelayAction.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        }
                        break;
                    case -1:
                        for(int i = 0; i < 5; i++) {
                            Input.ExecuteAction syncSubtitleAheadAction = new Input.ExecuteAction(Input.ExecuteAction.SUBTITLEDELAYPLUS);
                            syncSubtitleAheadAction.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        }
                        break;
                    case 0:
                        // Download subtitles. First check host version to see which method to call
                        Application.GetProperties getProperties = new Application.GetProperties(Application.GetProperties.VERSION);
                        getProperties.execute(hostManager.getConnection(), new ApiCallback<ApplicationType.PropertyValue>() {
                            @Override
                            public void onSuccess(ApplicationType.PropertyValue result) {
                                if (!isAdded()) return;
                                // Ok, we've got a version, decide which method to call
                                if (result.version.major < 13) {
                                    showDownloadSubtitlesPreGotham();
                                } else {
                                    showDownloadSubtitlesPostGotham();
                                }
                            }

                            @Override
                            public void onError(int errorCode, String description) {
                                if (!isAdded()) return;
                                // Something went wrong
                                Toast.makeText(getActivity(),
                                        String.format(getString(R.string.error_getting_properties), description),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }, callbackHandler);
                        break;
                    case 1:
                        Input.ExecuteAction syncSubtitleAction = new Input.ExecuteAction(Input.ExecuteAction.SUBTITLEDELAY);
                        syncSubtitleAction.execute(hostManager.getConnection(), new ApiCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                if (!isAdded()) return;
                                // Notify enclosing activity to switch panels
                                nowPlayingListener.SwitchToRemotePanel();
                            }

                            @Override
                            public void onError(int errorCode, String description) { }
                        }, callbackHandler);
                        break;
                    case 2:
                        setSubtitle = new Player.SetSubtitle(currentActivePlayerId, Player.SetSubtitle.OFF, true);
                        setSubtitle.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        break;
                    default:
                        setSubtitle = new Player.SetSubtitle(currentActivePlayerId, which - ADDED_SUBTITLE_OPTIONS, true);
                        setSubtitle.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        break;
                }
                break;
        }
    }

    private void showDownloadSubtitlesPreGotham() {
        // Pre-Gotham
        Addons.ExecuteAddon action = new Addons.ExecuteAddon(Addons.ExecuteAddon.ADDON_SUBTITLES);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                // Notify enclosing activity to switch panels
                nowPlayingListener.SwitchToRemotePanel();
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_executing_subtitles), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    private void showDownloadSubtitlesPostGotham() {
        // Post-Gotham - HACK, HACK
        // Apparently Gui.ActivateWindow with subtitlesearch blocks the TCP listener thread on XBMC
        // While the subtitles windows is showing, i get no response to any call. See:
        // http://forum.xbmc.org/showthread.php?tid=198156
        // Forcing this call through HTTP works, as it doesn't block the TCP listener thread on XBMC
        HostInfo currentHostInfo = hostManager.getHostInfo();
        HostConnection httpHostConnection = new HostConnection(currentHostInfo);
        httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.SUBTITLESEARCH);

        LogUtils.LOGD(TAG, "Activating subtitles window.");
        action.execute(httpHostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                LogUtils.LOGD(TAG, "Sucessfully activated subtitles window.");
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Got an error activating subtitles window. Error: " + description);
            }
        }, callbackHandler);
        // Notify enclosing activity to switch panels
        nowPlayingListener.SwitchToRemotePanel();
    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getPropertiesResult, getItemResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getPropertiesResult, getItemResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);
    }

    public void playerOnStop() {
        HostInfo hostInfo = hostManager.getHostInfo();

        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.nothing_playing);
        infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
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
        switchToPanel(R.id.info_panel);
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
     * @param getItemResult Return from method {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
     */
    private void setNowPlayingInfo(PlayerType.PropertyValue getPropertiesResult,
                                   final ListType.ItemsAll getItemResult) {
        final String title, underTitle, art, poster, genreSeason, year,
                descriptionPlot, votes, maxRating;
        double rating;

        switch (getItemResult.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = getItemResult.tagline;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                maxRating = getString(R.string.max_rating_video);
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = getItemResult.showtitle;
                art = getItemResult.thumbnail;
                poster = getItemResult.art.poster;

                genreSeason = String.format(getString(R.string.season_episode), getItemResult.season, getItemResult.episode);
                year = getItemResult.premiered;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                maxRating = getString(R.string.max_rating_video);
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_SONG:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = getItemResult.displayartist + " | " + getItemResult.album;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.description;
                rating = getItemResult.rating;
                maxRating = getString(R.string.max_rating_music);
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = Utils.listStringConcat(getItemResult.artist, ", ")
                        + " | " + getItemResult.album;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.plot;
                rating = 0;
                maxRating = null;
                votes = null;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                switchToPanel(R.id.media_panel);

                title = getItemResult.label;
                underTitle = getItemResult.title;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = getItemResult.premiered;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                maxRating = null;
                votes = null;
                break;
            default:
                // Other type, just present basic info
                switchToPanel(R.id.media_panel);

                title = getItemResult.label;
                underTitle = "";
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = null;
                year = getItemResult.premiered;
                descriptionPlot = removeYouTubeMarkup(getItemResult.plot);
                rating = 0;
                maxRating = null;
                votes = null;
                break;
        }

        mediaTitle.setText(title);
        mediaUndertitle.setText(underTitle);

        setDurationInfo(getItemResult.type, getPropertiesResult.time, getPropertiesResult.totaltime, getPropertiesResult.speed);
        mediaSeekbar.setOnSeekBarChangeListener(seekbarChangeListener);

        if (!TextUtils.isEmpty(year) || !TextUtils.isEmpty(genreSeason)) {
            mediaYear.setVisibility(View.VISIBLE);
            mediaGenreSeason.setVisibility(View.VISIBLE);
            mediaYear.setText(year);
            mediaGenreSeason.setText(genreSeason);
        } else {
            mediaYear.setVisibility(View.GONE);
            mediaGenreSeason.setVisibility(View.GONE);
        }

        // 0 rating will not be shown
        if (rating > 0) {
            mediaRating.setVisibility(View.VISIBLE);
            mediaMaxRating.setVisibility(View.VISIBLE);
            mediaRatingVotes.setVisibility(View.VISIBLE);
            mediaRating.setText(String.format("%01.01f", rating));
            mediaMaxRating.setText(maxRating);
            mediaRatingVotes.setText(votes);
        } else {
            mediaRating.setVisibility(View.GONE);
            mediaMaxRating.setVisibility(View.GONE);
            mediaRatingVotes.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(descriptionPlot)) {
            mediaDescription.setVisibility(View.VISIBLE);
            mediaDescription.setText(descriptionPlot);
        } else {
            mediaDescription.setVisibility(View.GONE);
        }

        Resources.Theme theme = getActivity().getTheme();
        TypedArray styledAttributes = theme.obtainStyledAttributes(new int[]{
                R.attr.colorAccent,
                R.attr.iconRepeat,
                R.attr.iconRepeatOne});
        int accentDefaultColor = getResources().getColor(R.color.accent_default);
        if (getPropertiesResult.repeat.equals(PlayerType.Repeat.OFF)) {
            repeatButton.setImageResource(styledAttributes.getResourceId(1, R.drawable.ic_repeat_white_24dp));
            repeatButton.clearColorFilter();
        } else if (getPropertiesResult.repeat.equals(PlayerType.Repeat.ONE)) {
            repeatButton.setImageResource(styledAttributes.getResourceId(2, R.drawable.ic_repeat_one_white_24dp));
            repeatButton.setColorFilter(styledAttributes.getColor(0, accentDefaultColor));
        } else {
            repeatButton.setImageResource(styledAttributes.getResourceId(1, R.drawable.ic_repeat_white_24dp));
            repeatButton.setColorFilter(styledAttributes.getColor(0, accentDefaultColor));
        }
        if (!getPropertiesResult.shuffled) {
            shuffleButton.clearColorFilter();
        } else {
            shuffleButton.setColorFilter(styledAttributes.getColor(0, accentDefaultColor));
        }
        styledAttributes.recycle();

        Resources resources = getActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        if (!TextUtils.isEmpty(art)) {
            mediaPoster.setVisibility(View.VISIBLE);
            int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
            int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height);

            // If not video, change aspect ration of poster to a square
            boolean isVideo = (getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
                    (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE));
            if (!isVideo) {
                ViewGroup.LayoutParams layoutParams = mediaPoster.getLayoutParams();
                layoutParams.height = layoutParams.width;
                mediaPoster.setLayoutParams(layoutParams);
                posterHeight = posterWidth;
            }

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                    poster, title,
                    mediaPoster, posterWidth, posterHeight);
            UIUtils.loadImageIntoImageview(hostManager, art, mediaArt, displayMetrics.widthPixels, artHeight);

            // Reset padding
            int paddingLeft = resources.getDimensionPixelOffset(R.dimen.poster_width_plus_padding),
                    paddingRight = mediaTitle.getPaddingRight(),
                    paddingTop = mediaTitle.getPaddingTop(),
                    paddingBottom = mediaTitle.getPaddingBottom();
            mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        } else {
            // No fanart, just present the poster
            mediaPoster.setVisibility(View.GONE);
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager, poster, title, mediaArt, artWidth, artHeight);
            // Reset padding
            int paddingLeft = mediaTitle.getPaddingRight(),
                    paddingRight = mediaTitle.getPaddingRight(),
                    paddingTop = mediaTitle.getPaddingTop(),
                    paddingBottom = mediaTitle.getPaddingBottom();
            mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }

//        UIUtils.loadImageIntoImageview(hostManager, poster, mediaPoster);
//        UIUtils.loadImageIntoImageview(hostManager, art, mediaArt);

        // Continue for videos
//        if (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE) ||
//            getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) {
        // TODO: change this check to the commeted out one when jsonrpc returns the correct type
//        if (getPropertiesResult.type.equals(PlayerType.PropertyValue.TYPE_VIDEO)) {
        if ((getPropertiesResult.audiostreams != null) &&
                (getPropertiesResult.audiostreams.size() > 0)) {
            overflowButton.setVisibility(View.VISIBLE);
            videoCastList.setVisibility(View.VISIBLE);

            // Save subtitles and audiostreams list
            availableAudioStreams = getPropertiesResult.audiostreams;
            availableSubtitles = getPropertiesResult.subtitles;
            currentAudiostreamIndex = getPropertiesResult.currentaudiostream.index;
            currentSubtitleIndex = getPropertiesResult.currentsubtitle.index;

            // Cast list
            UIUtils.setupCastInfo(getActivity(), getItemResult.cast, videoCastList,
                                  AllCastActivity.buildLaunchIntent(getActivity(), title,
                                                                    (ArrayList<VideoType.Cast>)getItemResult.cast));
        } else {
            overflowButton.setVisibility(View.GONE);
            videoCastList.setVisibility(View.GONE);
        }
    }

    /**
     * Cleans up anything left when stop playing
     */
    private void stopNowPlayingInfo() {
        // Just stop the seek bar handler callbacks
        mediaSeekbar.removeCallbacks(seekBarUpdater);
        availableSubtitles = null;
        availableAudioStreams = null;
        currentSubtitleIndex = -1;
        currentAudiostreamIndex = -1;
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        switch (panelResId) {
            case R.id.info_panel:
                mediaPanel.setVisibility(View.GONE);
                mediaArt.setVisibility(View.GONE);
                infoPanel.setVisibility(View.VISIBLE);
                break;
            case R.id.media_panel:
                infoPanel.setVisibility(View.GONE);
                mediaPanel.setVisibility(View.VISIBLE);
                mediaArt.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Removes some markup that appears on the plot for youtube videos
     *
     * @param plot Plot as returned by youtube plugin
     * @return Plot without markup
     */
    private String removeYouTubeMarkup(String plot) {
        if (plot == null) return null;
        return plot.replaceAll("\\[.*\\]", "");
    }

    private int mediaTotalTime = 0,
            mediaCurrentTime = 0; // s
    private static final int SEEK_BAR_UPDATE_INTERVAL = 1000; // ms

    /**
     * Seek bar runnable updater. Runs once a second
     */
    private Runnable seekBarUpdater = new Runnable() {
        @Override
        public void run() {
            if ((mediaTotalTime == 0) || (mediaCurrentTime >= mediaTotalTime)) {
                mediaSeekbar.removeCallbacks(this);
                return;
            }

            mediaCurrentTime += 1;
            mediaSeekbar.setProgress(mediaCurrentTime);

            int hours = mediaCurrentTime / 3600;
            int minutes = (mediaCurrentTime % 3600) / 60;
            int seconds = (mediaCurrentTime % 3600) % 60;

            mediaProgress.setText(UIUtils.formatTime(hours, minutes, seconds));
            mediaSeekbar.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
        }
    };

    /**
     * Sets the information about current media duration and sets seekbar
     * @param type What is playing
     * @param time Current time
     * @param totalTime Total time
     * @param speed Media speed
     */
    private void setDurationInfo(String type, GlobalType.Time time, GlobalType.Time totalTime, int speed) {
        mediaTotalTime = totalTime.hours * 3600 +
                totalTime.minutes * 60 +
                totalTime.seconds;
        mediaSeekbar.setMax(mediaTotalTime);
        mediaDuration.setText(UIUtils.formatTime(totalTime));

        mediaCurrentTime = time.hours * 3600 +
                time.minutes * 60 +
                time.seconds;
        mediaSeekbar.setProgress(mediaCurrentTime);
        mediaProgress.setText(UIUtils.formatTime(time));

        // Only update when its playing
        mediaSeekbar.removeCallbacks(seekBarUpdater);
        if ((speed == 1) || (type.equals(ListType.ItemsAll.TYPE_CHANNEL))) {
            mediaSeekbar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
        }
    }

    /**
     * Seekbar change listener. Sends seek commands to XBMC based on the seekbar position
     */
    private SeekBar.OnSeekBarChangeListener seekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mediaCurrentTime = progress;
                int hours = mediaCurrentTime / 3600;
                int minutes = (mediaCurrentTime % 3600) / 60;
                int seconds = (mediaCurrentTime % 3600) % 60;

                mediaProgress.setText(UIUtils.formatTime(hours, minutes, seconds));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Stop the seekbar updating
            seekBar.removeCallbacks(seekBarUpdater);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int hours = mediaCurrentTime / 3600;
            int minutes = (mediaCurrentTime % 3600) / 60;
            int seconds = (mediaCurrentTime % 3600) % 60;

            PlayerType.PositionTime positionTime = new PlayerType.PositionTime(hours, minutes, seconds, 0);
            Player.Seek seekAction = new Player.Seek(currentActivePlayerId, positionTime);
            seekAction.execute(hostManager.getConnection(), new ApiCallback<PlayerType.SeekReturnType>() {
                @Override
                public void onSuccess(PlayerType.SeekReturnType result) {
                    // Ignore
                }

                @Override
                public void onError(int errorCode, String description) {
                    LogUtils.LOGD(TAG, "Got an error calling Player.Seek. Error code: " + errorCode + ", description: " + description);
                }
            }, callbackHandler);

            // Reset the updating
            seekBar.postDelayed(seekBarUpdater, 1000);
        }
    };
}
