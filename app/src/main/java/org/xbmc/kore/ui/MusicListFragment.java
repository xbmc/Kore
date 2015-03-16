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
package org.xbmc.kore.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import org.xbmc.kore.R;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Container for the various music lists
 */
public class MusicListFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_music_list, container, false);
        ButterKnife.inject(this, root);
        // DanhDroid add file browsing
        Bundle mediaFileListArgs = new Bundle();
        mediaFileListArgs.putString(MediaFileListFragment.MEDIA_TYPE, MediaFileListFragment.MUSIC_MEDIA_TYPE);
        TabsAdapter tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(MusicVideoListFragment.class, getArguments(), R.string.music_videos, 4)
                .addTab(MediaFileListFragment.class, mediaFileListArgs, R.string.media_file_list, 5);
        viewPager.setAdapter(tabsAdapter);
        pagerTabStrip.setViewPager(viewPager);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putInt(TVSHOWID, tvshowId);
    }
}
