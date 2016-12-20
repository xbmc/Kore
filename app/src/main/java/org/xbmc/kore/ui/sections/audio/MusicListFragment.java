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
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Container for the various music lists
 */
public class MusicListFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    private TabsAdapter tabsAdapter;

    private int currentItem;

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_default_view_pager, container, false);
        ButterKnife.inject(this, root);

        tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(SongsListFragment.class, getArguments(), R.string.songs, 4)
                .addTab(MusicVideoListFragment.class, getArguments(), R.string.music_videos, 5);

        viewPager.setAdapter(tabsAdapter);
        pagerTabStrip.setViewPager(viewPager);

        currentItem = viewPager.getCurrentItem();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
                currentItem = viewPager.getCurrentItem();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }
}
