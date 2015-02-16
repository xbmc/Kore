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
package com.syncedsynapse.kore2.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostConnectionObserver;
import com.syncedsynapse.kore2.host.HostConnectionObserver.PlayerEventsObserver;
import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.ApiCallback;
import com.syncedsynapse.kore2.jsonrpc.ApiMethod;
import com.syncedsynapse.kore2.jsonrpc.method.Player;
import com.syncedsynapse.kore2.jsonrpc.method.Playlist;
import com.syncedsynapse.kore2.jsonrpc.type.ListType;
import com.syncedsynapse.kore2.jsonrpc.type.PlayerType;
import com.syncedsynapse.kore2.jsonrpc.type.PlaylistType;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.UIUtils;
import com.syncedsynapse.kore2.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Playlist view
 */
public class PlaylistFragment extends Fragment
        implements PlayerEventsObserver {
    private static final String TAG = LogUtils.makeLogTag(PlaylistFragment.class);

    /**
     * Host manager from which to get info about the current XBMC
     */
    private HostManager hostManager;

    /**
     * Activity to communicate potential actions that change what's playing
     */
    private HostConnectionObserver hostConnectionObserver;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    /**
     * The current active player id
     */
    private int currentActivePlayerId = -1;

    /**
     * Current playlist
     */
    private int currentPlaylistId = -1;

    /**
     * Playlist adapter
     */
    private PlayListAdapter playListAdapter;

    /**
     * Injectable views
     */
    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.playlist) GridView playlistGridView;

    @InjectView(R.id.info_title) TextView infoTitle;
    @InjectView(R.id.info_message) TextView infoMessage;

//    @InjectView(R.id.play) ImageButton playButton;
//    @InjectView(R.id.stop) ImageButton stopButton;
//    @InjectView(R.id.previous) ImageButton previousButton;
//    @InjectView(R.id.next) ImageButton nextButton;
//    @InjectView(R.id.rewind) ImageButton rewindButton;
//    @InjectView(R.id.fast_forward) ImageButton fastForwardButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, root);

        playListAdapter = new PlayListAdapter();
        playlistGridView.setAdapter(playListAdapter);

        // When clicking on an item, play it
        playlistGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Player.Open action = new Player.Open(currentPlaylistId, position);
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
            }
        });

//        // Pad main content view to overlap bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), playlistGridView, false, false, true);
//        playlistGridView.setClipToPadding(false);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have options
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        hostConnectionObserver.registerPlayerObserver(this, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        hostConnectionObserver.unregisterPlayerObserver(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.playlist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_playlist:
                Playlist.Clear action = new Playlist.Clear(currentPlaylistId);
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                // If we are playing something, refresh playlist
                forceRefreshPlaylist();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        // Add the options
//        menu.add(0, CONTEXT_MENU_REMOVE_ITEM, 1, R.string.remove);
//    }
//
//    @Override
//    public boolean onContextItemSelected(android.view.MenuItem item) {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//
//        switch (item.getItemId()) {
//            case CONTEXT_MENU_REMOVE_ITEM:
//                // Remove this item from the playlist
//                Playlist.Remove action = new Playlist.Remove(currentPlaylistId, info.position);
//                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//                forceRefreshPlaylist();
//                return true;
//        }
//        return super.onContextItemSelected(item);
//    }

    private void forceRefreshPlaylist() {
        // If we are playing something, refresh playlist
        if ((lastCallResult == PLAYER_IS_PLAYING) || (lastCallResult == PLAYER_IS_PAUSED)) {
            setupPlaylistInfo(lastGetActivePlayerResult, lastGetItemResult);
        }
    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

//    /**
//     * Callback for methods that change the play speed
//     */
//    private ApiCallback<Integer> defaultPlaySpeedChangedCallback = new ApiCallback<Integer>() {
//        @Override
//        public void onSucess(Integer result) {
//            UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, result);
//        }
//
//        @Override
//        public void onError(int errorCode, String description) { }
//    };
//
//    /**
//     * Callbacks for bottom button bar
//     */
//    @OnClick(R.id.play)
//    public void onPlayClicked(View v) {
//        Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
//        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.stop)
//    public void onStopClicked(View v) {
//        Player.Stop action = new Player.Stop(currentActivePlayerId);
//        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, 0);
//    }
//
//    @OnClick(R.id.fast_forward)
//    public void onFastForwardClicked(View v) {
//        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.INCREMENT);
//        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.rewind)
//    public void onRewindClicked(View v) {
//        Player.SetSpeed action = new Player.SetSpeed(currentActivePlayerId, GlobalType.IncrementDecrement.DECREMENT);
//        action.execute(hostManager.getConnection(), defaultPlaySpeedChangedCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.previous)
//    public void onPreviousClicked(View v) {
//        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.PREVIOUS);
//        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//    }
//
//    @OnClick(R.id.next)
//    public void onNextClicked(View v) {
//        Player.GoTo action = new Player.GoTo(currentActivePlayerId, Player.GoTo.NEXT);
//        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
//    }

    /**
     * Last call results
     */
    private int lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    private ListType.ItemsAll lastGetItemResult = null;
    private PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult;
    private List<ListType.ItemsAll> lastGetPlaylistItemsResult = null;

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        if ((lastGetPlaylistItemsResult == null) ||
                (lastCallResult != PlayerEventsObserver.PLAYER_IS_PLAYING) ||
                (currentActivePlayerId != getActivePlayerResult.playerid) ||
                (lastGetItemResult.id != getItemResult.id)) {
            // Check if something is different, and only if so, start the chain calls
            setupPlaylistInfo(getActivePlayerResult, getItemResult);
            currentActivePlayerId = getActivePlayerResult.playerid;
        } else {
            // Hopefully nothing changed, so just use the last results
            displayPlaylist(getItemResult, lastGetPlaylistItemsResult);
        }
        // Switch icon
//        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);

        // Save results
        lastCallResult = PLAYER_IS_PLAYING;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetItemResult = getItemResult;
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        if ((lastGetPlaylistItemsResult == null) ||
                (lastCallResult != PlayerEventsObserver.PLAYER_IS_PLAYING) ||
                (currentActivePlayerId != getActivePlayerResult.playerid) ||
                (lastGetItemResult.id != getItemResult.id)) {
            setupPlaylistInfo(getActivePlayerResult, getItemResult);
            currentActivePlayerId = getActivePlayerResult.playerid;
        } else {
            // Hopefully nothing changed, so just use the last results
            displayPlaylist(getItemResult, lastGetPlaylistItemsResult);
        }
        // Switch icon
//        UIUtils.setPlayPauseButtonIcon(getActivity(), playButton, getPropertiesResult.speed);

        lastCallResult = PLAYER_IS_PAUSED;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetItemResult = getItemResult;
    }

    public void playerOnStop() {
        HostInfo hostInfo = hostManager.getHostInfo();
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.nothing_playing);
        infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));

        lastCallResult = PLAYER_IS_STOPPED;
    }

    public void playerOnConnectionError(int errorCode, String description) {
        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel);
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
            // TODO: check error code
            infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
            infoMessage.setText(null);
        }

        lastCallResult = PlayerEventsObserver.PLAYER_CONNECTION_ERROR;
    }

    public void playerNoResultsYet() {
        // Initialize info panel
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            infoTitle.setText(R.string.connecting);
        } else {
            infoTitle.setText(R.string.no_xbmc_configured);
        }
        infoMessage.setText(null);
        lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    }

    public void systemOnQuit() {
        playerNoResultsYet();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {}
    public void observerOnStopObserving() {}

    /**
     * Starts the call chain to display the playlist
     * @param getActivePlayerResult Return from method {@link com.syncedsynapse.kore2.jsonrpc.method.Player.GetActivePlayers}
     */
    private void setupPlaylistInfo(final PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   final ListType.ItemsAll getItemResult) {

        // Call GetPlaylists followed by GetItems
        Playlist.GetPlaylists getPlaylists = new Playlist.GetPlaylists();
        getPlaylists.execute(hostManager.getConnection(), new ApiCallback<ArrayList<PlaylistType.GetPlaylistsReturnType>>() {
            @Override
            public void onSucess(ArrayList<PlaylistType.GetPlaylistsReturnType> result) {
                if (!isAdded()) return;
                getPlaylistItems(getActivePlayerResult, getItemResult, result);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                // Oops
                displayErrorGettingPlaylistMessage(description);
            }
        }, callbackHandler);
    }

    private void getPlaylistItems(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                  final ListType.ItemsAll getItemResult,
                                  final ArrayList<PlaylistType.GetPlaylistsReturnType> getPlaylistsResult) {
        // Get the playlist id for the type of media thats playing
        int playlistId = -1;
        for (int i = 0; i < getPlaylistsResult.size(); i++) {
            if (getPlaylistsResult.get(i).type.equals(getActivePlayersResult.type)) {
                playlistId = getPlaylistsResult.get(i).playlistid;
                break;
            }
        }
        currentPlaylistId = playlistId;

        if (playlistId == -1) {
            // Couldn't find a playlist of the same type, just report empty
            displayEmptyPlaylistMessage();
        } else {
            // Call GetItems
            String[] propertiesToGet = new String[] {
                    ListType.FieldsAll.ART,
                    ListType.FieldsAll.ARTIST,
                    ListType.FieldsAll.ALBUMARTIST,
                    ListType.FieldsAll.ALBUM,
                    ListType.FieldsAll.DISPLAYARTIST,
                    ListType.FieldsAll.EPISODE,
                    ListType.FieldsAll.FANART,
                    ListType.FieldsAll.FILE,
                    ListType.FieldsAll.SEASON,
                    ListType.FieldsAll.SHOWTITLE,
                    ListType.FieldsAll.STUDIO,
                    ListType.FieldsAll.TAGLINE,
                    ListType.FieldsAll.THUMBNAIL,
                    ListType.FieldsAll.TITLE,
                    ListType.FieldsAll.TRACK,
                    ListType.FieldsAll.DURATION,
                    ListType.FieldsAll.RUNTIME,
            };
            Playlist.GetItems getItems = new Playlist.GetItems(playlistId, propertiesToGet);
            getItems.execute(hostManager.getConnection(), new ApiCallback<List<ListType.ItemsAll>>() {
                @Override
                public void onSucess(List<ListType.ItemsAll> result) {
                    if (!isAdded()) return;
                    // Ok, we've got all the info, save and display playlist
                    lastGetPlaylistItemsResult = result;
                    displayPlaylist(getItemResult, result);
                }

                @Override
                public void onError(int errorCode, String description) {
                    if (!isAdded()) return;
                    // Oops
                    displayErrorGettingPlaylistMessage(description);
                }
            }, callbackHandler);
        }
    }

    private void displayPlaylist(final ListType.ItemsAll getItemResult,
                                 final List<ListType.ItemsAll> playlistItems) {
        if (playlistItems.size() == 0) {
            displayEmptyPlaylistMessage();
            return;
        }
        switchToPanel(R.id.playlist);

        // Set items, which call notifyDataSetChanged
        playListAdapter.setPlaylistItems(playlistItems);
        // Present the checked item
        for (int i = 0; i < playlistItems.size(); i++) {
            if ((playlistItems.get(i).id == getItemResult.id) &&
                (playlistItems.get(i).type.equals(getItemResult.type))) {
                playlistGridView.setItemChecked(i, true);
            }
        }
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        switch (panelResId) {
            case R.id.info_panel:
                infoPanel.setVisibility(View.VISIBLE);
                playlistGridView.setVisibility(View.GONE);
                break;
            case R.id.playlist:
                infoPanel.setVisibility(View.GONE);
                playlistGridView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Displays an error on the info panel
     * @param details
     */
    private void displayErrorGettingPlaylistMessage(String details) {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.error_getting_playlist);
        infoMessage.setText(String.format(getString(R.string.error_message), details));
    }

    /**
     * Displays empty playlist
     */
    private void displayEmptyPlaylistMessage() {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.playlist_empty);
        infoMessage.setText(null);
    }

    /**
     * Adapter used to show the hosts in the ListView
     */
    private class PlayListAdapter extends BaseAdapter
            implements ListAdapter {
        private View.OnClickListener playlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();

                final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.playlist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_remove_playlist_item:
                                // Remove this item from the playlist
                                Playlist.Remove action = new Playlist.Remove(currentPlaylistId, position);
                                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                                forceRefreshPlaylist();
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        };

        /**
         * The playlist items
         */
        List<ListType.ItemsAll> playlistItems;

        public PlayListAdapter(List<ListType.ItemsAll> playlistItems) {
            super();
            this.playlistItems = playlistItems;
        }

        public PlayListAdapter() {
            super();
            this.playlistItems = null;
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param playlistItems
         */
        public void setPlaylistItems(List<ListType.ItemsAll> playlistItems) {
            this.playlistItems = playlistItems;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (playlistItems == null) {
                return 0;
            } else {
                return playlistItems.size();
            }
        }

        @Override
        public ListType.ItemsAll getItem(int position) {
            if (playlistItems == null) {
                return null;
            } else {
                return playlistItems.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount () {
            return 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.grid_item_playlist, parent, false);
                // ViewHolder pattern
                viewHolder = new ViewHolder();
                viewHolder.art = (ImageView)convertView.findViewById(R.id.art);
                viewHolder.title = (TextView)convertView.findViewById(R.id.title);
                viewHolder.details = (TextView)convertView.findViewById(R.id.details);
                viewHolder.contextMenu = (ImageView)convertView.findViewById(R.id.list_context_menu);
                viewHolder.duration = (TextView)convertView.findViewById(R.id.duration);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final ListType.ItemsAll item = this.getItem(position);

            // Differentiate between media
            String title, details, artUrl;
            int duration;
            if (item.type.equals(ListType.ItemsAll.TYPE_MOVIE)) {
                title = item.title;
                details = item.tagline;
                artUrl = item.thumbnail;
                duration = item.runtime;
            } else if (item.type.equals(ListType.ItemsAll.TYPE_EPISODE)) {
                title = item.title;
                String season = String.format(getString(R.string.season_episode_abbrev), item.season, item.episode);
                details = String.format("%s | %s", item.showtitle, season);
                artUrl = item.art.poster;
                duration = item.runtime;
            } else if (item.type.equals(ListType.ItemsAll.TYPE_SONG)) {
                title = item.title;
                details = item.displayartist + " | " + item.album;
                artUrl = item.thumbnail;
                duration = item.duration;
            } else if (item.type.equals(ListType.ItemsAll.TYPE_MUSIC_VIDEO)) {
                title = item.title;
                details = Utils.listStringConcat(item.artist, ", ") + " | " + item.album;
                artUrl = item.thumbnail;
                duration = item.runtime;
            } else {
                // Don't yet recognize this type
                title = item.label;
                details = item.type;
                artUrl = item.thumbnail;
                duration = item.runtime;
            }

            viewHolder.title.setText(title);
            viewHolder.details.setText(details);
            viewHolder.duration.setText((duration > 0) ? UIUtils.formatTime(duration) : "");
            viewHolder.position = position;

            int artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
            int artHeigth = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                    artUrl, title,
                    viewHolder.art, artWidth, artHeigth);

            // For the popupmenu
            viewHolder.contextMenu.setTag(position);
            viewHolder.contextMenu.setOnClickListener(playlistItemMenuClickListener);

            return convertView;
        }

        private class ViewHolder {
            ImageView art;
            TextView title;
            TextView details;
            ImageView contextMenu;
            TextView duration;
            int position;
        }
    }

}
