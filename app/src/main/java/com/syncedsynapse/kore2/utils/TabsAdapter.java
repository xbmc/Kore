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
package com.syncedsynapse.kore2.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

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

        TabInfo(Class<?> fragmentClass, Bundle args, int titleRes, long fragmentId) {
            this.fragmentClass = fragmentClass;
            this.args = args;
            this.titleRes = titleRes;
            this.fragmentId = fragmentId;
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

    @Override
    public int getCount() {
        return tabInfos.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = tabInfos.get(position);
        return Fragment.instantiate(context, info.fragmentClass.getName(), info.args);
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
            return context.getString(tabInfo.titleRes);
        }
        return null;
    }
}
