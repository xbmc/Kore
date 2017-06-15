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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.UIUtils;

public class MediaProgressIndicator extends LinearLayout {

    private SeekBar seekBar;
    private TextView durationTextView;
    private TextView progressTextView;
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

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.media_progress_indicator, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        seekBar = (SeekBar) findViewById(R.id.mpi_seek_bar);
        progressTextView = (TextView) findViewById(R.id.mpi_progress);
        durationTextView = (TextView) findViewById(R.id.mpi_duration);

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
                if (onProgressChangeListener != null)
                    onProgressChangeListener.onProgressChanged(seekBar.getProgress());

                if (speed > 0)
                    seekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
            }
        });
    }

    private Runnable seekBarUpdater = new Runnable() {
        @Override
        public void run() {
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
}