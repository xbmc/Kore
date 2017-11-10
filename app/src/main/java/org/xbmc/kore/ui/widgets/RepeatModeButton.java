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

public class RepeatModeButton extends HighlightButton {
    public enum MODE {
        OFF,
        ONE,
        ALL
    }

    private MODE mode;
    private static TypedArray styledAttributes;

    public RepeatModeButton(Context context) {
        super(context);
        setStyle(context);
    }

    public RepeatModeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStyle(context);
    }

    public RepeatModeButton(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        setStyle(context);
    }

    public void setMode(MODE mode) {
        this.mode = mode;

        switch (mode) {
            case OFF:
                setImageResource(styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_repeat_white_24dp));
                setHighlight(false);
                break;
            case ONE:
                setImageResource(styledAttributes.getResourceId(styledAttributes.getIndex(2), R.drawable.ic_repeat_one_white_24dp));
                setHighlight(true);
                break;
            case ALL:
                setImageResource(styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_repeat_white_24dp));
                setHighlight(true);
                break;
        }
    }

    public MODE getMode() {
        return mode;
    }

    private void setStyle(Context context) {
        styledAttributes = context.getTheme().obtainStyledAttributes(new int[]{
                R.attr.colorAccent,
                R.attr.iconRepeat,
                R.attr.iconRepeatOne});
    }
}
