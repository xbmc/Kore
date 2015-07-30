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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.TaskStackBuilder;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Simple fragment to display preferences screen
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LogUtils.makeLogTag(SettingsFragment.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        setupPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update summaries
        setupPreferences();

        if (key.equals(Settings.KEY_PREF_THEME)) {
            //String newTheme = sharedPreferences.getString(key, DEFAULT_PREF_THEME);

            // Explicitly clear cache of resource ids that is maintained in the activity
            UIUtils.playPauseIconsLoaded = false;

            // restart to apply new theme (actually build an entirely new task stack)
            TaskStackBuilder.create(getActivity())
                            .addNextIntent(new Intent(getActivity(), RemoteActivity.class))
                            .addNextIntent(new Intent(getActivity(), SettingsActivity.class))
                            .startActivities();
        }
    }

    /**
     * Sets up the preferences state and summaries
     */
    private void setupPreferences() {
        // Theme preferences
        ListPreference themePref = (ListPreference)findPreference(Settings.KEY_PREF_THEME);
        themePref.setSummary(themePref.getEntry());

        // About preference
        String nameAndVersion = getActivity().getString(R.string.app_name);
        try {
            nameAndVersion += " v" +
                    getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException exc) {
        }
        Preference aboutPreference = findPreference(Settings.KEY_PREF_ABOUT);
        aboutPreference.setSummary(nameAndVersion);
        aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AboutDialogFragment aboutDialog = new AboutDialogFragment();
                aboutDialog.show(getActivity().getFragmentManager(), null);
                return true;
            }
        });

    }
}
