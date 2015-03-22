/*
 * Copyright 2015 DanhDroid. All rights reserved.
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
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.utils.TabsAdapter;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by danhdroid on 3/18/15.
 */
public class FileListFragment extends Fragment {

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_media_list, container, false);
        ButterKnife.inject(this, root);

        Bundle videoFileListArgs = new Bundle();
        videoFileListArgs.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.VIDEO);
        Bundle musicFileListArgs = new Bundle();
        musicFileListArgs.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.MUSIC);
        TabsAdapter tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(MediaFileListFragment.class, videoFileListArgs, R.string.video, 1)
                .addTab(MediaFileListFragment.class, musicFileListArgs, R.string.music, 2);
        viewPager.setAdapter(tabsAdapter);
        pagerTabStrip.setViewPager(viewPager);
        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(false);
    }
}