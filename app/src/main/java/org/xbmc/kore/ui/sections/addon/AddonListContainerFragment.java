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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

import java.util.Collections;
import java.util.Set;

public class AddonListContainerFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(AddonListContainerFragment.class);

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        if (HostManager.getInstance(getContext()).getHostInfo() == null)
            return null;

        int hostId = HostManager.getInstance(getContext()).getHostInfo().getId();

        TabsAdapter tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> bookmarked = prefs.getStringSet(Settings.getBookmarkedAddonsPrefKey(hostId), Collections.<String>emptySet());
        long baseFragmentId = 70 + bookmarked.size() * 100;
        tabsAdapter.addTab(AddonListFragment.class, new Bundle(), R.string.addons, baseFragmentId);
        for (String path: bookmarked) {
            Bundle args = (Bundle) arguments.clone();
            String name = prefs.getString(Settings.getNameBookmarkedAddonsPrefKey(hostId) + path, Settings.DEFAULT_PREF_NAME_BOOKMARKED_ADDON);
            args.putParcelable(MediaFileListFragment.ROOT_PATH,
                                    new MediaFileListFragment.FileLocation(name, "plugin://" + path, true));
            args.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.FILES);
            tabsAdapter.addTab(MediaFileListFragment.class, args, name, ++baseFragmentId);
        }

        return tabsAdapter;
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}
