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

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.VolumeLevelIndicatorBinding;
import org.xbmc.kore.utils.LogUtils;

public class VolumeLevelIndicator extends LinearLayout {
    public static final String TAG = LogUtils.makeLogTag(VolumeLevelIndicator.class);

    VolumeLevelIndicatorBinding binding;

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
        binding = null;
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = VolumeLevelIndicatorBinding.inflate(inflater, this);

        binding.vliSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    binding.vliVolumeText.setText(String.valueOf(progress));
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
     * @param muted Mute
     * @param volume Volume
     */
    public void setVolume(boolean muted, int volume) {
        if (muted) {
            binding.vliVolumeText.setText(R.string.muted);
            binding.vliSeekBar.setProgress(0);
        } else {
            binding.vliVolumeText.setText(String.valueOf(volume));
            binding.vliSeekBar.setProgress(volume);
        }
    }
}
