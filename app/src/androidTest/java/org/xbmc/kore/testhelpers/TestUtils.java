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

import android.database.Cursor;

import org.xbmc.kore.ui.SongsListFragment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
}
