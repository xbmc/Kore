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
package com.syncedsynapse.kore2.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.ApiException;
import com.syncedsynapse.kore2.jsonrpc.event.MediaSyncEvent;
import com.syncedsynapse.kore2.provider.MediaContract;
import com.syncedsynapse.kore2.service.LibrarySyncService;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * Fragment that presents the albums list
 */
public class AlbumListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        SearchView.OnQueryTextListener {
    private static final String TAG = LogUtils.makeLogTag(AlbumListFragment.class);

    public interface OnAlbumSelectedListener {
        public void onAlbumSelected(int albumId, String albumTitle);
    }

    // Loader IDs
    private static final int LOADER_ALBUMS = 0;

    private static final String GENREID = "genreid",
            ARTISTID = "artistid";

    // The search filter to use in the loader
    private String searchFilter = null;
    private int genreId = -1;
    private int artistId = -1;

    // Movies adapter
    private CursorAdapter adapter;

    // Activity listener
    private OnAlbumSelectedListener listenerActivity;

    private HostInfo hostInfo;
    private EventBus bus;

    @InjectView(R.id.list) GridView albumsGridView;
    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(android.R.id.empty) TextView emptyView;

    /**
     * Create a new instance of this, initialized to show albums of genres
     */
    public static AlbumListFragment newInstanceForGenre(final int genreId) {
        AlbumListFragment fragment = new AlbumListFragment();

        Bundle args = new Bundle();
        args.putInt(GENREID, genreId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new instance of this, initialized to show albums of artists
     */
    public static AlbumListFragment newInstanceForArtist(final int artistId) {
        AlbumListFragment fragment = new AlbumListFragment();

        Bundle args = new Bundle();
        args.putInt(ARTISTID, artistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            genreId = getArguments().getInt(GENREID, -1);
            artistId = getArguments().getInt(ARTISTID, -1);
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
        ButterKnife.inject(this, root);

        bus = EventBus.getDefault();
        hostInfo = HostManager.getInstance(getActivity()).getHostInfo();

        swipeRefreshLayout.setOnRefreshListener(this);

        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), albumsGridView, false, false, true);
//        albumsGridView.setClipToPadding(false);

        return root;
    }


    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        albumsGridView.setEmptyView(emptyView);
        albumsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onAlbumSelected(tag.albumId, tag.albumTitle);
            }
        });

        // Configure the adapter and start the loader
        adapter = new AlbumsAdapter(getActivity());
        albumsGridView.setAdapter(adapter);
        getLoaderManager().initLoader(LOADER_ALBUMS, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnAlbumSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAlbumSelectedListener");
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
        inflater.inflate(R.menu.media_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search_albums));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Search view callbacks
     */
    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextChange(String newText) {
        searchFilter = newText;
        getLoaderManager().restartLoader(LOADER_ALBUMS, null, this);
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
    public void onRefresh () {
        if (hostInfo != null) {
            // Make sure we're showing the refresh
            swipeRefreshLayout.setRefreshing(true);
            // Start the syncing process
            Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
            syncIntent.putExtra(LibrarySyncService.SYNC_ALL_MUSIC, true);
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
        if (event.syncType.equals(LibrarySyncService.SYNC_ALL_MUSIC)) {
            swipeRefreshLayout.setRefreshing(false);
            if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                getLoaderManager().restartLoader(LOADER_ALBUMS, null, this);
                Toast.makeText(getActivity(), R.string.sync_successful, Toast.LENGTH_SHORT)
                     .show();
            } else {
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
        Uri uri;
        int hostId = hostInfo != null ? hostInfo.getId() : -1;

        if (artistId != -1) {
            uri = MediaContract.AlbumArtists.buildAlbumsForArtistListUri(hostId, artistId);
        } else if (genreId != -1) {
            uri = MediaContract.AlbumGenres.buildAlbumsForGenreListUri(hostId, genreId);
        } else {
            uri = MediaContract.Albums.buildAlbumsListUri(hostId);
        }

        String selection = null;
        String selectionArgs[] = null;
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.Albums.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(getActivity(), uri,
                AlbumListQuery.PROJECTION, selection, selectionArgs, AlbumListQuery.SORT);
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        adapter.swapCursor(cursor);
        // To prevent the empty text from appearing on the first load, set it now
        emptyView.setText(getString(R.string.no_albums_found_refresh));
        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.swapCursor(null);
    }

    /**
     * Album list query parameters.
     */
    private interface AlbumListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Albums.ALBUMID,
                MediaContract.Albums.TITLE,
                MediaContract.Albums.DISPLAYARTIST,
                MediaContract.Albums.GENRE,
                MediaContract.Albums.THUMBNAIL,
                MediaContract.Albums.YEAR,
        };

        String SORT = MediaContract.Albums.TITLE + " ASC";

        final int ID = 0;
        final int ALBUMID = 1;
        final int TITLE = 2;
        final int DISPLAYARTIST = 3;
        final int GENRE = 4;
        final int THUMBNAIL = 5;
        final int YEAR = 6;
    }

    private static class AlbumsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public AlbumsAdapter(Context context) {
            super(context, null, false);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.albumlist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.albumlist_art_heigth) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                                            .inflate(R.layout.grid_item_album, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.titleView = (TextView)view.findViewById(R.id.title);
            viewHolder.artistView = (TextView)view.findViewById(R.id.name);
            viewHolder.genresView = (TextView)view.findViewById(R.id.genres);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            viewHolder.albumId = cursor.getInt(AlbumListQuery.ALBUMID);
            viewHolder.albumTitle = cursor.getString(AlbumListQuery.TITLE);

            viewHolder.titleView.setText(viewHolder.albumTitle);
            viewHolder.artistView.setText(cursor.getString(AlbumListQuery.DISPLAYARTIST));
            int year = cursor.getInt(AlbumListQuery.YEAR);
            String genres = cursor.getString(AlbumListQuery.GENRE);
            String desc = (genres != null) ?
                          ((year > 0) ? genres + "  |  " + year : genres) :
                          String.valueOf(year);
            viewHolder.genresView.setText(desc);

            String thumbnail = cursor.getString(AlbumListQuery.THUMBNAIL);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    thumbnail, viewHolder.albumTitle,
                    viewHolder.artView, artWidth, artHeight);
        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder {
        TextView titleView;
        TextView artistView;
        TextView genresView;
        ImageView artView;

        int albumId;
        String albumTitle;
    }
}
