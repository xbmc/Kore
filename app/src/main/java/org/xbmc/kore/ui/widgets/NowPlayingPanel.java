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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.ui.generic.SubtitlesOptionsDialog;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class NowPlayingPanel extends SlidingUpPanelLayout
        implements  SubtitlesOptionsDialog.SubtitlesOptionsDialogListener{

    /**
     * After subtitle option selected show the Remote control activity
     *
     */
    @Override
    public void onRemoteRequired() {
        Intent launchIntent = new Intent(this.getContext(), RemoteActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.getContext().startActivity(launchIntent);
    }

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

    private SubtitlesOptionsDialog subtitlesDialog;

    @InjectView(R.id.npp_title) TextView title;
    @InjectView(R.id.npp_details) TextView details;
    @InjectView(R.id.npp_poster) ImageView poster;
    @InjectView(R.id.npp_previous) ImageButton previousButton;
    @InjectView(R.id.npp_next) ImageButton nextButton;
    @InjectView(R.id.npp_play) ImageButton playButton;
    @InjectView(R.id.npp_subtitles) ImageButton subtitlesButton;
    @InjectView(R.id.npp_progress_indicator) MediaProgressIndicator mediaProgressIndicator;
    @InjectView(R.id.npp_volume_level_indicator) VolumeLevelIndicator volumeLevelIndicator;
    @InjectView(R.id.npp_volume_mute) HighlightButton volumeMuteButton;
    @InjectView(R.id.npp_volume_muted_indicator) HighlightButton volumeMutedIndicatorButton;
    @InjectView(R.id.npp_repeat) RepeatModeButton repeatModeButton;
    @InjectView(R.id.npp_shuffle) HighlightButton shuffleButton;

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
        subtitlesDialog = new SubtitlesOptionsDialog((Activity)context);
        subtitlesDialog.addListener(this);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.now_playing_panel, this);
        ButterKnife.inject(view);
        setupButtonClickListeners();
    }

    public void setOnPanelButtonsClickListener(OnPanelButtonsClickListener listener) {
        onPanelButtonsClickListener = listener;
    }

    public void setOnVolumeChangeListener(VolumeLevelIndicator.OnVolumeChangeListener listener) {
        volumeLevelIndicator.setOnVolumeChangeListener(listener);
    }

    public void setOnProgressChangeListener(MediaProgressIndicator.OnProgressChangeListener listener) {
        mediaProgressIndicator.setOnProgressChangeListener(listener);
    }

    public void setVolume(int volume, boolean muted) {
        volumeLevelIndicator.setVolume(muted, volume);

        if (muted) {
            volumeMutedIndicatorButton.setVisibility(View.VISIBLE);
        } else {
            volumeMutedIndicatorButton.setVisibility(View.GONE);
        }

        volumeMutedIndicatorButton.setHighlight(muted);
        volumeMuteButton.setHighlight(muted);
    }

    public void setRepeatMode(String repeatMode) {
        UIUtils.setRepeatButton(repeatModeButton, repeatMode);
    }

    public void setShuffled(boolean shuffled) {
        shuffleButton.setHighlight(shuffled);
    }

    /**
     * Sets the state of the play button
     * @param play true if playing, false if paused
     */
    public void setPlayButton(boolean play) {
        UIUtils.setPlayPauseButtonIcon(getContext(), playButton, play);
    }

    public void setMediaProgress(GlobalType.Time time, GlobalType.Time totalTime) {
        mediaProgressIndicator.setMaxProgress(totalTime.ToSeconds());
        mediaProgressIndicator.setProgress(time.ToSeconds());
    }

    /**
     * Returns the progression indicator used for media progression
     * @return
     */
    public MediaProgressIndicator getMediaProgress() {
        return mediaProgressIndicator;
    }

    /**
     *
     * @param speed
     */
    public void setSpeed(int speed) {
        mediaProgressIndicator.setSpeed(speed);
    }

    public CharSequence getTitle() {
        return title.getText();
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setDetails(String details) {
        this.details.setText(details);
    }

    public void setNextPrevVisibility(int visibility) {
        nextButton.setVisibility(visibility);
        previousButton.setVisibility(visibility);
    }

    public void setSquarePoster(boolean square) {
        if (square) {
            ViewGroup.LayoutParams layoutParams = poster.getLayoutParams();
            layoutParams.width = layoutParams.height;
            poster.setLayoutParams(layoutParams);
        }
    }

    public ImageView getPoster() {
        return poster;
    }

    private void setupButtonClickListeners() {
        playButton.setOnClickListener(handleButtonClickListener);
        previousButton.setOnClickListener(handleButtonClickListener);
        nextButton.setOnClickListener(handleButtonClickListener);
        volumeMuteButton.setOnClickListener(handleButtonClickListener);
        shuffleButton.setOnClickListener(handleButtonClickListener);
        repeatModeButton.setOnClickListener(handleButtonClickListener);
        volumeMutedIndicatorButton.setOnClickListener(handleButtonClickListener);
        subtitlesButton.setOnClickListener(handleButtonClickListener);
    }

    private OnClickListener handleButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onPanelButtonsClickListener == null)
                return;

            switch (view.getId()) {
                case R.id.npp_previous:
                    onPanelButtonsClickListener.onPreviousClicked();
                    break;
                case R.id.npp_next:
                    onPanelButtonsClickListener.onNextClicked();
                    break;
                case R.id.npp_play:
                    onPanelButtonsClickListener.onPlayClicked();
                    break;
                case R.id.npp_volume_mute:
                    onPanelButtonsClickListener.onVolumeMuteClicked();
                    break;
                case R.id.npp_repeat:
                    onPanelButtonsClickListener.onRepeatClicked();
                    break;
                case R.id.npp_shuffle:
                    onPanelButtonsClickListener.onShuffleClicked();
                    break;
                case R.id.npp_volume_muted_indicator:
                    onPanelButtonsClickListener.onVolumeMutedIndicatorClicked();
                    break;
                case R.id.npp_subtitles:
                    subtitlesDialog.Show();
                    break;
            }
        }
    };
}
