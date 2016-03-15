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

package org.xbmc.kore.tests.mediaprovider;

import android.database.Cursor;

import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.ui.AlbumListFragment;
import org.xbmc.kore.ui.ArtistListFragment;
import org.xbmc.kore.ui.ArtistOverviewFragment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestValues {
    public static class Artist {
        public static int artistId = 13;
        public static String artist = "Bernstein, Charles";

        public static String[] PROJECTION = MediaContract.Artists.ALL_COLUMNS;

        public static void test(Cursor cursor) {
            assertEquals(TestValues.Artist.artistId, cursor.getInt(cursor.getColumnIndex(MediaContract.ArtistsColumns.ARTISTID)));
            assertEquals(TestValues.Artist.artist, cursor.getString(cursor.getColumnIndex(MediaContract.ArtistsColumns.ARTIST)));
        }
    }

    public static class Album {
        public static int albumId = 13;
        public static String title = "The Entity";
        public static String displayArtist = "Bernstein, Charles";
        public static int year = 1982;
        public static String genre = "Soundtrack";

        public static String[] PROJECTION = MediaContract.Albums.ALL_COLUMNS;

        public static void test(Cursor cursor) {
            int resultAlbumId = cursor.getInt(cursor.getColumnIndex(MediaContract.AlbumsColumns.ALBUMID));
            assertEquals(albumId, resultAlbumId);
            String resultTitle = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.TITLE));
            assertEquals(title, resultTitle);
            String resultArtist = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.DISPLAYARTIST));
            assertEquals(displayArtist, resultArtist);
            String resultGenre = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.GENRE));
            assertEquals(genre, resultGenre);
            int resultYear = cursor.getInt(cursor.getColumnIndex(MediaContract.AlbumsColumns.YEAR));
            assertEquals(year, resultYear);
        }
    }

    public static class AlbumWithoutArtist {
        public static int albumId = 82;
        public static String title = "The Album";
        public static String displayArtist = "";
        public static int year = 0;
        public static String genre = "";

        public static String[] PROJECTION = MediaContract.Albums.ALL_COLUMNS;

        public static void test(Cursor cursor) {
            int resultAlbumId = cursor.getInt(cursor.getColumnIndex(MediaContract.AlbumsColumns.ALBUMID));
            assertEquals(albumId, resultAlbumId);
            String resultTitle = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.TITLE));
            assertEquals(title, resultTitle);
            String resultArtist = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.DISPLAYARTIST));
            assertEquals(displayArtist, resultArtist);
            String resultGenre = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.GENRE));
            assertEquals(genre, resultGenre);
            int resultYear = cursor.getInt(cursor.getColumnIndex(MediaContract.AlbumsColumns.YEAR));
            assertEquals(year, resultYear);
        }
    }

    public static class AlbumWithMultipleArtists {
        public static int albumId = 234;
        public static String title = "ThreeArtistsAlbum";
        public static String displayArtist = "First artist / Second artist / Third artist";
        public static int year = 0;
        public static String genre = "";

        public static String[] PROJECTION = MediaContract.Albums.ALL_COLUMNS;

        public static void test(Cursor cursor) {
            int resultAlbumId = cursor.getInt(cursor.getColumnIndex(MediaContract.AlbumsColumns.ALBUMID));
            assertEquals(albumId, resultAlbumId);
            String resultTitle = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.TITLE));
            assertEquals(title, resultTitle);
            String resultArtist = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.DISPLAYARTIST));
            assertEquals(displayArtist, resultArtist);
            String resultGenre = cursor.getString(cursor.getColumnIndex(MediaContract.AlbumsColumns.GENRE));
            assertEquals(genre, resultGenre);
            int resultYear = cursor.getInt(cursor.getColumnIndex(MediaContract.AlbumsColumns.YEAR));
            assertEquals(year, resultYear);
        }
    }

    public static class ArtistSong {
        public static int songId = 96;
        public static int artistId = Artist.artistId;
        public static int albumId = Album.albumId;
        public static String title = "Intro & Main Title";
        public static String[] PROJECTION = new String[] { MediaContract.Songs.SONGID,
                                                           MediaContract.Songs.TITLE,
                                                           MediaContract.Songs.ALBUMID,
                                                           MediaContract.SongArtists.ARTISTID,
                                                           MediaContract.Artists.ARTIST };

        public static void test(Cursor cursor) {
            assertEquals(songId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.SONGID)));
            assertEquals(title, cursor.getString(cursor.getColumnIndex(MediaContract.Songs.TITLE)));
            assertEquals(albumId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.ALBUMID)));
            assertEquals(artistId, cursor.getInt(cursor.getColumnIndex(MediaContract.SongArtists.ARTISTID)));
        }
    }

    public static class SongWithAlbumAndArtist {
        public static int songId = 1487;
        public static int artistId = 195;
        public static int albumId = 201;
        public static String title = "The Lone Ranger (William Tell Overture)";

        public static String[] PROJECTION = ArtistSong.PROJECTION;

        public static void test(Cursor cursor) {
            assertEquals(songId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.SONGID)));
            assertEquals(title, cursor.getString(cursor.getColumnIndex(MediaContract.Songs.TITLE)));
            assertEquals(albumId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.ALBUMID)));
            assertEquals(artistId, cursor.getInt(cursor.getColumnIndex(MediaContract.SongArtists.ARTISTID)));
        }
    }

    public static class SongWithAlbumWithoutArtist {
        public static int songId = 1219;
        public static int artistId = 0;
        public static String title = "Unknown";
        public static int albumId = 82;

        public static String[] PROJECTION = ArtistSong.PROJECTION;

        public static void test(Cursor cursor) {
            assertEquals(songId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.SONGID)));
            assertEquals(title, cursor.getString(cursor.getColumnIndex(MediaContract.Songs.TITLE)));
            assertEquals(albumId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.ALBUMID)));
            assertEquals(artistId, cursor.getInt(cursor.getColumnIndex(MediaContract.SongArtists.ARTISTID)));

        }
    }

    public static class SongWithArtistWithoutAlbum {
        public static int songId = 1128;
        public static int artistId = 73;
        public static int albumId = 76;
        public static String title = "Unknown";

        public static String[] PROJECTION = ArtistSong.PROJECTION;

        public static void test(Cursor cursor) {
            assertEquals(songId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.SONGID)));
            assertEquals(title, cursor.getString(cursor.getColumnIndex(MediaContract.Songs.TITLE)));
            assertEquals(albumId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.ALBUMID)));
            assertEquals(artistId, cursor.getInt(cursor.getColumnIndex(MediaContract.SongArtists.ARTISTID)));
        }
    }

    public static class SongWithMultipleArtists {
        public static int songId = 1804;
        public static int firstArtistId = 226;
        public static int secondArtistId = 227;
        public static int thirdArtistId = 228;
        public static int albumId = 234;
        public static String title = "threeartists";

        public static String[] PROJECTION = ArtistSong.PROJECTION;

        public static void test(Cursor cursor) {
            assertEquals(songId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.SONGID)));
            assertEquals(title, cursor.getString(cursor.getColumnIndex(MediaContract.Songs.TITLE)));
            assertEquals(albumId, cursor.getInt(cursor.getColumnIndex(MediaContract.Songs.ALBUMID)));
        }
    }
}
