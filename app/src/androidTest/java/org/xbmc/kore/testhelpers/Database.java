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

package org.xbmc.kore.testhelpers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.AudioType;
import org.xbmc.kore.jsonrpc.type.LibraryType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.service.library.SyncUtils;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;

public class Database {
    public static final String TAG = LogUtils.makeLogTag(Database.class);

    public static HostInfo fill(Context context) throws ApiException, IOException {
        MediaProvider mediaProvider = new MediaProvider();
        mediaProvider.setContext(context);
        mediaProvider.onCreate();

        HostInfo hostInfo = addHost(context);

        insertMovies(context, hostInfo.getId());
        insertArtists(context, hostInfo.getId());
        insertGenres(context, hostInfo.getId());
        insertAlbums(context, hostInfo.getId());
        insertSongs(context, hostInfo.getId());

        return hostInfo;
    }

    public static void flush(Context context, HostInfo hostInfo) {
        context.getContentResolver()
                .delete(MediaContract.Hosts.buildHostUri(hostInfo.getId()), null, null);
    }

    private static HostInfo addHost(Context context) {
        return HostManager.getInstance(context).addHost("TestHost", "127.0.0.1", 1, 80, 9090, null, null, "52:54:00:12:35:02", 9, false, 9777);
    }

    private static void insertMovies(Context context, int hostId) throws ApiException, IOException {
        VideoLibrary.GetMovies getMovies = new VideoLibrary.GetMovies();
        String result = Utils.readFile(context, "Video.Details.Movie.json");
        ArrayList<VideoType.DetailsMovie> movieList = (ArrayList) getMovies.resultFromJson(result);


        ContentValues[] movieValuesBatch = new ContentValues[movieList.size()];
        int castCount = 0;

        // Iterate on each movie
        for (int i = 0; i < movieList.size(); i++) {
            VideoType.DetailsMovie movie = movieList.get(i);
            movieValuesBatch[i] = SyncUtils.contentValuesFromMovie(hostId, movie);
            castCount += movie.cast.size();
        }

        context.getContentResolver().bulkInsert(MediaContract.Movies.CONTENT_URI, movieValuesBatch);

        ContentValues[] movieCastValuesBatch = new ContentValues[castCount];
        int count = 0;
        // Iterate on each movie/cast
        for (VideoType.DetailsMovie movie : movieList) {
            for (VideoType.Cast cast : movie.cast) {
                movieCastValuesBatch[count] = SyncUtils.contentValuesFromCast(hostId, cast);
                movieCastValuesBatch[count].put(MediaContract.MovieCastColumns.MOVIEID, movie.movieid);
                count++;
            }
        }

        context.getContentResolver().bulkInsert(MediaContract.MovieCast.CONTENT_URI, movieCastValuesBatch);
    }

    private static void insertArtists(Context context, int hostId) throws ApiException, IOException {
        AudioLibrary.GetArtists getArtists = new AudioLibrary.GetArtists(false);
        String result = Utils.readFile(context, "AudioLibrary.GetArtists.json");
        ArrayList<AudioType.DetailsArtist> artistList = (ArrayList) getArtists.resultFromJson(result).items;

        ContentValues[] artistValuesBatch = new ContentValues[artistList.size()];
        for (int i = 0; i < artistList.size(); i++) {
            AudioType.DetailsArtist artist = artistList.get(i);
            artistValuesBatch[i] = SyncUtils.contentValuesFromArtist(hostId, artist);
        }

        context.getContentResolver().bulkInsert(MediaContract.Artists.CONTENT_URI, artistValuesBatch);
    }

    private static void insertGenres(Context context, int hostId) throws ApiException, IOException {
        AudioLibrary.GetGenres getGenres = new AudioLibrary.GetGenres();
        ArrayList<LibraryType.DetailsGenre> genreList = (ArrayList) getGenres.resultFromJson(Utils.readFile(context, "AudioLibrary.GetGenres.json"));

        ContentValues[] genresValuesBatch = new ContentValues[genreList.size()];
        for (int i = 0; i < genreList.size(); i++) {
            LibraryType.DetailsGenre genre = genreList.get(i);
            genresValuesBatch[i] = SyncUtils.contentValuesFromAudioGenre(hostId, genre);
        }

        context.getContentResolver().bulkInsert(MediaContract.AudioGenres.CONTENT_URI, genresValuesBatch);
    }

    private static void insertAlbums(Context context, int hostId) throws ApiException, IOException {
        AudioLibrary.GetAlbums getAlbums = new AudioLibrary.GetAlbums();
        String result = Utils.readFile(context, "AudioLibrary.GetAlbums.json");
        ArrayList<AudioType.DetailsAlbum> albumList = (ArrayList) getAlbums.resultFromJson(result).items;

        ContentResolver contentResolver = context.getContentResolver();

        ContentValues[] albumValuesBatch = new ContentValues[albumList.size()];
        int artistsCount = 0, genresCount = 0;
        for (int i = 0; i < albumList.size(); i++) {
            AudioType.DetailsAlbum album = albumList.get(i);
            albumValuesBatch[i] = SyncUtils.contentValuesFromAlbum(hostId, album);

            artistsCount += album.artistid.size();
            genresCount += album.genreid.size();
        }
        contentResolver.bulkInsert(MediaContract.Albums.CONTENT_URI, albumValuesBatch);

        // Iterate on each album, collect the artists and the genres and insert them
        ContentValues[] albumArtistsValuesBatch = new ContentValues[artistsCount];
        ContentValues[] albumGenresValuesBatch = new ContentValues[genresCount];
        int artistCount = 0, genreCount = 0;
        for (AudioType.DetailsAlbum album : albumList) {
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
    }

    private static void insertSongs(Context context, int hostId) throws ApiException, IOException {
        AudioLibrary.GetSongs getSongs = new AudioLibrary.GetSongs();
        ArrayList<AudioType.DetailsSong> songList = (ArrayList) getSongs.resultFromJson(Utils.readFile(context, "AudioLibrary.GetSongs.json")).items;

        ContentValues[] songValuesBatch = new ContentValues[songList.size()];
        for (int i = 0; i < songList.size(); i++) {
            AudioType.DetailsSong song = songList.get(i);
            songValuesBatch[i] = SyncUtils.contentValuesFromSong(hostId, song);
        }
        context.getContentResolver().bulkInsert(MediaContract.Songs.CONTENT_URI, songValuesBatch);
    }
}
