/*
 * Copyright 2015 Martijn Brekhof. All rights reserved.
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import org.xbmc.kore.R;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedElementTransition;
import org.xbmc.kore.utils.TabsAdapter;
import org.xbmc.kore.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

abstract public class AbstractTabsFragment extends AbstractFragment
        implements SharedElementTransition.SharedElement {
    private static final String TAG = LogUtils.makeLogTag(AbstractTabsFragment.class);

    @BindView(R.id.pager) ViewPager viewPager;

    private Unbinder unbinder;

    /**
     * Use {@link #setDataHolder(AbstractInfoFragment.DataHolder)} to provide the required info
     * after creating a new instance of this Fragment
     */
    public AbstractTabsFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_default_view_pager, container, false);
        unbinder = ButterKnife.bind(this, root);

        viewPager.setAdapter(createTabsAdapter(getDataHolder()));
        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public boolean isSharedElementVisible() {
        View view = getView();
        if (view == null)
            return false;

        //Note: this works as R.id.poster is only used in *InfoFragment.
        //If the same id is used in other fragments in the TabsAdapter we
        //need to check which fragment is currently displayed
        View artView = view.findViewById(R.id.poster);
        View scrollView = view.findViewById(R.id.media_panel);
        if (( artView != null ) &&
            ( scrollView != null ) &&
            UIUtils.isViewInBounds(scrollView, artView)) {
            return true;
        }

        return false;
    }

    protected ViewPager getViewPager() {
        return viewPager;
    }

    /**
     * Called to get the TabsAdapter that should be connected to the ViewPager
     * @param dataHolder the data passed to the *DetailsFragment
     * @return
     */
    abstract protected TabsAdapter createTabsAdapter(AbstractInfoFragment.DataHolder dataHolder);
}
