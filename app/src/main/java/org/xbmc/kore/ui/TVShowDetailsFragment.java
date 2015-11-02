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

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.astuetz.PagerSlidingTabStrip;
import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Container for the TV Show overview and Episodes list
 */
public class TVShowDetailsFragment extends AbstractDetailsFragment {
    private static final String TAG = LogUtils.makeLogTag(TVShowDetailsFragment.class);

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    /**
     * Create a new instance of this, initialized to show tvshowId
     */
    public static TVShowDetailsFragment newInstance(int tvshowId, ImageView poster) {
        TVShowDetailsFragment fragment = new TVShowDetailsFragment();

        fragment.setupArguments(tvshowId, poster);
        return fragment;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_tvshow_details, container, false);
        ButterKnife.inject(this, root);

        Bundle bundleForTVShowEpisodeListFragment = new Bundle();
        bundleForTVShowEpisodeListFragment.putInt(TVShowEpisodeListFragment.TVSHOWID, getItemId());

        long baseFragmentId = getItemId() * 10;
        TabsAdapter tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(TVShowOverviewFragment.class, getArguments(), R.string.tvshow_overview,
                        baseFragmentId)
                .addTab(TVShowEpisodeListFragment.class, bundleForTVShowEpisodeListFragment,
                        R.string.tvshow_episodes, baseFragmentId + 1);

        viewPager.setAdapter(tabsAdapter);
        pagerTabStrip.setViewPager(viewPager);

        return root;
    }

    @Override
    protected ArrayList<String> getSyncTypes() {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected void onSyncProcessEnded(MediaSyncEvent event) {

    }
}
