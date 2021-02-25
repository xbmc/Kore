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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.Unbinder;

public class MediaProgressIndicator extends LinearLayout {

    @BindView(R.id.mpi_seek_bar) SeekBar seekBar;
    @BindView(R.id.mpi_duration) TextView durationTextView;
    @BindView(R.id.mpi_progress) TextView progressTextView;

    private Unbinder unbinder;
    private int speed = 0;
    private int maxProgress;
    private int progress;
    private static final int SEEK_BAR_UPDATE_INTERVAL = 1000; // ms
    private int progressIncrement;

    private OnProgressChangeListener onProgressChangeListener;

    public interface OnProgressChangeListener {
        void onProgressChanged(Double percentage);
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
        View view = inflater.inflate(R.layout.media_progress_indicator, this);

        unbinder = ButterKnife.bind(this, view);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MediaProgressIndicator.this.progress = progress;

                    progressTextView.setText(UIUtils.formatTime(MediaProgressIndicator.this.progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Stop the seekbar from updating
                seekBar.removeCallbacks(seekBarUpdater);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onProgressChangeListener != null){
                    int progress = seekBar.getProgress();
                    int max = seekBar.getMax();
                    float fPercentage = (((float)progress/(float)max) * 100);
                    Double percentage = new Double(fPercentage);
                    onProgressChangeListener.onProgressChanged(percentage);
                }
                if (speed > 0)
                    seekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        seekBar.removeCallbacks(seekBarUpdater);

        unbinder.unbind();

        onProgressChangeListener = null;
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

    private Runnable seekBarUpdater = new Runnable() {
        @Override
        public void run() {
            if (seekBar == null) // prevent NPE when Butterknife unbinds the view while there was still a runnable pending
                return;

            if ((maxProgress == 0) || (progress >= maxProgress)) {
                seekBar.removeCallbacks(this);
                return;
            }

            progress += progressIncrement;
            setProgress(progress);

            seekBar.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
        }
    };

    public void setOnProgressChangeListener(OnProgressChangeListener onProgressChangeListener) {
        this.onProgressChangeListener = onProgressChangeListener;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        seekBar.setProgress(progress);
        progressTextView.setText(UIUtils.formatTime(progress));
    }

    public int getProgress() {
        return progress;
    }

    public void setMaxProgress(int max) {
        maxProgress = max;
        seekBar.setMax(max);
        durationTextView.setText(UIUtils.formatTime(max));
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

        seekBar.removeCallbacks(seekBarUpdater);
        if (speed > 0)
            seekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
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