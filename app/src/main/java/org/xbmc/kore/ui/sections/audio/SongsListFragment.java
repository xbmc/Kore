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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.loader.content.CursorLoader;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
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
    private static String albumTitle = "";


    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    /**
     * Use this to display all songs for a specific artist
     * @param artistId Artist id
     */
    public void setArtist(int artistId) {
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_ARTISTID, artistId);
        setArguments(args);
    }

    /**
     * Use this to display all songs for a specific album
     * @param albumId Album
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
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        if (albumId != -1 ) {
            return new AlbumSongsAdapter(getContext(), this::showPopupMenu);
        } else {
            return new SongsAdapter(getContext(), this::showPopupMenu);
        }
    }

    @Override
    protected void onListItemClicked(View view) {
        ImageView contextMenu = view.findViewById(R.id.list_context_menu);
        showPopupMenu(contextMenu);
    }

    @Override
    protected CursorLoader createCursorLoader() {
        Uri uri;
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        int hostId = hostInfo != null ? hostInfo.getId() : -1;

        if (artistId != -1) { // get songs for artist
            uri = MediaContract.Songs.buildArtistSongsListUri(hostId, artistId);
        } else if (albumId != -1) {
            uri = MediaContract.Songs.buildAlbumSongsListUri(hostId, albumId);
        } else { // get all songs
            uri = MediaContract.Songs.buildSongsListUri(hostId);
        }

        String selection = null;
        String[] selectionArgs = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        if (albumId != -1) {
            return new CursorLoader(requireContext(), uri,
                                    AlbumSongsListQuery.PROJECTION, selection, selectionArgs, AlbumSongsListQuery.SORT);
        } else {
            return new CursorLoader(requireContext(), uri,
                                    SongsListQuery.PROJECTION, selection, selectionArgs, SongsListQuery.SORT);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        setSupportsSearch(true);
        super.onAttach(context);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            // HACK: Fix crash reported on Play Store. Why does this is necessary is beyond me
            super.onCreateOptionsMenu(menu, inflater);
            return;
        }

        inflater.inflate(R.menu.media_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
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

    private static class SongsAdapter extends RecyclerViewCursorAdapter {

        private final HostManager hostManager;
        private final int artWidth, artHeight;
        private final Context context;
        private final View.OnClickListener contextMenuClickListener;

        public SongsAdapter(Context context, View.OnClickListener contextMenuClickListener) {
            this.hostManager = HostManager.getInstance(context);
            this.context = context;
            this.contextMenuClickListener = contextMenuClickListener;
            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_width_square);
            artHeight = resources.getDimensionPixelOffset(R.dimen.info_poster_height_square);
        }

        @NonNull
        @Override
        public CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                                      .inflate(R.layout.item_music_generic, parent, false);

            return new SongViewHolder(view, context, hostManager, artWidth, artHeight,
                                      contextMenuClickListener);
        }

        protected int getSectionColumnIdx() { return SongsListQuery.TITLE; }
    }

    private static class AlbumSongsAdapter extends RecyclerViewCursorAdapter {
        private final Context context;
        private final View.OnClickListener contextMenuClickListener;

        public AlbumSongsAdapter(Context context, View.OnClickListener contextMenuClickListener) {
            this.context = context;
            this.contextMenuClickListener = contextMenuClickListener;
        }

        @NonNull
        @Override
        public CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                                      .inflate(R.layout.item_music_song, parent, false);

            return new AlbumViewHolder(view, contextMenuClickListener);
        }

        protected int getSectionColumnIdx() { return AlbumSongsListQuery.TITLE; }
    }

    /**
     * View holder pattern
     */
    abstract static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        ImageView art;
        TextView titleView, detailsView, artistView;
        ImageView contextMenu;

        FileDownloadHelper.SongInfo songInfo;

        ViewHolder(View itemView, View.OnClickListener contextMenuClickListener) {
            super(itemView);
            art = itemView.findViewById(R.id.art);
            titleView = itemView.findViewById(R.id.title);
            artistView = itemView.findViewById(R.id.details);
            detailsView = itemView.findViewById(R.id.other_info);
            contextMenu = itemView.findViewById(R.id.list_context_menu);
            songInfo = new FileDownloadHelper.SongInfo();

            contextMenu.setTag(this);
            contextMenu.setOnClickListener(contextMenuClickListener);
        }

        @Override
        public void bindView(Cursor cursor) {
            songInfo.songId = cursor.getInt(SongsListQuery.SONGID);
            songInfo.title = cursor.getString(SongsListQuery.TITLE);
            songInfo.fileName = cursor.getString(SongsListQuery.FILE);
            songInfo.track = cursor.getInt(SongsListQuery.TRACK);
        }
    }

    public static class SongViewHolder extends ViewHolder {
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        SongViewHolder(View itemView, Context context, HostManager hostManager,
                       int artWidth, int artHeight, View.OnClickListener contextMenuClickListener) {
            super(itemView, contextMenuClickListener);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
        }

        static String DELIMITER = "  |  ";

        @Override
        public void bindView(Cursor cursor) {
            super.bindView(cursor);

            String title = cursor.getString(SongsListQuery.TITLE);
            titleView.setText(title);
            String artistAlbum = cursor.getString(SongsListQuery.SONGDISPLAYARTIST) + DELIMITER +
                                 cursor.getString(SongsListQuery.ALBUMTITLE);
            artistView.setText(artistAlbum);
//            int year = cursor.getInt(SongsListQuery.YEAR);
            int duration = cursor.getInt(SongsListQuery.DURATION);
            String genres = cursor.getString(SongsListQuery.GENRE);

            ArrayList<String> details = new ArrayList<>();
            if (duration > 0) details.add(UIUtils.formatTime(duration));
//            if (year > 0) details.add(String.valueOf(year));
            if (genres != null) details.add(genres);
            detailsView.setText(TextUtils.join(DELIMITER, details));

            String thumbnail = cursor.getString(SongsListQuery.THUMBNAIL);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 thumbnail, title,
                                                 art, artWidth, artHeight);
        }
    }

    public static class AlbumViewHolder extends ViewHolder {
        TextView trackNumberTextView;

        AlbumViewHolder(View itemView, View.OnClickListener contextMenuClickListener) {
            super(itemView, contextMenuClickListener);
            trackNumberTextView = itemView.findViewById(R.id.track_number);
        }

        @Override
        public void bindView(Cursor cursor) {
            super.bindView(cursor);

            trackNumberTextView.setText(String.valueOf(songInfo.track));

            String artist = cursor.getString(AlbumSongsListQuery.ARTIST);
            titleView.setText(cursor.getString(AlbumSongsListQuery.TITLE));

            songInfo.artist = artist;
            songInfo.album = albumTitle;

            String duration = UIUtils.formatTime(cursor.getInt(AlbumSongsListQuery.DURATION));
            String detailsText = TextUtils.isEmpty(artist) ? duration : duration + "  |  " + artist;
            detailsView.setText(detailsText);
        }
    }

    private void showPopupMenu(View v) {
        final ViewHolder viewHolder = (ViewHolder) v.getTag();

        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.songid = viewHolder.songInfo.songId;

        final PopupMenu popupMenu = new PopupMenu(requireContext(), v);
        popupMenu.getMenuInflater().inflate(R.menu.song_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play_song) {
                MediaPlayerUtils.play(SongsListFragment.this, playListItem);
                return true;
            } else if (itemId == R.id.action_add_to_playlist) {
                MediaPlayerUtils.queue(SongsListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                return true;
            } else if (itemId == R.id.download) {
                ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>();
                songInfoList.add(viewHolder.songInfo);
                UIUtils.downloadSongs(requireContext(),
                                      songInfoList,
                                      HostManager.getInstance(requireContext()).getHostInfo(),
                                      callbackHandler);
            }
            return false;
        });
        popupMenu.show();
    }
}
