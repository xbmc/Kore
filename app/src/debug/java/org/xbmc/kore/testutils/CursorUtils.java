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

public class CursorUtils {
    /**
     * Converts the current row in cursor to a string with each line
     * containing a column name and value pair.
     * @param cursor
     * @return
     */
    public static String cursorToString(Cursor cursor) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String name : cursor.getColumnNames()) {
            int index = cursor.getColumnIndex(name);
            stringBuffer.append(name + "=" + cursor.getString(index) + "\n");
        }
        return stringBuffer.toString();
    }

    /**
     * Moves cursor to first position item is found at column index
     * @param cursor
     * @param columnIndex
     * @param item integer to search for at given column index
     * @return true if item found, false otherwise
     */
    public static boolean moveCursorToFirstOccurrence(Cursor cursor, int columnIndex, int item) {
        if (( cursor == null ) || ( ! cursor.moveToFirst() ))
            return false;

        do {
            if ( cursor.getInt(columnIndex) == item )
                return true;
        } while (cursor.moveToNext());

        return false;
    }

    /**
     * Counts the occurences item is found at given column index
     * @param cursor
     * @param columnIndex
     * @param item integer to search for at given column index
     * @return amount of occurences, -1 if an error occured
     */
    public static int countOccurences(Cursor cursor, int columnIndex, int item) {
        if (( cursor == null ) || ( ! cursor.moveToFirst() ))
            return -1;

        int count = 0;
        do {
            if ( cursor.getInt(columnIndex) == item )
                count++;
        } while (cursor.moveToNext());

        return count;
    }
}
