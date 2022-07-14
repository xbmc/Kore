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
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.NowPlayingPanelBinding;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.utils.UIUtils;

public class NowPlayingPanel extends SlidingUpPanelLayout {

    public interface OnPanelButtonsClickListener {
        void onPlayClicked();
        void onPreviousClicked();
        void onNextClicked();
        void onVolumeMuteClicked();
        void onShuffleClicked();
        void onRepeatClicked();
        void onVolumeMutedIndicatorClicked();
    }

    private OnPanelButtonsClickListener onPanelButtonsClickListener;
    NowPlayingPanelBinding binding;

    public NowPlayingPanel(Context context) {
        super(context);
        initializeView(context);
    }
    public NowPlayingPanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeView(context);
    }

    public NowPlayingPanel(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        initializeView(context);
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = NowPlayingPanelBinding.inflate(inflater, this);

        setDragView(binding.nppCollapsedView);
        setupButtonClickListeners();
        binding.nppProgressIndicator.setDefaultOnProgressChangeListener(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        onPanelButtonsClickListener = null;
        binding = null;
    }

    public void setOnPanelButtonsClickListener(OnPanelButtonsClickListener listener) {
        onPanelButtonsClickListener = listener;
    }

    public void setOnVolumeChangeListener(VolumeLevelIndicator.OnVolumeChangeListener listener) {
        binding.nppVolumeLevelIndicator.setOnVolumeChangeListener(listener);
    }

    public void setVolume(int volume, boolean muted) {
        binding.nppVolumeLevelIndicator.setVolume(muted, volume);

        if (muted) {
            binding.nppVolumeMutedIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.nppVolumeMutedIndicator.setVisibility(View.GONE);
        }

        binding.nppVolumeMutedIndicator.setHighlight(muted);
        binding.nppVolumeMute.setHighlight(muted);
    }

    public void setRepeatMode(String repeatMode) {
        UIUtils.setRepeatButton(binding.nppRepeat, repeatMode);
    }

    public void setShuffled(boolean shuffled) {
        binding.nppShuffle.setHighlight(shuffled);
    }

    /**
     * Sets the playback state of the panel
     * @param activePlayerId Current player id
     * @param speed Playback speed
     * @param time Playback time
     * @param totalTime Total playback time
     */
    public void setPlaybackState(int activePlayerId, int speed, GlobalType.Time time, GlobalType.Time totalTime) {
        binding.nppProgressIndicator.setPlaybackState(activePlayerId, speed, time.toSeconds(), totalTime.toSeconds());
        UIUtils.setPlayPauseButtonIcon(getContext(), binding.nppPlay, speed == 1);
    }

    public CharSequence getTitle() {
        return binding.nppTitle.getText();
    }

    public void setTitle(String title) {
        binding.nppTitle.setText(UIUtils.applyMarkup(getContext(), title));
    }

    public void setDetails(String details) {
        binding.nppDetails.setText(details);
    }

    public void setNextPrevVisibility(int visibility) {
        binding.nppNext.setVisibility(visibility);
        binding.nppPrevious.setVisibility(visibility);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setSquarePoster(boolean square) {
        if (square) {
            ViewGroup.LayoutParams layoutParams = binding.nppPoster.getLayoutParams();
            layoutParams.width = layoutParams.height;
            binding.nppPoster.setLayoutParams(layoutParams);
        }
    }

    public ImageView getPoster() {
        return binding.nppPoster;
    }

    private void setupButtonClickListeners() {
        binding.nppPlay.setOnClickListener(handleButtonClickListener);
        binding.nppPrevious.setOnClickListener(handleButtonClickListener);
        binding.nppNext.setOnClickListener(handleButtonClickListener);
        binding.nppVolumeMute.setOnClickListener(handleButtonClickListener);
        binding.nppShuffle.setOnClickListener(handleButtonClickListener);
        binding.nppRepeat.setOnClickListener(handleButtonClickListener);
        binding.nppVolumeMutedIndicator.setOnClickListener(handleButtonClickListener);
    }

    private final OnClickListener handleButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onPanelButtonsClickListener == null)
                return;

            int viewId = view.getId();
            if (viewId == R.id.npp_previous) {
                onPanelButtonsClickListener.onPreviousClicked();
            } else if (viewId == R.id.npp_next) {
                onPanelButtonsClickListener.onNextClicked();
            } else if (viewId == R.id.npp_play) {
                onPanelButtonsClickListener.onPlayClicked();
            } else if (viewId == R.id.npp_volume_mute) {
                onPanelButtonsClickListener.onVolumeMuteClicked();
            } else if (viewId == R.id.npp_repeat) {
                onPanelButtonsClickListener.onRepeatClicked();
            } else if (viewId == R.id.npp_shuffle) {
                onPanelButtonsClickListener.onShuffleClicked();
            } else if (viewId == R.id.npp_volume_muted_indicator) {
                onPanelButtonsClickListener.onVolumeMutedIndicatorClicked();
            }
        }
    };
}
