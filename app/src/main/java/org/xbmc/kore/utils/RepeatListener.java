/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;

import org.xbmc.kore.Settings;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
 * click is fired immediately, next after initialInterval, and subsequent after
 * repeatInterval.
 *
 * <p>Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks.
 */
public class RepeatListener implements View.OnTouchListener {
    private static final String TAG = LogUtils.makeLogTag(RepeatListener.class);

    private static Handler repeatHandler = new Handler();

    private int initialInterval;
    private final int repeatInterval;
    private final View.OnClickListener clickListener;

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            if (downView.isShown()) {
                if (repeatInterval >= 0) {
                    repeatHandler.postDelayed(this, repeatInterval);
                }
                clickListener.onClick(downView);
            }
        }
    };

    /**
     * Animations for down/up
     */
    private Animation animDown;
    private Animation animUp;

    private View downView;

    private Context context;
    private Vibrator vibrator;

    /**
     * Constructor for a repeat listener
     *
     * @param initialInterval The interval after first click event
     * @param repeatInterval The interval after second and subsequent click events
     * @param clickListener The OnClickListener, that will be called periodically
     */
    public RepeatListener(int initialInterval, int repeatInterval, View.OnClickListener clickListener) {
        this(initialInterval, repeatInterval, clickListener, null, null, null);
    }

    public RepeatListener(int initialInterval, int repeatInterval, View.OnClickListener clickListener,
                          Animation animDown, Animation animUp) {
        this(initialInterval, repeatInterval, clickListener, animUp, animDown, null);
    }

    /**
     * Constructor for a repeat listener, with animation and vibration
     *
     * @param initialInterval The interval after first click event. If negative, no repeat will occur
     * @param repeatInterval The interval after second and subsequent click events. If negative, no repeat will occur
     * @param clickListener The OnClickListener, that will be called periodically
     * @param animDown Animation to play on touch
     * @param animUp Animation to play on release
     * @param context Context used to access preferences and services
     */
    public RepeatListener(int initialInterval, int repeatInterval, View.OnClickListener clickListener,
                          Animation animDown, Animation animUp, Context context) {
        this.initialInterval = initialInterval;
        this.repeatInterval = repeatInterval;
        this.clickListener = clickListener;

        this.animDown = animDown;
        this.animUp = animUp;

        if (context != null) {
            this.context = context;
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    /**
     * Handle touch events.
     *
     * Note: For buttons, this event Handler returns false, so that the other event handlers
     * of buttons get called. For other views this event Handler consumes the event
     * @param view
     * @param motionEvent
     * @return
     */
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleVibration();
                repeatHandler.removeCallbacks(handlerRunnable);
                if (initialInterval >= 0) {
                    repeatHandler.postDelayed(handlerRunnable, initialInterval);
                }
                downView = view;

                if (animDown != null) {
                    animDown.setFillAfter(true);
                    view.startAnimation(animDown);
                }
                break;
            case MotionEvent.ACTION_UP:
                clickListener.onClick(view);
                view.playSoundEffect(SoundEffectConstants.CLICK);
                // Fallthrough
            case MotionEvent.ACTION_CANCEL:
                repeatHandler.removeCallbacks(handlerRunnable);
                downView = null;

                if (animUp != null) {
                    view.startAnimation(animUp);
                }
                break;
        }
        // Consume the event for views other than buttons
        return !((view instanceof Button) || (view instanceof ImageButton));
    }

    private void handleVibration() {
        if(context != null) {
            //Check if we should vibrate
            boolean vibrateOnPress = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getBoolean(Settings.KEY_PREF_VIBRATE_REMOTE_BUTTONS,
                            Settings.DEFAULT_PREF_VIBRATE_REMOTE_BUTTONS);
            if (vibrateOnPress) {
                vibrator.vibrate(UIUtils.buttonVibrationDuration);
            }
        }
    }

}