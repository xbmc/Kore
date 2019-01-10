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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
import org.xbmc.kore.ui.sections.audio.SongsListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Fragment that presents the movie list
 */
public class MovieListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(MovieListFragment.class);

    public interface OnMovieSelectedListener {
        void onMovieSelected(ViewHolder vh);
    }

    // Activity listener
    private OnMovieSelectedListener listenerActivity;

    private static boolean showWatchedStatus;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MOVIES; }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onMovieSelected(tag);
    }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new MoviesAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        Uri uri = MediaContract.Movies.buildMoviesListUri(hostInfo != null? hostInfo.getId() : -1);

        StringBuilder selection = new StringBuilder();
        String selectionArgs[] = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection.append(MediaContract.MoviesColumns.TITLE + " LIKE ?");
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED)) {
            if (selection.length() != 0)
                selection.append(" AND ");
            selection.append(MediaContract.MoviesColumns.PLAYCOUNT)
                     .append("=0");
        }

        showWatchedStatus = preferences.getBoolean(Settings.KEY_PREF_MOVIES_SHOW_WATCHED_STATUS, Settings.DEFAULT_PREF_MOVIES_SHOW_WATCHED_STATUS);

        String sortOrderStr;
        int sortOrder = preferences.getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
        if (sortOrder == Settings.SORT_BY_DATE_ADDED) {
            sortOrderStr = MovieListQuery.SORT_BY_DATE_ADDED;
        } else if (sortOrder == Settings.SORT_BY_LAST_PLAYED) {
            sortOrderStr = MovieListQuery.SORT_BY_LAST_PLAYED;
        } else if (sortOrder == Settings.SORT_BY_RATING) {
            sortOrderStr = MovieListQuery.SORT_BY_RATING;
        } else if (sortOrder == Settings.SORT_BY_YEAR) {
            sortOrderStr = MovieListQuery.SORT_BY_YEAR;
        } else if (sortOrder == Settings.SORT_BY_LENGTH) {
            sortOrderStr = MovieListQuery.SORT_BY_LENGTH;
        } else {
            // Sort by name
            if (preferences.getBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, Settings.DEFAULT_PREF_MOVIES_IGNORE_PREFIXES)) {
                sortOrderStr = MovieListQuery.SORT_BY_NAME_IGNORE_ARTICLES;
            } else {
                sortOrderStr = MovieListQuery.SORT_BY_NAME;
            }
        }

        return new CursorLoader(getActivity(), uri,
                                MovieListQuery.PROJECTION, selection.toString(), selectionArgs, sortOrderStr);
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
        try {
            listenerActivity = (OnMovieSelectedListener) ctx;
        } catch (ClassCastException e) {
            throw new ClassCastException(ctx.toString() + " must implement OnMovieSelectedListener");
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

        inflater.inflate(R.menu.movie_list, menu);

        // Setup filters
        MenuItem hideWatched = menu.findItem(R.id.action_hide_watched),
                ignoreArticles = menu.findItem(R.id.action_ignore_prefixes),
                sortByName = menu.findItem(R.id.action_sort_by_name),
                sortByYear = menu.findItem(R.id.action_sort_by_year),
                sortByRating = menu.findItem(R.id.action_sort_by_rating),
                sortByDateAdded = menu.findItem(R.id.action_sort_by_date_added),
                sortByLastPlayed = menu.findItem(R.id.action_sort_by_last_played),
                sortByLength = menu.findItem(R.id.action_sort_by_length),
                showWatchedStatusMenuItem = menu.findItem(R.id.action_show_watched_status);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED));
        showWatchedStatusMenuItem.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_SHOW_WATCHED_STATUS, Settings.DEFAULT_PREF_MOVIES_SHOW_WATCHED_STATUS));
        ignoreArticles.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, Settings.DEFAULT_PREF_MOVIES_IGNORE_PREFIXES));

        int sortOrder = preferences.getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
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
            case Settings.SORT_BY_LENGTH:
                sortByLength.setChecked(true);
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
                           .putBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, item.isChecked())
                           .apply();
                refreshList();
                break;
            case R.id.action_show_watched_status:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putBoolean(Settings.KEY_PREF_MOVIES_SHOW_WATCHED_STATUS, item.isChecked())
                           .apply();
                showWatchedStatus = item.isChecked();
                refreshList();
                break;
            case R.id.action_ignore_prefixes:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, item.isChecked())
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_name:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_NAME)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_year:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_YEAR)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_rating:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_RATING)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_date_added:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_last_played:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_LAST_PLAYED)
                        .apply();
                refreshList();
                break;
            case R.id.action_sort_by_length:
                item.setChecked(true);
                preferences.edit()
                           .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_LENGTH)
                           .apply();
                refreshList();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Movie list query parameters.
     */
    private interface MovieListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Movies.MOVIEID,
                MediaContract.Movies.TITLE,
                MediaContract.Movies.THUMBNAIL,
                MediaContract.Movies.YEAR,
                MediaContract.Movies.GENRES,
                MediaContract.Movies.RUNTIME,
                MediaContract.Movies.RATING,
                MediaContract.Movies.TAGLINE,
                MediaContract.Movies.PLAYCOUNT,
                };


        String SORT_BY_NAME = MediaContract.Movies.TITLE + " COLLATE NOCASE ASC";
        String SORT_BY_YEAR = MediaContract.Movies.YEAR + " ASC";
        String SORT_BY_RATING = MediaContract.Movies.RATING + " DESC";
        String SORT_BY_DATE_ADDED = MediaContract.Movies.DATEADDED + " DESC";
        String SORT_BY_LAST_PLAYED = MediaContract.Movies.LASTPLAYED + " DESC";
        String SORT_BY_LENGTH = MediaContract.Movies.RUNTIME + " DESC";
        String SORT_BY_NAME_IGNORE_ARTICLES = MediaDatabase.sortCommonTokens(MediaContract.Movies.TITLE) + " COLLATE NOCASE ASC";

        int ID = 0;
        int MOVIEID = 1;
        int TITLE = 2;
        int THUMBNAIL = 3;
        int YEAR = 4;
        int GENRES = 5;
        int RUNTIME = 6;
        int RATING = 7;
        int TAGLINE = 8;
        int PLAYCOUNT = 9;
    }

    private class MoviesAdapter extends RecyclerViewCursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;
        private int themeAccentColor;

        MoviesAdapter(Context context) {
            // Get the default accent color
            Resources.Theme theme = context.getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorAccent
            });

            themeAccentColor = styledAttributes.getColor(styledAttributes.getIndex(0), getResources().getColor(R.color.accent_default));
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
                                            .inflate(R.layout.grid_item_movie, parent, false);

            return new ViewHolder(view, getContext(), themeAccentColor, hostManager, artWidth, artHeight);
        }

        protected int getSectionColumnIdx() { return MovieListQuery.TITLE; }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView durationView;
        ImageView checkmarkView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;
        int themeAccentColor;

        AbstractFragment.DataHolder dataHolder = new AbstractFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, int themeAccentColor,
                   HostManager hostManager,
                   int artWidth, int artHeight) {
            super(itemView);
            this.context = context;
            this.themeAccentColor = themeAccentColor;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            durationView = itemView.findViewById(R.id.duration);
            checkmarkView = itemView.findViewById(R.id.checkmark);
            artView = itemView.findViewById(R.id.art);
        }

        @Override
        public void bindView(Cursor cursor) {
            // Save the movie id
            dataHolder.setId(cursor.getInt(MovieListQuery.MOVIEID));
            dataHolder.setTitle(cursor.getString(MovieListQuery.TITLE));
            dataHolder.setUndertitle(cursor.getString(MovieListQuery.TAGLINE));

            int movieYear = cursor.getInt(MovieListQuery.YEAR);
            dataHolder.setRating(cursor.getDouble(MovieListQuery.RATING));
            dataHolder.setMaxRating(10);

            titleView.setText(dataHolder.getTitle());

            String genres = cursor.getString(MovieListQuery.GENRES);
            String details = TextUtils.isEmpty(dataHolder.getUnderTitle()) ?
                             genres : dataHolder.getUnderTitle();
            detailsView.setText(details);

            int runtime = cursor.getInt(MovieListQuery.RUNTIME) / 60;
            String duration =  runtime > 0 ?
                               String.format(context.getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                               "  |  " + movieYear :
                               String.valueOf(movieYear);
            durationView.setText(duration);
            dataHolder.setDetails(duration + "\n" + details);

            dataHolder.setPosterUrl(cursor.getString(MovieListQuery.THUMBNAIL));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 dataHolder.getPosterUrl(),
                                                 dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            if (showWatchedStatus && (cursor.getInt(MovieListQuery.PLAYCOUNT) > 0)) {
                checkmarkView.setVisibility(View.VISIBLE);
                checkmarkView.setColorFilter(themeAccentColor);
            } else {
                checkmarkView.setVisibility(View.INVISIBLE);
            }

            if (Utils.isLollipopOrLater()) {
                artView.setTransitionName("a" + dataHolder.getId());
            }
        }
    }
}
