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
package org.xbmc.kore.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Fragment that presents the movie list
 */
public class MovieListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(MovieListFragment.class);

    public interface OnMovieSelectedListener {
        public void onMovieSelected(ViewHolder vh);
    }

    // Activity listener
    private OnMovieSelectedListener listenerActivity;

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
    protected CursorAdapter createAdapter() {
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

        String sortOrderStr;
        int sortOrder = preferences.getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
        if (sortOrder == Settings.SORT_BY_DATE_ADDED) {
            sortOrderStr = MovieListQuery.SORT_BY_DATE_ADDED;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnMovieSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMovieSelectedListener");
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
                sortByLength = menu.findItem(R.id.action_sort_by_length);


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED));
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
        };


        String SORT_BY_NAME = MediaContract.Movies.TITLE + " ASC";
        String SORT_BY_YEAR = MediaContract.Movies.YEAR + " ASC";
        String SORT_BY_RATING = MediaContract.Movies.RATING + " DESC";
        String SORT_BY_DATE_ADDED = MediaContract.Movies.DATEADDED + " DESC";
        String SORT_BY_LENGTH = MediaContract.Movies.RUNTIME + " DESC";
        String SORT_BY_NAME_IGNORE_ARTICLES = MediaDatabase.sortCommonTokens(MediaContract.Movies.TITLE) + " ASC";

        final int ID = 0;
        final int MOVIEID = 1;
        final int TITLE = 2;
        final int THUMBNAIL = 3;
        final int YEAR = 4;
        final int GENRES = 5;
        final int RUNTIME = 6;
        final int RATING = 7;
        final int TAGLINE = 8;
    }

    private static class MoviesAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public MoviesAdapter(Context context) {
            super(context, null, false);
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

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                    .inflate(R.layout.grid_item_movie, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.titleView = (TextView)view.findViewById(R.id.title);
            viewHolder.detailsView = (TextView)view.findViewById(R.id.details);
//            viewHolder.yearView = (TextView)view.findViewById(R.id.year);
            viewHolder.durationView = (TextView)view.findViewById(R.id.duration);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @TargetApi(21)
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            // Save the movie id
            viewHolder.movieId = cursor.getInt(MovieListQuery.MOVIEID);
            viewHolder.movieTitle = cursor.getString(MovieListQuery.TITLE);
            viewHolder.movieTagline = cursor.getString(MovieListQuery.TAGLINE);
            viewHolder.movieYear = cursor.getInt(MovieListQuery.YEAR);
            viewHolder.movieRating = cursor.getDouble(MovieListQuery.RATING);

            viewHolder.titleView.setText(viewHolder.movieTitle);

            viewHolder.movieGenres = cursor.getString(MovieListQuery.GENRES);
            String details = TextUtils.isEmpty(viewHolder.movieTagline) ?
                    viewHolder.movieGenres :
                    viewHolder.movieTagline;
            viewHolder.detailsView.setText(details);
//            viewHolder.yearView.setText(String.valueOf(cursor.getInt(MovieListQuery.YEAR)));
            viewHolder.movieRuntime = cursor.getInt(MovieListQuery.RUNTIME) / 60;
            String duration =  viewHolder.movieRuntime > 0 ?
                    String.format(context.getString(R.string.minutes_abbrev), String.valueOf(viewHolder.movieRuntime)) +
                            "  |  " + viewHolder.movieYear :
                    String.valueOf(viewHolder.movieYear);
            viewHolder.durationView.setText(duration);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    cursor.getString(MovieListQuery.THUMBNAIL), viewHolder.movieTitle,
                    viewHolder.artView, artWidth, artHeight);

            if(Utils.isLollipopOrLater()) {
                viewHolder.artView.setTransitionName("a"+viewHolder.movieId);
            }
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        TextView titleView;
        TextView detailsView;
        //        TextView yearView;
        TextView durationView;
        ImageView artView;

        int movieId;
        String movieTitle;
        String movieTagline;
        int movieYear;
        int movieRuntime;
        String movieGenres;
        double movieRating;
    }
}
