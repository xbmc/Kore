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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * Fragment that presents the tv show list
 */
public class TVShowListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        SearchView.OnQueryTextListener {
    private static final String TAG = LogUtils.makeLogTag(TVShowListFragment.class);

    public interface OnTVShowSelectedListener {
        public void onTVShowSelected(int tvshowId, String tvshowTitle);
    }

    // Loader IDs
    private static final int LOADER_TVSHOWS = 0;

    // The search filter to use in the loader
    private String searchFilter = null;

    // Movies adapter
    private CursorAdapter adapter;

    // Activity listener
    private OnTVShowSelectedListener listenerActivity;

    private HostManager hostManager;
    private HostInfo hostInfo;
    private EventBus bus;

    @InjectView(R.id.list) GridView tvshowsGridView;
    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(android.R.id.empty) TextView emptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
        ButterKnife.inject(this, root);

        bus = EventBus.getDefault();
        hostManager = HostManager.getInstance(getActivity());
        hostInfo = hostManager.getHostInfo();

        swipeRefreshLayout.setOnRefreshListener(this);
        //UIUtils.setSwipeRefreshLayoutColorScheme(swipeRefreshLayout);

        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), moviesGridView, false, false, true);
//        moviesGridView.setClipToPadding(false);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        tvshowsGridView.setEmptyView(emptyView);
        tvshowsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onTVShowSelected(tag.tvshowId, tag.tvshowTitle);
            }
        });

        // Configure the adapter and start the loader
        adapter = new TVShowsAdapter(getActivity());
        tvshowsGridView.setAdapter(adapter);
        getLoaderManager().initLoader(LOADER_TVSHOWS, null, this);

        setHasOptionsMenu(true);
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
    public void onResume() {
        bus.register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        bus.unregister(this);
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tvshow_list, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search_tvshows));

        // Setup filters
        MenuItem hideWatched = menu.findItem(R.id.action_hide_watched),
                ignoreArticles = menu.findItem(R.id.action_ignore_prefixes),
                sortByName = menu.findItem(R.id.action_sort_by_name),
                sortByYear = menu.findItem(R.id.action_sort_by_year),
                sortByRating = menu.findItem(R.id.action_sort_by_rating),
                sortByDateAdded = menu.findItem(R.id.action_sort_by_date_added);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOWS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOWS_FILTER_HIDE_WATCHED));
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
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                break;
            case R.id.action_ignore_prefixes:
                item.setChecked(!item.isChecked());
                preferences.edit()
                        .putBoolean(Settings.KEY_PREF_TVSHOWS_IGNORE_PREFIXES, item.isChecked())
                        .apply();
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                break;
            case R.id.action_sort_by_name:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_NAME)
                        .apply();
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                break;
            case R.id.action_sort_by_year:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_YEAR)
                        .apply();
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                break;
            case R.id.action_sort_by_rating:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_RATING)
                        .apply();
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                break;
            case R.id.action_sort_by_date_added:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_TVSHOWS_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                        .apply();
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Search view callbacks
     */
    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextChange(String newText) {
        searchFilter = newText;
        getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextSubmit(String newText) {
        // All is handled in onQueryTextChange
        return true;
    }

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh() {
        if (hostInfo != null) {
            LogUtils.LOGD(TAG, "Starting onRefresh");
            // Make sure we're showing the refresh
            swipeRefreshLayout.setRefreshing(true);
            // Start the syncing process
            Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
            syncIntent.putExtra(LibrarySyncService.SYNC_ALL_TVSHOWS, true);
            getActivity().startService(syncIntent);
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * Event bus post. Called when the syncing process ended
     *
     * @param event Refreshes data
     */
    public void onEventMainThread(MediaSyncEvent event) {
        boolean silentSync = false;
        if (event.syncExtras != null) {
            silentSync = event.syncExtras.getBoolean(LibrarySyncService.SILENT_SYNC, false);
        }

        if (event.syncType.equals(LibrarySyncService.SYNC_SINGLE_TVSHOW) ||
            event.syncType.equals(LibrarySyncService.SYNC_ALL_TVSHOWS)) {
            swipeRefreshLayout.setRefreshing(false);
            if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                getLoaderManager().restartLoader(LOADER_TVSHOWS, null, this);
                if (!silentSync) {
                    Toast.makeText(getActivity(), R.string.sync_successful, Toast.LENGTH_SHORT)
                         .show();
                }
            } else if (!silentSync) {
                String msg = (event.errorCode == ApiException.API_ERROR) ?
                             String.format(getString(R.string.error_while_syncing), event.errorMessage) :
                             getString(R.string.unable_to_connect_to_xbmc);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = MediaContract.TVShows.buildTVShowsListUri(hostInfo != null ? hostInfo.getId() : -1);

        StringBuilder selection = new StringBuilder();
        String selectionArgs[] = null;
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
        } else if (sortOrder == Settings.SORT_BY_YEAR) {
            sortOrderStr = TVShowListQuery.SORT_BY_YEAR;
        } else if (sortOrder == Settings.SORT_BY_RATING) {
            sortOrderStr = TVShowListQuery.SORT_BY_RATING;
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

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        adapter.swapCursor(cursor);
        // To prevent the empty text from appearing on the first load, set it now
        emptyView.setText(getString(R.string.no_tvshows_found_refresh));
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.swapCursor(null);
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
        String SORT_BY_YEAR = MediaContract.TVShows.PREMIERED + " DESC";
        String SORT_BY_RATING = MediaContract.TVShows.RATING + " DESC";
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
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.tvshowlist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.tvshowlist_art_heigth) /
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
