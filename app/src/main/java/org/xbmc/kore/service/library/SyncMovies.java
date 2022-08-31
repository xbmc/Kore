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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;

import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiList;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncMovies extends SyncItem {
    public static final String TAG = LogUtils.makeLogTag(SyncMovies.class);

    private static final int LIMIT_SYNC_MOVIES = 300;

    private final int hostId;
    private final int movieId;
    private final Bundle syncParams;

    /**
     * Syncs all the movies on selected XBMC to the local database
     * @param hostId XBMC host id
     */
    public SyncMovies(final int hostId, Bundle syncParams) {
        this.hostId = hostId;
        this.movieId = -1;
        this.syncParams = syncParams;
    }

    /**
     * Syncs a specific movie on selected XBMC to the local database
     * @param hostId XBMC host id
     */
    public SyncMovies(final int hostId, final int movieId, Bundle syncParams) {
        this.hostId = hostId;
        this.movieId = movieId;
        this.syncParams = syncParams;
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return (movieId != -1) ?
               "Sync movies for host: " + hostId :
               "Sync movie " + movieId + " for host: " + hostId;
    }

    /** {@inheritDoc} */
    public String getSyncType() {
        return (movieId == -1) ? LibrarySyncService.SYNC_ALL_MOVIES
                               : LibrarySyncService.SYNC_SINGLE_MOVIE;
    }

    /** {@inheritDoc} */
    public Bundle getSyncParams() {
        return syncParams;
    }

    /** {@inheritDoc} */
    public void sync(final SyncOrchestrator orchestrator,
                     final HostConnection hostConnection,
                     final Handler callbackHandler,
                     final ContentResolver contentResolver) {
        String[] properties = {
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
            VideoType.FieldsMovie.VOTES,
            // VideoType.FieldsMovie.FANART, VideoType.FieldsMovie.THUMBNAIL,
            VideoType.FieldsMovie.FILE,
            // VideoType.FieldsMovie.SORTTITLE, VideoType.FieldsMovie.RESUME,
            VideoType.FieldsMovie.SETID,
            // VideoType.FieldsMovie.DATEADDED, VideoType.FieldsMovie.TAG,
            VideoType.FieldsMovie.ART
        };

        if (movieId == -1) {
            syncAllMovies(orchestrator, hostConnection, callbackHandler, contentResolver, properties, 0);
        } else {
            // Sync a specific movie
            VideoLibrary.GetMovieDetails action =
                    new VideoLibrary.GetMovieDetails(movieId, properties);
            action.execute(hostConnection, new ApiCallback<VideoType.DetailsMovie>() {
                @Override
                public void onSuccess(VideoType.DetailsMovie result) {
                    deleteMovies(contentResolver, hostId, movieId);
                    List<VideoType.DetailsMovie> movies = new ArrayList<>(1);
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
                               final String[] properties,
                               final int startIdx) {
        // Call GetMovies with the current limits set
        ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_MOVIES);
        VideoLibrary.GetMovies action = new VideoLibrary.GetMovies(limits, properties);
        action.execute(hostConnection, new ApiCallback<ApiList<VideoType.DetailsMovie>>() {
            @Override
            public void onSuccess(ApiList<VideoType.DetailsMovie> result) {
                ListType.LimitsReturned limitsReturned = null;
                if (result != null) {
                    limitsReturned = result.limits;
                }

                if (startIdx == 0) {
                    // First call, delete movies from DB
                    deleteMovies(contentResolver, hostId, -1);
                }
                if (result != null && !result.items.isEmpty()) {
                    insertMovies(orchestrator, contentResolver, result.items);
                }

                if (SyncUtils.moreItemsAvailable(limitsReturned)) {
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
                // Ok, something bad happened, just quit
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
        ContentValues[] movieValuesBatch = new ContentValues[movies.size()];
        int castCount = 0;

        // Iterate on each movie
        for (int i = 0; i < movies.size(); i++) {
            VideoType.DetailsMovie movie = movies.get(i);
            movieValuesBatch[i] = SyncUtils.contentValuesFromMovie(hostId, movie);
            castCount += movie.cast.size();
        }

        // Insert the movies
        contentResolver.bulkInsert(MediaContract.Movies.CONTENT_URI, movieValuesBatch);

        ContentValues[] movieCastValuesBatch = new ContentValues[castCount];
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
