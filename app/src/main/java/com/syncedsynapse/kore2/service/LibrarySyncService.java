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
package com.syncedsynapse.kore2.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;

import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.ApiCallback;
import com.syncedsynapse.kore2.jsonrpc.HostConnection;
import com.syncedsynapse.kore2.jsonrpc.event.MediaSyncEvent;
import com.syncedsynapse.kore2.jsonrpc.method.*;
import com.syncedsynapse.kore2.jsonrpc.type.AudioType;
import com.syncedsynapse.kore2.jsonrpc.type.LibraryType;
import com.syncedsynapse.kore2.jsonrpc.type.ListType;
import com.syncedsynapse.kore2.jsonrpc.type.VideoType;
import com.syncedsynapse.kore2.provider.MediaContract;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.Utils;

import java.lang.System;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Service that syncs the XBMC local database with the remote library
 */
public class LibrarySyncService extends Service {
    public static final String TAG = LogUtils.makeLogTag(LibrarySyncService.class);

    private static final int LIMIT_SYNC_MOVIES = 300;
    private static final int LIMIT_SYNC_TVSHOWS = 300;
    private static final int LIMIT_SYNC_ARTISTS = 300;
    private static final int LIMIT_SYNC_ALBUMS = 300;
    private static final int LIMIT_SYNC_SONGS = 600;

    /**
     * Possible requests to sync
     */
    public static final String SYNC_ALL_MOVIES = "sync_all_movies";
    public static final String SYNC_SINGLE_MOVIE = "sync_single_movie";
    public static final String SYNC_ALL_TVSHOWS = "sync_all_tvshows";
    public static final String SYNC_SINGLE_TVSHOW = "sync_single_tvshow";
    public static final String SYNC_ALL_MUSIC = "sync_all_music";
    public static final String SYNC_ALL_MUSIC_VIDEOS = "sync_all_music_videos";

    public static final String SYNC_MOVIEID = "sync_movieid";
    public static final String SYNC_TVSHOWID = "sync_tvshowid";

    /**
     * Extra used to pass parameters that will be sent back to the caller
     */
    public static final String SYNC_EXTRAS = "sync_extras";

    /**
     * Constant for UI to use to signal a silent sync (pass these in SYNC_EXTRAS)
     */
    public static final String SILENT_SYNC = "silent_sync";

    /**
     * Our handler to post callbacks from {@link HostConnection} calls
     */
    private Handler callbackHandler;
    private HandlerThread handlerThread;

    @Override
    public void onCreate() {
        // Create a Handler Thread to process callback calls after the Xbmc method call
        handlerThread = new HandlerThread("LibrarySyncService", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        callbackHandler = new Handler(handlerThread.getLooper());
        // Check which libraries to update and call the corresponding methods on Xbmc
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get the connection here, not on create because we can be called for different hosts
        // We'll use a specific connection through HTTP, not the singleton one,
        // to not interfere with the normal application usage of it (namely calls to disconnect
        // and usage of the socket).
        HostInfo hostInfo = HostManager.getInstance(this).getHostInfo();
        HostConnection hostConnection = new HostConnection(hostInfo);
        hostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        SyncOrchestrator syncOrchestrator = new SyncOrchestrator(this, startId, hostConnection,
                callbackHandler, getContentResolver());

        // Get the request parameters that we should pass when calling back the caller
        Bundle syncExtras = intent.getBundleExtra(SYNC_EXTRAS);

        // Sync all movies
        boolean syncAllMovies = intent.getBooleanExtra(SYNC_ALL_MOVIES, false);
        if (syncAllMovies) {
            syncOrchestrator.addSyncItem(new SyncMovies(hostInfo.getId(), syncExtras));
        }

        // Sync a single movie
        boolean syncSingleMovie = intent.getBooleanExtra(SYNC_SINGLE_MOVIE, false);
        if (syncSingleMovie) {
            int movieId = intent.getIntExtra(SYNC_MOVIEID, -1);
            if (movieId != -1) {
                syncOrchestrator.addSyncItem(new SyncMovies(hostInfo.getId(), movieId, syncExtras));
            }
        }

        // Sync all tvshows
        boolean syncAllTVShows = intent.getBooleanExtra(SYNC_ALL_TVSHOWS, false);
        if (syncAllTVShows) {
            syncOrchestrator.addSyncItem(new SyncTVShows(hostInfo.getId(), syncExtras));
        }

        // Sync a single tvshow
        boolean syncSingleTVShow = intent.getBooleanExtra(SYNC_SINGLE_TVSHOW, false);
        if (syncSingleTVShow) {
            int tvshowId = intent.getIntExtra(SYNC_TVSHOWID, -1);
            if (tvshowId != -1) {
                syncOrchestrator.addSyncItem(new SyncTVShows(hostInfo.getId(), tvshowId, syncExtras));
            }
        }

        // Sync all music
        boolean syncAllMusic = intent.getBooleanExtra(SYNC_ALL_MUSIC, false);
        if (syncAllMusic) {
            syncOrchestrator.addSyncItem(new SyncMusic(hostInfo.getId(), syncExtras));
        }

        // Sync all music videos
        boolean syncAllMusicVideos = intent.getBooleanExtra(SYNC_ALL_MUSIC_VIDEOS, false);
        if (syncAllMusicVideos) {
            syncOrchestrator.addSyncItem(new SyncMusicVideos(hostInfo.getId(), syncExtras));
        }

        // Start syncing
        syncOrchestrator.startSync();

        // If we get killed, after returning from here, don't restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        LogUtils.LOGD(TAG, "Destroying the service.");
        if (Utils.isJellybeanMR2OrLater()) {
            handlerThread.quitSafely();
        } else {
            handlerThread.quit();
        }
    }

    /**
     * Orchestrator for a list os SyncItems
     * Keeps a list of SyncItems to sync, and calls each one in order
     * When finishes cleans up and stops the service by calling stopSelf
     */
    private static class SyncOrchestrator {
        private ArrayDeque<SyncItem> syncItems;
        private Service syncService;
        private final int serviceStartId;
        private final HostConnection hostConnection;
        private final Handler callbackHandler;
        private final ContentResolver contentResolver;

        private SyncItem currentSyncItem;
        /**
         * Constructor
         * @param syncService Service on which to call {@link #stopSelf()} when finished
         * @param startId Service startid to use when calling {@link #stopSelf()}
         * @param hostConnection Host connection to use
         * @param callbackHandler Handler on which to post callbacks
         * @param contentResolver Content resolver
         */
        public SyncOrchestrator(Service syncService, final int startId,
                                final HostConnection hostConnection,
                                final Handler callbackHandler,
                                final ContentResolver contentResolver) {
            this.syncService = syncService;
            this.syncItems = new ArrayDeque<SyncItem>();
            this.serviceStartId = startId;
            this.hostConnection = hostConnection;
            this.callbackHandler = callbackHandler;
            this.contentResolver = contentResolver;
        }

        /**
         * Add this item to the sync list
         * @param syncItem Sync item
         */
        public void addSyncItem(SyncItem syncItem) {
            syncItems.add(syncItem);
        }

        private long startTime = -1;
        private long partialStartTime;

        /**
         * Starts the syncing process
         */
        public void startSync() {
            startTime = System.currentTimeMillis();
            nextSync();
        }

        /**
         * Processes the next item on the sync list, or cleans up if it is finished.
         */
        public void nextSync() {
            if (syncItems.size() > 0) {
                partialStartTime = System.currentTimeMillis();
                currentSyncItem = syncItems.poll();
                currentSyncItem.sync(this, hostConnection, callbackHandler, contentResolver);
            } else {
                LogUtils.LOGD(TAG, "Sync finished for all items. Total time: " +
                                   (System.currentTimeMillis() - startTime));
                // No more syncs, cleanup.
                // No need to disconnect, as this is HTTP
                //hostConnection.disconnect();
                syncService.stopSelf(serviceStartId);
            }
        }

        /**
         * One of the syync items finish syncing
         */
        public void syncItemFinished() {
            LogUtils.LOGD(TAG, "Sync finished for item: " + currentSyncItem.getDescription() +
                               ". Total time: " + (System.currentTimeMillis() - partialStartTime));

            EventBus.getDefault()
                    .post(new MediaSyncEvent(currentSyncItem.getSyncType(),
                            currentSyncItem.getSyncExtras(),
                            MediaSyncEvent.STATUS_SUCCESS));
            nextSync();
        }

        /**
         * One of the sync items failed, stop and clean up
         * @param errorCode Error code
         * @param description Description
         */
        public void syncItemFailed(int errorCode, String description) {
            LogUtils.LOGD(TAG, "A Sync item has got an error. Sync item: " +
                               currentSyncItem.getDescription() +
                               ". Error description: " + description);
            // No need to disconnect, as this is HTTP
            //hostConnection.disconnect();
            EventBus.getDefault()
                    .post(new MediaSyncEvent(currentSyncItem.getSyncType(),
                            currentSyncItem.getSyncExtras(),
                            MediaSyncEvent.STATUS_FAIL, errorCode, description));
            // Keep syncing till the end
            nextSync();
            //syncService.stopSelf(serviceStartId);
        }
    }

    /**
     * Represent an item that can be synced
     */
    private interface SyncItem {
        /**
         * Syncs an item from the XBMC host to the local database
         * @param orchestrator Orchestrator to call when finished
         * @param hostConnection Host connection to use
         * @param callbackHandler Handler on which to post callbacks
         * @param contentResolver Content resolver
         */
        public void sync(final SyncOrchestrator orchestrator,
                         final HostConnection hostConnection,
                         final Handler callbackHandler,
                         final ContentResolver contentResolver);

        /**
         * Friendly description of this sync item
         * @return Description
         */
        public String getDescription();

        /**
         * Returns the sync event that should be posted after completion
         * @return Sync type, one of the constants in {@link com.syncedsynapse.kore2.service.LibrarySyncService}
         */
        public String getSyncType();

        /**
         * Returns the extras that were passed during creation.
         * Allows the caller to pass parameters that will be sent back to him
         * @return Sync extras passed during construction
         */
        public Bundle getSyncExtras();
    }

    /**
     * Syncs all the movies on XBMC or a specific movie, to the local database
     */
    private static class SyncMovies implements SyncItem {
        private final int hostId;
        private final int movieId;
        private final Bundle syncExtras;

        /**
         * Syncs all the movies on selected XBMC to the local database
         * @param hostId XBMC host id
         */
        public SyncMovies(final int hostId, Bundle syncExtras) {
            this.hostId = hostId;
            this.movieId = -1;
            this.syncExtras = syncExtras;
        }

        /**
         * Syncs a specific movie on selected XBMC to the local database
         * @param hostId XBMC host id
         */
        public SyncMovies(final int hostId, final int movieId, Bundle syncExtras) {
            this.hostId = hostId;
            this.movieId = movieId;
            this.syncExtras = syncExtras;
        }

        /** {@inheritDoc} */
        public String getDescription() {
            return (movieId != -1) ?
                   "Sync movies for host: " + hostId :
                   "Sync movie " + movieId + " for host: " + hostId;
        }

        /** {@inheritDoc} */
        public String getSyncType() {
            return (movieId == -1) ? SYNC_ALL_MOVIES : SYNC_SINGLE_MOVIE;
        }

        /** {@inheritDoc} */
        public Bundle getSyncExtras() {
            return syncExtras;
        }

        /** {@inheritDoc} */
        public void sync(final SyncOrchestrator orchestrator,
                         final HostConnection hostConnection,
                         final Handler callbackHandler,
                         final ContentResolver contentResolver) {
            String properties[] = {
                    VideoType.FieldsMovie.TITLE, VideoType.FieldsMovie.GENRE,
                    VideoType.FieldsMovie.YEAR, VideoType.FieldsMovie.RATING,
                    VideoType.FieldsMovie.DIRECTOR, VideoType.FieldsMovie.TRAILER,
                    VideoType.FieldsMovie.TAGLINE, VideoType.FieldsMovie.PLOT,
                    // VideoType.FieldsMovie.PLOTOUTLINE, VideoType.FieldsMovie.ORIGINALTITLE,
                    // VideoType.FieldsMovie.LASTPLAYED,
                    VideoType.FieldsMovie.PLAYCOUNT, VideoType.FieldsMovie.DATEADDED,
                    VideoType.FieldsMovie.WRITER, VideoType.FieldsMovie.STUDIO,
                    VideoType.FieldsMovie.MPAA, VideoType.FieldsMovie.CAST,
                    VideoType.FieldsMovie.COUNTRY, VideoType.FieldsMovie.IMDBNUMBER,
                    VideoType.FieldsMovie.RUNTIME, VideoType.FieldsMovie.SET,
                    // VideoType.FieldsMovie.SHOWLINK,
                    VideoType.FieldsMovie.STREAMDETAILS, VideoType.FieldsMovie.TOP250,
                    VideoType.FieldsMovie.VOTES, VideoType.FieldsMovie.FANART,
                    VideoType.FieldsMovie.THUMBNAIL, VideoType.FieldsMovie.FILE,
                    // VideoType.FieldsMovie.SORTTITLE, VideoType.FieldsMovie.RESUME,
                    VideoType.FieldsMovie.SETID,
                    // VideoType.FieldsMovie.DATEADDED, VideoType.FieldsMovie.TAG,
                    // VideoType.FieldsMovie.ART
            };

            if (movieId == -1) {
                syncAllMovies(orchestrator, hostConnection, callbackHandler, contentResolver, properties, 0);
            } else {
                // Sync a specific movie
                VideoLibrary.GetMovieDetails action =
                        new VideoLibrary.GetMovieDetails(movieId, properties);
                action.execute(hostConnection, new ApiCallback<VideoType.DetailsMovie>() {
                    @Override
                    public void onSucess(VideoType.DetailsMovie result) {
                        deleteMovies(contentResolver, hostId, movieId);
                        List<VideoType.DetailsMovie> movies = new ArrayList<VideoType.DetailsMovie>(1);
                        movies.add(result);
                        insertMovies(orchestrator, contentResolver, movies);
                        orchestrator.syncItemFinished();
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        // Ok, something bad happend, just quit
                        orchestrator.syncItemFailed(errorCode, description);
                    }
                }, callbackHandler);
            }
        }

        /**
         * Syncs all the movies, calling itself recursively
         * Uses the {@link VideoLibrary.GetMovies} version with limits to make sure
         * that Kodi doesn't blow up, and calls itself recursively until all the
         * movies are returned
         */
        private void syncAllMovies(final SyncOrchestrator orchestrator,
                                   final HostConnection hostConnection,
                                   final Handler callbackHandler,
                                   final ContentResolver contentResolver,
                                   final String properties[],
                                   final int startIdx) {
            // Call GetMovies with the current limits set
            ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_MOVIES);
            VideoLibrary.GetMovies action = new VideoLibrary.GetMovies(limits, properties);
            action.execute(hostConnection, new ApiCallback<List<VideoType.DetailsMovie>>() {
                @Override
                public void onSucess(List<VideoType.DetailsMovie> result) {
                    if (startIdx == 0) {
                        // First call, delete movies from DB
                        deleteMovies(contentResolver, hostId, -1);
                    }
                    if (result.size() > 0) {
                        insertMovies(orchestrator, contentResolver, result);
                    }

                    LogUtils.LOGD(TAG, "syncAllMovies, movies gotten: " + result.size());
                    if (result.size() == LIMIT_SYNC_MOVIES) {
                        // Max limit returned, there may be some more movies
                        // As we're going to recurse, these result objects can add up, so
                        // let's help the GC and indicate that we don't need this memory
                        // (hopefully this works)
                        result = null;
                        syncAllMovies(orchestrator, hostConnection, callbackHandler, contentResolver,
                                properties, startIdx + LIMIT_SYNC_MOVIES);
                    } else {
                        // Less than the limit was returned so we can finish
                        // (if it returned more there's a bug in Kodi but it
                        // shouldn't be a problem as they got inserted in the DB)
                        orchestrator.syncItemFinished();
                    }
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

        /**
         * Deletes one or all movies from the database (pass -1 on movieId to delete all)
         */
        private void deleteMovies(final ContentResolver contentResolver,
                                  int hostId, int movieId) {
            if (movieId == -1) {
                // Delete all movies
                String where = MediaContract.MoviesColumns.HOST_ID + "=?";
                contentResolver.delete(MediaContract.MovieCast.CONTENT_URI,
                        where, new String[]{String.valueOf(hostId)});
                contentResolver.delete(MediaContract.Movies.CONTENT_URI,
                        where, new String[]{String.valueOf(hostId)});
            } else {
                // Delete a movie
                contentResolver.delete(MediaContract.MovieCast.buildMovieCastListUri(hostId, movieId),
                        null, null);
                contentResolver.delete(MediaContract.Movies.buildMovieUri(hostId, movieId),
                        null, null);
            }
        }

        /**
         * Inserts the given movies in the database
         */
        private void insertMovies(final SyncOrchestrator orchestrator,
                                  final ContentResolver contentResolver,
                                  final List<VideoType.DetailsMovie> movies) {
            ContentValues movieValuesBatch[] = new ContentValues[movies.size()];
            int castCount = 0;

            // Iterate on each movie
            for (int i = 0; i < movies.size(); i++) {
                VideoType.DetailsMovie movie = movies.get(i);
                movieValuesBatch[i] = SyncUtils.contentValuesFromMovie(hostId, movie);
                castCount += movie.cast.size();
            }

            // Insert the movies
            contentResolver.bulkInsert(MediaContract.Movies.CONTENT_URI, movieValuesBatch);

            ContentValues movieCastValuesBatch[] = new ContentValues[castCount];
            int count = 0;
            // Iterate on each movie/cast
            for (VideoType.DetailsMovie movie : movies) {
                for (VideoType.Cast cast : movie.cast) {
                    movieCastValuesBatch[count] = SyncUtils.contentValuesFromCast(hostId, cast);
                    movieCastValuesBatch[count].put(MediaContract.MovieCastColumns.MOVIEID, movie.movieid);
                    count++;
                }
            }

            // Insert the cast list for this movie
            contentResolver.bulkInsert(MediaContract.MovieCast.CONTENT_URI, movieCastValuesBatch);
        }
    }

    /**
     * Syncs all the TV shows or a specific show and its information to the local database
     */
    private static class SyncTVShows implements SyncItem {
        private final int hostId;
        private final int tvshowId;
        private final Bundle syncExtras;

        /**
         * Syncs all the TVShows on selected XBMC to the local database
         * @param hostId XBMC host id
         */
        public SyncTVShows(final int hostId, Bundle syncExtras) {
            this.hostId = hostId;
            this.tvshowId = -1;
            this.syncExtras = syncExtras;
        }

        /**
         * Syncs a specific TVShow to the local database
         * @param hostId XBMC host id
         * @param tvshowId Show to sync
         */
        public SyncTVShows(final int hostId, final int tvshowId, Bundle syncExtras) {
            this.hostId = hostId;
            this.tvshowId = tvshowId;
            this.syncExtras = syncExtras;
        }

        /** {@inheritDoc} */
        public String getDescription() {
            return (tvshowId != -1) ?
                   "Sync TV shows for host: " + hostId :
                   "Sync TV show " + tvshowId + " for host: " + hostId;
        }

        /** {@inheritDoc} */
        public String getSyncType() {
            return (tvshowId == -1) ? SYNC_ALL_TVSHOWS : SYNC_SINGLE_TVSHOW;
        }

        /** {@inheritDoc} */
        public Bundle getSyncExtras() {
            return syncExtras;
        }

        private final static String getTVShowsProperties[] = {
                VideoType.FieldsTVShow.TITLE, VideoType.FieldsTVShow.GENRE,
                //VideoType.FieldsTVShow.YEAR,
                VideoType.FieldsTVShow.RATING, VideoType.FieldsTVShow.PLOT,
                VideoType.FieldsTVShow.STUDIO, VideoType.FieldsTVShow.MPAA,
                VideoType.FieldsTVShow.CAST, VideoType.FieldsTVShow.PLAYCOUNT,
                VideoType.FieldsTVShow.EPISODE, VideoType.FieldsTVShow.IMDBNUMBER,
                VideoType.FieldsTVShow.PREMIERED,
                //VideoType.FieldsTVShow.VOTES, VideoType.FieldsTVShow.LASTPLAYED,
                VideoType.FieldsTVShow.FANART, VideoType.FieldsTVShow.THUMBNAIL,
                VideoType.FieldsTVShow.FILE,
                //VideoType.FieldsTVShow.ORIGINALTITLE, VideoType.FieldsTVShow.SORTTITLE,
                // VideoType.FieldsTVShow.EPISODEGUIDE, VideoType.FieldsTVShow.SEASON,
                VideoType.FieldsTVShow.WATCHEDEPISODES, VideoType.FieldsTVShow.DATEADDED,
                //VideoType.FieldsTVShow.TAG, VideoType.FieldsTVShow.ART
        };
        /** {@inheritDoc} */
        public void sync(final SyncOrchestrator orchestrator,
                         final HostConnection hostConnection,
                         final Handler callbackHandler,
                         final ContentResolver contentResolver) {
            if (tvshowId == -1) {
                syncAllTVShows(orchestrator, hostConnection, callbackHandler, contentResolver,
                        0, new ArrayList<VideoType.DetailsTVShow>());
            } else {
                VideoLibrary.GetTVShowDetails action =
                        new VideoLibrary.GetTVShowDetails(tvshowId, getTVShowsProperties);
                action.execute(hostConnection, new ApiCallback<VideoType.DetailsTVShow>() {
                    @Override
                    public void onSucess(VideoType.DetailsTVShow result) {
                        deleteTVShows(contentResolver, hostId, tvshowId);
                        List<VideoType.DetailsTVShow> tvShows = new ArrayList<>(1);
                        tvShows.add(result);
                        insertTVShowsAndGetDetails(orchestrator, hostConnection, callbackHandler,
                                contentResolver, tvShows);
                        // insertTVShows calls syncItemFinished
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        // Ok, something bad happend, just quit
                        orchestrator.syncItemFailed(errorCode, description);
                    }
                }, callbackHandler);
            }
        }

        /**
         * Syncs all the TV shows, calling itself recursively
         * Uses the {@link VideoLibrary.GetTVShows} version with limits to make sure
         * that Kodi doesn't blow up, and calls itself recursively until all the
         * shows are returned
         */
        private void syncAllTVShows(final SyncOrchestrator orchestrator,
                                   final HostConnection hostConnection,
                                   final Handler callbackHandler,
                                   final ContentResolver contentResolver,
                                   final int startIdx,
                                   final List<VideoType.DetailsTVShow> allResults) {
            // Call GetTVShows with the current limits set
            ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_TVSHOWS);
            VideoLibrary.GetTVShows action = new VideoLibrary.GetTVShows(limits, getTVShowsProperties);
            action.execute(hostConnection, new ApiCallback<List<VideoType.DetailsTVShow>>() {
                @Override
                public void onSucess(List<VideoType.DetailsTVShow> result) {
                    allResults.addAll(result);
                    if (result.size() == LIMIT_SYNC_TVSHOWS) {
                        // Max limit returned, there may be some more movies
                        LogUtils.LOGD(TAG, "syncAllTVShows: More tv shows on media center, recursing.");
                        syncAllTVShows(orchestrator, hostConnection, callbackHandler, contentResolver,
                                startIdx + LIMIT_SYNC_TVSHOWS, allResults);
                    } else {
                        // Ok, we have all the shows, insert them
                        LogUtils.LOGD(TAG, "syncAllTVShows: Got all tv shows");
                        deleteTVShows(contentResolver, hostId, -1);
                        insertTVShowsAndGetDetails(orchestrator, hostConnection, callbackHandler,
                                contentResolver, allResults);
                    }
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

        private void deleteTVShows(final ContentResolver contentResolver,
                                   int hostId, int tvshowId) {
            if (tvshowId == -1) {
                // Delete all tvshows
                String where = MediaContract.TVShowsColumns.HOST_ID + "=?";
                contentResolver.delete(MediaContract.Episodes.CONTENT_URI,
                        where, new String[]{String.valueOf(hostId)});
                contentResolver.delete(MediaContract.Seasons.CONTENT_URI,
                        where, new String[]{String.valueOf(hostId)});
                contentResolver.delete(MediaContract.TVShowCast.CONTENT_URI,
                        where, new String[]{String.valueOf(hostId)});
                contentResolver.delete(MediaContract.TVShows.CONTENT_URI,
                        where, new String[]{String.valueOf(hostId)});
            } else {
                // Delete a specific tvshow
                contentResolver.delete(MediaContract.Episodes.buildTVShowEpisodesListUri(hostId, tvshowId),
                        null, null);
                contentResolver.delete(MediaContract.Seasons.buildTVShowSeasonsListUri(hostId, tvshowId),
                        null, null);
                contentResolver.delete(MediaContract.TVShowCast.buildTVShowCastListUri(hostId, tvshowId),
                        null, null);
                contentResolver.delete(MediaContract.TVShows.buildTVShowUri(hostId, tvshowId),
                        null, null);
            }
        }

        private void insertTVShowsAndGetDetails(final SyncOrchestrator orchestrator,
                                                final HostConnection hostConnection,
                                                final Handler callbackHandler,
                                                final ContentResolver contentResolver,
                                                List<VideoType.DetailsTVShow> tvShows) {
            ContentValues tvshowsValuesBatch[] = new ContentValues[tvShows.size()];
            int castCount = 0;

            // Iterate on each show
            for (int i = 0; i < tvShows.size(); i++) {
                VideoType.DetailsTVShow tvshow = tvShows.get(i);
                tvshowsValuesBatch[i] = SyncUtils.contentValuesFromTVShow(hostId, tvshow);
                castCount += tvshow.cast.size();
            }
            // Insert the tvshows
            contentResolver.bulkInsert(MediaContract.TVShows.CONTENT_URI, tvshowsValuesBatch);

            ContentValues tvshowsCastValuesBatch[] = new ContentValues[castCount];
            int count = 0;
            // Iterate on each show/cast
            for (VideoType.DetailsTVShow tvshow : tvShows) {
                for (VideoType.Cast cast : tvshow.cast) {
                    tvshowsCastValuesBatch[count] = SyncUtils.contentValuesFromCast(hostId, cast);
                    tvshowsCastValuesBatch[count].put(MediaContract.TVShowCastColumns.TVSHOWID, tvshow.tvshowid);
                    count++;
                }
            }
            // Insert the cast list for this movie
            contentResolver.bulkInsert(MediaContract.TVShowCast.CONTENT_URI, tvshowsCastValuesBatch);

            // Start the sequential syncing of seasons
            chainSyncSeasons(orchestrator, hostConnection, callbackHandler,
                    contentResolver, tvShows, 0);
        }

        private final static String seasonsProperties[] = {
                VideoType.FieldsSeason.SEASON, VideoType.FieldsSeason.SHOWTITLE,
                //VideoType.FieldsSeason.PLAYCOUNT,
                VideoType.FieldsSeason.EPISODE,
                VideoType.FieldsSeason.FANART, VideoType.FieldsSeason.THUMBNAIL,
                VideoType.FieldsSeason.TVSHOWID, VideoType.FieldsSeason.WATCHEDEPISODES,
                //VideoType.FieldsSeason.ART
        };

        /**
         * Sequentially syncs seasons for the tvshow specified, and on success recursively calls
         * itself to sync the next tvshow on the list.
         * This basically iterates through the tvshows list updating the seasons,
         * in a sequential manner (defeating the parallel nature of host calls)
         * After processing all tvshows on the list, starts the episode syncing
         *
         * @param orchestrator Orchestrator to call when finished
         * @param hostConnection Host connection to use
         * @param callbackHandler Handler on which to post callbacks
         * @param contentResolver Content resolver
         * @param tvShows TV shows list to get seasons to
         * @param position Position of the tvshow on the list to process
         */
        private void chainSyncSeasons(final SyncOrchestrator orchestrator,
                                      final HostConnection hostConnection,
                                      final Handler callbackHandler,
                                      final ContentResolver contentResolver,
                                      final List<VideoType.DetailsTVShow> tvShows,
                                      final int position) {
            if (position < tvShows.size()) {
                // Process this tvshow
                final VideoType.DetailsTVShow tvShow = tvShows.get(position);

                VideoLibrary.GetSeasons action = new VideoLibrary.GetSeasons(tvShow.tvshowid, seasonsProperties);
                action.execute(hostConnection, new ApiCallback<List<VideoType.DetailsSeason>>() {
                    @Override
                    public void onSucess(List<VideoType.DetailsSeason> result) {
                        ContentValues seasonsValuesBatch[] = new ContentValues[result.size()];
                        int totalWatchedEpisodes = 0;
                        for (int i = 0; i < result.size(); i++) {
                            VideoType.DetailsSeason season = result.get(i);
                            seasonsValuesBatch[i] = SyncUtils.contentValuesFromSeason(hostId, season);

                            totalWatchedEpisodes += season.watchedepisodes;
                        }
                        // Insert the seasons
                        contentResolver.bulkInsert(MediaContract.Seasons.CONTENT_URI, seasonsValuesBatch);

                        if (getSyncType().equals(SYNC_SINGLE_TVSHOW)) {
                            // HACK: Update watched episodes count for the tvshow with the sum
                            // of watched episodes from seasons, given that the value that we
                            // got from XBMC from the call to GetTVShowDetails is wrong (note
                            // that the value returned from GetTVShows is correct).
                            Uri uri = MediaContract.TVShows.buildTVShowUri(hostId, tvShow.tvshowid);
                            ContentValues tvshowUpdate = new ContentValues(1);
                            tvshowUpdate.put(MediaContract.TVShowsColumns.WATCHEDEPISODES, totalWatchedEpisodes);
                            contentResolver.update(uri, tvshowUpdate, null, null);
                        }

                        // Sync the next tv show
                        chainSyncSeasons(orchestrator, hostConnection, callbackHandler,
                                contentResolver, tvShows, position + 1);
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        // Ok, something bad happend, just quit
                        orchestrator.syncItemFailed(errorCode, description);
                    }
                }, callbackHandler);
            } else {
                // We've processed all tvshows, start episode syncing
                chainSyncEpisodes(orchestrator, hostConnection, callbackHandler,
                        contentResolver, tvShows, 0);
            }
        }

        private final static String getEpisodesProperties[] = {
                VideoType.FieldsEpisode.TITLE, VideoType.FieldsEpisode.PLOT,
                //VideoType.FieldsEpisode.VOTES,
                VideoType.FieldsEpisode.RATING,
                VideoType.FieldsEpisode.WRITER, VideoType.FieldsEpisode.FIRSTAIRED,
                VideoType.FieldsEpisode.PLAYCOUNT, VideoType.FieldsEpisode.RUNTIME,
                VideoType.FieldsEpisode.DIRECTOR,
                //VideoType.FieldsEpisode.PRODUCTIONCODE,
                VideoType.FieldsEpisode.SEASON,
                VideoType.FieldsEpisode.EPISODE,
                //VideoType.FieldsEpisode.ORIGINALTITLE,
                VideoType.FieldsEpisode.SHOWTITLE,
                //VideoType.FieldsEpisode.CAST,
                VideoType.FieldsEpisode.STREAMDETAILS,
                //VideoType.FieldsEpisode.LASTPLAYED,
                VideoType.FieldsEpisode.FANART,  VideoType.FieldsEpisode.THUMBNAIL,
                VideoType.FieldsEpisode.FILE,
                //VideoType.FieldsEpisode.RESUME,
                VideoType.FieldsEpisode.TVSHOWID, VideoType.FieldsEpisode.DATEADDED,
                //VideoType.FieldsEpisode.UNIQUEID, VideoType.FieldsEpisode.ART
        };

        /**
         * Sequentially syncs episodes for the tvshow specified, and on success recursively calls
         * itself to sync the next tvshow on the list.
         * This basically iterates through the tvshows list updating the episodes,
         * in a sequential manner (defeating the parallel nature of host calls)
         *
         * @param orchestrator Orchestrator to call when finished
         * @param hostConnection Host connection to use
         * @param callbackHandler Handler on which to post callbacks
         * @param contentResolver Content resolver
         * @param tvShows TV shows list to get episodes to
         * @param position Position of the tvshow on the list to process
         */
        private void chainSyncEpisodes(final SyncOrchestrator orchestrator,
                                       final HostConnection hostConnection,
                                       final Handler callbackHandler,
                                       final ContentResolver contentResolver,
                                       final List<VideoType.DetailsTVShow> tvShows,
                                       final int position) {
            if (position < tvShows.size()) {
                VideoType.DetailsTVShow tvShow = tvShows.get(position);

                VideoLibrary.GetEpisodes action = new VideoLibrary.GetEpisodes(tvShow.tvshowid, getEpisodesProperties);
                action.execute(hostConnection, new ApiCallback<List<VideoType.DetailsEpisode>>() {
                    @Override
                    public void onSucess(List<VideoType.DetailsEpisode> result) {
                        ContentValues episodesValuesBatch[] = new ContentValues[result.size()];
                        for (int i = 0; i < result.size(); i++) {
                            VideoType.DetailsEpisode episode = result.get(i);
                            episodesValuesBatch[i] = SyncUtils.contentValuesFromEpisode(hostId, episode);
                        }
                        // Insert the episodes
                        contentResolver.bulkInsert(MediaContract.Episodes.CONTENT_URI, episodesValuesBatch);

                        chainSyncEpisodes(orchestrator, hostConnection, callbackHandler,
                                contentResolver, tvShows, position + 1);
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        // Ok, something bad happend, just quit
                        orchestrator.syncItemFailed(errorCode, description);
                    }
                }, callbackHandler);
            } else {
                // We're finished
                orchestrator.syncItemFinished();
            }
        }
    }

    /**
     * Syncs all the music on XBMC to the local database
     */
    private static class SyncMusic implements SyncItem {
        private final int hostId;
        private final Bundle syncExtras;

        /**
         * Syncs all the music on selected XBMC to the local database
         * @param hostId XBMC host id
         */
        public SyncMusic(final int hostId, Bundle syncExtras) {
            this.hostId = hostId;
            this.syncExtras = syncExtras;
        }

        /** {@inheritDoc} */
        public String getDescription() {
            return "Sync music for host: " + hostId;
        }

        /** {@inheritDoc} */
        public String getSyncType() { return SYNC_ALL_MUSIC; }

        /** {@inheritDoc} */
        public Bundle getSyncExtras() {
            return syncExtras;
        }

        /** {@inheritDoc} */
        public void sync(final SyncOrchestrator orchestrator,
                         final HostConnection hostConnection,
                         final Handler callbackHandler,
                         final ContentResolver contentResolver) {
            chainCallSyncArtists(orchestrator, hostConnection, callbackHandler, contentResolver, 0);
        }

        private final static String getArtistsProperties[] = {
                // AudioType.FieldsArtists.INSTRUMENT, AudioType.FieldsArtists.STYLE,
                // AudioType.FieldsArtists.MOOD, AudioType.FieldsArtists.BORN,
                // AudioType.FieldsArtists.FORMED,
                AudioType.FieldsArtists.DESCRIPTION,
                AudioType.FieldsArtists.GENRE,
                // AudioType.FieldsArtists.DIED,
                // AudioType.FieldsArtists.DISBANDED, AudioType.FieldsArtists.YEARSACTIVE,
                //AudioType.FieldsArtists.MUSICBRAINZARTISTID,
                AudioType.FieldsArtists.FANART,
                AudioType.FieldsArtists.THUMBNAIL
        };
        /**
         * Gets all artists recursively and forwards the call to Genres
         * Genres->Albums->Songs
         */
        public void chainCallSyncArtists(final SyncOrchestrator orchestrator,
                                         final HostConnection hostConnection,
                                         final Handler callbackHandler,
                                         final ContentResolver contentResolver,
                                         final int startIdx) {
            // Artists->Genres->Albums->Songs
            // Only gets album artists (first parameter)
            ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_ARTISTS);
            AudioLibrary.GetArtists action = new AudioLibrary.GetArtists(limits, true, getArtistsProperties);
            action.execute(hostConnection, new ApiCallback<List<AudioType.DetailsArtist>>() {
                @Override
                public void onSucess(List<AudioType.DetailsArtist> result) {
                    if (result == null) result = new ArrayList<>(0); // Safeguard
                    // First delete all music info
                    if (startIdx == 0) deleteMusicInfo(contentResolver, hostId);

                    // Insert artists
                    ContentValues artistValuesBatch[] = new ContentValues[result.size()];
                    for (int i = 0; i < result.size(); i++) {
                        AudioType.DetailsArtist artist = result.get(i);
                        artistValuesBatch[i] = SyncUtils.contentValuesFromArtist(hostId, artist);
                    }
                    contentResolver.bulkInsert(MediaContract.Artists.CONTENT_URI, artistValuesBatch);

                    if (result.size() == LIMIT_SYNC_ARTISTS) {
                        // Max limit returned, there may be some more
                        LogUtils.LOGD(TAG, "chainCallSyncArtists: More results on media center, recursing.");
                        result = null; // Help the GC?
                        chainCallSyncArtists(orchestrator, hostConnection, callbackHandler, contentResolver,
                                startIdx + LIMIT_SYNC_ARTISTS);
                    } else {
                        // Ok, we have all the artists, proceed
                        LogUtils.LOGD(TAG, "chainCallSyncArtists: Got all results, continuing");
                        chainCallSyncGenres(orchestrator, hostConnection, callbackHandler, contentResolver);
                    }
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

        private void deleteMusicInfo(final ContentResolver contentResolver,
                                     int hostId) {
            // Delete music info
            String where = MediaContract.Artists.HOST_ID + "=?";
            contentResolver.delete(MediaContract.AlbumArtists.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
            contentResolver.delete(MediaContract.AlbumGenres.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
            contentResolver.delete(MediaContract.Songs.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
            contentResolver.delete(MediaContract.AudioGenres.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
            contentResolver.delete(MediaContract.Albums.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
            contentResolver.delete(MediaContract.Artists.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
        }


        private final static String getGenresProperties[] = {
                LibraryType.FieldsGenre.TITLE, LibraryType.FieldsGenre.THUMBNAIL
        };
        /**
         * Syncs Audio genres and forwards calls to sync albums:
         * Genres->Albums->Songs
         */
        private void chainCallSyncGenres(final SyncOrchestrator orchestrator,
                                         final HostConnection hostConnection,
                                         final Handler callbackHandler,
                                         final ContentResolver contentResolver) {
            // Genres->Albums->Songs
            AudioLibrary.GetGenres action = new AudioLibrary.GetGenres(getGenresProperties);
            action.execute(hostConnection, new ApiCallback<List<LibraryType.DetailsGenre>>() {
                @Override
                public void onSucess(List<LibraryType.DetailsGenre> result) {
                    if (result == null) result = new ArrayList<>(0); // Safeguard
                    ContentValues genresValuesBatch[] = new ContentValues[result.size()];

                    for (int i = 0; i < result.size(); i++) {
                        LibraryType.DetailsGenre genre = result.get(i);
                        genresValuesBatch[i] = SyncUtils.contentValuesFromAudioGenre(hostId, genre);
                    }

                    // Insert the genres and proceed to albums
                    contentResolver.bulkInsert(MediaContract.AudioGenres.CONTENT_URI, genresValuesBatch);
                    chainCallSyncAlbums(orchestrator, hostConnection, callbackHandler, contentResolver, 0);
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

        private static final String getAlbumsProperties[] = {
                AudioType.FieldsAlbum.TITLE, AudioType.FieldsAlbum.DESCRIPTION,
                AudioType.FieldsAlbum.ARTIST, AudioType.FieldsAlbum.GENRE,
                //AudioType.FieldsAlbum.THEME, AudioType.FieldsAlbum.MOOD,
                //AudioType.FieldsAlbum.STYLE, AudioType.FieldsAlbum.TYPE,
                AudioType.FieldsAlbum.ALBUMLABEL, AudioType.FieldsAlbum.RATING,
                AudioType.FieldsAlbum.YEAR,
                //AudioType.FieldsAlbum.MUSICBRAINZALBUMID,
                //AudioType.FieldsAlbum.MUSICBRAINZALBUMARTISTID,
                AudioType.FieldsAlbum.FANART, AudioType.FieldsAlbum.THUMBNAIL,
                AudioType.FieldsAlbum.PLAYCOUNT, AudioType.FieldsAlbum.GENREID,
                AudioType.FieldsAlbum.ARTISTID, AudioType.FieldsAlbum.DISPLAYARTIST
        };

        /**
         * Syncs Albums recursively and forwards calls to sync songs:
         * Albums->Songs
         */
        private void chainCallSyncAlbums(final SyncOrchestrator orchestrator,
                                         final HostConnection hostConnection,
                                         final Handler callbackHandler,
                                         final ContentResolver contentResolver,
                                         final int startIdx) {
            final long albumSyncStartTime = System.currentTimeMillis();
            // Albums->Songs
            ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_ALBUMS);
            AudioLibrary.GetAlbums action = new AudioLibrary.GetAlbums(limits, getAlbumsProperties);
            action.execute(hostConnection, new ApiCallback<List<AudioType.DetailsAlbum>>() {
                @Override
                public void onSucess(List<AudioType.DetailsAlbum> result) {
                    if (result == null) result = new ArrayList<>(0); // Safeguard
                    // Insert the partial results
                    ContentValues albumValuesBatch[] = new ContentValues[result.size()];
                    int artistsCount = 0, genresCount = 0;
                    for (int i = 0; i < result.size(); i++) {
                        AudioType.DetailsAlbum album = result.get(i);
                        albumValuesBatch[i] = SyncUtils.contentValuesFromAlbum(hostId, album);

                        artistsCount += album.artistid.size();
                        genresCount += album.genreid.size();
                    }
                    contentResolver.bulkInsert(MediaContract.Albums.CONTENT_URI, albumValuesBatch);

                    LogUtils.LOGD(TAG, "Finished inserting albums in: " +
                            (System.currentTimeMillis() - albumSyncStartTime));

                    // Iterate on each album, collect the artists and the genres and insert them
                    ContentValues albumArtistsValuesBatch[] = new ContentValues[artistsCount];
                    ContentValues albumGenresValuesBatch[] = new ContentValues[genresCount];
                    int artistCount = 0, genreCount = 0;
                    for (AudioType.DetailsAlbum album : result) {
                        for (int artistId : album.artistid) {
                            albumArtistsValuesBatch[artistCount] = new ContentValues();
                            albumArtistsValuesBatch[artistCount].put(MediaContract.AlbumArtists.HOST_ID, hostId);
                            albumArtistsValuesBatch[artistCount].put(MediaContract.AlbumArtists.ALBUMID, album.albumid);
                            albumArtistsValuesBatch[artistCount].put(MediaContract.AlbumArtists.ARTISTID, artistId);
                            artistCount++;
                        }

                        for (int genreId : album.genreid) {
                            albumGenresValuesBatch[genreCount] = new ContentValues();
                            albumGenresValuesBatch[genreCount].put(MediaContract.AlbumGenres.HOST_ID, hostId);
                            albumGenresValuesBatch[genreCount].put(MediaContract.AlbumGenres.ALBUMID, album.albumid);
                            albumGenresValuesBatch[genreCount].put(MediaContract.AlbumGenres.GENREID, genreId);
                            genreCount++;
                        }
                    }

                    contentResolver.bulkInsert(MediaContract.AlbumArtists.CONTENT_URI, albumArtistsValuesBatch);
                    contentResolver.bulkInsert(MediaContract.AlbumGenres.CONTENT_URI, albumGenresValuesBatch);

                    LogUtils.LOGD(TAG, "Finished inserting artists and genres in: " +
                            (System.currentTimeMillis() - albumSyncStartTime));

                    if (result.size() == LIMIT_SYNC_ALBUMS) {
                        // Max limit returned, there may be some more
                        LogUtils.LOGD(TAG, "chainCallSyncAlbums: More results on media center, recursing.");
                        result = null; // Help the GC?
                        chainCallSyncAlbums(orchestrator, hostConnection, callbackHandler, contentResolver,
                                startIdx + LIMIT_SYNC_ALBUMS);
                    } else {
                        // Ok, we have all the albums, proceed to songs
                        LogUtils.LOGD(TAG, "chainCallSyncAlbums: Got all results, continuing");
                        chainCallSyncSongs(orchestrator, hostConnection, callbackHandler, contentResolver, 0);
                    }
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

        private static final String getSongsProperties[] = {
                AudioType.FieldsSong.TITLE,
                //AudioType.FieldsSong.ARTIST, AudioType.FieldsSong.ALBUMARTIST, AudioType.FieldsSong.GENRE,
                //AudioType.FieldsSong.YEAR, AudioType.FieldsSong.RATING,
                //AudioType.FieldsSong.ALBUM,
                AudioType.FieldsSong.TRACK, AudioType.FieldsSong.DURATION,
                //AudioType.FieldsSong.COMMENT, AudioType.FieldsSong.LYRICS,
                //AudioType.FieldsSong.MUSICBRAINZTRACKID,
                //AudioType.FieldsSong.MUSICBRAINZARTISTID,
                //AudioType.FieldsSong.MUSICBRAINZALBUMID,
                //AudioType.FieldsSong.MUSICBRAINZALBUMARTISTID,
                //AudioType.FieldsSong.PLAYCOUNT, AudioType.FieldsSong.FANART,
                AudioType.FieldsSong.THUMBNAIL, AudioType.FieldsSong.FILE,
                AudioType.FieldsSong.ALBUMID,
                //AudioType.FieldsSong.LASTPLAYED, AudioType.FieldsSong.DISC,
                //AudioType.FieldsSong.GENREID, AudioType.FieldsSong.ARTISTID,
                //AudioType.FieldsSong.DISPLAYARTIST, AudioType.FieldsSong.ALBUMARTISTID
        };

        /**
         * Syncs songs and stops
         */
        private void chainCallSyncSongs(final SyncOrchestrator orchestrator,
                                         final HostConnection hostConnection,
                                         final Handler callbackHandler,
                                         final ContentResolver contentResolver,
                                         final int startIdx) {
            // Songs
            ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_SONGS);
            AudioLibrary.GetSongs action = new AudioLibrary.GetSongs(limits, getSongsProperties);
            action.execute(hostConnection, new ApiCallback<List<AudioType.DetailsSong>>() {
                @Override
                public void onSucess(List<AudioType.DetailsSong> result) {
                    if (result == null) result = new ArrayList<>(0); // Safeguard
                    // Save partial results to DB
                    ContentValues songValuesBatch[] = new ContentValues[result.size()];
                    for (int i = 0; i < result.size(); i++) {
                        AudioType.DetailsSong song = result.get(i);
                        songValuesBatch[i] = SyncUtils.contentValuesFromSong(hostId, song);
                    }
                    contentResolver.bulkInsert(MediaContract.Songs.CONTENT_URI, songValuesBatch);

                    if (result.size() == LIMIT_SYNC_SONGS) {
                        // Max limit returned, there may be some more
                        LogUtils.LOGD(TAG, "chainCallSyncSongs: More results on media center, recursing.");
                        result = null; // Help the GC?
                        chainCallSyncSongs(orchestrator, hostConnection, callbackHandler, contentResolver,
                                startIdx + LIMIT_SYNC_SONGS);
                    } else {
                        // Ok, we have all the songs, insert them
                        LogUtils.LOGD(TAG, "chainCallSyncSongs: Got all results, continuing");
                        orchestrator.syncItemFinished();
                    }
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

    }

    /**
     * Syncs all the music videos on XBMC, to the local database
     */
    private static class SyncMusicVideos implements SyncItem {
        private final int hostId;
        private final Bundle syncExtras;

        /**
         * Syncs all the music videos on XBMC, to the local database
         * @param hostId XBMC host id
         */
        public SyncMusicVideos(final int hostId, Bundle syncExtras) {
            this.hostId = hostId;
            this.syncExtras = syncExtras;
        }

        /** {@inheritDoc} */
        public String getDescription() {
            return "Sync music videos for host: " + hostId;
        }

        /** {@inheritDoc} */
        public String getSyncType() {
            return SYNC_ALL_MUSIC_VIDEOS;
        }

        /** {@inheritDoc} */
        public Bundle getSyncExtras() {
            return syncExtras;
        }

        /** {@inheritDoc} */
        public void sync(final SyncOrchestrator orchestrator,
                         final HostConnection hostConnection,
                         final Handler callbackHandler,
                         final ContentResolver contentResolver) {
            String properties[] = {
                    VideoType.FieldsMusicVideo.TITLE, VideoType.FieldsMusicVideo.PLAYCOUNT,
                    VideoType.FieldsMusicVideo.RUNTIME, VideoType.FieldsMusicVideo.DIRECTOR,
                    VideoType.FieldsMusicVideo.STUDIO, VideoType.FieldsMusicVideo.YEAR,
                    VideoType.FieldsMusicVideo.PLOT, VideoType.FieldsMusicVideo.ALBUM,
                    VideoType.FieldsMusicVideo.ARTIST, VideoType.FieldsMusicVideo.GENRE,
                    VideoType.FieldsMusicVideo.TRACK, VideoType.FieldsMusicVideo.STREAMDETAILS,
                    //VideoType.FieldsMusicVideo.LASTPLAYED,
                    VideoType.FieldsMusicVideo.FANART,
                    VideoType.FieldsMusicVideo.THUMBNAIL, VideoType.FieldsMusicVideo.FILE,
                    // VideoType.FieldsMusicVideo.RESUME, VideoType.FieldsMusicVideo.DATEADDED,
                    VideoType.FieldsMusicVideo.TAG,
                    //VideoType.FieldsMusicVideo.ART
            };

            // Delete and sync all music videos
            VideoLibrary.GetMusicVideos action = new VideoLibrary.GetMusicVideos(properties);
            action.execute(hostConnection, new ApiCallback<List<VideoType.DetailsMusicVideo>>() {
                @Override
                public void onSucess(List<VideoType.DetailsMusicVideo> result) {
                    deleteMusicVideos(contentResolver, hostId);
                    insertMusicVideos(orchestrator, contentResolver, result);
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Ok, something bad happend, just quit
                    orchestrator.syncItemFailed(errorCode, description);
                }
            }, callbackHandler);
        }

        private void deleteMusicVideos(final ContentResolver contentResolver, int hostId) {
            // Delete all music videos
            String where = MediaContract.MusicVideosColumns.HOST_ID + "=?";
            contentResolver.delete(MediaContract.MusicVideos.CONTENT_URI,
                    where, new String[]{String.valueOf(hostId)});
        }

        private void insertMusicVideos(final SyncOrchestrator orchestrator,
                                       final ContentResolver contentResolver,
                                       final List<VideoType.DetailsMusicVideo> musicVideos) {
            ContentValues musicVideosValuesBatch[] = new ContentValues[musicVideos.size()];

            // Iterate on each music video
            for (int i = 0; i < musicVideos.size(); i++) {
                VideoType.DetailsMusicVideo musicVideo = musicVideos.get(i);
                musicVideosValuesBatch[i] = SyncUtils.contentValuesFromMusicVideo(hostId, musicVideo);
            }

            // Insert the movies
            contentResolver.bulkInsert(MediaContract.MusicVideos.CONTENT_URI, musicVideosValuesBatch);
            orchestrator.syncItemFinished();
        }
    }

}
