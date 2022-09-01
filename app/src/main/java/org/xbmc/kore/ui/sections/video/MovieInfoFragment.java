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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.CastFragment;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;

import java.io.File;

/**
 * Presents movie details
 */
public class MovieInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(MovieInfoFragment.class);

    // Loader IDs
    private static final int LOADER_MOVIE = 0;

    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    // Controls whether a automatic sync refresh has been issued for this show
    private static boolean hasIssuedOutdatedRefresh = false;

    private int moviePlaycount = 0;
    private FileDownloadHelper.MovieInfo movieDownloadInfo;

    @Override
    protected String getSyncType() {
        return LibrarySyncService.SYNC_SINGLE_MOVIE;
    }

    @Override
    protected Bundle getSyncExtras() {
        Bundle bundle = new Bundle();
        bundle.putInt(LibrarySyncService.SYNC_MOVIEID, getDataHolder().getId());
        return bundle;
    }

    @Override
    protected void onSyncProcessEnded(MediaSyncEvent event) {
        if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
            LoaderManager.getInstance(this).restartLoader(LOADER_MOVIE, null, this);
        }
    }

    @Override
    protected boolean setupInfoActionsBar() {
        setOnDownloadClickListener(view -> {
            // Check if the directory exists and whether to overwrite it
            File file = new File(movieDownloadInfo.getAbsoluteFilePath());
            if (file.exists()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setTitle(R.string.download)
                       .setMessage(R.string.download_file_exists)
                       .setPositiveButton(R.string.overwrite,
                                          (dialog, which) -> FileDownloadHelper.downloadFiles(requireContext(), getHostInfo(),
                                                                           movieDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                                                           callbackHandler))
                       .setNeutralButton(R.string.download_with_new_name,
                                         (dialog, which) -> FileDownloadHelper.downloadFiles(requireContext(), getHostInfo(),
                                                                          movieDownloadInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                                          callbackHandler))
                       .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                       .show();
            } else {
                // Confirm that the user really wants to download the file
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setTitle(R.string.download)
                       .setMessage(R.string.confirm_movie_download)
                       .setPositiveButton(android.R.string.ok,
                                          (dialog, which) -> FileDownloadHelper.downloadFiles(requireContext(), getHostInfo(),
                                                                           movieDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                                                           callbackHandler))
                       .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                       .setOnCancelListener(dialog -> setDownloadButtonState(false))
                       .show();
            }
        });
        setOnWatchedClickListener(view -> {
            // Set the playcount
            int newPlaycount = (moviePlaycount > 0) ? 0 : 1;

            VideoLibrary.SetMovieDetails action =
                    new VideoLibrary.SetMovieDetails(getDataHolder().getId(), newPlaycount, null);
            action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!isAdded()) return;
                    // Force a refresh, but don't show a message
                    startSync(true);
                    moviePlaycount = newPlaycount;
                    setWatchedButtonState(newPlaycount > 0);
                }

                @Override
                public void onError(int errorCode, String description) {
                    LogUtils.LOGD(TAG, "Error while setting watched state: " + description);
                }
            }, callbackHandler);
        });

        setOnQueueClickListener(view -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.movieid = getDataHolder().getId();
            MediaPlayerUtils.queue(MovieInfoFragment.this, item, PlaylistType.GetPlaylistsReturnType.VIDEO);
        });

        setOnStreamClickListener(v -> streamItemFromKodi(movieDownloadInfo.getMediaUrl(getHostInfo()), "video/*"));
        return true;
    }

    @Override
    protected boolean setupFAB(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            PlaylistType.Item item = new PlaylistType.Item();
            item.movieid = getDataHolder().getId();
            playItemOnKodi(item);
        });
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExpandDescription(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hasIssuedOutdatedRefresh = false;
        // Start the loaders
        LoaderManager.getInstance(this).initLoader(LOADER_MOVIE, null, this);
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
            case LOADER_MOVIE:
            default:
                uri = MediaContract.Movies.buildMovieUri(getHostInfo().getId(), getDataHolder().getId());
                return new CursorLoader(requireContext(), uri,
                                        MovieDetailsQuery.PROJECTION, null, null, null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_MOVIE:
                    cursor.moveToFirst();
                    moviePlaycount = cursor.getInt(MovieDetailsQuery.PLAYCOUNT);

                    DataHolder dataHolder = getDataHolder();
                    dataHolder.setFanArtUrl(cursor.getString(MovieDetailsQuery.FANART));
                    dataHolder.setPosterUrl(cursor.getString(MovieDetailsQuery.THUMBNAIL));
                    dataHolder.setRating(cursor.getDouble(MovieDetailsQuery.RATING));
                    dataHolder.setVotes(cursor.getString(MovieDetailsQuery.VOTES));

                    String director = cursor.getString(MovieDetailsQuery.DIRECTOR);
                    if (!TextUtils.isEmpty(director)) {
                        director = getString(R.string.directors) + " " + director;
                    }

                    int runtime = cursor.getInt(MovieDetailsQuery.RUNTIME) / 60;
                    String year = String.valueOf(cursor.getInt(MovieDetailsQuery.YEAR));
                    String durationYear =  runtime > 0 ?
                                           String.format(getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                                           "  |  " + year :
                                           year;

                    dataHolder.setDetails(durationYear
                                          + "\n" +
                                          cursor.getString(MovieDetailsQuery.GENRES)
                                          +"\n" +
                                          director);

                    dataHolder.setTitle(cursor.getString(MovieDetailsQuery.TITLE));
                    dataHolder.setUndertitle(cursor.getString(MovieDetailsQuery.TAGLINE));
                    dataHolder.setDescription(cursor.getString(MovieDetailsQuery.PLOT));

                    dataHolder.setSearchTerms(dataHolder.getTitle() + " movie");

                    movieDownloadInfo = new FileDownloadHelper.MovieInfo(
                            dataHolder.getTitle(), cursor.getString(MovieDetailsQuery.FILE));
                    setDownloadButtonState(movieDownloadInfo.downloadDirectoryExists());
                    setWatchedButtonState(cursor.getInt(MovieDetailsQuery.PLAYCOUNT) > 0);
                    updateView(dataHolder);
                    checkOutdatedMovieDetails(cursor);
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
    protected AbstractFragment getAdditionalInfoFragment() {
        CastFragment castFragment = new CastFragment();
        castFragment.setArgs(getDataHolder().getId(), getDataHolder().getTitle(),
                             CastFragment.TYPE.MOVIE);
        return castFragment;
    }

    /**
     * Checks wether we should refresh the movie details with the info on XBMC
     * The details will be updated if the last update is older than what is configured in the
     * settings
     *
     * @param cursor Cursor with the data
     */
    private void checkOutdatedMovieDetails(Cursor cursor) {
        if (hasIssuedOutdatedRefresh)
            return;

        long lastUpdated = cursor.getLong(MovieDetailsQuery.UPDATED);

        if (System.currentTimeMillis() > lastUpdated + Settings.DB_UPDATE_INTERVAL) {
            // Trigger a silent refresh
            hasIssuedOutdatedRefresh = true;
            startSync(true);
        }
    }

    /**
     * Movie details query parameters.
     */
    private interface MovieDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Movies.TITLE,
                MediaContract.Movies.TAGLINE,
                MediaContract.Movies.POSTER,
                MediaContract.Movies.FANART,
                MediaContract.Movies.YEAR,
                MediaContract.Movies.GENRES,
                MediaContract.Movies.RUNTIME,
                MediaContract.Movies.RATING,
                MediaContract.Movies.VOTES,
                MediaContract.Movies.PLOT,
                MediaContract.Movies.PLAYCOUNT,
                MediaContract.Movies.DIRECTOR,
                MediaContract.Movies.IMDBNUMBER,
                MediaContract.Movies.FILE,
                MediaContract.SyncColumns.UPDATED,
                };

        int ID = 0;
        int TITLE = 1;
        int TAGLINE = 2;
        int THUMBNAIL = 3;
        int FANART = 4;
        int YEAR = 5;
        int GENRES = 6;
        int RUNTIME = 7;
        int RATING = 8;
        int VOTES = 9;
        int PLOT = 10;
        int PLAYCOUNT = 11;
        int DIRECTOR = 12;
        int IMDBNUMBER = 13;
        int FILE = 14;
        int UPDATED = 15;
    }
}
