/*
 * Copyright 2019 Upabjojr. All rights reserved.
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

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.utils.TabsAdapter;

import java.io.File;

/**
 * Manages the viewpager of files
 */
public class LocalFileListFragment extends AbstractTabsFragment
        implements OnBackPressedListener {

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {

        Bundle dcimFileListArgs = new Bundle();
        String dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        dcimFileListArgs.putString(LocalMediaFileListFragment.ROOT_PATH, dcim);

        Bundle directoryMusicFileListArgs = new Bundle();
        String directoryMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        directoryMusicFileListArgs.putString(LocalMediaFileListFragment.ROOT_PATH, directoryMusic);

        Bundle directoryMoviesFileListArgs = new Bundle();
        String directoryMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        directoryMoviesFileListArgs.putString(LocalMediaFileListFragment.ROOT_PATH, directoryMovies);

        Bundle externalStorageFileListArgs = new Bundle();
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        externalStorageFileListArgs.putString(LocalMediaFileListFragment.ROOT_PATH, externalStorage);

        TabsAdapter tabsAdapter = new TabsAdapter(this)
                .addTab(LocalMediaFileListFragment.class, dcimFileListArgs, R.string.dcim, 1)
                .addTab(LocalMediaFileListFragment.class, directoryMusicFileListArgs, R.string.music, 2)
                .addTab(LocalMediaFileListFragment.class, directoryMoviesFileListArgs, R.string.movies, 3)
                .addTab(LocalMediaFileListFragment.class, externalStorageFileListArgs, R.string.external_storage, 4);
        Environment.getRootDirectory();
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(requireContext(),null);
        for (int i = 0; i < externalFilesDirs.length; i++) {
            File parent = externalFilesDirs[i].getParentFile();
            if (parent == null || parent.getParentFile() == null || parent.getParentFile().getParentFile() == null)
                continue;
            File file = parent.getParentFile().getParentFile().getParentFile();
            if (file == null || file.getAbsolutePath().equals(externalStorage))
                continue;
            Bundle bundle = new Bundle();
            bundle.putString(LocalMediaFileListFragment.ROOT_PATH, file.getAbsolutePath());

            tabsAdapter.addTab(LocalMediaFileListFragment.class, bundle, file.getName(),i+2);
        }

        return tabsAdapter;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            LocalFileActivity listenerActivity = (LocalFileActivity) context;
            listenerActivity.setBackPressedListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " unable to register BackPressedListener");
        }
    }

    @Override
    public boolean onBackPressed() {
        // Tell current fragment to move up one directory, if possible
        LocalMediaFileListFragment frag = (LocalMediaFileListFragment)getCurrentSelectedFragment();
        return (frag != null) && frag.navigateToParentDir();
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}