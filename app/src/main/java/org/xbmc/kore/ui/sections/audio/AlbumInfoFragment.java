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
import android.os.Looper;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Presents album details
 */
public class AlbumInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(AlbumInfoFragment.class);

    private static final int LOADER_ALBUM = 0;

    private final Handler callbackHandler = new Handler(Looper.getMainLooper());
    private AlbumSongsListFragment albumSongsListFragment;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoaderManager.getInstance(this).initLoader(LOADER_ALBUM, null, this);
        setHasOptionsMenu(false);
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
        // Don't start refresh on details screen
        return null;
//        RefreshItem refreshItem = new RefreshItem(requireContext(), LibrarySyncService.SYNC_ALL_MUSIC);
//        refreshItem.setListener(event -> {
//            if (event.status == MediaSyncEvent.STATUS_SUCCESS)
//                LoaderManager.getInstance(this).restartLoader(LOADER_ALBUM, null, AlbumInfoFragment.this);
//        });
//        return refreshItem;
    }

    @Override
    protected boolean setupInfoActionsBar() {
        setOnDownloadClickListener(view -> UIUtils.downloadSongs(requireContext(),
                                                                 albumSongsListFragment.getSongInfoList(),
                                                                 getHostInfo(),
                                                                 callbackHandler));

        setOnQueueClickListener(view -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.albumid = getDataHolder().getId();
            MediaPlayerUtils.queue(AlbumInfoFragment.this, item, PlaylistType.GetPlaylistsReturnType.AUDIO);
        });

        return true;
    }

    @Override
    protected boolean setupFAB(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.albumid = getDataHolder().getId();
            playItemOnKodi(item);
        });
        return true;
    }

    /*
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        int albumId = getDataHolder().getId();
        int id = HostManager.getInstance(requireContext()).getHostInfo().getId();
        switch (i) {
            case LOADER_ALBUM:
            default:
                uri = MediaContract.Albums.buildAlbumUri(id, albumId);
                return new CursorLoader(requireContext(), uri,
                                        AlbumDetailsQuery.PROJECTION, null, null, null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_ALBUM:
                    cursor.moveToFirst();

                    String artist = cursor.getString(AlbumDetailsQuery.DISPLAYARTIST),
                            albumTitle = cursor.getString(AlbumDetailsQuery.TITLE);

                    DataHolder dataHolder = getDataHolder();

                    dataHolder.setRating(cursor.getDouble(AlbumDetailsQuery.RATING));
                    dataHolder.setTitle(albumTitle);
                    dataHolder.setUndertitle(artist);
                    dataHolder.setDescription(cursor.getString(AlbumDetailsQuery.DESCRIPTION));
                    dataHolder.setFanArtUrl(cursor.getString(AlbumInfoFragment.AlbumDetailsQuery.FANART));
                    dataHolder.setPosterUrl(cursor.getString(AlbumInfoFragment.AlbumDetailsQuery.THUMBNAIL));
                    dataHolder.setSearchTerms(artist + " " + albumTitle);

                    int year = cursor.getInt(AlbumDetailsQuery.YEAR);
                    String genres = cursor.getString(AlbumDetailsQuery.GENRE);
                    dataHolder.setDetails((year > 0) ?
                                          (!TextUtils.isEmpty(genres) ?
                                           genres + "  |  " + year :
                                           String.valueOf(year)) :
                                          genres);

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
