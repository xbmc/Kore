/*
 * Copyright 2018 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.google.android.material.tabs.TabLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.type.PlaylistType;

import java.util.ArrayList;

public class PlaylistsBar extends TabLayout {

    private int highlightColor;
    private int defaultColor;

    public interface OnPlaylistSelectedListener {
        void onPlaylistSelected(String playlistType);
        void onPlaylistDeselected(String playlistType);
    }

    final Handler handler = new Handler(Looper.getMainLooper());

    private final ArrayList<TabState> tabStates = new ArrayList<>();

    public PlaylistsBar(Context context) {
        super(context);
        init(context);
    }

    public PlaylistsBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlaylistsBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.playlist_bar, this);

        setStyle(context);

        for(int i = 0; i < getTabCount(); i++) {
            Tab tab = getTabAt(i);
            if (tab == null) continue;
            TabState tabState = new TabState();
            tabState.position = i;
            tabState.icon = (tab.getIcon() != null) ? tab.getIcon().mutate() : null;
            tabState.setEnabled(false);
            tabStates.add(tabState);
        }
    }

    /**
     * Sets the tab for the given playlist type to selected state.
     * Note: this does not call the {@link OnPlaylistSelectedListener#onPlaylistSelected(String)}
     */
    public void selectTab(String playlistType) {
        Tab tab = getTabAt(getTabPositionForType(playlistType));
        if (tab == null) return;
        tab.setTag(new Object()); // Make we do not trigger OnPlaylistSelectedListener
        tab.select();
    }

    public void setHasPlaylistAvailable(String playlistType, boolean playlistAvailable) {
        tabStates.get(getTabPositionForType(playlistType)).setEnabled(playlistAvailable);
    }

    private Runnable runnable;

    public void setIsPlaying(final String playlistType, final boolean isPlaying) {
        handler.removeCallbacks(runnable);

        runnable = () -> {
            TabState tabStatePlaying = tabStates.get(getTabPositionForType(playlistType));
            tabStatePlaying.setPlaying(isPlaying);

            for (TabState tabState : tabStates) {
                if (tabStatePlaying != tabState)
                    tabState.setPlaying(false);
            }
        };

        handler.postDelayed(runnable, 1000);
    }

    public String getTypeForTabPosition(int tabPosition) {
        switch (tabPosition) {
            case 0:
                return PlaylistType.GetPlaylistsReturnType.VIDEO;
            case 1:
                return PlaylistType.GetPlaylistsReturnType.AUDIO;
            case 2:
                return PlaylistType.GetPlaylistsReturnType.PICTURE;
            default:
                return PlaylistType.GetPlaylistsReturnType.VIDEO;
        }
    }

    public String getSelectedPlaylistType() {
        return getTypeForTabPosition(getSelectedTabPosition());
    }

    public void setOnPlaylistSelectedListener(final OnPlaylistSelectedListener onPlaylistSelectedListener) {
        addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                if (tab.getTag() == null)
                    onPlaylistSelectedListener.onPlaylistSelected(getTypeForTabPosition(tab.getPosition()));

                tab.setTag(null);
            }

            @Override
            public void onTabUnselected(Tab tab) {
                onPlaylistSelectedListener.onPlaylistDeselected(getTypeForTabPosition(tab.getPosition()));
            }

            @Override
            public void onTabReselected(Tab tab) {
                tab.setTag(null);
            }
        });
    }

    private int getTabPositionForType(String playlistType) {
        switch(playlistType) {
            case PlaylistType.GetPlaylistsReturnType.VIDEO:
                return 0;
            case PlaylistType.GetPlaylistsReturnType.AUDIO:
                return 1;
            case PlaylistType.GetPlaylistsReturnType.PICTURE:
                return 2;
            default:
                return 0;
        }
    }

    private void setStyle(Context context) {
        TypedArray styledAttributes = context.getTheme()
                                             .obtainStyledAttributes(new int[]{ R.attr.colorAccent,
                                                                                R.attr.defaultButtonColorFilter });
        highlightColor = styledAttributes.getColor(styledAttributes.getIndex(0),
                                                   context.getResources().getColor(R.color.default_accent));
        defaultColor = styledAttributes.getColor(styledAttributes.getIndex(1),
                                                 context.getResources().getColor(R.color.white));
        styledAttributes.recycle();
    }

    private class TabState {
        boolean enabled;
        boolean isPlaying;
        int position;
        Drawable icon;

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if(enabled) {
                icon.setAlpha(255);
            } else {
                icon.setAlpha(127);
                setPlaying(false);
            }
        }

        public void setPlaying(boolean playing) {
            isPlaying = playing;
            if (playing) {
                icon.setColorFilter(highlightColor, PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.setColorFilter(defaultColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }
}
