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
package org.xbmc.kore.ui.sections.audio;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

/**
 * Container for the various music lists
 */
public class MusicListFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    // Kodi's special virtual folder that holds saved music playlists (.m3u/.pls files)
    private static final String MUSIC_PLAYLISTS_PATH = "special://musicplaylists/";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);

        getViewPager().registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getCurrentSelectedFragment();
                if (fragment instanceof AbstractCursorListFragment) {
                    ((AbstractCursorListFragment)fragment).saveSearchState();
                }
            }
        });
    }

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        return new TabsAdapter(this)
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(SongsListFragment.class, getArguments(), R.string.songs, 4)
                .addTab(MediaFileListFragment.class, createPlaylistsTabArgs(), R.string.playlists, 5);
    }

    /**
     * Builds the arguments that tell the reused {@link MediaFileListFragment} to start
     * browsing directly inside Kodi's saved music playlists folder, instead of starting
     * at the top-level file Sources list (which is what it does with no arguments).
     *
     * Tapping a playlist file plays it immediately (MediaFileListFragment's existing
     * behaviour for any non-directory item) - see Player.Open being called from
     * handleFileSelect()/playMediaFile() in that class.
     */
    private Bundle createPlaylistsTabArgs() {
        Bundle args = new Bundle();
        args.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.MUSIC);
        args.putBoolean(MediaFileListFragment.ENABLE_PLAYLIST_HANDLING, true);
        args.putParcelable(MediaFileListFragment.CURRENT_LOCATION,
                new MediaFileListFragment.FileLocation(
                        getString(R.string.playlists), // title (not shown here - this location is the root of the tab)
                        MUSIC_PLAYLISTS_PATH,           // Kodi's special path for saved music playlists
                        true,                           // isDirectory
                        null,                           // no parent - this is the top of this tab
                        true));                          // isRoot - stops back-navigation going further up
        return args;
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}
