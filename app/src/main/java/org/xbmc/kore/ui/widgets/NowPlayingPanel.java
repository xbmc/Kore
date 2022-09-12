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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.xbmc.kore.databinding.NowPlayingPanelBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.UIUtils;

/**
 * Shows a pannel of what's playing, which can be dragged up by the user to show actions on the content
 * This panel needs to be setup by calling {@link NowPlayingPanel#completeSetup(Context, FragmentManager, View)}
 * by the enclosing Activity/Fragment, and needs to be kept updated by calling the various Set* functions when
 * playback state changes.
 */
public class NowPlayingPanel extends LinearLayout {

    int activePlayerId = -1;
    NowPlayingPanelBinding binding;

    BottomSheetBehavior<View> bottomSheetBehavior = null;

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
        final HostConnection connection = HostManager.getInstance(context).getConnection();
        binding.play.setOnClickListener(v -> new Player.PlayPause(activePlayerId)
                .execute(connection, null, null));
        binding.volumeMutedIndicator.setOnClickListener(v -> new Application.SetMute()
                .execute(connection, null, null));
        binding.getRoot().setOnClickListener(null);
        binding.collapsedView.setOnClickListener(v -> {
            if (bottomSheetBehavior == null) return;
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });
    }

    public int getPanelState() {
        if (bottomSheetBehavior == null)
            return BottomSheetBehavior.STATE_HIDDEN;
        return bottomSheetBehavior.getState();
    }

    public void setPanelState(int state) {
        if (bottomSheetBehavior != null)
            bottomSheetBehavior.setState(state);
    }

    public void showPanel() {
        if (bottomSheetBehavior == null) return;
        // Only set state to collapsed if panel is currently hidden.
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setHideable(false);
        }
    }

    public void hidePanel() {
        if (bottomSheetBehavior == null) return;
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;

    /**
     * This completes the panel's setup, needed to be called separately to provide a FragmentManager, used to
     * show a Dialog box in {@link MediaActionsBar}
     * @param context Context
     * @param fragmentManager FragmentManager needed to show a Dialog
     */
    public void completeSetup(final Context context, final FragmentManager fragmentManager, View dependentView) {
        bottomSheetBehavior = BottomSheetBehavior.from(this);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        if (dependentView != null) {
            if (bottomSheetCallback != null) bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);

            bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    int collapseHeight = binding.collapsedView.getHeight();
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) dependentView.getLayoutParams();
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        layoutParams.bottomMargin = 0;
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED ||
                               newState == BottomSheetBehavior.STATE_EXPANDED) {
                        layoutParams.bottomMargin = collapseHeight;
                    }
                    dependentView.requestLayout();
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                    if (dependentView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
//                        int collapseHeight = binding.collapsedView.getHeight(), expandedHeight = getHeight();
//                        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) dependentView.getLayoutParams();
//                        layoutParams.bottomMargin = (int)((slideOffset <= 0) ?
//                                                          (slideOffset + 1) * collapseHeight :
//                                                          collapseHeight + slideOffset * (expandedHeight - collapseHeight));
//                        dependentView.requestLayout();
//                    }
                }
            };
            bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);
        }
        binding.mediaActionsBar.completeSetup(context, fragmentManager);
    }

    public void freeResources() {
        if (bottomSheetBehavior != null && bottomSheetCallback != null) {
            bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);
            bottomSheetCallback = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        freeResources();
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
        binding.mediaPlaybackBar.setPlaybackState(getActivePlayerResult, getPropertiesResult.speed);
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
