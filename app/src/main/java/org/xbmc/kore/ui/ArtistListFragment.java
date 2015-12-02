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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

/**
 * Fragment that presents the artists list
 */
public class ArtistListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(ArtistListFragment.class);

    public interface OnArtistSelectedListener {
        public void onArtistSelected(int artistId, String artistName);
    }

    // Activity listener
    private OnArtistSelectedListener listenerActivity;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    @Override
    protected AdapterView.OnItemClickListener createOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the artist id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onArtistSelected(tag.artistId, tag.artistName);
            }
        };
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
        inflater.inflate(R.menu.media_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search_artists));
        super.onCreateOptionsMenu(menu, inflater);
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
                MediaContract.Movies.THUMBNAIL,
        };

        String SORT = MediaDatabase.sortCommonTokens(MediaContract.Artists.ARTIST) + " ASC";

        final int ID = 0;
        final int ARTISTID = 1;
        final int ARTIST = 2;
        final int GENRE = 3;
        final int THUMBNAIL = 4;
    }

    private class ArtistsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public ArtistsAdapter(Context context) {
            super(context, null, false);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.artistlist_art_width) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.artistlist_art_heigth) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
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

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            // Save the movie id
            viewHolder.artistId = cursor.getInt(ArtistListQuery.ARTISTID);
            viewHolder.artistName = cursor.getString(ArtistListQuery.ARTIST);

            viewHolder.nameView.setText(viewHolder.artistName);
            viewHolder.genresView.setText(cursor.getString(ArtistListQuery.GENRE));

            String thumbnail = cursor.getString(ArtistListQuery.THUMBNAIL);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    thumbnail, viewHolder.artistName,
                    viewHolder.artView, artWidth, artHeight);

            // For the popupmenu
            ImageView contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
            contextMenu.setTag(viewHolder);
            contextMenu.setOnClickListener(artistlistItemMenuClickListener);
        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder {
        TextView nameView;
        TextView genresView;
        ImageView artView;

        int artistId;
        String artistName;
    }

    private View.OnClickListener artistlistItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final ViewHolder viewHolder = (ViewHolder)v.getTag();

            final PlaylistType.Item playListItem = new PlaylistType.Item();
            playListItem.artistid = viewHolder.artistId;

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
                            MediaPlayerUtils.queueAudio(ArtistListFragment.this, playListItem);
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };
}
