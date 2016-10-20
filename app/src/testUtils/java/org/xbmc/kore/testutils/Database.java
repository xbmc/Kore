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
import org.xbmc.kore.jsonrpc.method.AudioLibrary;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.AudioType;
import org.xbmc.kore.jsonrpc.type.LibraryType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.SyncMusic;
import org.xbmc.kore.service.library.SyncUtils;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;

public class Database {
    public static final String TAG = LogUtils.makeLogTag(Database.class);

    public static HostInfo fill(Context context, ContentResolver contentResolver) throws ApiException, IOException {
        HostInfo hostInfo = addHost(context);

        SyncMusic syncMusic = new SyncMusic(hostInfo.getId(), null);

        insertMovies(context, contentResolver, hostInfo.getId());
        insertArtists(context, contentResolver, syncMusic);
        insertGenres(context, contentResolver, syncMusic);
        insertAlbums(context, contentResolver, syncMusic);
        insertSongs(context, contentResolver, syncMusic);

        return hostInfo;
    }

    public static void flush(ContentResolver contentResolver, HostInfo hostInfo) {
        contentResolver.delete(MediaContract.Hosts.buildHostUri(hostInfo.getId()), null, null);
    }

    private static HostInfo addHost(Context context) {
        return HostManager.getInstance(context).addHost("TestHost", "127.0.0.1", 1, 80, 9090, null,
                                                        null, "52:54:00:12:35:02", 9, false, 9777);
    }

    public static void insertMovies(Context context, ContentResolver contentResolver, int hostId)
            throws ApiException, IOException {
        VideoLibrary.GetMovies getMovies = new VideoLibrary.GetMovies();
        String result = FileUtils.readFile(context, "Video.Details.Movie.json");
        ApiList<VideoType.DetailsMovie> movieList = getMovies.resultFromJson(result);


        ContentValues movieValuesBatch[] = new ContentValues[movieList.items.size()];
        int castCount = 0;

        // Iterate on each movie
        for (int i = 0; i < movieList.items.size(); i++) {
            VideoType.DetailsMovie movie = movieList.items.get(i);
            movieValuesBatch[i] = SyncUtils.contentValuesFromMovie(hostId, movie);
            castCount += movie.cast.size();
        }

        contentResolver.bulkInsert(MediaContract.Movies.CONTENT_URI, movieValuesBatch);

        ContentValues movieCastValuesBatch[] = new ContentValues[castCount];
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

    private static void insertArtists(Context context, ContentResolver contentResolver, SyncMusic syncMusic) throws ApiException, IOException {
        AudioLibrary.GetArtists getArtists = new AudioLibrary.GetArtists(false);
        String result = FileUtils.readFile(context, "AudioLibrary.GetArtists.json");
        ArrayList<AudioType.DetailsArtist> artistList = (ArrayList) getArtists.resultFromJson(result).items;

        syncMusic.insertArtists(artistList, contentResolver);
    }

    private static void insertGenres(Context context, ContentResolver contentResolver, SyncMusic syncMusic) throws ApiException, IOException {
        AudioLibrary.GetGenres getGenres = new AudioLibrary.GetGenres();
        ArrayList<LibraryType.DetailsGenre> genreList =
                (ArrayList) getGenres.resultFromJson(FileUtils.readFile(context,
                                                                        "AudioLibrary.GetGenres.json"));

        syncMusic.insertGenresItems(genreList, contentResolver);
    }

    private static void insertAlbums(Context context, ContentResolver contentResolver, SyncMusic syncMusic) throws ApiException, IOException {
        AudioLibrary.GetAlbums getAlbums = new AudioLibrary.GetAlbums();
        String result = FileUtils.readFile(context, "AudioLibrary.GetAlbums.json");
        ArrayList<AudioType.DetailsAlbum> albumList = (ArrayList) getAlbums.resultFromJson(result).items;

        syncMusic.insertAlbumsItems(albumList, contentResolver);
    }

    private static void insertSongs(Context context, ContentResolver contentResolver, SyncMusic syncMusic) throws ApiException, IOException {
        AudioLibrary.GetSongs getSongs = new AudioLibrary.GetSongs();
        ArrayList<AudioType.DetailsSong> songList = (ArrayList)
                getSongs.resultFromJson(FileUtils.readFile(context, "AudioLibrary.GetSongs.json")).items;

        syncMusic.insertSongsItems(songList, contentResolver);
    }
}
