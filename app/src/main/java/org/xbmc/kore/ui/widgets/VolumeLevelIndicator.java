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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.LogUtils;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.Unbinder;

public class VolumeLevelIndicator extends LinearLayout {
    public static final String TAG = LogUtils.makeLogTag(VolumeLevelIndicator.class);

    @BindView(R.id.vli_seek_bar) SeekBar volumeSeekBar;
    @BindView(R.id.vli_volume_text) TextView volumeTextView;

    private OnVolumeChangeListener onVolumeChangeListener;
    private VolumeBarTouchTrackerListener volumeBarTouchTrackerListener;

    public interface OnVolumeChangeListener {
        void onVolumeChanged(int volume);
    }

    public interface VolumeBarTouchTrackerListener {
        void onStartTrackingTouch();
        void onStopTrackingTouch();
    }

    public VolumeLevelIndicator(Context context) {
        super(context);
        initializeView(context);
    }

    public VolumeLevelIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeView(context);
    }

    public VolumeLevelIndicator(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        initializeView(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onVolumeChangeListener = null;
        volumeBarTouchTrackerListener = null;
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.volume_level_indicator, this);
        ButterKnife.bind(view);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    volumeTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (volumeBarTouchTrackerListener != null) {
                    volumeBarTouchTrackerListener.onStartTrackingTouch();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onVolumeChangeListener != null) {
                    onVolumeChangeListener.onVolumeChanged(seekBar.getProgress());
                    if (volumeBarTouchTrackerListener != null) {
                        volumeBarTouchTrackerListener.onStopTrackingTouch();
                    }
                }
            }
        });
    }

    public void setOnVolumeChangeListener(OnVolumeChangeListener onVolumeChangeListener) {
        this.onVolumeChangeListener = onVolumeChangeListener;
    }

    public void setVolumeBarTouchTrackerListener(
            VolumeBarTouchTrackerListener volumeBarTouchTrackerListener) {
        this.volumeBarTouchTrackerListener = volumeBarTouchTrackerListener;
    }

    /**
     * Sets UI volume state
     * @param muted
     * @param volume
     */
    public void setVolume(boolean muted, int volume) {
        if (muted) {
            volumeTextView.setText(R.string.muted);
            volumeSeekBar.setProgress(0);
        } else {
            volumeTextView.setText(String.valueOf(volume));
            volumeSeekBar.setProgress(volume);
        }
    }
}
