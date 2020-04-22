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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Fragment that presents the songs list
 */
public class AlbumSongsListFragment extends AbstractAdditionalInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(AlbumSongsListFragment.class);

    public static final String BUNDLE_KEY_ALBUMID = "albumid";
    public static final String BUNDLE_KEY_ALBUMTITLE = "albumtitle";

    private static final int LOADER = 0;

    private int albumId = -1;
    private String albumTitle = "";

    private Handler callbackHandler = new Handler();

    private ArrayList<FileDownloadHelper.SongInfo> songInfoList;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            albumId = arguments.getInt(BUNDLE_KEY_ALBUMID, -1);
            albumTitle = arguments.getString(BUNDLE_KEY_ALBUMTITLE, "");
        }

        getLoaderManager().initLoader(LOADER, null, this);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        linearLayout.setLayoutParams(lp);
        return linearLayout;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        int hostId = hostInfo != null ? hostInfo.getId() : -1;

        uri = MediaContract.Songs.buildAlbumSongsListUri(hostId, albumId);

        return new CursorLoader(getActivity(), uri, AlbumSongsListQuery.PROJECTION, null,
                                null, AlbumSongsListQuery.SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (! data.moveToFirst()) {
            Toast.makeText(getActivity(), R.string.no_songs_found_refresh,
                           Toast.LENGTH_SHORT).show();
            return;
        }
        displaySongs(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Returns all songs displayed by this fragment
     * @return
     */
    public ArrayList<FileDownloadHelper.SongInfo> getSongInfoList() {
        return songInfoList;
    }

    private void displaySongs(Cursor cursor) {
        songInfoList = new ArrayList<>(cursor.getCount());
        LinearLayout listView = (LinearLayout) getView();
        do {
            View songView = LayoutInflater.from(getActivity())
                                          .inflate(R.layout.list_item_song, listView, false);
            TextView songTitle = (TextView)songView.findViewById(R.id.song_title);
            TextView trackNumber = (TextView)songView.findViewById(R.id.track_number);
            TextView details = (TextView)songView.findViewById(R.id.details);
            ImageView contextMenu = (ImageView)songView.findViewById(R.id.list_context_menu);

            String artist = cursor.getString(AlbumSongsListQuery.ARTIST);

            // Add this song to the list
            FileDownloadHelper.SongInfo songInfo = new FileDownloadHelper.SongInfo(
                    artist,
                    albumTitle,
                    cursor.getInt(AlbumSongsListQuery.SONGID),
                    cursor.getInt(AlbumSongsListQuery.TRACK),
                    cursor.getString(AlbumSongsListQuery.TITLE),
                    cursor.getString(AlbumSongsListQuery.FILE));
            songInfoList.add(songInfo);

            songTitle.setText(songInfo.title);


            trackNumber.setText(String.valueOf(songInfo.track));

            String duration = UIUtils.formatTime(cursor.getInt(AlbumSongsListQuery.DURATION));
            String detailsText = TextUtils.isEmpty(artist) ? duration : duration + "  |  " + artist;
            details.setText(detailsText);

            contextMenu.setTag(songInfo);
            contextMenu.setOnClickListener(songItemMenuClickListener);

            songView.setTag(songInfo);
            songView.setOnClickListener(songClickListener);
            listView.addView(songView);
        } while (cursor.moveToNext());
    }

    View.OnClickListener songClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playSong(((FileDownloadHelper.SongInfo)v.getTag()).songId);
        }
    };

    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

    private void playSong(int songId) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.songid = songId;
        Player.Open action = new Player.Open(item);
        action.execute(HostManager.getInstance(getActivity()).getConnection(),
                       defaultStringActionCallback, callbackHandler);
    }

    private View.OnClickListener songItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final FileDownloadHelper.SongInfo songInfo = ((FileDownloadHelper.SongInfo)v.getTag());
            final int songId = songInfo.songId;

            final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.song_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_play_song:
                            playSong(songId);
                            return true;
                        case R.id.action_add_to_playlist:
                            addToPlaylist(songId);
                            return true;
                        case R.id.download:
                            ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>();
                            songInfoList.add(songInfo);
                            UIUtils.downloadSongs(getActivity(), songInfoList,
                                                  HostManager.getInstance(getActivity()).getHostInfo(), callbackHandler);
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };

    private void addToPlaylist(final int id) {
        Playlist.GetPlaylists getPlaylists = new Playlist.GetPlaylists();

        getPlaylists.execute(HostManager.getInstance(getActivity()).getConnection(), new ApiCallback<ArrayList<PlaylistType.GetPlaylistsReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlaylistType.GetPlaylistsReturnType> result) {
                if (!isAdded()) return;
                // Ok, loop through the playlists, looking for the audio one
                int audioPlaylistId = -1;
                for (PlaylistType.GetPlaylistsReturnType playlist : result) {
                    if (playlist.type.equals(PlaylistType.GetPlaylistsReturnType.AUDIO)) {
                        audioPlaylistId = playlist.playlistid;
                        break;
                    }
                }
                // If found, add to playlist
                if (audioPlaylistId != -1) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.songid = id;
                    Playlist.Add action = new Playlist.Add(audioPlaylistId, item);
                    action.execute(HostManager.getInstance(getActivity()).getConnection(),
                                   new ApiCallback<String>() {
                                       @Override
                                       public void onSuccess(String result) {
                                           if (!isAdded()) return;
                                           // Got an error, show toast
                                           Toast.makeText(getActivity(), R.string.item_added_to_playlist, Toast.LENGTH_SHORT)
                                                .show();
                                       }

                                       @Override
                                       public void onError(int errorCode, String description) {
                                           if (!isAdded()) return;
                                           // Got an error, show toast
                                           Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                                                .show();
                                       }
                                   }, callbackHandler);
                } else {
                    Toast.makeText(getActivity(), R.string.no_suitable_playlist, Toast.LENGTH_SHORT)
                         .show();
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(LOADER, null, this);
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
            vh.songInfo.songId = cursor.getInt(AlbumSongsListQuery.SONGID);
            vh.songInfo.title = cursor.getString(AlbumSongsListQuery.TITLE);
            vh.songInfo.fileName = cursor.getString(AlbumSongsListQuery.FILE);
            vh.songInfo.track = cursor.getInt(AlbumSongsListQuery.TRACK);

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
        TextView title;
        TextView details;
        TextView trackNumber;
        ImageView contextMenu;

        FileDownloadHelper.SongInfo songInfo;
    }

    private void showPopupMenu(View v) {
        final ViewHolder viewHolder = (ViewHolder) v.getTag();

        final PlaylistType.Item playListItem = new PlaylistType.Item();

        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.song_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_play_song:
                        MediaPlayerUtils.play(AlbumSongsListFragment.this, playListItem);
                        return true;
                    case R.id.action_add_to_playlist:
                        MediaPlayerUtils.queue(AlbumSongsListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
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
}
