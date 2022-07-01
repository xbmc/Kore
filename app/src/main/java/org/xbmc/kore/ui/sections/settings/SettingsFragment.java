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

import static org.xbmc.kore.service.NotificationObserver.NOTIFICATION_ID;
import static org.xbmc.kore.utils.Utils.getLocale;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import org.xbmc.kore.BuildConfig;
import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.service.ConnectionObserversManagerService;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

/**
 * Simple fragment to display preferences screen
 */
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LogUtils.makeLogTag(SettingsFragment.class);

    private int hostId;
    private final ActivityResultLauncher<String> phonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(requireContext(), R.string.read_phone_state_permission_denied, Toast.LENGTH_SHORT)
                         .show();
                    TwoStatePreference pauseCallPreference = (TwoStatePreference)findPreference(Settings.KEY_PREF_PAUSE_DURING_CALLS);
                    if (pauseCallPreference != null) pauseCallPreference.setChecked(false);
                }
            });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, null);

        // Get the preference for side menu items and change its Id to include the current host
        Preference sideMenuItems = findPreference(Settings.KEY_PREF_NAV_DRAWER_ITEMS);
        Preference remoteBarItems = findPreference(Settings.KEY_PREF_REMOTE_BAR_ITEMS);
        hostId = HostManager.getInstance(requireContext()).getHostInfo().getId();
        if (sideMenuItems != null) sideMenuItems.setKey(Settings.getNavDrawerItemsPrefKey(hostId));
        if (remoteBarItems != null) remoteBarItems.setKey(Settings.getRemoteBarItemsPrefKey(hostId));

        // HACK: After changing the key dynamically like above, we need to force the preference
        // to read its value. This can be done by calling onSetInitialValue, which is protected,
        // so, instead of subclassing MultiSelectListPreference and make it public, this little
        // hack changes its access mode.
        // Furthermore, only do this if nothing is saved yet on the shared preferences,
        // otherwise the defaults won't be applied
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null && sideMenuItems != null &&
            sharedPreferences.getStringSet(Settings.getNavDrawerItemsPrefKey(hostId), null) != null) {
            Class<? extends Preference> iterClass = sideMenuItems.getClass();
            try {
                Method m = iterClass.getDeclaredMethod("onSetInitialValue", Object.class);
                m.setAccessible(true);
                m.invoke(sideMenuItems, (Object)null);
            } catch (Exception e) {
                LogUtils.LOGD(TAG, "Error while setting default Nav Drawer shortcuts: " + e);
            }
        }
        if (sharedPreferences != null && remoteBarItems != null &&
            sharedPreferences.getStringSet(Settings.getRemoteBarItemsPrefKey(hostId), null) != null) {
            Class<? extends Preference> iterClass = remoteBarItems.getClass();
            try {
                Method m = iterClass.getDeclaredMethod("onSetInitialValue", Object.class);
                m.setAccessible(true);
                m.invoke(remoteBarItems, (Object)null);
            } catch (Exception e) {
                LogUtils.LOGD(TAG, "Error while setting default bottom bar shortcuts: " + e);
            }
        }

        // Check permission for phone state and set preference accordingly
        if (!hasPhonePermission()) {
            TwoStatePreference pauseCallPreference =
                    (TwoStatePreference)findPreference(Settings.KEY_PREF_PAUSE_DURING_CALLS);
            if (pauseCallPreference != null) pauseCallPreference.setChecked(false);
        }

        setupPreferences();

        ListPreference languagePref = (ListPreference) findPreference(Settings.KEY_PREF_LANGUAGE);
        if (languagePref != null) {
            Locale currentLocale = getCurrentLocale();
            languagePref.setSummary(currentLocale.getDisplayLanguage(currentLocale));
            languagePref.setOnPreferenceClickListener(preference -> {
                setupLanguagePreference((ListPreference) preference);
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update summaries
        setupPreferences();
        Context ctx = requireContext();

        if (key.equals(Settings.KEY_PREF_THEME) || key.equals(Settings.getNavDrawerItemsPrefKey(hostId))
            || key.equals((Settings.getRemoteBarItemsPrefKey(hostId)))) {
            // Explicitly clear cache of resource ids that is maintained in the activity
            UIUtils.playPauseIconsLoaded = false;

            // restart to apply new theme (actually build an entirely new task stack)
            TaskStackBuilder.create(ctx)
                            .addNextIntent(new Intent(ctx, RemoteActivity.class))
                            .addNextIntent(new Intent(ctx, SettingsActivity.class))
                            .startActivities();
        }

        // If the pause during call is selected, make sure we have permission to read phone state
        if (key.equals(Settings.KEY_PREF_PAUSE_DURING_CALLS) &&
            (sharedPreferences.getBoolean(Settings.KEY_PREF_PAUSE_DURING_CALLS, Settings.DEFAULT_PREF_PAUSE_DURING_CALLS))) {
            if (!hasPhonePermission()) {
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
            }
        }

        // If one of the settings that use the observer service are modified, restart it
        if (key.equals(Settings.KEY_PREF_SHOW_NOTIFICATION) || key.equals(Settings.KEY_PREF_PAUSE_DURING_CALLS)) {
            LogUtils.LOGD(TAG, "Stoping connection observer service");
            Intent intent = new Intent(getActivity(), ConnectionObserversManagerService.class);
            ctx.stopService(intent);
            if (sharedPreferences.getBoolean(Settings.KEY_PREF_SHOW_NOTIFICATION, Settings.DEFAULT_PREF_SHOW_NOTIFICATION)) {
                if (Utils.isOreoOrLater()) {
                    ctx.startForegroundService(intent);
                } else {
                    ctx.startService(intent);
                }
            } else {
                NotificationManager notificationManager =
                        (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
            }
        }
    }

    private boolean hasPhonePermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Sets up the preferences state and summaries
     */
    private void setupPreferences() {
        // Theme preferences
        ListPreference themePref = (ListPreference)findPreference(Settings.KEY_PREF_THEME);
        if (themePref != null) themePref.setSummary(themePref.getEntry());
        Context context = requireContext();

        // About preference
        String nameAndVersion = context.getString(R.string.app_name);
        try {
            nameAndVersion += " " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        Preference aboutPreference = findPreference(Settings.KEY_PREF_ABOUT);
        if (aboutPreference != null) {
            aboutPreference.setSummary(nameAndVersion);
            aboutPreference.setOnPreferenceClickListener(preference -> {
                AboutDialogFragment aboutDialog = new AboutDialogFragment();
                aboutDialog.show(getParentFragmentManager(), null);
                return true;
            });
        }
    }

    private void setupLanguagePreference(final ListPreference languagePref) {
        Locale[] locales = getLocales();

        final Locale currentLocale = getCurrentLocale();
        Arrays.sort(locales, (o1, o2) -> o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()));

        String[] displayNames = new String[locales.length];
        String[] entryValues = new String[locales.length];
        for(int index = 0; index < locales.length; index++) {
            Locale locale = locales[index];
            displayNames[index] = locale.getDisplayName(locale);
            entryValues[index] = getLanguageCountryCode(locale);
        }

        languagePref.setValue(getLanguageCountryCode(currentLocale));
        languagePref.setEntries(displayNames);
        languagePref.setEntryValues(entryValues);
        languagePref.setOnPreferenceChangeListener((preference, o) -> {
            languagePref.setValue(o.toString());
            updatePreferredLanguage(o.toString());
            return true;
        });
    }

    private String getLanguageCountryCode(Locale locale) {
        String result = locale.getLanguage();
        if (!locale.getCountry().isEmpty()) {
            result += "-" + locale.getCountry();
        }
        return result;
    }

    /**
     * Converts the locale names into a list of Locale objects
     */
    private Locale[] getLocales() {
        Locale[] locales = new Locale[BuildConfig.SUPPORTED_LOCALES.length];
        for (int index = 0; index < BuildConfig.SUPPORTED_LOCALES.length; index++) {
            locales[index] = getLocale(BuildConfig.SUPPORTED_LOCALES[index]);
        }
        return locales;
    }

    private void updatePreferredLanguage(String localeName) {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null)
            sharedPreferences.edit()
                             .putString(Settings.KEY_PREF_SELECTED_LANGUAGE, localeName)
                             .apply();

        // Restart app to apply locale change
        Intent i = requireContext().getPackageManager().getLaunchIntentForPackage(requireContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private Locale getCurrentLocale() {
        Locale currentLocale = Utils.isNOrLater() ?
                               getResources().getConfiguration().getLocales().get(0) :
                               getResources().getConfiguration().locale;

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) {
            String currentLocaleName = sharedPreferences.getString(Settings.KEY_PREF_SELECTED_LANGUAGE, "");

            if (currentLocaleName != null && !currentLocaleName.isEmpty()) {
                currentLocale = getLocale(currentLocaleName);
            }
        }
        return currentLocale;
    }
}
