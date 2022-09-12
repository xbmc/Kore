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
package org.xbmc.kore.ui.sections.addon;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.type.AddonType;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Presents addon details
 */
public class AddonInfoFragment extends AbstractInfoFragment {
    private static final String TAG = LogUtils.makeLogTag(AddonInfoFragment.class);

    public static final String BUNDLE_KEY_ADDONID = "addonid";
    public static final String BUNDLE_KEY_BROWSABLE = "browsable";

    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private String addonId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addonId = getDataHolder().getBundle().getString(BUNDLE_KEY_ADDONID, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);
        updateView(getDataHolder());
    }

    @Override
    protected AbstractFragment getAdditionalInfoFragment() {
        return null;
    }

    @Override
    protected String getSyncType() {
        // Don't start refresh on details screen
        return null;
    }

    @Override
    protected boolean setupInfoActionsBar() {
        boolean browsable = getDataHolder().getBundle().getBoolean(BUNDLE_KEY_BROWSABLE, true);
        if (browsable) {
            setupPinButton();
        }

        setupEnabledButton();
        return true;
    }

    @Override
    protected View.OnClickListener getFABClickListener() {
        return (v -> {
            Addons.ExecuteAddon action = new Addons.ExecuteAddon(addonId);
            action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    animateSwitchToRemote();
                }

                @Override
                public void onError(int errorCode, String description) {
                    if (!isResumed()) return;
                    // Got an error, show toast
                    UIUtils.showSnackbar(getView(), R.string.unable_to_connect_to_xbmc);
                }
            }, callbackHandler);
        });
    }

    private void setupEnabledButton() {
        setOnEnableClickListener(v -> {
            final boolean isEnabled = v.getTag() != null && (Boolean) v.getTag();

            Addons.SetAddonEnabled action = new Addons.SetAddonEnabled(addonId, !isEnabled);
            action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!isResumed()) return;
                    int messageResId = (!isEnabled) ? R.string.addon_enabled : R.string.addon_disabled;
                    UIUtils.showSnackbar(getView(), messageResId);
                    setEnableButtonState(!isEnabled);
                    setFabState(!isEnabled);
                }

                @Override
                public void onError(int errorCode, String description) {
                    if (!isResumed()) return;
                    UIUtils.showSnackbar(getView(), String.format(getString(R.string.general_error_executing_action), description));
                }
            }, callbackHandler);
        });

        // Get the addon details, this is done asyhnchronously
        String[] properties = new String[] {
                AddonType.Fields.ENABLED
        };
        Addons.GetAddonDetails action = new Addons.GetAddonDetails(
                addonId, properties);
        action.execute(getHostManager().getConnection(), new ApiCallback<AddonType.Details>() {
            @Override
            public void onSuccess(AddonType.Details result) {
                setEnableButtonState(result.enabled);
                setFabState(result.enabled);
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);
    }

    private void setupPinButton() {
        final int hostId = HostManager.getInstance(requireContext()).getHostInfo().getId();

        setOnPinClickedListener(view -> {
            final boolean isBookmarked = view.getTag() != null && (Boolean) view.getTag();
            String name = getDataHolder().getTitle();
            String path = addonId;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            Set<String> bookmarks = new HashSet<>(prefs.getStringSet(Settings.getBookmarkedAddonsPrefKey(hostId), Collections.emptySet()));
            if (!isBookmarked)
                bookmarks.add(path);
            else
                bookmarks.remove(path);
            prefs.edit()
                 .putStringSet(Settings.getBookmarkedAddonsPrefKey(hostId), bookmarks)
                 .putString(Settings.getNameBookmarkedAddonsPrefKey(hostId) + path, name)
                 .apply();
            UIUtils.showSnackbar(getView(), !isBookmarked ? R.string.addon_pinned : R.string.addon_unpinned);
            setPinButtonState(!isBookmarked);
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> bookmarked = prefs.getStringSet(Settings.getBookmarkedAddonsPrefKey(hostId), Collections.emptySet());
        setPinButtonState(bookmarked.contains(addonId));
    }
}
