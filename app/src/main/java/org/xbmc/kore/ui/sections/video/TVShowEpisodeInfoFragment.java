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
package org.xbmc.kore.ui.sections.video;

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.ui.widgets.fabspeeddial.FABSpeedDial;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

import java.io.File;

/**
 * Presents movie details
 */
public class TVShowEpisodeInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(TVShowEpisodeInfoFragment.class);

    public static final String BUNDLE_KEY_TVSHOWID = "tvshow_id";

    // Loader IDs
    private static final int LOADER_EPISODE = 0;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    // Displayed episode
    private int tvshowId = -1;

    private Cursor cursor;

    FileDownloadHelper.TVShowInfo fileDownloadHelper;

    public void setTvshowId(int tvshowId) {
        getDataHolder().getBundle().putInt(BUNDLE_KEY_TVSHOWID, tvshowId);
    }

    @Override
    protected RefreshItem createRefreshItem() {
        RefreshItem refreshItem = new RefreshItem(getActivity(),
                                                  LibrarySyncService.SYNC_SINGLE_TVSHOW);
        refreshItem.setSyncItem(LibrarySyncService.SYNC_TVSHOWID, tvshowId);
        refreshItem.setListener(event -> {
            if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                getLoaderManager().restartLoader(LOADER_EPISODE, null,
                                                 TVShowEpisodeInfoFragment.this);
            }
        });
        return refreshItem;
    }

    @Override
    protected boolean setupMediaActionBar() {
        setOnDownloadListener(view -> downloadEpisode());

        setOnAddToPlaylistListener(view -> Utils.addToPlaylist(TVShowEpisodeInfoFragment.this, getDataHolder().getId(),
                            PlaylistType.GetPlaylistsReturnType.VIDEO));

        setOnSeenListener(view -> {
            int playcount = cursor.getInt(EpisodeDetailsQuery.PLAYCOUNT);
            int newPlaycount = (playcount > 0) ? 0 : 1;

            VideoLibrary.SetEpisodeDetails action =
                    new VideoLibrary.SetEpisodeDetails(getDataHolder().getId(), newPlaycount, null);
            action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Force a refresh, but don't show a message
                    if (!isAdded()) return;
                    getRefreshItem().startSync(true);
                }

                @Override
                public void onError(int errorCode, String description) { }
            }, callbackHandler);
        });

        return true;
    }

    @Override
    protected boolean setupFAB(FABSpeedDial FAB) {
        FAB.setOnDialItemClickListener(new FABSpeedDial.DialListener() {
            @Override
            public void onLocalPlayClicked() {
                playItemLocally(fileDownloadHelper.getMediaUrl(getHostInfo()), "video/*");
            }

            @Override
            public void onRemotePlayClicked() {
                PlaylistType.Item item = new PlaylistType.Item();
                item.episodeid = getDataHolder().getId();
                playItemOnKodi(item);
            }
        });
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.tvshowId = getArguments().getInt(BUNDLE_KEY_TVSHOWID, -1);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_EPISODE, null, this);
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        switch (i) {
            case LOADER_EPISODE:
                uri = MediaContract.Episodes.buildTVShowEpisodeUri(getHostInfo().getId(), tvshowId,
                                                                   getDataHolder().getId());
                return new CursorLoader(getActivity(), uri,
                                        EpisodeDetailsQuery.PROJECTION, null, null, null);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_EPISODE:
                    cursor.moveToFirst();
                    this.cursor = cursor;

                    DataHolder dataHolder = getDataHolder();

                    dataHolder.setPosterUrl(cursor.getString(EpisodeDetailsQuery.THUMBNAIL));

                    dataHolder.setRating(cursor.getDouble(EpisodeDetailsQuery.RATING));
                    dataHolder.setMaxRating(10);

                    String director = cursor.getString(EpisodeDetailsQuery.DIRECTOR);
                    if (!TextUtils.isEmpty(director)) {
                        director = getActivity().getResources().getString(R.string.directors) + " " + director;
                    }
                    int runtime = cursor.getInt(EpisodeDetailsQuery.RUNTIME) / 60;
                    String durationPremiered = runtime > 0 ?
                                               String.format(getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                                               "  |  " + cursor.getString(EpisodeDetailsQuery.FIRSTAIRED) :
                                               cursor.getString(EpisodeDetailsQuery.FIRSTAIRED);
                    String season = String.format(getString(R.string.season_episode),
                                                  cursor.getInt(EpisodeDetailsQuery.SEASON),
                                                  cursor.getInt(EpisodeDetailsQuery.EPISODE));

                    dataHolder.setDetails(durationPremiered + "\n" + season + "\n" + director);

                    fileDownloadHelper = new FileDownloadHelper.TVShowInfo(
                            cursor.getString(EpisodeDetailsQuery.SHOWTITLE),
                            cursor.getInt(EpisodeDetailsQuery.SEASON),
                            cursor.getInt(EpisodeDetailsQuery.EPISODE),
                            cursor.getString(EpisodeDetailsQuery.TITLE),
                            cursor.getString(EpisodeDetailsQuery.FILE));

                    setDownloadButtonState(fileDownloadHelper.downloadFileExists());

                    setSeenButtonState(cursor.getInt(EpisodeDetailsQuery.PLAYCOUNT) > 0);

                    getDataHolder().setTitle(cursor.getString(EpisodeDetailsQuery.TITLE));
                    getDataHolder().setUndertitle(cursor.getString(EpisodeDetailsQuery.SHOWTITLE));
                    setExpandDescription(true);
                    getDataHolder().setDescription(cursor.getString(EpisodeDetailsQuery.PLOT));

                    updateView(dataHolder);
                    break;
            }
        }

        getFabButton().enableLocalPlay(fileDownloadHelper != null);
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        // Release loader's data
    }

    private void downloadEpisode() {
        DialogInterface.OnClickListener noopClickListener =
                (dialog, which) -> { };

        // Check if the directory exists and whether to overwrite it
        File file = new File(fileDownloadHelper.getAbsoluteFilePath());
        if (file.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                   .setMessage(R.string.download_file_exists)
                   .setPositiveButton(R.string.overwrite,
                           (dialog, which) -> FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                            fileDownloadHelper, FileDownloadHelper.OVERWRITE_FILES,
                                                            callbackHandler))
                   .setNeutralButton(R.string.download_with_new_name,
                           (dialog, which) -> FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                            fileDownloadHelper, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                            callbackHandler))
                   .setNegativeButton(android.R.string.cancel, noopClickListener)
                   .show();
        } else {
            // Confirm that the user really wants to download the file
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                   .setMessage(R.string.confirm_episode_download)
                   .setPositiveButton(android.R.string.ok,
                           (dialog, which) -> FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                                            fileDownloadHelper, FileDownloadHelper.OVERWRITE_FILES,
                                                            callbackHandler))
                   .setNegativeButton(android.R.string.cancel, noopClickListener)
                   .show();
        }
    }

    @Override
    protected AbstractAdditionalInfoFragment getAdditionalInfoFragment() {
        return null;
    }

    /**
     * Episode details query parameters.
     */
    private interface EpisodeDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Episodes.TITLE,
                MediaContract.Episodes.SHOWTITLE,
                MediaContract.Episodes.SEASON,
                MediaContract.Episodes.EPISODE,
                MediaContract.Episodes.THUMBNAIL,
                MediaContract.Episodes.FANART,
                MediaContract.Episodes.FIRSTAIRED,
                MediaContract.Episodes.RUNTIME,
                MediaContract.Episodes.RATING,
                MediaContract.Episodes.PLOT,
                MediaContract.Episodes.PLAYCOUNT,
                MediaContract.Episodes.DIRECTOR,
                MediaContract.Episodes.WRITER,
                MediaContract.Episodes.FILE,
                };

        int ID = 0;
        int TITLE = 1;
        int SHOWTITLE = 2;
        int SEASON = 3;
        int EPISODE = 4;
        int THUMBNAIL = 5;
        int FANART = 6;
        int FIRSTAIRED = 7;
        int RUNTIME = 8;
        int RATING = 9;
        int PLOT = 10;
        int PLAYCOUNT = 11;
        int DIRECTOR = 12;
        int WRITER = 13;
        int FILE = 14;
    }
}
