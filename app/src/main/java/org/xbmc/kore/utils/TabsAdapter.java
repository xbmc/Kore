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
package org.xbmc.kore.utils;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

/**
 * This is a helper class that implements the management of tabs and all
 * details of connecting a ViewPager with associated TabHost.
 */
public class TabsAdapter extends FragmentStateAdapter {
    private final Context context;
    private final FragmentManager fragmentManager;
    private final ArrayList<TabInfo> tabInfos;

    public static final class TabInfo {
        private final Class<?> fragmentClass;
        private final Bundle args;
        private final int titleRes;
        private final long fragmentId;
        private final String titleString;

        TabInfo(Class<?> fragmentClass, Bundle args, int titleRes, long fragmentId) {
            this.fragmentClass = fragmentClass;
            this.args = args;
            this.titleRes = titleRes;
            this.fragmentId = fragmentId;
            this.titleString = null;
        }
        TabInfo(Class<?> fragmentClass, Bundle args, String titleString, long fragmentId) {
            this.fragmentClass = fragmentClass;
            this.args = args;
            this.titleRes = 0;
            this.fragmentId = fragmentId;
            this.titleString = titleString;
        }
    }

    public TabsAdapter(Fragment fragment) {
        super(fragment);
        this.fragmentManager = fragment.getChildFragmentManager();
        this.context = fragment.getContext();
        this.tabInfos = new ArrayList<>();
    }

    public TabsAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentManager = fragmentActivity.getSupportFragmentManager();
        this.context = fragmentActivity;
        this.tabInfos = new ArrayList<>();
    }

    public TabsAdapter addTab(Class<?> fragmentClass, Bundle args, int titleRes, long fragmentId) {
        TabInfo info = new TabInfo(fragmentClass, args, titleRes, fragmentId);
        tabInfos.add(info);
        return this;
    }

    public TabsAdapter addTab(Class<?> fragmentClass, Bundle args, String titleString, long fragmentId) {
        TabInfo info = new TabInfo(fragmentClass, args, titleString, fragmentId);
        tabInfos.add(info);
        return this;
    }

    @Override
    public int getItemCount() {
        return tabInfos.size();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        TabInfo info = tabInfos.get(position);
        Fragment fragment = fragmentManager.getFragmentFactory().instantiate(context.getClassLoader(), info.fragmentClass.getName());
        fragment.setArguments(info.args);
        return fragment;
    }

    public CharSequence getPageTitle(int position) {
        TabInfo tabInfo = tabInfos.get(position);
        if (tabInfo != null) {
            return tabInfo.titleString == null? context.getString(tabInfo.titleRes) : tabInfo.titleString;
        }
        return null;
    }
}
