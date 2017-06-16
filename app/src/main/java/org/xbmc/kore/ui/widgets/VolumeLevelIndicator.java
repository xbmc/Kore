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

public class VolumeLevelIndicator extends LinearLayout {
    private SeekBar volumeSeekBar;
    private TextView volumeTextView;

    private OnVolumeChangeListener onVolumeChangeListener;

    public interface OnVolumeChangeListener {
        void onVolumeChanged(int volume);
    }

    public VolumeLevelIndicator(Context context) {
        super(context);
        initializeView(context);
    }

    public VolumeLevelIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeView(context);
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.volume_level_indicator, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        volumeSeekBar = (SeekBar) findViewById(R.id.vli_seek_bar);
        volumeTextView = (TextView) findViewById(R.id.vli_volume_text);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    volumeTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onVolumeChangeListener != null)
                    onVolumeChangeListener.onVolumeChanged(seekBar.getProgress());
            }
        });
    }

    public void setOnVolumeChangeListener(OnVolumeChangeListener onVolumeChangeListener) {
        this.onVolumeChangeListener = onVolumeChangeListener;
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
