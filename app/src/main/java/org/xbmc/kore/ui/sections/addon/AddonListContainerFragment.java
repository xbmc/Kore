/*
 * Copyright 2016 Synced Synapse. All rights reserved.
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

import java.util.Collections;
import java.util.Set;

public class AddonListContainerFragment
        extends AbstractTabsFragment
        implements OnBackPressedListener {

    private static final String TAG = LogUtils.makeLogTag(AddonListContainerFragment.class);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected TabsAdapter createTabsAdapter(AbstractInfoFragment.DataHolder dataHolder) {
        Bundle arguments = dataHolder.getBundle();
        if (arguments == null)
            arguments = new Bundle();

        /*
         * Following check required to support testing AddonsActivity.
         * The database with host info is setup after initializing AddonsActivity using
         * ActivityTestRule from the test support library. This means getHostInfo() will
         * return null as long as the database info has not been set.
         */
        if (HostManager.getInstance(requireContext()).getHostInfo() == null)
            return null;

        int hostId = HostManager.getInstance(requireContext()).getHostInfo().getId();

        TabsAdapter tabsAdapter = new TabsAdapter(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> bookmarked = prefs.getStringSet(Settings.getBookmarkedAddonsPrefKey(hostId), Collections.emptySet());
        long baseFragmentId = 70 + bookmarked.size() * 100L;
        tabsAdapter.addTab(AddonListFragment.class, new Bundle(), R.string.addons, baseFragmentId);
        for (String path: bookmarked) {
            Bundle args = (Bundle) arguments.clone();
            String name = prefs.getString(Settings.getNameBookmarkedAddonsPrefKey(hostId) + path, Settings.DEFAULT_PREF_NAME_BOOKMARKED_ADDON);
            args.putParcelable(MediaFileListFragment.CURRENT_LOCATION,
                               new MediaFileListFragment.FileLocation(name, "plugin://" + path, true, null, true));
            args.putBoolean(MediaFileListFragment.DELAY_LOAD, true);
            args.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.FILES);
            tabsAdapter.addTab(MediaFileListFragment.class, args, name, ++baseFragmentId);
        }

        return tabsAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            BaseMediaActivity listenerActivity = (BaseMediaActivity) requireContext();
            listenerActivity.setBackPressedListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(requireContext() + " unable to register BackPressedListener");
        }
    }

    @Override
    public void onPause() {
        try {
            BaseMediaActivity listenerActivity = (BaseMediaActivity) getContext();
            assert listenerActivity != null;
            listenerActivity.setBackPressedListener(null);
        } catch (ClassCastException e) {
            throw new ClassCastException(getContext() + " unable to unregister BackPressedListener");
        }
        super.onPause();
    }

    @Override
    public boolean onBackPressed() {
        // Tell current fragment to move up one directory, if possible
        Fragment fragment = getCurrentSelectedFragment();
        if (fragment instanceof MediaFileListFragment) {
            return ((MediaFileListFragment) fragment).navigateToParentDir();
        }
        return false;
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return false;
    }
}
