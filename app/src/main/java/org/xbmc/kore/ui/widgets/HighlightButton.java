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
import android.view.ViewTreeObserver;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.Utils;

public class HighlightButton extends AppCompatImageButton {

    private int highlightColor;
    private int defaultColor;

    private boolean highlight;

    public HighlightButton(Context context) {
        super(context);
        setStyle(context);
    }

    public HighlightButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStyle(context);
        fixThemeColorForPreLollipop(context);
    }

    public HighlightButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setStyle(context);
        fixThemeColorForPreLollipop(context);
    }

    public void setHighlight(boolean highlight) {
        if (highlight) {
            setColorFilter(highlightColor);
        } else {
            setColorFilter(defaultColor);
        }
        this.highlight = highlight;
    }

    public boolean isHighlighted() {
        return highlight;
    }

    private void setStyle(Context context) {
        if (!this.isInEditMode()) {
            TypedArray styledAttributes = context.getTheme()
                                                 .obtainStyledAttributes(new int[]{R.attr.colorAccent,
                                                                                   R.attr.defaultButtonColorFilter});
            highlightColor = styledAttributes.getColor(styledAttributes.getIndex(0),
                                                       context.getResources().getColor(R.color.accent_default));
            defaultColor = styledAttributes.getColor(styledAttributes.getIndex(1),
                                                     context.getResources().getColor(R.color.white));
            styledAttributes.recycle();
        }
    }

    /**
     * Hack!
     * Tinting is not applied on pre-lollipop devices.
     * As there is (AFAICT) no proper way to set this manually we simply
     * apply the color filter each time the view has been laid out.
     * @param context
     */
    private void fixThemeColorForPreLollipop(Context context) {
        if (Utils.isLollipopOrLater() || this.isInEditMode())
            return;

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (highlight) {
                    setColorFilter(highlightColor);
                } else {
                    setColorFilter(defaultColor);
                }
            }
        });
    }
}
