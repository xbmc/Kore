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
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import org.xbmc.kore.databinding.MediaProgressIndicatorBinding;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

public class MediaProgressIndicator extends LinearLayout {
    private static final String TAG = LogUtils.makeLogTag(MediaProgressIndicator.class);

    MediaProgressIndicatorBinding binding;

    private int speed = 0;
    private int maxProgress;
    private int progress;
    private static final int SEEK_BAR_UPDATE_INTERVAL = 1000; // ms
    private int progressIncrement;

    private ProgressChangeListener progressChangeListener;

    public abstract static class ProgressChangeListener {
        final Context context;
        int activePlayerId;
        final Handler callbackHandler;

        public ProgressChangeListener(Context context, Handler callbackHandler) {
            this.context = context;
            activePlayerId = -1;
            this.callbackHandler = callbackHandler;
        }

        public void setActivePlayerId(int activePlayerId) {
            this.activePlayerId = activePlayerId;
        }

        public abstract void onProgressChanged(int progress);

        public static ProgressChangeListener buildDefault(Context context, Handler callbackHandler) {
            return new ProgressChangeListener(context, callbackHandler) {
                @Override
                public void onProgressChanged(int progress) {
                    PlayerType.PositionTime positionTime = new PlayerType.PositionTime(progress);
                    Player.Seek seekAction = new Player.Seek(activePlayerId, positionTime);
                    seekAction.execute(HostManager.getInstance(context).getConnection(),
                                       new ApiCallback<PlayerType.SeekReturnType>() {
                                           @Override
                                           public void onSuccess(PlayerType.SeekReturnType result) {/* Ignored */ }

                                           @Override
                                           public void onError(int errorCode, String description) {
                                               LogUtils.LOGD(TAG, "Error calling Player.Seek: " + description);
                                           }
                                       },
                                       callbackHandler);
                }
            };
        }
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
                if (progressChangeListener != null)
                    progressChangeListener.onProgressChanged(seekBar.getProgress());

                if (speed > 0)
                    seekBar.postDelayed(seekBarUpdater, SEEK_BAR_UPDATE_INTERVAL);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        binding.mpiSeekBar.removeCallbacks(seekBarUpdater);
        progressChangeListener = null;
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

    public void setOnProgressChangeListener(ProgressChangeListener progressChangeListener) {
        this.progressChangeListener = progressChangeListener;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        binding.mpiSeekBar.setProgress(progress);
        binding.mpiProgress.setText(UIUtils.formatTime(progress));
    }

    public void setActivePlayerId(int activePlayerId) {
        this.progressChangeListener.setActivePlayerId(activePlayerId);
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