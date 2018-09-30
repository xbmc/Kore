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
package org.xbmc.kore.ui.sections.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Fragment that presents the tv show list
 */
public class TVShowListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(TVShowListFragment.class);

    public interface OnTVShowSelectedListener {
        void onTVShowSelected(TVShowListFragment.ViewHolder vh);
    }

    // Activity listener
    private OnTVShowSelectedListener listenerActivity;

    private static boolean showWatchedStatus;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_TVSHOWS; }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onTVShowSelected(tag);
    }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new TVShowsAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        Uri uri = MediaContract.TVShows.buildTVShowsListUri(hostInfo != null ? hostInfo.getId() : -1);

        StringBuilder selection = new StringBuilder();
        String selectionArgs[] = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection.append(MediaContract.TVShowsColumns.TITLE + " LIKE ?");
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        // Filters
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOWS_FILTER_HIDE_WATCHED)) {
            if (selection.length() != 0)
                selection.append(" AND ");
            selection.append(MediaContract.TVShowsColumns.WATCHEDEPISODES)
                     .append("!=")
                     .append(MediaContract.TVShowsColumns.EPISODE);
        }

        showWatchedStatus = preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_SHOW_WATCHED_STATUS, Settings.DEFAULT_PREF_TVSHOWS_SHOW_WATCHED_STATUS);

        String sortOrderStr;
        int sortOrder = preferences.getInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.DEFAULT_PREF_TVSHOWS_SORT_ORDER);
        if (sortOrder == Settings.SORT_BY_DATE_ADDED) {
            sortOrderStr = TVShowListQuery.SORT_BY_DATE_ADDED;
        } else if (sortOrder == Settings.SORT_BY_YEAR) {
            sortOrderStr = TVShowListQuery.SORT_BY_YEAR;
        } else if (sortOrder == Settings.SORT_BY_RATING) {
            sortOrderStr = TVShowListQuery.SORT_BY_RATING;
        } else if (sortOrder == Settings.SORT_BY_LAST_PLAYED) {
            sortOrderStr = TVShowListQuery.SORT_BY_LAST_PLAYED;
        } else {
            // Sort by name
            if (preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_IGNORE_PREFIXES, Settings.DEFAULT_PREF_TVSHOWS_IGNORE_PREFIXES)) {
                sortOrderStr = TVShowListQuery.SORT_BY_NAME_IGNORE_ARTICLES;
            } else {
                sortOrderStr = TVShowListQuery.SORT_BY_NAME;
            }
        }


        return new CursorLoader(getActivity(), uri,
                                TVShowListQuery.PROJECTION, selection.toString(),
                                selectionArgs, sortOrderStr);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnTVShowSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTVShowSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            // HACK: Fix crash reported on Play Store. Why does this is necessary is beyond me
            super.onCreateOptionsMenu(menu, inflater);
            return;
        }

        inflater.inflate(R.menu.tvshow_list, menu);

        // Setup filters
        MenuItem hideWatched = menu.findItem(R.id.action_hide_watched),
                ignoreArticles = menu.findItem(R.id.action_ignore_prefixes),
                sortByName = menu.findItem(R.id.action_sort_by_name),
                sortByYear = menu.findItem(R.id.action_sort_by_year),
                sortByRating = menu.findItem(R.id.action_sort_by_rating),
                sortByDateAdded = menu.findItem(R.id.action_sort_by_date_added),
                sortByLastPlayed = menu.findItem(R.id.action_sort_by_last_played),
                showWatchedStatusMenuItem = menu.findItem(R.id.action_show_watched_status);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOWS_FILTER_HIDE_WATCHED));
        showWatchedStatusMenuItem.setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_SHOW_WATCHED_STATUS, Settings.DEFAULT_PREF_TVSHOWS_SHOW_WATCHED_STATUS));
        ignoreArticles.setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_IGNORE_PREFIXES, Settings.DEFAULT_PREF_TVSHOWS_IGNORE_PREFIXES));

        int sortOrder = preferences.getInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.DEFAULT_PREF_TVSHOWS_SORT_ORDER);
        switch (sortOrder) {
            case Settings.SORT_BY_YEAR:
                sortByYear.setChecked(true);
                break;
            case Settings.SORT_BY_RATING:
                sortByRating.setChecked(true);
                break;
            case Settings.SORT_BY_DATE_ADDED:
                sortByDateAdded.setChecked(true);
                break;
            case Settings.SORT_BY_LAST_PLAYED:
                sortByLastPlayed.setChecked(true);
                break;
            default:
                sortByName.setChecked(true);
                break;
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        switch (item.getItemId()) {
            case R.id.action_hide_watched:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putBoolean(Settings.KEY_PREF_TVSHOWS_FILTER_HIDE_WATCHED, item.isChecked())
                           .apply();
                refreshList();
                break;
            case R.id.action_show_watched_status:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putBoolean(Settings.KEY_PREF_TVSHOWS_SHOW_WATCHED_STATUS, item.isChecked())
                           .apply();
                showWatchedStatus = item.isChecked();
                refreshList();
                break;
            case R.id.action_ignore_prefixes:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putBoolean(Settings.KEY_PREF_TVSHOWS_IGNORE_PREFIXES, item.isChecked())
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_name:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_NAME)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_year:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_YEAR)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_rating:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_RATING)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_date_added:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_last_played:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_LAST_PLAYED)
                        .apply();
                refreshList();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * TVShow list query parameters.
     */
    private interface TVShowListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.TVShows.TVSHOWID,
                MediaContract.TVShows.TITLE,
                MediaContract.TVShows.THUMBNAIL,
                MediaContract.TVShows.FANART,
                MediaContract.TVShows.PREMIERED,
                MediaContract.TVShows.STUDIO,
                MediaContract.TVShows.EPISODE,
                MediaContract.TVShows.WATCHEDEPISODES,
                MediaContract.TVShows.RATING,
                MediaContract.TVShows.PLOT,
                MediaContract.TVShows.PLAYCOUNT,
                MediaContract.TVShows.IMDBNUMBER,
                MediaContract.TVShows.GENRES,
                };

        String SORT_BY_NAME = MediaContract.TVShows.TITLE + " COLLATE NOCASE ASC";
        String SORT_BY_YEAR = MediaContract.TVShows.PREMIERED + " ASC";
        String SORT_BY_RATING = MediaContract.TVShows.RATING + " DESC";
        String SORT_BY_DATE_ADDED = MediaContract.TVShows.DATEADDED + " DESC";
        String SORT_BY_LAST_PLAYED = MediaContract.TVShows.LASTPLAYED + " DESC";
        String SORT_BY_NAME_IGNORE_ARTICLES = MediaDatabase.sortCommonTokens(MediaContract.TVShows.TITLE) + " COLLATE NOCASE ASC";

        int ID = 0;
        int TVSHOWID = 1;
        int TITLE = 2;
        int THUMBNAIL = 3;
        int FANART = 4;
        int PREMIERED = 5;
        int STUDIO = 6;
        int EPISODE = 7;
        int WATCHEDEPISODES = 8;
        int RATING = 9;
        int PLOT = 10;
        int PLAYCOUNT = 11;
        int IMDBNUMBER = 12;
        int GENRES = 13;
    }

    private class TVShowsAdapter extends RecyclerViewCursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;
        private int themeAccentColor,
                inProgressColor, finishedColor;

        public TVShowsAdapter(Context context) {
            // Get the default accent color
            Resources.Theme theme = context.getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorAccent,
                    R.attr.colorinProgress,
                    R.attr.colorFinished
            });

            themeAccentColor = styledAttributes.getColor(styledAttributes.getIndex(0), getResources().getColor(R.color.accent_default));
            inProgressColor = styledAttributes.getColor(styledAttributes.getIndex(1), getResources().getColor(R.color.orange_500));
            finishedColor = styledAttributes.getColor(styledAttributes.getIndex(2), getResources().getColor(R.color.light_green_600));
            styledAttributes.recycle();

            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.now_playing_poster_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.now_playing_poster_height) /
                              UIUtils.IMAGE_RESIZE_FACTOR);
        }

        @Override
        public CursorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(getContext())
                                            .inflate(R.layout.grid_item_tvshow, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder(view, getContext(),
                                                   themeAccentColor, inProgressColor, finishedColor,
                                                   hostManager,
                                                   artWidth, artHeight);

            return viewHolder;
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView premieredView;
        ImageView artView;
        ProgressBar watchedProgressView;
        ImageView contextMenu;
        Context context;
        int themeAccentColor, inProgressColor, finishedColor;
        String numEpisodesFormat;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        ReentrantLock viewUpdatLock;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);

        ViewHolder(View itemView, Context context,
                   int themeAccentColor, int inProgressColor, int finishedColor,
                   HostManager hostManager,
                   int artWidth, int artHeight) {
            super(itemView);
            this.hostManager = hostManager;
            this.context = context;
            this.artHeight = artHeight;
            this.artWidth = artWidth;
            this.themeAccentColor = themeAccentColor;
            this.inProgressColor = inProgressColor;
            this.finishedColor = finishedColor;
            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            premieredView = itemView.findViewById(R.id.premiered);
            artView = itemView.findViewById(R.id.art);
            watchedProgressView = itemView.findViewById(R.id.tv_shows_progress_bar);
            contextMenu = itemView.findViewById(R.id.list_context_menu);
            numEpisodesFormat = context.getString(R.string.num_episodes);
            viewUpdatLock = new ReentrantLock();
        }

        @Override
        public void bindView(Cursor cursor) {
            // Save the movie id
            dataHolder.setId(cursor.getInt(TVShowListQuery.TVSHOWID));
            dataHolder.setTitle(cursor.getString(TVShowListQuery.TITLE));
            dataHolder.setDescription(cursor.getString(TVShowListQuery.PLOT));
            dataHolder.setRating(cursor.getInt(TVShowListQuery.RATING));
            int numEpisodes = cursor.getInt(TVShowListQuery.EPISODE);
            int watchedEpisodes = cursor.getInt(TVShowListQuery.WATCHEDEPISODES);

            titleView.setText(dataHolder.getTitle());
            String details = String.format(numEpisodesFormat, numEpisodes, numEpisodes - watchedEpisodes);
            detailsView.setText(details);
            dataHolder.setUndertitle(details);

            String premiered = String.format(context.getString(R.string.premiered),
                                             cursor.getString(TVShowListQuery.PREMIERED));
            premieredView.setText(premiered);
            dataHolder.setDetails(premiered);
            dataHolder.setPosterUrl(cursor.getString(TVShowListQuery.THUMBNAIL));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 dataHolder.getPosterUrl(),
                                                 dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            if (showWatchedStatus) {
                watchedProgressView.setVisibility(View.VISIBLE);
                watchedProgressView.setMax(numEpisodes);
                watchedProgressView.setProgress(watchedEpisodes);
            } else {
                watchedProgressView.setVisibility(View.INVISIBLE);
            }

            if (Utils.isLollipopOrLater()) {
                if (showWatchedStatus) {
                    int watchedColor = (numEpisodes - watchedEpisodes == 0)? finishedColor : inProgressColor;
                    watchedProgressView.setProgressTintList(ColorStateList.valueOf(watchedColor));
                }
                artView.setTransitionName("a" + dataHolder.getId());
            }

            View.OnClickListener contextSeasonItemMenuClickListener = new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    final PopupMenu popupMenu = new PopupMenu(context, v);
                    popupMenu.getMenuInflater().inflate(R.menu.tvshow_season_list_item, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_queue:
                                    //MediaPlayerUtils.queue(null, playListItem, PlaylistType.GetPlaylistsReturnType.VIDEO);
                                    return true;
                                case R.id.action_mark_watched:
                                    markSeasonWatched(true, finishedColor);
                                    return true;
                                case R.id.action_mark_unwatched:
                                    markSeasonWatched(false, inProgressColor);
                                    return true;
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
                }
            };
            contextMenu.setOnClickListener(contextSeasonItemMenuClickListener);
        }

        private void markSeasonWatched(final boolean watched, final int finalColor) {

            ((FragmentActivity)context).getSupportLoaderManager().restartLoader(1, null,
                    new LoaderManager.LoaderCallbacks<Cursor>() {
                        @Override
                        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                            HostInfo hostInfo = HostManager.getInstance(context).getHostInfo();
                            Uri uri = MediaContract.Episodes.buildTVShowEpisodesListUri(hostInfo.getId(), dataHolder.getId());
                            String filter = MediaContract.EpisodesColumns.PLAYCOUNT;
                            filter += watched ? "=0" : ">0";
                            return new CursorLoader(context, uri, TVShowListFragment.EpisodesListQuery.PROJECTION, filter, null, null);
                        }

                        @Override
                        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                            if (data.moveToFirst()) {
                                do {
                                    VideoLibrary.SetEpisodeDetails action = new VideoLibrary.SetEpisodeDetails(data.getInt(
                                            TVShowListFragment.EpisodesListQuery.EPISODEID), watched ? 1 : 0, null);
                                    action.execute(HostManager.getInstance(context).getConnection(), new ApiCallback<String>() {
                                        @Override
                                        public void onSuccess(String result) {
                                            viewUpdatLock.lock();
                                            int watchedEpisodes = watchedProgressView.getProgress() + (watched ? 1 : -1);
                                            int numEpisodes = watchedProgressView.getMax();
                                            watchedProgressView.setProgress(watchedEpisodes);

                                            String numEpisodesStr = String.format(numEpisodesFormat, numEpisodes, numEpisodes - watchedEpisodes);
                                            dataHolder.setUndertitle(numEpisodesStr);
                                            detailsView.setText(numEpisodesStr);

                                            boolean done = false;
                                            if (watched) {
                                                done = watchedEpisodes == numEpisodes;
                                                if (Utils.isLollipopOrLater() && done) {
                                                    watchedProgressView.setProgressTintList(ColorStateList.valueOf(finalColor));
                                                }
                                            } else {
                                                done = watchedEpisodes == 0;
                                                if (Utils.isLollipopOrLater() && (watchedEpisodes + 1 == numEpisodes)) {
                                                    watchedProgressView.setProgressTintList(ColorStateList.valueOf(finalColor));
                                                }
                                            }

                                            if (done) {
                                                Context c = context;
                                                if (c != null) {
                                                    Intent syncIntent = new Intent(c, LibrarySyncService.class);
                                                    syncIntent.putExtra(LibrarySyncService.SYNC_SINGLE_TVSHOW, true);
                                                    syncIntent.putExtra(LibrarySyncService.SYNC_TVSHOWID, dataHolder.getId());
                                                    c.startService(syncIntent);
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
