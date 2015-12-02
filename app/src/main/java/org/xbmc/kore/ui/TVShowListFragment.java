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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Fragment that presents the tv show list
 */
public class TVShowListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(TVShowListFragment.class);

    public interface OnTVShowSelectedListener {
        public void onTVShowSelected(int tvshowId, String tvshowTitle);
    }

    // Activity listener
    private OnTVShowSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_TVSHOWS; }

    @Override
    protected AdapterView.OnItemClickListener createOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onTVShowSelected(tag.tvshowId, tag.tvshowTitle);
            }
        };
    }

    @Override
    protected CursorAdapter createAdapter() {
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

        String sortOrderStr;
        int sortOrder = preferences.getInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.DEFAULT_PREF_TVSHOWS_SORT_ORDER);
        if (sortOrder == Settings.SORT_BY_DATE_ADDED) {
            sortOrderStr = TVShowListQuery.SORT_BY_DATE_ADDED;
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
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search_tvshows));

        // Setup filters
        MenuItem hideWatched = menu.findItem(R.id.action_hide_watched),
                ignoreArticles = menu.findItem(R.id.action_ignore_prefixes),
                sortByName = menu.findItem(R.id.action_sort_by_name),
                sortByDateAdded = menu.findItem(R.id.action_sort_by_date_added);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOWS_FILTER_HIDE_WATCHED));
        ignoreArticles.setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_IGNORE_PREFIXES, Settings.DEFAULT_PREF_TVSHOWS_IGNORE_PREFIXES));

        int sortOrder = preferences.getInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.DEFAULT_PREF_TVSHOWS_SORT_ORDER);
        switch (sortOrder) {
            case Settings.SORT_BY_DATE_ADDED:
                sortByDateAdded.setChecked(true);
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
            case R.id.action_sort_by_date_added:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
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
                MediaContract.TVShows.PREMIERED,
                MediaContract.TVShows.EPISODE,
                MediaContract.TVShows.WATCHEDEPISODES,
        };

        String SORT_BY_NAME = MediaContract.TVShows.TITLE + " ASC";
        String SORT_BY_DATE_ADDED = MediaContract.TVShows.DATEADDED + " DESC";
        String SORT_BY_NAME_IGNORE_ARTICLES = MediaDatabase.sortCommonTokens(MediaContract.TVShows.TITLE) + " ASC";

        final int ID = 0;
        final int TVSHOWID = 1;
        final int TITLE = 2;
        final int THUMBNAIL = 3;
        final int PREMIERED = 4;
        final int EPISODE = 5;
        final int WATCHEDEPISODES = 6;
    }

    private static class TVShowsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public TVShowsAdapter(Context context) {
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
                                            .inflate(R.layout.grid_item_tvshow, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.titleView = (TextView)view.findViewById(R.id.title);
            viewHolder.detailsView = (TextView)view.findViewById(R.id.details);
//            viewHolder.yearView = (TextView)view.findViewById(R.id.year);
            viewHolder.premieredView = (TextView)view.findViewById(R.id.premiered);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            // Save the movie id
            viewHolder.tvshowId = cursor.getInt(TVShowListQuery.TVSHOWID);
            viewHolder.tvshowTitle = cursor.getString(TVShowListQuery.TITLE);

            viewHolder.titleView.setText(viewHolder.tvshowTitle);
            int numEpisodes = cursor.getInt(TVShowListQuery.EPISODE),
                    watchedEpisodes = cursor.getInt(TVShowListQuery.WATCHEDEPISODES);
            String details = String.format(context.getString(R.string.num_episodes),
                    numEpisodes, numEpisodes - watchedEpisodes);
            viewHolder.detailsView.setText(details);

            String premiered = String.format(context.getString(R.string.premiered),
                    cursor.getString(TVShowListQuery.PREMIERED));
            viewHolder.premieredView.setText(premiered);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    cursor.getString(TVShowListQuery.THUMBNAIL), viewHolder.tvshowTitle,
                    viewHolder.artView, artWidth, artHeight);
        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder {
        TextView titleView;
        TextView detailsView;
//        TextView yearView;
        TextView premieredView;
        ImageView artView;

        int tvshowId;
        String tvshowTitle;
    }
}
