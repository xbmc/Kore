/*
 * Copyright 2012 Google Inc.
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

/*
 * Modifications:
 * -Imported from AOSP frameworks/base/core/java/com/android/internal/content
 * -Changed package name
 */

package org.xbmc.kore.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper for building selection clauses for {@link android.database.sqlite.SQLiteDatabase}. Each
 * appended clause is combined using {@code AND}. This class is <em>not</em>
 * thread safe.
 */
public class SelectionBuilder {
    private static final String TAG = LogUtils.makeLogTag(SelectionBuilder.class);

    private String mTable = null;
    private final Map<String, String> mProjectionMap = new HashMap<>();
    private final StringBuilder mSelection = new StringBuilder();
    private final ArrayList<String> mSelectionArgs = new ArrayList<>();
    private final StringBuilder mGroupBy = new StringBuilder();
    /**
     * Reset any internal state, allowing this builder to be recycled.
     */
    public SelectionBuilder reset() {
        mTable = null;
        mSelection.setLength(0);
        mSelectionArgs.clear();
        return this;
    }

    public SelectionBuilder groupBy(String... groupByArgs) {
        if (groupByArgs != null) {
            if (mGroupBy.length() > 0)
                mGroupBy.append(", ");

            int size = groupByArgs.length - 1;
            for (int i = 0; i < size; i++) {
                mGroupBy.append(groupByArgs[i]).append(", ");
            }
            mGroupBy.append(groupByArgs[size]);
        }
        return this;
    }

    /**
     * Append the given selection clause to the internal state. Each clause is
     * surrounded with parenthesis and combined using {@code AND}.
     */
    public SelectionBuilder where(String selection, String... selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                throw new IllegalArgumentException(
                        "Valid selection required when including arguments=");
            }

            // Shortcut when clause is empty
            return this;
        }

        if (mSelection.length() > 0) {
            mSelection.append(" AND ");
        }

        mSelection.append("(").append(selection).append(")");
        if (selectionArgs != null) {
            Collections.addAll(mSelectionArgs, selectionArgs);
        }

        return this;
    }

    public SelectionBuilder table(String table) {
        mTable = table;
        return this;
    }

    private void assertTable() {
        if (mTable == null) {
            throw new IllegalStateException("Table not specified");
        }
    }

    public SelectionBuilder mapToTable(String column, String table) {
        mProjectionMap.put(column, table + "." + column);
        return this;
    }

    public SelectionBuilder map(String fromColumn, String toClause) {
        mProjectionMap.put(fromColumn, toClause + " AS " + fromColumn);
        return this;
    }

    /**
     * Return selection string for current internal state.
     *
     * @see #getSelectionArgs()
     */
    public String getSelection() {
        return mSelection.toString();
    }

    /**
     * Return selection arguments for current internal state.
     *
     * @see #getSelection()
     */
    public String[] getSelectionArgs() {
        return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
    }

    private void mapColumns(String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            final String target = mProjectionMap.get(columns[i]);
            if (target != null) {
                columns[i] = target;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "SelectionBuilder[table=" + mTable + ", selection=" + getSelection()
                + ", selectionArgs=" + Arrays.toString(getSelectionArgs()) + "]";
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
        return query(db, columns, mGroupBy.toString(), null, orderBy, null);
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SQLiteDatabase db, String[] columns, String orderBy, String limit) {
        return query(db, columns, mGroupBy.toString(), null, orderBy, limit);
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SQLiteDatabase db, String[] columns, String groupBy,
            String having, String orderBy, String limit) {
        assertTable();
        if (columns != null) mapColumns(columns);
        return db.query(mTable, columns, getSelection(), getSelectionArgs(), groupBy, having,
                orderBy, limit);
    }

    /**
     * Execute update using the current internal state as {@code WHERE} clause.
     */
    public int update(SQLiteDatabase db, ContentValues values) {
        assertTable();
        return db.update(mTable, values, getSelection(), getSelectionArgs());
    }

    /**
     * Execute delete using the current internal state as {@code WHERE} clause.
     */
    public int delete(SQLiteDatabase db) {
        assertTable();
        return db.delete(mTable, getSelection(), getSelectionArgs());
    }
}
