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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Fragment that presents the artists list
 */
public class ArtistListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(ArtistListFragment.class);

    public interface OnArtistSelectedListener {
        public void onArtistSelected(ViewHolder tag);
    }

    // Activity listener
    private OnArtistSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    @Override
    protected void onListItemClicked(View view) {
        // Get the artist id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onArtistSelected(tag);
    }

    @Override
    protected CursorAdapter createAdapter() {
        return new ArtistsAdapter(getActivity());
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        Uri uri = MediaContract.Artists.buildArtistsListUri(hostInfo != null ? hostInfo.getId() : -1);

        String selection = null;
        String selectionArgs[] = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.ArtistsColumns.ARTIST + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(getActivity(), uri,
                ArtistListQuery.PROJECTION, selection, selectionArgs, ArtistListQuery.SORT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArtistSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    /**
     * Artist list query parameters.
     */
    private interface ArtistListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Artists.ARTISTID,
                MediaContract.Artists.ARTIST,
                MediaContract.Artists.GENRE,
                MediaContract.Artists.THUMBNAIL,
                MediaContract.Artists.DESCRIPTION,
                MediaContract.Artists.FANART
        };

        String SORT = MediaDatabase.sortCommonTokens(MediaContract.Artists.ARTIST) + " COLLATE NOCASE ASC";

        int ID = 0;
        int ARTISTID = 1;
        int ARTIST = 2;
        int GENRE = 3;
        int THUMBNAIL = 4;
        int DESCRIPTION = 5;
        int FANART = 6;
    }

    private class ArtistsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public ArtistsAdapter(Context context) {
            super(context, null, 0);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.detail_poster_width_square));
            artHeight = (int)(resources.getDimension(R.dimen.detail_poster_height_square));
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                    .inflate(R.layout.grid_item_artist, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.nameView = (TextView)view.findViewById(R.id.name);
            viewHolder.genresView = (TextView)view.findViewById(R.id.genres);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);
            viewHolder.contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
            view.setTag(viewHolder);

            return view;
        }

        /** {@inheritDoc} */
        @TargetApi(21)
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            // Save the movie id
            viewHolder.dataHolder.setId(cursor.getInt(ArtistListQuery.ARTISTID));
            viewHolder.dataHolder.setTitle(cursor.getString(ArtistListQuery.ARTIST));
            viewHolder.dataHolder.setUndertitle(cursor.getString(ArtistListQuery.GENRE));
            viewHolder.dataHolder.setDescription(cursor.getString(ArtistListQuery.DESCRIPTION));
            viewHolder.dataHolder.setFanArtUrl(cursor.getString(ArtistListQuery.FANART));

            viewHolder.nameView.setText(cursor.getString(ArtistListQuery.ARTIST));
            viewHolder.genresView.setText(cursor.getString(ArtistListQuery.GENRE));
            viewHolder.dataHolder.setPosterUrl(cursor.getString(ArtistListQuery.THUMBNAIL));

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    viewHolder.dataHolder.getPosterUrl(), viewHolder.dataHolder.getTitle(),
                    viewHolder.artView, artWidth, artHeight);

            viewHolder.contextMenu.setTag(viewHolder);
            viewHolder.contextMenu.setOnClickListener(artistlistItemMenuClickListener);

            if (Utils.isLollipopOrLater()) {
                viewHolder.artView.setTransitionName("ar"+viewHolder.dataHolder.getId());
            }
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        TextView nameView;
        TextView genresView;
        ImageView artView;
        ImageView contextMenu;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);
    }

    private View.OnClickListener artistlistItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final ViewHolder viewHolder = (ViewHolder)v.getTag();

            final PlaylistType.Item playListItem = new PlaylistType.Item();
            playListItem.artistid = viewHolder.dataHolder.getId();

            final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_play:
                            MediaPlayerUtils.play(ArtistListFragment.this, playListItem);
                            return true;
                        case R.id.action_queue:
                            MediaPlayerUtils.queue(ArtistListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };
}
