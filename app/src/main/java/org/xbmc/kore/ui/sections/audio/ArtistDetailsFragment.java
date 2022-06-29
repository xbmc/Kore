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
package org.xbmc.kore.ui.sections.audio;

import android.os.Bundle;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

public class ArtistDetailsFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(ArtistDetailsFragment.class);

    @Override
    protected TabsAdapter createTabsAdapter(AbstractInfoFragment.DataHolder dataHolder) {
        Bundle arguments = dataHolder.getBundle();
        int itemId = dataHolder.getId();
        long baseFragmentId = itemId * 10L;

        arguments.putInt(AlbumListFragment.BUNDLE_KEY_ARTISTID, itemId);
        arguments.putInt(SongsListFragment.BUNDLE_KEY_ARTISTID, itemId);

        return  new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(ArtistInfoFragment.class, arguments, R.string.info,
                        baseFragmentId)
                .addTab(AlbumListFragment.class, arguments,
                        R.string.albums, baseFragmentId + 1)
                .addTab(SongsListFragment.class, arguments,
                        R.string.songs, baseFragmentId + 2);
    }


    @Override
    protected boolean shouldRememberLastTab() {
        return false;
    }
}
