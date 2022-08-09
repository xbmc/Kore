package org.xbmc.kore.ui.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.MediaActionsBarBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.ui.generic.GenericSelectDialog;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.List;

/**
 * This presents a row of buttons that allow for media actions, namely:
 * mute, sound level control, repeat, shuffle, audiostreams and subtitles
 * Include this in a layout and the buttons work as expected, sending each command to Kodi without further processing.
 * Note that the action for the "audiostream" and "subtitles" is to show a UI Dialog with the available audiostreams
 * or subtitles, so this class needs a reference to the calling Fragment to show that Dialog, which should be provided
 * by calling {@link MediaActionsBar#completeSetup} in the Fragment/Activity onCreateView.
 * During playback this view needs to be manually updated, by calling {@link MediaActionsBar#setPlaybackState}
 * when the playback state changes on Kodi. This view could be auto-suficient by subscribing to
 * {@link org.xbmc.kore.host.HostConnectionObserver} and keeping itself updated when the state changes, but given
 * that clients of this view are most likely already subscribers of that, this prevents the proliferation of observers
 */
public class MediaActionsBar extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaActionsBar.class);

    MediaActionsBarBinding binding;

    int activePlayerId = -1;
    String activePlayerType = PlayerType.GetActivePlayersReturnType.AUDIO;
    // List of available subtitles and audiostremas to show the user
    private List<PlayerType.Subtitle> availableSubtitles;
    private List<PlayerType.AudioStream> availableAudioStreams;

    public MediaActionsBar(Context context) {
        super(context);
        initializeView(context);
    }

    public MediaActionsBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeView(context);
    }

    public MediaActionsBar(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        initializeView(context);
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = MediaActionsBarBinding.inflate(inflater, this);
        updateMutableButtons();
    }

    private void updateMutableButtons() {
        if (activePlayerType.equals(PlayerType.GetActivePlayersReturnType.VIDEO)) {
            binding.repeat.setVisibility(View.GONE);
            binding.shuffle.setVisibility(View.GONE);
            binding.partyMode.setVisibility(View.GONE);
            binding.audiostreams.setVisibility(View.VISIBLE);
            binding.subtitles.setVisibility(View.VISIBLE);
            binding.overflow.setVisibility(View.VISIBLE);
        } else {
            binding.repeat.setVisibility(View.VISIBLE);
            binding.shuffle.setVisibility(View.VISIBLE);
            binding.partyMode.setVisibility(View.VISIBLE);
            binding.audiostreams.setVisibility(View.GONE);
            binding.subtitles.setVisibility(View.GONE);
            binding.overflow.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        binding = null;
    }

    /**
     * This completes the View setup. This needs to be called explicitly because some options, namely the audiostreams
     * and subtitles options show the user a Dialog which needs a FragmentManager to display, and that needs to be
     * supplied by the enclosing Activity/Fragment
     * @param context Context
     * @param fragmentManager FragmentManager needed to show a Dialog
     */
    public void completeSetup(final Context context, final FragmentManager fragmentManager) {
        final HostConnection connection = HostManager.getInstance(context).getConnection();
        final HostConnectionObserver hostConnectionObserver = HostManager.getInstance(context).getHostConnectionObserver();
        final Handler callbackHandler = new Handler(Looper.getMainLooper());
        // Callback that forces a refresh of what's playing
        final ApiCallback<String> forceRefreshCallback = new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) { hostConnectionObserver.refreshWhatsPlaying(); }

            @Override
            public void onError(int errorCode, String description) { }
        };
        // Callback that switches to the remore screen
        final ApiCallback<String> switchToRemoteCallback = new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {/* nowPlayingListener.SwitchToRemotePanel();*/}

            @Override
            public void onError(int errorCode, String description) { }
        };

        // Number of explicitly added options for audio and subtitles (to subtract from the number returned by Kodi)
        final int ADDED_AUDIO_OPTIONS = 1, ADDED_SUBTITLE_OPTIONS = 3;
        final int SELECT_AUDIOSTREAM = 0, SELECT_SUBTITLES = 1;

        // Dialog listener that reacts to the users selection after being shown the Audiostreams/Subtitles Dialog
        final GenericSelectDialog.GenericSelectDialogListener dialogListener = (token, which) -> {
            switch (token) {
                case SELECT_AUDIOSTREAM:
                    // 0 is to sync audio, other is for a specific audiostream
                    switch (which) {
                        case 0:
                            new Input.ExecuteAction(Input.ExecuteAction.AUDIODELAY)
                                    .execute(connection, switchToRemoteCallback, callbackHandler);
                            break;
                        default:
                            new Player.SetAudioStream(activePlayerId, which - ADDED_AUDIO_OPTIONS)
                                    .execute(connection, null, null);
                            break;
                    }
                    break;
                case SELECT_SUBTITLES:
                    // 0 is to download subtitles, 1 is for sync, 2 is for none, other is for a specific subtitle index
                    switch (which) {
                        case 0:
                            ApiCallback<String> subtitlesDownloadCallback = new ApiCallback<String>() {
                                @Override
                                public void onSuccess(String result) {/* nowPlayingListener.SwitchToRemotePanel();*/}

                                @Override
                                public void onError(int errorCode, String description) {
                                    Toast.makeText(getContext(),
                                                   getResources().getString(R.string.error_executing_subtitles, description),
                                                   Toast.LENGTH_SHORT)
                                         .show();
                                }
                            };

                            // Download subtitles. First check host version to see which method to call
                            HostInfo hostInfo = HostManager.getInstance(getContext()).getHostInfo();
                            if (hostInfo.isGothamOrLater()) {
                                // Post-Gotham - Hack. Apparently Gui.ActivateWindow with subtitlesearch blocks the TCP listener
                                // thread on XBMC While the subtitles windows is showing, i get no response to any call. See:
                                // http://forum.xbmc.org/showthread.php?tid=198156
                                // Forcing this call through HTTP works, as it doesn't block the TCP listener thread on XBMC
                                HostInfo currentHostInfo = HostManager.getInstance(getContext()).getHostInfo();
                                HostConnection httpHostConnection = new HostConnection(currentHostInfo);
                                httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);
                                new GUI.ActivateWindow(GUI.ActivateWindow.SUBTITLESEARCH)
                                        .execute(httpHostConnection, subtitlesDownloadCallback, callbackHandler);
                            } else {
                                // Pre-Gotham
                                new Addons.ExecuteAddon(Addons.ExecuteAddon.ADDON_SUBTITLES)
                                        .execute(connection, subtitlesDownloadCallback, callbackHandler);
                            }
                            break;
                        case 1:
                            new Input.ExecuteAction(Input.ExecuteAction.SUBTITLEDELAY)
                                    .execute(connection, switchToRemoteCallback, callbackHandler);
                            break;
                        case 2:
                            new Player.SetSubtitle(activePlayerId, Player.SetSubtitle.OFF, true)
                                    .execute(connection, forceRefreshCallback, callbackHandler);
                            break;
                        default:
                            new Player.SetSubtitle(activePlayerId, which - ADDED_SUBTITLE_OPTIONS, true)
                                    .execute(connection, forceRefreshCallback, callbackHandler);
                            break;
                    }
                    break;
            }
        };

        // Default actions: send to Kodi or show the popupmenu
        binding.volumeLevelIndicator.setOnVolumeChangeListener(volume -> {
            new Application.SetVolume(volume)
                    .execute(connection, null, null);
        });
        binding.volumeMute.setOnClickListener(v -> {
            new Application.SetMute()
                    .execute(connection, null, null);
        });
        binding.repeat.setOnClickListener(v -> {
            onRepeatClicked(connection, forceRefreshCallback, callbackHandler);
        });
        binding.shuffle.setOnClickListener(v -> {
            onShuffleClicked(connection, forceRefreshCallback, callbackHandler);
        });
        binding.audiostreams.setOnClickListener(v -> {
            // Setup audiostream select dialog
            String[] audiostreams = new String[(availableAudioStreams != null) ?
                                               availableAudioStreams.size() + ADDED_AUDIO_OPTIONS : ADDED_AUDIO_OPTIONS];
            audiostreams[0] = getResources().getString(R.string.audio_sync);
            if (availableAudioStreams != null) {
                for (int i = 0; i < availableAudioStreams.size(); i++) {
                    PlayerType.AudioStream current = availableAudioStreams.get(i);
                    audiostreams[i + ADDED_AUDIO_OPTIONS] = TextUtils.isEmpty(current.language) || current.language.equals("und") ?
                                                            current.name : current.language + " | " + current.name;
                }
            }
            GenericSelectDialog.newInstance(dialogListener,
                                            SELECT_AUDIOSTREAM,
                                            getResources().getString(R.string.audiostreams),
                                            audiostreams, -1)
                               .show(fragmentManager, null);
        });
        binding.subtitles.setOnClickListener(v -> {
            String[] subtitles = new String[(availableSubtitles != null) ?
                                            availableSubtitles.size() + ADDED_SUBTITLE_OPTIONS : ADDED_SUBTITLE_OPTIONS];

            subtitles[0] = getResources().getString(R.string.download_subtitle);
            subtitles[1] = getResources().getString(R.string.subtitle_sync);
            subtitles[2] = getResources().getString(R.string.none);

            if (availableSubtitles != null) {
                for (int i = 0; i < availableSubtitles.size(); i++) {
                    PlayerType.Subtitle current = availableSubtitles.get(i);
                    subtitles[i + ADDED_SUBTITLE_OPTIONS] = TextUtils.isEmpty(current.language) ?
                                                            current.name : current.language + " | " + current.name;
                }
            }

            GenericSelectDialog.newInstance(dialogListener,
                                            SELECT_SUBTITLES,
                                            getResources().getString(R.string.subtitles),
                                            subtitles, -1)
                               .show(fragmentManager, null);
        });
        binding.partyMode.setOnClickListener(v -> {
            new Player.SetPartymode(activePlayerId)
                    .execute(connection, forceRefreshCallback, callbackHandler);
        });
        binding.overflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, binding.overflow);
            popup.inflate(R.menu.actions_overflow_video);
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.repeat) {
                    onRepeatClicked(connection, forceRefreshCallback, callbackHandler);
                } else if (item.getItemId() == R.id.shuffle) {
                    onShuffleClicked(connection, forceRefreshCallback, callbackHandler);
                }
                return true;
            });
            popup.show();
        });
    }

    private void onRepeatClicked(HostConnection connection, ApiCallback<String> forceRefreshCallback, Handler callbackHandler) {
        new Player.SetRepeat(activePlayerId, PlayerType.Repeat.CYCLE)
                .execute(connection, forceRefreshCallback, callbackHandler);
    }

    private void onShuffleClicked(HostConnection connection, ApiCallback<String> forceRefreshCallback, Handler callbackHandler) {
        new Player.SetShuffle(activePlayerId)
                .execute(connection, forceRefreshCallback, callbackHandler);
    }

    /**
     * Updates the playback state. Important to keep the default Listener working properly
     */
    public void setPlaybackState(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                 PlayerType.PropertyValue getPropertiesResult) {
        this.activePlayerId = getActivePlayerResult.playerid;
        this.activePlayerType = getActivePlayerResult.type;
        this.availableSubtitles = getPropertiesResult.subtitles;
        this.availableAudioStreams = getPropertiesResult.audiostreams;

        updateMutableButtons();

        if (activePlayerType.equals(PlayerType.GetActivePlayersReturnType.VIDEO)) {
            binding.subtitles.setHighlight(getPropertiesResult.subtitleenabled);
        } else {
            setRepeatShuffleState(getPropertiesResult.repeat, getPropertiesResult.shuffled, getPropertiesResult.partymode);
        }
    }

    /**
     * Call when volume state changes, to update the UI
     */
    public void setVolumeState(int volume, boolean muted) {
        binding.volumeLevelIndicator.setVolume(muted, volume);
        binding.volumeMute.setHighlight(muted);
    }

    /**
     * Call when repeat/shuffle state changes, to update the UI
     */
    public void setRepeatShuffleState(String repeatMode, Boolean shuffled, Boolean partymode) {
        if (activePlayerType.equals(PlayerType.GetActivePlayersReturnType.VIDEO)) return;

        if (repeatMode != null) binding.repeat.setMode(repeatMode);
        if (shuffled != null) binding.shuffle.setHighlight(shuffled);
        if (partymode != null) binding.partyMode.setHighlight(partymode);
    }
}
