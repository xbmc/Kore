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

package org.xbmc.kore.provider.mediaprovider;

import android.database.Cursor;
import android.net.Uri;

import org.junit.Test;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.testutils.CursorUtils;
import org.xbmc.kore.testutils.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SongsTest extends AbstractTestClass {
    @Test
    public void queryAlbumSongsTest() {
        Uri uri = MediaContract.Songs.buildAlbumSongsListUri(hostInfo.getId(), TestValues.Album.albumId);

        Cursor cursor = shadowContentResolver.query(uri, new String[] {MediaProvider.Qualified.SONGS_SONGID}, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 17, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.SongsColumns.SONGID),
                                          96, 112);
    }

    @Test
    public void queryArtistSongsTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(), TestValues.ArtistSong.artistId);

        Cursor cursor = shadowContentResolver.query(uri, TestValues.ArtistSong.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 17, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.SongsColumns.SONGID),
                                          96, 112);
        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                                           TestValues.ArtistSong.songId));

        assertTrue(cursor.moveToFirst());
        do {
            String displayArtist =
                    cursor.getString(cursor.getColumnIndex(MediaProvider.Qualified.SONGS_DISPLAYARTIST));
            assertEquals( "Found " + displayArtist + ", but should be " + TestValues.ArtistSong.displayArtist,
                          displayArtist, TestValues.ArtistSong.displayArtist);
        } while (cursor.moveToNext());
    }

    @Test
    public void queryArtistsSongWithArtistWithoutAlbumTest() {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(),
                                                              TestValues.SongWithArtistWithoutAlbum.artistId);

        Cursor cursor = shadowContentResolver.query(uri, TestValues.SongWithArtistWithoutAlbum.PROJECTION,
                                                    null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.SongWithArtistWithoutAlbum.test(cursor);
    }

    @Test
    public void queryFirstArtistSongWithMultipleArtistsTest() {
        testMultipleArtistInArtistSongsList(TestValues.SongWithMultipleArtists.firstArtistId,
                                            TestValues.SongWithMultipleArtists.songId);
    }

    @Test
    public void querySecondArtistSongWithMultipleArtistsTest() {
        testMultipleArtistInArtistSongsList(TestValues.SongWithMultipleArtists.secondArtistId,
                                            TestValues.SongWithMultipleArtists.songId);
    }

    @Test
    public void queryThirdArtistSongWithMultipleArtistsTest() {
        testMultipleArtistInArtistSongsList(TestValues.SongWithMultipleArtists.thirdArtistId,
                                            TestValues.SongWithMultipleArtists.songId);
    }

    @Test
    public void queryAllSongsTest() {
        Uri uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId());

        Cursor cursor = shadowContentResolver.query(uri,
                                                    TestValues.ArtistSong.PROJECTION,
                                                    null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1817, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                          1, 1817);
    }

    @Test
    public void queryAllSongsSongWithAlbumAndArtistTest() {
        Uri uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId());

        Cursor cursor = shadowContentResolver.query(uri,
                                                    TestValues.ArtistSong.PROJECTION,
                                                    null, null, null);

        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                                           TestValues.SongWithAlbumAndArtist.songId));
        TestValues.SongWithAlbumAndArtist.test(cursor);
    }

    @Test
    public void queryAllSongsSongWithArtistWithoutAlbumTest() {
        Uri uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId());

        Cursor cursor = shadowContentResolver.query(uri,
                                                    TestValues.ArtistSong.PROJECTION,
                                                    null, null, null);

        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                                           TestValues.SongWithArtistWithoutAlbum.songId));
        TestValues.SongWithArtistWithoutAlbum.test(cursor);
    }

    @Test
    public void queryAllSongsSongWithAlbumWithoutArtistTest() {
        Uri uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId());

        Cursor cursor = shadowContentResolver.query(uri,
                                                    TestValues.ArtistSong.PROJECTION,
                                                    null, null, null);

        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                                           TestValues.SongWithAlbumWithoutArtist.songId));
        TestValues.SongWithAlbumWithoutArtist.test(cursor);
    }

    @Test
    public void queryAllSongsSongWithMultipleArtistsTest() {
        Uri uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId());

        Cursor cursor = shadowContentResolver.query(uri,
                                                    TestValues.ArtistSong.PROJECTION,
                                                    null, null, null);

        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor, cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                                           TestValues.SongWithMultipleArtists.songId));
        TestValues.SongWithMultipleArtists.test(cursor);
    }


    @Test
    public void queryMultidiscAlbumSongsTest() {
        Uri uri = MediaContract.Songs.buildAlbumSongsListUri(hostInfo.getId(),
                                                             TestValues.MultidiscAlbumSongs.albumId);

        Cursor cursor = shadowContentResolver.query(uri, TestValues.MultidiscAlbumSongs.PROJECTION,
                                                    null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 7, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.SongsColumns.SONGID),
                                          1811, 1817);
    }

    private void testMultipleArtistInArtistSongsList(int artistId, int songId) {
        Uri uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(),
                                                              artistId);

        Cursor cursor = shadowContentResolver.query(uri, TestValues.SongWithMultipleArtists.PROJECTION,
                                                    null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 2, cursor.getCount());
        CursorUtils.moveCursorToFirstOccurrence(cursor,
                                                cursor.getColumnIndex(MediaContract.Songs.SONGID),
                                                songId);
        TestValues.SongWithMultipleArtists.test(cursor);
    }
}