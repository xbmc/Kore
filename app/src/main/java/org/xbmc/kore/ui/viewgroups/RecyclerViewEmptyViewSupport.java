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

package org.xbmc.kore.ui.viewgroups;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

/**
 * <p>A Recycler view using a grid layout that supports auto sizing and showing an empty view when the adapter
 * has no items.
 * </p>
 * <p>
 * You can set the column width and column count using styleables:
 * </p>
 * <ul>
 *  <li>android:columnWidth=INTEGER</li>
 *  <li>android:columnCount=INTEGER</li>
 * </ul>
 *
 * Inspired by <a href="http://blog.sqisland.com/2014/12/recyclerview-autofit-grid.html">RecyclerView: Autofit grid</a>
 */
public class RecyclerViewEmptyViewSupport extends FastScrollRecyclerView {

    public final static int AUTO_FIT = -1;

    private View emptyView;
    private OnItemClickListener onItemClickListener;
    private int columnWidth;
    private int columnCount = AUTO_FIT;
    private GridLayoutManager gridLayoutManager;
    private boolean multiColumnSupported;

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public RecyclerViewEmptyViewSupport(Context context) {
        this(context, null);
    }

    public RecyclerViewEmptyViewSupport(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("ResourceType")
    public RecyclerViewEmptyViewSupport(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setSaveEnabled(true);

        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth, android.R.attr.columnCount
            };
            TypedArray array = context.obtainStyledAttributes(
                    attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            columnCount = array.getInteger(1, AUTO_FIT);
            array.recycle();
        }

        gridLayoutManager = new GridLayoutManager(getContext(), 1);
        setLayoutManager(gridLayoutManager);
    }

    @Override
    public void setAdapter(final Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter == null)
            return;

        adapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                if (emptyView == null)
                    return;

                if (adapter.getItemCount() == 0) {
                    emptyView.setVisibility(View.VISIBLE);
                    setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onViewAdded(final View child) {
        super.onViewAdded(child);
        child.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(v, getChildAdapterPosition(child));
            }
        });
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
        multiColumnSupported = spanCount > 1;

        if (columnCount == AUTO_FIT) {
            gridLayoutManager.setSpanCount(spanCount);
        } else {
            gridLayoutManager.setSpanCount(columnCount);
        }
    }

    public boolean isMultiColumnSupported() {
        return multiColumnSupported;
    }

    /**
     * Sets the amount of columns.
     * @param count amount of columns to use. Use {@link #AUTO_FIT}
     * to calculate the amount based on available screen width
     * and the specified column width
     */
    public void setColumnCount(int count) {
        columnCount = count;
        invalidate();
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }

    public View getEmptyView() {
        return emptyView;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
