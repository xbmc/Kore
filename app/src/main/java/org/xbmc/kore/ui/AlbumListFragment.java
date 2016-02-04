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
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.PopupMenu;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Fragment that presents the albums list
 */
public class AlbumListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(AlbumListFragment.class);

    public interface OnAlbumSelectedListener {
        public void onAlbumSelected(ViewHolder vh);
    }

    private static final String GENREID = "genreid",
            ARTISTID = "artistid";

    private int genreId = -1;
    private int artistId = -1;

    // Activity listener
    private OnAlbumSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

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
    protected AdapterView.OnItemClickListener createOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onAlbumSelected(tag);
            }
        };
    }

    @Override
    protected CursorAdapter createAdapter() {
        return new AlbumsAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        Uri uri;
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
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
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.Albums.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(getActivity(), uri,
                AlbumListQuery.PROJECTION, selection, selectionArgs, AlbumListQuery.SORT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            genreId = getArguments().getInt(GENREID, -1);
            artistId = getArguments().getInt(ARTISTID, -1);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            // HACK: Fix crash reported on Play Store. Why does this is necessary is beyond me
            super.onCreateOptionsMenu(menu, inflater);
            return;
        }

        inflater.inflate(R.menu.media_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search_albums));
        super.onCreateOptionsMenu(menu, inflater);
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
                MediaContract.Albums.RATING,
        };

        String SORT = MediaDatabase.sortCommonTokens(MediaContract.Albums.TITLE) + " ASC";

        final int ID = 0;
        final int ALBUMID = 1;
        final int TITLE = 2;
        final int DISPLAYARTIST = 3;
        final int GENRE = 4;
        final int THUMBNAIL = 5;
        final int YEAR = 6;
        final int RATING = 7;
    }

    private class AlbumsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public AlbumsAdapter(Context context) {
            super(context, null, false);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = (int) resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_width);
            artHeight = (int) resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_heigth);
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
        @TargetApi(21)
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            viewHolder.albumId = cursor.getInt(AlbumListQuery.ALBUMID);
            viewHolder.albumTitle = cursor.getString(AlbumListQuery.TITLE);
            viewHolder.albumArtist = cursor.getString(AlbumListQuery.DISPLAYARTIST);
            viewHolder.albumGenre = cursor.getString(AlbumListQuery.GENRE);
            viewHolder.albumYear = cursor.getInt(AlbumListQuery.YEAR);
            viewHolder.albumRating = cursor.getDouble(AlbumListQuery.RATING);

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

            // For the popupmenu
            ImageView contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
            contextMenu.setTag(viewHolder);
            contextMenu.setOnClickListener(albumlistItemMenuClickListener);

            if(Utils.isLollipopOrLater()) {
                viewHolder.artView.setTransitionName("a"+viewHolder.albumId);
            }
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        TextView titleView;
        TextView artistView;
        TextView genresView;
        ImageView artView;

        int albumId;
        String albumTitle;
        String albumArtist;
        int albumYear;
        String albumGenre;
        double albumRating;
    }

    private View.OnClickListener albumlistItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final ViewHolder viewHolder = (ViewHolder)v.getTag();

            final PlaylistType.Item playListItem = new PlaylistType.Item();
            playListItem.albumid = viewHolder.albumId;

            final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_play:
                            MediaPlayerUtils.play(AlbumListFragment.this, playListItem);
                            return true;
                        case R.id.action_queue:
                            MediaPlayerUtils.queue(AlbumListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };
}
