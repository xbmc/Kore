/*
 * Copyright 2019 Martijn Brekhof. All rights reserved.
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
package org.xbmc.kore.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ClipDrawable;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.xbmc.kore.R;

import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.google.android.material.color.MaterialColors;

public class RatingBar extends LinearLayoutCompat {

    private @DrawableRes int iconResourceId;
    private int iconCount;
    private double maxRating = 5;
    private final ArrayList<ClipDrawable> clipDrawables = new ArrayList<>(iconCount);
    private int backgroundColor;
    private int foregroundColor;

    private int maxTotalClipLevel;
    private final int maxClipLevel = 10000;
    private double ratingScaleFactor;

    public RatingBar(Context context) {
        this(context, null);
    }

    public RatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RatingBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeView(context, attrs, defStyle);
    }

    private void initializeView(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RatingBar, 0, 0);

        try {
            backgroundColor = a.getColor(R.styleable.RatingBar_backgroundColor,
                                         MaterialColors.getColor(context, R.attr.colorSurfaceVariant, null));
            foregroundColor = a.getColor(R.styleable.RatingBar_foregroundColor,
                                         MaterialColors.getColor(context, R.attr.colorOnSurfaceVariant, null));
            iconCount = a.getInteger(R.styleable.RatingBar_iconCount, 5);
            iconResourceId = a.getResourceId(R.styleable.RatingBar_icon, R.drawable.ic_round_star_rate_24);
        } finally {
            a.recycle();
        }

        maxTotalClipLevel = iconCount * maxClipLevel;
        ratingScaleFactor = maxTotalClipLevel / maxRating;

        for(int i = 0; i < iconCount; i++) {
            View star = createStar(context, attrs, defStyle);
            addView(star);
        }
    }

    public void setRating(double rating) {
        if (rating > this.maxRating)
            rating = this.maxRating;

        int scaledRating = (int) (rating * ratingScaleFactor);

        int fullyFilledIconsCount = scaledRating / maxClipLevel;
        for(int i = 0; i < fullyFilledIconsCount; i++) {
            clipDrawables.get(i).setLevel(maxClipLevel);
        }

        if (fullyFilledIconsCount < clipDrawables.size()) {
            clipDrawables.get(fullyFilledIconsCount).setLevel(scaledRating - (fullyFilledIconsCount * maxClipLevel));

            for (int i = fullyFilledIconsCount + 1 ; i < clipDrawables.size(); i++) {
                clipDrawables.get(i).setLevel(0);
            }
        }

        invalidate();
    }

    public void setMaxRating(double maxRating) {
        this.maxRating = maxRating;
        ratingScaleFactor = maxTotalClipLevel / maxRating;
    }

    private FrameLayout createStar(Context context, AttributeSet attrs, int defStyle) {
        FrameLayout frameLayout = new FrameLayout(getContext());
        frameLayout.setLayoutParams(new LayoutParams(WRAP_CONTENT, MATCH_PARENT));

        AppCompatImageView ivStarBackground = new AppCompatImageView(context, attrs, defStyle);
        ivStarBackground.setLayoutParams(new LayoutParams(WRAP_CONTENT, MATCH_PARENT));
        ivStarBackground.setImageResource(iconResourceId);
        ivStarBackground.setAdjustViewBounds(true);
        ImageViewCompat.setImageTintList(ivStarBackground, ColorStateList.valueOf(backgroundColor));
        frameLayout.addView(ivStarBackground);

        ClipDrawable clipDrawable = new ClipDrawable(
                ContextCompat.getDrawable(context, iconResourceId),
                Gravity.START,
                ClipDrawable.HORIZONTAL);

        AppCompatImageView ivStarForeground = new AppCompatImageView(context, attrs, defStyle);
        ivStarForeground.setLayoutParams(new LayoutParams(WRAP_CONTENT, MATCH_PARENT));
        ivStarForeground.setImageDrawable(clipDrawable);
        ivStarForeground.setAdjustViewBounds(true);
        ImageViewCompat.setImageTintList(ivStarForeground, ColorStateList.valueOf(foregroundColor));
        frameLayout.addView(ivStarForeground);

        clipDrawables.add((ClipDrawable) ivStarForeground.getDrawable());

        return frameLayout;
    }
}
