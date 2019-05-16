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
import org.xbmc.kore.testutils.CursorUtils;
import org.xbmc.kore.testutils.TestUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AlbumsTest extends AbstractTestClass {

    @Test
    public void queryAllAlbumsTest() throws Exception {
        Uri uri = MediaContract.Albums.buildAlbumsListUri(hostInfo.getId());

        Cursor cursor = client.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 235, cursor.getCount());
        int columnIndex = cursor.getColumnIndex(MediaContract.AlbumsColumns.ALBUMID);
        TestUtils.testCursorContainsRange(cursor, columnIndex, 1, 75);
        TestUtils.testCursorContainsRange(cursor, columnIndex, 77, 82);
        TestUtils.testCursorContainsRange(cursor, columnIndex, 84, 237);
    }

    @Test
    public void queryAlbumTest() throws Exception {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(), TestValues.Album.albumId);

        Cursor cursor = client.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.Album.test(cursor);
    }

    @Test
    public void queryAlbumsForArtistTest() throws Exception {
        Uri uri = MediaContract.AlbumArtists.buildAlbumsForArtistListUri(hostInfo.getId(),
                                                                         TestValues.Artist.artistId);

        Cursor cursor = client.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.Album.test(cursor);
    }

    @Test
    public void queryAlbumsForArtistWithVariousArtistsTest() throws Exception {
        Uri uri = MediaContract.AlbumArtists.buildAlbumsForArtistListUri(hostInfo.getId(),
                                                                         TestValues.AlbumWithVariousArtists.artistId);

        Cursor cursor = client.query(uri, TestValues.AlbumWithVariousArtists.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 2, cursor.getCount());
        cursor.moveToFirst();

        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor,
                                                           cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                                           TestValues.AlbumWithVariousArtists.albumId));
        TestValues.AlbumWithVariousArtists.test(cursor);

        assertTrue(CursorUtils.moveCursorToFirstOccurrence(cursor,
                                                           cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                                           TestValues.AlbumWithVariousArtistsNoSongArtists.albumId));
        TestValues.AlbumWithVariousArtistsNoSongArtists.test(cursor);
    }

    @Test
    public void queryAlbumWithoutArtist() throws Exception {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(),
                                                     TestValues.AlbumWithoutArtist.albumId);

        Cursor cursor = client.query(uri, TestValues.AlbumWithoutArtist.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.AlbumWithoutArtist.test(cursor);
    }

    @Test
    public void queryAlbumWithMultipleArtists() throws Exception {
        Uri uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(),
                                                     TestValues.AlbumWithMultipleArtists.albumId);

        Cursor cursor = client.query(uri, TestValues.AlbumWithMultipleArtists.PROJECTION,
                                                    null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.AlbumWithMultipleArtists.test(cursor);
    }
}