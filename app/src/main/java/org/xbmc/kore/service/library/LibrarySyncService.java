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
package org.xbmc.kore.service.library;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;

import org.xbmc.kore.BuildConfig;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;

/**
 * Service that syncs the XBMC local database with the remote library
 */
public class LibrarySyncService extends Service {
    public static final String TAG = LogUtils.makeLogTag(LibrarySyncService.class);
    private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;

    /**
     * Possible requests to sync, where more than one can be specified.
     * Include a boolean extra in the start intent with these identifiers to set the type of requests to sync
     */
    public static final String SYNC_ALL_MOVIES = PACKAGE_NAME + ".sync_all_movies";
    public static final String SYNC_SINGLE_MOVIE = PACKAGE_NAME + ".sync_single_movie";
    public static final String SYNC_ALL_TVSHOWS = PACKAGE_NAME + ".sync_all_tvshows";
    public static final String SYNC_SINGLE_TVSHOW = PACKAGE_NAME + ".sync_single_tvshow";
    public static final String SYNC_ALL_MUSIC = PACKAGE_NAME + ".sync_all_music";
    public static final String SYNC_ALL_MUSIC_VIDEOS = PACKAGE_NAME + ".sync_all_music_videos";

    /**
     * If a single movie or tv show is requested to be synced, specify its id in the intent by including an
     * integer extra with these identifiers
     */
    public static final String SYNC_MOVIEID = PACKAGE_NAME + ".sync_movieid";
    public static final String SYNC_TVSHOWID = PACKAGE_NAME + ".sync_tvshowid";

    /**
     * Extra used to pass parameters that will be sent back to the caller
     */
    public static final String SYNC_ITEM_PARAMS = PACKAGE_NAME + ".sync_item_params";

    /**
     * Constant for UI to use to signal a silent sync (pass these in SYNC_EXTRAS)
     */
    public static final String SILENT_SYNC = PACKAGE_NAME + ".silent_sync";

    /**
     * Our handler to post callbacks from {@link HostConnection} calls
     */
    private Handler callbackHandler;
    private HandlerThread handlerThread;

    private ArrayList<SyncOrchestrator> syncOrchestrators;

    private final IBinder serviceBinder = new LocalBinder();

    @Override
    public void onCreate() {
        // Create a Handler Thread to process callback calls after the Xbmc method call
        handlerThread = new HandlerThread("LibrarySyncService", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        callbackHandler = new Handler(handlerThread.getLooper());
        // Check which libraries to update and call the corresponding methods on Xbmc

        syncOrchestrators = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        // Get the connection here, not on create because we can be called for different hosts
        // We'll use a specific connection through HTTP, not the singleton one,
        // to not interfere with the normal application usage of it (namely calls to disconnect
        // and usage of the socket).
        HostInfo hostInfo = HostManager.getInstance(this).getHostInfo();

        SyncOrchestrator syncOrchestrator = new SyncOrchestrator(this, startId, hostInfo,
                callbackHandler, getContentResolver());
        syncOrchestrator.setListener(syncOrchestrator1 -> stopSelf(startId));

        syncOrchestrators.add(syncOrchestrator);

        // Get the request parameters that we should pass when calling back the caller
        Bundle syncItemParams = intent.getBundleExtra(SYNC_ITEM_PARAMS);

        // Sync all movies
        boolean syncAllMovies = intent.getBooleanExtra(SYNC_ALL_MOVIES, false);
        if (syncAllMovies) {
            syncOrchestrator.addSyncItem(new SyncMovies(hostInfo.getId(), syncItemParams));
        }

        // Sync a single movie
        boolean syncSingleMovie = intent.getBooleanExtra(SYNC_SINGLE_MOVIE, false);
        if (syncSingleMovie) {
            int movieId = intent.getIntExtra(SYNC_MOVIEID, -1);
            if (movieId != -1) {
                syncOrchestrator.addSyncItem(new SyncMovies(hostInfo.getId(), movieId, syncItemParams));
            }
        }

        // Sync all tvshows
        boolean syncAllTVShows = intent.getBooleanExtra(SYNC_ALL_TVSHOWS, false);
        if (syncAllTVShows) {
            syncOrchestrator.addSyncItem(new SyncTVShows(hostInfo.getId(), syncItemParams));
        }

        // Sync a single tvshow
        boolean syncSingleTVShow = intent.getBooleanExtra(SYNC_SINGLE_TVSHOW, false);
        if (syncSingleTVShow) {
            int tvshowId = intent.getIntExtra(SYNC_TVSHOWID, -1);
            if (tvshowId != -1) {
                syncOrchestrator.addSyncItem(new SyncTVShows(hostInfo.getId(), tvshowId, syncItemParams));
            }
        }

        // Sync all music
        boolean syncAllMusic = intent.getBooleanExtra(SYNC_ALL_MUSIC, false);
        if (syncAllMusic) {
            syncOrchestrator.addSyncItem(new SyncMusic(syncItemParams));
        }

        // Sync all music videos
        boolean syncAllMusicVideos = intent.getBooleanExtra(SYNC_ALL_MUSIC_VIDEOS, false);
        if (syncAllMusicVideos) {
            syncOrchestrator.addSyncItem(new SyncMusicVideos(hostInfo.getId(), syncItemParams));
        }

        // Start syncing
        syncOrchestrator.startSync();

        // If we get killed, after returning from here, don't restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        LogUtils.LOGD(TAG, "Destroying the service.");
        handlerThread.quitSafely();
    }

    public class LocalBinder extends Binder {
        public LibrarySyncService getService() {
            return LibrarySyncService.this;
        }
    }

    /**
     *
     * @param hostInfo host information for which to get items currently syncing
     * @return currently syncing syncitems for given hostInfo
     */
    public ArrayList<SyncItem> getItemsSyncing(HostInfo hostInfo) {
        for(SyncOrchestrator orchestrator : syncOrchestrators) {
            if(orchestrator.getHostInfo().getId() == hostInfo.getId() ) {
                return new ArrayList<>(orchestrator.getSyncItems());
            }
        }
        return null;
    }
}
