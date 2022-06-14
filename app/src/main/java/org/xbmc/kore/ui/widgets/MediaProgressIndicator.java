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
import org.xbmc.kore.utils.UIUtils;

public class MediaProgressIndicator extends LinearLayout {

    MediaProgressIndicatorBinding binding;

    private int speed = 0;
    private int maxProgress;
    private int progress;
    private static final int SEEK_BAR_UPDATE_INTERVAL = 1000; // ms
    private int progressIncrement;

    private OnProgressChangeListener onProgressChangeListener;

    public interface OnProgressChangeListener {
        void onProgressChanged(int progress);
    }

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

        binding.mpiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MediaProgressIndicator.this.progress = progress;

                    binding.mpiProgress.setText(UIUtils.formatTime(MediaProgressIndicator.this.progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Stop the seekbar from updating
                seekBar.removeCallbacks(seekBarUpdater);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onProgressChangeListener != null)
                    onProgressChangeListener.onProgressChanged(seekBar.getProgress());

                if (speed > 0)
                    seekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        binding.mpiSeekBar.removeCallbacks(seekBarUpdater);
        onProgressChangeListener = null;
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
                binding.mpiSeekBar.removeCallbacks(this);
                return;
            }

            progress += progressIncrement;
            setProgress(progress);

            binding.mpiSeekBar.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
        }
    };

    public void setOnProgressChangeListener(OnProgressChangeListener onProgressChangeListener) {
        this.onProgressChangeListener = onProgressChangeListener;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        binding.mpiSeekBar.setProgress(progress);
        binding.mpiProgress.setText(UIUtils.formatTime(progress));
    }

    public int getProgress() {
        return progress;
    }

    public void setMaxProgress(int max) {
        maxProgress = max;
        binding.mpiSeekBar.setMax(max);
        binding.mpiDuration.setText(UIUtils.formatTime(max));
    }

    /**
     * Sets the play speed for the progress indicator
     * @param speed set to 0 to stop updating the progress indicator.
     */
    public void setSpeed(int speed) {
        if( speed == this.speed )
            return;

        this.speed = speed;
        this.progressIncrement = speed * (SEEK_BAR_UPDATE_INTERVAL/1000);

        binding.mpiSeekBar.removeCallbacks(seekBarUpdater);
        if (speed > 0)
            binding.mpiSeekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
    }

    private static class SavedState extends BaseSavedState {
        int progress;
        int maxProgress;

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