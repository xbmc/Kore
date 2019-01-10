/*
 * Copyright 2018 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.ui;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

abstract public class RecyclerViewCursorAdapter
        extends RecyclerView.Adapter<RecyclerViewCursorAdapter.CursorViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {

    protected boolean dataValid;
    private int rowIDColumn;
    protected Cursor cursor;

    @Override
    public void onBindViewHolder(CursorViewHolder holder, int position) {
        if (!dataValid) {
            throw new IllegalStateException("Cannot bind viewholder when cursor is in invalid state.");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("Could not move cursor to position " + position + " when trying to bind viewholder");
        }

        holder.bindView(cursor);
    }

    @Override
    public int getItemCount() {
        if (dataValid) {
            return cursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public long getItemId(int position) {
        if (!dataValid) {
            throw new IllegalStateException("Cursor is in an invalid state.");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("Could not move cursor to position " + position);
        }

        return cursor.getLong(rowIDColumn);
    }

    public String getSectionName(int position) {
        if (!dataValid) {
            throw new IllegalStateException("Cursor is in an invalid state.");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("Could not move cursor to position " + position);
        }

        return cursor.getString(getSectionColumnIdx()).substring(0, 1).toUpperCase();
    }

    /**
     * Should return the cursor column index that contains the corresponding field to be used
     * on the fastscroller. Should be a string!
     *
     * @return Cursor column index of the field to show in the fastscroller
     */
    abstract protected int getSectionColumnIdx();

    public void swapCursor(Cursor newCursor) {
        if (newCursor == cursor) {
            return;
        }

        if (newCursor != null) {
            cursor = newCursor;
            rowIDColumn = cursor.getColumnIndexOrThrow("_id");
            dataValid = true;
            notifyDataSetChanged();
        } else {
            notifyItemRangeRemoved(0, getItemCount());
            cursor = null;
            rowIDColumn = -1;
            dataValid = false;
        }
    }

    abstract public static class CursorViewHolder extends RecyclerView.ViewHolder {
        public CursorViewHolder(View itemView) {
            super(itemView);
            itemView.setTag(this);
        }

        /**
         * Called to update the content of {@link RecyclerView.ViewHolder#itemView} this holder holds.
         */
        abstract public void bindView(Cursor cursor);
    }
}

