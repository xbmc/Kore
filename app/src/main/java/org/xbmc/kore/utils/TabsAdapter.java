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
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a helper class that implements the management of tabs and all
 * details of connecting a ViewPager with associated TabHost.
 */
public class TabsAdapter extends FragmentPagerAdapter {
    private final Context context;
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

    public TabsAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        this.context = context;
        this.tabInfos = new ArrayList<TabInfo>();
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
    public int getCount() {
        return tabInfos.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = tabInfos.get(position);
        return Fragment.instantiate(context, info.fragmentClass.getName(), info.args);
    }

    /**
     * Store the created fragments, so that it is possible to get them by position later
     */
    private HashMap<Integer, Fragment> createdFragments = new HashMap<>(5);

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment)super.instantiateItem(container, position);
        createdFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem (ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        createdFragments.remove(position);
    }

    public Fragment getStoredFragment(int position) {
        return createdFragments.get(position);
    }
    @Override
    public long getItemId(int position) {
        return tabInfos.get(position).fragmentId;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        TabInfo tabInfo = tabInfos.get(position);
        if (tabInfo != null) {
//            return context.getString(tabInfo.titleRes).toUpperCase(Locale.getDefault());
            return tabInfo.titleString == null? context.getString(tabInfo.titleRes) : tabInfo.titleString;
        }
        return null;
    }
}
