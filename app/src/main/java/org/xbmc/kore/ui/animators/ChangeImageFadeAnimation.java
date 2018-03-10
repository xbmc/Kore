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
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;

import org.xbmc.kore.utils.LogUtils;

public class ChangeImageFadeAnimation {

    private Drawable fadeOutImage;
    private Drawable fadeInImage;
    private Drawable animatedImage;
    private FloatingActionButton imageHolder;

    private ValueAnimator fadeOutAnimator;

    private ChangeImageFadeAnimation() {

    }

    public ChangeImageFadeAnimation(@NonNull FloatingActionButton imageHolder,
                                    @NonNull Drawable fadeOutImage, @NonNull Drawable fadeInImage) {
        this.fadeOutImage = fadeOutImage.getConstantState().newDrawable();
        this.fadeOutImage.mutate();
        this.fadeInImage = fadeInImage.getConstantState().newDrawable();
        this.fadeInImage.mutate();

        this.imageHolder = imageHolder;
        setupAnimation();
    }

    public void cancel() {
        fadeOutAnimator.cancel();
    }

    public void start() {
        fadeOutAnimator.start();
    }

    private void setupAnimation() {
        fadeOutAnimator = new ValueAnimator();
        fadeOutAnimator.setIntValues(255, 0);
        fadeOutAnimator.setDuration(500);
        final ValueAnimator fadeInAnimator = new ValueAnimator();
        fadeInAnimator.setIntValues(0, 255);
        fadeInAnimator.setDuration(500);
        animatedImage = fadeOutImage;

        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedImage.setAlpha((int) animation.getAnimatedValue());
            }
        };
        fadeInAnimator.addUpdateListener(updateListener);
        fadeOutAnimator.addUpdateListener(updateListener);

        fadeOutAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animatedImage = fadeInImage;
                animatedImage.setAlpha(0);
                imageHolder.setImageDrawable(animatedImage);
                fadeInAnimator.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }
}
