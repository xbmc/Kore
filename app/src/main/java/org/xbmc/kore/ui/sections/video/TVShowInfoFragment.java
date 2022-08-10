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
import android.provider.BaseColumns;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.utils.LogUtils;

/**
 * Presents a TV Show overview
 */
public class TVShowInfoFragment extends AbstractInfoFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(TVShowInfoFragment.class);

    // Loader IDs
    private static final int LOADER_TVSHOW = 0;

    // Controls whether a automatic sync refresh has been issued for this show
    private static boolean hasIssuedOutdatedRefresh = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hasIssuedOutdatedRefresh = false;
        LoaderManager.getInstance(this).initLoader(LOADER_TVSHOW, null, this);
    }

    @Override
    protected RefreshItem createRefreshItem() {
        RefreshItem refreshItem = new RefreshItem(requireContext(), LibrarySyncService.SYNC_SINGLE_TVSHOW);
        refreshItem.setSyncItem(LibrarySyncService.SYNC_TVSHOWID, getDataHolder().getId());
        refreshItem.setListener(event -> {
            if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                LoaderManager.getInstance(this).restartLoader(LOADER_TVSHOW, null, TVShowInfoFragment.this);
                refreshAdditionInfoFragment();
            }
        });

        return refreshItem;
    }

    @Override
    protected boolean setupInfoActionsBar() {
        return false;
    }

    @Override
    protected boolean setupFAB(FloatingActionButton fab) {
        return false;
    }

    @Override
    protected AbstractAdditionalInfoFragment getAdditionalInfoFragment() {
        TVShowProgressFragment tvShowProgressFragment = new TVShowProgressFragment();
        tvShowProgressFragment.setArgs(getDataHolder().getId(), getDataHolder().getTitle(), getDataHolder().getPosterUrl());
        return tvShowProgressFragment;
    }

    /*
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = MediaContract.TVShows.buildTVShowUri(getHostInfo().getId(), getDataHolder().getId());
        return new CursorLoader(requireContext(), uri,
                                TVShowDetailsQuery.PROJECTION, null, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null) {
            switch (cursorLoader.getId()) {
                case LOADER_TVSHOW:
                    cursor.moveToFirst();

                    DataHolder dataHolder = getDataHolder();

                    dataHolder.setFanArtUrl(cursor.getString(TVShowDetailsQuery.FANART));

                    dataHolder.setPosterUrl(cursor.getString(TVShowDetailsQuery.THUMBNAIL));
                    dataHolder.setRating(cursor.getDouble(TVShowDetailsQuery.RATING));
                    dataHolder.setVotes(cursor.getString(TVShowDetailsQuery.VOTES));
                    dataHolder.setImdbNumber(cursor.getString(TVShowDetailsQuery.IMDBNUMBER));

                    String premiered = cursor.getString(TVShowDetailsQuery.PREMIERED);
                    String studio = cursor.getString(TVShowDetailsQuery.STUDIO);

                    dataHolder.setDetails(String.format(getString(R.string.premiered), premiered) + "  |  " + studio +
                                          "\n" +
                                          cursor.getString(TVShowDetailsQuery.GENRES));

                    dataHolder.setTitle(cursor.getString(TVShowDetailsQuery.TITLE));

                    int numEpisodes = cursor.getInt(TVShowDetailsQuery.EPISODE),
                            watchedEpisodes = cursor.getInt(TVShowDetailsQuery.WATCHEDEPISODES);

                    dataHolder.setUndertitle(String.format(getString(R.string.num_episodes),
                                                           numEpisodes, numEpisodes - watchedEpisodes));

                    dataHolder.setDescription(cursor.getString(TVShowDetailsQuery.PLOT));

                    updateView(dataHolder);
                    checkOutdatedTVShowDetails(cursor);
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
     * Checks wether we should refresh the TV Show details with the info on XBMC
     * The details will be updated if the last update is older than what is configured in the
     * settings
     *
     * @param cursor Cursor with the data
     */
    private void checkOutdatedTVShowDetails(Cursor cursor) {
        if (hasIssuedOutdatedRefresh)
            return;

        cursor.moveToFirst();
        long lastUpdated = cursor.getLong(TVShowDetailsQuery.UPDATED);

        if (System.currentTimeMillis() > lastUpdated + Settings.DB_UPDATE_INTERVAL) {
            // Trigger a silent refresh
            hasIssuedOutdatedRefresh = true;
            getRefreshItem().startSync(true);
        }
    }

    /**
     * TV Show details query parameters.
     */
    private interface TVShowDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.TVShows.TITLE,
                MediaContract.TVShows.POSTER,
                MediaContract.TVShows.FANART,
                MediaContract.TVShows.PREMIERED,
                MediaContract.TVShows.STUDIO,
                MediaContract.TVShows.EPISODE,
                MediaContract.TVShows.WATCHEDEPISODES,
                MediaContract.TVShows.RATING,
                MediaContract.TVShows.PLOT,
                MediaContract.TVShows.PLAYCOUNT,
                MediaContract.TVShows.IMDBNUMBER,
                MediaContract.TVShows.GENRES,
                MediaContract.SyncColumns.UPDATED,
                MediaContract.TVShows.VOTES,
                };

        int ID = 0;
        int TITLE = 1;
        int THUMBNAIL = 2;
        int FANART = 3;
        int PREMIERED = 4;
        int STUDIO = 5;
        int EPISODE = 6;
        int WATCHEDEPISODES = 7;
        int RATING = 8;
        int PLOT = 9;
        int PLAYCOUNT = 10;
        int IMDBNUMBER = 11;
        int GENRES = 12;
        int UPDATED = 13;
        int VOTES = 14;
    }
}
