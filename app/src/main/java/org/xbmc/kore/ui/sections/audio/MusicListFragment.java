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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.ViewPager;

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

    private int currentItem;
    private TabsAdapter tabsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null)
            return view;

        currentItem = getViewPager().getCurrentItem();

        getViewPager().addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                AbstractCursorListFragment f =
                        ((AbstractCursorListFragment) tabsAdapter.getStoredFragment(currentItem));
                if (f != null) {
                    f.saveSearchState();
                }
                currentItem = getViewPager().getCurrentItem();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(SongsListFragment.class, getArguments(), R.string.songs, 4)
                .addTab(MusicVideoListFragment.class, getArguments(), R.string.music_videos, 5);
        return tabsAdapter;
    }
}
