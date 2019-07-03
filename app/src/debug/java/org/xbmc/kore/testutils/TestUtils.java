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

import android.database.Cursor;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TestUtils {
    /**
     * Tests if cursor contains all numbers from ids given column index.
     * @param cursor
     * @param columnIndex
     * @param numbers
     */
    public static void testCursorContainsNumbers(Cursor cursor, int columnIndex, int... numbers) {
        HashMap<Integer, Boolean> idsFound = new HashMap<>();
        for(int number : numbers) {
            idsFound.put(number, false);
        }

        assertTrue(cursor.moveToFirst());
        do {
            idsFound.put(cursor.getInt(columnIndex),  true);
        } while(cursor.moveToNext());

        for(Map.Entry<Integer, Boolean> entry : idsFound.entrySet() ) {
            int key = entry.getKey();
            assertTrue("Id " + key + " not found", entry.getValue());
        }
    }

    /**
     * Tests if cursor contains all numbers from start until end for given column index.
     * Including the start and end integers.
     * @param columnIndex
     * @param cursor
     * @param start
     * @param end
     */
    public static void testCursorContainsRange(Cursor cursor, int columnIndex, int start, int end) {
        HashMap<Integer, Boolean> idsFound = new HashMap<>();
        for(int i = start; i <= end; i++) {
            idsFound.put(i, false);
        }

        assertTrue(cursor.moveToFirst());
        do {
            idsFound.put(cursor.getInt(columnIndex),  true);
        } while(cursor.moveToNext());

        for(Map.Entry<Integer, Boolean> entry : idsFound.entrySet() ) {
            int key = entry.getKey();
            assertTrue("Id " + key + " not found", entry.getValue());
        }
    }

    public static Player.GetItem createMusicItem(int i, int libraryId) {
        Player.GetItem getItem = new Player.GetItem();
        getItem.addTrack(i);
        getItem.addLibraryId(libraryId);
        getItem.addAlbum("Album 1");
        getItem.addArtist("Artist 1");
        getItem.addDisplayartist("Artist 1");
        getItem.addAlbumArtist("Album Artist 1");
        getItem.addFanart("image://http%3a%2f%2fmedia.theaudiodb.com%2fimages%2fmedia%2fartist%2ffanart%2fxpptss1381301172.jpg/");
        getItem.addDuration(240);
        getItem.addFile("/Users/martijn/Projects/dummymediafiles/media/music/Artist 1/Album 1/Track " + i + ".mp3");
        getItem.addLabel("Label " + i);
        getItem.addThumbnail("");
        getItem.addTitle("Music "+ i);
        getItem.addType(Player.GetItem.TYPE.song);

        return getItem;
    }

    public static Player.GetItem createVideoItem(int i, int libraryId) {
        Player.GetItem getItem = new Player.GetItem(0);
        getItem.addTrack(i);
        getItem.addLibraryId(libraryId);
        getItem.addDirector("Director 1");
        getItem.addDescription("Description of video " + i);
        getItem.addGenre("Drama");
        getItem.addFanart("image://http%3a%2f%2fmedia.theaudiodb.com%2fimages%2fmedia%2fartist%2ffanart%2fxpptss1381301172.jpg/");
        getItem.addDuration(25);
        getItem.addFile("/Users/martijn/Projects/dummymediafiles/media/music/Artist 1/Album 1/Track " + i + ".mp3");
        getItem.addLabel("Label " + i);
        getItem.addThumbnail("");
        getItem.addTitle("Video "+ i);
        getItem.addPlot("Plot " + i);
        getItem.addYear(2009);
        getItem.addType(Player.GetItem.TYPE.movie);

        return getItem;
    }

    public static Player.GetItem createMusicVideoItem(int i, int libraryId) {
        Player.GetItem getItem = new Player.GetItem(0);
        getItem.addTrack(i);
        getItem.addLibraryId(libraryId);
        getItem.addType(Player.GetItem.TYPE.musicvideo);
        getItem.addAlbum("...Baby One More Time");
        getItem.addDirector("Nigel Dick");
        getItem.addThumbnail("image://http%3a%2f%2fwww.theaudiodb.com%2fimages%2fmedia%2falbum%2fthumb%2fbaby-one-more-time-4dcff7453745a.jpg/");
        getItem.addYear(1999);
        getItem.addTitle("(You Drive Me) Crazy");
        getItem.addLabel("(You Drive Me) Crazy");
        getItem.addDuration(201);
        getItem.addGenre("Pop");
        getItem.addPremiered("1999-01-01");

        return getItem;
    }

    public static Player.GetItem createPictureItem(int i, int libraryId) {
        Player.GetItem getItem = new Player.GetItem(0);
        getItem.addLibraryId(libraryId);
        getItem.addDescription("Description of picture " + i);
        getItem.addFile("/Users/martijn/Projects/dummymediafiles/media/music/Artist 1/Album 1/Track " + i + ".mp3");
        getItem.addYear(2008);
        getItem.addType(Player.GetItem.TYPE.picture);

        return getItem;
    }
}
