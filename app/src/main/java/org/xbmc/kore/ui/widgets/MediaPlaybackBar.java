package org.xbmc.kore.ui.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import org.xbmc.kore.databinding.MediaPlaybackBarBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * This presents a row of buttons that allow for media control
 * The buttons are (in order) previous, rewind, play/pause, forward, next
 * Include this in a layout and the buttons work as expected, sending each command to Kodi without further processing.
 * During playback this view needs to be manually updated, by calling {@link MediaPlaybackBar#setPlaybackState(int, int)}
 * when the playback state changes on Kodi. This view could be auto-suficient by subscribing to
 * {@link org.xbmc.kore.host.HostConnectionObserver} and keeping itself updated when the state changes, but given
 * that clients of this view are most likely already subscribers of that, this prevents the proliferation of observers
 */
public class MediaPlaybackBar extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaPlaybackBar.class);

    MediaPlaybackBarBinding binding;
    int activePlayerId = -1;

    public MediaPlaybackBar(Context context) {
        super(context);
        initializeView(context);
    }

    public MediaPlaybackBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeView(context);
    }

    public MediaPlaybackBar(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        initializeView(context);
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = MediaPlaybackBarBinding.inflate(inflater, this);

        final HostConnection connection = HostManager.getInstance(context).getConnection();
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

    /**
     * Update the playback state. This needs to be called when the playback state of Kodi changes
     * @param activePlayerId Current active player id
     * @param speed Playback speed
     */
    public void setPlaybackState(int activePlayerId, int speed) {
        this.activePlayerId = activePlayerId;
        UIUtils.setPlayPauseButtonIcon(getContext(), binding.play, speed == 1);
    }
}
