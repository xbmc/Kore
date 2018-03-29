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
package org.xbmc.kore.ui.sections.file;

import android.app.Activity;
import android.os.Bundle;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.utils.TabsAdapter;

/**
 * Manages the viewpager of files
 */
public class FileListFragment extends AbstractTabsFragment
        implements OnBackPressedListener {

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        Bundle videoFileListArgs = new Bundle();
        videoFileListArgs.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.VIDEO);
        Bundle musicFileListArgs = new Bundle();
        musicFileListArgs.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.MUSIC);
        Bundle pictureFileListArgs = new Bundle();
        pictureFileListArgs.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.PICTURES);
        return new TabsAdapter(getActivity(), getChildFragmentManager())
                .addTab(MediaFileListFragment.class, videoFileListArgs, R.string.video, 1)
                .addTab(MediaFileListFragment.class, musicFileListArgs, R.string.music, 2)
                .addTab(MediaFileListFragment.class, pictureFileListArgs, R.string.pictures, 3);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            FileActivity listenerActivity = (FileActivity) activity;
            listenerActivity.setBackPressedListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " unable to register BackPressedListener");
        }
    }

    @Override
    public boolean onBackPressed() {
        // Tell current fragment to move up one directory, if possible
        MediaFileListFragment curPage = (MediaFileListFragment)((TabsAdapter)getViewPager().getAdapter())
                .getStoredFragment(getViewPager().getCurrentItem());
        if ((curPage != null) && !curPage.atRootDirectory()) {
            curPage.onBackPressed();
            return true;
        }

        // Not handled, let the activity handle it
        return false;
    }
}