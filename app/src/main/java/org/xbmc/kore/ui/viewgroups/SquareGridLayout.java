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
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import org.xbmc.kore.utils.LogUtils;

/**
 * The square grid layout creates a square layout that will fit inside
 * the boundaries provided by the parent layout.
 */
public class SquareGridLayout extends GridLayout {

    public SquareGridLayout(Context context) {
        super(context);
        fixForRelativeLayout();
    }

    public SquareGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        fixForRelativeLayout();
    }

    public SquareGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        fixForRelativeLayout();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        int size = Math.min(width, height);

        super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
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
