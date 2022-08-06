/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.ui.widgets;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import org.xbmc.kore.databinding.MediaProgressIndicatorBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * This presents a seekbar with the playback progress, allowing for touch and dragging it and sending a seeking
 * action to Kodi. Include this in a layout and, during playback keep this view manually updated by calling
 * {@link MediaProgressIndicator#setPlaybackState(int, int, int, int)} and {@link MediaProgressIndicator#stopUpdating()}
 * This view could be auto-suficient by subscribing to {@link org.xbmc.kore.host.HostConnectionObserver} and keeping
 * itself updated when the state changes, but given that clients of this view are most likely already subscribers of
 * that, this prevents the proliferation of observers
 */
public class MediaProgressIndicator extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaProgressIndicator.class);

    MediaProgressIndicatorBinding binding;

    private int speed = 0;
    private int maxProgress;
    private int progress;
    private static final int SEEK_BAR_UPDATE_INTERVAL = 1000; // ms
    private int progressIncrement;
    private int activePlayerId;

    public MediaProgressIndicator(Context context) {
        super(context);
        initializeView(context);
    }

    public MediaProgressIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeView(context);
    }

    public MediaProgressIndicator(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        initializeView(context);
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = MediaProgressIndicatorBinding.inflate(inflater, this);

        if (this.isInEditMode()) return;

        final HostConnection connection = HostManager.getInstance(context).getConnection();
        binding.mpiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int newProgress, boolean fromUser) {
                if (fromUser) {
                    progress = newProgress;
                    binding.mpiProgress.setText(UIUtils.formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Stop the seekbar from updating
                seekBar.removeCallbacks(seekBarUpdater);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progress = seekBar.getProgress();
                new Player.Seek(activePlayerId, new PlayerType.PositionTime(progress))
                        .execute(connection, null, null);
                if (speed > 0)
                    seekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        binding.mpiSeekBar.removeCallbacks(seekBarUpdater);
        binding = null;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.progress = progress;
        savedState.maxProgress = maxProgress;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        progress = savedState.progress;
        maxProgress = savedState.maxProgress;
        setProgress(progress);
        setMaxProgress(maxProgress);
    }

    private final Runnable seekBarUpdater = new Runnable() {
        @Override
        public void run() {
            if (binding == null)
                return;

            if ((maxProgress == 0) || (progress >= maxProgress)) {
                setSpeed(0);
                return;
            }

            progress += progressIncrement;
            setProgress(progress);

            binding.mpiSeekBar.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
        }
    };

    /**
     * Set the current playback state, adjusting the UI and the internal state
     * Call this whenever the playback state changes
     *
     * @param activePlayerId Active player id
     * @param speed          Current speed. Set to 0 to stop updating the progress indicator
     * @param progress       Current progress
     * @param maxProgress    Max progress
     */
    public void setPlaybackState(int activePlayerId, int speed, int progress, int maxProgress) {
        this.activePlayerId = activePlayerId;
        setProgress(progress);
        setMaxProgress(maxProgress);
        setSpeed(speed);
    }

    private void setProgress(int progress) {
        this.progress = progress;
        binding.mpiSeekBar.setProgress(progress);
        binding.mpiProgress.setText(UIUtils.formatTime(progress));
    }

    private void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
        binding.mpiSeekBar.setMax(maxProgress);
        binding.mpiDuration.setText(UIUtils.formatTime(maxProgress));
    }

    private void setSpeed(int speed) {
        if (speed == this.speed) return;

        this.speed = speed;
        this.progressIncrement = speed * (SEEK_BAR_UPDATE_INTERVAL / 1000);

        binding.mpiSeekBar.removeCallbacks(seekBarUpdater);
        if (speed > 0)
            binding.mpiSeekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
    }

    public void stopUpdating() {
        setSpeed(0);
    }

    /**
     * To prevent some flickering on the progress bar, save the current progress and max progress
     * The other attributes aren't needed, as they should be updated by a subsequent call to setPlaybackState
     */
    private static class SavedState extends BaseSavedState {
        int progress, maxProgress;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            maxProgress = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(maxProgress);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}