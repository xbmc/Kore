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
package com.syncedsynapse.kore2.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.SelectionBuilder;

import java.util.Arrays;

/**
 * Provider for {@link MediaContract} data.
 */
public class MediaProvider extends ContentProvider {
    private static final String TAG = LogUtils.makeLogTag(MediaProvider.class);

    private MediaDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int HOSTS_LIST = 100;
    private static final int HOSTS_ID = 101;

    private static final int MOVIES_ALL = 200;
    private static final int MOVIES_LIST = 201;
    private static final int MOVIES_ID = 202;

    private static final int MOVIE_CAST_ALL = 210;
    private static final int MOVIE_CAST_LIST = 211;

    private static final int TVSHOWS_ALL = 300;
    private static final int TVSHOWS_LIST = 302;
    private static final int TVSHOWS_ID = 303;

    private static final int TVSHOWS_CAST_ALL = 310;
    private static final int TVSHOWS_CAST_LIST = 311;

    private static final int SEASONS_ALL = 400;
    private static final int TVSHOW_SEASONS_LIST = 401;
    private static final int TVSHOW_SEASONS_ID = 402;

    private static final int EPISODES_ALL = 500;
    private static final int TVSHOW_EPISODES_LIST = 501;
    private static final int TVSHOW_EPISODES_ID = 502;
    private static final int TVSHOW_SEASON_EPISODES_LIST = 503;
    private static final int TVSHOW_SEASON_EPISODES_ID = 504;

    private static final int ARTISTS_ALL = 600;
    private static final int ARTISTS_LIST = 601;
    private static final int ARTISTS_ID = 602;
    private static final int ARTIST_ALBUMS_LIST = 610;

    private static final int ALBUMS_ALL = 700;
    private static final int ALBUMS_LIST = 701;
    private static final int ALBUMS_ID = 702;
    private static final int ALBUM_ARTISTS_LIST = 710;
    private static final int ALBUM_GENRES_LIST = 711;

    private static final int SONGS_ALL = 800;
    private static final int SONGS_LIST = 802;
    private static final int SONGS_ID = 803;

    private static final int AUDIO_GENRES_ALL = 900;
    private static final int AUDIO_GENRES_LIST = 901;
    private static final int AUDIO_GENRES_ID = 902;
    private static final int AUDIO_GENRE_ALBUMS_LIST = 910;

    private static final int ALBUM_ARTISTS_ALL = 1000;
    private static final int ALBUM_GENRES_ALL = 1001;

    private static final int MUSIC_VIDEOS_ALL = 1100;
    private static final int MUSIC_VIDEOS_LIST = 1101;
    private static final int MUSIC_VIDEOS_ID = 1102;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MediaContract.CONTENT_AUTHORITY;

        // Hosts
        matcher.addURI(authority, MediaContract.PATH_HOSTS, HOSTS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*", HOSTS_ID);

        // Movies and cast
        matcher.addURI(authority, MediaContract.PATH_MOVIES, MOVIES_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_MOVIES, MOVIES_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_MOVIES + "/*", MOVIES_ID);

        matcher.addURI(authority, MediaContract.PATH_MOVIE_CAST, MOVIE_CAST_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_MOVIES + "/*/" +
                                  MediaContract.PATH_MOVIE_CAST, MOVIE_CAST_LIST);

        // TV Shows and cast
        matcher.addURI(authority, MediaContract.PATH_TVSHOWS, TVSHOWS_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS, TVSHOWS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*", TVSHOWS_ID);

        matcher.addURI(authority, MediaContract.PATH_TVSHOW_CAST, TVSHOWS_CAST_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_TVSHOW_CAST, TVSHOWS_CAST_LIST);

        // Seasons
        matcher.addURI(authority, MediaContract.PATH_SEASONS, SEASONS_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_SEASONS, TVSHOW_SEASONS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_SEASONS + "/*", TVSHOW_SEASONS_ID);

        // Episodes
        matcher.addURI(authority, MediaContract.PATH_EPISODES, EPISODES_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_EPISODES, TVSHOW_EPISODES_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_EPISODES + "/*", TVSHOW_EPISODES_ID);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_SEASONS + "/*/" +
                                  MediaContract.PATH_EPISODES, TVSHOW_SEASON_EPISODES_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_TVSHOWS + "/*/" +
                                  MediaContract.PATH_SEASONS + "/*/" +
                                  MediaContract.PATH_EPISODES + "/*", TVSHOW_SEASON_EPISODES_ID);

        // Artists
        matcher.addURI(authority, MediaContract.PATH_ARTISTS, ARTISTS_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ARTISTS, ARTISTS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ARTISTS + "/*", ARTISTS_ID);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ARTISTS + "/*/" +
                                  MediaContract.PATH_ALBUMS, ARTIST_ALBUMS_LIST);

        // Albums
        matcher.addURI(authority, MediaContract.PATH_ALBUMS, ALBUMS_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ALBUMS, ALBUMS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ALBUMS + "/*", ALBUMS_ID);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ALBUMS + "/*/" +
                                  MediaContract.PATH_ARTISTS, ALBUM_ARTISTS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ALBUMS + "/*/" +
                                  MediaContract.PATH_AUDIO_GENRES, ALBUM_GENRES_LIST);

        // Songs
        matcher.addURI(authority, MediaContract.PATH_SONGS, SONGS_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ALBUMS + "/*/" +
                                  MediaContract.PATH_SONGS, SONGS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_ALBUMS + "/*/" +
                                  MediaContract.PATH_SONGS + "/*", SONGS_ID);

        // Genres
        matcher.addURI(authority, MediaContract.PATH_AUDIO_GENRES, AUDIO_GENRES_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_AUDIO_GENRES, AUDIO_GENRES_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_AUDIO_GENRES + "/*", AUDIO_GENRES_ID);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_AUDIO_GENRES + "/*/" +
                                  MediaContract.PATH_ALBUMS, AUDIO_GENRE_ALBUMS_LIST);

        // AlbumArtists
        matcher.addURI(authority, MediaContract.PATH_ALBUM_ARTISTS, ALBUM_ARTISTS_ALL);
        // AlbumGenres
        matcher.addURI(authority, MediaContract.PATH_ALBUM_GENRES, ALBUM_GENRES_ALL);

        // Music Videos
        matcher.addURI(authority, MediaContract.PATH_MUSIC_VIDEOS, MUSIC_VIDEOS_ALL);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_MUSIC_VIDEOS, MUSIC_VIDEOS_LIST);
        matcher.addURI(authority, MediaContract.PATH_HOSTS + "/*/" +
                                  MediaContract.PATH_MUSIC_VIDEOS + "/*", MUSIC_VIDEOS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MediaDatabase(getContext());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case HOSTS_LIST:
                return MediaContract.Hosts.CONTENT_TYPE;
            case HOSTS_ID:
                return MediaContract.Hosts.CONTENT_ITEM_TYPE;
            case MOVIES_ALL:
            case MOVIES_LIST:
                return MediaContract.Movies.CONTENT_TYPE;
            case MOVIES_ID:
                return MediaContract.Movies.CONTENT_ITEM_TYPE;
            case MOVIE_CAST_ALL:
            case MOVIE_CAST_LIST:
                return MediaContract.MovieCast.CONTENT_TYPE;
            case TVSHOWS_ALL:
            case TVSHOWS_LIST:
                return MediaContract.TVShows.CONTENT_TYPE;
            case TVSHOWS_ID:
                return MediaContract.TVShows.CONTENT_ITEM_TYPE;
            case TVSHOWS_CAST_ALL:
            case TVSHOWS_CAST_LIST:
                return MediaContract.TVShowCast.CONTENT_TYPE;
            case SEASONS_ALL:
            case TVSHOW_SEASONS_LIST:
                return MediaContract.Seasons.CONTENT_TYPE;
            case TVSHOW_SEASONS_ID:
                return MediaContract.Seasons.CONTENT_ITEM_TYPE;
            case EPISODES_ALL:
            case TVSHOW_EPISODES_LIST:
            case TVSHOW_SEASON_EPISODES_LIST:
                return MediaContract.Episodes.CONTENT_TYPE;
            case TVSHOW_EPISODES_ID:
            case TVSHOW_SEASON_EPISODES_ID:
                return MediaContract.Episodes.CONTENT_ITEM_TYPE;
            case ARTISTS_ALL:
            case ARTISTS_LIST:
            case ALBUM_ARTISTS_LIST:
                return MediaContract.Artists.CONTENT_TYPE;
            case ARTISTS_ID:
                return MediaContract.Artists.CONTENT_ITEM_TYPE;
            case ALBUMS_ALL:
            case ALBUMS_LIST:
            case ARTIST_ALBUMS_LIST:
            case AUDIO_GENRE_ALBUMS_LIST:
                return MediaContract.Albums.CONTENT_TYPE;
            case ALBUMS_ID:
                return MediaContract.Albums.CONTENT_ITEM_TYPE;
            case SONGS_ALL:
            case SONGS_LIST:
                return MediaContract.Songs.CONTENT_TYPE;
            case SONGS_ID:
                return MediaContract.Songs.CONTENT_ITEM_TYPE;
            case AUDIO_GENRES_ALL:
            case AUDIO_GENRES_LIST:
            case ALBUM_GENRES_LIST:
                return MediaContract.AudioGenres.CONTENT_TYPE;
            case AUDIO_GENRES_ID:
                return MediaContract.AudioGenres.CONTENT_ITEM_TYPE;
            case ALBUM_ARTISTS_ALL:
                return MediaContract.AlbumArtists.CONTENT_TYPE;
            case ALBUM_GENRES_ALL:
                return MediaContract.AlbumGenres.CONTENT_TYPE;
            case MUSIC_VIDEOS_ALL:
            case MUSIC_VIDEOS_LIST:
                return MediaContract.MusicVideos.CONTENT_TYPE;
            case MUSIC_VIDEOS_ID:
                return MediaContract.MusicVideos.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        LogUtils.LOGV(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        Cursor cursor;
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildQuerySelection(uri, match);

                cursor = builder.where(selection, selectionArgs)
                                .query(db, projection, sortOrder);
            }
        }
        return cursor;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LogUtils.LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri insertedUri;
        switch (match) {
            case HOSTS_LIST: {
                values.put(MediaContract.SyncColumns.UPDATED, System.currentTimeMillis());
                long hostId = db.insertOrThrow(MediaDatabase.Tables.HOSTS, null, values);
                insertedUri = MediaContract.Hosts.buildHostUri(hostId);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unsuported uri: " + uri);
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return insertedUri;
    }

    /** {@inheritDoc} */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        long startTime = System.currentTimeMillis();
        final int match = sUriMatcher.match(uri);

        String table;
        switch (match) {
            case MOVIES_ALL: {
                table = MediaDatabase.Tables.MOVIES;
                break;
            }
            case MOVIE_CAST_ALL: {
                table = MediaDatabase.Tables.MOVIE_CAST;
                break;
            }
            case TVSHOWS_ALL: {
                table = MediaDatabase.Tables.TVSHOWS;
                break;
            }
            case TVSHOWS_CAST_ALL: {
                table = MediaDatabase.Tables.TVSHOWS_CAST;
                break;
            }
            case SEASONS_ALL: {
                table = MediaDatabase.Tables.SEASONS;
                break;
            }
            case EPISODES_ALL: {
                table = MediaDatabase.Tables.EPISODES;
                break;
            }
            case ARTISTS_ALL: {
                table = MediaDatabase.Tables.ARTISTS;
                break;
            }
            case ALBUMS_ALL: {
                table = MediaDatabase.Tables.ALBUMS;
                break;
            }
            case SONGS_ALL: {
                table = MediaDatabase.Tables.SONGS;
                break;
            }
            case AUDIO_GENRES_ALL: {
                table = MediaDatabase.Tables.AUDIO_GENRES;
                break;
            }
            case ALBUM_GENRES_ALL: {
                table = MediaDatabase.Tables.ALBUM_GENRES;
                break;
            }
            case ALBUM_ARTISTS_ALL: {
                table = MediaDatabase.Tables.ALBUM_ARTISTS;
                break;
            }
            case MUSIC_VIDEOS_ALL: {
                table = MediaDatabase.Tables.MUSIC_VIDEOS;
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();

        long updateTime = System.currentTimeMillis();
        try {
            for (ContentValues value : values) {
                switch (match) {
                    case ALBUM_GENRES_ALL:
                    case ALBUM_ARTISTS_ALL:
                        // Nothing to add to these tables
                        break;
                    default:
                        value.put(MediaContract.SyncColumns.UPDATED, updateTime);
                        break;
                }
                db.insertOrThrow(table, null, value);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LogUtils.LOGD(TAG, "Couldn't bulk insert records. Exception: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);

        LogUtils.LOGD(TAG, "Bulk insert finished for uri (" + uri +
                           ") in (ms): " + (System.currentTimeMillis() - startTime));
        return values.length;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LogUtils.LOGD(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case HOSTS_ID:
            case MOVIES_ID:
            case TVSHOWS_ID:
            case TVSHOW_SEASONS_ID:
            case TVSHOW_EPISODES_ID:
            case ARTISTS_ID:
            case ALBUMS_ID:
            case SONGS_ID:
            case AUDIO_GENRES_ID:
            case MUSIC_VIDEOS_ID: {
                // Add updated field
                values.put(MediaContract.SyncColumns.UPDATED, System.currentTimeMillis());
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for update: " + uri);
            }
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildQuerySelection(uri, match);
        int result = builder.where(selection, selectionArgs)
                            .update(db, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildQuerySelection(uri, match);
        int result = builder.where(selection, selectionArgs)
                            .delete(db);
        LogUtils.LOGD(TAG, "delete(uri=" + uri + "). Rows affected: " + result);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildUpdateDeleteSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case HOSTS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.HOSTS)
                              .where(BaseColumns._ID + "=?", hostId);
            }
            default: {
                throw new UnsupportedOperationException("Unsupported uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildQuerySelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();

        switch (match) {
            case HOSTS_LIST: {
                return builder.table(MediaDatabase.Tables.HOSTS);
            }
            case HOSTS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.HOSTS)
                              .where(BaseColumns._ID + "=?", hostId);
            }
            case MOVIES_ALL: {
                return builder.table(MediaDatabase.Tables.MOVIES);
            }
            case MOVIES_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.MOVIES)
                              .where(MediaContract.Movies.HOST_ID + "=?", hostId);
            }
            case MOVIES_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String movieId = MediaContract.Movies.getMovieId(uri);
                return builder.table(MediaDatabase.Tables.MOVIES)
                              .where(MediaContract.Movies.HOST_ID + "=?", hostId)
                              .where(MediaContract.Movies.MOVIEID + "=?", movieId);
            }
            case MOVIE_CAST_ALL: {
                return builder.table(MediaDatabase.Tables.MOVIE_CAST);
            }
            case MOVIE_CAST_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String movieId = MediaContract.Movies.getMovieId(uri);
                return builder.table(MediaDatabase.Tables.MOVIE_CAST)
                              .where(MediaContract.MovieCast.HOST_ID + "=?", hostId)
                              .where(MediaContract.MovieCast.MOVIEID + "=?", movieId);
            }
            case TVSHOWS_ALL: {
                return builder.table(MediaDatabase.Tables.TVSHOWS);
            }
            case TVSHOWS_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.TVSHOWS)
                              .where(MediaContract.TVShows.HOST_ID + "=?", hostId);
            }
            case TVSHOWS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                return builder.table(MediaDatabase.Tables.TVSHOWS)
                              .where(MediaContract.TVShows.HOST_ID + "=?", hostId)
                              .where(MediaContract.TVShows.TVSHOWID + "=?", tvshowId);
            }
            case TVSHOWS_CAST_ALL: {
                return builder.table(MediaDatabase.Tables.TVSHOWS_CAST);
            }
            case TVSHOWS_CAST_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                return builder.table(MediaDatabase.Tables.TVSHOWS_CAST)
                              .where(MediaContract.TVShowCast.HOST_ID + "=?", hostId)
                              .where(MediaContract.TVShowCast.TVSHOWID + "=?", tvshowId);
            }
            case SEASONS_ALL: {
                return builder.table(MediaDatabase.Tables.SEASONS);
            }
            case TVSHOW_SEASONS_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                return builder.table(MediaDatabase.Tables.SEASONS)
                              .where(MediaContract.Seasons.HOST_ID + "=?", hostId)
                              .where(MediaContract.Seasons.TVSHOWID + "=?", tvshowId);
            }
            case TVSHOW_SEASONS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                final String season = MediaContract.Seasons.getTVShowSeasonId(uri);
                return builder.table(MediaDatabase.Tables.SEASONS)
                              .where(MediaContract.Seasons.HOST_ID + "=?", hostId)
                              .where(MediaContract.Seasons.TVSHOWID + "=?", tvshowId)
                              .where(MediaContract.Seasons.SEASON + "=?", season);
            }
            case EPISODES_ALL: {
                return builder.table(MediaDatabase.Tables.EPISODES);
            }
            case TVSHOW_EPISODES_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                return builder.table(MediaDatabase.Tables.EPISODES)
                              .where(MediaContract.Episodes.HOST_ID + "=?", hostId)
                              .where(MediaContract.Episodes.TVSHOWID + "=?", tvshowId);
            }
            case TVSHOW_EPISODES_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                final String episodeId = MediaContract.Episodes.getTVShowEpisodeId(uri);
                return builder.table(MediaDatabase.Tables.EPISODES)
                              .where(MediaContract.Episodes.HOST_ID + "=?", hostId)
                              .where(MediaContract.Episodes.TVSHOWID + "=?", tvshowId)
                              .where(MediaContract.Episodes.EPISODEID + "=?", episodeId);
            }
            case TVSHOW_SEASON_EPISODES_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                final String season = MediaContract.Seasons.getTVShowSeasonId(uri);
                return builder.table(MediaDatabase.Tables.EPISODES)
                              .where(MediaContract.Episodes.HOST_ID + "=?", hostId)
                              .where(MediaContract.Episodes.TVSHOWID + "=?", tvshowId)
                              .where(MediaContract.Episodes.SEASON + "=?", season);
            }
            case TVSHOW_SEASON_EPISODES_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String tvshowId = MediaContract.TVShows.getTVShowId(uri);
                final String season = MediaContract.Seasons.getTVShowSeasonId(uri);
                final String episodeId = MediaContract.Episodes.getTVShowSeasonEpisodeId(uri);
                return builder.table(MediaDatabase.Tables.EPISODES)
                              .where(MediaContract.Episodes.HOST_ID + "=?", hostId)
                              .where(MediaContract.Episodes.TVSHOWID + "=?", tvshowId)
                              .where(MediaContract.Episodes.SEASON + "=?", season)
                              .where(MediaContract.Episodes.EPISODEID + "=?", episodeId);
            }
            case ARTISTS_ALL: {
                return builder.table(MediaDatabase.Tables.ARTISTS);
            }
            case ARTISTS_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.ARTISTS)
                              .where(MediaContract.Artists.HOST_ID + "=?", hostId);
            }
            case ARTISTS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String artistId = MediaContract.Artists.getArtistId(uri);
                return builder.table(MediaDatabase.Tables.ARTISTS)
                              .where(MediaContract.Artists.HOST_ID + "=?", hostId)
                              .where(MediaContract.Artists.ARTISTID + "=?", artistId);
            }
            case ALBUMS_ALL: {
                return builder.table(MediaDatabase.Tables.ALBUMS);
            }
            case ALBUMS_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.ALBUMS)
                              .where(MediaContract.Albums.HOST_ID + "=?", hostId);
            }
            case ALBUMS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String albumId = MediaContract.Albums.getAlbumId(uri);
                return builder.table(MediaDatabase.Tables.ALBUMS)
                              .where(MediaContract.Albums.HOST_ID + "=?", hostId)
                              .where(MediaContract.Albums.ALBUMID + "=?", albumId);
            }
            case SONGS_ALL: {
                return builder.table(MediaDatabase.Tables.SONGS);
            }
            case SONGS_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String albumId = MediaContract.Albums.getAlbumId(uri);
                return builder.table(MediaDatabase.Tables.SONGS)
                              .where(MediaContract.Songs.HOST_ID + "=?", hostId)
                              .where(MediaContract.Songs.ALBUMID + "=?", albumId);
            }
            case SONGS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String albumId = MediaContract.Albums.getAlbumId(uri);
                final String songId = MediaContract.Songs.getSongId(uri);
                return builder.table(MediaDatabase.Tables.SONGS)
                              .where(MediaContract.Songs.HOST_ID + "=?", hostId)
                              .where(MediaContract.Songs.ALBUMID + "=?", albumId)
                              .where(MediaContract.Songs.SONGID + "=?", songId);
            }
            case AUDIO_GENRES_ALL: {
                return builder.table(MediaDatabase.Tables.AUDIO_GENRES);
            }
            case AUDIO_GENRES_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.AUDIO_GENRES)
                              .where(MediaContract.AudioGenres.HOST_ID + "=?", hostId);
            }
            case AUDIO_GENRES_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String audioGenreId = MediaContract.AudioGenres.getAudioGenreId(uri);
                return builder.table(MediaDatabase.Tables.AUDIO_GENRES)
                              .where(MediaContract.AudioGenres.HOST_ID + "=?", hostId)
                              .where(MediaContract.AudioGenres.GENREID + "=?", audioGenreId);
            }
            case ALBUM_ARTISTS_ALL: {
                return builder.table(MediaDatabase.Tables.ALBUM_ARTISTS);
            }
            case ALBUM_GENRES_ALL: {
                return builder.table(MediaDatabase.Tables.ALBUM_GENRES);
            }
            case ARTIST_ALBUMS_LIST: {
                // Albums for Artists
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String artistId = MediaContract.Artists.getArtistId(uri);
                return builder.table(MediaDatabase.Tables.ALBUMS_FOR_ARTIST_JOIN)
                              .mapToTable(MediaContract.Albums._ID, MediaDatabase.Tables.ALBUMS)
                              .mapToTable(MediaContract.Albums.HOST_ID, MediaDatabase.Tables.ALBUMS)
                              .mapToTable(MediaContract.Albums.ALBUMID, MediaDatabase.Tables.ALBUMS)
                              .mapToTable(MediaContract.AlbumArtists.ARTISTID, MediaDatabase.Tables.ALBUM_ARTISTS)
                              .where(Qualified.ALBUM_ARTISTS_HOST_ID + "=?", hostId)
                              .where(Qualified.ALBUM_ARTISTS_ARTISTID + "=?", artistId);
            }
            case ALBUM_ARTISTS_LIST: {
                // Artists for Album
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String albumId = MediaContract.Albums.getAlbumId(uri);
                return builder.table(MediaDatabase.Tables.ARTISTS_FOR_ALBUM_JOIN)
                              .mapToTable(MediaContract.Artists._ID, MediaDatabase.Tables.ARTISTS)
                              .mapToTable(MediaContract.Artists.HOST_ID, MediaDatabase.Tables.ARTISTS)
                              .mapToTable(MediaContract.Artists.ARTISTID, MediaDatabase.Tables.ARTISTS)
                              .mapToTable(MediaContract.AlbumArtists.ALBUMID, MediaDatabase.Tables.ALBUM_ARTISTS)
                              .where(Qualified.ALBUM_ARTISTS_HOST_ID + "=?", hostId)
                              .where(Qualified.ALBUM_ARTISTS_ALBUMID + "=?", albumId);
            }
            case ALBUM_GENRES_LIST: {
                // Genres for Album
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String albumId = MediaContract.Albums.getAlbumId(uri);
                return builder.table(MediaDatabase.Tables.GENRES_FOR_ALBUM_JOIN)
                              .mapToTable(MediaContract.AudioGenres._ID, MediaDatabase.Tables.AUDIO_GENRES)
                              .mapToTable(MediaContract.AudioGenres.HOST_ID, MediaDatabase.Tables.AUDIO_GENRES)
                              .mapToTable(MediaContract.AudioGenres.GENREID, MediaDatabase.Tables.AUDIO_GENRES)
                              .mapToTable(MediaContract.AlbumGenres.ALBUMID, MediaDatabase.Tables.ALBUM_GENRES)
                              .where(Qualified.ALBUM_GENRES_HOST_ID + "=?", hostId)
                              .where(Qualified.ALBUM_GENRES_ALBUMID + "=?", albumId);
            }
            case AUDIO_GENRE_ALBUMS_LIST: {
                // Album for a Genre
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String genreId = MediaContract.AudioGenres.getAudioGenreId(uri);
                return builder.table(MediaDatabase.Tables.ALBUMS_FOR_GENRE_JOIN)
                              .mapToTable(MediaContract.Albums._ID, MediaDatabase.Tables.ALBUMS)
                              .mapToTable(MediaContract.Albums.HOST_ID, MediaDatabase.Tables.ALBUMS)
                              .mapToTable(MediaContract.Albums.ALBUMID, MediaDatabase.Tables.ALBUMS)
                              .mapToTable(MediaContract.AlbumGenres.GENREID, MediaDatabase.Tables.ALBUM_GENRES)
                              .where(Qualified.ALBUM_GENRES_HOST_ID + "=?", hostId)
                              .where(Qualified.ALBUM_GENRES_GENREID + "=?", genreId);
            }
            case MUSIC_VIDEOS_ALL: {
                return builder.table(MediaDatabase.Tables.MUSIC_VIDEOS);
            }
            case MUSIC_VIDEOS_LIST: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                return builder.table(MediaDatabase.Tables.MUSIC_VIDEOS)
                              .where(MediaContract.MusicVideos.HOST_ID + "=?", hostId);
            }
            case MUSIC_VIDEOS_ID: {
                final String hostId = MediaContract.Hosts.getHostId(uri);
                final String musicVideoId = MediaContract.MusicVideos.getMusicVideoId(uri);
                return builder.table(MediaDatabase.Tables.MUSIC_VIDEOS)
                              .where(MediaContract.MusicVideos.HOST_ID + "=?", hostId)
                              .where(MediaContract.MusicVideos.MUSICVIDEOID + "=?", musicVideoId);
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * {@link MediaContract} fields that are fully qualified with a specific
     * parent {@link MediaDatabase.Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        String ALBUM_ARTISTS_HOST_ID =
                MediaDatabase.Tables.ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.HOST_ID;
        String ALBUM_ARTISTS_ARTISTID =
                MediaDatabase.Tables.ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.ARTISTID;
        String ALBUM_ARTISTS_ALBUMID =
                MediaDatabase.Tables.ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.ALBUMID;
        String ALBUM_GENRES_HOST_ID =
                MediaDatabase.Tables.ALBUM_GENRES + "." + MediaContract.AlbumGenres.HOST_ID;
        String ALBUM_GENRES_GENREID =
                MediaDatabase.Tables.ALBUM_GENRES + "." + MediaContract.AlbumGenres.GENREID;
        String ALBUM_GENRES_ALBUMID =
                MediaDatabase.Tables.ALBUM_GENRES + "." + MediaContract.AlbumGenres.ALBUMID;

    }
}
