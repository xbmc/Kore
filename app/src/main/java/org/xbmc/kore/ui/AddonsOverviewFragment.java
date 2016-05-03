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
package org.xbmc.kore.ui;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.Collections;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Container for the TV Show overview and Episodes list
 */
public class AddonsOverviewFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(AddonsOverviewFragment.class);

    private TabsAdapter tabsAdapter;

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_default_view_pager, container, false);
        ButterKnife.inject(this, root);

        tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager());
        SharedPreferences prefs = getActivity().getSharedPreferences("addons", Context.MODE_PRIVATE);
        Set<String> bookmarked = prefs.getStringSet("bookmarked", Collections.<String>emptySet());
        long baseFragmentId = 70 + bookmarked.size() * 100;
        for (String path: bookmarked) {
            String name = prefs.getString("name_" + path, "Content");
            Bundle addon = new Bundle();
            addon.putString(AddonDetailsFragment.BUNDLE_KEY_NAME, name);
            addon.putParcelable(MediaFileListFragment.ROOT_PATH, new MediaFileListFragment.FileLocation(name, "plugin://" + path, true));
            tabsAdapter.addTab(MediaFileListFragment.class, addon, name, baseFragmentId++);
        }
        tabsAdapter.addTab(AddonListFragment.class, new Bundle(), R.string.addons, baseFragmentId);
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
