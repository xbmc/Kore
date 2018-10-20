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
package org.xbmc.kore.ui.sections.remote;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostConnectionObserver.PlayerEventsObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.viewgroups.DynamicListView;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.Unbinder;

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

    private Unbinder unbinder;

    /**
     * Injectable views
     */
    @BindView(R.id.info_panel) RelativeLayout infoPanel;
    @BindView(R.id.playlist) DynamicListView playlistListView;

    @BindView(R.id.info_title) TextView infoTitle;
    @BindView(R.id.info_message) TextView infoMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(getActivity());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        unbinder = ButterKnife.bind(this, root);

        playListAdapter = new PlayListAdapter();
        playlistListView.setAdapter(playListAdapter);

        // When clicking on an item, play it
        playlistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Player.Open action = new Player.Open(Player.Open.TYPE_PLAYLIST, currentPlaylistId, position);
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
            }
        });

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
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
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

    public void forceRefreshPlaylist() {
        // If we are playing something, refresh playlist
        if ((lastCallResult == PLAYER_IS_PLAYING) || (lastCallResult == PLAYER_IS_PAUSED)) {
            setupPlaylistInfo(lastGetActivePlayerResult, lastGetPropertiesResult, lastGetItemResult);
        }
    }

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

    /**
     * Last call results
     */
    private int lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    private ListType.ItemsAll lastGetItemResult = null;
    private PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult;
    private PlayerType.PropertyValue lastGetPropertiesResult;
    private List<ListType.ItemsAll> lastGetPlaylistItemsResult = null;

    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {
        if (notificationsData.property.shuffled != null)
            setupPlaylistInfo(lastGetActivePlayerResult, lastGetPropertiesResult, lastGetItemResult);
    }

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
            setupPlaylistInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
            currentActivePlayerId = getActivePlayerResult.playerid;
        } else {
            // Hopefully nothing changed, so just use the last results
            displayPlaylist(getItemResult, lastGetPlaylistItemsResult);
        }

        // Save results
        lastCallResult = PLAYER_IS_PLAYING;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetPropertiesResult = getPropertiesResult;
        lastGetItemResult = getItemResult;
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        if ((lastGetPlaylistItemsResult == null) ||
            (lastCallResult != PlayerEventsObserver.PLAYER_IS_PLAYING) ||
            (currentActivePlayerId != getActivePlayerResult.playerid) ||
            (lastGetItemResult.id != getItemResult.id)) {
            setupPlaylistInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
            currentActivePlayerId = getActivePlayerResult.playerid;
        } else {
            // Hopefully nothing changed, so just use the last results
            displayPlaylist(getItemResult, lastGetPlaylistItemsResult);
        }

        lastCallResult = PLAYER_IS_PAUSED;
        lastGetActivePlayerResult = getActivePlayerResult;
        lastGetPropertiesResult = getPropertiesResult;
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
     */
    private void setupPlaylistInfo(final PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   final PlayerType.PropertyValue getPropertiesResult,
                                   final ListType.ItemsAll getItemResult) {

        currentPlaylistId = getPropertiesResult.playlistid;

        if (currentPlaylistId == -1) {
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
            Playlist.GetItems getItems = new Playlist.GetItems(currentPlaylistId, propertiesToGet);
            getItems.execute(hostManager.getConnection(), new ApiCallback<List<ListType.ItemsAll>>() {
                @Override
                public void onSuccess(List<ListType.ItemsAll> result) {
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
        if (playlistItems.isEmpty()) {
            displayEmptyPlaylistMessage();
            return;
        }
        switchToPanel(R.id.playlist);

        //If a user is dragging a list item we must not modify the adapter to prevent
        //the dragged item's adapter position from diverging from its listview position
        if (!playlistListView.isItemBeingDragged()) {
            // Set items, which call notifyDataSetChanged
            playListAdapter.setPlaylistItems(playlistItems);
            highlightItem(getItemResult, playlistItems);
        } else {
            highlightItem(getItemResult, playListAdapter.playlistItems);
        }
    }

    private void highlightItem(final ListType.ItemsAll item,
                               final List<ListType.ItemsAll> playlistItems) {
        for (int i = 0; i < playlistItems.size(); i++) {
            if ((playlistItems.get(i).id == item.id) &&
                (playlistItems.get(i).type.equals(item.type))) {

                //When user is dragging an item it is very annoying when we change the list position
                if (!playlistListView.isItemBeingDragged()) {
                    playlistListView.setSelection(i);
                }
                playlistListView.setItemChecked(i, true);

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
                playlistListView.setVisibility(View.GONE);
                break;
            case R.id.playlist:
                infoPanel.setVisibility(View.GONE);
                playlistListView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Displays an error on the info panel
     * @param details Details message
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
            implements DynamicListView.DynamicListAdapter {
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
        int artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
        int artHeight = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);

        int cardBackgroundColor, selectedCardBackgroundColor;

        public PlayListAdapter(List<ListType.ItemsAll> playlistItems) {
            super();
            this.playlistItems = playlistItems;

            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.appCardBackgroundColor,
                    R.attr.appSelectedCardBackgroundColor});
            Resources resources = getResources();
            cardBackgroundColor =
                    styledAttributes.getColor(styledAttributes.getIndex(0), resources.getColor(R.color.dark_content_background));
            selectedCardBackgroundColor =
                    styledAttributes.getColor(styledAttributes.getIndex(1), resources.getColor(R.color.dark_selected_content_background));
            styledAttributes.recycle();
        }

        public PlayListAdapter() {
            this(null);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param playlistItems Items
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
            if (position < 0 || position >= playlistItems.size()) {
                return -1;
            }
            return playlistItems.get(position).id;
        }

        @Override
        public int getViewTypeCount () {
            return 1;
        }

        @Override
        public void onSwapItems(int positionOne, int positionTwo) {
            ListType.ItemsAll tmp = playlistItems.get(positionOne);
            playlistItems.set(positionOne, playlistItems.get(positionTwo));
            playlistItems.set(positionTwo, tmp);
        }

        @Override
        public void onSwapFinished(final int originalPosition, final int finalPosition) {
            final HostConnection hostConnection = hostManager.getConnection();

            if (playlistItems.get(finalPosition).id == lastGetItemResult.id) {
                Toast.makeText(getActivity(), R.string.cannot_move_playing_item, Toast.LENGTH_SHORT)
                     .show();
                rollbackSwappedItems(originalPosition, finalPosition);
                notifyDataSetChanged();
                return;
            }

            Playlist.Remove remove = new Playlist.Remove(currentPlaylistId, originalPosition);
            remove.execute(hostConnection, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Playlist.Insert insert = new Playlist.Insert(currentPlaylistId, finalPosition, createPlaylistTypeItem(playlistItems.get(finalPosition)));
                    insert.execute(hostConnection, new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                        }

                        @Override
                        public void onError(int errorCode, String description) {
                            //Remove succeeded but insert failed, so we need to remove item from playlist at final position
                            playlistItems.remove(finalPosition);
                            notifyDataSetChanged();
                            if (!isAdded()) return;
                            // Got an error, show toast
                            Toast.makeText(getActivity(), R.string.unable_to_move_item, Toast.LENGTH_SHORT)
                                 .show();
                        }
                    }, callbackHandler);
                }

                @Override
                public void onError(int errorCode, String description) {
                    rollbackSwappedItems(originalPosition, finalPosition);
                    notifyDataSetChanged();
                    if (!isAdded()) return;
                    // Got an error, show toast
                    Toast.makeText(getActivity(), R.string.unable_to_move_item, Toast.LENGTH_SHORT)
                         .show();
                }
            }, callbackHandler);
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
                viewHolder.card = (CardView)convertView.findViewById(R.id.card);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ListType.ItemsAll item = this.getItem(position);

            // Differentiate between media
            String title, details, artUrl;
            int duration;
            switch (item.type) {
                case ListType.ItemsAll.TYPE_MOVIE:
                    title = TextUtils.isEmpty(item.title)? item.label : item.title;
                    details = item.tagline;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
                case ListType.ItemsAll.TYPE_EPISODE:
                    title = TextUtils.isEmpty(item.title)? item.label : item.title;
                    String season = String.format(getString(R.string.season_episode_abbrev), item.season, item.episode);
                    details = String.format("%s | %s", item.showtitle, season);
                    artUrl = item.art.poster;
                    duration = item.runtime;
                    break;
                case ListType.ItemsAll.TYPE_SONG:
                    title = TextUtils.isEmpty(item.title)? item.label : item.title;
                    details = item.displayartist + " | " + item.album;
                    artUrl = item.thumbnail;
                    duration = item.duration;
                    break;
                case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                    title = TextUtils.isEmpty(item.title)? item.label : item.title;
                    details = Utils.listStringConcat(item.artist, ", ") + " | " + item.album;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
                default:
                    // Don't yet recognize this type
                    title = TextUtils.isEmpty(item.label)? item.file : item.label;
                    details = item.type;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
            }

            viewHolder.title.setText(title);
            viewHolder.details.setText(details);
            viewHolder.duration.setText((duration > 0) ? UIUtils.formatTime(duration) : "");
            viewHolder.position = position;

            int cardColor = (position == playlistListView.getCheckedItemPosition()) ?
                            selectedCardBackgroundColor: cardBackgroundColor;
            viewHolder.card.setCardBackgroundColor(cardColor);

            // If not video, change aspect ration of poster to a square
            boolean isVideo = (item.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
                              (item.type.equals(ListType.ItemsAll.TYPE_EPISODE));
            if (!isVideo) {
                ViewGroup.LayoutParams layoutParams = viewHolder.art.getLayoutParams();
                layoutParams.width = layoutParams.height;
                viewHolder.art.setLayoutParams(layoutParams);
                artWidth = artHeight;
            }
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 artUrl, title,
                                                 viewHolder.art, artWidth, artHeight);

            // For the popupmenu
            viewHolder.contextMenu.setTag(position);
            viewHolder.contextMenu.setOnClickListener(playlistItemMenuClickListener);

            return convertView;
        }

        private PlaylistType.Item createPlaylistTypeItem(ListType.ItemsAll item) {
            PlaylistType.Item playlistItem = new PlaylistType.Item();

            switch (item.type) {
                case ListType.ItemsAll.TYPE_MOVIE:
                    playlistItem.movieid = item.id;
                    break;
                case ListType.ItemsAll.TYPE_EPISODE:
                    playlistItem.episodeid = item.id;
                    break;
                case ListType.ItemsAll.TYPE_SONG:
                    playlistItem.songid = item.id;
                    break;
                case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                    playlistItem.musicvideoid = item.id;
                    break;
                default:
                    LogUtils.LOGE(TAG, "createPlaylistTypeItem, failed to create item for "+item.type);
                    break;
            }

            return playlistItem;
        }

        private void rollbackSwappedItems(int originalPosition, int newPosition) {
            if (originalPosition > newPosition) {
                for (int i = newPosition; i < originalPosition; i++) {
                    onSwapItems(i, i + 1);
                }
            } else if (originalPosition < newPosition) {
                for (int i = newPosition; i > originalPosition; i--) {
                    onSwapItems(i, i - 1);
                }
            }
        }

        private class ViewHolder {
            ImageView art;
            TextView title;
            TextView details;
            ImageView contextMenu;
            TextView duration;
            CardView card;
            int position;
        }
    }

}
