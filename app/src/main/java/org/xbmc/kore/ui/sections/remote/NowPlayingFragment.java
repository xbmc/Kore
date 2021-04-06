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

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.ui.generic.GenericSelectDialog;
import org.xbmc.kore.ui.sections.video.AllCastActivity;
import org.xbmc.kore.ui.widgets.HighlightButton;
import org.xbmc.kore.ui.widgets.MediaProgressIndicator;
import org.xbmc.kore.ui.widgets.RepeatModeButton;
import org.xbmc.kore.ui.widgets.VolumeLevelIndicator;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Now playing view
 */
public class NowPlayingFragment extends Fragment
        implements HostConnectionObserver.PlayerEventsObserver,
                   HostConnectionObserver.ApplicationEventsObserver,
                   GenericSelectDialog.GenericSelectDialogListener,
                   MediaProgressIndicator.OnProgressChangeListener,
                   ViewTreeObserver.OnScrollChangedListener {
    private static final String TAG = LogUtils.makeLogTag(NowPlayingFragment.class);

    /**
     * Interface for this fragment to communicate with the enclosing activity
     */
    public interface NowPlayingListener {
        public void SwitchToRemotePanel();
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

    private ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();
    private ApiCallback<Boolean> defaultBooleanActionCallback = ApiMethod.getDefaultActionCallback();

    private Unbinder unbinder;

    private int pixelsToTransparent;

    /**
     * Injectable views
     */
    @BindView(R.id.play) ImageButton playButton;

    @BindView(R.id.volume_mute) HighlightButton volumeMuteButton;
    @BindView(R.id.shuffle) HighlightButton shuffleButton;
    @BindView(R.id.repeat) RepeatModeButton repeatButton;
    @BindView(R.id.overflow) ImageButton overflowButton;

    @BindView(R.id.info_panel) RelativeLayout infoPanel;
    @BindView(R.id.media_panel) ScrollView mediaPanel;

    @BindView(R.id.info_title) TextView infoTitle;
    @BindView(R.id.info_message) TextView infoMessage;

    @BindView(R.id.art) ImageView mediaArt;
    @BindView(R.id.poster) ImageView mediaPoster;

    @BindView(R.id.media_title) TextView mediaTitle;
    @BindView(R.id.media_undertitle) TextView mediaUndertitle;
    @BindView(R.id.progress_info) MediaProgressIndicator mediaProgressIndicator;

    @BindView(R.id.volume_level_indicator) VolumeLevelIndicator volumeLevelIndicator;

    @BindView(R.id.rating) TextView mediaRating;
    @BindView(R.id.max_rating) TextView mediaMaxRating;
    @BindView(R.id.year) TextView mediaYear;
    @BindView(R.id.genres) TextView mediaGenreSeason;
    @BindView(R.id.rating_votes) TextView mediaRatingVotes;

    @BindView(R.id.media_description) TextView mediaDescription;
    @BindView(R.id.cast_list) GridLayout videoCastList;

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
        unbinder = ButterKnife.bind(this, root);

        volumeLevelIndicator.setOnVolumeChangeListener(new VolumeLevelIndicator.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(int volume) {
                new Application.SetVolume(volume)
                        .execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
            }
        });

        mediaProgressIndicator.setOnProgressChangeListener(this);

        volumeLevelIndicator.setOnVolumeChangeListener(new VolumeLevelIndicator.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(int volume) {
                new Application.SetVolume(volume).execute(hostManager.getConnection(),
                                                          defaultIntActionCallback, callbackHandler);
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

        /** Setup dim the fanart when scroll changes
         * Full dim on 4 * iconSize dp
         * @see {@link #onScrollChanged()}
         */
        pixelsToTransparent  = 4 * getActivity().getResources().getDimensionPixelSize(R.dimen.default_icon_size);
        mediaPanel.getViewTreeObserver().addOnScrollChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this);
        hostConnectionObserver.registerApplicationObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopNowPlayingInfo();
        hostConnectionObserver.unregisterPlayerObserver(this);
        hostConnectionObserver.unregisterApplicationObserver(this);
    }

    @Override
    public void onDestroyView() {
        mediaPanel.getViewTreeObserver().removeOnScrollChangedListener(this);
        super.onDestroyView();
        unbinder.unbind();
    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

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
    public void onScrollChanged() {
        float y = mediaPanel.getScrollY();
        float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
        mediaArt.setAlpha(newAlpha);
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
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, false);
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

    private void moveProgress(int deltaSeconds) {
        int progress = mediaProgressIndicator.getProgress();
        progress += deltaSeconds;
        progress = Math.max(progress, 0);
        progress = Math.min(progress, mediaProgressIndicator.getMaxProgress());
        mediaProgressIndicator.setProgress(progress);
        onProgressChanged(progress);
    }

    @OnClick(R.id.back_30_seconds)
    public void backThirtySeconds(View v) {
        moveProgress(-30);
    }

    @OnClick(R.id.back_10_seconds)
    public void backTenSeconds(View v) {
        moveProgress(-10);
    }

    @OnClick(R.id.go_to_position)
    public void seekToPosition(View v) {
    }

    @OnClick(R.id.forward_10_seconds)
    public void forwardTenSeconds(View v) {
        moveProgress(10);
    }

    @OnClick(R.id.forward_30_seconds)
    public void forwardThirtySeconds(View v) {
        moveProgress(30);
    }

    @OnClick(R.id.volume_mute)
    public void onVolumeMuteClicked(View v) {
        Application.SetMute action = new Application.SetMute();
        action.execute(hostManager.getConnection(), defaultBooleanActionCallback, callbackHandler);
    }

    @OnClick(R.id.shuffle)
    public void onShuffleClicked(View v) {
        Player.SetShuffle action = new Player.SetShuffle(currentActivePlayerId);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                // Force a refresh
                hostConnectionObserver.refreshWhatsPlaying();
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
                hostConnectionObserver.refreshWhatsPlaying();
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
                    case 0:
                        // Download subtitles. First check host version to see which method to call
                        HostInfo hostInfo = hostManager.getHostInfo();
                        if (hostInfo.isGothamOrLater()) {
                            showDownloadSubtitlesPostGotham();
                        } else {
                            showDownloadSubtitlesPreGotham();
                        }
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

    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {
        if (notificationsData.property.shuffled != null)
            shuffleButton.setHighlight(notificationsData.property.shuffled);

        if (notificationsData.property.repeatMode != null)
            UIUtils.setRepeatButton(repeatButton, notificationsData.property.repeatMode);
    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed == 1);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed == 1);
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

    @Override
    public void applicationOnVolumeChanged(int volume, boolean muted) {
        volumeLevelIndicator.setVolume(muted, volume);
        volumeMuteButton.setHighlight(muted);
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {}
    public void observerOnStopObserving() {}

    @Override
    public void onProgressChanged(int progress) {
        PlayerType.PositionTime positionTime = new PlayerType.PositionTime(progress);
        Player.Seek seekAction = new Player.Seek(currentActivePlayerId, positionTime);
        seekAction.execute(HostManager.getInstance(getContext()).getConnection(), new ApiCallback<PlayerType.SeekReturnType>() {
            @Override
            public void onSuccess(PlayerType.SeekReturnType result) {
                // Ignore
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD("MediaSeekBar", "Got an error calling Player.Seek. Error code: " + errorCode + ", description: " + description);
            }
        }, callbackHandler);


    }

    /**
     * Sets whats playing information
     * @param getItemResult Return from method {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
     */
    private void setNowPlayingInfo(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   PlayerType.PropertyValue getPropertiesResult,
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

        mediaTitle.setText(UIUtils.applyMarkup(getContext(), title));
        mediaTitle.post(UIUtils.getMarqueeToggleableAction(mediaTitle));
        mediaUndertitle.setText(underTitle);

        mediaProgressIndicator.setOnProgressChangeListener(this);
        mediaProgressIndicator.setMaxProgress(getPropertiesResult.totaltime.ToSeconds());
        mediaProgressIndicator.setProgress(getPropertiesResult.time.ToSeconds());

        int speed = getPropertiesResult.speed;
        //TODO: check if following is still necessary for PVR playback
        if (getItemResult.type.equals(ListType.ItemsAll.TYPE_CHANNEL))
            speed = 1;
        mediaProgressIndicator.setSpeed(speed);

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
            mediaDescription.setText(UIUtils.applyMarkup(getContext(), descriptionPlot));
        } else {
            mediaDescription.setVisibility(View.GONE);
        }

        UIUtils.setRepeatButton(repeatButton, getPropertiesResult.repeat);
        shuffleButton.setHighlight(getPropertiesResult.shuffled);

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
        mediaProgressIndicator.setSpeed(0);

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
}
