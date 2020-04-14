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

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.jsonrpc.type.AudioType;
import org.xbmc.kore.jsonrpc.type.LibraryType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Util functions for the Library Sync Service
 */
public class SyncUtils {

    public interface OnServiceListener {
        void onServiceConnected(LibrarySyncService librarySyncService);
    }

    public static final String LIST_DELIMITER = ", ";

    /**
     * Returns {@link android.content.ContentValues} from a {@link org.xbmc.kore.jsonrpc.type.VideoType.DetailsMovie} movie
     * @param hostId Host id for this movie
     * @param movie {@link org.xbmc.kore.jsonrpc.type.VideoType.DetailsMovie}
     * @return {@link android.content.ContentValues} with the movie values
     */
    public static ContentValues contentValuesFromMovie(int hostId, VideoType.DetailsMovie movie) {
        ContentValues movieValues = new ContentValues();
        movieValues.put(MediaContract.MoviesColumns.HOST_ID, hostId);
        movieValues.put(MediaContract.MoviesColumns.MOVIEID, movie.movieid);
        movieValues.put(MediaContract.MoviesColumns.FANART, movie.fanart);
        movieValues.put(MediaContract.MoviesColumns.THUMBNAIL, movie.thumbnail);
        movieValues.put(MediaContract.MoviesColumns.PLAYCOUNT, movie.playcount);
        movieValues.put(MediaContract.MoviesColumns.DATEADDED, movie.dateadded);
        movieValues.put(MediaContract.MoviesColumns.LASTPLAYED, movie.lastplayed);
        movieValues.put(MediaContract.MoviesColumns.TITLE, movie.title);
        movieValues.put(MediaContract.MoviesColumns.FILE, movie.file);
        movieValues.put(MediaContract.MoviesColumns.PLOT, movie.plot);
        movieValues.put(MediaContract.MoviesColumns.DIRECTOR, Utils.listStringConcat(movie.director, LIST_DELIMITER));
        movieValues.put(MediaContract.MoviesColumns.RUNTIME, movie.runtime);
        if (movie.streamdetails != null) {
            if (movie.streamdetails.audio.size() > 0) {
                // Get the stream with the most channels and concat all the languages
                VideoType.Streams.Audio selectedStream = movie.streamdetails.audio.get(0);
                List<String> languages = new ArrayList<>(movie.streamdetails.audio.size());
                for (int j = 0; j < movie.streamdetails.audio.size(); j++) {
                    VideoType.Streams.Audio stream = movie.streamdetails.audio.get(0);
                    if (stream.channels > selectedStream.channels) {
                        selectedStream = stream;
                    }
                    languages.add(stream.language);
                }
                movieValues.put(MediaContract.MoviesColumns.AUDIO_CHANNELS, selectedStream.channels);
                movieValues.put(MediaContract.MoviesColumns.AUDIO_CODEC, selectedStream.codec);
                movieValues.put(MediaContract.MoviesColumns.AUDIO_LANGUAGE,
                        Utils.listStringConcat(languages, LIST_DELIMITER));
            }
            if (movie.streamdetails.subtitle.size() > 0) {
                // Concat all subtitle languages
                ArrayList<String> subtitles = new ArrayList<>(movie.streamdetails.subtitle.size());
                for (int j = 0; j < movie.streamdetails.subtitle.size(); j++) {
                    subtitles.add(movie.streamdetails.subtitle.get(j).language);
                }
                movieValues.put(MediaContract.MoviesColumns.SUBTITLES_LANGUAGES,
                        Utils.listStringConcat(subtitles, LIST_DELIMITER));
            }
            if (movie.streamdetails.video.size() > 0) {
                // We're only getting the first video channel...
                movieValues.put(MediaContract.MoviesColumns.VIDEO_ASPECT,
                        movie.streamdetails.video.get(0).aspect);
                movieValues.put(MediaContract.MoviesColumns.VIDEO_CODEC,
                        movie.streamdetails.video.get(0).codec);
                movieValues.put(MediaContract.MoviesColumns.VIDEO_HEIGHT,
                        movie.streamdetails.video.get(0).height);
                movieValues.put(MediaContract.MoviesColumns.VIDEO_WIDTH,
                        movie.streamdetails.video.get(0).width);
            }
        }
        movieValues.put(MediaContract.MoviesColumns.COUNTRIES,
                Utils.listStringConcat(movie.country, LIST_DELIMITER));
        movieValues.put(MediaContract.MoviesColumns.GENRES,
                Utils.listStringConcat(movie.genre, LIST_DELIMITER));
        movieValues.put(MediaContract.MoviesColumns.IMDBNUMBER, movie.imdbnumber);
        movieValues.put(MediaContract.MoviesColumns.MPAA, movie.mpaa);
        movieValues.put(MediaContract.MoviesColumns.RATING, movie.rating);
        movieValues.put(MediaContract.MoviesColumns.SET, movie.set);
        movieValues.put(MediaContract.MoviesColumns.SETID, movie.setid);
        movieValues.put(MediaContract.MoviesColumns.STUDIOS,
                Utils.listStringConcat(movie.studio, LIST_DELIMITER));
        movieValues.put(MediaContract.MoviesColumns.TAGLINE, movie.tagline);
        movieValues.put(MediaContract.MoviesColumns.TOP250, movie.top250);
        movieValues.put(MediaContract.MoviesColumns.TRAILER, movie.trailer);
        movieValues.put(MediaContract.MoviesColumns.VOTES, movie.votes);
        movieValues.put(MediaContract.MoviesColumns.WRITERS,
                Utils.listStringConcat(movie.writer, LIST_DELIMITER));
        movieValues.put(MediaContract.MoviesColumns.YEAR, movie.year);

        return movieValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link org.xbmc.kore.jsonrpc.type.VideoType.Cast} movie cast
     * @param hostId Host id for this movie
     * @param cast {@link org.xbmc.kore.jsonrpc.type.VideoType.Cast}
     * @return {@link android.content.ContentValues} with the cast values
     */
    public static ContentValues contentValuesFromCast(int hostId, VideoType.Cast cast) {
        ContentValues castValues = new ContentValues();
        castValues.put(MediaContract.MovieCastColumns.HOST_ID, hostId);
        castValues.put(MediaContract.MovieCastColumns.NAME, cast.name);
        castValues.put(MediaContract.MovieCastColumns.ORDER, cast.order);
        castValues.put(MediaContract.MovieCastColumns.ROLE, cast.role);
        castValues.put(MediaContract.MovieCastColumns.THUMBNAIL, cast.thumbnail);

        return castValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link VideoType.DetailsTVShow} show
     * @param hostId Host id for this tvshow
     * @param tvshow {@link org.xbmc.kore.jsonrpc.type.VideoType.DetailsTVShow}
     * @return {@link android.content.ContentValues} with the tvshow values
     */
    public static ContentValues contentValuesFromTVShow(int hostId, VideoType.DetailsTVShow tvshow) {
        ContentValues tvshowValues = new ContentValues();

        tvshowValues.put(MediaContract.TVShowsColumns.HOST_ID, hostId);
        tvshowValues.put(MediaContract.TVShowsColumns.TVSHOWID, tvshow.tvshowid);
        tvshowValues.put(MediaContract.TVShowsColumns.FANART, tvshow.fanart);
        tvshowValues.put(MediaContract.TVShowsColumns.THUMBNAIL, tvshow.thumbnail);
        tvshowValues.put(MediaContract.TVShowsColumns.PLAYCOUNT, tvshow.playcount);
        tvshowValues.put(MediaContract.TVShowsColumns.TITLE, tvshow.title);
        tvshowValues.put(MediaContract.TVShowsColumns.DATEADDED, tvshow.dateadded);
        tvshowValues.put(MediaContract.TVShowsColumns.LASTPLAYED, tvshow.lastplayed);
        tvshowValues.put(MediaContract.TVShowsColumns.FILE, tvshow.file);
        tvshowValues.put(MediaContract.TVShowsColumns.PLOT, tvshow.plot);
        tvshowValues.put(MediaContract.TVShowsColumns.EPISODE, tvshow.episode);
        tvshowValues.put(MediaContract.TVShowsColumns.IMDBNUMBER, tvshow.imdbnumber);
        tvshowValues.put(MediaContract.TVShowsColumns.MPAA, tvshow.mpaa);
        tvshowValues.put(MediaContract.TVShowsColumns.PREMIERED, tvshow.premiered);
        tvshowValues.put(MediaContract.TVShowsColumns.RATING, tvshow.rating);
        tvshowValues.put(MediaContract.TVShowsColumns.STUDIO,
                Utils.listStringConcat(tvshow.studio, LIST_DELIMITER));
        tvshowValues.put(MediaContract.TVShowsColumns.WATCHEDEPISODES, tvshow.watchedepisodes);
        tvshowValues.put(MediaContract.TVShowsColumns.GENRES,
                Utils.listStringConcat(tvshow.genre, LIST_DELIMITER));

        return tvshowValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link VideoType.DetailsSeason} tv
     * show season
     * @param hostId Host id for this season
     * @param season {@link org.xbmc.kore.jsonrpc.type.VideoType.DetailsSeason}
     * @return {@link android.content.ContentValues} with the season values
     */
    public static ContentValues contentValuesFromSeason(int hostId, VideoType.DetailsSeason season) {
        ContentValues seasonValues = new ContentValues();

        seasonValues.put(MediaContract.SeasonsColumns.HOST_ID, hostId);
        seasonValues.put(MediaContract.SeasonsColumns.TVSHOWID, season.tvshowid);
        seasonValues.put(MediaContract.SeasonsColumns.SEASON, season.season);
        seasonValues.put(MediaContract.SeasonsColumns.LABEL, season.label);
        seasonValues.put(MediaContract.SeasonsColumns.FANART, season.fanart);
        seasonValues.put(MediaContract.SeasonsColumns.THUMBNAIL, season.thumbnail);
        seasonValues.put(MediaContract.SeasonsColumns.EPISODE, season.episode);
        seasonValues.put(MediaContract.SeasonsColumns.SHOWTITLE, season.showtitle);
        seasonValues.put(MediaContract.SeasonsColumns.WATCHEDEPISODES, season.watchedepisodes);
        return seasonValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link VideoType.DetailsEpisode} tv show episode
     * @param hostId Host id for this episode
     * @param episode {@link VideoType.DetailsEpisode}
     * @return {@link android.content.ContentValues} with the eppisode values
     */
    public static ContentValues contentValuesFromEpisode(int hostId, VideoType.DetailsEpisode episode) {
        ContentValues episodeValues = new ContentValues();
        episodeValues.put(MediaContract.EpisodesColumns.HOST_ID, hostId);
        episodeValues.put(MediaContract.EpisodesColumns.EPISODEID, episode.episodeid);
        episodeValues.put(MediaContract.EpisodesColumns.TVSHOWID, episode.tvshowid);
        episodeValues.put(MediaContract.EpisodesColumns.SEASON, episode.season);
        episodeValues.put(MediaContract.EpisodesColumns.EPISODE, episode.episode);

        episodeValues.put(MediaContract.EpisodesColumns.FANART, episode.fanart);
        episodeValues.put(MediaContract.EpisodesColumns.THUMBNAIL, episode.thumbnail);
        episodeValues.put(MediaContract.EpisodesColumns.PLAYCOUNT, episode.playcount);
        episodeValues.put(MediaContract.EpisodesColumns.DATEADDED, episode.dateadded);
        episodeValues.put(MediaContract.EpisodesColumns.TITLE, episode.title);
        episodeValues.put(MediaContract.EpisodesColumns.FILE, episode.file);
        episodeValues.put(MediaContract.EpisodesColumns.PLOT, episode.plot);
        episodeValues.put(MediaContract.EpisodesColumns.DIRECTOR, Utils.listStringConcat(episode.director, LIST_DELIMITER));
        episodeValues.put(MediaContract.EpisodesColumns.RUNTIME, episode.runtime);
        episodeValues.put(MediaContract.EpisodesColumns.FIRSTAIRED, episode.firstaired);
        episodeValues.put(MediaContract.EpisodesColumns.RATING, episode.rating);
        episodeValues.put(MediaContract.EpisodesColumns.SHOWTITLE, episode.showtitle);
        episodeValues.put(MediaContract.EpisodesColumns.WRITER, Utils.listStringConcat(episode.writer, LIST_DELIMITER));

        if (episode.streamdetails.audio.size() > 0) {
            // Get the stream with the most channels and concat all the languages
            VideoType.Streams.Audio selectedStream = episode.streamdetails.audio.get(0);
            List<String> languages = new ArrayList<>(episode.streamdetails.audio.size());
            for (int j = 0; j < episode.streamdetails.audio.size(); j++) {
                VideoType.Streams.Audio stream = episode.streamdetails.audio.get(0);
                if (stream.channels > selectedStream.channels) {
                    selectedStream = stream;
                }
                languages.add(stream.language);
            }
            episodeValues.put(MediaContract.EpisodesColumns.AUDIO_CHANNELS, selectedStream.channels);
            episodeValues.put(MediaContract.EpisodesColumns.AUDIO_CODEC, selectedStream.codec);
            episodeValues.put(MediaContract.EpisodesColumns.AUDIO_LANGUAGE, Utils.listStringConcat(languages, LIST_DELIMITER));
        }
        if (episode.streamdetails.subtitle.size() > 0) {
            // Concat all subtitle languages
            ArrayList<String> subtitles = new ArrayList<>(episode.streamdetails.subtitle.size());
            for (int j = 0; j < episode.streamdetails.subtitle.size(); j++) {
                subtitles.add(episode.streamdetails.subtitle.get(j).language);
            }
            episodeValues.put(MediaContract.EpisodesColumns.SUBTITLES_LANGUAGES, Utils.listStringConcat(subtitles, LIST_DELIMITER));
        }
        if (episode.streamdetails.video.size() > 0) {
            // We're only getting the first video channel...
            episodeValues.put(MediaContract.EpisodesColumns.VIDEO_ASPECT,
                    episode.streamdetails.video.get(0).aspect);
            episodeValues.put(MediaContract.EpisodesColumns.VIDEO_CODEC,
                    episode.streamdetails.video.get(0).codec);
            episodeValues.put(MediaContract.EpisodesColumns.VIDEO_HEIGHT,
                    episode.streamdetails.video.get(0).height);
            episodeValues.put(MediaContract.EpisodesColumns.VIDEO_WIDTH,
                    episode.streamdetails.video.get(0).width);
        }

        return episodeValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link AudioType.DetailsArtist} artist
     * @param hostId Host id for this movie
     * @param artist {@link AudioType.DetailsArtist}
     * @return {@link android.content.ContentValues} with the artist values
     */
    public static ContentValues contentValuesFromArtist(int hostId, AudioType.DetailsArtist artist) {
        ContentValues castValues = new ContentValues();
        castValues.put(MediaContract.ArtistsColumns.HOST_ID, hostId);
        castValues.put(MediaContract.ArtistsColumns.ARTISTID, artist.artistid);
        castValues.put(MediaContract.ArtistsColumns.ARTIST, artist.artist);
        castValues.put(MediaContract.ArtistsColumns.DESCRIPTION, artist.description);
        castValues.put(MediaContract.ArtistsColumns.GENRE,
                Utils.listStringConcat(artist.genre, LIST_DELIMITER));
        castValues.put(MediaContract.ArtistsColumns.FANART, artist.fanart);
        castValues.put(MediaContract.ArtistsColumns.THUMBNAIL, artist.thumbnail);

        return castValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link LibraryType.DetailsGenre} genre
     * @param hostId Host id for the genres
     * @param genre {@link LibraryType.DetailsGenre}
     * @return {@link android.content.ContentValues} with the genre values
     */
    public static ContentValues contentValuesFromAudioGenre(int hostId, LibraryType.DetailsGenre genre) {
        ContentValues castValues = new ContentValues();
        castValues.put(MediaContract.AudioGenres.HOST_ID, hostId);
        castValues.put(MediaContract.AudioGenres.GENREID, genre.genreid);
        castValues.put(MediaContract.AudioGenres.TITLE, genre.title);
        castValues.put(MediaContract.AudioGenres.THUMBNAIL, genre.thumbnail);

        return castValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link AudioType.DetailsAlbum} album
     * @param hostId Host id for the album
     * @param album {@link AudioType.DetailsAlbum}
     * @return {@link android.content.ContentValues} with the album values
     */
    public static ContentValues contentValuesFromAlbum(int hostId, AudioType.DetailsAlbum album) {
        ContentValues castValues = new ContentValues();
        castValues.put(MediaContract.Albums.HOST_ID, hostId);
        castValues.put(MediaContract.Albums.ALBUMID, album.albumid);
        castValues.put(MediaContract.Albums.FANART, album.fanart);
        castValues.put(MediaContract.Albums.THUMBNAIL, album.thumbnail);
        castValues.put(MediaContract.Albums.DISPLAYARTIST, album.displayartist);
        castValues.put(MediaContract.Albums.RATING, album.rating);
        castValues.put(MediaContract.Albums.TITLE, album.title);
        castValues.put(MediaContract.Albums.YEAR, album.year);
        castValues.put(MediaContract.Albums.ALBUMLABEL, album.albumlabel);
        castValues.put(MediaContract.Albums.DESCRIPTION, album.description);
        castValues.put(MediaContract.Albums.PLAYCOUNT, album.playcount);
        castValues.put(MediaContract.Albums.GENRE, Utils.listStringConcat(album.genre, LIST_DELIMITER));

        return castValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link AudioType.DetailsSong} song
     * @param hostId Host id for the song
     * @param song {@link AudioType.DetailsSong}
     * @return {@link android.content.ContentValues} with the song values
     */
    public static ContentValues contentValuesFromSong(int hostId, AudioType.DetailsSong song) {
        ContentValues songValues = new ContentValues();
        songValues.put(MediaContract.Songs.HOST_ID, hostId);
        songValues.put(MediaContract.Songs.ALBUMID, song.albumid);
        songValues.put(MediaContract.Songs.SONGID, song.songid);
        songValues.put(MediaContract.Songs.DURATION, song.duration);
        songValues.put(MediaContract.Songs.THUMBNAIL, song.thumbnail);
        songValues.put(MediaContract.Songs.FILE, song.file);
        songValues.put(MediaContract.Songs.TRACK, song.track);
        songValues.put(MediaContract.Songs.TITLE, song.title);
        songValues.put(MediaContract.Songs.DISPLAYARTIST, song.displayartist);
        songValues.put(MediaContract.Songs.DISC, song.disc);

        return songValues;
    }

    /**
     * Returns {@link android.content.ContentValues} from a {@link VideoType.DetailsMusicVideo} music video
     * @param hostId Host id
     * @param musicVideo {@link org.xbmc.kore.jsonrpc.type.VideoType.DetailsMusicVideo}
     * @return {@link android.content.ContentValues} with the music video values
     */
    public static ContentValues contentValuesFromMusicVideo(int hostId,
                                                            VideoType.DetailsMusicVideo musicVideo) {
        ContentValues musicVideoValues = new ContentValues();
        musicVideoValues.put(MediaContract.MusicVideosColumns.HOST_ID, hostId);
        musicVideoValues.put(MediaContract.MusicVideosColumns.MUSICVIDEOID, musicVideo.musicvideoid);
        musicVideoValues.put(MediaContract.MusicVideosColumns.FANART, musicVideo.fanart);
        musicVideoValues.put(MediaContract.MusicVideosColumns.THUMBNAIL, musicVideo.thumbnail);
        musicVideoValues.put(MediaContract.MusicVideosColumns.PLAYCOUNT, musicVideo.playcount);
        musicVideoValues.put(MediaContract.MusicVideosColumns.TITLE, musicVideo.title);
        musicVideoValues.put(MediaContract.MusicVideosColumns.FILE, musicVideo.file);
        musicVideoValues.put(MediaContract.MusicVideosColumns.PLOT, musicVideo.plot);
        musicVideoValues.put(MediaContract.MusicVideosColumns.DIRECTOR, Utils.listStringConcat(musicVideo.director, LIST_DELIMITER));
        musicVideoValues.put(MediaContract.MusicVideosColumns.RUNTIME, musicVideo.runtime);
        if (musicVideo.streamdetails != null) {
            if (musicVideo.streamdetails.audio.size() > 0) {
                // Get the stream with the most channels and concat all the languages
                VideoType.Streams.Audio selectedStream = musicVideo.streamdetails.audio.get(0);
                List<String> languages = new ArrayList<>(musicVideo.streamdetails.audio.size());
                for (int j = 0; j < musicVideo.streamdetails.audio.size(); j++) {
                    VideoType.Streams.Audio stream = musicVideo.streamdetails.audio.get(0);
                    if (stream.channels > selectedStream.channels) {
                        selectedStream = stream;
                    }
                    languages.add(stream.language);
                }
                musicVideoValues.put(MediaContract.MusicVideosColumns.AUDIO_CHANNELS, selectedStream.channels);
                musicVideoValues.put(MediaContract.MusicVideosColumns.AUDIO_CODEC, selectedStream.codec);
                musicVideoValues.put(MediaContract.MusicVideosColumns.AUDIO_LANGUAGE,
                        Utils.listStringConcat(languages, LIST_DELIMITER));
            }
            if (musicVideo.streamdetails.subtitle.size() > 0) {
                // Concat all subtitle languages
                ArrayList<String> subtitles = new ArrayList<>(musicVideo.streamdetails.subtitle.size());
                for (int j = 0; j < musicVideo.streamdetails.subtitle.size(); j++) {
                    subtitles.add(musicVideo.streamdetails.subtitle.get(j).language);
                }
                musicVideoValues.put(MediaContract.MusicVideosColumns.SUBTITLES_LANGUAGES,
                        Utils.listStringConcat(subtitles, LIST_DELIMITER));
            }
            if (musicVideo.streamdetails.video.size() > 0) {
                // We're only getting the first video channel...
                musicVideoValues.put(MediaContract.MusicVideosColumns.VIDEO_ASPECT,
                        musicVideo.streamdetails.video.get(0).aspect);
                musicVideoValues.put(MediaContract.MusicVideosColumns.VIDEO_CODEC,
                        musicVideo.streamdetails.video.get(0).codec);
                musicVideoValues.put(MediaContract.MusicVideosColumns.VIDEO_HEIGHT,
                        musicVideo.streamdetails.video.get(0).height);
                musicVideoValues.put(MediaContract.MusicVideosColumns.VIDEO_WIDTH,
                        musicVideo.streamdetails.video.get(0).width);
            }
        }
        musicVideoValues.put(MediaContract.MusicVideosColumns.ALBUM, musicVideo.album);
        musicVideoValues.put(MediaContract.MusicVideosColumns.ARTIST,
                Utils.listStringConcat(musicVideo.artist, LIST_DELIMITER));
        musicVideoValues.put(MediaContract.MusicVideosColumns.GENRES,
                Utils.listStringConcat(musicVideo.genre, LIST_DELIMITER));
        musicVideoValues.put(MediaContract.MusicVideosColumns.STUDIOS,
                Utils.listStringConcat(musicVideo.studio, LIST_DELIMITER));
        musicVideoValues.put(MediaContract.MusicVideosColumns.TAG,
                Utils.listStringConcat(musicVideo.tag, LIST_DELIMITER));
        musicVideoValues.put(MediaContract.MusicVideosColumns.TRACK, musicVideo.track);
        musicVideoValues.put(MediaContract.MusicVideosColumns.YEAR, musicVideo.year);

        return musicVideoValues;
    }

    /**
     * Binds to {@link LibrarySyncService} and calls {@link OnServiceListener#onServiceConnected(LibrarySyncService)} when connected
     * @param context {@link Context}
     * @param listener {@link OnServiceListener}
     * @return {@link ServiceConnection} to be able to disconnect the connection
     */
    public static ServiceConnection connectToLibrarySyncService(Context context, final OnServiceListener listener) {
        Intent intent = new Intent(context, LibrarySyncService.class);

        final ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {

                LibrarySyncService.LocalBinder binder = (LibrarySyncService.LocalBinder) service;
                LibrarySyncService librarySyncService = binder.getService();

                listener.onServiceConnected(librarySyncService);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        };

        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        return serviceConnection;
    }

    /**
     * Disconnects from a running LibrarySyncService
     * @param context {@link Context}
     * @param serviceConnection {@link ServiceConnection} that was used to connect/bind to the service
     */
    public static void disconnectFromLibrarySyncService(Context context, ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
    };

    /**
     * Returns the first {@link SyncItem} of the given type that is currently
     * syncing in a running {@link LibrarySyncService} for a specific host
     * @param service running LibrarySyncService. Use {@link #connectToLibrarySyncService(Context, OnServiceListener)} to connect to a running LibrarySyncService
     * @param hostInfo host to check for sync items currently running or queued
     * @param syncType sync type to check
     * @return The first {@link SyncItem} of the given syncType if any is running or queued, null otherwise
     */
    public static SyncItem getCurrentSyncItem(LibrarySyncService service, HostInfo hostInfo, String syncType) {
        if (service == null || hostInfo == null || syncType == null)
            return null;

        ArrayList<SyncItem> itemsSyncing = service.getItemsSyncing(hostInfo);
        if (itemsSyncing == null)
            return null;

        for (SyncItem syncItem : itemsSyncing) {
            if (syncItem.getSyncType().equals(syncType)) {
                return syncItem;
            }
        }

        return null;
    }

    /**
     * Checks if there are more items available according to the limits returned
     * @param limitsReturned
     * @return true if there are more items available, false otherwise
     */
    public static boolean moreItemsAvailable(ListType.LimitsReturned limitsReturned) {
        if (limitsReturned != null) {
            return ( limitsReturned.total - limitsReturned.end ) > 0;
        }
        return false;
    }
}
