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
package org.xbmc.kore.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import org.xbmc.kore.R;

public class HighlightButton extends AppCompatImageButton {
    private int colorFilter;

    private boolean highlight;

    public HighlightButton(Context context) {
        super(context);
        setStyle(context);
    }

    public HighlightButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStyle(context);
    }

    public HighlightButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setStyle(context);
    }

    public void setHighlight(boolean highlight) {
        if (highlight) {
            setColorFilter(colorFilter);
        } else {
            clearColorFilter();
        }
        this.highlight = highlight;
    }

    public boolean isHighlighted() {
        return highlight;
    }

    private void setStyle(Context context) {
        TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(new int[]{
                R.attr.colorAccent});
        colorFilter = styledAttributes.getColor(styledAttributes.getIndex(0),
                                                context.getResources().getColor(R.color.accent_default));
        styledAttributes.recycle();
    }
}
