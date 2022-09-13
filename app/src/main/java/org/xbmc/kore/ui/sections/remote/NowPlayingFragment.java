/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore.ui.sections.remote;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentNowPlayingBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.notification.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.ui.sections.video.AllCastActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;

/**
 * Now playing view
 */
public class NowPlayingFragment extends Fragment
        implements HostConnectionObserver.PlayerEventsObserver {
    private static final String TAG = LogUtils.makeLogTag(NowPlayingFragment.class);

    /**
     * Interface for this fragment to communicate with the enclosing activity
     */
    public interface NowPlayingListener {
        void SwitchToRemotePanel();
    }

    /**
     * Host manager from which to get info about the current XBMC
     */
    private HostManager hostManager;

    /**
     * Activity to communicate potential actions that change what's playing
     */
    private HostConnectionObserver hostConnectionObserver;

    private ViewTreeObserver.OnScrollChangedListener onScrollChangedListener;

    private FragmentNowPlayingBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(requireContext());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNowPlayingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);

        /* Setup dim the fanart when scroll changes */
        onScrollChangedListener = UIUtils.createInfoPanelScrollChangedListener(requireContext(), binding.mediaPanel, binding.art, binding.mediaPanelGroup);
        binding.mediaPanel.getViewTreeObserver().addOnScrollChangedListener(onScrollChangedListener);

        binding.includeInfoPanel.infoPanel.setVisibility(View.VISIBLE);
        binding.mediaPanel.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        hostConnectionObserver.registerPlayerObserver(this);
    }

    @Override
    public void onStop() {
        stopNowPlayingInfo();
        hostConnectionObserver.unregisterPlayerObserver(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding.mediaPanel.getViewTreeObserver().removeOnScrollChangedListener(onScrollChangedListener);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPlayerPropertyChanged(Player.NotificationsData notificationsData) { }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void onPlayerPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void onPlayerPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void onPlayerStop() {
        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        binding.includeInfoPanel.infoTitle.setText(R.string.nothing_playing);
        binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    public void onPlayerConnectionError(int errorCode, String description) {
        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.not_connected);
            binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
            binding.includeInfoPanel.infoMessage.setText(null);
        }
    }

    public void onPlayerNoResultsYet() {
        // Initialize info panel
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.connecting);
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
        }
        binding.includeInfoPanel.infoMessage.setText(null);
    }

    public void onSystemQuit() {
        onPlayerNoResultsYet();
    }

    // Ignore this
    public void onInputRequested(String title, String type, String value) {}
    public void onObserverStopObserving() {}

    /**
     * Sets whats playing information
     * @param getItemResult Return from method {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @SuppressLint("DefaultLocale")
    private void setNowPlayingInfo(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   PlayerType.PropertyValue getPropertiesResult,
                                   final ListType.ItemsAll getItemResult) {
        final String title, underTitle, art, poster, genreSeason, year, descriptionPlot, votes;
        double rating;

        switchToPanel(R.id.media_panel);

        switch (getItemResult.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                title = getItemResult.title;
                underTitle = getItemResult.tagline;
                art = getItemResult.art.fanart;
                poster = getItemResult.art.poster;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                title = getItemResult.title;
                underTitle = getItemResult.showtitle;
                art = getItemResult.thumbnail;
                poster = getItemResult.art.poster;

                genreSeason = String.format(getString(R.string.season_episode), getItemResult.season, getItemResult.episode);
                year = getItemResult.premiered;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_SONG:
                title = getItemResult.title;
                underTitle = getItemResult.displayartist + " | " + getItemResult.album;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.description;
                rating = getItemResult.rating;
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                title = getItemResult.title;
                underTitle = Utils.listStringConcat(getItemResult.artist, ", ")
                             + " | " + getItemResult.album;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.plot;
                rating = 0;
                votes = null;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                title = getItemResult.label;
                underTitle = getItemResult.title;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = getItemResult.premiered;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                votes = null;
                break;
            default:
                // Other type, just present basic info
                title = getItemResult.label;
                underTitle = "";
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = null;
                year = getItemResult.premiered;
                descriptionPlot = removeYouTubeMarkup(getItemResult.plot);
                rating = 0;
                votes = null;
                break;
        }

        binding.mediaTitle.setText(UIUtils.applyMarkup(getContext(), title));
        binding.mediaTitle.post(UIUtils.getMarqueeToggleableAction(binding.mediaTitle));
        binding.mediaUndertitle.setText(underTitle);

        // Check if this is still necessary for PVR playback
        int speed = getItemResult.type.equals(ListType.ItemsAll.TYPE_CHANNEL)? 1 : getPropertiesResult.speed;
        binding.progressInfo.setPlaybackState(getActivePlayerResult.playerid,
                                              speed,
                                              getPropertiesResult.time.toSeconds(),
                                              getPropertiesResult.totaltime.toSeconds());
        binding.mediaPlaybackBar.setPlaybackState(getActivePlayerResult, speed);

        if (!TextUtils.isEmpty(year) || !TextUtils.isEmpty(genreSeason)) {
            binding.year.setVisibility(View.VISIBLE);
            binding.genres.setVisibility(View.VISIBLE);
            binding.year.setText(year);
            binding.genres.setText(genreSeason);
        } else {
            binding.year.setVisibility(View.GONE);
            binding.genres.setVisibility(View.GONE);
        }

        // 0 rating will not be shown
        if (rating > 0) {
            binding.rating.setVisibility(View.VISIBLE);
            binding.ratingVotes.setVisibility(View.VISIBLE);
            binding.rating.setText(String.format("%01.01f", rating));
            binding.ratingVotes.setText(votes);
        } else {
            binding.rating.setVisibility(View.GONE);
            binding.ratingVotes.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(descriptionPlot)) {
            binding.mediaDescription.setVisibility(View.VISIBLE);
            binding.mediaDescription.setText(UIUtils.applyMarkup(getContext(), descriptionPlot));
        } else {
            binding.mediaDescription.setVisibility(View.GONE);
        }

        Resources resources = requireActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int artHeight = resources.getDimensionPixelOffset(R.dimen.info_art_height),
                artWidth = binding.art.getWidth(); // displayMetrics.widthPixels;
        if (!TextUtils.isEmpty(art)) {
            binding.poster.setVisibility(View.VISIBLE);
            int posterWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_width);
            int posterHeight = resources.getDimensionPixelOffset(R.dimen.info_poster_height);

            // If not video, change aspect ration of poster to a square
            boolean isVideo = (getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
                              (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE));
            if (!isVideo) {
                ViewGroup.LayoutParams layoutParams = binding.poster.getLayoutParams();
                layoutParams.height = layoutParams.width;
                binding.poster.setLayoutParams(layoutParams);
                posterHeight = posterWidth;
            }

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 poster, title,
                                                 binding.poster, posterWidth, posterHeight);
            UIUtils.loadImageIntoImageview(hostManager, art, binding.art, artWidth, artHeight);
        } else {
            // No fanart, just present the poster
            binding.poster.setVisibility(View.GONE);
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager, poster, title, binding.art, artWidth, artHeight);
        }

        if (getPropertiesResult.type.equals(PlayerType.PropertyValue.TYPE_VIDEO)) {
            binding.castList.setVisibility(View.VISIBLE);
            UIUtils.setupCastInfo(getActivity(), getItemResult.cast, binding.castList,
                                  AllCastActivity.buildLaunchIntent(getActivity(), title,
                                                                    (ArrayList<VideoType.Cast>)getItemResult.cast));
        } else {
            binding.castList.setVisibility(View.GONE);
        }
    }

    /**
     * Cleans up anything left when stop playing
     */
    private void stopNowPlayingInfo() {
        // Just stop the seek bar handler callbacks
        binding.progressInfo.stopUpdating();
    }

    private int shortAnimationDuration = -1;

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        if (shortAnimationDuration == -1)
            shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (panelResId == R.id.info_panel && binding.includeInfoPanel.infoPanel.getVisibility() != View.VISIBLE) {
            UIUtils.fadeOutView(binding.mediaPanel, shortAnimationDuration, 0);
            UIUtils.fadeInView(binding.includeInfoPanel.infoPanel, shortAnimationDuration, shortAnimationDuration);
            binding.art.setVisibility(View.GONE);
        } else if (panelResId == R.id.media_panel && binding.mediaPanel.getVisibility() != View.VISIBLE) {
            UIUtils.fadeOutView(binding.includeInfoPanel.infoPanel, shortAnimationDuration, 0);
            UIUtils.fadeInView(binding.mediaPanel, shortAnimationDuration, shortAnimationDuration);
            binding.art.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Removes some markup that appears on the plot for youtube videos
     *
     * @param plot Plot as returned by youtube plugin
     * @return Plot without markup
     */
    private String removeYouTubeMarkup(String plot) {
        if (plot == null) return null;
        return plot.replaceAll("\\[.*\\]", "");
    }
}
