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
package org.xbmc.kore.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.utils.LogUtils;


/**
 * Manages the XBMC local database. Info is stored in a {@link SQLiteDatabase}.
 */
public class MediaDatabase extends SQLiteOpenHelper {
	private static final String TAG = LogUtils.makeLogTag(MediaDatabase.class);

	private static final String DB_NAME = "xbmc.sqlite";
    private static final int DB_VERSION_PRE_EVENT_SERVER = 4,
            DB_VERSION_PRE_SONG_ARTISTS = 5,
            DB_VERSION_PRE_SONG_DISPLAY_ARTIST = 6,
            DB_VERSION_PRE_SONG_DISC = 7,
            DB_VERSION_PRE_HOST_VERSION = 8,
            DB_VERSION = 9;

	/**
	 * Tables exposed
	 */
	public interface Tables {
		String HOSTS = "hosts";
        String MOVIES = "movies";
        String MOVIE_CAST = "movie_cast";
        String TVSHOWS = "tvshows";
        String TVSHOWS_CAST = "tvshows_cast";
        String SEASONS = "seasons";
        String EPISODES = "episodes";
        String ARTISTS = "artists";
        String ALBUMS = "albums";
        String SONGS = "songs";
        String SONG_ARTISTS = "song_artists";
        String AUDIO_GENRES = "audio_genres";
        String ALBUM_ARTISTS = "album_artists";
        String ALBUM_GENRES = "album_genres";
        String MUSIC_VIDEOS = "music_videos";

        /**
         * Join to get Albums for an Artist
         */
        String ALBUMS_FOR_ARTIST_JOIN =
                ALBUM_ARTISTS + " JOIN " + ALBUMS + " ON " +
                ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.HOST_ID + "=" + ALBUMS + "." + MediaContract.Albums.HOST_ID +
                " AND " +
                ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.ALBUMID + "=" + ALBUMS + "." + MediaContract.Albums.ALBUMID;

        /**
         * Join to get Artists for an Album
         */
        String ARTISTS_FOR_ALBUM_JOIN =
                ALBUM_ARTISTS + " JOIN " + ARTISTS + " ON " +
                ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.HOST_ID + "=" + ARTISTS + "." + MediaContract.Artists.HOST_ID +
                " AND " +
                ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.ARTISTID + "=" + ARTISTS + "." + MediaContract.Artists.ARTISTID;

        /**
         * Join to get Album for a Genre
         */
        String ALBUMS_FOR_GENRE_JOIN =
                ALBUM_GENRES + " JOIN " + ALBUMS + " ON " +
                ALBUM_GENRES + "." + MediaContract.AlbumGenres.HOST_ID + "=" + ALBUMS + "." + MediaContract.Albums.HOST_ID +
                " AND " +
                ALBUM_GENRES + "." + MediaContract.AlbumGenres.ALBUMID + "=" + ALBUMS + "." + MediaContract.Albums.ALBUMID;

        /**
         * Join to get Genres for an Album
         */
        String GENRES_FOR_ALBUM_JOIN =
                ALBUM_GENRES + " JOIN " + AUDIO_GENRES + " ON " +
                ALBUM_GENRES + "." + MediaContract.AlbumGenres.HOST_ID + "=" + AUDIO_GENRES + "." + MediaContract.AudioGenres.HOST_ID +
                " AND " +
                ALBUM_GENRES + "." + MediaContract.AlbumGenres.GENREID + "=" + AUDIO_GENRES + "." + MediaContract.AudioGenres.GENREID;

        /**
         * Join to get Songs for an Artist or Album with artist info and album info only if available
         */
        String SONGS_FOR_ARTIST_AND_OR_ALBUM_JOIN =
                SONGS + " JOIN " + SONG_ARTISTS + " ON " +
                SONG_ARTISTS + "." + MediaContract.SongArtists.HOST_ID + "=" + SONGS + "." + MediaContract.Songs.HOST_ID +
                " AND " +
                SONG_ARTISTS + "." + MediaContract.SongArtists.SONGID + "=" + SONGS + "." + MediaContract.Songs.SONGID +
                " LEFT JOIN " + ARTISTS + " ON " +
                SONG_ARTISTS + "." + MediaContract.SongArtists.HOST_ID + "=" + ARTISTS + "." + MediaContract.Artists.HOST_ID +
                " AND " +
                SONG_ARTISTS + "." + MediaContract.SongArtists.ARTISTID + "=" + ARTISTS + "." + MediaContract.Artists.ARTISTID +
                " LEFT JOIN " + ALBUM_ARTISTS + " ON " +
                SONG_ARTISTS + "." + MediaContract.SongArtists.HOST_ID + "=" + ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.HOST_ID +
                " AND " +
                SONGS + "." + MediaContract.Songs.ALBUMID + "=" + ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.ALBUMID +
                " LEFT JOIN " + ALBUMS + " ON " +
                SONG_ARTISTS + "." + MediaContract.SongArtists.HOST_ID + "=" + ALBUMS + "." + MediaContract.Albums.HOST_ID +
                " AND " +
                ALBUM_ARTISTS + "." + MediaContract.AlbumArtists.ALBUMID + "=" + ALBUMS + "." + MediaContract.Albums.ALBUMID;
    }



    private interface References {
        String HOST_ID =
                "REFERENCES " + Tables.HOSTS + "(" + BaseColumns._ID + ")";
        String ALBUMID =
                "REFERENCES " + Tables.ALBUMS + "(" + MediaContract.AlbumsColumns.ALBUMID + ")";
        String ARTISTID =
                "REFERENCES " + Tables.ARTISTS + "(" + MediaContract.ArtistsColumns.ARTISTID + ")";
        String GENREID =
                "REFERENCES " + Tables.AUDIO_GENRES + "(" + MediaContract.AudioGenresColumns.GENREID + ")";
        String SONGID =
                "REFERENCES " + Tables.SONGS + "(" + MediaContract.Songs.SONGID + ")";
    }

    public MediaDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

        // Hosts
        db.execSQL("CREATE TABLE " + Tables.HOSTS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.HostsColumns.NAME + " TEXT, " +
                   MediaContract.HostsColumns.ADDRESS + " TEXT, " +
                   MediaContract.HostsColumns.PROTOCOL + " INTEGER, " +
                   MediaContract.HostsColumns.HTTP_PORT + " INTEGER, " +
                   MediaContract.HostsColumns.TCP_PORT + " INTEGER, " +
                   MediaContract.HostsColumns.USERNAME + " TEXT, " +
                   MediaContract.HostsColumns.PASSWORD + " TEXT, " +
                   MediaContract.HostsColumns.MAC_ADDRESS + " TEXT, " +
                   MediaContract.HostsColumns.WOL_PORT + " INTEGER, " +
                   MediaContract.HostsColumns.USE_EVENT_SERVER + " INTEGER, " +
                   MediaContract.HostsColumns.EVENT_SERVER_PORT + " INTEGER, " +

                   MediaContract.HostsColumns.KODI_VERSION_MAJOR + " INTEGER, " +
                   MediaContract.HostsColumns.KODI_VERSION_MINOR + " INTEGER, " +
                   MediaContract.HostsColumns.KODI_VERSION_REVISION + " TEXT, " +
                   MediaContract.HostsColumns.KODI_VERSION_TAG + " TEXT)"
		);

        // Movies
        db.execSQL("CREATE TABLE " + Tables.MOVIES + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.MoviesColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.MoviesColumns.MOVIEID + " INTEGER NOT NULL, " +
                   MediaContract.MoviesColumns.FANART + " TEXT, " +
                   MediaContract.MoviesColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.MoviesColumns.PLAYCOUNT + " INTEGER, " +
                   MediaContract.MoviesColumns.TITLE + " TEXT, " +
                   MediaContract.MoviesColumns.FILE + " TEXT, " +
                   MediaContract.MoviesColumns.PLOT + " TEXT, " +
                   MediaContract.MoviesColumns.DIRECTOR + " TEXT, " +
                   MediaContract.MoviesColumns.RUNTIME + " INTEGER, " +
                   MediaContract.MoviesColumns.AUDIO_CHANNELS + " INTEGER, " +
                   MediaContract.MoviesColumns.AUDIO_CODEC + " TEXT, " +
                   MediaContract.MoviesColumns.AUDIO_LANGUAGE + " TEXT, " +
                   MediaContract.MoviesColumns.SUBTITLES_LANGUAGES + " TEXT, " +
                   MediaContract.MoviesColumns.VIDEO_ASPECT + " REAL, " +
                   MediaContract.MoviesColumns.VIDEO_CODEC + " TEXT, " +
                   MediaContract.MoviesColumns.VIDEO_HEIGHT + " INTEGER, " +
                   MediaContract.MoviesColumns.VIDEO_WIDTH + " INTEGER, " +
                   MediaContract.MoviesColumns.COUNTRIES + " TEXT, " +
                   MediaContract.MoviesColumns.GENRES + " TEXT, " +
                   MediaContract.MoviesColumns.IMDBNUMBER + " TEXT, " +
                   MediaContract.MoviesColumns.MPAA + " TEXT, " +
                   MediaContract.MoviesColumns.RATING + " REAL, " +
                   MediaContract.MoviesColumns.SET + " TEXT, " +
                   MediaContract.MoviesColumns.SETID + " INTEGER, " +
                   MediaContract.MoviesColumns.STUDIOS + " TEXT, " +
                   MediaContract.MoviesColumns.TAGLINE + " TEXT, " +
                   MediaContract.MoviesColumns.TOP250 + " INTEGER, " +
                   MediaContract.MoviesColumns.TRAILER + " TEXT, " +
                   MediaContract.MoviesColumns.VOTES + " TEXT, " +
                   MediaContract.MoviesColumns.WRITERS + " TEXT, " +
                   MediaContract.MoviesColumns.YEAR + " INTEGER, " +
                   MediaContract.MoviesColumns.DATEADDED + " TEXT, " +
                   "UNIQUE (" + MediaContract.MoviesColumns.HOST_ID + ", " + MediaContract.MoviesColumns.MOVIEID + ") ON CONFLICT REPLACE)"
        );

        // Movie Cast
        db.execSQL("CREATE TABLE " + Tables.MOVIE_CAST + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.MovieCastColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.MovieCastColumns.MOVIEID + " INTEGER NOT NULL, " +
                   MediaContract.MovieCastColumns.NAME + " TEXT, " +
                   MediaContract.MovieCastColumns.ORDER + " INTEGER, " +
                   MediaContract.MovieCastColumns.ROLE + " TEXT, " +
                   MediaContract.MovieCastColumns.THUMBNAIL + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.MovieCastColumns.HOST_ID + ", " +
                   MediaContract.MovieCastColumns.MOVIEID + ", " +
                   MediaContract.MovieCastColumns.NAME +
                   ") ON CONFLICT REPLACE)"
        );

        // TVShows
        db.execSQL("CREATE TABLE " + Tables.TVSHOWS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.TVShowsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.TVShowsColumns.TVSHOWID + " INTEGER NOT NULL, " +
                   MediaContract.TVShowsColumns.FANART + " TEXT, " +
                   MediaContract.TVShowsColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.TVShowsColumns.PLAYCOUNT + " INTEGER, " +
                   MediaContract.TVShowsColumns.TITLE + " TEXT, " +
                   MediaContract.TVShowsColumns.DATEADDED + " TEXT, " +
                   MediaContract.TVShowsColumns.FILE + " TEXT, " +
                   MediaContract.TVShowsColumns.PLOT + " TEXT, " +
                   MediaContract.TVShowsColumns.EPISODE + " INTEGER, " +
                   MediaContract.TVShowsColumns.IMDBNUMBER + " TEXT, " +
                   MediaContract.TVShowsColumns.MPAA + " TEXT, " +
                   MediaContract.TVShowsColumns.PREMIERED + " TEXT, " +
                   MediaContract.TVShowsColumns.RATING + " REAL, " +
                   MediaContract.TVShowsColumns.STUDIO + " TEXT, " +
                   MediaContract.TVShowsColumns.WATCHEDEPISODES + " INTEGER, " +
                   MediaContract.MoviesColumns.GENRES + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.TVShowsColumns.HOST_ID + ", " +
                   MediaContract.TVShowsColumns.TVSHOWID +
                   ") ON CONFLICT REPLACE)"
        );

        // TVShows Cast
        db.execSQL("CREATE TABLE " + Tables.TVSHOWS_CAST + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.TVShowCastColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.TVShowCastColumns.TVSHOWID + " INTEGER NOT NULL, " +
                   MediaContract.TVShowCastColumns.NAME + " TEXT, " +
                   MediaContract.TVShowCastColumns.ORDER + " INTEGER, " +
                   MediaContract.TVShowCastColumns.ROLE + " TEXT, " +
                   MediaContract.TVShowCastColumns.THUMBNAIL + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.TVShowCastColumns.HOST_ID + ", " +
                   MediaContract.TVShowCastColumns.TVSHOWID + ", " +
                   MediaContract.TVShowCastColumns.NAME +
                   ") ON CONFLICT REPLACE)"
        );

        // Seasons
        db.execSQL("CREATE TABLE " + Tables.SEASONS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.SeasonsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.SeasonsColumns.TVSHOWID + " INTEGER NOT NULL, " +
                   MediaContract.SeasonsColumns.SEASON + " INTEGER NOT NULL, " +
                   MediaContract.SeasonsColumns.LABEL + " TEXT, " +
                   MediaContract.SeasonsColumns.FANART + " TEXT, " +
                   MediaContract.SeasonsColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.SeasonsColumns.EPISODE + " INTEGER, " +
                   MediaContract.SeasonsColumns.SHOWTITLE + " TEXT, " +
                   MediaContract.SeasonsColumns.WATCHEDEPISODES + " INTEGER, " +
                   "UNIQUE (" +
                   MediaContract.SeasonsColumns.HOST_ID + ", " +
                   MediaContract.SeasonsColumns.TVSHOWID + ", " +
                   MediaContract.SeasonsColumns.SEASON +
                   ") ON CONFLICT REPLACE)"
        );

        // Episodes
        db.execSQL("CREATE TABLE " + Tables.EPISODES + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.EpisodesColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.EpisodesColumns.EPISODEID + " INTEGER NOT NULL, " +
                   MediaContract.SeasonsColumns.TVSHOWID + " INTEGER NOT NULL, " +
                   MediaContract.SeasonsColumns.SEASON + " INTEGER NOT NULL, " +
                   MediaContract.SeasonsColumns.EPISODE + " INTEGER NOT NULL, " +
                   MediaContract.EpisodesColumns.FANART + " TEXT, " +
                   MediaContract.EpisodesColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.EpisodesColumns.PLAYCOUNT + " INTEGER, " +
                   MediaContract.EpisodesColumns.TITLE + " TEXT, " +
                   MediaContract.EpisodesColumns.DATEADDED + " TEXT, " +
                   MediaContract.EpisodesColumns.FILE + " TEXT, " +
                   MediaContract.EpisodesColumns.PLOT + " TEXT, " +
                   MediaContract.EpisodesColumns.DIRECTOR + " TEXT, " +
                   MediaContract.EpisodesColumns.RUNTIME + " INTEGER, " +
                   MediaContract.EpisodesColumns.FIRSTAIRED + " TEXT, " +
                   MediaContract.EpisodesColumns.RATING + " REAL, " +
                   MediaContract.EpisodesColumns.SHOWTITLE + " TEXT, " +
                   MediaContract.EpisodesColumns.WRITER + " TEXT, " +
                   MediaContract.EpisodesColumns.AUDIO_CHANNELS + " INTEGER, " +
                   MediaContract.EpisodesColumns.AUDIO_CODEC + " TEXT, " +
                   MediaContract.EpisodesColumns.AUDIO_LANGUAGE + " TEXT, " +
                   MediaContract.EpisodesColumns.SUBTITLES_LANGUAGES + " TEXT, " +
                   MediaContract.EpisodesColumns.VIDEO_ASPECT + " REAL, " +
                   MediaContract.EpisodesColumns.VIDEO_CODEC + " TEXT, " +
                   MediaContract.EpisodesColumns.VIDEO_HEIGHT + " INTEGER, " +
                   MediaContract.EpisodesColumns.VIDEO_WIDTH + " INTEGER, " +
                   "UNIQUE (" +
                   MediaContract.EpisodesColumns.HOST_ID + ", " +
                   MediaContract.EpisodesColumns.EPISODEID +
                   ") ON CONFLICT REPLACE)"
        );

        // Artists
        db.execSQL("CREATE TABLE " + Tables.ARTISTS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.ArtistsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.ArtistsColumns.ARTISTID + " INTEGER NOT NULL, " +
                   MediaContract.ArtistsColumns.ARTIST + " TEXT, " +
                   MediaContract.ArtistsColumns.DESCRIPTION + " TEXT, " +
                   MediaContract.ArtistsColumns.GENRE + " TEXT, " +
                   MediaContract.ArtistsColumns.FANART + " TEXT, " +
                   MediaContract.ArtistsColumns.THUMBNAIL + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.ArtistsColumns.HOST_ID + ", " +
                   MediaContract.ArtistsColumns.ARTISTID +
                   ") ON CONFLICT REPLACE)"
        );

        // Albums
        db.execSQL("CREATE TABLE " + Tables.ALBUMS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.AlbumsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.AlbumsColumns.ALBUMID + " INTEGER NOT NULL, " +
                   MediaContract.AlbumsColumns.FANART + " TEXT, " +
                   MediaContract.AlbumsColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.AlbumsColumns.DISPLAYARTIST + " TEXT, " +
                   MediaContract.AlbumsColumns.RATING + " INTEGER, " +
                   MediaContract.AlbumsColumns.TITLE + " TEXT, " +
                   MediaContract.AlbumsColumns.YEAR + " INTEGER, " +
                   MediaContract.AlbumsColumns.ALBUMLABEL + " TEXT, " +
                   MediaContract.AlbumsColumns.DESCRIPTION + " TEXT, " +
                   MediaContract.AlbumsColumns.PLAYCOUNT + " INTEGER, " +
                   MediaContract.AlbumsColumns.GENRE + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.AlbumsColumns.HOST_ID + ", " +
                   MediaContract.AlbumsColumns.ALBUMID +
                   ") ON CONFLICT REPLACE)"
        );

        // Songs
        db.execSQL("CREATE TABLE " + Tables.SONGS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.SongsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.SongsColumns.ALBUMID + " INTEGER NOT NULL, " +
                   MediaContract.SongsColumns.DISC + " INTEGER NOT NULL, " +
                   MediaContract.SongsColumns.SONGID + " INTEGER NOT NULL, " +
                   MediaContract.SongsColumns.DURATION + " INTEGER, " +
                   MediaContract.SongsColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.SongsColumns.FILE + " TEXT, " +
                   MediaContract.SongsColumns.TRACK + " INTEGER, " +
                   MediaContract.SongsColumns.TITLE + " TEXT, " +
                   MediaContract.SongsColumns.DISPLAYARTIST + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.SongsColumns.HOST_ID + ", " +
                   MediaContract.SongsColumns.ALBUMID + ", " +
                   MediaContract.SongsColumns.SONGID +
                   ") ON CONFLICT REPLACE)"
        );

        createSongArtistsTable(db);

        // AudioGenres
        db.execSQL("CREATE TABLE " + Tables.AUDIO_GENRES + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.AudioGenresColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.AudioGenresColumns.GENREID + " INTEGER NOT NULL, " +
                   MediaContract.AudioGenresColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.AudioGenresColumns.TITLE + " TEXT, " +
                   "UNIQUE (" +
                   MediaContract.AudioGenresColumns.HOST_ID + ", " +
                   MediaContract.AudioGenresColumns.GENREID +
                   ") ON CONFLICT REPLACE)"
        );

        // AlbumArtists
        db.execSQL("CREATE TABLE " + Tables.ALBUM_ARTISTS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.AlbumArtistsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.AlbumArtistsColumns.ALBUMID + " INTEGER NOT NULL " + References.ALBUMID + ", " +
                   MediaContract.AlbumArtistsColumns.ARTISTID + " INTEGER NOT NULL " + References.ARTISTID + ", " +
                   "UNIQUE (" +
                   MediaContract.AlbumArtistsColumns.HOST_ID + ", " +
                   MediaContract.AlbumArtistsColumns.ALBUMID + ", " +
                   MediaContract.AlbumArtistsColumns.ARTISTID +
                   ") ON CONFLICT REPLACE)"
        );

        // AlbumGenres
        db.execSQL("CREATE TABLE " + Tables.ALBUM_GENRES + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.AlbumGenresColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.AlbumGenresColumns.ALBUMID + " INTEGER NOT NULL " + References.ALBUMID + ", " +
                   MediaContract.AlbumGenresColumns.GENREID + " INTEGER NOT NULL " + References .GENREID + ", " +
                   "UNIQUE (" +
                   MediaContract.AlbumGenresColumns.HOST_ID + ", " +
                   MediaContract.AlbumGenresColumns.ALBUMID + ", " +
                   MediaContract.AlbumGenresColumns.GENREID +
                   ") ON CONFLICT REPLACE)"
        );

        // Music videos
        db.execSQL("CREATE TABLE " + Tables.MUSIC_VIDEOS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SyncColumns.UPDATED + " INTEGER NOT NULL," +
                   MediaContract.MusicVideosColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.MusicVideosColumns.MUSICVIDEOID + " INTEGER NOT NULL, " +
                   MediaContract.MusicVideosColumns.FANART + " TEXT, " +
                   MediaContract.MusicVideosColumns.THUMBNAIL + " TEXT, " +
                   MediaContract.MusicVideosColumns.PLAYCOUNT + " INTEGER, " +
                   MediaContract.MusicVideosColumns.TITLE + " TEXT, " +
                   MediaContract.MusicVideosColumns.FILE + " TEXT, " +
                   MediaContract.MusicVideosColumns.PLOT + " TEXT, " +
                   MediaContract.MusicVideosColumns.DIRECTOR + " TEXT, " +
                   MediaContract.MusicVideosColumns.RUNTIME + " INTEGER, " +
                   MediaContract.MusicVideosColumns.AUDIO_CHANNELS + " INTEGER, " +
                   MediaContract.MusicVideosColumns.AUDIO_CODEC + " TEXT, " +
                   MediaContract.MusicVideosColumns.AUDIO_LANGUAGE + " TEXT, " +
                   MediaContract.MusicVideosColumns.SUBTITLES_LANGUAGES + " TEXT, " +
                   MediaContract.MusicVideosColumns.VIDEO_ASPECT + " REAL, " +
                   MediaContract.MusicVideosColumns.VIDEO_CODEC + " TEXT, " +
                   MediaContract.MusicVideosColumns.VIDEO_HEIGHT + " INTEGER, " +
                   MediaContract.MusicVideosColumns.VIDEO_WIDTH + " INTEGER, " +
                   MediaContract.MusicVideosColumns.ALBUM + " TEXT, " +
                   MediaContract.MusicVideosColumns.ARTIST + " TEXT, " +
                   MediaContract.MusicVideosColumns.GENRES + " TEXT, " +
                   MediaContract.MusicVideosColumns.STUDIOS + " TEXT, " +
                   MediaContract.MusicVideosColumns.TAG + " TEXT, " +
                   MediaContract.MusicVideosColumns.TRACK + " INTEGER, " +
                   MediaContract.MusicVideosColumns.YEAR + " INTEGER, " +
                   "UNIQUE (" + MediaContract.MusicVideosColumns.HOST_ID + ", " +
                   "" + MediaContract.MusicVideosColumns.MUSICVIDEOID + ") ON CONFLICT REPLACE)"
        );


        // TODO: Indices?

        // Triggers on host delete
        db.execSQL(buildHostsDeleteTrigger(Tables.MOVIES, MediaContract.MoviesColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.MOVIE_CAST, MediaContract.MovieCastColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.TVSHOWS, MediaContract.TVShowsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.TVSHOWS_CAST, MediaContract.TVShowCastColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.EPISODES, MediaContract.EpisodesColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.SEASONS, MediaContract.SeasonsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.ARTISTS, MediaContract.ArtistsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.ALBUMS, MediaContract.AlbumsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.SONGS, MediaContract.SongsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.AUDIO_GENRES, MediaContract.AudioGenresColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.ALBUM_ARTISTS, MediaContract.AlbumArtistsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.SONG_ARTISTS, MediaContract.SongArtistsColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.ALBUM_GENRES, MediaContract.AlbumGenresColumns.HOST_ID));
        db.execSQL(buildHostsDeleteTrigger(Tables.MUSIC_VIDEOS, MediaContract.MusicVideosColumns.HOST_ID));

    }

    private String buildHostsDeleteTrigger(String onTable, String hostIdColumn) {
        return "CREATE TRIGGER host_" + onTable + "_delete AFTER DELETE ON " + Tables.HOSTS +
               " BEGIN DELETE FROM " + onTable +
               " WHERE " + onTable + "." + hostIdColumn + "=old." + BaseColumns._ID +
               ";" + " END;";
    }

    @Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        switch (oldVersion) {
            case DB_VERSION_PRE_EVENT_SERVER:
                // Add USE_EVENT_SERVER and EVENT_SERVER_PORT columns to HOSTS table
                db.execSQL("ALTER TABLE " + Tables.HOSTS +
                           " ADD COLUMN " + MediaContract.HostsColumns.USE_EVENT_SERVER +
                           " INTEGER DEFAULT 1;");
                db.execSQL("ALTER TABLE " + Tables.HOSTS +
                           " ADD COLUMN " + MediaContract.HostsColumns.EVENT_SERVER_PORT +
                           " INTEGER DEFAULT " + HostInfo.DEFAULT_EVENT_SERVER_PORT + ";");
            case DB_VERSION_PRE_SONG_ARTISTS:
                createSongArtistsTable(db);
            case DB_VERSION_PRE_SONG_DISPLAY_ARTIST:
                db.execSQL("ALTER TABLE " + Tables.SONGS +
                           " ADD COLUMN " + MediaContract.SongsColumns.DISPLAYARTIST +
                           " TEXT;");
            case DB_VERSION_PRE_SONG_DISC:
                db.execSQL("ALTER TABLE " + Tables.SONGS +
                           " ADD COLUMN " + MediaContract.SongsColumns.DISC +
                           " INTEGER DEFAULT 1;");
            case DB_VERSION_PRE_HOST_VERSION:
                db.execSQL("ALTER TABLE " + Tables.HOSTS +
                           " ADD COLUMN " + MediaContract.HostsColumns.KODI_VERSION_MAJOR +
                           " INTEGER DEFAULT " + String.valueOf(HostInfo.DEFAULT_KODI_VERSION_MAJOR) + ";");
                db.execSQL("ALTER TABLE " + Tables.HOSTS +
                           " ADD COLUMN " + MediaContract.HostsColumns.KODI_VERSION_MINOR +
                           " INTEGER DEFAULT " + String.valueOf(HostInfo.DEFAULT_KODI_VERSION_MINOR) + ";");
                db.execSQL("ALTER TABLE " + Tables.HOSTS +
                           " ADD COLUMN " + MediaContract.HostsColumns.KODI_VERSION_REVISION +
                           " TEXT DEFAULT " + HostInfo.DEFAULT_KODI_VERSION_REVISION + ";");
                db.execSQL("ALTER TABLE " + Tables.HOSTS +
                           " ADD COLUMN " + MediaContract.HostsColumns.KODI_VERSION_TAG +
                           " TEXT DEFAULT " + HostInfo.DEFAULT_KODI_VERSION_TAG + ";");
        }
	}

    /**
     * Tokens to move from prefix to suffix when sorting titles
     *
     * TODO: Extract from host's advancedsettings.xml - sortTokens if available via JSONAPI
     */
    private static String[] commonTokens = {"The", /* "An", "A" */};

    /**
     * Given column create SQLite column expression to convert any sortTokens prefixes to suffixes
     *
     * eg.column = "title", commonTokens = {"The", "An", "A"};
     *
     *      (
     *          CASE
     *              WHEN title LIKE 'The %' THEN SUBSTR(title,5) || ', The'
     *              WHEN title LIKE 'An %'  THEN SUBSTR(title,4) || ', An'
     *              WHEN title LIKE 'A %'   THEN SUBSTR(title,3) || ', A'
     *              ELSE title
     *          END
     *      )
     *
     * This allows it to be used in SQL where a column expression is expected ( SELECT, ORDER BY )
     */
    public static String sortCommonTokens(String column) {
        StringBuilder order = new StringBuilder();

        order.append(" (CASE ");

        // Create WHEN for each token, eg 'The Dog' would become 'Dog, The'
        for (String token: commonTokens) {
            order.append(
                 " WHEN " + column + " LIKE '" + token + " %'" +
                 " THEN SUBSTR(" + column + "," + String.valueOf(token.length() + 2) + ")" +
                 " || ', " + token + "' "
            );
        }

        order.append(" ELSE " + column + " END) ");

        return order.toString();
    }

    private void createSongArtistsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.SONG_ARTISTS + "(" +
                   BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   MediaContract.SongArtistsColumns.HOST_ID + " INTEGER NOT NULL " + References.HOST_ID + ", " +
                   MediaContract.SongArtistsColumns.SONGID + " INTEGER NOT NULL " + References.SONGID + ", " +
                   MediaContract.SongArtistsColumns.ARTISTID + " INTEGER NOT NULL " + References .ARTISTID + ", " +
                   "UNIQUE (" +
                   MediaContract.SongArtistsColumns.HOST_ID + ", " +
                   MediaContract.SongArtistsColumns.SONGID + ", " +
                   MediaContract.SongArtistsColumns.ARTISTID +
                   ") ON CONFLICT REPLACE)"
                  );
    }
}

