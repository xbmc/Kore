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

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Presents music videos details
 */
public class MusicVideoInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(MusicVideoInfoFragment.class);

    // Loader IDs
    private static final int LOADER_MUSIC_VIDEO = 0;

    //    /**
//     * Handler on which to post RPC callbacks
//     */
    private Handler callbackHandler = new Handler();

    private Cursor cursor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExpandDescription(true);
    }

    @Override
    protected RefreshItem createRefreshItem() {
        RefreshItem refreshItem = new RefreshItem(getActivity(),
                                                  LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS);
        refreshItem.setListener(new RefreshItem.RefreshItemListener() {
            @Override
            public void onSyncProcessEnded(MediaSyncEvent event) {
                if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                    getLoaderManager().restartLoader(LOADER_MUSIC_VIDEO, null,
                                                     MusicVideoInfoFragment.this);
                }
            }
        });

        return refreshItem;
    }

    @Override
    protected boolean setupMediaActionBar() {
        setOnAddToPlaylistListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToPlaylist(getDataHolder().getId());
            }
        });

        setOnDownloadListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download();
            }
        });

        return true;
    }

    @Override
    protected boolean setupFAB(ImageButton FAB) {
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistType.Item item = new PlaylistType.Item();
                item.musicvideoid = getDataHolder().getId();
                fabActionPlayItem(item);
            }
        });
        return true;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Start the loaders
        getLoaderManager().initLoader(LOADER_MUSIC_VIDEO, null, this);
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        switch (i) {
            case LOADER_MUSIC_VIDEO:
                uri = MediaContract.MusicVideos.buildMusicVideoUri(getHostInfo().getId(),
                                                                   getDataHolder().getId());
                return new CursorLoader(getActivity(), uri,
                                        MusicVideoDetailsQuery.PROJECTION, null, null, null);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_MUSIC_VIDEO:
                    this.cursor = cursor;
                    cursor.moveToFirst();
                    DataHolder dataHolder = getDataHolder();

                    dataHolder.setFanArtUrl(cursor.getString(MusicVideoDetailsQuery.FANART));
                    dataHolder.setPosterUrl(cursor.getString(MusicVideoDetailsQuery.THUMBNAIL));

                    int runtime = cursor.getInt(MusicVideoDetailsQuery.RUNTIME);
                    int year = cursor.getInt(MusicVideoDetailsQuery.YEAR);
                    String details = runtime > 0 ?
                                     UIUtils.formatTime(runtime) + " | " +
                                     String.valueOf(year) :
                                     String.valueOf(year);
                    dataHolder.setDetails(details + "\n" + cursor.getString(MusicVideoDetailsQuery.GENRES));

                    dataHolder.setTitle(cursor.getString(MusicVideoDetailsQuery.TITLE));
                    dataHolder.setUndertitle(cursor.getString(MusicVideoDetailsQuery.ARTIST)
                                             + " | " +
                                             cursor.getString(MusicVideoDetailsQuery.ALBUM));
                    dataHolder.setDescription(cursor.getString(MusicVideoDetailsQuery.PLOT));

                    FileDownloadHelper.MusicVideoInfo musicVideoDownloadInfo = new FileDownloadHelper.MusicVideoInfo(
                            dataHolder.getTitle(), cursor.getString(MusicVideoDetailsQuery.FILE));
                    setDownloadButtonState(musicVideoDownloadInfo.downloadFileExists());

                    updateView(dataHolder);
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Release loader's data
    }

    public void addToPlaylist(final int itemId) {
        Playlist.GetPlaylists getPlaylists = new Playlist.GetPlaylists();

        getPlaylists.execute(getHostManager().getConnection(), new ApiCallback<ArrayList<PlaylistType.GetPlaylistsReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlaylistType.GetPlaylistsReturnType> result) {
                if (!isAdded()) return;
                // Ok, loop through the playlists, looking for the video one
                int videoPlaylistId = -1;
                for (PlaylistType.GetPlaylistsReturnType playlist : result) {
                    if (playlist.type.equals(PlaylistType.GetPlaylistsReturnType.VIDEO)) {
                        videoPlaylistId = playlist.playlistid;
                        break;
                    }
                }
                // If found, add to playlist
                if (videoPlaylistId != -1) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.musicvideoid = itemId;
                    Playlist.Add action = new Playlist.Add(videoPlaylistId, item);
                    action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
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

    protected void download() {
        final FileDownloadHelper.MusicVideoInfo musicVideoDownloadInfo = new FileDownloadHelper.MusicVideoInfo(
                getDataHolder().getTitle(), cursor.getString(MusicVideoDetailsQuery.FILE));

        // Check if the directory exists and whether to overwrite it
        File file = new File(musicVideoDownloadInfo.getAbsoluteFilePath());
        if (file.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                   .setMessage(R.string.download_file_exists)
                   .setPositiveButton(R.string.overwrite,
                                      new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                              FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                                               musicVideoDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                                                               callbackHandler);
                                          }
                                      })
                   .setNeutralButton(R.string.download_with_new_name,
                                     new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                             FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                                              musicVideoDownloadInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                                              callbackHandler);
                                         }
                                     })
                   .setNegativeButton(android.R.string.cancel,
                                      new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                              // Nothing to do
                                          }
                                      })
                   .show();
        } else {
            FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                             musicVideoDownloadInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                             callbackHandler);
        }
    }

    @Override
    protected AbstractAdditionalInfoFragment getAdditionalInfoFragment() {
        return null;
    }

    /**
     * Video details query parameters.
     */
    private interface MusicVideoDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.MusicVideos.TITLE,
                MediaContract.MusicVideos.ALBUM,
                MediaContract.MusicVideos.ARTIST,
                MediaContract.MusicVideos.THUMBNAIL,
                MediaContract.MusicVideos.FANART,
                MediaContract.MusicVideos.YEAR,
                MediaContract.MusicVideos.GENRES,
                MediaContract.MusicVideos.RUNTIME,
                MediaContract.MusicVideos.PLOT,
                MediaContract.MusicVideos.FILE,
                };

        int ID = 0;
        int TITLE = 1;
        int ALBUM = 2;
        int ARTIST = 3;
        int THUMBNAIL =4;
        int FANART = 5;
        int YEAR = 6;
        int GENRES = 7;
        int RUNTIME = 8;
        int PLOT = 9;
        int FILE = 10;
    }
}
