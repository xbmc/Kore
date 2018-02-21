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
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.type.AudioType;
import org.xbmc.kore.jsonrpc.type.LibraryType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncMusic extends SyncItem {
    public static final String TAG = LogUtils.makeLogTag(SyncMusic.class);

    private static final int LIMIT_SYNC_ARTISTS = 300;
    private static final int LIMIT_SYNC_ALBUMS = 300;
    private static final int LIMIT_SYNC_SONGS = 600;

    private final Bundle syncExtras;

    /**
     * Syncs all the music to the local database
     */
    public SyncMusic(Bundle syncExtras) {
        this.syncExtras = syncExtras;
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "Sync music";
    }

    /** {@inheritDoc} */
    public String getSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

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
    private void chainCallSyncArtists(final SyncOrchestrator orchestrator,
                                     final HostConnection hostConnection,
                                     final Handler callbackHandler,
                                     final ContentResolver contentResolver,
                                     final int startIdx) {
        final int hostId = hostConnection.getHostInfo().getId();

        // Artists->Genres->Albums->Songs
        // Only gets album artists (first parameter)
        ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_ARTISTS);
        AudioLibrary.GetArtists action = new AudioLibrary.GetArtists(limits, true, getArtistsProperties);
        action.execute(hostConnection, new ApiCallback<ApiList<AudioType.DetailsArtist>>() {
            @Override
            public void onSuccess(ApiList<AudioType.DetailsArtist> result) {
                List<AudioType.DetailsArtist> items;
                ListType.LimitsReturned limitsReturned;
                if (result == null) {  // Safeguard
                    items = new ArrayList<>(0);
                    limitsReturned = null;
                } else {
                    items = result.items;
                    limitsReturned = result.limits;
                }

                // First delete all music info
                if (startIdx == 0) deleteMusicInfo(contentResolver, hostId);

                insertArtists(hostId, items, contentResolver);

                if (SyncUtils.moreItemsAvailable(limitsReturned)) {
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
        contentResolver.delete(MediaContract.SongArtists.CONTENT_URI,
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
        final int hostId = hostConnection.getHostInfo().getId();

        // Genres->Albums->Songs
        AudioLibrary.GetGenres action = new AudioLibrary.GetGenres(getGenresProperties);
        action.execute(hostConnection, new ApiCallback<List<LibraryType.DetailsGenre>>() {
            @Override
            public void onSuccess(List<LibraryType.DetailsGenre> result) {
                if (result != null)
                    insertGenresItems(hostId, result, contentResolver);

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
            AudioType.FieldsAlbum.PLAYCOUNT, // AudioType.FieldsAlbum.GENREID,
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
        final int hostId = hostConnection.getHostInfo().getId();
        final long albumSyncStartTime = System.currentTimeMillis();
        // Albums->Songs
        ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_ALBUMS);

        AudioLibrary.GetAlbums action = new AudioLibrary.GetAlbums(limits, getAlbumsProperties);

        action.execute(hostConnection, new ApiCallback<ApiList<AudioType.DetailsAlbum>>() {
            @Override
            public void onSuccess(ApiList<AudioType.DetailsAlbum> result) {
                List<AudioType.DetailsAlbum> items;
                ListType.LimitsReturned limitsReturned;
                if (result == null) {  // Safeguard
                    items = new ArrayList<>(0);
                    limitsReturned = null;
                } else {
                    items = result.items;
                    limitsReturned = result.limits;
                }

                // Insert the partial results
                insertAlbumsItems(hostId, items, contentResolver);

                LogUtils.LOGD(TAG, "Finished inserting artists and genres in: " +
                                   (System.currentTimeMillis() - albumSyncStartTime));

                if (SyncUtils.moreItemsAvailable(limitsReturned)) {
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
            //AudioType.FieldsSong.LASTPLAYED,
            AudioType.FieldsSong.DISC,
            AudioType.FieldsSong.GENREID,
            AudioType.FieldsSong.ARTISTID,
//            AudioType.FieldsSong.ALBUMARTISTID,
            AudioType.FieldsSong.DISPLAYARTIST,
    };

    /**
     * Syncs songs and stops
     */
    private void chainCallSyncSongs(final SyncOrchestrator orchestrator,
                                    final HostConnection hostConnection,
                                    final Handler callbackHandler,
                                    final ContentResolver contentResolver,
                                    final int startIdx) {
        final int hostId = hostConnection.getHostInfo().getId();
        // Songs
        ListType.Limits limits = new ListType.Limits(startIdx, startIdx + LIMIT_SYNC_SONGS);
        AudioLibrary.GetSongs action = new AudioLibrary.GetSongs(limits, getSongsProperties);
        action.execute(hostConnection, new ApiCallback<ApiList<AudioType.DetailsSong>>() {
            @Override
            public void onSuccess(ApiList<AudioType.DetailsSong> result) {
                List<AudioType.DetailsSong> items;
                ListType.LimitsReturned limitsReturned;
                if (result == null) {  // Safeguard
                    items = new ArrayList<>(0);
                    limitsReturned = null;
                } else {
                    items = result.items;
                    limitsReturned = result.limits;
                }

                // Save partial results to DB
                insertSongsItems(hostId, items, contentResolver);

                if (SyncUtils.moreItemsAvailable(limitsReturned)) {
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
                // Ok, something bad happened, just quit
                orchestrator.syncItemFailed(errorCode, description);
            }
        }, callbackHandler);
    }

    public void insertArtists(int hostId, List<AudioType.DetailsArtist> items, ContentResolver contentResolver) {
        ContentValues artistValuesBatch[] = new ContentValues[items.size()];
        for (int i = 0; i < items.size(); i++) {
            AudioType.DetailsArtist artist = items.get(i);
            artistValuesBatch[i] = SyncUtils.contentValuesFromArtist(hostId, artist);
        }
        contentResolver.bulkInsert(MediaContract.Artists.CONTENT_URI, artistValuesBatch);
    }

    public void insertGenresItems(int hostId, List<LibraryType.DetailsGenre> items, ContentResolver contentResolver) {
        ContentValues genresValuesBatch[] = new ContentValues[items.size()];

        for (int i = 0; i < items.size(); i++) {
            LibraryType.DetailsGenre genre = items.get(i);
            genresValuesBatch[i] = SyncUtils.contentValuesFromAudioGenre(hostId, genre);
        }

        // Insert the genres and proceed to albums
        contentResolver.bulkInsert(MediaContract.AudioGenres.CONTENT_URI, genresValuesBatch);
    }

    public void insertAlbumsItems(int hostId, List<AudioType.DetailsAlbum> items, ContentResolver contentResolver) {
        ContentValues albumValuesBatch[] = new ContentValues[items.size()];
        int artistsCount = 0;
        for (int i = 0; i < items.size(); i++) {
            AudioType.DetailsAlbum album = items.get(i);
            albumValuesBatch[i] = SyncUtils.contentValuesFromAlbum(hostId, album);

            artistsCount += album.artistid.size();
        }
        contentResolver.bulkInsert(MediaContract.Albums.CONTENT_URI, albumValuesBatch);

        // Iterate on each album, collect the artists and insert them
        ContentValues albumArtistsValuesBatch[] = new ContentValues[artistsCount];
        int artistCount = 0;
        for (AudioType.DetailsAlbum album : items) {
            for (int artistId : album.artistid) {
                albumArtistsValuesBatch[artistCount] = new ContentValues();
                albumArtistsValuesBatch[artistCount].put(MediaContract.AlbumArtists.HOST_ID, hostId);
                albumArtistsValuesBatch[artistCount].put(MediaContract.AlbumArtists.ALBUMID, album.albumid);
                albumArtistsValuesBatch[artistCount].put(MediaContract.AlbumArtists.ARTISTID, artistId);
                artistCount++;
            }
        }

        contentResolver.bulkInsert(MediaContract.AlbumArtists.CONTENT_URI, albumArtistsValuesBatch);
    }

    public void insertSongsItems(int hostId, List<AudioType.DetailsSong> items, ContentResolver contentResolver) {
        ContentValues songValuesBatch[] = new ContentValues[items.size()];
        int totalArtistsCount = 0, totalGenresCount = 0;
        for (int i = 0; i < items.size(); i++) {
            AudioType.DetailsSong song = items.get(i);
            songValuesBatch[i] = SyncUtils.contentValuesFromSong(hostId, song);

            totalArtistsCount += song.artistid.size();
            totalGenresCount += song.genreid.size();
        }
        contentResolver.bulkInsert(MediaContract.Songs.CONTENT_URI, songValuesBatch);

        // Iterate on each song, collect the artists and the genres and insert them
        ContentValues songArtistsValuesBatch[] = new ContentValues[totalArtistsCount];
        ContentValues songGenresValuesBatch[] = new ContentValues[totalGenresCount];
        int artistCount = 0, genreCount = 0;
        for (AudioType.DetailsSong song : items) {
            for (int artistId : song.artistid) {
                songArtistsValuesBatch[artistCount] = new ContentValues();
                songArtistsValuesBatch[artistCount].put(MediaContract.SongArtists.HOST_ID, hostId);
                songArtistsValuesBatch[artistCount].put(MediaContract.SongArtists.SONGID, song.songid);
                songArtistsValuesBatch[artistCount].put(MediaContract.SongArtists.ARTISTID, artistId);
                artistCount++;
            }

             for (int genreId : song.genreid) {
                 songGenresValuesBatch[genreCount] = new ContentValues();
                 songGenresValuesBatch[genreCount].put(MediaContract.AlbumGenres.HOST_ID, hostId);
                 songGenresValuesBatch[genreCount].put(MediaContract.AlbumGenres.ALBUMID, song.albumid);
                 songGenresValuesBatch[genreCount].put(MediaContract.AlbumGenres.GENREID, genreId);
                 genreCount++;
             }
        }

        contentResolver.bulkInsert(MediaContract.SongArtists.CONTENT_URI, songArtistsValuesBatch);
        contentResolver.bulkInsert(MediaContract.AlbumGenres.CONTENT_URI, songGenresValuesBatch);
    }
}
