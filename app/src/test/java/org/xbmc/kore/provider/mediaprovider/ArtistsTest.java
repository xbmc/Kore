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
import static org.junit.Assert.assertTrue;


public class ArtistsTest extends AbstractTestClass {

    @Test
    public void queryAllArtistsTest() {
        Uri uri = MediaContract.Artists.buildArtistsListUri(hostInfo.getId());

        Cursor cursor = shadowContentResolver.query(uri, TestValues.Artist.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 228, cursor.getCount());
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.ArtistsColumns.ARTISTID),
                                          1, 94);
        //Artist id 95 should be missing
        TestUtils.testCursorContainsRange(cursor, cursor.getColumnIndex(MediaContract.ArtistsColumns.ARTISTID),
                                          96, 228);
    }

    @Test
    public void queryArtistTest() {
        Uri uri = MediaContract.Artists.buildArtistUri(hostInfo.getId(), TestValues.Artist.artistId);

        Cursor cursor = shadowContentResolver.query(uri, TestValues.Artist.PROJECTION, null, null, null);

        assertNotNull(cursor);
        assertEquals("cursor size ", 1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        TestValues.Artist.test(cursor);
    }
}