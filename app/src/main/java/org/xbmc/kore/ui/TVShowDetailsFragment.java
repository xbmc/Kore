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

import android.annotation.TargetApi;
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
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Container for the TV Show overview and Episodes list
 */
public class TVShowDetailsFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(TVShowDetailsFragment.class);

    public static final String POSTER_TRANS_NAME = "POSTER_TRANS_NAME";
    public static final String BUNDLE_KEY_TVSHOWID = "tvshow_id";
    public static final String BUNDLE_KEY_TITLE = "title";
    public static final String BUNDLE_KEY_PREMIERED = "premiered";
    public static final String BUNDLE_KEY_STUDIO = "studio";
    public static final String BUNDLE_KEY_EPISODE = "episode";
    public static final String BUNDLE_KEY_WATCHEDEPISODES = "watchedepisodes";
    public static final String BUNDLE_KEY_RATING = "rating";
    public static final String BUNDLE_KEY_PLOT = "plot";
    public static final String BUNDLE_KEY_GENRES = "genres";

    // Displayed movie id
    private int tvshowId = -1;

    private TabsAdapter tabsAdapter;

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    /**
     * Create a new instance of this, initialized to show tvshowId
     */
    @TargetApi(21)
    public static TVShowDetailsFragment newInstance(TVShowListFragment.ViewHolder vh) {
        TVShowDetailsFragment fragment = new TVShowDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_TVSHOWID, vh.tvshowId);
        args.putInt(BUNDLE_KEY_EPISODE, vh.episode);
        args.putString(BUNDLE_KEY_GENRES, vh.genres);
        args.putString(BUNDLE_KEY_PLOT, vh.plot);
        args.putString(BUNDLE_KEY_PREMIERED, vh.premiered);
        args.putDouble(BUNDLE_KEY_RATING, vh.rating);
        args.putString(BUNDLE_KEY_STUDIO, vh.studio);
        args.putString(BUNDLE_KEY_TITLE, vh.tvshowTitle);
        args.putInt(BUNDLE_KEY_WATCHEDEPISODES, vh.watchedEpisodes);
        if( Utils.isLollipopOrLater()) {
            args.putString(POSTER_TRANS_NAME, vh.artView.getTransitionName());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tvshowId = getArguments().getInt(BUNDLE_KEY_TVSHOWID, -1);

        if ((container == null) || (tvshowId == -1)) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_default_view_pager, container, false);
        ButterKnife.inject(this, root);

        long baseFragmentId = tvshowId * 10;
        tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(TVShowOverviewFragment.class, getArguments(), R.string.tvshow_overview,
                        baseFragmentId)
                .addTab(TVShowEpisodeListFragment.class, getArguments(),
                        R.string.tvshow_episodes, baseFragmentId + 1);

        viewPager.setAdapter(tabsAdapter);
        pagerTabStrip.setViewPager(viewPager);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    public Fragment getCurrentTabFragment() {
        return tabsAdapter.getItem(viewPager.getCurrentItem());
    }

    public View getSharedElement() {
        View view = getView();
        if (view == null)
            return null;

        //Note: this works as R.id.poster is only used in TVShowOverviewFragment.
        //If the same id is used in other fragments in the TabsAdapter we
        //need to check which fragment is currently displayed
        View artView = view.findViewById(R.id.poster);
        View scrollView = view.findViewById(R.id.media_panel);
        if (( artView != null ) &&
                ( scrollView != null ) &&
                UIUtils.isViewInBounds(scrollView, artView)) {
            return artView;
        }

        return null;
    }
}
