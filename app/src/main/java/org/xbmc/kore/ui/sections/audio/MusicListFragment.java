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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

/**
 * Container for the various music lists
 */
public class MusicListFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);

        getViewPager().registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getCurrentSelectedFragment();
                if (fragment instanceof AbstractCursorListFragment) {
                    ((AbstractCursorListFragment)fragment).saveSearchState();
                }
            }
        });
    }

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        return new TabsAdapter(this)
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(SongsListFragment.class, getArguments(), R.string.songs, 4)
                .addTab(MusicVideoListFragment.class, getArguments(), R.string.music_videos, 5);
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}
