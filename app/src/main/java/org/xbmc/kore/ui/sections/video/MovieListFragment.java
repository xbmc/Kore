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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.loader.content.CursorLoader;
import androidx.preference.PreferenceManager;

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
import org.xbmc.kore.ui.views.RatingBar;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

/**
 * Fragment that presents the movie list
 */
public class MovieListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(MovieListFragment.class);

    public interface OnMovieSelectedListener {
        void onMovieSelected(AbstractFragment.DataHolder dataHolder, ImageView sharedImageView);
    }

    // Activity listener
    private OnMovieSelectedListener listenerActivity;

    private static boolean showWatchedStatus;
    private static boolean showRating;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MOVIES; }

    @Override
    protected void onListItemClicked(View view, int position) {
        super.onListItemClicked(view, position);
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onMovieSelected(tag.dataHolder, tag.artView);
    }

    @Override
    protected String getEmptyResultsTitle() { return getString(R.string.no_movies_found_refresh); }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new MoviesAdapter(requireContext());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        Uri uri = MediaContract.Movies.buildMoviesListUri(hostInfo != null? hostInfo.getId() : -1);

        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection.append(MediaContract.MoviesColumns.TITLE + " LIKE ?");
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED)) {
            if (selection.length() != 0)
                selection.append(" AND ");
            selection.append(MediaContract.MoviesColumns.PLAYCOUNT)
                     .append("=0");
        }

        showWatchedStatus = preferences.getBoolean(Settings.KEY_PREF_MOVIES_SHOW_WATCHED_STATUS, Settings.DEFAULT_PREF_MOVIES_SHOW_WATCHED_STATUS);
        showRating = preferences.getBoolean(Settings.KEY_PREF_MOVIES_SHOW_RATING, Settings.DEFAULT_PREF_MOVIES_SHOW_RATING);

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

        return new CursorLoader(requireContext(), uri,
                                MovieListQuery.PROJECTION, selection.toString(), selectionArgs, sortOrderStr);
    }

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        try {
            listenerActivity = (OnMovieSelectedListener) ctx;
        } catch (ClassCastException e) {
            throw new ClassCastException(ctx + " must implement OnMovieSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
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
                showWatchedStatusMenuItem = menu.findItem(R.id.action_show_watched_status),
                showRatingMenuItem = menu.findItem(R.id.action_show_rating);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED));
        showWatchedStatusMenuItem.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_SHOW_WATCHED_STATUS, Settings.DEFAULT_PREF_MOVIES_SHOW_WATCHED_STATUS));
        ignoreArticles.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, Settings.DEFAULT_PREF_MOVIES_IGNORE_PREFIXES));
        showRatingMenuItem.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_SHOW_RATING, Settings.DEFAULT_PREF_MOVIES_SHOW_RATING));

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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int itemId = item.getItemId();
        if (itemId == R.id.action_hide_watched) {
            item.setChecked(!item.isChecked());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, item.isChecked())
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_show_watched_status) {
            item.setChecked(!item.isChecked());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_MOVIES_SHOW_WATCHED_STATUS, item.isChecked())
                       .apply();
            showWatchedStatus = item.isChecked();
            restartLoader();
        } else if (itemId == R.id.action_show_rating) {
            item.setChecked(!item.isChecked());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_MOVIES_SHOW_RATING, item.isChecked())
                       .apply();
            showRating = item.isChecked();
            restartLoader();
        } else if (itemId == R.id.action_ignore_prefixes) {
            item.setChecked(!item.isChecked());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, item.isChecked())
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_sort_by_name) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_NAME)
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_sort_by_year) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_YEAR)
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_sort_by_rating) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_RATING)
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_sort_by_date_added) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_sort_by_last_played) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_LAST_PLAYED)
                       .apply();
            restartLoader();
        } else if (itemId == R.id.action_sort_by_length) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_LENGTH)
                       .apply();
            restartLoader();
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
                MediaContract.Movies.POSTER,
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

        private final HostManager hostManager;
        private final int artWidth, artHeight;

        MoviesAdapter(Context context) {
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.info_poster_width) / UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.info_poster_height) / UIUtils.IMAGE_RESIZE_FACTOR);
        }

        @NonNull
        @Override
        public CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(getContext())
                                            .inflate(R.layout.item_movie, parent, false);

            return new ViewHolder(view, getContext(), hostManager, artWidth, artHeight);
        }

        protected int getSectionColumnIdx() {
            int sortOrder = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
            if (sortOrder == Settings.SORT_BY_YEAR) {
                return MovieListQuery.YEAR;
            } else {
                return MovieListQuery.TITLE;
            }
        }

        protected int getSectionType() {
            int sortOrder = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
            if (sortOrder == Settings.SORT_BY_YEAR) {
                return RecyclerViewCursorAdapter.SECTION_TYPE_YEAR_INTEGER;
            } else {
                return RecyclerViewCursorAdapter.SECTION_TYPE_ALPHANUMERIC;
            }
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView metaInfoView;
        RatingBar ratingBar;
        ImageView checkmarkView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        AbstractFragment.DataHolder dataHolder = new AbstractFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            metaInfoView = itemView.findViewById(R.id.meta_info);
            checkmarkView = itemView.findViewById(R.id.watched_check);
            artView = itemView.findViewById(R.id.art);
            ratingBar = itemView.findViewById(R.id.rating_bar);
        }

        @Override
        public void bindView(Cursor cursor) {
            // Save the movie id
            dataHolder.setId(cursor.getInt(MovieListQuery.MOVIEID));
            dataHolder.setTitle(cursor.getString(MovieListQuery.TITLE));
            dataHolder.setUndertitle(cursor.getString(MovieListQuery.TAGLINE));

            dataHolder.setRating(cursor.getDouble(MovieListQuery.RATING));

            titleView.setText(dataHolder.getTitle());

            String genres = cursor.getString(MovieListQuery.GENRES);
            String details = TextUtils.isEmpty(dataHolder.getUnderTitle()) ?
                             genres : dataHolder.getUnderTitle();
            detailsView.setText(details);

            String metaInfo = getMetaInfo(cursor);
            metaInfoView.setText(metaInfo);
            dataHolder.setDetails(metaInfo + "\n" + details);

            if (showRating && dataHolder.getRating() > 0) {
                ratingBar.setMaxRating(10);
                ratingBar.setRating(dataHolder.getRating());
                ratingBar.setVisibility(View.VISIBLE);
            } else {
                ratingBar.setVisibility(View.GONE);
            }

            dataHolder.setPosterUrl(cursor.getString(MovieListQuery.THUMBNAIL));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 dataHolder.getPosterUrl(),
                                                 dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            if (showWatchedStatus && (cursor.getInt(MovieListQuery.PLAYCOUNT) > 0)) {
                checkmarkView.setVisibility(View.VISIBLE);
            }
            else {
                checkmarkView.clearColorFilter();
                checkmarkView.setVisibility(View.GONE);
            }

            artView.setTransitionName("movie" + dataHolder.getId());
        }

        private String getMetaInfo(Cursor cursor) {
            int runtime = cursor.getInt(MovieListQuery.RUNTIME) / 60;
            int movieYear = cursor.getInt(MovieListQuery.YEAR);

            ArrayList<String> duration = new ArrayList<>();
            if (runtime > 0)
                duration.add(String.format(context.getString(R.string.minutes_abbrev),
                                           String.valueOf(runtime)));
            duration.add(String.valueOf(movieYear));

            return TextUtils.join(" | ", duration);
        }
    }
}
