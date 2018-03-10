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
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
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
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

/**
 * Fragment that presents the songs list
 */
public class SongsListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(SongsListFragment.class);

    public static final String BUNDLE_KEY_ARTISTID = "artistid";
    public static final String BUNDLE_KEY_ALBUMID = "albumid";
    public static final String BUNDLE_KEY_ALBUMTITLE = "albumtitle";

    private int artistId = -1;
    private int albumId = -1;
    private String albumTitle = "";

    private Handler callbackHandler = new Handler();

    /**
     * Use this to display all songs for a specific artist
     * @param artistId
     */
    public void setArtist(int artistId) {
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_ARTISTID, artistId);
        setArguments(args);
    }

    /**
     * Use this to display all songs for a specific album
     * @param albumId
     */
    public void setAlbum(int albumId, String albumTitle) {
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_ALBUMID, albumId);
        args.putString(BUNDLE_KEY_ALBUMTITLE, albumTitle);
        setArguments(args);
    }

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    @Override
    protected CursorAdapter createAdapter() {
        if (albumId != -1 ) {
            return new AlbumSongsAdapter(getActivity());
        } else {
            return new SongsAdapter(getActivity());
        }
    }

    @Override
    protected void onListItemClicked(View view) {
        ImageView contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
        showPopupMenu(contextMenu);
    }

    @Override
    protected CursorLoader createCursorLoader() {
        Uri uri;
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        int hostId = hostInfo != null ? hostInfo.getId() : -1;

        if (artistId != -1) { // get songs for artist
            uri = MediaContract.Songs.buildArtistSongsListUri(hostId, artistId);
        } else if (albumId != -1) {
            uri = MediaContract.Songs.buildAlbumSongsListUri(hostId, albumId);
        } else { // get all songs
            uri = MediaContract.Songs.buildSongsListUri(hostId);
        }

        String selection = null;
        String selectionArgs[] = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        if (albumId != -1) {
            return new CursorLoader(getActivity(), uri,
                                    AlbumSongsListQuery.PROJECTION, selection, selectionArgs, AlbumSongsListQuery.SORT);
        } else {
            return new CursorLoader(getActivity(), uri,
                                    SongsListQuery.PROJECTION, selection, selectionArgs, SongsListQuery.SORT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        setSupportsSearch(true);
        super.onAttach(activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            artistId = arguments.getInt(BUNDLE_KEY_ARTISTID, -1);
            albumId = arguments.getInt(BUNDLE_KEY_ALBUMID, -1);
            albumTitle = arguments.getString(BUNDLE_KEY_ALBUMTITLE, "");
        }
        super.onCreate(savedInstanceState);
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
     * Album songs list query parameters.
     */
    public interface SongsListQuery {
        String[] PROJECTION = {
                MediaProvider.Qualified.SONGS_ID,
                MediaProvider.Qualified.SONGS_TITLE,
                MediaProvider.Qualified.SONGS_TRACK,
                MediaProvider.Qualified.SONGS_DURATION,
                MediaProvider.Qualified.SONGS_FILE,
                MediaProvider.Qualified.SONGS_SONGID,
                MediaProvider.Qualified.SONGS_DISPLAYARTIST,
                MediaProvider.Qualified.ALBUMS_TITLE,
                MediaProvider.Qualified.ALBUMS_GENRE,
                MediaProvider.Qualified.ALBUMS_YEAR,
                MediaProvider.Qualified.ALBUMS_THUMBNAIL
        };

        String SORT = MediaDatabase.sortCommonTokens(MediaProvider.Qualified.SONGS_TITLE) + " COLLATE NOCASE ASC";

        int ID = 0;
        int TITLE = 1;
        int TRACK = 2;
        int DURATION = 3;
        int FILE = 4;
        int SONGID = 5;
        int SONGDISPLAYARTIST = 6;
        int ALBUMTITLE = 7;
        int GENRE = 8;
        int YEAR = 9;
        int THUMBNAIL = 10;
    }

    /**
     * Album songs list query parameters.
     */
    public interface AlbumSongsListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Songs.TITLE,
                MediaContract.Songs.TRACK,
                MediaContract.Songs.DURATION,
                MediaContract.Songs.FILE,
                MediaContract.Songs.SONGID,
                MediaContract.Songs.DISPLAYARTIST,
                MediaContract.Songs.DISC
        };

        String SORT = MediaContract.Songs.DISC + " ASC, " + MediaContract.Songs.TRACK + " ASC";

        int ID = 0;
        int TITLE = 1;
        int TRACK = 2;
        int DURATION = 3;
        int FILE = 4;
        int SONGID = 5;
        int ARTIST = 6;
        int DISC = 7;
    }

    private class SongsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public SongsAdapter(Context context) {
            super(context, null, 0);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = resources.getDimensionPixelOffset(R.dimen.detail_poster_width_square);
            artHeight = resources.getDimensionPixelOffset(R.dimen.detail_poster_height_square);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context)
                                            .inflate(R.layout.grid_item_song, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.title = (TextView)view.findViewById(R.id.title);
            viewHolder.details = (TextView)view.findViewById(R.id.details);
            viewHolder.art = (ImageView)view.findViewById(R.id.art);
            viewHolder.artist = (TextView)view.findViewById(R.id.artist);
            viewHolder.songInfo = new FileDownloadHelper.SongInfo();
            viewHolder.contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            String title = cursor.getString(SongsListQuery.TITLE);

            viewHolder.title.setText(title);

            String artist = cursor.getString(SongsListQuery.SONGDISPLAYARTIST);
            viewHolder.artist.setText(artist);

            String albumTitle = cursor.getString(SongsListQuery.ALBUMTITLE);
            int year = cursor.getInt(SongsListQuery.YEAR);
            if (year > 0) {
                setDetails(viewHolder.details,
                           albumTitle,
                           String.valueOf(year),
                           cursor.getString(SongsListQuery.GENRE));
            } else {
                setDetails(viewHolder.details,
                           albumTitle,
                           cursor.getString(SongsListQuery.GENRE));
            }

            String thumbnail = cursor.getString(SongsListQuery.THUMBNAIL);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 thumbnail, title,
                                                 viewHolder.art, artWidth, artHeight);

            viewHolder.songInfo.artist = artist;
            viewHolder.songInfo.album = albumTitle;
            viewHolder.songInfo.songId = cursor.getInt(SongsListQuery.SONGID);
            viewHolder.songInfo.title = cursor.getString(SongsListQuery.TITLE);
            viewHolder.songInfo.fileName = cursor.getString(SongsListQuery.FILE);
            viewHolder.songInfo.track = cursor.getInt(SongsListQuery.TRACK);

            // For the popupmenu
            viewHolder.contextMenu.setTag(viewHolder);
            viewHolder.contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    showPopupMenu(v);
                }
            });
        }
    }

    private class AlbumSongsAdapter extends CursorAdapter {

        public AlbumSongsAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View view = LayoutInflater.from(context)
                                      .inflate(R.layout.list_item_song, viewGroup, false);
            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.trackNumber = (TextView)view.findViewById(R.id.track_number);
            viewHolder.title = (TextView)view.findViewById(R.id.song_title);
            viewHolder.details = (TextView)view.findViewById(R.id.details);
            viewHolder.contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
            viewHolder.songInfo = new FileDownloadHelper.SongInfo();

            view.setTag(viewHolder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String artist = cursor.getString(AlbumSongsListQuery.ARTIST);

            ViewHolder vh = (ViewHolder) view.getTag();

            vh.title.setText(cursor.getString(AlbumSongsListQuery.TITLE));

            vh.songInfo.artist = artist;
            vh.songInfo.album = albumTitle;
            vh.songInfo.songId = cursor.getInt(SongsListQuery.SONGID);
            vh.songInfo.title = cursor.getString(SongsListQuery.TITLE);
            vh.songInfo.fileName = cursor.getString(SongsListQuery.FILE);
            vh.songInfo.track = cursor.getInt(SongsListQuery.TRACK);

            vh.trackNumber.setText(String.valueOf(vh.songInfo.track));

            String duration = UIUtils.formatTime(cursor.getInt(AlbumSongsListQuery.DURATION));
            String detailsText = TextUtils.isEmpty(artist) ? duration : duration + "  |  " + artist;
            vh.details.setText(detailsText);

            vh.contextMenu.setTag(vh);
            vh.contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    showPopupMenu(v);
                }
            });
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        ImageView art;
        TextView title;
        TextView details;
        TextView artist;
        TextView trackNumber;
        ImageView contextMenu;

        FileDownloadHelper.SongInfo songInfo;
    }

    private void showPopupMenu(View v) {
        final ViewHolder viewHolder = (ViewHolder) v.getTag();

        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.songid = viewHolder.songInfo.songId;

        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.song_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_play_song:
                        MediaPlayerUtils.play(SongsListFragment.this, playListItem);
                        return true;
                    case R.id.action_add_to_playlist:
                        MediaPlayerUtils.queue(SongsListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                        return true;
                    case R.id.download:
                        ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>();
                        songInfoList.add(viewHolder.songInfo);
                        UIUtils.downloadSongs(getActivity(),
                                              songInfoList,
                                              HostManager.getInstance(getActivity()).getHostInfo(),
                                              callbackHandler);
                }
                return false;
            }
        });
        popupMenu.show();
    }


    private void setDetails(TextView textView, String... elements) {
        if ((elements == null) || (elements.length < 1)) {
            return;
        }

        ArrayList<String> details = new ArrayList<>();
        for (int i = 0; i < elements.length; i++) {
            if (!TextUtils.isEmpty(elements[i]))
                details.add(elements[i]);
        }

        textView.setText(TextUtils.join(" | ", details.toArray()));
    }
}
