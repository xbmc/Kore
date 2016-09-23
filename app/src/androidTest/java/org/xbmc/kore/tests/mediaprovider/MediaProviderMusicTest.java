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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.testhelpers.Database;
import org.xbmc.kore.testhelpers.TestUtils;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.ui.MoviesActivity;
import org.xbmc.kore.utils.LogUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MediaProviderMusicTest {
    private static HostInfo hostInfo;
    private static Context context;
    private ContentResolver contentResolver;

    /**
     * Note that the activity MoviesActivity is only needed for context and is not tested
     */
    @Rule
    public ActivityTestRule<MoviesActivity> mActivityRule = new ActivityTestRule<>(
            MoviesActivity.class);

    @Before
    public void setUp() throws Exception {
        context = mActivityRule.getActivity();

        if (hostInfo == null) // We only need to fill the database the first time
            hostInfo = Database.fill(context);

        contentResolver = mActivityRule.getActivity().getContentResolver();
    }

    @After
    public void tearDown() throws Exception {

    }

    @AfterClass
    public static void cleanup() {
        Database.flush(context, hostInfo);
    }

    @Test
    public void queryAllArtistsTest() {
        Uri uri = MediaContract.Artists.buildArtistsListUri(hostInfo.getId());

        Cursor cursor = contentResolver.query(uri, TestValues.Artist.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 227, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.ArtistsColumns.ARTISTID),
                                          1, 94);
        //Artist id 95 should be missing
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.ArtistsColumns.ARTISTID),
                                          96, 228);
    }

    @Test
    public void queryArtistTest() {
        Uri uri = MediaContract.Artists.buildArtistUri(hostInfo.getId(), TestValues.Artist.artistId);

        Cursor cursor = contentResolver.query(uri, TestValues.Artist.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.Artist.test(cursor);
    }

    @Test
    public void queryAllAlbumsTest() {
        Uri uri = MediaContract.Albums.buildAlbumsListUri(hostInfo.getId());

        Cursor cursor = contentResolver.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 232, cursor.getCount());
        int columnIndex = cursor.getColumnIndex(MediaContract.AlbumsColumns.ALBUMID);
        TestUtils.testCursorContainsRange(cursor, columnIndex, 1, 75);
        TestUtils.testCursorContainsRange(cursor, columnIndex, 77, 82);
        TestUtils.testCursorContainsRange(cursor, columnIndex, 84, 234);
    }

    @Test
    public void queryAlbumTest() {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(), TestValues.Album.albumId);

        Cursor cursor = contentResolver.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.Album.test(cursor);
    }

    @Test
    public void queryAlbumsForArtistTest() {
        Uri uri = MediaContract.AlbumArtists.buildAlbumsForArtistListUri(hostInfo.getId(),
                                                                         TestValues.Artist.artistId);

        Cursor cursor = contentResolver.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.Album.test(cursor);
    }

    @Test
    public void queryAlbumsForGenreTest() {
        int genreId = 13;
        Uri uri = MediaContract.AlbumGenres.buildAlbumsForGenreListUri(hostInfo.getId(), genreId);

        Cursor cursor = contentResolver.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 31, cursor.getCount());
        TestUtils.testCursorContainsNumbers(cursor, cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                            28, 43, 47, 66, 100);
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                          50, 55);
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                          201, 220);
    }

    @Test
    public void queryAlbumSongsTest() {
        Uri uri = MediaContract.Songs.buildAlbumSongsListUri(hostInfo.getId(), TestValues.Album.albumId);

        Cursor cursor = contentResolver.query(uri, new String[] {MediaContract.Songs.SONGID}, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 17, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.SongsColumns.SONGID),
                                          96, 112);
    }

    @Test
    public void queryAlbumWithoutArtist() {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(),
                                                     TestValues.AlbumWithoutArtist.albumId);

        Cursor cursor = contentResolver.query(uri, TestValues.AlbumWithoutArtist.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.AlbumWithoutArtist.test(cursor);
    }

    @Test
    public void queryAlbumWithMultipleArtists() {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(),
                                                     TestValues.AlbumWithMultipleArtists.albumId);

        Cursor cursor = contentResolver.query(uri, TestValues.AlbumWithMultipleArtists.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.AlbumWithMultipleArtists.test(cursor);
    }

    @Test
    public void queryArtistSongsTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(), TestValues.ArtistSong.artistId);

        Cursor cursor = contentResolver.query(uri, TestValues.ArtistSong.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 17, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.SongsColumns.SONGID),
                                          96, 112);
        assertTrue(Utils.moveCursorTo(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                      TestValues.ArtistSong.songId));
    }

    @Test
    public void querySongWithArtistWithoutAlbumTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(),
                                                              TestValues.SongWithArtistWithoutAlbum.artistId);

        Cursor cursor = contentResolver.query(uri, TestValues.SongWithArtistWithoutAlbum.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        Utils.cursorToString(cursor);
        TestValues.SongWithArtistWithoutAlbum.test(cursor);
    }

    @Test
    public void queryFirstArtistSongWithMultipleArtistsTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(),
                                                              TestValues.SongWithMultipleArtists.firstArtistId);

        Cursor cursor = contentResolver.query(uri, TestValues.SongWithMultipleArtists.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.SongWithMultipleArtists.test(cursor);
    }

    @Test
    public void querySecondArtistSongWithMultipleArtistsTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(),
                                                              TestValues.SongWithMultipleArtists.secondArtistId);

        Cursor cursor = contentResolver.query(uri, TestValues.SongWithMultipleArtists.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.SongWithMultipleArtists.test(cursor);
    }

    @Test
    public void queryThirdArtistSongWithMultipleArtistsTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(),
                                                              TestValues.SongWithMultipleArtists.thirdArtistId);

        Cursor cursor = contentResolver.query(uri,
                                              TestValues.SongWithMultipleArtists.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.SongWithMultipleArtists.test(cursor);
    }

    @Test
    public void queryAllSongsTest() {
        Uri uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId());

         Cursor cursor = contentResolver.query(uri,
                                              TestValues.ArtistSong.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1804, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                          1, 1804);

        //Test if list also contains a song WITH an album AND an artist
        assertTrue(Utils.moveCursorTo(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                      TestValues.SongWithAlbumAndArtist.songId));
        TestValues.SongWithAlbumAndArtist.test(cursor);

        //Test if list also contains a song WITHOUT an album but WITH an artist
        assertTrue(Utils.moveCursorTo(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                      TestValues.SongWithArtistWithoutAlbum.songId));
        TestValues.SongWithArtistWithoutAlbum.test(cursor);

        //Test if list also contains a song WITH an album but WITHOUT an artist
        assertTrue(Utils.moveCursorTo(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                      TestValues.SongWithAlbumWithoutArtist.songId));
        TestValues.SongWithAlbumWithoutArtist.test(cursor);

        //Test if list contains a song WITH MULTIPLE artists
        assertTrue(Utils.moveCursorTo(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                      TestValues.SongWithMultipleArtists.songId));
        TestValues.SongWithMultipleArtists.test(cursor);
    }

    @Test
    public void queryAlbumWithMultipleArtistsTest() {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(),
                                                   TestValues.AlbumWithMultipleArtists.albumId);

        Cursor cursor = contentResolver.query(uri,
                                              TestValues.AlbumWithMultipleArtists.PROJECTION,
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        LogUtils.LOGD("MediaProviderMusicTest", Utils.cursorToString(cursor));
        TestValues.AlbumWithMultipleArtists.test(cursor);
    }

    @Test
    public void queryAllGenresTest() {
        Uri uri = MediaContract.AudioGenres.buildAudioGenresListUri(hostInfo.getId());

        Cursor cursor = contentResolver.query(uri,
                                              new String[] {MediaContract.AudioGenresColumns.GENREID},
                                              null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 39, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor,
                                          cursor.getColumnIndex(MediaContract.AudioGenresColumns.GENREID),
                                          1, 39);
    }
}