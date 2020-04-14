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
package org.xbmc.kore.ui.animators;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.PorterDuff;
import android.view.View;

public class PulsateAnimation {

    private View view;
    private AnimatorSet animatorSet;
    private boolean stopAnimation;
    private int startColor;
    private int endColor;

    private PulsateAnimation() {

    }

    public PulsateAnimation(View v, int startColor, int endColor) {
        view = v;
        this.startColor = startColor;
        this.endColor = endColor;

        setupAnimation();
    }

    public void start() {
        stopAnimation = false;
        animatorSet.start();
    }

    public void stop() {
        stopAnimation = true;
    }

    public boolean isRunning() {
        return animatorSet.isRunning();
    }

    private void setupAnimation() {
        animatorSet = new AnimatorSet();

        //Creates an animation that first changes color from startColor to endColor and
        //afterwards changes color from endColor to startColor
        animatorSet.playSequentially(createValueAnimator(startColor, endColor),
                                     createValueAnimator(endColor, startColor));

        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!stopAnimation)
                    animatorSet.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private ValueAnimator createValueAnimator(int startColor, int endColor) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setDuration(1000);
        valueAnimator.setIntValues(startColor, endColor);
        valueAnimator.setEvaluator(new ArgbEvaluator());
        valueAnimator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        });
        return valueAnimator;
    }
}
