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
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Fragment that presents the artists list
 */
public class MusicVideoListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(MusicVideoListFragment.class);

    public interface OnMusicVideoSelectedListener {
        public void onMusicVideoSelected(ViewHolder vh);
    }

    // Activity listener
    private OnMusicVideoSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS; }

    @Override
    protected void onListItemClicked(View view) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder)view.getTag();
        // Notify the activity
        listenerActivity.onMusicVideoSelected(tag);
    }

    @Override
    protected CursorAdapter createAdapter() {
        return new MusicVideosAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        Uri uri = MediaContract.MusicVideos.buildMusicVideosListUri(hostInfo != null ? hostInfo.getId() : -1);

        String selection = null;
        String selectionArgs[] = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.MusicVideosColumns.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(getActivity(), uri,
                MusicVideosListQuery.PROJECTION, selection, selectionArgs, MusicVideosListQuery.SORT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnMusicVideoSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMusicVideoSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
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

        String SORT = MediaDatabase.sortCommonTokens(MediaContract.MusicVideos.TITLE) + " ASC";

        final int ID = 0;
        final int MUSICVIDEOID = 1;
        final int TITLE = 2;
        final int ARTIST = 3;
        final int ALBUM = 4;
        final int THUMBNAIL = 5;
        final int RUNTIME = 6;
        final int GENRES = 7;
        final int YEAR = 8;
        final int PLOT = 9;
    }

    private static class MusicVideosAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public MusicVideosAdapter(Context context) {
            super(context, null, false);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artHeight = resources.getDimensionPixelOffset(R.dimen.musicvideodetail_poster_heigth);
            artWidth = resources.getDimensionPixelOffset(R.dimen.musicvideodetail_poster_width);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                                            .inflate(R.layout.grid_item_music_video, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.titleView = (TextView)view.findViewById(R.id.title);
            viewHolder.artistAlbumView = (TextView)view.findViewById(R.id.details);
            viewHolder.durationGenresView = (TextView)view.findViewById(R.id.duration);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @TargetApi(21)
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            // Save the movie id
            viewHolder.musicVideoId = cursor.getInt(MusicVideosListQuery.MUSICVIDEOID);
            viewHolder.musicVideoTitle = cursor.getString(MusicVideosListQuery.TITLE);
            viewHolder.album = cursor.getString(MusicVideosListQuery.ALBUM);
            viewHolder.artist = cursor.getString(MusicVideosListQuery.ARTIST);
            viewHolder.genres = cursor.getString(MusicVideosListQuery.GENRES);
            viewHolder.plot = cursor.getString(MusicVideosListQuery.PLOT);
            viewHolder.runtime = cursor.getInt(MusicVideosListQuery.RUNTIME);
            viewHolder.year = cursor.getInt(MusicVideosListQuery.YEAR);

            viewHolder.titleView.setText(viewHolder.musicVideoTitle);
            String artistAlbum = viewHolder.artist + "  |  " +
                                 viewHolder.album;
            viewHolder.artistAlbumView.setText(artistAlbum);

            String durationGenres =
                    viewHolder.runtime > 0 ?
                    UIUtils.formatTime(viewHolder.runtime) + "  |  " + viewHolder.genres :
                    viewHolder.genres;
            viewHolder.durationGenresView.setText(durationGenres);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    cursor.getString(MusicVideosListQuery.THUMBNAIL), viewHolder.musicVideoTitle,
                    viewHolder.artView, artWidth, artHeight);

            if(Utils.isLollipopOrLater()) {
                viewHolder.artView.setTransitionName("a"+viewHolder.musicVideoId);
            }
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        TextView titleView;
        TextView artistAlbumView;
        TextView durationGenresView;
        ImageView artView;

        int musicVideoId;
        String musicVideoTitle;
        String artist;
        String album;
        int runtime;
        String genres;
        int year;
        String plot;
    }
}
