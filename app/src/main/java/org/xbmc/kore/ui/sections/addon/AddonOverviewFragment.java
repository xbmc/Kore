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
package org.xbmc.kore.ui.sections.addon;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Container for the TV Show overview and Episodes list
 */
public class AddonOverviewFragment extends SharedElementFragment {
    private static final String TAG = LogUtils.makeLogTag(AddonOverviewFragment.class);

    private TabsAdapter tabsAdapter;

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    /**
     * Create a new instance of this, initialized to show the addon addonId
     */
    @TargetApi(21)
    public static AddonOverviewFragment newInstance(AddonListFragment.ViewHolder vh) {
        AddonOverviewFragment fragment = new AddonOverviewFragment();

        Bundle args = new Bundle();
        args.putString(AddonDetailsFragment.BUNDLE_KEY_ADDONID, vh.addonId);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_NAME, vh.addonName);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_AUTHOR, vh.author);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_VERSION, vh.version);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_SUMMARY, vh.summary);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_DESCRIPTION, vh.description);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_FANART, vh.fanart);
        args.putString(AddonDetailsFragment.BUNDLE_KEY_POSTER, vh.poster);
        args.putBoolean(AddonDetailsFragment.BUNDLE_KEY_ENABLED, vh.enabled);
        args.putBoolean(AddonDetailsFragment.BUNDLE_KEY_BROWSABLE, vh.browsable);

        if( Utils.isLollipopOrLater()) {
            args.putString(AddonDetailsFragment.POSTER_TRANS_NAME, vh.artView.getTransitionName());
        }
        fragment.setArguments(args);
        return fragment;
    }

    public Bundle contentArgs(Bundle details) {
        String name = details.getString(AddonDetailsFragment.BUNDLE_KEY_NAME, "Content");
        String path = details.getString(AddonDetailsFragment.BUNDLE_KEY_ADDONID);

        Bundle content = new Bundle();
        content.putString(AddonDetailsFragment.BUNDLE_KEY_NAME, name);
        MediaFileListFragment.FileLocation rootPath = new MediaFileListFragment.FileLocation(name, "plugin://" + path, true);
        rootPath.setRootDir(true);
        content.putParcelable(MediaFileListFragment.ROOT_PATH, rootPath);
        content.putBoolean(MediaFileListFragment.DELAY_LOAD, true);
        return content;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();

        if ((container == null) || (args == null)) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_default_view_pager, container, false);
        ButterKnife.inject(this, root);

        long baseFragmentId = 1000;
        tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(AddonDetailsFragment.class, args, R.string.addon_overview, baseFragmentId++)
                .addTab(MediaFileListFragment.class, contentArgs(args), R.string.addon_content, baseFragmentId++)
                ;
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

    @Override
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
