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
package org.xbmc.kore.ui.sections.audio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
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

/**
 * Fragment that presents the albums list
 */
public class AlbumListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(AlbumListFragment.class);

    public interface OnAlbumSelectedListener {
        public void onAlbumSelected(ViewHolder viewHolder);
    }

    public static final String BUNDLE_KEY_GENREID = "genreid",
            BUNDLE_KEY_ARTISTID = "artistid";

    private int genreId = -1;
    private int artistId = -1;

    // Activity listener
    private OnAlbumSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    /**
     * Use this to display all albums for a specific artist
     * @param artistId
     */
    public void setArtist(int artistId) {
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_ARTISTID, artistId);
        setArguments(args);
    }

    /**
     * Use this to display all albums for a specific genre
     * @param genreId genre id
     */
    public void setGenre(int genreId) {
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_GENREID, genreId);
        setArguments(args);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.album_list, menu);

        MenuItem sortByAlbum = menu.findItem(R.id.action_sort_by_album),
                sortByArtist = menu.findItem(R.id.action_sort_by_artist),
                sortByArtistYear = menu.findItem(R.id.action_sort_by_artist_year);


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int sortOrder = preferences.getInt(Settings.KEY_PREF_ALBUMS_SORT_ORDER, Settings.DEFAULT_PREF_ALBUMS_SORT_ORDER);
        switch (sortOrder) {
            case Settings.SORT_BY_ALBUM:
                sortByAlbum.setChecked(true);
                break;
            case Settings.SORT_BY_ARTIST:
                sortByArtist.setChecked(true);
                break;
            case Settings.SORT_BY_ARTIST_YEAR:
                sortByArtistYear.setChecked(true);
                break;
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        switch (item.getItemId()) {
            case R.id.action_sort_by_album:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putInt(Settings.KEY_PREF_ALBUMS_SORT_ORDER, Settings.SORT_BY_ALBUM)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_artist:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putInt(Settings.KEY_PREF_ALBUMS_SORT_ORDER, Settings.SORT_BY_ARTIST)
                           .apply();
                refreshList();
                break;
            case R.id.action_sort_by_artist_year:
                item.setChecked(!item.isChecked());
                preferences.edit()
                           .putInt(Settings.KEY_PREF_ALBUMS_SORT_ORDER, Settings.SORT_BY_ARTIST_YEAR)
                           .apply();
                refreshList();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onAlbumSelected(tag);
    }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new AlbumsAdapter(this);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String sortOrderStr;
        int sortOrder = preferences.getInt(Settings.KEY_PREF_ALBUMS_SORT_ORDER, Settings.DEFAULT_PREF_ALBUMS_SORT_ORDER);
        if (sortOrder == Settings.SORT_BY_ARTIST) {
            sortOrderStr = AlbumListQuery.SORT_BY_ARTIST;
        } else if (sortOrder == Settings.SORT_BY_ARTIST_YEAR) {
            sortOrderStr = AlbumListQuery.SORT_BY_ARTIST_YEAR;
        } else {
            sortOrderStr = AlbumListQuery.SORT_BY_ALBUM;
        }

        return new CursorLoader(getActivity(), uri,
                                AlbumListQuery.PROJECTION, selection, selectionArgs, sortOrderStr);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            genreId = getArguments().getInt(BUNDLE_KEY_GENREID, -1);
            artistId = getArguments().getInt(BUNDLE_KEY_ARTISTID, -1);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
        try {
            listenerActivity = (OnAlbumSelectedListener) ctx;
        } catch (ClassCastException e) {
            throw new ClassCastException(ctx.toString() + " must implement OnAlbumSelectedListener");
        }

        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    /**
     * Album list query parameters.
     */
    public interface AlbumListQuery {
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

        String SORT_BY_ALBUM = MediaDatabase.sortCommonTokens(MediaContract.Albums.TITLE) + " COLLATE NOCASE ASC";
        String SORT_BY_ARTIST = MediaDatabase.sortCommonTokens(MediaContract.Albums.DISPLAYARTIST) + " COLLATE NOCASE ASC";
        String SORT_BY_ARTIST_YEAR = MediaDatabase.sortCommonTokens(MediaContract.Albums.DISPLAYARTIST)
                                     + " COLLATE NOCASE ASC, " + MediaContract.Albums.YEAR + " ASC";

        int ID = 0;
        int ALBUMID = 1;
        int TITLE = 2;
        int DISPLAYARTIST = 3;
        int GENRE = 4;
        int THUMBNAIL = 5;
        int YEAR = 6;
        int RATING = 7;
    }

    private static class AlbumsAdapter extends RecyclerViewCursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;
        private Fragment fragment;

        public AlbumsAdapter(Fragment fragment) {
            this.hostManager = HostManager.getInstance(fragment.getContext());
            this.fragment = fragment;

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = fragment.getContext().getResources();
            artWidth = resources.getDimensionPixelOffset(R.dimen.detail_poster_width_square);
            artHeight = resources.getDimensionPixelOffset(R.dimen.detail_poster_height_square);
        }

        @Override
        public CursorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(fragment.getContext())
                                      .inflate(R.layout.grid_item_album, parent, false);

            return new ViewHolder(view, fragment.getContext(), hostManager, artWidth, artHeight,
                                  albumlistItemMenuClickListener);
        }

        private View.OnClickListener albumlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ViewHolder viewHolder = (ViewHolder)v.getTag();

                final PlaylistType.Item playListItem = new PlaylistType.Item();
                playListItem.albumid = viewHolder.dataHolder.getId();

                final PopupMenu popupMenu = new PopupMenu(fragment.getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_play:
                                MediaPlayerUtils.play(fragment, playListItem);
                                return true;
                            case R.id.action_queue:
                                MediaPlayerUtils.queue(fragment, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        };

        protected int getSectionColumnIdx() { return AlbumListQuery.TITLE; }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView artistView;
        TextView genresView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight,
                   View.OnClickListener contextMenuClickListener) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            titleView = itemView.findViewById(R.id.title);
            artistView = itemView.findViewById(R.id.name);
            genresView = itemView.findViewById(R.id.genres);
            artView = itemView.findViewById(R.id.art);

            // For the popupmenu
            ImageView contextMenu = itemView.findViewById(R.id.list_context_menu);
            contextMenu.setTag(this);
            contextMenu.setOnClickListener(contextMenuClickListener);
        }
        @Override
        public void bindView(Cursor cursor) {
            dataHolder.setId(cursor.getInt(AlbumListQuery.ALBUMID));
            dataHolder.setTitle(cursor.getString(AlbumListQuery.TITLE));
            dataHolder.setUndertitle(cursor.getString(AlbumListQuery.DISPLAYARTIST));

            titleView.setText(dataHolder.getTitle());
            artistView.setText(dataHolder.getUnderTitle());
            int year = cursor.getInt(AlbumListQuery.YEAR);
            String genres = cursor.getString(AlbumListQuery.GENRE);
            String desc = (genres != null) ?
                          ((year > 0) ? genres + "  |  " + year : genres) :
                          String.valueOf(year);
            dataHolder.setDescription(desc);
            genresView.setText(desc);

            dataHolder.setPosterUrl(cursor.getString(AlbumListQuery.THUMBNAIL));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 dataHolder.getPosterUrl(),
                                                 dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            if (Utils.isLollipopOrLater()) {
                artView.setTransitionName("al"+dataHolder.getId());
            }
        }
    }
}
