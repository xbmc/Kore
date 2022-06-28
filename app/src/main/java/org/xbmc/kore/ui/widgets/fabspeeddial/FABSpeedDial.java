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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.ui.animators.ChangeImageFadeAnimation;
import org.xbmc.kore.ui.animators.PulsateAnimation;

/*
 * The Floating Action Button Speed Dial uses a {@link FloatingActionButton} and can
 * optionally show a speed dial menu. To enable the speed dials add a listener
 * for the dials using {@link #setOnDialItemClickListener(DialListener)}.
 *
 * <p>The icons for the FAB needs to be set through your theme:
 * <ul>
 *     <li>org.xbmc.kore.R.attr.iconFABDefault sets the icon when the dials are disabled</li>
 *     <li>org.xbmc.kore.R.attr.iconFABDialsOpenClose sets the icon when the dials are enabled</li>
 * </ul>
 * </p>
 *
 * <p>
 *     The background color can be set through your theme:
 *     <ul>
 *         <li>org.xbmc.kore.R.attr.fabColorNormal sets the default color</li>
 *         <li>org.xbmc.kore.R.attr.fabColorPressed sets the pressed state color</li>
 *         <li>org.xbmc.kore.R.attr.fabColorFocus sets the focus state color</li>
 *     </ul>
 * </p>
 */
public class FABSpeedDial extends LinearLayout {
    FloatingActionButton FABMain;
    DialActionButton FABPlayLocal;
    DialActionButton FABPlayRemote;

    private final String BUNDLE_KEY_EXPANDED = "expanded";
    private final String BUNDLE_KEY_PARENT = "parent";
    private final String BUNDLE_KEY_DIALCLICKED = "dialclicked";

    private PulsateAnimation busyAnimation;
    private DialActionButton dialSelected;
    private boolean dialsVisible;
    private boolean dialsEnabled;

    private Drawable iconFABDefault;
    private Drawable iconFABOpenClose;

    private final OvershootInterpolator showDialsInterpolator = new OvershootInterpolator();
    private final AccelerateInterpolator hideDialsInterpolator = new AccelerateInterpolator();

    public interface DialListener {
        void onLocalPlayClicked();
        void onRemotePlayClicked();
    }

    private DialListener dialListener;
    private OnClickListener fabListener;

    public FABSpeedDial(Context context) {
        this(context, null);
    }

    public FABSpeedDial(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FABSpeedDial(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView(context);
    }

    public FABSpeedDial(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeView(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        dialListener = null;
        fabListener = null;
    }

    /**
     * Enables/disables the speed dials. This means that if enabled,
     * the dials will be shown if the user pressed the FAB button.
     * @param enable true to enable the dials, false to disable
     * @param animate true to use animation to change FAB icon, false to instantly change the FAB icon
     */
    public void enableSpeedDials(boolean enable, boolean animate) {
        if (dialsEnabled == enable)
            return;

        dialsEnabled = enable;

        changeFABIcon(animate);
    }

    /**
     * Add listener to handle dial button click events.
     * <br/>
     * Note: adding a listener for the dials also enables the speed dials if
     * user didn't disable usage in settings
     * @param dialListener Listener
     */
    public void setOnDialItemClickListener(DialListener dialListener) {
        this.dialListener = dialListener;

        // Disable speed dials if user disabled it through settings
        boolean disable = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getBoolean(Settings.KEY_PREF_DISABLE_LOCAL_PLAY,
                            Settings.DEFAULT_PREF_DISABLE_LOCAL_PLAY);

        enableSpeedDials(!disable, false);
    }

    /**
     * Add listener to handle FAB click events.
     * <br/>
     * Note: if the speed dials are enabled this won't be called
     * when the FAB button is pressed.
     * @param fabListener Listener
     */
    public void setOnFabClickListener(OnClickListener fabListener) {
        this.fabListener = fabListener;
    }

    /**
     * WARNING: Do not use this to set a listener for the FAB button.
     * Use {@link #setOnFabClickListener(OnClickListener)}
     * instead.
     * <br/>
     * {@inheritDoc}
     * @param l
     */
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
    }

    /**
     * Enables/disables the FAB button and starts/stops the busy animation
     * @param enable true to disable the FAB button and start the busy animation, false to enable
     *                the FAB button and stop the busy animation.
     */
    public void enableBusyAnimation(boolean enable) {
        if (enable) {
            busyAnimation.start();
            if (dialSelected != null) {
                changeFABIcon(FABMain.getDrawable(), dialSelected.getDrawable());
            }
            FABMain.setEnabled(false);
        } else {
            busyAnimation.stop();
            if (dialSelected != null) {
                changeFABIcon(true);
                dialSelected = null;
            }
            FABMain.setEnabled(true);
        }
    }

    public boolean busyAnimationIsEnabled() {
        return busyAnimation.isRunning();
    }

    public void enableLocalPlay(boolean enable) {
        FABPlayLocal.setEnabled(enable);
    }

    public void showDials(boolean show) {
        dialsVisible = show;

        if (show) {
            FABMain.animate().setInterpolator(showDialsInterpolator);
            FABMain.animate().rotation(-45f);
            FABPlayLocal.show();
            FABPlayRemote.show();
        } else {
            FABMain.animate().setInterpolator(hideDialsInterpolator);
            FABMain.animate().rotation(0f);
            FABPlayLocal.hide();
            FABPlayRemote.hide();
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_KEY_PARENT, super.onSaveInstanceState());
        bundle.putBoolean(BUNDLE_KEY_EXPANDED, dialsVisible);
        if (dialSelected != null) {
            bundle.putCharSequence(BUNDLE_KEY_DIALCLICKED, dialSelected.getLabel().getText());
        }
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state != null) {
            Bundle bundle = (Bundle) state;

            super.onRestoreInstanceState(bundle.getParcelable(BUNDLE_KEY_PARENT));
            showDials(bundle.getBoolean(BUNDLE_KEY_EXPANDED));

            CharSequence charSequence = bundle.getCharSequence(BUNDLE_KEY_DIALCLICKED);
            if ((charSequence != null) && (! charSequence.equals(FABPlayLocal.getLabel().getText()))) {
                dialSelected = FABPlayRemote;

                enableBusyAnimation(true);
            }
        }
    }

    private void initializeView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.fab_speed_dial, this);
        FABMain = view.findViewById(R.id.fabspeeddial);
        FABPlayLocal = view.findViewById(R.id.play_local);
        FABPlayRemote = view.findViewById(R.id.play_remote);

        // Makes sure shadow is not clipped
        setClipToPadding(false);

        // Makes sure translation animations do not cause clipping
        // by parent view group when moving outside its boundaries.
        // For example, when using the overshoot interpolator.
        setClipChildren(false);

        setupListeners();

        setupFABIcon(context);
        setupDial(FABPlayLocal);
        setupDial(FABPlayRemote);
    }

    private void setupFABIcon(Context context) {
        TypedValue tv = new TypedValue();

        context.getTheme().resolveAttribute(R.attr.iconFABDialsOpenClose, tv, false);
        iconFABOpenClose = AppCompatResources.getDrawable(context, tv.data);
        context.getTheme().resolveAttribute(R.attr.iconFABDefault, tv, false);
        iconFABDefault = AppCompatResources.getDrawable(context, tv.data);

        FABMain.setImageDrawable(dialsEnabled ? iconFABOpenClose : iconFABDefault);

        ColorStateList colorStateList = AppCompatResources.getColorStateList(context, R.color.fabspeeddial);
        int fabColorNormal = colorStateList.getColorForState(new int[] {android.R.attr.state_enabled},
                                                             R.attr.colorPrimaryDark);
        int fabColorPressed = colorStateList.getColorForState(new int[] {android.R.attr.state_pressed},
                                                              R.attr.colorPrimary);

        busyAnimation = new PulsateAnimation(FABMain, fabColorNormal, fabColorPressed);

        FABMain.setBackgroundTintList(colorStateList);
    }

    private void setupDial(DialActionButton dialActionButton) {
        dialActionButton.setAnchorView(FABMain);
        dialActionButton.setShowInterpolator(showDialsInterpolator);
        dialActionButton.setHideInterpolator(hideDialsInterpolator);
    }

    private void setupListeners() {
        FABMain.setOnClickListener(v -> {
            if (dialsEnabled) {
                showDials(!FABPlayLocal.isShown());
            } else {
                if (fabListener != null) {
                    fabListener.onClick(v);
                } else if (dialListener != null) {
                    /*
                     * We take remote play as default and we try to fallback if dev misconfigured
                     * the FAB in {@link org.xbmc.kore.ui.AbstractInfoFragment#setupFAB(FABSpeedDial)}.
                     * This is also needed to support disabling local playback through settings.
                     */
                    dialListener.onRemotePlayClicked();
                }
            }
        });

        FABPlayLocal.setOnClickListener(v -> {
            dialSelected = FABPlayLocal;
            if (dialListener != null) {
                dialListener.onLocalPlayClicked();
                showDials(false);
            }
        });


        FABPlayRemote.setOnClickListener(v -> {
            dialSelected = FABPlayRemote;
            if (dialListener != null) {
                dialListener.onRemotePlayClicked();
                showDials(false);
            }
        });
    }

    private ChangeImageFadeAnimation changeImageFadeAnimation;

    private void changeFABIcon(final Drawable from, final Drawable to) {
        // Cancel previous animation if any
        if (changeImageFadeAnimation != null)
            changeImageFadeAnimation.cancel();

        changeImageFadeAnimation = new ChangeImageFadeAnimation(FABMain, from, to);
        changeImageFadeAnimation.start();
    }

    /**
     * Changes the FAB icon to its default value.
     * @param animate true to use an animation to change the icon, false to change it instantly
     */
    private void changeFABIcon(boolean animate) {
        Drawable drawable = dialsEnabled ? iconFABOpenClose : iconFABDefault;

        if (animate) {
            changeFABIcon(FABMain.getDrawable(), drawable);
        } else {
            FABMain.setImageDrawable(drawable);
        }
    }
}
