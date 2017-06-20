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

public class RepeatModeButton extends AppCompatImageButton {
    public enum MODE {
        OFF,
        ONE,
        ALL
    }

    private MODE mode;
    private static TypedArray styledAttributes;
    private static int accentDefaultColor;

    public RepeatModeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        styledAttributes = context.getTheme().obtainStyledAttributes(new int[]{
                R.attr.colorAccent,
                R.attr.iconRepeat,
                R.attr.iconRepeatOne});
        accentDefaultColor = getContext().getResources().getColor(R.color.accent_default);
    }

    public void setMode(MODE mode) {
        this.mode = mode;

        switch (mode) {
            case OFF:
                setImageResource(styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_repeat_white_24dp));
                clearColorFilter();
                break;
            case ONE:
                setImageResource(styledAttributes.getResourceId(styledAttributes.getIndex(2), R.drawable.ic_repeat_one_white_24dp));
                setColorFilter(styledAttributes.getColor(styledAttributes.getIndex(0), accentDefaultColor));
                break;
            case ALL:
                setImageResource(styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_repeat_white_24dp));
                setColorFilter(styledAttributes.getColor(styledAttributes.getIndex(0), accentDefaultColor));
                break;
        }
    }

    public MODE getMode() {
        return mode;
    }
}
