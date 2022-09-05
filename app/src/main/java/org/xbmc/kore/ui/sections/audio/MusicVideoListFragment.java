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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.loader.content.CursorLoader;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Fragment that presents the artists list
 */
public class MusicVideoListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(MusicVideoListFragment.class);

    public interface OnMusicVideoSelectedListener {
        void onMusicVideoSelected(AbstractInfoFragment.DataHolder dataHolder, ImageView sharedImageView);
    }

    // Activity listener
    private OnMusicVideoSelectedListener listenerActivity;

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        try {
            listenerActivity = (OnMusicVideoSelectedListener) ctx;
        } catch (ClassCastException e) {
            throw new ClassCastException(ctx + " must implement OnMusicVideoSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS; }

    @Override
    protected void onListItemClicked(View view, int position) {
        super.onListItemClicked(view, position);
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder)view.getTag();
        // Notify the activity
        listenerActivity.onMusicVideoSelected(tag.dataHolder, tag.artView);
    }

    @Override
    protected String getEmptyResultsTitle() { return getString(R.string.no_music_videos_found_refresh); }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new MusicVideosAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        Uri uri = MediaContract.MusicVideos.buildMusicVideosListUri(hostInfo != null ? hostInfo.getId() : -1);

        String selection = null;
        String[] selectionArgs = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.MusicVideosColumns.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(requireContext(), uri,
                MusicVideosListQuery.PROJECTION, selection, selectionArgs, MusicVideosListQuery.SORT);
    }

    /**
     * Videos list query parameters.
     */
    private interface MusicVideosListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.MusicVideos.MUSICVIDEOID,
                MediaContract.MusicVideos.TITLE,
                MediaContract.MusicVideos.ARTIST,
                MediaContract.MusicVideos.ALBUM,
                MediaContract.MusicVideos.THUMBNAIL,
                MediaContract.MusicVideos.RUNTIME,
                MediaContract.MusicVideos.GENRES,
                MediaContract.MusicVideos.YEAR,
                MediaContract.MusicVideos.PLOT,
        };

        String SORT = MediaDatabase.sortCommonTokens(MediaContract.MusicVideos.TITLE) + " COLLATE NOCASE ASC";

        int ID = 0;
        int MUSICVIDEOID = 1;
        int TITLE = 2;
        int ARTIST = 3;
        int ALBUM = 4;
        int THUMBNAIL = 5;
        int RUNTIME = 6;
        int GENRES = 7;
        int YEAR = 8;
        int PLOT = 9;
    }

    private static class MusicVideosAdapter extends RecyclerViewCursorAdapter {

        private final HostManager hostManager;
        private final int artWidth, artHeight;
        private final Context context;

        public MusicVideosAdapter(Context context) {
            this.context = context;

            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artHeight = resources.getDimensionPixelOffset(R.dimen.info_poster_width_square);
            artWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_height_square);
        }

        @NonNull
        @Override
        public CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                                      .inflate(R.layout.item_music_generic, parent, false);
            return new ViewHolder(view, context, hostManager, artWidth, artHeight);
        }
        protected int getSectionColumnIdx() { return MusicVideosListQuery.TITLE; }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView titleView;
        TextView artistAlbumView;
        TextView durationGenresView;
        ImageView artView, contextMenu;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            titleView = itemView.findViewById(R.id.title);
            artistAlbumView = itemView.findViewById(R.id.details);
            durationGenresView = itemView.findViewById(R.id.other_info);
            artView = itemView.findViewById(R.id.art);
            contextMenu = itemView.findViewById(R.id.list_context_menu);
        }
        @Override
        public void bindView(Cursor cursor) {
            dataHolder.setId(cursor.getInt(MusicVideosListQuery.MUSICVIDEOID));
            dataHolder.setTitle(cursor.getString(MusicVideosListQuery.TITLE));

            titleView.setText(dataHolder.getTitle());
            String artistAlbum = cursor.getString(MusicVideosListQuery.ARTIST) + "  |  " +
                                 cursor.getString(MusicVideosListQuery.ALBUM);
            artistAlbumView.setText(artistAlbum);
            dataHolder.setUndertitle(artistAlbum);

            int runtime = cursor.getInt(MusicVideosListQuery.RUNTIME);
            String genres = cursor.getString(MusicVideosListQuery.GENRES);
            String durationGenres =
                    runtime > 0 ?
                    UIUtils.formatTime(runtime) + "  |  " + genres :
                    genres;
            durationGenresView.setText(durationGenres);
            dataHolder.setDetails(durationGenres);

            String posterUrl = cursor.getString(MusicVideosListQuery.THUMBNAIL);
            dataHolder.setPosterUrl(posterUrl);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager, posterUrl
                    , dataHolder.getTitle(), artView, artWidth, artHeight);
            contextMenu.setVisibility(View.GONE);
            artView.setTransitionName("music_video"+dataHolder.getId());
        }
    }
}
