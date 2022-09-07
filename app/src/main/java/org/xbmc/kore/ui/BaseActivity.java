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
package org.xbmc.kore.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import org.xbmc.kore.Settings;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Base activity, where common behaviour is implemented
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                                           .detectDiskReads()
//                                           .detectDiskWrites()
//                                           .detectNetwork()   // or .detectAll() for all detectable problems
//                                           .penaltyLog()
//                                           .build());
//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                                       .detectLeakedSqlLiteObjects()
//                                       .detectLeakedClosableObjects()
//                                       .penaltyLog()
//                                       .penaltyDeath()
//                                       .build());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(UIUtils.getThemeResourceId(prefs.getString(Settings.KEY_PREF_THEME, Settings.DEFAULT_PREF_THEME)));
        String preferredLocale = prefs.getString(Settings.KEY_PREF_SELECTED_LANGUAGE, null);
        if (!TextUtils.isEmpty(preferredLocale))
            Utils.setLocale(this, preferredLocale);

        UIUtils.tintSystemBars(this);
        super.onCreate(savedInstanceState);
    }
}
