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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.CastFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.concurrent.locks.ReentrantLock;

public class TVShowProgressFragment extends AbstractAdditionalInfoFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(TVShowProgressFragment.class);

    public static final String BUNDLE_ITEM_ID = "itemid";
    public static final String BUNDLE_TITLE = "title";

    private static final int NEXT_EPISODES_COUNT = 2;
    private int itemId = -1;
    private CastFragment castFragment;

    public static final int LOADER_NEXT_EPISODES = 1;
    public static final int LOADER_SEASONS = 2;
    public static final int LOADER_SEASON_EPISODES = 3;

    public interface TVShowProgressActionListener {
        void onSeasonSelected(int tvshowId, int season);
        void onNextEpisodeSelected(int tvshowId, AbstractInfoFragment.DataHolder dataHolder);
    }

    // Activity listener
    private TVShowProgressActionListener listenerActivity;

    public void setArgs(int itemId, String showTitle) {
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_ITEM_ID, itemId);
        bundle.putString(BUNDLE_TITLE, showTitle);
        setArguments(bundle);
    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(LOADER_NEXT_EPISODES, null, this);
        getLoaderManager().restartLoader(LOADER_SEASONS, null, this);
        castFragment.refresh();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listenerActivity = (TVShowProgressActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement TVShowProgressActionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("Use setArgs to set required item id");
        }
        this.itemId = arguments.getInt(BUNDLE_ITEM_ID);
        String title = arguments.getString(BUNDLE_TITLE);

        View view = inflater.inflate(R.layout.fragment_tvshow_progress, container, false);

        castFragment = new CastFragment();
        castFragment.setArgs(this.itemId, title, CastFragment.TYPE.TVSHOW);
        getActivity().getSupportFragmentManager()
                     .beginTransaction()
                     .add(R.id.cast_fragment, castFragment)
                     .commit();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_NEXT_EPISODES, null, this);
        getLoaderManager().initLoader(LOADER_SEASONS, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;

        int hostId = HostManager.getInstance(getActivity()).getHostInfo().getId();
        switch (id) {
            case LOADER_NEXT_EPISODES:
                // Load seasons
                uri = MediaContract.Episodes.buildTVShowEpisodesListUri(hostId, itemId, NEXT_EPISODES_COUNT);
                String selection = MediaContract.EpisodesColumns.PLAYCOUNT + "=0";
                return new CursorLoader(getActivity(), uri,
                                        NextEpisodesListQuery.PROJECTION, selection, null, NextEpisodesListQuery.SORT);
            case LOADER_SEASONS:
                // Load seasons
                uri = MediaContract.Seasons.buildTVShowSeasonsListUri(hostId, itemId);
                return new CursorLoader(getActivity(), uri,
                                        SeasonsListQuery.PROJECTION, null, null, SeasonsListQuery.SORT);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
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
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Display next episode list
     *
     * @param cursor Cursor with the data
     */
    @TargetApi(21)
    private void displayNextEpisodeList(Cursor cursor) {
        TextView nextEpisodeTitle = (TextView) getActivity().findViewById(R.id.next_episode_title);
        GridLayout nextEpisodeList = (GridLayout) getActivity().findViewById(R.id.next_episode_list);

        if (cursor.moveToFirst()) {
            nextEpisodeTitle.setVisibility(View.VISIBLE);
            nextEpisodeList.setVisibility(View.VISIBLE);

            HostManager hostManager = HostManager.getInstance(getActivity());

            View.OnClickListener episodeClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AbstractInfoFragment.DataHolder vh = (AbstractInfoFragment.DataHolder) v.getTag();
                    listenerActivity.onNextEpisodeSelected(itemId, vh);
                }
            };

            // Get the art dimensions
            Resources resources = getActivity().getResources();
            int artWidth = (int)(resources.getDimension(R.dimen.detail_poster_width_square) /
                                 UIUtils.IMAGE_RESIZE_FACTOR);
            int artHeight = (int)(resources.getDimension(R.dimen.detail_poster_height_square) /
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

                View episodeView = LayoutInflater.from(getActivity())
                                                 .inflate(R.layout.list_item_next_episode, nextEpisodeList, false);

                ImageView artView = (ImageView)episodeView.findViewById(R.id.art);
                TextView titleView = (TextView)episodeView.findViewById(R.id.title);
                TextView detailsView = (TextView)episodeView.findViewById(R.id.details);
                TextView durationView = (TextView)episodeView.findViewById(R.id.duration);

                titleView.setText(title);
                detailsView.setText(seasonEpisode);
                durationView.setText(duration);

                UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                     thumbnail, title,
                                                     artView, artWidth, artHeight);

                AbstractInfoFragment.DataHolder vh = new AbstractInfoFragment.DataHolder(episodeId);
                vh.setTitle(title);
                vh.setUndertitle(seasonEpisode);
                episodeView.setTag(vh);
                episodeView.setOnClickListener(episodeClickListener);

                // For the popupmenu
                ImageView contextMenu = (ImageView)episodeView.findViewById(R.id.list_context_menu);
                contextMenu.setTag(episodeId);
                contextMenu.setOnClickListener(contextlistItemMenuClickListener);

                nextEpisodeList.addView(episodeView);
            } while (cursor.moveToNext());
        } else {
            // No episodes, hide views
            nextEpisodeTitle.setVisibility(View.GONE);
            nextEpisodeList.setVisibility(View.GONE);
        }
    }

    /**
     * Display the seasons list
     *
     * @param cursor Cursor with the data
     */
    @TargetApi(21)
    private void displaySeasonList(final Cursor cursor) {
        TextView seasonsListTitle = (TextView) getActivity().findViewById(R.id.seasons_title);
        GridLayout seasonsList = (GridLayout) getActivity().findViewById(R.id.seasons_list);

        if (cursor.moveToFirst()) {
            seasonsListTitle.setVisibility(View.VISIBLE);
            seasonsList.setVisibility(View.VISIBLE);

            HostManager hostManager = HostManager.getInstance(getActivity());

            View.OnClickListener seasonListClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listenerActivity.onSeasonSelected(itemId, (int)v.getTag());
                }
            };

            // Get the art dimensions
            Resources resources = getActivity().getResources();
            int artWidth = (int)(resources.getDimension(R.dimen.seasonlist_art_width) /
                                 UIUtils.IMAGE_RESIZE_FACTOR);
            int artHeight = (int)(resources.getDimension(R.dimen.seasonlist_art_heigth) /
                                  UIUtils.IMAGE_RESIZE_FACTOR);

            // Get theme colors
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorinProgress,
                    R.attr.colorFinished
            });

            final int inProgressColor = styledAttributes.getColor(styledAttributes.getIndex(0), resources.getColor(R.color.orange_500));
            final int finishedColor = styledAttributes.getColor(styledAttributes.getIndex(1), resources.getColor(R.color.green_400));
            styledAttributes.recycle();

            seasonsList.removeAllViews();
            do {
                final int seasonNumber = cursor.getInt(SeasonsListQuery.SEASON);
                String thumbnail = cursor.getString(SeasonsListQuery.THUMBNAIL);
                final int numEpisodes = cursor.getInt(SeasonsListQuery.EPISODE);
                int watchedEpisodes = cursor.getInt(SeasonsListQuery.WATCHEDEPISODES);

                View seasonView = LayoutInflater.from(getActivity()).inflate(R.layout.grid_item_season, seasonsList, false);

                ImageView seasonPictureView = (ImageView) seasonView.findViewById(R.id.art);
                TextView seasonNumberView = (TextView) seasonView.findViewById(R.id.season);
                final TextView seasonEpisodesView = (TextView) seasonView.findViewById(R.id.episodes);
                final ProgressBar seasonProgressBar = (ProgressBar) seasonView.findViewById(R.id.season_progress_bar);
                ImageView contextMenu = (ImageView) seasonView.findViewById(R.id.list_context_menu);
                final String numEpisodesFormat = getActivity().getString(R.string.num_episodes);

                seasonNumberView.setText(String.format(getActivity().getString(R.string.season_number), seasonNumber));
                seasonEpisodesView.setText(String.format(numEpisodesFormat, numEpisodes, numEpisodes - watchedEpisodes));
                seasonProgressBar.setMax(numEpisodes);
                seasonProgressBar.setProgress(watchedEpisodes);

                if (Utils.isLollipopOrLater()) {
                    int watchedColor = (numEpisodes - watchedEpisodes == 0) ? finishedColor : inProgressColor;
                    seasonProgressBar.setProgressTintList(ColorStateList.valueOf(watchedColor));
                }

                final ReentrantLock viewUpdatLock = new ReentrantLock();

                View.OnClickListener contextSeasonItemMenuClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        final PlaylistType.Item playListItem = new PlaylistType.Item();
                        playListItem.episodeid = (int) v.getTag();

                        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.getMenuInflater().inflate(R.menu.tvshow_season_list_item, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.action_queue:
                                        MediaPlayerUtils.queue(TVShowProgressFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.VIDEO);
                                        return true;
                                    case R.id.action_mark_watched:
                                        markSeasonWatched(true, seasonNumber, seasonEpisodesView, seasonProgressBar, numEpisodesFormat, finishedColor, viewUpdatLock);
                                        return true;
                                    case R.id.action_mark_unwatched:
                                        markSeasonWatched(false, seasonNumber, seasonEpisodesView, seasonProgressBar, numEpisodesFormat, inProgressColor, viewUpdatLock);
                                        return true;
                                }
                                return false;
                            }
                        });
                        popupMenu.show();
                    }
                };
                contextMenu.setTag(seasonNumber);
                contextMenu.setOnClickListener(contextSeasonItemMenuClickListener);

                UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                     thumbnail,
                                                     String.valueOf(seasonNumber),
                                                     seasonPictureView, artWidth, artHeight);

                seasonView.setTag(seasonNumber);
                seasonView.setOnClickListener(seasonListClickListener);
                seasonsList.addView(seasonView);
            } while (cursor.moveToNext());
        } else {
            // No seasons, hide views
            seasonsListTitle.setVisibility(View.GONE);
            seasonsList.setVisibility(View.GONE);
        }
    }

    private void markSeasonWatched(final boolean watched, final int seasonNumber, final TextView seasonEpisodesView, final ProgressBar progressBar,
                                   final String numEpisodesFormat, final int finalColor, final ReentrantLock viewUpdatLock) {

        getLoaderManager().restartLoader(LOADER_SEASON_EPISODES, null,
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
                        Uri uri = MediaContract.Episodes.buildTVShowSeasonEpisodesListUri(hostInfo.getId(), itemId, seasonNumber);
                        String filter = MediaContract.EpisodesColumns.PLAYCOUNT;
                        filter += watched ? "=0" : ">0";
                        return new CursorLoader(getActivity(), uri, EpisodesListQuery.PROJECTION, filter, null, null);
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                        if (data.moveToFirst()) {
                            do {
                                VideoLibrary.SetEpisodeDetails action = new VideoLibrary.SetEpisodeDetails(data.getInt(
                                        EpisodesListQuery.EPISODEID), watched ? 1 : 0, null);
                                action.execute(HostManager.getInstance(getActivity()).getConnection(), new ApiCallback<String>() {
                                    @Override
                                    public void onSuccess(String result) {
                                        viewUpdatLock.lock();
                                        int watchedEpisodes = progressBar.getProgress() + (watched ? 1 : -1);
                                        int numEpisodes = progressBar.getMax();
                                        progressBar.setProgress(watchedEpisodes);
                                        seasonEpisodesView.setText(String.format(numEpisodesFormat, numEpisodes, numEpisodes - watchedEpisodes));
                                        boolean done = false;
                                        if (watched) {
                                            done = watchedEpisodes == numEpisodes;
                                            if (Utils.isLollipopOrLater() && done) {
                                                progressBar.setProgressTintList(ColorStateList.valueOf(finalColor));
                                            }
                                        } else {
                                            done = watchedEpisodes == 0;
                                            if (Utils.isLollipopOrLater() && (watchedEpisodes + 1 == numEpisodes)) {
                                                progressBar.setProgressTintList(ColorStateList.valueOf(finalColor));
                                            }
                                        }

                                        if (done) {
                                            Activity activity = getActivity();
                                            if (activity != null) {
                                                Intent syncIntent = new Intent(activity, LibrarySyncService.class);
                                                syncIntent.putExtra(LibrarySyncService.SYNC_SINGLE_TVSHOW, true);
                                                syncIntent.putExtra(LibrarySyncService.SYNC_TVSHOWID, itemId);
                                                activity.startService(syncIntent);
                                            }
                                        }
                                        viewUpdatLock.unlock();
                                    }

                                    @Override
                                    public void onError(int errorCode, String description) {

                                    }
                                }, null);
                            } while (data.moveToNext());
                        }

                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {

                    }
                });
    }

    private View.OnClickListener contextlistItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final PlaylistType.Item playListItem = new PlaylistType.Item();
            playListItem.episodeid = (int)v.getTag();

            final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.tvshow_episode_list_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_play:
                            MediaPlayerUtils.play(TVShowProgressFragment.this, playListItem);
                            return true;
                        case R.id.action_queue:
                            MediaPlayerUtils.queue(TVShowProgressFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.VIDEO);
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
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
                MediaContract.Seasons.THUMBNAIL,
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

    /**
     * Episodes list query parameters.
     */
    private interface EpisodesListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Episodes.EPISODEID,
                MediaContract.Episodes.PLAYCOUNT
        };

        int ID = 0;
        int EPISODEID = 1;
        int PLAYCOUNT = 2;
    }
}
