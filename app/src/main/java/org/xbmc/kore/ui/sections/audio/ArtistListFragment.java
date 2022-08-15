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
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

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

/**
 * Fragment that presents the artists list
 */
public class ArtistListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(ArtistListFragment.class);

    public interface OnArtistSelectedListener {
        void onArtistSelected(AbstractInfoFragment.DataHolder dataHolder, ImageView sharedImageView);
    }

    // Activity listener
    private OnArtistSelectedListener listenerActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listenerActivity = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnArtistSelectedListener");
        }
        setSupportsSearch(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    @Override
    protected void onListItemClicked(View view) {
        // Get the artist id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onArtistSelected(tag.dataHolder, tag.art);
    }

    @Override
    protected RecyclerViewCursorAdapter createCursorAdapter() {
        return new ArtistsAdapter(this);
    }

    @Override
    protected CursorLoader createCursorLoader() {
        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        Uri uri = MediaContract.Artists.buildArtistsListUri(hostInfo != null ? hostInfo.getId() : -1);

        String selection = null;
        String[] selectionArgs = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaContract.ArtistsColumns.ARTIST + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(requireContext(), uri,
                ArtistListQuery.PROJECTION, selection, selectionArgs, ArtistListQuery.SORT);
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

        private final HostManager hostManager;
        private final int artWidth, artHeight;
        Fragment fragment;

        public ArtistsAdapter(Fragment fragment) {
            this.fragment = fragment;
            this.hostManager = HostManager.getInstance(fragment.requireContext());

            // Get the art dimensions
            Resources resources = fragment.requireContext().getResources();
            artWidth = (int)(resources.getDimension(R.dimen.info_poster_width_square));
            artHeight = (int)(resources.getDimension(R.dimen.info_poster_height_square));
        }

        @NonNull
        @Override
        public CursorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(fragment.getContext())
                    .inflate(R.layout.item_music_generic, parent, false);

            return new ViewHolder(view, fragment.getContext(), hostManager, artWidth, artHeight, artistlistItemMenuClickListener);
        }

        private final View.OnClickListener artistlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ViewHolder viewHolder = (ViewHolder)v.getTag();

                final PlaylistType.Item playListItem = new PlaylistType.Item();
                playListItem.artistid = viewHolder.dataHolder.getId();

                final PopupMenu popupMenu = new PopupMenu(fragment.getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_play) {
                        MediaPlayerUtils.play(fragment, playListItem);
                        return true;
                    } else if (itemId == R.id.action_queue) {
                        MediaPlayerUtils.queue(fragment, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                        return true;
                    }
                    return false;
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
        TextView title;
        TextView details;
        TextView otherInfo;
        ImageView art;
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
            title = itemView.findViewById(R.id.title);
            details = itemView.findViewById(R.id.details);
            otherInfo = itemView.findViewById(R.id.other_info);
            art = itemView.findViewById(R.id.art);

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

            title.setText(cursor.getString(ArtistListQuery.ARTIST));
            details.setText(cursor.getString(ArtistListQuery.GENRE));
            otherInfo.setVisibility(View.GONE);
            dataHolder.setPosterUrl(cursor.getString(ArtistListQuery.THUMBNAIL));

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 dataHolder.getPosterUrl(), dataHolder.getTitle(),
                                                 art, artWidth, artHeight);

            art.setTransitionName("ar" + dataHolder.getId());
        }
    }
}
