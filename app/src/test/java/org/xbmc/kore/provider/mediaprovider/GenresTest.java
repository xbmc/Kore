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
import org.xbmc.kore.testutils.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenresTest extends AbstractTestClass {
    @Test
    public void queryAllGenresTest() throws Exception {
        Uri uri = MediaContract.AudioGenres.buildAudioGenresListUri(hostInfo.getId());

        Cursor cursor = client.query(uri,
                                     new String[] {MediaContract.AudioGenresColumns.GENREID},
                                     null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 39, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor,
                                          cursor.getColumnIndex(MediaContract.AudioGenresColumns.GENREID),
                                          1, 39);
    }

    @Test
    public void queryAlbumsForGenreTest() throws Exception {
        int genreId = 13;
        Uri uri = MediaContract.AlbumGenres.buildAlbumsForGenreListUri(hostInfo.getId(), genreId);

        Cursor cursor = client.query(uri, TestValues.Album.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 31, cursor.getCount());
        TestUtils.testCursorContainsNumbers(cursor, cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                            28, 43, 47, 66, 100);
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                          50, 55);
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.Albums.ALBUMID),
                                          201, 220);
    }
}