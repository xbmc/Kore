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
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import org.xbmc.kore.databinding.FragmentDefaultViewPagerBinding;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

abstract public class AbstractTabsFragment
        extends AbstractFragment {
    private static final String TAG = LogUtils.makeLogTag(AbstractTabsFragment.class);
    public static final String PREFERENCES_NAME = "AbstractTabsFragmentPreferences";
    private static final String PREFERENCE_PREFIX_LAST_TAB = "lastTab_";

    FragmentDefaultViewPagerBinding binding;

    private SharedPreferences preferences;

    /**
     * Set args with {@link #(AbstractInfoFragment.DataHolder)} to provide the required info
     */
    public AbstractTabsFragment() {
        super();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        preferences = requireContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        binding = FragmentDefaultViewPagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);

        binding.pager.setOffscreenPageLimit(getOffscreenPageLimit());
        TabsAdapter tabsAdapter = createTabsAdapter(getDataHolder());
        binding.pager.setAdapter(tabsAdapter);
        new TabLayoutMediator(binding.tabLayout, binding.pager,
                              (tab, position) -> tab.setText(tabsAdapter.getPageTitle(position)))
                .attach();

        if (shouldRememberLastTab()) {
            binding.pager.setCurrentItem(preferences.getInt(PREFERENCE_PREFIX_LAST_TAB + getClass().getName(), 0), false);
        }

        if (shouldPostponeReenterTransition) {
            // We are in a reenter transition with a shared element transition
            // Note that the shared element will be made available in a *child* fragment of the ViewPager, but the
            // postponing is done here, otherwise it doesn't work.
            // The child fragment needs to send the Event Bus message ListFragmentSetupComplete so that we can
            // start the postponed transition (done in AbstractFragment)
            postponeEnterTransition();
        }
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    protected Fragment getCurrentSelectedFragment() {
        return getChildFragmentManager().findFragmentByTag("f" + binding.pager.getCurrentItem());
    }

    protected ViewPager2 getViewPager() {
        return binding.pager;
    }

    /**
     * Override to specify the OffscreenPageLimit to set on the ViewPager2
     * @return OffscreenPageLimit to set on the ViewPager2
     */
    protected int getOffscreenPageLimit() {
        return 2;
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
