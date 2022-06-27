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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentNowPlayingBinding;
import org.xbmc.kore.databinding.FragmentPlaylistBinding;
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
    private final Handler callbackHandler = new Handler();

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

    private final ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();
    private final ApiCallback<Boolean> defaultBooleanActionCallback = ApiMethod.getDefaultActionCallback();

    private FragmentNowPlayingBinding binding;

    private int pixelsToTransparent;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNowPlayingBinding.inflate(inflater, container, false);
        ViewGroup root = binding.getRoot();

        binding.volumeLevelIndicator.setOnVolumeChangeListener(new VolumeLevelIndicator.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(int volume) {
                new Application.SetVolume(volume)
                        .execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
            }
        });

        binding.progressInfo.setOnProgressChangeListener(this);

        binding.volumeLevelIndicator.setOnVolumeChangeListener(new VolumeLevelIndicator.OnVolumeChangeListener() {
            @Override
            public void onVolumeChanged(int volume) {
                new Application.SetVolume(volume).execute(hostManager.getConnection(),
                                                          defaultIntActionCallback, callbackHandler);
            }
        });

        binding.play.setOnClickListener(this::onPlayClicked);
        binding.stop.setOnClickListener(this::onStopClicked);
        binding.fastForward.setOnClickListener(this::onFastForwardClicked);
        binding.rewind.setOnClickListener(this::onRewindClicked);
        binding.previous.setOnClickListener(this::onPreviousClicked);
        binding.next.setOnClickListener(this::onNextClicked);
        binding.volumeMute.setOnClickListener(this::onVolumeMuteClicked);
        binding.shuffle.setOnClickListener(this::onShuffleClicked);
        binding.repeat.setOnClickListener(this::onRepeatClicked);
        binding.overflow.setOnClickListener(this::onOverflowClicked);

        // Pad main content view to overlap with bottom system bar
        // UIUtils.setPaddingForSystemBars(getActivity(), mediaPanel, false, false, true);
        // mediaPanel.setClipToPadding(false);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);

        /* Setup dim the fanart when scroll changes
         * Full dim on 4 * iconSize dp
         * @see {@link #onScrollChanged()}
         */
        pixelsToTransparent  = 4 * requireActivity().getResources().getDimensionPixelSize(R.dimen.default_icon_size);
        binding.mediaPanel.getViewTreeObserver().addOnScrollChangedListener(this);
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
        binding.mediaPanel.getViewTreeObserver().removeOnScrollChangedListener(this);
        super.onDestroyView();
        binding = null;
    }

    /**
     * Default callback for methods that don't return anything
     */
    private final ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

    /**
     * Callback for methods that change the play speed
     */
    private final ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
        @Override
        public void onSuccess(Integer result) {
            if (!isAdded()) return;
            UIUtils.setPlayPauseButtonIcon(getActivity(), binding.play, result == 1);
        }

        @Override
        public void onError(int errorCode, String description) { }
    };

    @Override
    public void onScrollChanged() {
        float y = binding.mediaPanel.getScrollY();
        float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
        binding.art.setAlpha(newAlpha);
    }

    /**
     * Callbacks for bottom button bar
     */
    public void onPlayClicked(View v) {
        Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

   public void onStopClicked(View v) {
        Player.Stop action = new Player.Stop(currentActivePlayerId);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
        UIUtils.setPlayPauseButtonIcon(getActivity(), binding.play, false);
    }

    public void onFastForwardClicked(View v) {
        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.INCREMENT);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    public void onRewindClicked(View v) {
        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.DECREMENT);
        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
    }

    public void onPreviousClicked(View v) {
        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.PREVIOUS);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

   public void onNextClicked(View v) {
        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.NEXT);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

    public void onVolumeMuteClicked(View v) {
        Application.SetMute action = new Application.SetMute();
        action.execute(hostManager.getConnection(), defaultBooleanActionCallback, callbackHandler);
    }

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
    private final PopupMenu.OnMenuItemClickListener overflowMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int selectedItem = -1;
            int itemId = item.getItemId();
            if (itemId == R.id.audiostreams) {
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
            } else if (itemId == R.id.subtitles) {
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
     * @param token Dialog option selected
     * @param which Which option was selected
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
            binding.shuffle.setHighlight(notificationsData.property.shuffled);

        if (notificationsData.property.repeatMode != null)
            UIUtils.setRepeatButton(binding.repeat, notificationsData.property.repeatMode);
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
        UIUtils.setPlayPauseButtonIcon(getActivity(), binding.play, getPropertiesResult.speed == 1);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Switch icon
        UIUtils.setPlayPauseButtonIcon(getActivity(), binding.play, getPropertiesResult.speed == 1);
    }

    public void playerOnStop() {
        HostInfo hostInfo = hostManager.getHostInfo();

        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        binding.includeInfoPanel.infoTitle.setText(R.string.nothing_playing);
        binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.connecting);
            // TODO: check error code
            binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
            binding.includeInfoPanel.infoMessage.setText(null);
        }
    }

    public void playerNoResultsYet() {
        // Initialize info panel
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.connecting);
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
        }
        binding.includeInfoPanel.infoMessage.setText(null);
    }

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    @Override
    public void applicationOnVolumeChanged(int volume, boolean muted) {
        binding.volumeLevelIndicator.setVolume(muted, volume);
        binding.volumeMute.setHighlight(muted);
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
                art = getItemResult.art.fanart;
                poster = getItemResult.art.poster;

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

        binding.mediaTitle.setText(UIUtils.applyMarkup(getContext(), title));
        binding.mediaTitle.post(UIUtils.getMarqueeToggleableAction(binding.mediaTitle));
        binding.mediaUndertitle.setText(underTitle);

        binding.progressInfo.setOnProgressChangeListener(this);
        binding.progressInfo.setMaxProgress(getPropertiesResult.totaltime.ToSeconds());
        binding.progressInfo.setProgress(getPropertiesResult.time.ToSeconds());

        int speed = getPropertiesResult.speed;
        //TODO: check if following is still necessary for PVR playback
        if (getItemResult.type.equals(ListType.ItemsAll.TYPE_CHANNEL))
            speed = 1;
        binding.progressInfo.setSpeed(speed);

        if (!TextUtils.isEmpty(year) || !TextUtils.isEmpty(genreSeason)) {
            binding.year.setVisibility(View.VISIBLE);
            binding.genres.setVisibility(View.VISIBLE);
            binding.year.setText(year);
            binding.genres.setText(genreSeason);
        } else {
            binding.year.setVisibility(View.GONE);
            binding.genres.setVisibility(View.GONE);
        }

        // 0 rating will not be shown
        if (rating > 0) {
            binding.rating.setVisibility(View.VISIBLE);
            binding.maxRating.setVisibility(View.VISIBLE);
            binding.ratingVotes.setVisibility(View.VISIBLE);
            binding.rating.setText(String.format("%01.01f", rating));
            binding.maxRating.setText(maxRating);
            binding.ratingVotes.setText(votes);
        } else {
            binding.rating.setVisibility(View.GONE);
            binding.maxRating.setVisibility(View.GONE);
            binding.ratingVotes.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(descriptionPlot)) {
            binding.mediaDescription.setVisibility(View.VISIBLE);
            binding.mediaDescription.setText(UIUtils.applyMarkup(getContext(), descriptionPlot));
        } else {
            binding.mediaDescription.setVisibility(View.GONE);
        }

        UIUtils.setRepeatButton(binding.repeat, getPropertiesResult.repeat);
        binding.shuffle.setHighlight(getPropertiesResult.shuffled);

        Resources resources = requireActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        if (!TextUtils.isEmpty(art)) {
            binding.poster.setVisibility(View.VISIBLE);
            int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
            int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height);

            // If not video, change aspect ration of poster to a square
            boolean isVideo = (getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
                              (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE));
            if (!isVideo) {
                ViewGroup.LayoutParams layoutParams = binding.poster.getLayoutParams();
                layoutParams.height = layoutParams.width;
                binding.poster.setLayoutParams(layoutParams);
                posterHeight = posterWidth;
            }

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 poster, title,
                                                 binding.poster, posterWidth, posterHeight);
            UIUtils.loadImageIntoImageview(hostManager, art, binding.art, displayMetrics.widthPixels, artHeight);

            // Reset padding
            int paddingLeft = resources.getDimensionPixelOffset(R.dimen.poster_width_plus_padding),
                    paddingRight = binding.mediaTitle.getPaddingRight(),
                    paddingTop = binding.mediaTitle.getPaddingTop(),
                    paddingBottom = binding.mediaTitle.getPaddingBottom();
            binding.mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            binding.mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        } else {
            // No fanart, just present the poster
            binding.poster.setVisibility(View.GONE);
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager, poster, title, binding.art, artWidth, artHeight);
            // Reset padding
            int paddingLeft = binding.mediaTitle.getPaddingRight(),
                    paddingRight = binding.mediaTitle.getPaddingRight(),
                    paddingTop = binding.mediaTitle.getPaddingTop(),
                    paddingBottom = binding.mediaTitle.getPaddingBottom();
            binding.mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            binding.mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
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
            binding.overflow.setVisibility(View.VISIBLE);
            binding.castList.setVisibility(View.VISIBLE);

            // Save subtitles and audiostreams list
            availableAudioStreams = getPropertiesResult.audiostreams;
            availableSubtitles = getPropertiesResult.subtitles;
            currentAudiostreamIndex = getPropertiesResult.currentaudiostream.index;
            currentSubtitleIndex = getPropertiesResult.currentsubtitle.index;

            // Cast list
            UIUtils.setupCastInfo(getActivity(), getItemResult.cast, binding.castList,
                                  AllCastActivity.buildLaunchIntent(getActivity(), title,
                                                                    (ArrayList<VideoType.Cast>)getItemResult.cast));
        } else {
            binding.overflow.setVisibility(View.GONE);
            binding.castList.setVisibility(View.GONE);
        }
    }

    /**
     * Cleans up anything left when stop playing
     */
    private void stopNowPlayingInfo() {
        // Just stop the seek bar handler callbacks
        binding.progressInfo.setSpeed(0);

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
        if (panelResId == R.id.info_panel) {
            binding.mediaPanel.setVisibility(View.GONE);
            binding.art.setVisibility(View.GONE);
            binding.includeInfoPanel.infoPanel.setVisibility(View.VISIBLE);
        } else if (panelResId == R.id.media_panel) {
            binding.includeInfoPanel.infoPanel.setVisibility(View.GONE);
            binding.mediaPanel.setVisibility(View.VISIBLE);
            binding.art.setVisibility(View.VISIBLE);
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
