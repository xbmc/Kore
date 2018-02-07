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
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;

import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.ui.widgets.fabspeeddial.FABSpeedDial;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

public class ArtistInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(ArtistInfoFragment.class);

    // Loader IDs
    private static final int LOADER_ARTIST = 0,
            LOADER_SONGS = 1;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    @Override
    protected AbstractAdditionalInfoFragment getAdditionalInfoFragment() {
        return null;
    }

    @Override
    protected RefreshItem createRefreshItem() {
        RefreshItem refreshItem = new RefreshItem(getActivity(), LibrarySyncService.SYNC_ALL_MUSIC);
        refreshItem.setListener(new RefreshItem.RefreshItemListener() {
            @Override
            public void onSyncProcessEnded(MediaSyncEvent event) {
                if (event.status == MediaSyncEvent.STATUS_SUCCESS)
                    getLoaderManager().restartLoader(LOADER_ARTIST, null, ArtistInfoFragment.this);
            }
        });
        return refreshItem;
    }

    @Override
    protected boolean setupMediaActionBar() {
        setOnAddToPlaylistListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final PlaylistType.Item playListItem = new PlaylistType.Item();
                playListItem.artistid = getDataHolder().getId();
                MediaPlayerUtils.queue(ArtistInfoFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
            }
        });

        setOnDownloadListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLoaderManager().initLoader(LOADER_SONGS, null, ArtistInfoFragment.this);
            }
        });

        return true;
    }

    @Override
    protected boolean setupFAB(FABSpeedDial FAB) {
        FAB.setOnFabClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistType.Item item = new PlaylistType.Item();
                item.artistid = getDataHolder().getId();
                playItemOnKodi(item);
            }
        });
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExpandDescription(true);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_ARTIST, null, this);

        setHasOptionsMenu(false);
    }

    @Override
    public void onPause() {
        //Make sure loader is not reloaded for albums and songs when we return
        //These loaders should only be activated by the user pressing the download button
        getLoaderManager().destroyLoader(LOADER_SONGS);
        super.onPause();
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        switch (i) {
            case LOADER_ARTIST:
                uri = MediaContract.Artists.buildArtistUri(getHostInfo().getId(), getDataHolder().getId());
                return new CursorLoader(getActivity(), uri,
                                        DetailsQuery.PROJECTION, null, null, null);
            case LOADER_SONGS:
                uri = MediaContract.Songs.buildArtistSongsListUri(getHostInfo().getId(), getDataHolder().getId());
                return new CursorLoader(getActivity(), uri,
                                        SongsListQuery.PROJECTION, null, null, SongsListQuery.SORT);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_ARTIST:
                    cursor.moveToFirst();

                    FileDownloadHelper.SongInfo songInfo = new FileDownloadHelper.SongInfo(
                            cursor.getString(DetailsQuery.ARTIST),null, -1, -1, null, null);
                    setDownloadButtonState(songInfo.downloadDirectoryExists());

                    DataHolder dataHolder = getDataHolder();
                    dataHolder.setTitle(cursor.getString(DetailsQuery.ARTIST));
                    dataHolder.setUndertitle(cursor.getString(DetailsQuery.GENRE));
                    dataHolder.setDescription(cursor.getString(DetailsQuery.DESCRIPTION));
                    dataHolder.setPosterUrl(cursor.getString(DetailsQuery.THUMBNAIL));
                    dataHolder.setFanArtUrl(cursor.getString(DetailsQuery.FANART));
                    updateView(dataHolder);
                    break;
                case LOADER_SONGS:
                    final ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>(cursor.getCount());
                    if (cursor.moveToFirst()) {
                        do {
                            songInfoList.add(createSongInfo(cursor));
                        } while (cursor.moveToNext());
                    }

                    UIUtils.downloadSongs(getActivity(), songInfoList, getHostInfo(), callbackHandler);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Release loader's data
    }


    private FileDownloadHelper.SongInfo createSongInfo(Cursor cursor) {
        return new FileDownloadHelper.SongInfo(
                cursor.getString(SongsListQuery.DISPLAYARTIST),
                cursor.getString(SongsListQuery.ALBUMTITLE),
                cursor.getInt(SongsListQuery.SONGID),
                cursor.getInt(SongsListQuery.TRACK),
                cursor.getString(SongsListQuery.TITLE),
                cursor.getString(SongsListQuery.FILE));
    }

    private interface DetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Artists.ARTISTID,
                MediaContract.Artists.ARTIST,
                MediaContract.Artists.GENRE,
                MediaContract.Artists.THUMBNAIL,
                MediaContract.Artists.DESCRIPTION,
                MediaContract.Artists.FANART
        };

        int ID = 0;
        int ARTISTID = 1;
        int ARTIST = 2;
        int GENRE = 3;
        int THUMBNAIL = 4;
        int DESCRIPTION = 5;
        int FANART = 6;
    }

    /**
     * Song list query parameters.
     */
    private interface SongsListQuery {
        String[] PROJECTION = {
                MediaDatabase.Tables.SONGS + "." + BaseColumns._ID,
                MediaProvider.Qualified.SONGS_TITLE,
                MediaProvider.Qualified.SONGS_TRACK,
                MediaProvider.Qualified.SONGS_DURATION,
                MediaProvider.Qualified.SONGS_FILE,
                MediaProvider.Qualified.SONGS_SONGID,
                MediaProvider.Qualified.SONGS_ALBUMID,
                MediaProvider.Qualified.ALBUMS_TITLE,
                MediaProvider.Qualified.SONGS_DISPLAYARTIST
        };

        String SORT = MediaContract.Songs.TRACK + " ASC";

        int ID = 0;
        int TITLE = 1;
        int TRACK = 2;
        int DURATION = 3;
        int FILE = 4;
        int SONGID = 5;
        int ALBUMID = 6;
        int ALBUMTITLE = 7;
        int DISPLAYARTIST = 8;
    }
}
