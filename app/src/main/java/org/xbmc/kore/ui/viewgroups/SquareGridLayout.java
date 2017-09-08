/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import org.xbmc.kore.R;

/**
 * The square grid layout creates a square layout that will fit inside
 * the boundaries provided by the parent layout. Note that all cells
 * will have the same size.
 *
 * The attribute columnCount is available to specify the amount of columns
 * when using SquareGridLayout in a XML layout file.
 */
public class SquareGridLayout extends ViewGroup {

    private int columnCount = 1;
    private int cellSize;

    public SquareGridLayout(Context context) {
        this(context, null);
    }

    public SquareGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SquareGridLayout, 0, 0);
        setColumnCount(a.getInt(R.styleable.SquareGridLayout_columnCount, 1));
        a.recycle();
        fixForRelativeLayout();
    }

    public void setColumnCount(int columnCount) {
        if (columnCount < 1) throw new IllegalArgumentException("Column count must be 1 or more");
        this.columnCount = columnCount;
    }

    /**
     * Methods overridden to make sure we pass in the correct layout parameters for the child views
     */
    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT,
                                      MarginLayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int paddingWidth = getPaddingLeft() + getPaddingRight();
        int paddingHeight = getPaddingTop() + getPaddingBottom();

        int size;
        int padding;
        if ((width - paddingWidth) < (height - paddingHeight)) {
            size = width;
            padding = size - paddingWidth;
        } else {
            size = height;
            padding = size - paddingHeight;
        }

        for (int y = 0; y < columnCount; y++) {
            for (int x = 0; x < columnCount; x++) {
                View child = getChildAt(y * size + x);
                if (child != null) {
                    measureChildWithMargins(child,
                                            MeasureSpec.makeMeasureSpec((padding + x) / columnCount, MeasureSpec.EXACTLY),
                                            0,
                                            MeasureSpec.makeMeasureSpec((padding + y) / columnCount, MeasureSpec.EXACTLY),
                                            0);
                }
            }
        }

        setMeasuredDimension(size, size);
        cellSize = padding;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // top left is used to position child views
        left = getPaddingLeft();
        top = getPaddingTop();

        for (int y = 0; y < columnCount; y++) {
            for (int x = 0; x < columnCount; x++) {
                View child = getChildAt(y * columnCount + x);
                MarginLayoutParams childLayoutParams = (MarginLayoutParams) child.getLayoutParams();
                child.layout(left + (cellSize *  x) / columnCount + childLayoutParams.leftMargin,
                             top + (cellSize * y) / columnCount + childLayoutParams.topMargin,
                             left + (cellSize * (x+1)) / columnCount - childLayoutParams.rightMargin,
                             top + (cellSize * (y+1)) / columnCount - childLayoutParams.bottomMargin
                            );
            }
        }
    }

    /**
     * When used in a relative layout we need to set the layout parameters to
     * the correct size manually. Otherwise the grid layout will be stretched.
     */
    private void fixForRelativeLayout() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewGroup.LayoutParams pParams = getLayoutParams();

                if (pParams instanceof RelativeLayout.LayoutParams) {
                    int size = Math.min(getWidth(), getHeight());
                    pParams.width = size;
                    pParams.height = size;
                    setLayoutParams(pParams);
                }

                getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }
}