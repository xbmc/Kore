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

import android.os.Bundle;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

public class AddonDetailsFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(AddonDetailsFragment.class);

    public Bundle contentArgs(Bundle details) {
        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(details);
        String name = dataHolder.getTitle();
        String path = details.getString(AddonInfoFragment.BUNDLE_KEY_ADDONID);

        MediaFileListFragment.FileLocation rootPath = new MediaFileListFragment.FileLocation(name, "plugin://" + path, true);
        rootPath.setRootDir(true);
        details.putParcelable(MediaFileListFragment.ROOT_PATH, rootPath);
        details.putBoolean(MediaFileListFragment.DELAY_LOAD, true);
        return details;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected TabsAdapter createTabsAdapter(AbstractInfoFragment.DataHolder dataHolder) {
        long baseFragmentId = 1000;
        Bundle args = getArguments();
        return new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(AddonInfoFragment.class, args, R.string.addon_overview, baseFragmentId++)
                .addTab(MediaFileListFragment.class, contentArgs(args), R.string.addon_content, baseFragmentId++)
                ;
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return false;
    }
}
