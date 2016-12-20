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

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

public class ArtistDetailsFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(ArtistDetailsFragment.class);

    @InjectView(R.id.pager_tab_strip) PagerSlidingTabStrip pagerTabStrip;
    @InjectView(R.id.pager) ViewPager viewPager;

    /**
     * Create a new instance of this, initialized to show tvshowId
     */
    @TargetApi(21)
    public static ArtistDetailsFragment newInstance(ArtistListFragment.ViewHolder vh) {
        ArtistDetailsFragment fragment = new ArtistDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(ArtistOverviewFragment.BUNDLE_KEY_ARTISTID, vh.artistId);
        args.putInt(AlbumListFragment.BUNDLE_KEY_ARTISTID, vh.artistId);
        args.putString(ArtistOverviewFragment.BUNDLE_KEY_TITLE, vh.artistName);
        args.putString(ArtistOverviewFragment.BUNDLE_KEY_FANART, vh.fanart);
        args.putString(ArtistOverviewFragment.BUNDLE_KEY_DESCRIPTION, vh.description);
        args.putString(ArtistOverviewFragment.BUNDLE_KEY_GENRE, vh.genres);
        args.putString(ArtistOverviewFragment.BUNDLE_KEY_POSTER, vh.poster);

        if( Utils.isLollipopOrLater()) {
            args.putString(ArtistOverviewFragment.POSTER_TRANS_NAME, vh.artView.getTransitionName());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        int id = arguments.getInt(ArtistOverviewFragment.BUNDLE_KEY_ARTISTID, -1);

        arguments.putInt(SongsListFragment.BUNDLE_KEY_ARTISTID, id);

        if ((container == null) || (id == -1)) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_default_view_pager, container, false);
        ButterKnife.inject(this, root);

        long baseFragmentId = id * 10;
        TabsAdapter tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(ArtistOverviewFragment.class, arguments, R.string.info,
                        baseFragmentId)
                .addTab(AlbumListFragment.class, arguments,
                        R.string.albums, baseFragmentId + 1)
                .addTab(SongsListFragment.class, arguments,
                        R.string.songs, baseFragmentId + 2);

        viewPager.setAdapter(tabsAdapter);
        pagerTabStrip.setViewPager(viewPager);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    public View getSharedElement() {
        View view = getView();
        if (view == null)
            return null;

        //Note: this works as R.id.poster is only used in ArtistShowOverviewFragment.
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
