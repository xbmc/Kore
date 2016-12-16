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
package org.xbmc.kore.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.CursorAdapter;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Presents a list of episodes for a TV show season
 */
public class TVShowEpisodeListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(TVShowEpisodeListFragment.class);

    public interface OnEpisodeSelectedListener {
        void onEpisodeSelected(EpisodeViewHolder vh);
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
    @TargetApi(21)
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
    protected String getSyncID() { return LibrarySyncService.SYNC_TVSHOWID; };

    @Override
    protected int getSyncItemID() { return tvshowId; };

    @TargetApi(16) @Nullable @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        tvshowId = getArguments().getInt(TVSHOWID, -1);
        tvshowSeason = getArguments().getInt(TVSHOWSEASON, -1);
        if ((tvshowId == -1) || (tvshowSeason == -1)) {
            // There's nothing to show
            return null;
        }
        return root;
    }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        EpisodeViewHolder tag = (EpisodeViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onEpisodeSelected(tag);
    }


    @Override
    protected CursorAdapter createAdapter() {
        return new SeasonsEpisodesAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        Uri uri = MediaContract.Episodes.buildTVShowSeasonEpisodesListUri(hostInfo.getId(), tvshowId, tvshowSeason);

        // Filters
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        StringBuilder selection = new StringBuilder();
        if (preferences.getBoolean(Settings.KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED)) {
            selection.append(MediaContract.EpisodesColumns.PLAYCOUNT)
                     .append("=0");
        }

        return new CursorLoader(getActivity(), uri,
                                EpisodesListQuery.PROJECTION, selection.toString(), null, EpisodesListQuery.SORT);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listenerActivity = (OnEpisodeSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnEpisodeSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            // HACK: Fix crash reported on Play Store. Why does this is necessary is beyond me
            super.onCreateOptionsMenu(menu, inflater);
            return;
        }
        inflater.inflate(R.menu.tvshow_episode_list, menu);

        // Setup filters
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menu.findItem(R.id.action_hide_watched)
            .setChecked(preferences.getBoolean(Settings.KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED));

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_hide_watched:
                item.setChecked(!item.isChecked());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                preferences.edit()
                           .putBoolean(Settings.KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED, item.isChecked())
                           .apply();
                refreshList();
                break;
            default:
                break;
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


    private class SeasonsEpisodesAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;
        private int themeAccentColor;

        public SeasonsEpisodesAdapter(Context context) {
            super(context, null, false);

            // Get the default accent color
            Resources.Theme theme = context.getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorAccent
            });

            themeAccentColor = styledAttributes.getColor(styledAttributes.getIndex(0), getResources().getColor(R.color.accent_default));
            styledAttributes.recycle();

            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.episodelist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.episodelist_art_heigth) /
                              UIUtils.IMAGE_RESIZE_FACTOR);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                                            .inflate(R.layout.list_item_episode, parent, false);

            // Setup View holder pattern
            EpisodeViewHolder viewHolder = new EpisodeViewHolder();
            viewHolder.titleView = (TextView)view.findViewById(R.id.title);
            viewHolder.detailsView = (TextView)view.findViewById(R.id.details);
            viewHolder.episodenumberView = (TextView)view.findViewById(R.id.episode_number);
            viewHolder.contextMenuView = (ImageView)view.findViewById(R.id.list_context_menu);
            viewHolder.checkmarkView = (ImageView)view.findViewById(R.id.checkmark);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final EpisodeViewHolder viewHolder = (EpisodeViewHolder)view.getTag();

            // Save the episode id
            viewHolder.episodeId = cursor.getInt(EpisodesListQuery.EPISODEID);
            viewHolder.title = cursor.getString(EpisodesListQuery.TITLE);

            if(Utils.isLollipopOrLater()) {
                viewHolder.artView.setTransitionName("a"+viewHolder.episodeId);
            }

            viewHolder.episodenumberView.setText(
                    String.format(context.getString(R.string.episode_number),
                                  cursor.getInt(EpisodesListQuery.EPISODE)));
            int runtime = cursor.getInt(EpisodesListQuery.RUNTIME) / 60;
            String duration =  runtime > 0 ?
                               String.format(context.getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                               "  |  " + cursor.getString(EpisodesListQuery.FIRSTAIRED) :
                               cursor.getString(EpisodesListQuery.FIRSTAIRED);
            viewHolder.titleView.setText(cursor.getString(EpisodesListQuery.TITLE));
            viewHolder.detailsView.setText(duration);

            if (cursor.getInt(EpisodesListQuery.PLAYCOUNT) > 0) {
                viewHolder.checkmarkView.setVisibility(View.VISIBLE);
                viewHolder.checkmarkView.setColorFilter(themeAccentColor);
            } else {
                viewHolder.checkmarkView.setVisibility(View.INVISIBLE);
            }

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 cursor.getString(EpisodesListQuery.THUMBNAIL), viewHolder.title,
                                                 viewHolder.artView, artWidth, artHeight);

            // For the popupmenu
            ImageView contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
            contextMenu.setTag(viewHolder);
            contextMenu.setOnClickListener(contextlistItemMenuClickListener);
        }
    }

    /**
     * View holder pattern, only for episodes
     */
    public static class EpisodeViewHolder {
        TextView titleView;
        TextView detailsView;
        TextView episodenumberView;
        ImageView contextMenuView;
        ImageView checkmarkView;
        ImageView artView;

        int episodeId;
        String title;
    }

    private View.OnClickListener contextlistItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final EpisodeViewHolder viewHolder = (EpisodeViewHolder)v.getTag();

            final PlaylistType.Item playListItem = new PlaylistType.Item();
            playListItem.episodeid = viewHolder.episodeId;

            final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_play:
                            MediaPlayerUtils.play(TVShowEpisodeListFragment.this, playListItem);
                            return true;
                        case R.id.action_queue:
                            MediaPlayerUtils.queue(TVShowEpisodeListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.VIDEO);
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };

}
