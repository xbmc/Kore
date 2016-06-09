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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for interacting with {@link MediaProvider}.
 */
public class MediaContract {

    public static final String CONTENT_AUTHORITY = "org.xbmc.kore.provider";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Paths to tables
     */
    public static final String PATH_HOSTS = "hosts";
    public static final String PATH_MOVIES = "movies";
    public static final String PATH_MOVIE_CAST = "movie_cast";
    public static final String PATH_TVSHOWS = "tvshows";
    public static final String PATH_TVSHOW_CAST = "tvshow_cast";
    public static final String PATH_SEASONS = "seasons";
    public static final String PATH_EPISODES = "episodes";
    public static final String PATH_ARTISTS = "artists";
    public static final String PATH_ALBUMS = "albums";
    public static final String PATH_AUDIO_GENRES = "audio_genres";
    public static final String PATH_SONGS = "songs";
    public static final String PATH_SONG_ARTISTS = "song_artists";
    public static final String PATH_ALBUM_ARTISTS = "album_artists";
    public static final String PATH_ALBUM_GENRES = "album_genres";
    public static final String PATH_MUSIC_VIDEOS = "music_videos";

    /** Last time this entry was updated or synchronized. */
    public interface SyncColumns {
        String UPDATED = "updated";
    }

    /**
     * Columns for table HOSTS
     */
    public interface HostsColumns {
        String NAME = "name";
        String ADDRESS = "address";
        String PROTOCOL = "protocol";
        String HTTP_PORT = "http_port";
        String TCP_PORT = "tcp_port";
        String USERNAME = "username";
        String PASSWORD = "password";
        String MAC_ADDRESS = "mac_address";
        String WOL_PORT = "wol_port";
        String USE_EVENT_SERVER = "use_event_server";
        String EVENT_SERVER_PORT = "event_server_port";
    }

    public static class Hosts implements BaseColumns, SyncColumns, HostsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_HOSTS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_HOSTS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_HOSTS;

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildHostUri(long hostId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(hostId)).build();
        }

        /** Read {@link #_ID} from {@link Hosts} {@link Uri}. */
        public static String getHostId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, NAME, ADDRESS, PROTOCOL, HTTP_PORT, TCP_PORT, USERNAME, PASSWORD,
                MAC_ADDRESS, WOL_PORT, USE_EVENT_SERVER, EVENT_SERVER_PORT
        };
    }

    /**
     * Columns for table Movies
     * For XBMC reference/unique key use HOST_ID + MOVIEID
     */
    public interface MoviesColumns {
        String HOST_ID = "host_id";
        String MOVIEID = "movieid";

        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String PLAYCOUNT = "playcount";
        String TITLE = "title";
        String FILE = "file";
        String PLOT = "plot";
        String DIRECTOR = "director";
        String RUNTIME = "runtime";
        String AUDIO_CHANNELS = "audio_channels";
        String AUDIO_CODEC = "audio_coded";
        String AUDIO_LANGUAGE = "audio_language";
        String SUBTITLES_LANGUAGES = "subtitles_languages";
        String VIDEO_ASPECT = "video_aspect";
        String VIDEO_CODEC = "video_codec";
        String VIDEO_HEIGHT = "video_height";
        String VIDEO_WIDTH = "video_width";
        String COUNTRIES = "countries";
        String GENRES = "genres";
        String IMDBNUMBER = "imdbnumber";
        String MPAA = "mpaa";
        String RATING = "rating";
        String SET = "movie_set";
        String SETID = "setid";
        String STUDIOS = "studios";
        String TAGLINE = "tagline";
        String TOP250 = "top250";
        String TRAILER = "trailer";
        String VOTES = "votes";
        String WRITERS = "writers";
        String YEAR = "year";
        String DATEADDED = "dateadded";
    }

    public static class Movies implements BaseColumns, SyncColumns, MoviesColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_MOVIES;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_MOVIES;

        /** Build {@link Uri} for movies list. */
        public static Uri buildMoviesListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_MOVIES)
                        .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildMovieUri(long hostId, long movieId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_MOVIES)
                        .appendPath(String.valueOf(movieId))
                        .build();
        }

        /** Read {@link #_ID} from {@link Movies} {@link Uri}. */
        public static String getMovieId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, MOVIEID, FANART, THUMBNAIL, PLAYCOUNT, TITLE, FILE, PLOT,
                DIRECTOR, RUNTIME, AUDIO_CHANNELS, AUDIO_CODEC, AUDIO_LANGUAGE,
                SUBTITLES_LANGUAGES, VIDEO_ASPECT, VIDEO_CODEC, VIDEO_HEIGHT, VIDEO_WIDTH,
                COUNTRIES, GENRES, IMDBNUMBER, MPAA, RATING, SET, SETID, STUDIOS, TAGLINE,
                TOP250, TRAILER, VOTES, WRITERS, YEAR, DATEADDED
        };
    }

    /**
     * Columns for MovieCast table
     * For XBMC reference/unique key use HOST_ID + MOVIEID + NAME
     */
    public interface MovieCastColumns {
        String HOST_ID = "host_id";
        String MOVIEID = "movieid";
        String NAME = "name";

        String ORDER = "cast_order";
        String ROLE = "role";
        String THUMBNAIL = "thumbnail";
    }

    public static class MovieCast implements BaseColumns, SyncColumns, MovieCastColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE_CAST).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_MOVIE_CAST;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_MOVIE_CAST;

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildMovieCastListUri(long hostId, long movieId) {
            return Movies.buildMovieUri(hostId, movieId).buildUpon()
                        .appendPath(PATH_MOVIE_CAST)
                        .build();
        }
    }

    /**
     * Columns for table TVShows
     * For XBMC reference use HOST_ID + TVSHOWID
     */
    public interface TVShowsColumns {
        String HOST_ID = "host_id";
        String TVSHOWID = "tvshowid";

        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String PLAYCOUNT = "playcount";
        String TITLE = "title";
        String DATEADDED = "dateadded";
        String FILE = "file";
        String PLOT = "plot";
        String EPISODE = "episode";
        String IMDBNUMBER = "imdbnumber";
        String MPAA = "mpaa";
        String PREMIERED = "premiered";
        String RATING = "rating";
        String STUDIO = "studio";
        String WATCHEDEPISODES = "watchedepisodes";
        String GENRES = "genres";
    }

    public static class TVShows implements BaseColumns, SyncColumns, TVShowsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TVSHOWS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_TVSHOWS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_TVSHOWS;

        /** Build {@link Uri} for tvshows list. */
        public static Uri buildTVShowsListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_TVSHOWS)
                        .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildTVShowUri(long hostId, long tvshowId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_TVSHOWS)
                        .appendPath(String.valueOf(tvshowId))
                        .build();
        }

        /** Read {@link #_ID} from {@link TVShows} {@link Uri}. */
        public static String getTVShowId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, TVSHOWID, FANART, THUMBNAIL, PLAYCOUNT, TITLE, DATEADDED,
                FILE, PLOT, EPISODE, IMDBNUMBER, MPAA, PREMIERED, RATING, STUDIO,
                WATCHEDEPISODES, GENRES
        };
    }

    /**
     * Columns for TVShowCast table
     * For XBMC reference/unique key use HOST_ID + TVSHOWID + NAME
     */
    public interface TVShowCastColumns {
        String HOST_ID = "host_id";
        String TVSHOWID = "tvshowid";
        String NAME = "name";

        String ORDER = "cast_order";
        String ROLE = "role";
        String THUMBNAIL = "thumbnail";
    }

    public static class TVShowCast implements BaseColumns, SyncColumns, TVShowCastColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TVSHOW_CAST).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_TVSHOW_CAST;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_TVSHOW_CAST;

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildTVShowCastListUri(long hostId, long tvshowId) {
            return TVShows.buildTVShowUri(hostId, tvshowId).buildUpon()
                         .appendPath(PATH_TVSHOW_CAST)
                         .build();
        }
    }

    /**
     * Columns for Seasons table
     * For XBMC reference/unique key use HOST_ID + TVSHOWID + SEASON
     */
    public interface SeasonsColumns {
       String HOST_ID = "host_id";
        String TVSHOWID = "tvshowid";
        String SEASON = "season";

        String LABEL = "label";
        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String EPISODE = "episode";
        String SHOWTITLE = "showtitle";
        String WATCHEDEPISODES = "watchedepisodes";
    }

    public static class Seasons implements BaseColumns, SyncColumns, SeasonsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEASONS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_SEASONS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_SEASONS;


        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildTVShowSeasonsListUri(long hostId, long tvshowId) {
            return TVShows.buildTVShowUri(hostId, tvshowId).buildUpon()
                          .appendPath(PATH_SEASONS)
                          .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildTVShowSeasonUri(long hostId, long tvshowId, long season) {
            return TVShows.buildTVShowUri(hostId, tvshowId).buildUpon()
                        .appendPath(PATH_SEASONS)
                        .appendPath(String.valueOf(season))
                        .build();
        }

        /** Read {@link #_ID} from {@link Seasons} {@link Uri}. */
        public static String getTVShowSeasonId(Uri uri) {
            return uri.getPathSegments().get(5);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, TVSHOWID, SEASON, LABEL, FANART, THUMBNAIL, EPISODE,
                SHOWTITLE, WATCHEDEPISODES,
        };
    }

    /**
     * Columns for Episodes table
     * For XBMC reference/unique key use HOST_ID + EPISODEID
     */
    public interface EpisodesColumns {
        String HOST_ID = "host_id";
        String EPISODEID = "episodeid";

        String TVSHOWID = "tvshowid";
        String SEASON = "season";
        String EPISODE = "episode";

        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String PLAYCOUNT = "playcount";
        String TITLE = "title";
        String DATEADDED = "dateadded";
        String FILE = "file";
        String PLOT = "plot";
        String DIRECTOR = "director";
        String RUNTIME = "runtime";
        String FIRSTAIRED = "firstaired";
        String RATING = "rating";
        String SHOWTITLE = "showtitle";
        String WRITER = "writer";
        String AUDIO_CHANNELS = "audio_channels";
        String AUDIO_CODEC = "audio_coded";
        String AUDIO_LANGUAGE = "audio_language";
        String SUBTITLES_LANGUAGES = "subtitles_languages";
        String VIDEO_ASPECT = "video_aspect";
        String VIDEO_CODEC = "video_codec";
        String VIDEO_HEIGHT = "video_height";
        String VIDEO_WIDTH = "video_width";
    }

    public static class Episodes implements BaseColumns, SyncColumns, EpisodesColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_EPISODES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_EPISODES;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_EPISODES;

        /** Build {@link Uri} for tvshows list. */
        public static Uri buildTVShowEpisodesListUri(long hostId, long tvshowId) {
            return TVShows.buildTVShowUri(hostId, tvshowId).buildUpon()
                          .appendPath(PATH_EPISODES)
                          .build();
        }

        /** Build {@link Uri} for tvshows for a season list. */
        public static Uri buildTVShowSeasonEpisodesListUri(long hostId, long tvshowId, long season) {
            return Seasons.buildTVShowSeasonUri(hostId, tvshowId, season).buildUpon()
                          .appendPath(PATH_EPISODES)
                          .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildTVShowEpisodeUri(long hostId, long tvshowId, long episodeId) {
            return TVShows.buildTVShowUri(hostId, tvshowId).buildUpon()
                          .appendPath(PATH_EPISODES)
                          .appendPath(String.valueOf(episodeId))
                          .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildTVShowSeasonEpisodeUri(long hostId, long tvshowId,
                                                      long season, long episodeId) {
            return Seasons.buildTVShowSeasonUri(hostId, tvshowId, season).buildUpon()
                          .appendPath(PATH_EPISODES)
                          .appendPath(String.valueOf(episodeId))
                          .build();
        }

        /** Read {@link #_ID} from {@link Episodes} {@link Uri}. */
        public static String getTVShowEpisodeId(Uri uri) {
            return uri.getPathSegments().get(5);
        }

        /** Read {@link #_ID} from {@link Episodes} {@link Uri}. */
        public static String getTVShowSeasonEpisodeId(Uri uri) {
            return uri.getPathSegments().get(7);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, EPISODEID, TVSHOWID, SEASON, EPISODE, FANART, THUMBNAIL,
                PLAYCOUNT, TITLE, DATEADDED, FILE, PLOT, DIRECTOR, RUNTIME, FIRSTAIRED, RATING,
                SHOWTITLE, WRITER, AUDIO_CHANNELS, AUDIO_CODEC, AUDIO_LANGUAGE,
                SUBTITLES_LANGUAGES, VIDEO_ASPECT, VIDEO_CODEC, VIDEO_HEIGHT, VIDEO_WIDTH,
        };
    }

    /**
     * Columns for Artists table
     * For XBMC reference/unique key use HOST_ID + ARTISTID
     */
    public interface ArtistsColumns {
        String HOST_ID = "host_id";
        String ARTISTID = "artistid";

        String ARTIST = "artist";
        String DESCRIPTION = "description";
        String GENRE = "genre";
        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
    }

    public static class Artists implements BaseColumns, SyncColumns, ArtistsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTISTS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_ARTISTS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_ARTISTS;

        /** Build {@link Uri} for artists list. */
        public static Uri buildArtistsListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ARTISTS)
                        .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildArtistUri(long hostId, long artistId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ARTISTS)
                        .appendPath(String.valueOf(artistId))
                        .build();
        }

        /** Read {@link #_ID} from {@link Artists} {@link Uri}. */
        public static String getArtistId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, ARTISTID, ARTIST, DESCRIPTION, GENRE, FANART, THUMBNAIL,
        };
    }

    /**
     * Columns for Albums table
     * For XBMC reference/unique key use HOST_ID + ALBUMID
     */
    public interface AlbumsColumns {
        String HOST_ID = "host_id";
        String ALBUMID = "albumid";

        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String DISPLAYARTIST = "displayartist";
        String RATING = "rating";
        String TITLE = "title";
        String YEAR = "year";
        String ALBUMLABEL = "albumlabel";
        String DESCRIPTION = "description";
        String PLAYCOUNT = "playcount";
        String GENRE = "genre";
    }

    public static class Albums implements BaseColumns, SyncColumns, AlbumsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALBUMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_ALBUMS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_ALBUMS;

        /** Build {@link Uri} for albums list. */
        public static Uri buildAlbumsListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ALBUMS)
                        .build();
        }

        /** Build {@link Uri} for albums artists list. */
        public static Uri buildAlbumArtistsListUri(long hostId, long albumId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ALBUMS)
                        .appendPath(String.valueOf(albumId))
                        .appendPath(PATH_ARTISTS)
                        .build();
        }

        /** Build {@link Uri} for albums genres list. */
        public static Uri buildAlbumGenresListUri(long hostId, long albumId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ALBUMS)
                        .appendPath(String.valueOf(albumId))
                        .appendPath(PATH_AUDIO_GENRES)
                        .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildAlbumUri(long hostId, long albumId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ALBUMS)
                        .appendPath(String.valueOf(albumId))
                        .build();
        }

        /** Read {@link #_ID} from {@link Albums} {@link Uri}. */
        public static String getAlbumId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, ALBUMID, FANART, THUMBNAIL, DISPLAYARTIST, RATING, TITLE,
                YEAR, ALBUMLABEL, DESCRIPTION, PLAYCOUNT, GENRE
        };
    }

    /**
     * Columns for Songs table
     * For XBMC reference/unique key use HOST_ID + ALBUMID + SONGID
     */
    public interface SongsColumns {
        String HOST_ID = "host_id";
        String ALBUMID = "albumid";
        String SONGID = "songid";

        String DURATION = "duration";
        String THUMBNAIL = "thumbnail";
        String FILE = "file";
        String TRACK = "track";
        String TITLE = "title";
    }

    public static class Songs implements BaseColumns, SyncColumns, SongsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SONGS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_SONGS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_SONGS;

        /** Build {@link Uri} for album songs list. */
        public static Uri buildAlbumSongsListUri(long hostId, long albumId) {
            return Albums.buildAlbumUri(hostId, albumId).buildUpon()
                        .appendPath(PATH_SONGS)
                        .build();
        }

        /** Build {@link Uri} for artists songs list. */
        public static Uri buildArtistSongsListUri(long hostId, long artistId) {
            return Artists.buildArtistUri(hostId, artistId).buildUpon()
                    .appendPath(PATH_SONGS)
                    .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildSongUri(long hostId, long albumId, long songId) {
            return Albums.buildAlbumUri(hostId, albumId).buildUpon()
                         .appendPath(PATH_SONGS)
                         .appendPath(String.valueOf(songId))
                         .build();
        }

        public static Uri buildSongsListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_SONGS)
                        .build();
        }

        /** Read {@link #_ID} from {@link Albums} {@link Uri}. */
        public static String getSongId(Uri uri) {
            return uri.getPathSegments().get(5);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, ALBUMID, SONGID, DURATION, THUMBNAIL, FILE, TRACK, TITLE,
        };
    }

    /**
     * Columns for AudioGenres table
     * For XBMC reference/unique key use HOST_ID + GENREID
     */
    public interface AudioGenresColumns {
        String HOST_ID = "host_id";
        String GENREID = "genreid";

        String THUMBNAIL = "thumbnail";
        String TITLE = "title";
    }

    public static class AudioGenres implements BaseColumns, SyncColumns, AudioGenresColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_AUDIO_GENRES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_AUDIO_GENRES;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_AUDIO_GENRES;

        /** Build {@link Uri} for genres list. */
        public static Uri buildAudioGenresListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_AUDIO_GENRES)
                        .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildAudioGenreUri(long hostId, long genreId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_AUDIO_GENRES)
                        .appendPath(String.valueOf(genreId))
                        .build();
        }

        /** Read {@link #_ID} from {@link Albums} {@link Uri}. */
        public static String getAudioGenreId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, GENREID, THUMBNAIL, TITLE,
        };
    }

    /**
     * Columns for AlbumArtists table
     * All Other IDs refer to XBMC Ids, not Internal ones
     */
    public interface AlbumArtistsColumns {
        String HOST_ID = "host_id";
        String ALBUMID = "albumid";
        String ARTISTID = "artistid";
    }

    public static class AlbumArtists implements BaseColumns, AlbumArtistsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALBUM_ARTISTS).build();
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_ALBUM_ARTISTS;

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildAlbumsForArtistListUri(long hostId, long artistId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ARTISTS)
                        .appendPath(String.valueOf(artistId))
                        .appendPath(PATH_ALBUMS)
                        .build();
        }

        public final static String[] ALL_COLUMNS = {
                _ID, HOST_ID, ALBUMID, ARTISTID,
        };
    }

    /**
     * Columns for SongArtists table
     * All Other IDs refer to XBMC Ids, not Internal ones
     */
    public interface SongArtistsColumns {
        String HOST_ID = "host_id";
        String SONGID = "songid";
        String ARTISTID = "artistid";
    }

    public static class SongArtists implements BaseColumns, SongArtistsColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SONG_ARTISTS).build();
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_SONG_ARTISTS;

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildSongsForArtistListUri(long hostId, long artistId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_ARTISTS)
                        .appendPath(String.valueOf(artistId))
                        .appendPath(PATH_SONGS)
                        .build();
        }

        public final static String[] ALL_COLUMNS = {
                _ID, HOST_ID, SONGID, ARTISTID,
                };
    }

    /**
     * Columns for AlbumGenres table
     * All Other IDs refer to XBMC Ids, not Internal ones
     */
    public interface AlbumGenresColumns {
        String HOST_ID = "host_id";
        String ALBUMID = "albumid";
        String GENREID = "genreid";
    }

    public static class AlbumGenres implements BaseColumns, AlbumGenresColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALBUM_GENRES).build();
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_ALBUM_GENRES;

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildAlbumsForGenreListUri(long hostId, long genreId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_AUDIO_GENRES)
                        .appendPath(String.valueOf(genreId))
                        .appendPath(PATH_ALBUMS)
                        .build();
        }

        public final static String[] ALL_COLUMNS = {
                _ID, HOST_ID, ALBUMID, GENREID,
        };
    }

    /**
     * Columns for table MusicVideos
     * For XBMC reference/unique key use HOST_ID + MUSICVIDEOID
     */
    public interface MusicVideosColumns {
        String HOST_ID = "host_id";
        String MUSICVIDEOID = "musicvideoid";

        // ItemType.DetailsBase
        //String LABEL = "label";

        // MediaType.DetailsBase
        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";

        // DetailsBase
        //String ART = "art";
        String PLAYCOUNT = "playcount";

        // DetailsMedia
        String TITLE = "title";

        // DetailsItem
        //String DATEADDED = "dateadded";
        String FILE = "file";
        //String LASTPLAYED = "lastplayed";
        String PLOT = "plot";

        // DetailsFile
        String DIRECTOR = "director";
        //String RESUME = "resume";
        String RUNTIME = "runtime";
        //String STREAMDETAILS = "streamdetails";
        String AUDIO_CHANNELS = "audio_channels";
        String AUDIO_CODEC = "audio_coded";
        String AUDIO_LANGUAGE = "audio_language";
        String SUBTITLES_LANGUAGES = "subtitles_languages";
        String VIDEO_ASPECT = "video_aspect";
        String VIDEO_CODEC = "video_codec";
        String VIDEO_HEIGHT = "video_height";
        String VIDEO_WIDTH = "video_width";

        // MusicVideo
        String ALBUM = "album";
        String ARTIST = "artist";
        String GENRES = "genre";
        String STUDIOS = "studio";
        String TAG = "tag";
        String TRACK = "track";
        String YEAR = "year";
    }

    public static class MusicVideos implements BaseColumns, SyncColumns, MusicVideosColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MUSIC_VIDEOS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.xbmc." + PATH_MUSIC_VIDEOS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.org.xbmc." + PATH_MUSIC_VIDEOS;

        /** Build {@link Uri} for music videos list. */
        public static Uri buildMusicVideosListUri(long hostId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_MUSIC_VIDEOS)
                        .build();
        }

        /** Build {@link Uri} for requested {@link #_ID}. */
        public static Uri buildMusicVideoUri(long hostId, long musicVideoId) {
            return Hosts.buildHostUri(hostId).buildUpon()
                        .appendPath(PATH_MUSIC_VIDEOS)
                        .appendPath(String.valueOf(musicVideoId))
                        .build();
        }

        /** Read {@link #_ID} from {@link MusicVideos} {@link Uri}. */
        public static String getMusicVideoId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public final static String[] ALL_COLUMNS = {
                _ID, UPDATED, HOST_ID, MUSICVIDEOID, FANART, THUMBNAIL, PLAYCOUNT, TITLE, FILE,
                PLOT, DIRECTOR, RUNTIME, AUDIO_CHANNELS, AUDIO_CODEC, AUDIO_LANGUAGE,
                SUBTITLES_LANGUAGES, VIDEO_ASPECT, VIDEO_CODEC, VIDEO_HEIGHT, VIDEO_WIDTH,
                ALBUM, ARTIST, GENRES, STUDIOS, TAG, TRACK, YEAR
        };
    }

}
