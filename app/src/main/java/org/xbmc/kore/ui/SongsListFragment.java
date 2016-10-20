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
import android.os.Bundle;
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
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

/**
 * Fragment that presents the songs list
 */
public class SongsListFragment extends AbstractCursorListFragment {
    private static final String TAG = LogUtils.makeLogTag(SongsListFragment.class);

    public static final String BUNDLE_KEY_ARTISTID = "artistid";

    private int artistId = -1;

    @Override
    protected String getListSyncType() { return LibrarySyncService.SYNC_ALL_MUSIC; }

    @Override
    protected CursorAdapter createAdapter() {
        return new SongsAdapter(getActivity());
    }

    @Override
    protected void onListItemClicked(View view) {
        ImageView contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
        showPopupMenu(contextMenu);
    }

    @Override
    protected CursorLoader createCursorLoader() {
        Uri uri;
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        int hostId = hostInfo != null ? hostInfo.getId() : -1;

        if (artistId != -1) {
            uri = MediaContract.Songs.buildArtistSongsListUri(hostId, artistId);
        } else {
            uri = MediaContract.Songs.buildSongsListUri(hostId);
        }

        String selection = null;
        String selectionArgs[] = null;
        String searchFilter = getSearchFilter();
        if (!TextUtils.isEmpty(searchFilter)) {
            selection = MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.TITLE + " LIKE ?";
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        return new CursorLoader(getActivity(), uri,
                                SongsListQuery.PROJECTION, selection, selectionArgs, SongsListQuery.SORT);
    }

    @Override
    public void onAttach(Activity activity) {
        setSupportsSearch(true);
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            artistId = getArguments().getInt(BUNDLE_KEY_ARTISTID, -1);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
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
        searchView.setQueryHint(getString(R.string.action_search_albums));
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Album songs list query parameters.
     */
    public interface SongsListQuery {
        String[] PROJECTION = {
                MediaProvider.Qualified.SONGS_ID,
                MediaProvider.Qualified.SONGS_TITLE,
                MediaProvider.Qualified.SONGS_TRACK,
                MediaProvider.Qualified.SONGS_DURATION,
                MediaProvider.Qualified.SONGS_FILE,
                MediaProvider.Qualified.SONGS_SONGID,
                MediaProvider.Qualified.SONGS_DISPLAYARTIST,
                MediaProvider.Qualified.ALBUMS_TITLE,
                MediaProvider.Qualified.ALBUMS_GENRE,
                MediaProvider.Qualified.ALBUMS_YEAR,
                MediaProvider.Qualified.ALBUMS_THUMBNAIL
                };

        String SORT = MediaDatabase.sortCommonTokens(MediaProvider.Qualified.SONGS_TITLE) + " ASC";

        int ID = 0;
        int TITLE = 1;
        int TRACK = 2;
        int DURATION = 3;
        int FILE = 4;
        int SONGID = 5;
        int SONGARTIST = 6;
        int ALBUMTITLE = 7;
        int GENRE = 8;
        int YEAR = 9;
        int THUMBNAIL = 10;
    }

    private class SongsAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public SongsAdapter(Context context) {
            super(context, null, false);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = (int) resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_width);
            artHeight = (int) resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_heigth);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                                            .inflate(R.layout.grid_item_song, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.title = (TextView)view.findViewById(R.id.title);
            viewHolder.details = (TextView)view.findViewById(R.id.details);
            viewHolder.art = (ImageView)view.findViewById(R.id.art);
            viewHolder.artist = (TextView)view.findViewById(R.id.artist);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @TargetApi(21)
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            String title = cursor.getString(SongsListQuery.TITLE);
            viewHolder.songId = cursor.getInt(SongsListQuery.SONGID);

            viewHolder.title.setText(title);

            String artist = cursor.getString(SongsListQuery.SONGARTIST);
            viewHolder.artist.setText(artist);

            int year = cursor.getInt(SongsListQuery.YEAR);
            if (year > 0) {
                setDetails(viewHolder.details,
                           cursor.getString(SongsListQuery.ALBUMTITLE),
                           String.valueOf(year),
                           cursor.getString(SongsListQuery.GENRE));
            } else {
                setDetails(viewHolder.details,
                           cursor.getString(SongsListQuery.ALBUMTITLE),
                           cursor.getString(SongsListQuery.GENRE));
            }

            String thumbnail = cursor.getString(SongsListQuery.THUMBNAIL);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 thumbnail, title,
                                                 viewHolder.art, artWidth, artHeight);

            // For the popupmenu
            ImageView contextMenu = (ImageView)view.findViewById(R.id.list_context_menu);
            contextMenu.setTag(viewHolder);
            contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    showPopupMenu(v);
                }
            });
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        ImageView art;
        TextView title;
        TextView details;
        TextView artist;

        int songId;
    }

    private void showPopupMenu(View v) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.songid = viewHolder.songId;

        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.musiclist_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_play:
                        MediaPlayerUtils.play(SongsListFragment.this, playListItem);
                        return true;
                    case R.id.action_queue:
                        MediaPlayerUtils.queue(SongsListFragment.this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
                        return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void setDetails(TextView textView, String... elements) {
        if ((elements == null) || (elements.length < 1)) {
            return;
        }

        ArrayList<String> details = new ArrayList<>();
        for (int i = 0; i < elements.length; i++) {
            if (!TextUtils.isEmpty(elements[i]))
                details.add(elements[i]);
        }

        textView.setText(TextUtils.join(" | ", details.toArray()));
    }
}
