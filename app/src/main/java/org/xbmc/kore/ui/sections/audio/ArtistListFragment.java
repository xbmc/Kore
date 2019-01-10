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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import org.xbmc.kore.ui.RecyclerViewCursorAdapter;
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
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new ArtistsAdapter(this);
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
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listenerActivity = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
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

    private static class ArtistsAdapter extends RecyclerViewCursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;
        Fragment fragment;

        public ArtistsAdapter(Fragment fragment) {
            this.fragment = fragment;
            this.hostManager = HostManager.getInstance(fragment.getContext());

            // Get the art dimensions
            Resources resources = fragment.getContext().getResources();
            artWidth = (int)(resources.getDimension(R.dimen.detail_poster_width_square));
            artHeight = (int)(resources.getDimension(R.dimen.detail_poster_height_square));
        }

        @Override
        public CursorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(fragment.getContext())
                    .inflate(R.layout.grid_item_artist, parent, false);

            return new ViewHolder(view, fragment.getContext(), hostManager, artWidth, artHeight, artistlistItemMenuClickListener);
        }

        private View.OnClickListener artistlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ViewHolder viewHolder = (ViewHolder)v.getTag();

                final PlaylistType.Item playListItem = new PlaylistType.Item();
                playListItem.artistid = viewHolder.dataHolder.getId();

                final PopupMenu popupMenu = new PopupMenu(fragment.getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_play:
                                MediaPlayerUtils.play(fragment, playListItem);
                                return true;
                            case R.id.action_queue:
                                MediaPlayerUtils.queue(fragment, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        };

        protected int getSectionColumnIdx() { return ArtistListQuery.ARTIST; }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerViewCursorAdapter.CursorViewHolder {
        TextView nameView;
        TextView genresView;
        ImageView artView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight,
                   View.OnClickListener contextMenuClickListener) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            nameView = itemView.findViewById(R.id.name);
            genresView = itemView.findViewById(R.id.genres);
            artView = itemView.findViewById(R.id.art);

            ImageView contextMenu = itemView.findViewById(R.id.list_context_menu);
            contextMenu.setTag(this);
            contextMenu.setOnClickListener(contextMenuClickListener);
        }

        @Override
        public void bindView(Cursor cursor) {
            dataHolder.setId(cursor.getInt(ArtistListQuery.ARTISTID));
            dataHolder.setTitle(cursor.getString(ArtistListQuery.ARTIST));
            dataHolder.setUndertitle(cursor.getString(ArtistListQuery.GENRE));
            dataHolder.setDescription(cursor.getString(ArtistListQuery.DESCRIPTION));
            dataHolder.setFanArtUrl(cursor.getString(ArtistListQuery.FANART));

            nameView.setText(cursor.getString(ArtistListQuery.ARTIST));
            genresView.setText(cursor.getString(ArtistListQuery.GENRE));
            dataHolder.setPosterUrl(cursor.getString(ArtistListQuery.THUMBNAIL));

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 dataHolder.getPosterUrl(), dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            if (Utils.isLollipopOrLater()) {
                artView.setTransitionName("ar"+dataHolder.getId());
            }
        }
    }
}
