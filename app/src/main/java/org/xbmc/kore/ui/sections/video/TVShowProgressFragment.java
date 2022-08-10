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

package org.xbmc.kore.ui.sections.video;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.color.MaterialColors;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.ItemTvshowBinding;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.CastFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

public class TVShowProgressFragment extends AbstractAdditionalInfoFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(TVShowProgressFragment.class);

    public static final String BUNDLE_ITEM_ID = "itemid";
    public static final String BUNDLE_TITLE = "title";
    public static final String BUNDLE_POSTER_URL = "poster_url";

    private static final int NEXT_EPISODES_COUNT = 2;
    private int itemId = -1;
    private String showTitle, showPosterUrl;
    private CastFragment castFragment;

    public static final int LOADER_NEXT_EPISODES = 1,
            LOADER_SEASONS = 2;

    public interface TVShowProgressActionListener {
        void onSeasonSelected(int tvshowId, int season, String seasonPoster);
        void onNextEpisodeSelected(int tvshowId, AbstractInfoFragment.DataHolder dataHolder);
    }

    // Activity listener
    private TVShowProgressActionListener listenerActivity;

    public void setArgs(int itemId, String showTitle, String showPosterUrl) {
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_ITEM_ID, itemId);
        bundle.putString(BUNDLE_TITLE, showTitle);
        bundle.putString(BUNDLE_POSTER_URL, showPosterUrl);
        setArguments(bundle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("Use setArgs to set required item id");
        }
        this.itemId = arguments.getInt(BUNDLE_ITEM_ID);
        this.showTitle = arguments.getString(BUNDLE_TITLE);
        this.showPosterUrl = arguments.getString(BUNDLE_POSTER_URL);

        View view = inflater.inflate(R.layout.fragment_tvshow_progress, container, false);

        castFragment = new CastFragment();
        castFragment.setArgs(this.itemId, this.showTitle, CastFragment.TYPE.TVSHOW);
        requireActivity().getSupportFragmentManager()
                         .beginTransaction()
                         .add(R.id.cast_fragment, castFragment)
                         .commit();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoaderManager lm = LoaderManager.getInstance(this);
        lm.initLoader(LOADER_NEXT_EPISODES, null, this);
        lm.initLoader(LOADER_SEASONS, null, this);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listenerActivity = (TVShowProgressActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement TVShowProgressActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void refresh() {
        LoaderManager lm = LoaderManager.getInstance(this);
        lm.restartLoader(LOADER_NEXT_EPISODES, null, this);
        lm.restartLoader(LOADER_SEASONS, null, this);
        castFragment.refresh();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;

        int hostId = HostManager.getInstance(requireContext()).getHostInfo().getId();
        switch (id) {
            case LOADER_NEXT_EPISODES:
                // Load seasons
                uri = MediaContract.Episodes.buildTVShowEpisodesListUri(hostId, itemId, NEXT_EPISODES_COUNT);
                String selection = MediaContract.EpisodesColumns.PLAYCOUNT + "=0";
                return new CursorLoader(requireContext(), uri,
                                        NextEpisodesListQuery.PROJECTION, selection, null, NextEpisodesListQuery.SORT);
            case LOADER_SEASONS:
            default:
                // Load seasons
                uri = MediaContract.Seasons.buildTVShowSeasonsListUri(hostId, itemId);
                return new CursorLoader(requireContext(), uri,
                                        SeasonsListQuery.PROJECTION, null, null, SeasonsListQuery.SORT);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            switch (loader.getId()) {
                case LOADER_NEXT_EPISODES:
                    displayNextEpisodeList(data);
                    break;
                case LOADER_SEASONS:
                    displaySeasonList(data);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) { }

    /**
     * Display next episode list
     *
     * @param cursor Cursor with the data
     */
    private void displayNextEpisodeList(Cursor cursor) {
        TextView nextEpisodeTitle = requireActivity().findViewById(R.id.next_episode_title);
        GridLayout nextEpisodeList = requireActivity().findViewById(R.id.next_episode_list);
        View nextEpisodeDivider = requireActivity().findViewById(R.id.next_episode_divider);

        if (cursor.moveToFirst()) {
            nextEpisodeTitle.setVisibility(View.VISIBLE);
            nextEpisodeList.setVisibility(View.VISIBLE);
            nextEpisodeDivider.setVisibility(View.VISIBLE);

            HostManager hostManager = HostManager.getInstance(requireContext());

            View.OnClickListener episodeClickListener = v -> {
                DataHolder vh = (DataHolder) v.getTag();
                listenerActivity.onNextEpisodeSelected(itemId, vh);
            };

            // Get the art dimensions
            Resources resources = requireContext().getResources();
            int artWidth = (int)(resources.getDimension(R.dimen.info_poster_width_square) /
                                 UIUtils.IMAGE_RESIZE_FACTOR);
            int artHeight = (int)(resources.getDimension(R.dimen.info_poster_height_square) /
                                  UIUtils.IMAGE_RESIZE_FACTOR);

            nextEpisodeList.removeAllViews();
            do {
                int episodeId = cursor.getInt(NextEpisodesListQuery.EPISODEID);
                String title = cursor.getString(NextEpisodesListQuery.TITLE);
                String seasonEpisode = String.format(getString(R.string.season_episode),
                                                     cursor.getInt(NextEpisodesListQuery.SEASON),
                                                     cursor.getInt(NextEpisodesListQuery.EPISODE));
                int runtime = cursor.getInt(NextEpisodesListQuery.RUNTIME) / 60;
                String duration =  runtime > 0 ?
                                   String.format(getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                                   "  |  " + cursor.getString(NextEpisodesListQuery.FIRSTAIRED) :
                                   cursor.getString(NextEpisodesListQuery.FIRSTAIRED);
                String thumbnail = cursor.getString(NextEpisodesListQuery.THUMBNAIL);

                View episodeView = LayoutInflater.from(requireContext())
                                                 .inflate(R.layout.item_tvshow_episode, nextEpisodeList, false);

                ImageView artView = episodeView.findViewById(R.id.art);
                TextView titleView = episodeView.findViewById(R.id.title);
                TextView detailsView = episodeView.findViewById(R.id.details);
                TextView durationView = episodeView.findViewById(R.id.duration);
                ImageView watchedCheckView = episodeView.findViewById(R.id.watched_check);

                titleView.setText(title);
                detailsView.setText(seasonEpisode);
                durationView.setText(duration);
                watchedCheckView.setVisibility(View.GONE);

                UIUtils.loadImageWithCharacterAvatar(requireContext(), hostManager,
                                                     thumbnail, title,
                                                     artView, artWidth, artHeight);

                AbstractInfoFragment.DataHolder vh = new AbstractInfoFragment.DataHolder(episodeId);
                vh.setTitle(title);
                vh.setUndertitle(seasonEpisode);
                vh.setPosterUrl(this.showPosterUrl);
                episodeView.setTag(vh);
                episodeView.setOnClickListener(episodeClickListener);

                // For the popupmenu
                ImageView contextMenu = episodeView.findViewById(R.id.list_context_menu);
                contextMenu.setTag(episodeId);
                contextMenu.setOnClickListener(contextlistItemMenuClickListener);

                nextEpisodeList.addView(episodeView);
            } while (cursor.moveToNext());
        } else {
            // No episodes, hide views
            nextEpisodeTitle.setVisibility(View.GONE);
            nextEpisodeList.setVisibility(View.GONE);
            nextEpisodeDivider.setVisibility(View.GONE);
        }
    }

    /**
     * Display the seasons list
     *
     * @param cursor Cursor with the data
     */
    private void displaySeasonList(Cursor cursor) {
        TextView seasonsListTitle = requireActivity().findViewById(R.id.seasons_title);
        GridLayout seasonsList = requireActivity().findViewById(R.id.seasons_list);
        View seasonsDivider = requireActivity().findViewById(R.id.seasons_divider);

        if (cursor.moveToFirst()) {
            seasonsListTitle.setVisibility(View.VISIBLE);
            seasonsList.setVisibility(View.VISIBLE);
            seasonsDivider.setVisibility(View.VISIBLE);

            HostManager hostManager = HostManager.getInstance(requireContext());

            // Get the art dimensions
            Resources resources = requireContext().getResources();
            int artWidth = (int)(resources.getDimension(R.dimen.seasonlist_art_width) /
                                 UIUtils.IMAGE_RESIZE_FACTOR);
            int artHeight = (int)(resources.getDimension(R.dimen.seasonlist_art_heigth) /
                                  UIUtils.IMAGE_RESIZE_FACTOR);

            // Get theme colors
            int inProgressColor = MaterialColors.getColor(requireContext(), R.attr.colorInProgress, Color.GREEN);
            int finishedColor = MaterialColors.getColor(requireContext(), R.attr.colorFinished, Color.WHITE);

            seasonsList.removeAllViews();
            do {
                int seasonNumber = cursor.getInt(SeasonsListQuery.SEASON);
                String thumbnail = cursor.getString(SeasonsListQuery.THUMBNAIL);
                int numEpisodes = cursor.getInt(SeasonsListQuery.EPISODE);
                int watchedEpisodes = cursor.getInt(SeasonsListQuery.WATCHEDEPISODES);

                ItemTvshowBinding binding = ItemTvshowBinding.inflate(LayoutInflater.from(requireContext()),
                                                                      seasonsList, false);

                binding.title.setText(String.format(requireContext().getString(R.string.season_number), seasonNumber));
                binding.details.setText(String.format(requireContext().getString(R.string.num_episodes),
                                                      numEpisodes, numEpisodes - watchedEpisodes));
                binding.tvShowsProgressBar.setMax(numEpisodes);
                binding.tvShowsProgressBar.setProgress(watchedEpisodes);
                int watchedColor = (numEpisodes - watchedEpisodes == 0) ? finishedColor : inProgressColor;
                binding.tvShowsProgressBar.setProgressTintList(ColorStateList.valueOf(watchedColor));
                binding.otherInfo.setVisibility(View.GONE);

                UIUtils.loadImageWithCharacterAvatar(requireContext(), hostManager,
                                                     thumbnail,
                                                     String.valueOf(seasonNumber),
                                                     binding.art, artWidth, artHeight);

                View seasonView = binding.getRoot();
                seasonView.setTag(seasonNumber);
                seasonView.setOnClickListener(v -> listenerActivity.onSeasonSelected(itemId, seasonNumber, thumbnail));
                seasonsList.addView(seasonView);
            } while (cursor.moveToNext());
        } else {
            // No seasons, hide views
            seasonsListTitle.setVisibility(View.GONE);
            seasonsList.setVisibility(View.GONE);
            seasonsDivider.setVisibility(View.GONE);
        }
    }

    private final View.OnClickListener contextlistItemMenuClickListener = v -> {
        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.episodeid = (int)v.getTag();

        final PopupMenu popupMenu = new PopupMenu(requireContext(), v);
        popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play) {
                MediaPlayerUtils.play(TVShowProgressFragment.this, playListItem);
                return true;
            } else if (itemId == R.id.action_queue) {
                MediaPlayerUtils.queue(TVShowProgressFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.VIDEO);
                return true;
            }
            return false;
        });
        popupMenu.show();
    };

    /**
     * Next episodes list query parameters.
     */
    private interface NextEpisodesListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Episodes.EPISODEID,
                MediaContract.Episodes.SEASON,
                MediaContract.Episodes.EPISODE,
                MediaContract.Episodes.THUMBNAIL,
                MediaContract.Episodes.PLAYCOUNT,
                MediaContract.Episodes.TITLE,
                MediaContract.Episodes.RUNTIME,
                MediaContract.Episodes.FIRSTAIRED,
                };

        String SORT = MediaContract.Episodes.EPISODEID + " ASC";

        int ID = 0;
        int EPISODEID = 1;
        int SEASON = 2;
        int EPISODE = 3;
        int THUMBNAIL = 4;
        int PLAYCOUNT = 5;
        int TITLE = 6;
        int RUNTIME = 7;
        int FIRSTAIRED = 8;
    }

    /**
     * Seasons list query parameters.
     */
    private interface SeasonsListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Seasons.SEASON,
                MediaContract.Seasons.POSTER,
                MediaContract.Seasons.EPISODE,
                MediaContract.Seasons.WATCHEDEPISODES
        };

        String SORT = MediaContract.Seasons.SEASON + " ASC";

        int ID = 0;
        int SEASON = 1;
        int THUMBNAIL = 2;
        int EPISODE = 3;
        int WATCHEDEPISODES = 4;
    }
}
