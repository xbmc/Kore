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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.io.File;

/**
 * Presents music videos details
 */
public class MusicVideoInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(MusicVideoInfoFragment.class);

    // Loader IDs
    private static final int LOADER_MUSIC_VIDEO = 0;

    // Handler on which to post RPC callbacks
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private String musicVideoFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExpandDescription(true);
    }

    @Override
    protected RefreshItem createRefreshItem() {
        RefreshItem refreshItem = new RefreshItem(getActivity(),
                                                  LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS);
        refreshItem.setListener(event -> {
            if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                LoaderManager.getInstance(this).restartLoader(LOADER_MUSIC_VIDEO, null, MusicVideoInfoFragment.this);
            }
        });

        return refreshItem;
    }

    @Override
    protected boolean setupInfoActionsBar() {
        setOnQueueClickListener(view -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.musicvideoid = getDataHolder().getId();
            MediaPlayerUtils.queue(MusicVideoInfoFragment.this, item, PlaylistType.GetPlaylistsReturnType.VIDEO);
        });

        setOnDownloadClickListener(view -> download());

        return true;
    }

    @Override
    protected boolean setupFAB(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.musicvideoid = getDataHolder().getId();
            playItemOnKodi(item);
        });
        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Start the loaders
        LoaderManager.getInstance(this).initLoader(LOADER_MUSIC_VIDEO, null, this);
    }

    /*
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        switch (i) {
            case LOADER_MUSIC_VIDEO:
            default:
                uri = MediaContract.MusicVideos.buildMusicVideoUri(getHostInfo().getId(),
                                                                   getDataHolder().getId());
                return new CursorLoader(requireContext(), uri,
                                        MusicVideoDetailsQuery.PROJECTION, null, null, null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_MUSIC_VIDEO:
                    cursor.moveToFirst();
                    musicVideoFile = cursor.getString(MusicVideoDetailsQuery.FILE);
                    DataHolder dataHolder = getDataHolder();

                    dataHolder.setFanArtUrl(cursor.getString(MusicVideoDetailsQuery.FANART));
                    dataHolder.setPosterUrl(cursor.getString(MusicVideoDetailsQuery.THUMBNAIL));

                    int runtime = cursor.getInt(MusicVideoDetailsQuery.RUNTIME);
                    int year = cursor.getInt(MusicVideoDetailsQuery.YEAR);
                    String details = runtime > 0 ?
                                     UIUtils.formatTime(runtime) + " | " + year :
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
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        // Release loader's data
    }

    protected void download() {
        final FileDownloadHelper.MusicVideoInfo musicVideoDownloadInfo = new FileDownloadHelper.MusicVideoInfo(
                getDataHolder().getTitle(), musicVideoFile);

        // Check if the directory exists and whether to overwrite it
        File file = new File(musicVideoDownloadInfo.getAbsoluteFilePath());
        if (file.exists()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.download)
                   .setMessage(R.string.download_file_exists)
                   .setPositiveButton(R.string.overwrite,
                                      (dialog, which) ->
                                              FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                                               musicVideoDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                                                               callbackHandler))
                   .setNeutralButton(R.string.download_with_new_name,
                                     (dialog, which) ->
                                             FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                                              musicVideoDownloadInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                                              callbackHandler))
                   .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
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
