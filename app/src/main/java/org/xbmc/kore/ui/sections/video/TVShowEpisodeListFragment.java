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
package org.xbmc.kore.ui.sections.video;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.CursorLoader;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Presents a list of episodes for a TV show season
 */
public class TVShowEpisodeListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(TVShowEpisodeListFragment.class);

    public interface OnEpisodeSelectedListener {
        void onEpisodeSelected(int tvshowId, ViewHolder dataHolder);
    }

    public static final String TVSHOWID = "tvshow_id";
    public static final String TVSHOWSEASON = "season";

    // Displayed show id
    private int tvshowId = -1;
    private int tvshowSeason = -1;

    // Activity listener
    private OnEpisodeSelectedListener listenerActivity;

    /**
     * Create a new instance of this, initialized to show tvshowId
     */
    public static TVShowEpisodeListFragment newInstance(int tvshowId, int season) {
        TVShowEpisodeListFragment fragment = new TVShowEpisodeListFragment();

        Bundle args = new Bundle();
        args.putInt(TVSHOWID, tvshowId);
        args.putInt(TVSHOWSEASON, season);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_SINGLE_TVSHOW; }

    @Override
    protected String getSyncID() { return LibrarySyncService.SYNC_TVSHOWID; }

    @Override
    protected int getSyncItemID() { return tvshowId; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();
        tvshowId = (args == null) ? -1 : args.getInt(TVSHOWID, -1);
        tvshowSeason = (args == null) ? -1 : args.getInt(TVSHOWSEASON, -1);
        if ((tvshowId == -1) || (tvshowSeason == -1)) {
            // There's nothing to show
            return null;
        }
        return root;
    }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onEpisodeSelected(tvshowId, tag);
    }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new SeasonsEpisodesAdapter(requireContext());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        Uri uri = MediaContract.Episodes.buildTVShowSeasonEpisodesListUri(hostInfo.getId(), tvshowId, tvshowSeason);

        // Filters
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        StringBuilder selection = new StringBuilder();
        if (preferences.getBoolean(Settings.KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED)) {
            selection.append(MediaContract.EpisodesColumns.PLAYCOUNT)
                     .append("=0");
        }

        return new CursorLoader(requireContext(), uri,
                                EpisodesListQuery.PROJECTION, selection.toString(), null, EpisodesListQuery.SORT);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listenerActivity = (OnEpisodeSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnEpisodeSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            // HACK: Fix crash reported on Play Store. Why does this is necessary is beyond me
            super.onCreateOptionsMenu(menu, inflater);
            return;
        }
        inflater.inflate(R.menu.tvshow_episode_list, menu);

        // Setup filters
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        menu.findItem(R.id.action_hide_watched)
            .setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED));

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_hide_watched) {
            item.setChecked(!item.isChecked());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED, item.isChecked())
                       .apply();
            refreshList();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Episodes list query parameters.
     */
    private interface EpisodesListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Episodes.EPISODEID,
                MediaContract.Episodes.EPISODE,
                MediaContract.Episodes.THUMBNAIL,
                MediaContract.Episodes.PLAYCOUNT,
                MediaContract.Episodes.TITLE,
                MediaContract.Episodes.RUNTIME,
                MediaContract.Episodes.FIRSTAIRED,
        };

        String SORT = MediaContract.Episodes.EPISODE + " ASC";

        int ID = 0;
        int EPISODEID = 1;
        int EPISODE = 2;
        int THUMBNAIL = 3;
        int PLAYCOUNT = 4;
        int TITLE = 5;
        int RUNTIME = 6;
        int FIRSTAIRED = 7;
    }


    private class SeasonsEpisodesAdapter extends RecyclerViewCursorAdapter {

        private final int themeAccentColor;
        private final HostManager hostManager;
        private final int artWidth, artHeight;

        SeasonsEpisodesAdapter(Context context) {
            // Get the default accent color
            Resources.Theme theme = context.getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorAccent
            });
            themeAccentColor = styledAttributes.getColor(styledAttributes.getIndex(0), getResources().getColor(R.color.default_accent));
            styledAttributes.recycle();

            hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.episodelist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.episodelist_art_heigth) /
                              UIUtils.IMAGE_RESIZE_FACTOR);
        }

        @NonNull
        @Override
        public RecyclerViewCursorAdapter.CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                                      .inflate(R.layout.list_item_episode, parent, false);
            return new ViewHolder(view, requireContext(), themeAccentColor,
                                  contextlistItemMenuClickListener, hostManager,
                                  artWidth, artHeight);
        }

        protected int getSectionColumnIdx() { return EpisodesListQuery.TITLE; }
    }

    /**
     * View holder pattern, only for episodes
     */
    static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView episodenumberView;
        ImageView contextMenuView;
        ImageView checkmarkView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;
        int themeAccentColor;

        AbstractFragment.DataHolder dataHolder = new AbstractFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, int themeAccentColor,
                   View.OnClickListener contextlistItemMenuClickListener,
                   HostManager hostManager,
                   int artWidth, int artHeight) {
            super(itemView);
            this.context = context;
            this.themeAccentColor = themeAccentColor;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            episodenumberView = itemView.findViewById(R.id.episode_number);
            contextMenuView = itemView.findViewById(R.id.list_context_menu);
            checkmarkView = itemView.findViewById(R.id.checkmark);
            artView = itemView.findViewById(R.id.art);
            contextMenuView.setOnClickListener(contextlistItemMenuClickListener);
        }

        @Override
        public void bindView(Cursor cursor) {
            // Save the episode id
            dataHolder.setId(cursor.getInt(EpisodesListQuery.EPISODEID));
            dataHolder.setTitle(cursor.getString(EpisodesListQuery.TITLE));

            episodenumberView.setText(
                    String.format(context.getString(R.string.episode_number),
                                  cursor.getInt(EpisodesListQuery.EPISODE)));
            int runtime = cursor.getInt(EpisodesListQuery.RUNTIME) / 60;
            String duration = runtime > 0 ?
                              String.format(context.getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                              "  |  " + cursor.getString(EpisodesListQuery.FIRSTAIRED) :
                              cursor.getString(EpisodesListQuery.FIRSTAIRED);
            titleView.setText(cursor.getString(EpisodesListQuery.TITLE));
            detailsView.setText(duration);

            if (cursor.getInt(EpisodesListQuery.PLAYCOUNT) > 0) {
                checkmarkView.setVisibility(View.VISIBLE);
                checkmarkView.setColorFilter(themeAccentColor);
            } else {
                checkmarkView.setVisibility(View.INVISIBLE);
            }

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 cursor.getString(EpisodesListQuery.THUMBNAIL),
                                                 dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            contextMenuView.setTag(this);
        }
    }

    private final View.OnClickListener contextlistItemMenuClickListener = v -> {
        final ViewHolder viewHolder = (ViewHolder)v.getTag();

        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.episodeid = viewHolder.dataHolder.getId();

        final PopupMenu popupMenu = new PopupMenu(requireContext(), v);
        popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play) {
                MediaPlayerUtils.play(TVShowEpisodeListFragment.this, playListItem);
                return true;
            } else if (itemId == R.id.action_queue) {
                MediaPlayerUtils.queue(TVShowEpisodeListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.VIDEO);
                return true;
            }
            return false;
        });
        popupMenu.show();
    };

}
