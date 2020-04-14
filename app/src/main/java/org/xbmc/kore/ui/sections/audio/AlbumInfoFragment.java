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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.ui.widgets.fabspeeddial.FABSpeedDial;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

/**
 * Presents album details
 */
public class AlbumInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(AlbumInfoFragment.class);

    private static final int LOADER_ALBUM = 0;

    private Handler callbackHandler = new Handler();
    private AlbumSongsListFragment albumSongsListFragment;

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_ALBUM, null, this);

        setHasOptionsMenu(false);


    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        int albumId = getDataHolder().getId();
        int id = HostManager.getInstance(getActivity()).getHostInfo().getId();
        switch (i) {
            case LOADER_ALBUM:
                uri = MediaContract.Albums.buildAlbumUri(id, albumId);
                return new CursorLoader(getActivity(), uri,
                                        AlbumDetailsQuery.PROJECTION, null, null, null);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_ALBUM:
                    cursor.moveToFirst();

                    DataHolder dataHolder = getDataHolder();

                    dataHolder.setRating(cursor.getDouble(AlbumDetailsQuery.RATING));
                    dataHolder.setTitle(cursor.getString(AlbumDetailsQuery.TITLE));
                    dataHolder.setUndertitle(cursor.getString(AlbumDetailsQuery.DISPLAYARTIST));
                    dataHolder.setDescription(cursor.getString(AlbumDetailsQuery.DESCRIPTION));
                    dataHolder.setFanArtUrl(cursor.getString(AlbumInfoFragment.AlbumDetailsQuery.FANART));
                    dataHolder.setPosterUrl(cursor.getString(AlbumInfoFragment.AlbumDetailsQuery.THUMBNAIL));

                    int year = cursor.getInt(AlbumDetailsQuery.YEAR);
                    String genres = cursor.getString(AlbumDetailsQuery.GENRE);
                    dataHolder.setDetails ( (year > 0) ?
                                            (!TextUtils.isEmpty(genres) ?
                                             genres + "  |  " + String.valueOf(year) :
                                             String.valueOf(year)) :
                                            genres
                                          );

                    FileDownloadHelper.SongInfo songInfo = new FileDownloadHelper.SongInfo
                            (dataHolder.getUnderTitle(), dataHolder.getTitle(), 0, 0, null, null);
                    setDownloadButtonState(songInfo.downloadDirectoryExists());

                    updateView(dataHolder);
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        // Release loader's data
    }

    @Override
    protected AbstractAdditionalInfoFragment getAdditionalInfoFragment() {
        DataHolder dataHolder = getDataHolder();
        albumSongsListFragment = new AlbumSongsListFragment();
        albumSongsListFragment.setAlbum(dataHolder.getId(), dataHolder.getTitle());
        return albumSongsListFragment;
    }

    @Override
    protected RefreshItem createRefreshItem() {
        RefreshItem refreshItem = new RefreshItem(getActivity(), LibrarySyncService.SYNC_ALL_MUSIC);
        refreshItem.setListener((MediaSyncEvent event) -> {

            if (event.status == MediaSyncEvent.STATUS_SUCCESS)
                getLoaderManager().restartLoader(LOADER_ALBUM, null, AlbumInfoFragment.this);

        });
        return refreshItem;
    }

    @Override
    protected boolean setupMediaActionBar() {
        setOnDownloadListener((View view) -> {
            UIUtils.downloadSongs(getActivity(), albumSongsListFragment.getSongInfoList(),
                                  getHostInfo(), callbackHandler);

        });

        setOnAddToPlaylistListener((View view) -> addToPlaylist());

        return true;
    }

    @Override
    protected boolean setupFAB(FABSpeedDial FAB) {
        FAB.setOnFabClickListener((View v) -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.albumid = getDataHolder().getId();
            playItemOnKodi(item);
        });
        return true;
    }

    private void addToPlaylist() {
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
                    item.albumid = getDataHolder().getId();
                    Playlist.Add action = new Playlist.Add(audioPlaylistId, item);
                    action.execute(HostManager.getInstance(getActivity()).getConnection(), new ApiCallback<String>() {
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

    /**
     * Album details query parameters.
     */
    public interface AlbumDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Albums.TITLE,
                MediaContract.Albums.DISPLAYARTIST,
                MediaContract.Albums.THUMBNAIL,
                MediaContract.Albums.FANART,
                MediaContract.Albums.YEAR,
                MediaContract.Albums.GENRE,
                MediaContract.Albums.DESCRIPTION,
                MediaContract.Albums.RATING,
                };

        int ID = 0;
        int TITLE = 1;
        int DISPLAYARTIST = 2;
        int THUMBNAIL = 3;
        int FANART = 4;
        int YEAR = 5;
        int GENRE = 6;
        int DESCRIPTION = 7;
        int RATING = 8;
    }
}
