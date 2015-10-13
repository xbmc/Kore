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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.service.SyncUtils;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * Container for the various music lists
 */
public class MusicListFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    private TabsAdapter tabsAdapter;

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_music_list, container, false);
        ButterKnife.inject(this, root);

        tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(MusicVideoListFragment.class, getArguments(), R.string.music_videos, 4);

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
