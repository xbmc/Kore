/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiList;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.AudioType;
import org.xbmc.kore.jsonrpc.type.LibraryType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.SyncMusic;
import org.xbmc.kore.service.library.SyncMusicVideos;
import org.xbmc.kore.service.library.SyncTVShows;
import org.xbmc.kore.service.library.SyncUtils;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;

public class Database {
    public static final String TAG = LogUtils.makeLogTag(Database.class);

    public static HostInfo fill(HostInfo hostInfo, Context context, ContentResolver contentResolver) throws ApiException, IOException {
        SyncMusic syncMusic = new SyncMusic(null);
        insertMovies(context, contentResolver, hostInfo.getId());
        insertArtists(context, contentResolver, syncMusic, hostInfo.getId());
        insertGenres(context, contentResolver, syncMusic, hostInfo.getId());
        insertAlbums(context, contentResolver, syncMusic, hostInfo.getId());
        insertSongs(context, contentResolver, syncMusic, hostInfo.getId());

        SyncTVShows syncTVShows = new SyncTVShows(hostInfo.getId(), null);
        insertTVShows(context, contentResolver, syncTVShows);

        SyncMusicVideos syncMusicVideos = new SyncMusicVideos(hostInfo.getId(), null);
        insertMusicVideos(context, contentResolver, syncMusicVideos);

        return hostInfo;
    }

    public static void flush(ContentResolver contentResolver) {
        contentResolver.delete(MediaContract.Hosts.CONTENT_URI, null, null);
    }

    public static HostInfo addHost(Context context) {
        return addHost(context, "127.0.0.1", HostConnection.PROTOCOL_TCP,
                       HostInfo.DEFAULT_HTTP_PORT, HostInfo.DEFAULT_TCP_PORT, false,
                       HostInfo.DEFAULT_KODI_VERSION_MAJOR);

    }

    public static HostInfo addHost(Context context, String hostname, int protocol, int httpPort,
                                   int tcpPort, boolean useEventServer, int kodiMajorVersion) {
        return HostManager.getInstance(context).addHost("TestHost", hostname, protocol, httpPort,
                                                        tcpPort, null, null, "52:54:00:12:35:02", 9, true,
                                                        useEventServer, HostInfo.DEFAULT_EVENT_SERVER_PORT,
                                                        kodiMajorVersion,
                                                        HostInfo.DEFAULT_KODI_VERSION_MINOR,
                                                        HostInfo.DEFAULT_KODI_VERSION_REVISION,
                                                        HostInfo.DEFAULT_KODI_VERSION_TAG,
                                                        false);
    }

    private static void insertMovies(Context context, ContentResolver contentResolver, int hostId)
            throws ApiException, IOException {
        VideoLibrary.GetMovies getMovies = new VideoLibrary.GetMovies();
        String result = FileUtils.readFile(context, "Video.Details.Movie.json");
        ApiList<VideoType.DetailsMovie> movieList = getMovies.resultFromJson(result);


        ContentValues[] movieValuesBatch = new ContentValues[movieList.items.size()];
        int castCount = 0;

        // Iterate on each movie
        for (int i = 0; i < movieList.items.size(); i++) {
            VideoType.DetailsMovie movie = movieList.items.get(i);
            movieValuesBatch[i] = SyncUtils.contentValuesFromMovie(hostId, movie);
            castCount += movie.cast.size();
        }

        contentResolver.bulkInsert(MediaContract.Movies.CONTENT_URI, movieValuesBatch);

        ContentValues[] movieCastValuesBatch = new ContentValues[castCount];
        int count = 0;
        // Iterate on each movie/cast
        for (VideoType.DetailsMovie movie : movieList.items) {
            for (VideoType.Cast cast : movie.cast) {
                movieCastValuesBatch[count] = SyncUtils.contentValuesFromCast(hostId, cast);
                movieCastValuesBatch[count].put(MediaContract.MovieCastColumns.MOVIEID, movie.movieid);
                count++;
            }
        }

        contentResolver.bulkInsert(MediaContract.MovieCast.CONTENT_URI, movieCastValuesBatch);
    }

    private static void insertArtists(Context context, ContentResolver contentResolver, SyncMusic syncMusic, int hostId) throws ApiException, IOException {
        AudioLibrary.GetArtists getArtists = new AudioLibrary.GetArtists(false);
        String result = FileUtils.readFile(context, "AudioLibrary.GetArtists.json");
        ArrayList<AudioType.DetailsArtist> artistList = (ArrayList<AudioType.DetailsArtist>) getArtists.resultFromJson(result).items;

        syncMusic.insertArtists(hostId, artistList, contentResolver);
    }

    private static void insertGenres(Context context, ContentResolver contentResolver, SyncMusic syncMusic, int hostId) throws ApiException, IOException {
        AudioLibrary.GetGenres getGenres = new AudioLibrary.GetGenres();
        ArrayList<LibraryType.DetailsGenre> genreList =
                (ArrayList<LibraryType.DetailsGenre>) getGenres.resultFromJson(FileUtils.readFile(context,
                                                                        "AudioLibrary.GetGenres.json"));

        syncMusic.insertGenresItems(hostId, genreList, contentResolver);
    }

    private static void insertAlbums(Context context, ContentResolver contentResolver, SyncMusic syncMusic, int hostId) throws ApiException, IOException {
        AudioLibrary.GetAlbums getAlbums = new AudioLibrary.GetAlbums();
        String result = FileUtils.readFile(context, "AudioLibrary.GetAlbums.json");
        ArrayList<AudioType.DetailsAlbum> albumList = (ArrayList<AudioType.DetailsAlbum>) getAlbums.resultFromJson(result).items;

        syncMusic.insertAlbumsItems(hostId, albumList, contentResolver);
    }

    private static void insertSongs(Context context, ContentResolver contentResolver, SyncMusic syncMusic, int hostId) throws ApiException, IOException {
        AudioLibrary.GetSongs getSongs = new AudioLibrary.GetSongs();
        ArrayList<AudioType.DetailsSong> songList =
                (ArrayList<AudioType.DetailsSong>) getSongs.resultFromJson(FileUtils.readFile(context, "AudioLibrary.GetSongs.json")).items;

        syncMusic.insertSongsItems(hostId, songList, contentResolver);
    }

    private static void insertTVShows(Context context, ContentResolver contentResolver, SyncTVShows syncTVShows)
            throws ApiException, IOException {
        VideoLibrary.GetTVShows getTVShows = new VideoLibrary.GetTVShows();
        String result = FileUtils.readFile(context, "VideoLibrary.GetTVShows.json");
        ArrayList<VideoType.DetailsTVShow> tvShowList = (ArrayList<VideoType.DetailsTVShow>) getTVShows.resultFromJson(result).items;

        syncTVShows.insertTVShows(tvShowList, contentResolver);

        for ( VideoType.DetailsTVShow tvShow : tvShowList ) {
            VideoLibrary.GetSeasons getSeasons = new VideoLibrary.GetSeasons(tvShow.tvshowid);
            result = FileUtils.readFile(context, "VideoLibrary.GetSeasons.json");
            ArrayList<VideoType.DetailsSeason> detailsSeasons = (ArrayList<VideoType.DetailsSeason>) getSeasons.resultFromJson(result);
            syncTVShows.insertSeason(tvShow.tvshowid, detailsSeasons, contentResolver);
        }

        VideoLibrary.GetEpisodes getEpisodes = new VideoLibrary.GetEpisodes(0);
        result = FileUtils.readFile(context, "VideoLibrary.GetEpisodes.json");
        ArrayList<VideoType.DetailsEpisode> detailsEpisodes = (ArrayList<VideoType.DetailsEpisode>) getEpisodes.resultFromJson(result);
        syncTVShows.insertEpisodes(detailsEpisodes, contentResolver);
    }

    private static void insertMusicVideos(Context context, ContentResolver contentResolver, SyncMusicVideos syncMusicVideos)
        throws ApiException, IOException {
        VideoLibrary.GetMusicVideos getMusicVideos = new VideoLibrary.GetMusicVideos();
        String result = FileUtils.readFile(context, "VideoLibrary.GetMusicVideos.json");
        ArrayList<VideoType.DetailsMusicVideo> musicVideoList = (ArrayList<VideoType.DetailsMusicVideo>) getMusicVideos.resultFromJson(result);

        syncMusicVideos.insertMusicVideos(musicVideoList, contentResolver);
    }
}
