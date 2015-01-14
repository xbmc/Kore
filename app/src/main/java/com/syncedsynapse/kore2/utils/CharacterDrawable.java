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
package com.syncedsynapse.kore2.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

public class CharacterDrawable extends ColorDrawable {
    private final char character;
    private final Paint textPaint;
//    private final Paint borderPaint;
//    private static final int STROKE_WIDTH = 10;
//    private static final float SHADE_FACTOR = 0.9f;

    private static final Typeface typeface;
    static {
        if (Utils.isJellybeanMR1OrLater()) {
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL);
        } else if (Utils.isJellybeanOrLater()) {
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
        } else {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL);
        }
    }

    public CharacterDrawable(char character, int color) {
        super(color);
        this.character = character;
        this.textPaint = new Paint();
//        this.borderPaint = new Paint();

        // text paint settings
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(false);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(typeface);

        // border paint settings
//        borderPaint.setColor(getDarkerShade(color));
//        borderPaint.setStyle(Paint.Style.STROKE);
//        borderPaint.setStrokeWidth(STROKE_WIDTH);
    }

//    private int getDarkerShade(int color) {
//        return Color.rgb((int)(SHADE_FACTOR * Color.red(color)),
//                (int)(SHADE_FACTOR * Color.green(color)),
//                (int)(SHADE_FACTOR * Color.blue(color)));
//    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // draw border
//        canvas.drawRect(getBounds(), borderPaint);

        // draw text
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        textPaint.setTextSize(height / 2);
        canvas.drawText(String.valueOf(character), width/2, height/2 - ((textPaint.descent() + textPaint.ascent()) / 2) , textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}