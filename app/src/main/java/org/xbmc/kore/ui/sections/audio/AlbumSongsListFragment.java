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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

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

    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private ArrayList<FileDownloadHelper.SongInfo> songInfoList;

    /**
     * Use this to display all songs for a specific album
     * @param albumId Album id
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

        LoaderManager.getInstance(this).initLoader(LOADER, null, this);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        linearLayout.setLayoutParams(lp);
        return linearLayout;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        int hostId = hostInfo != null ? hostInfo.getId() : -1;

        uri = MediaContract.Songs.buildAlbumSongsListUri(hostId, albumId);

        return new CursorLoader(requireContext(), uri, AlbumSongsListQuery.PROJECTION, null,
                                null, AlbumSongsListQuery.SORT);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (! data.moveToFirst()) {
            Toast.makeText(getActivity(), R.string.no_songs_found_refresh,
                           Toast.LENGTH_SHORT).show();
            return;
        }
        displaySongs(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    /**
     * Returns all songs displayed by this fragment
     */
    public ArrayList<FileDownloadHelper.SongInfo> getSongInfoList() {
        return songInfoList;
    }

    private void displaySongs(Cursor cursor) {
        songInfoList = new ArrayList<>(cursor.getCount());
        LinearLayout listView = (LinearLayout) getView();
        if (listView == null) return;
        do {
            View songView = LayoutInflater.from(getActivity())
                                          .inflate(R.layout.item_music_song, listView, false);
            TextView songTitle = songView.findViewById(R.id.title);
            TextView trackNumber = songView.findViewById(R.id.track_number);
            TextView details = songView.findViewById(R.id.details);
            ImageView contextMenu = songView.findViewById(R.id.list_context_menu);

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

    View.OnClickListener songClickListener = v -> playSong(((FileDownloadHelper.SongInfo)v.getTag()).songId);

    private final ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

    private void playSong(int songId) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.songid = songId;
        Player.Open action = new Player.Open(item);
        action.execute(HostManager.getInstance(requireContext()).getConnection(),
                       defaultStringActionCallback, callbackHandler);
    }

    private final View.OnClickListener songItemMenuClickListener = v -> {
        final FileDownloadHelper.SongInfo songInfo = ((FileDownloadHelper.SongInfo)v.getTag());
        final int songId = songInfo.songId;

        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.song_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play_song) {
                playSong(songId);
                return true;
            } else if (itemId == R.id.action_add_to_playlist) {
                PlaylistType.Item playlistItem = new PlaylistType.Item();
                playlistItem.songid = songId;
                MediaPlayerUtils.queue(AlbumSongsListFragment.this, playlistItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                return true;
            } else if (itemId == R.id.download) {
                ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>();
                songInfoList.add(songInfo);
                UIUtils.downloadSongs(getActivity(), songInfoList,
                                      HostManager.getInstance(requireContext()).getHostInfo(), callbackHandler);
                return true;
            }
            return false;
        });
        popupMenu.show();
    };

    @Override
    public void refresh() {
        LoaderManager.getInstance(this).restartLoader(LOADER, null, this);
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
                                      .inflate(R.layout.item_music_song, viewGroup, false);
            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.trackNumber = view.findViewById(R.id.track_number);
            viewHolder.title = view.findViewById(R.id.title);
            viewHolder.details = view.findViewById(R.id.details);
            viewHolder.contextMenu = view.findViewById(R.id.list_context_menu);
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
            vh.contextMenu.setOnClickListener(AlbumSongsListFragment.this::showPopupMenu);
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
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play_song) {
                MediaPlayerUtils.play(AlbumSongsListFragment.this, playListItem);
                return true;
            } else if (itemId == R.id.action_add_to_playlist) {
                MediaPlayerUtils.queue(AlbumSongsListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                return true;
            } else if (itemId == R.id.download) {
                ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>();
                songInfoList.add(viewHolder.songInfo);
                UIUtils.downloadSongs(getActivity(),
                                      songInfoList,
                                      HostManager.getInstance(requireContext()).getHostInfo(),
                                      callbackHandler);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
