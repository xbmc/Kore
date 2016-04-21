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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncTVShows extends SyncItem {
    public static final String TAG = LogUtils.makeLogTag(SyncTVShows.class);

    private static final int LIMIT_SYNC_TVSHOWS = 200;

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
        return (tvshowId == -1) ? LibrarySyncService.SYNC_ALL_TVSHOWS
                                : LibrarySyncService.SYNC_SINGLE_TVSHOW;
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
                public void onSuccess(VideoType.DetailsTVShow result) {
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
            public void onSuccess(List<VideoType.DetailsTVShow> result) {
                allResults.addAll(result);
                if (result.size() == LIMIT_SYNC_TVSHOWS) {
                    // Max limit returned, there may be some more movies
                    LogUtils.LOGD(TAG, "syncAllTVShows: More tv shows on media center, recursing.");
                    syncAllTVShows(orchestrator, hostConnection, callbackHandler, contentResolver,
                                   startIdx + LIMIT_SYNC_TVSHOWS, allResults);
                } else {
                    // Ok, we have all the shows, insert them
                    LogUtils.LOGD(TAG, "syncAllTVShows: Got all tv shows. Total: " + allResults.size());
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
            LogUtils.LOGD(TAG, "Deleting all existing tv shows: ");
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
        LogUtils.LOGD(TAG, "Inserted " + tvShows.size() + " tv shows.");

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
                public void onSuccess(List<VideoType.DetailsSeason> result) {
                    ContentValues seasonsValuesBatch[] = new ContentValues[result.size()];
                    int totalWatchedEpisodes = 0;
                    for (int i = 0; i < result.size(); i++) {
                        VideoType.DetailsSeason season = result.get(i);
                        seasonsValuesBatch[i] = SyncUtils.contentValuesFromSeason(hostId, season);

                        totalWatchedEpisodes += season.watchedepisodes;
                    }
                    // Insert the seasons
                    contentResolver.bulkInsert(MediaContract.Seasons.CONTENT_URI, seasonsValuesBatch);

                    if (getSyncType().equals(LibrarySyncService.SYNC_SINGLE_TVSHOW)) {
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
                public void onSuccess(List<VideoType.DetailsEpisode> result) {
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
            LogUtils.LOGD(TAG, "Sync tv shows finished successfully");
            orchestrator.syncItemFinished();
        }
    }
}
