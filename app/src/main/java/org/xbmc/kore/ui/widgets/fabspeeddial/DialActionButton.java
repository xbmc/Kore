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
package org.xbmc.kore.ui.widgets.fabspeeddial;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.R;

public class DialActionButton extends LinearLayout {
    AppCompatTextView label;
    FloatingActionButton button;

    private View anchorView;
    private boolean isHiding;
    private TimeInterpolator showInterpolator;
    private TimeInterpolator hideInterpolator;

    public DialActionButton(Context context) {
        this(context, null, 0);
    }

    public DialActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initializeView(context, attrs, defStyleAttr);
    }

    public void setShowInterpolator(TimeInterpolator showInterpolator) {
        this.showInterpolator = showInterpolator;
    }

    public void setHideInterpolator(TimeInterpolator hideInterpolator) {
        this.hideInterpolator = hideInterpolator;
    }

    /**
     * Sets the View from which the DialActionButtons should appear or disappear.
     * It uses the anchorView's animation duration to set the duration for
     * the DialActionButton.
     * <br/>
     * Use {@link #setShowInterpolator(TimeInterpolator)} and
     * {@link #setHideInterpolator(TimeInterpolator)} to set the appropriate interpolators
     * for this DialActionButton
     * @param anchorView View to anchor
     */
    public void setAnchorView(View anchorView) {
        this.anchorView = anchorView;

        //Initialize animation
        long anim_duration = anchorView.animate().getDuration();

        label.setAlpha(0f);
        label.animate().setDuration(anim_duration);
        label.setScaleX(0f);
        label.setScaleY(0f);

        animate().setDuration(anim_duration);
        animate().setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (isHiding) {
                    setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    public void show() {
        isHiding = false;

        setVisibility(View.VISIBLE);

        if (anchorView != null) {
            setY(anchorView.getY());
            animate().translationY(0);
            animate().setInterpolator(showInterpolator);

            label.animate().setInterpolator(showInterpolator);
            label.setX(anchorView.getX());
            label.animate().translationX(0);
            label.animate().alpha(1f);
            label.animate().scaleX(1f);
            label.animate().scaleY(1f);
        }
    }

    public void hide() {
        if (isHiding)
            return;

        if (anchorView == null) {
            setVisibility(View.GONE);
        } else {
            isHiding = true;
            animate().setInterpolator(hideInterpolator);
            animate().translationY(anchorView.getY() - getY());

            label.animate().setInterpolator(hideInterpolator);
            label.animate().translationX(anchorView.getX() - label.getX());
            label.animate().alpha(0f);
            label.animate().scaleX(0f);
            label.animate().scaleY(0f);
        }
    }

    public Drawable getDrawable() {
        return button.getDrawable();
    }

    public AppCompatTextView getLabel() {
        return label;
    }

    public void setColorFilter(int color) {
        button.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        label.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void initializeView(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dial_action_button, this);
        label = view.findViewById(R.id.dial_label);
        button = view.findViewById(R.id.dial_action_button);

        // Make sure shadow is not clipped
        setClipToPadding(false);

        // Make sure translation animations do not cause clipping
        // by parent view group when moving outside its boundaries.
        // For example, when using the overshoot interpolator.
        setClipChildren(false);

        Resources.Theme theme = getContext().getTheme();
        TypedArray typedArray = theme.obtainStyledAttributes(attrs, new int[]{android.R.attr.text,
                                                                              R.attr.iconFABDial},
                                                             defStyleAttr,
                                                             0);
        String text = typedArray.getString(0);

        if (text != null) {
            label.setText(text);
        } else {
            label.setVisibility(View.GONE);
        }

        TypedValue typedValue = new TypedValue();
        typedArray.getValue(1, typedValue);
        button.setImageResource(typedValue.resourceId);

        typedArray.recycle();

        ColorStateList colorStateList = AppCompatResources.getColorStateList(context, R.color.fabspeeddial);
        button.setBackgroundTintList(colorStateList);
    }
}
