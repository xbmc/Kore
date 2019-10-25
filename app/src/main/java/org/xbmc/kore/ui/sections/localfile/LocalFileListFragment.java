/*
 * Copyright 2015 DanhDroid. All rights reserved.
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
package org.xbmc.kore.ui.sections.localfile;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.utils.TabsAdapter;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Manages the viewpager of files
 */
public class LocalFileListFragment extends AbstractTabsFragment
        implements OnBackPressedListener {

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        ListType.Sort sortMethod = new ListType.Sort(ListType.Sort.SORT_METHOD_PATH, true, true);

        Bundle dcimFileListArgs = new Bundle();
        String dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        dcimFileListArgs.putString(LocalPictureListFragment.ROOT_PATH_LOCATION, dcim);
        dcimFileListArgs.putParcelable(LocalPictureListFragment.SORT_METHOD, sortMethod);

        Bundle externalStorageFileListArgs = new Bundle();
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        externalStorageFileListArgs.putString(LocalPictureListFragment.ROOT_PATH_LOCATION, externalStorage);
        externalStorageFileListArgs.putParcelable(LocalPictureListFragment.SORT_METHOD, sortMethod);

        TabsAdapter tabsAdapter = new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(LocalPictureListFragment.class, dcimFileListArgs, R.string.dcim, 1)
                .addTab(LocalPictureListFragment.class, externalStorageFileListArgs, R.string.external_storage, 2);
        Environment.getRootDirectory();
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(getActivity(),null);
        for (int i = 0; i < externalFilesDirs.length; i++) {
            File file = externalFilesDirs[i].getParentFile().getParentFile().getParentFile().getParentFile();

            if (file.getAbsolutePath().equals(externalStorage))
                continue;
            Bundle bundle = new Bundle();
            bundle.putString(LocalPictureListFragment.ROOT_PATH_LOCATION, file.getAbsolutePath());
            bundle.putParcelable(LocalPictureListFragment.SORT_METHOD, sortMethod);

            tabsAdapter.addTab(LocalPictureListFragment.class, bundle, file.getName(),i+2);
        }

        return tabsAdapter;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            LocalFileActivity listenerActivity = (LocalFileActivity) activity;
            listenerActivity.setBackPressedListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " unable to register BackPressedListener");
        }
    }

    @Override
    public boolean onBackPressed() {
        // Tell current fragment to move up one directory, if possible
        LocalPictureListFragment curPage = (LocalPictureListFragment)((TabsAdapter)getViewPager().getAdapter())
                .getStoredFragment(getViewPager().getCurrentItem());
        if ((curPage != null) && !curPage.atRootDirectory()) {
            curPage.onBackPressed();
            return true;
        }

        // Not handled, let the activity handle it
        return false;
    }
}