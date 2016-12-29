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
package org.xbmc.kore.ui.sections.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.TwoStatePreference;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.service.ConnectionObserversManagerService;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.lang.reflect.Method;

/**
 * Simple fragment to display preferences screen
 */
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LogUtils.makeLogTag(SettingsFragment.class);

    private int hostId;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Get the preference for side menu itens and change its Id to include
        // the current host
        Preference sideMenuItems = findPreference(Settings.KEY_PREF_NAV_DRAWER_ITEMS);
        hostId = HostManager.getInstance(getActivity()).getHostInfo().getId();
        sideMenuItems.setKey(Settings.getNavDrawerItemsPrefKey(hostId));

        // HACK: After changing the key dinamically like above, we need to force the preference
        // to read its value. This can be done by calling onSetInitialValue, which is protected,
        // so, instead of subclassing MultiSelectListPreference and make it public, this little
        // hack changes its access mode.
        // Furthermore, only do this is nothing is saved yet on the shared preferences,
        // otherwise the defaults won't be applied
        if (getPreferenceManager().getSharedPreferences().getStringSet(Settings.getNavDrawerItemsPrefKey(hostId), null) != null) {
            Class iterClass = sideMenuItems.getClass();
            try {
                @SuppressWarnings("unchecked")
                Method m = iterClass.getDeclaredMethod("onSetInitialValue", boolean.class, Object.class);
                m.setAccessible(true);
                m.invoke(sideMenuItems, true, null);
            } catch (Exception e) {
            }
        }

        // Check permission for phone state and set preference accordingly
        if (!hasPhonePermission()) {
            TwoStatePreference pauseCallPreference =
                    (TwoStatePreference)findPreference(Settings.KEY_PREF_PAUSE_DURING_CALLS);
            pauseCallPreference.setChecked(false);
        }

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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update summaries
        setupPreferences();

        if (key.equals(Settings.KEY_PREF_THEME) || key.equals(Settings.getNavDrawerItemsPrefKey(hostId))) {
            // Explicitly clear cache of resource ids that is maintained in the activity
            UIUtils.playPauseIconsLoaded = false;

            // restart to apply new theme (actually build an entirely new task stack)
            TaskStackBuilder.create(getActivity())
                            .addNextIntent(new Intent(getActivity(), RemoteActivity.class))
                            .addNextIntent(new Intent(getActivity(), SettingsActivity.class))
                            .startActivities();
        }

        // If the pause during call is selected, make sure we have permission to read phone state
        if (key.equals(Settings.KEY_PREF_PAUSE_DURING_CALLS) &&
            (sharedPreferences.getBoolean(Settings.KEY_PREF_PAUSE_DURING_CALLS, Settings.DEFAULT_PREF_PAUSE_DURING_CALLS))) {
            if (!hasPhonePermission()) {
                requestPermissions(
                        new String[] {Manifest.permission.READ_PHONE_STATE},
                        Utils.PERMISSION_REQUEST_READ_PHONE_STATE);
            }
        }

            // If one of the settings that use the observer service are modified, restart it
        if (key.equals(Settings.KEY_PREF_SHOW_NOTIFICATION) || key.equals(Settings.KEY_PREF_PAUSE_DURING_CALLS)) {
            LogUtils.LOGD(TAG, "Stoping connection observer service");
            Intent intent = new Intent(getActivity(), ConnectionObserversManagerService.class);
            getActivity().stopService(intent);
            getActivity().startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.PERMISSION_REQUEST_READ_PHONE_STATE:
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.length == 0) ||
                    (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(getActivity(), R.string.read_phone_state_permission_denied, Toast.LENGTH_SHORT)
                         .show();
                    TwoStatePreference pauseCallPreference =
                            (TwoStatePreference)findPreference(Settings.KEY_PREF_PAUSE_DURING_CALLS);
                    pauseCallPreference.setChecked(false);
                }
                break;
        }
    }

    private boolean hasPhonePermission() {
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
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
            nameAndVersion += " " +
                    getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException exc) {
        }
        Preference aboutPreference = findPreference(Settings.KEY_PREF_ABOUT);
        aboutPreference.setSummary(nameAndVersion);
        aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AboutDialogFragment aboutDialog = new AboutDialogFragment();
                aboutDialog.show(getFragmentManager(), null);
                return true;
            }
        });
    }
}
