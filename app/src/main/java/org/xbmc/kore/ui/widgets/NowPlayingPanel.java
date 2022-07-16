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
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.FragmentManager;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.xbmc.kore.databinding.NowPlayingPanelBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.UIUtils;

/**
 * Shows a pannel of what's playing, which can be dragged up by the user to show actions on the content
 * This panel needs to be setup by calling {@link NowPlayingPanel#completeSetup(Context, FragmentManager)}
 * by the enclosing Activity/Fragment, and needs to be kept updated by calling the various Set* functions when
 * playback state changes.
 */
public class NowPlayingPanel extends SlidingUpPanelLayout {

    int activePlayerId = -1;
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
        setDragView(binding.collapsedView);

        final HostConnection connection = HostManager.getInstance(context).getConnection();
        final Handler callbackHandler = new Handler(Looper.getMainLooper());
        final ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                UIUtils.setPlayPauseButtonIcon(context, binding.play, result == 1);
            }

            @Override
            public void onError(int errorCode, String description) { }
        };

        binding.play.setOnClickListener(v -> new Player.PlayPause(activePlayerId)
                .execute(connection, defaultPlaySpeedChangedCallback, callbackHandler));
        binding.volumeMutedIndicator.setOnClickListener(v -> new Application.SetMute()
                .execute(connection, null, null));

        binding.progressInfo.setDefaultOnProgressChangeListener(context);
        binding.progressInfo.setDefaultOnProgressChangeListener(context);
        binding.mediaPlaybackBar.setDefaultOnClickListener(context);
    }

    /**
     * This completes the panel's setup, needed to be called separately to provide a FragmentManager, used to
     * show a Dialog box in {@link MediaActionsBar}
     * @param context Context
     * @param fragmentManager FragmentManager needed to show a Dialog
     */
    public void completeSetup(final Context context, final FragmentManager fragmentManager) {
        binding.mediaActionsBar.setDefaultOnClickListener(context, fragmentManager);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        binding = null;
    }

    /**
     * Sets the playback state of the panel
     */
    public void setPlaybackState(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                 PlayerType.PropertyValue getPropertiesResult) {
        activePlayerId = getActivePlayerResult.playerid;
        binding.progressInfo.setPlaybackState(getActivePlayerResult.playerid,
                                              getPropertiesResult.speed,
                                              getPropertiesResult.time.toSeconds(),
                                              getPropertiesResult.totaltime.toSeconds());
        binding.mediaPlaybackBar.setPlaybackState(getActivePlayerResult.playerid, getPropertiesResult.speed);
        binding.mediaActionsBar.setPlaybackState(getActivePlayerResult,
                                                 getPropertiesResult);
        UIUtils.setPlayPauseButtonIcon(getContext(), binding.play, getPropertiesResult.speed == 1);
    }

    public void setVolumeState(int volume, boolean muted) {
        binding.mediaActionsBar.setVolumeState(volume, muted);

        if (muted) {
            binding.volumeMutedIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.volumeMutedIndicator.setVisibility(View.GONE);
        }
        binding.volumeMutedIndicator.setHighlight(muted);
    }

    public void setRepeatShuffleState(String repeatMode, Boolean shuffled, Boolean partymode) {
        binding.mediaActionsBar.setRepeatShuffleState(repeatMode, shuffled, partymode);
    }

    public CharSequence getTitle() {
        return binding.title.getText();
    }

    public void setTitle(String title) {
        binding.title.setText(UIUtils.applyMarkup(getContext(), title));
    }

    public void setDetails(String details) {
        binding.details.setText(details);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setSquarePoster(boolean square) {
        if (square) {
            ViewGroup.LayoutParams layoutParams = binding.poster.getLayoutParams();
            layoutParams.width = layoutParams.height;
            binding.poster.setLayoutParams(layoutParams);
        }
    }

    public ImageView getPoster() {
        return binding.poster;
    }
}
