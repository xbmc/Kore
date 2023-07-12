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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.RemoteControlPadBinding;
import org.xbmc.kore.ui.viewgroups.SquareGridLayout;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.RepeatListener;
import org.xbmc.kore.utils.UIUtils;

public class ControlPad extends SquareGridLayout
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = LogUtils.makeLogTag(ControlPad.class);

    private static final int initialButtonRepeatInterval = 400; // ms
    private static final int buttonRepeatInterval = 80; // ms

    public interface OnPadButtonsListener {
        void leftButtonClicked();
        void rightButtonClicked();
        void upButtonClicked();
        void downButtonClicked();
        void selectButtonClicked();
        void backButtonClicked();
        void infoButtonClicked();
        boolean infoButtonLongClicked();
        void contextButtonClicked();
        void osdButtonClicked();
    }

    private OnPadButtonsListener onPadButtonsListener;

    private RemoteControlPadBinding binding;

    public ControlPad(Context context) {
        super(context);
        initializeView(context);
    }

    public ControlPad(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView(context);
    }

    public ControlPad(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeView(context);
    }

    @Override
    public void setOnClickListener(@Nullable View.OnClickListener l) {
        throw new Error("Use setOnPadButtonsListener(listener)");
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        throw new Error("Use setOnPadButtonsListener(listener)");
    }

    public void setOnPadButtonsListener(OnPadButtonsListener onPadButtonsListener) {
        this.onPadButtonsListener = onPadButtonsListener;
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = RemoteControlPadBinding.inflate(inflater, this);
        setupListeners(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onPadButtonsListener = null;
        binding = null;
    }

    @Override
    public void onClick(View v) {
        if (onPadButtonsListener == null)
            return;
        int viewId = v.getId();
        if (viewId == R.id.select) {
            onPadButtonsListener.selectButtonClicked();
        } else if (viewId == R.id.left) {
            onPadButtonsListener.leftButtonClicked();
        } else if (viewId == R.id.right) {
            onPadButtonsListener.rightButtonClicked();
        } else if (viewId == R.id.up) {
            onPadButtonsListener.upButtonClicked();
        } else if (viewId == R.id.down) {
            onPadButtonsListener.downButtonClicked();
        } else if (viewId == R.id.back) {
            onPadButtonsListener.backButtonClicked();
        } else if (viewId == R.id.info) {
            onPadButtonsListener.infoButtonClicked();
        } else if (viewId == R.id.context) {
            onPadButtonsListener.contextButtonClicked();
        } else if (viewId == R.id.osd) {
            onPadButtonsListener.osdButtonClicked();
        } else {
            LogUtils.LOGD(TAG, "Unknown button " + viewId + " clicked");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if ((onPadButtonsListener != null) && (v.getId() == R.id.info)) {
            return onPadButtonsListener.infoButtonLongClicked();
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners(Context context) {
        final Animation buttonInAnim = AnimationUtils.loadAnimation(context, R.anim.button_in);
        final Animation buttonOutAnim = AnimationUtils.loadAnimation(context, R.anim.button_out);

        RepeatListener repeatListener = new RepeatListener(initialButtonRepeatInterval,
                                                           buttonRepeatInterval, this,
                                                           buttonInAnim, buttonOutAnim, getContext());

        OnTouchListener feedbackTouchListener = (v, event) -> {
            UIUtils.handleVibration(context, v, event.getAction());
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    buttonInAnim.setFillAfter(true);
                    v.startAnimation(buttonInAnim);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.startAnimation(buttonOutAnim);
                    break;
            }
            return false;
        };

        binding.left.setOnTouchListener(repeatListener);
        binding.right.setOnTouchListener(repeatListener);
        binding.up.setOnTouchListener(repeatListener);
        binding.down.setOnTouchListener(repeatListener);
        setupButton(binding.select, feedbackTouchListener);
        setupButton(binding.back, feedbackTouchListener);
        setupButton(binding.info, feedbackTouchListener);
        setupButton(binding.context, feedbackTouchListener);
        setupButton(binding.osd, feedbackTouchListener);
    }

    private void setupButton(View button, OnTouchListener feedbackTouchListener) {
        button.setOnTouchListener(feedbackTouchListener);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);
    }
}
