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
package org.xbmc.kore.ui.sections.video;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

/**
 * Container for the various PVR lists
 */
public class PVRListFragment extends AbstractTabsFragment
        implements OnBackPressedListener {

    private static final String TAG = LogUtils.makeLogTag(PVRListFragment.class);

    public static final String PVR_LIST_TYPE_KEY = "pvr_list_type_key";
    public static final int LIST_TV_CHANNELS = 0,
            LIST_RADIO_CHANNELS = 1;

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        Bundle tvArgs = new Bundle();
        Bundle radioArgs = new Bundle();

        if (getArguments() != null) {
            tvArgs.putAll(getArguments());
            radioArgs.putAll(getArguments());
        }
        tvArgs.putInt(PVR_LIST_TYPE_KEY, LIST_TV_CHANNELS);
        radioArgs.putInt(PVR_LIST_TYPE_KEY, LIST_RADIO_CHANNELS);

        return new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(PVRChannelsListFragment.class, tvArgs, R.string.tv_channels, 1)
                .addTab(PVRChannelsListFragment.class, radioArgs, R.string.radio_channels, 2)
                .addTab(PVRRecordingsListFragment.class, getArguments(), R.string.recordings, 3);
    }

    @Override
    public boolean onBackPressed() {
        // Tell current fragment to move up one directory, if possible
        Fragment visibleFragment = ((TabsAdapter)getViewPager().getAdapter())
                .getStoredFragment(getViewPager().getCurrentItem());

        if (visibleFragment instanceof OnBackPressedListener) {
            return ((OnBackPressedListener) visibleFragment).onBackPressed();
        }

        // Not handled, let the activity handle it
        return false;
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}
