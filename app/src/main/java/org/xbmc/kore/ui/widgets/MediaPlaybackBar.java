package org.xbmc.kore.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.MediaPlaybackBarBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * This presents a row of buttons that allow for media control
 * The buttons are (in order) previous, rewind, play/pause, forward, next
 * Include this in a layout and the buttons work as expected, sending each command to Kodi without further processing.
 * During playback this view needs to be manually updated, by calling {@link MediaPlaybackBar#setPlaybackState}
 * when the playback state changes on Kodi. This view could be auto-suficient by subscribing to
 * {@link org.xbmc.kore.host.HostConnectionObserver} and keeping itself updated when the state changes, but given
 * that clients of this view are most likely already subscribers of that, this prevents the proliferation of observers
 */
public class MediaPlaybackBar extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaPlaybackBar.class);

    private static final int VIEW_MODE_FULL = 0,
            VIEW_MODE_NO_STOP_BUTTON = 1,
            VIEW_MODE_SINGLE_MOVEMENT_BUTTONS = 2;

    HostConnection connection;
    MediaPlaybackBarBinding binding;
    int activePlayerId = -1;
    String activePlayerType = PlayerType.GetActivePlayersReturnType.VIDEO;
    int viewMode;

    public MediaPlaybackBar(Context context) {
        this(context, null);
    }

    public MediaPlaybackBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MediaPlaybackBar(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        if (isInEditMode()) return;

        TypedArray a = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.MediaPlaybackBar, 0, 0);
        try {
            viewMode = a.getInt(R.styleable.MediaPlaybackBar_view_mode, VIEW_MODE_FULL);
        } finally {
            a.recycle();
        }
        initializeView(context);
        updateViewMode();
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = MediaPlaybackBarBinding.inflate(inflater, this);

        connection = HostManager.getInstance(context).getConnection();
        binding.play.setOnClickListener(v -> {
            new Player.PlayPause(activePlayerId)
                    .execute(connection, null, null);
        });
        binding.stop.setOnClickListener(v -> {
            new Player.Stop(activePlayerId)
                    .execute(connection, null, null);
        });
        binding.fastForward.setOnClickListener(v -> {
            new Player.SetSpeed(activePlayerId, GlobalType.IncrementDecrement.INCREMENT)
                    .execute(connection, null, null);
        });
        binding.rewind.setOnClickListener(v -> {
            new Player.SetSpeed(activePlayerId, GlobalType.IncrementDecrement.DECREMENT)
                    .execute(connection, null, null);
        });
        binding.previous.setOnClickListener(v -> {
            new Player.GoTo(activePlayerId, Player.GoTo.PREVIOUS)
                    .execute(connection, null, null);
        });
        binding.next.setOnClickListener(v -> {
            new Player.GoTo(activePlayerId, Player.GoTo.NEXT)
                    .execute(connection, null, null);
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        binding = null;
    }

    public int getViewMode() { return viewMode; }

    public void setViewMode(int viewMode) {
        this.viewMode = viewMode;
        updateViewMode();
        invalidate();
        requestLayout();
    }

    private void updateViewMode() {
        binding.stop.setVisibility(View.VISIBLE);
        binding.next.setVisibility(View.VISIBLE);
        binding.previous.setVisibility(View.VISIBLE);
        binding.fastForward.setVisibility(View.VISIBLE);
        binding.rewind.setVisibility(View.VISIBLE);

        if (viewMode == VIEW_MODE_NO_STOP_BUTTON) {
            binding.stop.setVisibility(View.GONE);
        } else if (viewMode == VIEW_MODE_SINGLE_MOVEMENT_BUTTONS) {
            if (activePlayerType.equals(PlayerType.GetActivePlayersReturnType.VIDEO)) {
                binding.next.setVisibility(View.GONE);
                binding.previous.setVisibility(View.GONE);
            } else {
                binding.fastForward.setVisibility(View.GONE);
                binding.rewind.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Update the playback state. This needs to be called when the playback state of Kodi changes
     * @param getActivePlayersResult Current active player id
     * @param speed Playback speed
     */
    public void setPlaybackState(PlayerType.GetActivePlayersReturnType getActivePlayersResult, int speed) {
        this.activePlayerId = getActivePlayersResult.playerid;
        this.activePlayerType = getActivePlayersResult.type;
        updateViewMode();
        UIUtils.setPlayPauseButtonIcon(getContext(), binding.play, speed == 1);
    }
}
