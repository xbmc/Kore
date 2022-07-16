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
 * This can be included in a layout, and on the corresponding activity/fragment call
 * {@link MediaPlaybackBar#setDefaultOnClickListener(Context)} to set a default click
 * listener that just sends each command to the Kodi host without further processing.
 * During playback, call {@link MediaPlaybackBar#setPlaybackState(int, int)} to keep this view updated
 */
public class MediaPlaybackBar extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaPlaybackBar.class);

    MediaPlaybackBarBinding binding;
    int activePlayerId = -1;

    private OnClickListener onClickListener;

    interface OnClickListener {
        void onPlayPauseClicked();
        void onStopClicked();
        void onFastForwardClicked();
        void onRewindClicked();
        void onPreviousClicked();
        void onNextClicked();
    }

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

        binding.play.setOnClickListener(v -> onClickListener.onPlayPauseClicked());
        binding.stop.setOnClickListener(v -> onClickListener.onStopClicked());
        binding.fastForward.setOnClickListener(v -> onClickListener.onFastForwardClicked());
        binding.rewind.setOnClickListener(v -> onClickListener.onRewindClicked());
        binding.previous.setOnClickListener(v -> onClickListener.onPreviousClicked());
        binding.next.setOnClickListener(v -> onClickListener.onNextClicked());
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
     * This sets default actions for each of the buttons, that just send the corresponding action to Kodi
     * @param context Context
     */
    public void setDefaultOnClickListener(Context context) {
        final HostConnection connection = HostManager.getInstance(context).getConnection();
        final Handler callbackHandler = new Handler(Looper.getMainLooper());
        final ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                UIUtils.setPlayPauseButtonIcon(context, binding.play, result == 1);
            }

            @Override
            public void onError(int errorCode, String description) { }
        };

        onClickListener = new OnClickListener() {
            @Override
            public void onPlayPauseClicked() {
                Player.PlayPause action = new Player.PlayPause(activePlayerId);
                action.execute(connection, defaultPlaySpeedChangedCallback, callbackHandler);
            }

            @Override
            public void onStopClicked() {
                Player.Stop action = new Player.Stop(activePlayerId);
                action.execute(connection, null, null);
                UIUtils.setPlayPauseButtonIcon(context, binding.play, false);
            }

            @Override
            public void onFastForwardClicked() {
                Player.SetSpeed action = new Player.SetSpeed(activePlayerId, GlobalType.IncrementDecrement.INCREMENT);
                action.execute(connection, defaultPlaySpeedChangedCallback, callbackHandler);
            }

            @Override
            public void onRewindClicked() {
                Player.SetSpeed action = new Player.SetSpeed(activePlayerId, GlobalType.IncrementDecrement.DECREMENT);
                action.execute(connection, defaultPlaySpeedChangedCallback, callbackHandler);
            }

            @Override
            public void onPreviousClicked() {
                Player.GoTo action = new Player.GoTo(activePlayerId, Player.GoTo.PREVIOUS);
                action.execute(connection, null, null);
            }

            @Override
            public void onNextClicked() {
                Player.GoTo action = new Player.GoTo(activePlayerId, Player.GoTo.NEXT);
                action.execute(connection, null, null);
            }
        };
    }

    /**
     * Update the playback state
     * @param activePlayerId Current active player id
     * @param speed Playback speed
     */
    public void setPlaybackState(int activePlayerId, int speed) {
        this.activePlayerId = activePlayerId;
        UIUtils.setPlayPauseButtonIcon(getContext(), binding.play, speed == 1);
    }


}
