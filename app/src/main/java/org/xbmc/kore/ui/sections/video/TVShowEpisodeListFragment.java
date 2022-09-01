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
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.preference.PreferenceManager;

import com.google.android.material.color.MaterialColors;

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
        void onEpisodeSelected(int tvshowId, ViewHolder viewHolder);
    }

    public static final String TVSHOWID = "tvshow_id";
    public static final String TVSHOWSEASON = "season";
    public static final String TVSHOWSEASONPOSTERURL = "season_poster_url";

    // Displayed show id
    private int tvshowId = -1;
    private int tvshowSeason = -1;
    private String tvshowSeasonPosterUrl;

    // Activity listener
    private OnEpisodeSelectedListener listenerActivity;

    /**
     * Create a new instance of this, initialized to show tvshowId
     */
    public static TVShowEpisodeListFragment newInstance(int tvshowId, int season, String seasonPosterUrl) {
        TVShowEpisodeListFragment fragment = new TVShowEpisodeListFragment();

        Bundle args = new Bundle();
        args.putInt(TVSHOWID, tvshowId);
        args.putInt(TVSHOWSEASON, season);
        args.putString(TVSHOWSEASONPOSTERURL, seasonPosterUrl);
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
        tvshowSeasonPosterUrl = (args == null) ? null : args.getString(TVSHOWSEASONPOSTERURL, null);
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
    protected String getEmptyResultsTitle() { return getString(R.string.no_episodes_found); }

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
            restartLoader();
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

        private final int statusWatchedColor;
        private final HostManager hostManager;
        private final int artWidth, artHeight;

        SeasonsEpisodesAdapter(Context context) {
            statusWatchedColor = MaterialColors.getColor(context, R.attr.colorFinished, Color.WHITE);
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
                                      .inflate(R.layout.item_tvshow_episode, parent, false);
            return new ViewHolder(view, requireContext(), statusWatchedColor,
                                  contextlistItemMenuClickListener, hostManager,
                                  artWidth, artHeight, tvshowSeasonPosterUrl);
        }

        protected int getSectionColumnIdx() { return EpisodesListQuery.TITLE; }
    }

    /**
     * View holder pattern, only for episodes
     */
    static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView durationView;
        ImageView contextMenuView;
        ImageView watchedCheckView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;
        int statusWatchedColor;
        String tvshowSeasonPosterUrl;

        AbstractFragment.DataHolder dataHolder = new AbstractFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, int statusWatchedColor,
                   View.OnClickListener contextlistItemMenuClickListener,
                   HostManager hostManager,
                   int artWidth, int artHeight, String tvshowSeasonPosterUrl) {
            super(itemView);
            this.context = context;
            this.statusWatchedColor = statusWatchedColor;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            this.tvshowSeasonPosterUrl = tvshowSeasonPosterUrl;
            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            durationView = itemView.findViewById(R.id.duration);
            contextMenuView = itemView.findViewById(R.id.list_context_menu);
            watchedCheckView = itemView.findViewById(R.id.watched_check);
            artView = itemView.findViewById(R.id.art);
            contextMenuView.setOnClickListener(contextlistItemMenuClickListener);
        }

        @Override
        public void bindView(Cursor cursor) {
            // Save the episode id
            dataHolder.setId(cursor.getInt(EpisodesListQuery.EPISODEID));
            dataHolder.setTitle(cursor.getString(EpisodesListQuery.TITLE));
            dataHolder.setPosterUrl(tvshowSeasonPosterUrl);

            String title = cursor.getString(EpisodesListQuery.TITLE);
            String seasonEpisode = String.format(context.getString(R.string.episode_number),
                                                 cursor.getInt(EpisodesListQuery.EPISODE));
            int runtime = cursor.getInt(EpisodesListQuery.RUNTIME) / 60;
            String duration = runtime > 0 ?
                              String.format(context.getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                              "  |  " + cursor.getString(EpisodesListQuery.FIRSTAIRED) :
                              cursor.getString(EpisodesListQuery.FIRSTAIRED);

            titleView.setText(title);
            detailsView.setText(seasonEpisode);
            durationView.setText(duration);

            if (cursor.getInt(EpisodesListQuery.PLAYCOUNT) > 0) {
                watchedCheckView.setVisibility(View.VISIBLE);
                watchedCheckView.setColorFilter(statusWatchedColor);
            } else {
                watchedCheckView.setVisibility(View.INVISIBLE);
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
