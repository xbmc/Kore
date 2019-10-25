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

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.ui.sections.file.FileListFragment;

/**
 * Handles listing of files fragments
 */
public class LocalFileActivity extends BaseMediaActivity {
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.local_file_browser);
    }

    @Override
    protected Fragment createFragment() {
        return new LocalFileListFragment();
    }

    OnBackPressedListener fragmentBackListener;

    public void setBackPressedListener(OnBackPressedListener listener) {
        fragmentBackListener = listener;
    }

    @Override
    public void onBackPressed() {
        // tell fragment to move up one directory
        if (fragmentBackListener != null) {
            boolean handled = fragmentBackListener.onBackPressed();
            if (!handled)
                super.onBackPressed();
        }

    }
}

