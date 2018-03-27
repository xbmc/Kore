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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.viewgroups.SquareGridLayout;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.RepeatListener;
import org.xbmc.kore.utils.Utils;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.Unbinder;

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
    private Unbinder unbinder;

    @BindView(R.id.select) ImageView selectButton;
    @BindView(R.id.left) ImageView leftButton;
    @BindView(R.id.right) ImageView rightButton;
    @BindView(R.id.up) ImageView upButton;
    @BindView(R.id.down) ImageView downButton;
    @BindView(R.id.back) ImageView backButton;
    @BindView(R.id.info) ImageView infoButton;
    @BindView(R.id.context) ImageView contextButton;
    @BindView(R.id.osd) ImageView osdButton;

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
        View view = inflater.inflate(R.layout.remote_control_pad, this);
        unbinder = ButterKnife.bind(this, view);

        setBackgroundImage();
        setupListeners(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbinder.unbind();
        onPadButtonsListener = null;
    }

    @Override
    public void onClick(View v) {
        if (onPadButtonsListener == null)
            return;

        switch (v.getId()) {
            case R.id.select:
                onPadButtonsListener.selectButtonClicked();
                break;
            case R.id.left:
                onPadButtonsListener.leftButtonClicked();
                break;
            case R.id.right:
                onPadButtonsListener.rightButtonClicked();
                break;
            case R.id.up:
                onPadButtonsListener.upButtonClicked();
                break;
            case R.id.down:
                onPadButtonsListener.downButtonClicked();
                break;
            case R.id.back:
                onPadButtonsListener.backButtonClicked();
                break;
            case R.id.info:
                onPadButtonsListener.infoButtonClicked();
                break;
            case R.id.context:
                onPadButtonsListener.contextButtonClicked();
                break;
            case R.id.osd:
                onPadButtonsListener.osdButtonClicked();
                break;
            default:
                LogUtils.LOGD(TAG, "Unknown button "+v.getId()+" clicked");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if ((onPadButtonsListener != null) && (v.getId() == R.id.info)) {
            return onPadButtonsListener.infoButtonLongClicked();
        }

        return false;
    }

    @TargetApi(21)
    private void setBackgroundImage() {
        Resources.Theme theme = getContext().getTheme();
        TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {R.attr.contentBackgroundColor});
        int remoteBackgroundColor = styledAttributes.getColor(styledAttributes.getIndex(0),
                                                              getResources().getColor(R.color.dark_content_background_dim_70pct));
        styledAttributes.recycle();

        // On ICS the remote background isn't shown as the tinting isn't supported
        //int backgroundResourceId = R.drawable.remote_background_square_black_alpha;
        int backgroundResourceId = R.drawable.remote_background_square_black;
        if (Utils.isLollipopOrLater()) {
            setBackgroundTintList(ColorStateList.valueOf(remoteBackgroundColor));
            setBackgroundResource(backgroundResourceId);
        } else if (Utils.isJellybeanOrLater()) {
            BitmapDrawable background = new BitmapDrawable(getResources(),
                                                           BitmapFactory.decodeResource(getResources(), backgroundResourceId));
            background.setColorFilter(new PorterDuffColorFilter(remoteBackgroundColor, PorterDuff.Mode.SRC_IN));
            setBackground(background);
        }
    }

    private void setupListeners(Context context) {
        final Animation buttonInAnim = AnimationUtils.loadAnimation(context, R.anim.button_in);
        final Animation buttonOutAnim = AnimationUtils.loadAnimation(context, R.anim.button_out);

        RepeatListener repeatListener = new RepeatListener(initialButtonRepeatInterval,
                                                           buttonRepeatInterval, this,
                                                           buttonInAnim, buttonOutAnim, getContext());

        OnTouchListener feedbackTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
            }
        };

        leftButton.setOnTouchListener(repeatListener);
        rightButton.setOnTouchListener(repeatListener);
        upButton.setOnTouchListener(repeatListener);
        downButton.setOnTouchListener(repeatListener);
        setupButton(selectButton, feedbackTouchListener);
        setupButton(backButton, feedbackTouchListener);
        setupButton(infoButton, feedbackTouchListener);
        setupButton(contextButton, feedbackTouchListener);
        setupButton(osdButton, feedbackTouchListener);
    }

    private void setupButton(View button, OnTouchListener feedbackTouchListener) {
        button.setOnTouchListener(feedbackTouchListener);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);
    }
}
