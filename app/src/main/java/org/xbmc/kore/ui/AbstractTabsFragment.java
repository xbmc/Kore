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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentDefaultViewPagerBinding;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedElementTransition;
import org.xbmc.kore.utils.TabsAdapter;
import org.xbmc.kore.utils.UIUtils;

abstract public class AbstractTabsFragment
        extends AbstractFragment
        implements SharedElementTransition.SharedElement {
    private static final String TAG = LogUtils.makeLogTag(AbstractTabsFragment.class);
    public static final String PREFERENCES_NAME = "AbstractTabsFragmentPreferences";
    private static final String PREFERENCE_PREFIX_LAST_TAB = "lastTab_";

    FragmentDefaultViewPagerBinding binding;

    private SharedPreferences preferences;

    /**
     * Use {@link #setDataHolder(AbstractInfoFragment.DataHolder)} to provide the required info
     * after creating a new instance of this Fragment
     */
    public AbstractTabsFragment() {
        super();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We're not being shown or there's nothing to show
            return null;
        }

        preferences = requireContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        binding = FragmentDefaultViewPagerBinding.inflate(inflater, container, false);

        binding.pager.setAdapter(createTabsAdapter(getDataHolder()));

        if (shouldRememberLastTab()) {
            binding.pager.setCurrentItem(preferences.getInt(PREFERENCE_PREFIX_LAST_TAB + getClass().getName(), 0), false);
        }
        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (shouldRememberLastTab()) {
            preferences.edit()
                    .putInt(PREFERENCE_PREFIX_LAST_TAB + getClass().getName(), binding.pager.getCurrentItem())
                    .apply();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        return (artView != null) &&
               (scrollView != null) &&
               UIUtils.isViewInBounds(scrollView, artView);
    }

    protected ViewPager getViewPager() {
        return binding.pager;
    }

    /**
     * Called to get the TabsAdapter that should be connected to the ViewPager
     * @param dataHolder the data passed to the *DetailsFragment
     * @return Tabs Adapter
     */
    abstract protected TabsAdapter createTabsAdapter(AbstractInfoFragment.DataHolder dataHolder);

    /**
     * Specifies whether to store the last-used tab.
     * @return <code>true</code> if the fragment should remember the last-used tab.
     */
    abstract protected boolean shouldRememberLastTab();
}
