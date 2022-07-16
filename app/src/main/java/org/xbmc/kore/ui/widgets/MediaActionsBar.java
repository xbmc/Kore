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
 * This can be included in a layout, and on the corresponding activity/fragment call
 * {@link MediaActionsBar#setOnClickListener(OnClickListener)} to set a specific {@link OnClickListener} for the
 * buttons, or call {@link MediaActionsBar#setDefaultOnClickListener(Context, FragmentManager)} to set a default click
 * listener that just sends each command to the Kodi host without further processing. Note that the default action
 * for the "audiostream" and "subtitles" is to show a UI Dialog with the available audiostreams or subtitles, so
 * this class needs a reference to the calling Fragment to show that Dialog. It also needs to be kept updated
 * during playback, by calling {@link MediaActionsBar#setPlaybackState(PlayerType.GetActivePlayersReturnType, PlayerType.PropertyValue)}
 */
public class MediaActionsBar extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaActionsBar.class);

    HostConnection connection;

    MediaActionsBarBinding binding;

    int activePlayerId = -1;
    String activePlayerType = PlayerType.GetActivePlayersReturnType.AUDIO;
    // List of available subtitles and audiostremas to show the user
    private List<PlayerType.Subtitle> availableSubtitles;
    private List<PlayerType.AudioStream> availableAudioStreams;

    private OnClickListener onClickListener;

    interface OnClickListener {
        void onVolumeChangeListener(int volume);

        void onVolumeMuteClicked();
        void onShuffleClicked();
        void onRepeatClicked();
        void onAudiostreamsClicked();
        void onSubtitlesClicked();
        void onPartyModeClicked();
    }

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

        connection = HostManager.getInstance(context).getConnection();

        binding.volumeLevelIndicator.setOnVolumeChangeListener(volume -> onClickListener.onVolumeChangeListener(volume));
        binding.volumeMute.setOnClickListener(v -> onClickListener.onVolumeMuteClicked());
        binding.repeat.setOnClickListener(v -> onClickListener.onRepeatClicked());
        binding.shuffle.setOnClickListener(v -> onClickListener.onShuffleClicked());
        binding.audiostreams.setOnClickListener(v -> onClickListener.onAudiostreamsClicked());
        binding.subtitles.setOnClickListener(v -> onClickListener.onSubtitlesClicked());
        binding.partyMode.setOnClickListener(v -> onClickListener.onPartyModeClicked());
        binding.overflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, binding.overflow);
            popup.inflate(R.menu.actions_overflow_video);
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.repeat) {
                    onClickListener.onRepeatClicked();
                } else if (item.getItemId() == R.id.shuffle) {
                    onClickListener. onShuffleClicked();
                }
                return true;
            });
            popup.show();
        });

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
        onClickListener = null;
    }

    public void setOnClickListener(OnClickListener listener) {
        onClickListener = listener;
    }

    /**
     * This sets default actions for each of the buttons, that sends the corresponding action to Kodi or,
     * for the audiostreams/subtitles options, shows the user a Dialog with the available audiostreams/subtitles,
     * and reacts according to its selection
     * @param context Context
     * @param fragmentManager FragmentManager needed to show a Dialog
     */
    public void setDefaultOnClickListener(final Context context, final FragmentManager fragmentManager) {
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
            public void onSuccess(String result) {
                // nowPlayingListener.SwitchToRemotePanel();
            }

            @Override
            public void onError(int errorCode, String description) { }
        };

        // Number of explicitly added options for audio and subtitles (to subtract from the number returned by Kodi)
        final int ADDED_AUDIO_OPTIONS = 1, ADDED_SUBTITLE_OPTIONS = 3;
        final int SELECT_AUDIOSTREAM = 0, SELECT_SUBTITLES = 1;

        // Dialog listener that reacts to the users selection after being shown the Audiostreams/Subtitles Dialog
        final GenericSelectDialog.GenericSelectDialogListener dialogListener = new GenericSelectDialog.GenericSelectDialogListener() {
            @Override
            public void onDialogSelect(int token, int which) {
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
                                // Download subtitles. First check host version to see which method to call
                                HostInfo hostInfo = HostManager.getInstance(getContext()).getHostInfo();
                                if (hostInfo.isGothamOrLater()) {
                                    showDownloadSubtitlesPostGotham();
                                } else {
                                    showDownloadSubtitlesPreGotham();
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
            }

            private void showDownloadSubtitlesPreGotham() {
                // Pre-Gotham
                Addons.ExecuteAddon action = new Addons.ExecuteAddon(Addons.ExecuteAddon.ADDON_SUBTITLES);
                action.execute(connection, new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        // Notify enclosing activity to switch panels
                        // nowPlayingListener.SwitchToRemotePanel();
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        Toast.makeText(getContext(),
                                       getResources().getString(R.string.error_executing_subtitles, description),
                                       Toast.LENGTH_SHORT)
                             .show();
                    }
                }, callbackHandler);
            }

            private void showDownloadSubtitlesPostGotham() {
                // Post-Gotham - Hack. Apparently Gui.ActivateWindow with subtitlesearch blocks the TCP listener
                // thread on XBMC While the subtitles windows is showing, i get no response to any call. See:
                // http://forum.xbmc.org/showthread.php?tid=198156
                // Forcing this call through HTTP works, as it doesn't block the TCP listener thread on XBMC
                HostInfo currentHostInfo = HostManager.getInstance(getContext()).getHostInfo();
                HostConnection httpHostConnection = new HostConnection(currentHostInfo);
                httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

                GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.SUBTITLESEARCH);
                action.execute(httpHostConnection, null, null);
                // Notify enclosing activity to switch panels
                // nowPlayingListener.SwitchToRemotePanel();
            }
        };

        // Default actions: send to Kodi or show the popupmenu
        onClickListener = new OnClickListener() {
            @Override
            public void onVolumeChangeListener(int volume) {
                new Application.SetVolume(volume).execute(connection, null, null);
            }

            @Override
            public void onVolumeMuteClicked() {
                new Application.SetMute().execute(connection, null, null);
            }

            @Override
            public void onShuffleClicked() {
                // Force a refresh
                new Player.SetShuffle(activePlayerId).execute(connection, forceRefreshCallback, callbackHandler);
            }

            @Override
            public void onRepeatClicked() {
                new Player.SetRepeat(activePlayerId, PlayerType.Repeat.CYCLE)
                        .execute(connection, forceRefreshCallback, callbackHandler);
            }

            @Override
            public void onPartyModeClicked() {
                new Player.SetPartymode(activePlayerId)
                        .execute(connection, forceRefreshCallback, callbackHandler);
            }

            @Override
            public void onAudiostreamsClicked() {
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
            }

            @Override
            public void onSubtitlesClicked() {
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
            }
        };
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

        if (repeatMode != null) UIUtils.setRepeatButton(binding.repeat, repeatMode);
        if (shuffled != null) binding.shuffle.setHighlight(shuffled);
        if (partymode != null) binding.partyMode.setHighlight(partymode);
    }
}
