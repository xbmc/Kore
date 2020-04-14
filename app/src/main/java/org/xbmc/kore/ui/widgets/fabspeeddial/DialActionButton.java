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
import android.graphics.Interpolator;
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
import org.xbmc.kore.databinding.DialActionButtonBinding;

public class DialActionButton extends LinearLayout {

    private View anchorView;
    private boolean isHiding;
    private TimeInterpolator showInterpolator;
    private TimeInterpolator hideInterpolator;

    private DialActionButtonBinding binding;

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
     * @param anchorView
     */
    public void setAnchorView(View anchorView) {
        this.anchorView = anchorView;

        //Initialize animation
        long anim_duration = anchorView.animate().getDuration();



        binding.dialLabel.setAlpha(0f);
        binding.dialLabel.animate().setDuration(anim_duration);
        binding.dialLabel.setScaleX(0f);
        binding.dialLabel.setScaleY(0f);

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

            binding.dialLabel.animate().setInterpolator(showInterpolator);
            binding.dialLabel.setX(anchorView.getX());
            binding.dialLabel.animate().translationX(0);
            binding.dialLabel.animate().alpha(1f);
            binding.dialLabel.animate().scaleX(1f);
            binding.dialLabel.animate().scaleY(1f);
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

            binding.dialLabel.animate().setInterpolator(hideInterpolator);
            binding.dialLabel.animate().translationX(anchorView.getX() - binding.dialLabel.getX());
            binding.dialLabel.animate().alpha(0f);
            binding.dialLabel.animate().scaleX(0f);
            binding.dialLabel.animate().scaleY(0f);
        }
    }

    public Drawable getDrawable() {
        return binding.dialActionButton.getDrawable();
    }

    public AppCompatTextView getLabel() {
        return binding.dialLabel;
    }

    public void setColorFilter(int color) {
        binding.dialActionButton.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        binding.dialLabel.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void initializeView(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        binding = DialActionButtonBinding.inflate(inflater, this);

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
            binding.dialLabel.setText(text);
        } else {
            binding.dialLabel.setVisibility(View.GONE);
        }

        TypedValue typedValue = new TypedValue();
        typedArray.getValue(1, typedValue);
        binding.dialActionButton.setImageResource(typedValue.resourceId);

        typedArray.recycle();

        ColorStateList colorStateList = AppCompatResources.getColorStateList(context, R.color.fabspeeddial);
        binding.dialActionButton.setBackgroundTintList(colorStateList);
    }
}
