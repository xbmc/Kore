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
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import org.xbmc.kore.Settings;

import java.util.List;
import java.util.Locale;

/**
 * Because every project needs one of these
 * */
public class Utils {
    private static final String TAG = LogUtils.makeLogTag(Utils.class);

    public static boolean isMOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isNOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean isOreoOrLater() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O; }

    public static boolean isOreoMR1OrLater() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1; }

    public static boolean isROrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    public static boolean isSOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /**
     * Concats a list of strings
     * @param list List to concatenate
     * @param delimiter Delimiter
     * @return Strings concatenated
     */
    public static String listStringConcat(List<String> list, String delimiter) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String item : list) {
            if (TextUtils.isEmpty(item)) continue;
            if (!first) builder.append(delimiter);
            builder.append(item);
            first = false;
        }
        return builder.toString();
    }

    public static final String IMDB_PERSON_SEARCH_URL = "https://m.imdb.com/find?q=%s&s=nm";

    public static final String IMDB_MOVIE_URL = "https://m.imdb.com/title/%s/";

    /**
     * Open the IMDb web page for the given person name.
     */
    public static void openImdbForPerson(Context context, String name) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }

        // Open IMDB
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(IMDB_PERSON_SEARCH_URL, name)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        context.startActivity(intent);
    }

    /**
     * Open the IMDb web page for the given person name.
     */
    public static void openImdbForMovie(Context context, String imdbNumber) {
        if (context == null || TextUtils.isEmpty(imdbNumber)) {
            return;
        }

        // Open IMDB
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(IMDB_MOVIE_URL, imdbNumber)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        context.startActivity(intent);
    }

    /**
     * Restrict a value to a range
     * @param value Value
     * @param min Range minimum
     * @param max Range maximum
     * @return Value if between [min, max], min or max
     */
    public static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(min, value));
    }

    /**
     * Converts a drawable to a bitmap
     * @param drawable Drawable to convert
     * @return Bitmap
     */
    public static Bitmap drawableToBitmap (Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static void setPreferredLocale(Context context) {
        String preferredLocale = PreferenceManager.getDefaultSharedPreferences(context)
                                                  .getString(Settings.KEY_PREF_SELECTED_LANGUAGE, "");
        if (! preferredLocale.isEmpty()) {
            Utils.setLocale(context, preferredLocale);
        }
    }

    private static void setLocale(Context context, String localeName) {
        Locale locale = getLocale(localeName);

        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static Locale getLocale(String localeName) {
        Locale locale;
        String[] languageAndRegion = localeName.split("-", 2);
        if (languageAndRegion.length > 1) {
            locale = new Locale(languageAndRegion[0], languageAndRegion[1]);
        } else {
            locale = new Locale(localeName);
        }
        return locale;
    }
}
