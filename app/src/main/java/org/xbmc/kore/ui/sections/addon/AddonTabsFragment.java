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
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

public class AddonTabsFragment
        extends AbstractTabsFragment
        implements OnBackPressedListener {
    private static final String TAG = LogUtils.makeLogTag(AddonTabsFragment.class);

    public Bundle contentArgs(Bundle details) {
        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(details);
        String name = dataHolder.getTitle();
        String path = details.getString(AddonInfoFragment.BUNDLE_KEY_ADDONID);

        details.putParcelable(MediaFileListFragment.CURRENT_LOCATION,
                              new MediaFileListFragment.FileLocation(name, "plugin://" + path, true, null, true));
        details.putBoolean(MediaFileListFragment.DELAY_LOAD, true);
        return details;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected TabsAdapter createTabsAdapter(AbstractInfoFragment.DataHolder dataHolder) {
        long baseFragmentId = 1000;
        Bundle args = getArguments();
        return new TabsAdapter(this)
                .addTab(AddonInfoFragment.class, args, R.string.addon_overview, baseFragmentId++)
                .addTab(MediaFileListFragment.class, contentArgs(args), R.string.addon_content, baseFragmentId++)
                ;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            BaseMediaActivity listenerActivity = (BaseMediaActivity) context;
            listenerActivity.setBackPressedListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " unable to register BackPressedListener");
        }
    }

    @Override
    public void onDestroy() {
        try {
            BaseMediaActivity listenerActivity = (BaseMediaActivity) getContext();
            assert listenerActivity != null;
            listenerActivity.setBackPressedListener(null);
        } catch (ClassCastException e) {
            throw new ClassCastException(getContext() + " unable to unregister BackPressedListener");
        }
        super.onDestroy();
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
