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
package com.syncedsynapse.kore2.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

import com.syncedsynapse.kore2.BuildConfig;
import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.Settings;
import com.syncedsynapse.kore2.billing.IabHelper;
import com.syncedsynapse.kore2.billing.IabResult;
import com.syncedsynapse.kore2.billing.Inventory;
import com.syncedsynapse.kore2.billing.Purchase;
import com.syncedsynapse.kore2.utils.LogUtils;

/**
 * Simple fragment to display preferences screen
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LogUtils.makeLogTag(SettingsFragment.class);

    public static final String COFFEE_SKU = "coffee";
    public static final int COFFEE_RC = 1001;

    /**
     * Preferences keys. Make sure these are the same as in preferences.xml
     */
    public static final String KEY_PREF_THEME = "pref_theme";
    public static final String KEY_PREF_ABOUT = "pref_about";
    public static final String KEY_PREF_COFFEE = "pref_coffee";
    public static final String DEFAULT_PREF_THEME = "0";

    // Billing helper
    private IabHelper mBillingHelper;

    private Settings mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        mSettings = Settings.getInstance(getActivity());

        if (BuildConfig.DEBUG) {
            mSettings.hasBoughtCoffee = true;
        }
        setupPreferences(mSettings.hasBoughtCoffee);
        if (!BuildConfig.DEBUG) {
            checkCoffeeUpgradeAsync();
        }
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
        setupPreferences(mSettings.hasBoughtCoffee);

        if (key.equals(KEY_PREF_THEME)) {
            //String newTheme = sharedPreferences.getString(key, DEFAULT_PREF_THEME);

            // restart to apply new theme (actually build an entirely new task stack)
            TaskStackBuilder.create(getActivity())
                            .addNextIntent(new Intent(getActivity(), RemoteActivity.class))
                            .addNextIntent(new Intent(getActivity(), SettingsActivity.class))
                            .startActivities();
        }
    }

    /**
     * Sets up the preferences state and summaries
     * @param hasBoughtCoffee Whether the user has bought me a coffee
     */
    private void setupPreferences(boolean hasBoughtCoffee) {
        final Settings settings = Settings.getInstance(getActivity());

        LogUtils.LOGD(TAG, "Setting up preferences. Has bought coffee? " + hasBoughtCoffee);

        // Coffee upgrade
        final Preference coffeePref = findPreference(KEY_PREF_COFFEE);
        if (coffeePref != null) {
            if (hasBoughtCoffee) {
                if (settings.showThanksForCofeeMessage) {
                    coffeePref.setTitle(getResources().getString(R.string.thanks_for_coffe));
                    coffeePref.setSummary(getResources().getString(R.string.remove_coffee_message));
                    coffeePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            settings.showThanksForCofeeMessage = false;
                            settings.save();
                            getPreferenceScreen().removePreference(coffeePref);
                            return true;
                        }
                    });
                } else {
                    getPreferenceScreen().removePreference(coffeePref);
                }
            } else {
                coffeePref.setTitle(getResources().getString(R.string.buy_me_coffee));
                coffeePref.setSummary(getResources().getString(R.string.expresso_please));
                coffeePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        launchCoffeePurchase();
                        return true;
                    }
                });
            }
        }

        // Theme preferences
        ListPreference themePref = (ListPreference)findPreference(KEY_PREF_THEME);
        if (hasBoughtCoffee) {
            themePref.setEnabled(true);
            themePref.setSummary(themePref.getEntry());
        } else {
            themePref.setEnabled(false);
            themePref.setSummary(getActivity().getString(R.string.buy_coffee_to_unlock_themes));
        }

        // About preference
        String nameAndVersion = getActivity().getString(R.string.app_name);
        try {
            nameAndVersion += " v" +
                    getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException exc) {
        }
        Preference aboutPreference = findPreference(KEY_PREF_ABOUT);
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

    /**
     * Check if the user has bought coffee and locks/unlocks the ui
     */
    private void checkCoffeeUpgradeAsync() {
        mBillingHelper = new IabHelper(this.getActivity(), BuildConfig.IAP_KEY);
        mBillingHelper.enableDebugLogging(BuildConfig.DEBUG);

        final Context context = this.getActivity();

        mBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Toast.makeText(context,
                            getResources().getString(R.string.error_setting_up_billing, result.getMessage()),
                            Toast.LENGTH_SHORT).show();

                    // Lock UI
                    mSettings.hasBoughtCoffee = false;
                    setupPreferences(mSettings.hasBoughtCoffee);
                    mSettings.save();

                    // Lock upgrade preference
                    Preference coffeePreference = findPreference(KEY_PREF_COFFEE);
                    coffeePreference.setEnabled(false);
                    coffeePreference.setSummary(getResources().getString(R.string.error_setting_up_billing, result.getMessage()));

                    LogUtils.LOGD(TAG, "Problem setting up In-app Billing: " + result);
                    mBillingHelper.dispose();
                    mBillingHelper = null;
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mBillingHelper == null) return;

                // IAB is fully set up. Query purchased items
                mBillingHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                        // Have we been disposed of in the meantime? If so, quit.
                        if (mBillingHelper == null) return;

                        if (result.isFailure()) {
                            LogUtils.LOGD(TAG, "Failed to query inventory. Result: " + result);
                            return;
                        }

                        Purchase upgradePurchase = inv.getPurchase(COFFEE_SKU);
                        if (upgradePurchase != null)
                            LogUtils.LOGD(TAG, "Purchase " + upgradePurchase.toString());
                        //boolean hasUpgrade = (upgradePurchase != null && verifyDeveloperPayload(upgradePurchase));
                        boolean hasUpgrade = inv.hasPurchase(COFFEE_SKU);
                        LogUtils.LOGD(TAG, "Has purchase " + String.valueOf(hasUpgrade));

                        // update UI accordingly
                        mSettings.hasBoughtCoffee = hasUpgrade;
                        setupPreferences(mSettings.hasBoughtCoffee);
                        mSettings.save();
                    }
                });
            }
        });
    }

    public void launchCoffeePurchase() {
        if (mBillingHelper == null) return;

        final Context context = this.getActivity();

        mBillingHelper.launchPurchaseFlow(this.getActivity(),
                COFFEE_SKU, COFFEE_RC,
                new IabHelper.OnIabPurchaseFinishedListener() {
                    @Override
                    public void onIabPurchaseFinished(IabResult result, Purchase info) {
                        // if we were disposed of in the meantime, quit.
                        if (mBillingHelper == null) return;

                        LogUtils.LOGD(TAG, "Purchase result: " + result + ". Info: " + info);
                        if (result.isFailure()) {
                            String msg = getResources().getString(R.string.error_during_purchased);
                            msg = msg + "\n" + result.getMessage();
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                            LogUtils.LOGD(TAG, "Error in purchase" + result);
                            return;
                        }

                        if (info.getSku().equals(COFFEE_SKU)) {
                            Toast.makeText(context, R.string.purchase_thanks, Toast.LENGTH_SHORT).show();
                            mSettings.hasBoughtCoffee = true;
                            setupPreferences(mSettings.hasBoughtCoffee);
                            mSettings.save();
                        }
                    }
                });
    }

    /**
     * This method gets called when the purchase workflow is finished by the enclosing activity
     */
    public boolean onPurchaseWorkflowFinish(int requestCode, int resultCode, Intent data) {
        LogUtils.LOGD(TAG, "onPurchaseWorkflowFinish(" + requestCode + "," + resultCode + "," + data);
        return (mBillingHelper != null) &&
                mBillingHelper.handleActivityResult(requestCode, resultCode, data);
    }

}
