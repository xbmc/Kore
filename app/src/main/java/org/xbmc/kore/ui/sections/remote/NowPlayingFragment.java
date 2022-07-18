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
import android.content.Context;
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
        implements HostConnectionObserver.PlayerEventsObserver,
                   ViewTreeObserver.OnScrollChangedListener {
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

    /**
     * Listener for events on this fragment
     */
    private NowPlayingListener nowPlayingListener;

    private FragmentNowPlayingBinding binding;

    private int pixelsToTransparent;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Try to cast the enclosing activity to the listener interface
        try {
            nowPlayingListener = (NowPlayingListener)context;
        } catch (ClassCastException e) {
            nowPlayingListener = null;
        }
    }

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

        /* Setup dim the fanart when scroll changes
         * Full dim on 4 * iconSize dp
         * @see {@link #onScrollChanged()}
         */
        pixelsToTransparent  = 4 * requireActivity().getResources().getDimensionPixelSize(R.dimen.default_icon_size);
        binding.mediaPanel.getViewTreeObserver().addOnScrollChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopNowPlayingInfo();
        hostConnectionObserver.unregisterPlayerObserver(this);
    }

    @Override
    public void onDestroyView() {
        binding.mediaPanel.getViewTreeObserver().removeOnScrollChangedListener(this);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onScrollChanged() {
        float y = binding.mediaPanel.getScrollY();
        float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
        binding.art.setAlpha(newAlpha);
    }

    @Override
    public void playerOnPropertyChanged(Player.NotificationsData notificationsData) {
    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnStop() {
        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        binding.includeInfoPanel.infoTitle.setText(R.string.nothing_playing);
        binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    public void playerOnConnectionError(int errorCode, String description) {
        stopNowPlayingInfo();
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.connecting);
            // TODO: check error code
            binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
            binding.includeInfoPanel.infoMessage.setText(null);
        }
    }

    public void playerNoResultsYet() {
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

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {}
    public void observerOnStopObserving() {}

    /**
     * Sets whats playing information
     * @param getItemResult Return from method {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @SuppressLint("DefaultLocale")
    private void setNowPlayingInfo(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   PlayerType.PropertyValue getPropertiesResult,
                                   final ListType.ItemsAll getItemResult) {
        final String title, underTitle, art, poster, genreSeason, year,
                descriptionPlot, votes, maxRating;
        double rating;

        switch (getItemResult.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = getItemResult.tagline;
                art = getItemResult.art.fanart;
                poster = getItemResult.art.poster;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                maxRating = getString(R.string.max_rating_video);
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = getItemResult.showtitle;
                art = getItemResult.thumbnail;
                poster = getItemResult.art.poster;

                genreSeason = String.format(getString(R.string.season_episode), getItemResult.season, getItemResult.episode);
                year = getItemResult.premiered;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                maxRating = getString(R.string.max_rating_video);
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_SONG:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = getItemResult.displayartist + " | " + getItemResult.album;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.description;
                rating = getItemResult.rating;
                maxRating = getString(R.string.max_rating_music);
                votes = (TextUtils.isEmpty(getItemResult.votes)) ? "" : String.format(getString(R.string.votes), getItemResult.votes);
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                switchToPanel(R.id.media_panel);

                title = getItemResult.title;
                underTitle = Utils.listStringConcat(getItemResult.artist, ", ")
                             + " | " + getItemResult.album;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = (getItemResult.year > 0)? String.format("%d", getItemResult.year) : null;
                descriptionPlot = getItemResult.plot;
                rating = 0;
                maxRating = null;
                votes = null;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                switchToPanel(R.id.media_panel);

                title = getItemResult.label;
                underTitle = getItemResult.title;
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = Utils.listStringConcat(getItemResult.genre, ", ");
                year = getItemResult.premiered;
                descriptionPlot = getItemResult.plot;
                rating = getItemResult.rating;
                maxRating = null;
                votes = null;
                break;
            default:
                // Other type, just present basic info
                switchToPanel(R.id.media_panel);

                title = getItemResult.label;
                underTitle = "";
                art = getItemResult.fanart;
                poster = getItemResult.thumbnail;

                genreSeason = null;
                year = getItemResult.premiered;
                descriptionPlot = removeYouTubeMarkup(getItemResult.plot);
                rating = 0;
                maxRating = null;
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
            binding.maxRating.setVisibility(View.VISIBLE);
            binding.ratingVotes.setVisibility(View.VISIBLE);
            binding.rating.setText(String.format("%01.01f", rating));
            binding.maxRating.setText(maxRating);
            binding.ratingVotes.setText(votes);
        } else {
            binding.rating.setVisibility(View.GONE);
            binding.maxRating.setVisibility(View.GONE);
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

        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        if (!TextUtils.isEmpty(art)) {
            binding.poster.setVisibility(View.VISIBLE);
            int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
            int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height);

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
            UIUtils.loadImageIntoImageview(hostManager, art, binding.art, displayMetrics.widthPixels, artHeight);

            // Reset padding
            int paddingLeft = resources.getDimensionPixelOffset(R.dimen.poster_width_plus_padding),
                    paddingRight = binding.mediaTitle.getPaddingRight(),
                    paddingTop = binding.mediaTitle.getPaddingTop(),
                    paddingBottom = binding.mediaTitle.getPaddingBottom();
            binding.mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            binding.mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        } else {
            // No fanart, just present the poster
            binding.poster.setVisibility(View.GONE);
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager, poster, title, binding.art, artWidth, artHeight);
            // Reset padding
            int paddingLeft = binding.mediaTitle.getPaddingRight(),
                    paddingRight = binding.mediaTitle.getPaddingRight(),
                    paddingTop = binding.mediaTitle.getPaddingTop(),
                    paddingBottom = binding.mediaTitle.getPaddingBottom();
            binding.mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            binding.mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }

//        if (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE) ||
//            getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) {
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

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        if (panelResId == R.id.info_panel) {
            binding.mediaPanel.setVisibility(View.GONE);
            binding.art.setVisibility(View.GONE);
            binding.includeInfoPanel.infoPanel.setVisibility(View.VISIBLE);
        } else if (panelResId == R.id.media_panel) {
            binding.includeInfoPanel.infoPanel.setVisibility(View.GONE);
            binding.mediaPanel.setVisibility(View.VISIBLE);
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
