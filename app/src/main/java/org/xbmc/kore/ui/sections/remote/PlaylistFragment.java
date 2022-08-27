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

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentPlaylistBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostConnectionObserver.PlayerEventsObserver;
import org.xbmc.kore.host.HostConnectionObserver.PlaylistEventsObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.host.actions.GetPlaylist;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.viewgroups.DynamicListView;
import org.xbmc.kore.ui.widgets.PlaylistsBar;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Playlist view
 */
public class PlaylistFragment extends Fragment
        implements PlayerEventsObserver, PlaylistEventsObserver {
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
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    /**
     * Playlist adapter
     */
    private PlayListAdapter playListAdapter;

   /**
     * Last call results
     */
    private ListType.ItemsAll lastGetItemResult = null;
    private PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult;
    private final HashMap<String, PlaylistHolder> playlists = new HashMap<>();

    private enum PLAYER_STATE {
        CONNECTION_ERROR,
        NO_RESULTS_YET,
        PLAYING,
        PAUSED,
        STOPPED
    }

    private PLAYER_STATE playerState;

    private boolean userSelectedTab;

    private FragmentPlaylistBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(requireContext());
        hostConnectionObserver = hostManager.getHostConnectionObserver();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // We have options
        setHasOptionsMenu(true);

        playListAdapter = new PlayListAdapter();
        binding.playlist.setAdapter(playListAdapter);

        // When clicking on an item, play it
        binding.playlist.setOnItemClickListener((parent, v, position, id) -> {
            PlaylistHolder holder = playlists.get(binding.playlistsBar.getSelectedPlaylistType());
            if (holder != null) {
                Player.Open action = new Player.Open(Player.Open.TYPE_PLAYLIST, holder.getPlaylistId(), position);
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
            }
        });

        binding.playlistsBar.setOnPlaylistSelectedListener(new PlaylistsBar.OnPlaylistSelectedListener() {
            @Override
            public void onPlaylistSelected(String playlistType) {
                userSelectedTab = true; // do not switch to active playlist when user selected a tab
                displayPlaylist();
            }

            @Override
            public void onPlaylistDeselected(String playlistType) {
                View v = binding.playlist.getChildAt(0);
                int top = (v == null) ? 0 : (v.getTop() - binding.playlist.getPaddingTop());

                PlaylistHolder playlistHolder = playlists.get(playlistType);
                if (playlistHolder != null)
                    playlistHolder.setListViewPosition(binding.playlist.getFirstVisiblePosition(), top);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        hostConnectionObserver.registerPlayerObserver(this);
        hostConnectionObserver.registerPlaylistObserver(this);
    }

    @Override
    public void onStop() {
        hostConnectionObserver.unregisterPlayerObserver(this);
        hostConnectionObserver.unregisterPlaylistObserver(this);
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.playlist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_playlist) {
            PlaylistHolder playlistHolder = playlists.get(binding.playlistsBar.getSelectedPlaylistType());
            if (playlistHolder != null) {
                onPlaylistClear(playlistHolder.getPlaylistId());
                Playlist.Clear action = new Playlist.Clear(playlistHolder.getPlaylistId());
                action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean refreshingPlaylist;
    private void refreshPlaylist(GetPlaylist getPlaylist) {
        if (refreshingPlaylist)
            return;

        refreshingPlaylist = true;
        getPlaylist.execute(hostManager.getConnection(),
                new ApiCallback<ArrayList<GetPlaylist.GetPlaylistResult>>() {
                    @Override
                    public void onSuccess(ArrayList<GetPlaylist.GetPlaylistResult> result) {
                        refreshingPlaylist = false;

                        if(!isAdded())
                            return;

                        updatePlaylists(result);
                        displayPlaylist();
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        refreshingPlaylist = false;

                        onPlayerConnectionError(errorCode, description);
                    }
                }, callbackHandler);
    }

    /**
     * Default callback for methods that don't return anything
     */
    private final ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

    @Override
    public void onPlayerPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {
        if (notificationsData.property.shuffled != null)
            refreshPlaylist(new GetPlaylist(lastGetActivePlayerResult.type));
    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    @Override
    public void onPlayerPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        playerState = PLAYER_STATE.PLAYING;

        lastGetItemResult = getItemResult;
        lastGetActivePlayerResult = getActivePlayerResult;

        if (!userSelectedTab) {
            binding.playlistsBar.selectTab(getActivePlayerResult.type);
        }

        binding.playlistsBar.setIsPlaying(getActivePlayerResult.type, true);

        displayPlaylist();

        PlaylistHolder playlistHolder = playlists.get(getActivePlayerResult.type);
        if (playlistHolder != null && isPlaying(playlistHolder.getPlaylistResult)) {
            highlightCurrentlyPlayingItem();
        } else {
            binding.playlist.clearChoices();
        }
    }

    @Override
    public void onPlayerPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        playerState = PLAYER_STATE.PAUSED;

        lastGetItemResult = getItemResult;
        lastGetActivePlayerResult = getActivePlayerResult;

        if (!userSelectedTab) {
            binding.playlistsBar.selectTab(getActivePlayerResult.type);
        }

        binding.playlistsBar.setIsPlaying(getActivePlayerResult.type, false);
    }

    @Override
    public void onPlayerStop() {
        playerState = PLAYER_STATE.STOPPED;

        if (lastGetActivePlayerResult != null)
            binding.playlistsBar.setIsPlaying(lastGetActivePlayerResult.type, false);

        displayPlaylist();

        binding.playlist.clearChoices();
    }

    @Override
    public void onPlayerConnectionError(int errorCode, String description) {
        playerState = PLAYER_STATE.CONNECTION_ERROR;

        HostInfo hostInfo = hostManager.getHostInfo();

        switchToPanel(R.id.info_panel);
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.connecting);
            binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
            binding.includeInfoPanel.infoMessage.setText(null);
        }
    }

    @Override
    public void onPlayerNoResultsYet() {
        playerState = PLAYER_STATE.NO_RESULTS_YET;

        // Initialize info panel
        switchToPanel(R.id.info_panel);
        HostInfo hostInfo = hostManager.getHostInfo();
        if (hostInfo != null) {
            binding.includeInfoPanel.infoTitle.setText(R.string.connecting);
        } else {
            binding.includeInfoPanel.infoTitle.setText(R.string.no_xbmc_configured);
        }
        binding.includeInfoPanel.infoMessage.setText(null);
    }

    @Override
    public void onSystemQuit() {
        onPlayerNoResultsYet();
    }

    // Ignore this
    @Override
    public void onInputRequested(String title, String type, String value) {}
    @Override
    public void onObserverStopObserving() {}

    @Override
    public void onPlaylistClear(int playlistId) {
        Iterator<String> it = playlists.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            PlaylistHolder holder = playlists.get(key);
            if (holder != null && holder.getPlaylistResult.id == playlistId) {
                it.remove();
                binding.playlistsBar.setHasPlaylistAvailable(key, false);
                binding.playlistsBar.setIsPlaying(key, false);
            }
        }
        displayPlaylist();
    }

    @Override
    public void onPlaylistsAvailable(ArrayList<GetPlaylist.GetPlaylistResult> playlists) {
        updatePlaylists(playlists);

        if ((playerState == PLAYER_STATE.PLAYING) &&
            (hostManager.getConnection().getProtocol() == HostConnection.PROTOCOL_TCP))
            // if item is currently playing displaying is already handled by onPlayerPlay callback
            return;

        // BUG: When playing movies playlist stops, audio tab gets selected when it contains a playlist.
        // We might want a separate var to check if something has already played and turn off automatic
        // playlist switching if playback stops
        if (playerState == PLAYER_STATE.STOPPED && lastGetActivePlayerResult == null && !userSelectedTab) { // do not automatically switch to first available playlist if user manually selected a playlist
            binding.playlistsBar.selectTab(playlists.get(0).type);
        }
        displayPlaylist();
    }

    @Override
    public void onPlaylistError(int errorCode, String description) {
        onPlayerConnectionError(errorCode, description);
    }

    private void updatePlaylists(ArrayList<GetPlaylist.GetPlaylistResult> playlists) {
        for (GetPlaylist.GetPlaylistResult getPlaylistResult : playlists) {
            binding.playlistsBar.setHasPlaylistAvailable(getPlaylistResult.type, true);

            PlaylistHolder playlistHolder = this.playlists.get(getPlaylistResult.type);

            if (playlistHolder == null) {
                playlistHolder = new PlaylistHolder();
                this.playlists.put(getPlaylistResult.type, playlistHolder);
            }

            playlistHolder.setPlaylist(getPlaylistResult);
        }
    }

    private void displayPlaylist() {
        switchToPanel(R.id.playlist);

        PlaylistHolder playlistHolder = playlists.get(binding.playlistsBar.getSelectedPlaylistType());
        if (playlistHolder == null) {
            displayEmptyPlaylistMessage();
            return;
        }

        GetPlaylist.GetPlaylistResult getPlaylistResult = playlistHolder.getPlaylistResult;
        if (getPlaylistResult == null) {
            displayEmptyPlaylistMessage();
            return;
        }

        // JSON RPC does not support picture items in Playlist.Item so we disable item movement
        // for the picture playlist
        if (getPlaylistResult.type.contentEquals(ListType.ItemBase.TYPE_PICTURE))
            binding.playlist.enableItemDragging(false);
        else
            binding.playlist.enableItemDragging(true);

        //If a user is dragging a list item we must not modify the adapter to prevent
        //the dragged item's adapter position from diverging from its listview position
        if (!binding.playlist.isItemBeingDragged()) {
            playListAdapter.setPlaylistItems(getPlaylistResult.items);
        }

        binding.playlist.setSelectionFromTop(playlistHolder.index, playlistHolder.top);
    }

    private boolean isPlaying(GetPlaylist.GetPlaylistResult getPlaylistResult) {
        return playerState == PLAYER_STATE.PLAYING && lastGetActivePlayerResult != null &&
                getPlaylistResult.id == lastGetActivePlayerResult.playerid;
    }

    private void highlightCurrentlyPlayingItem() {
        PlaylistHolder holder = playlists.get(binding.playlistsBar.getSelectedPlaylistType());
        if (!binding.playlistsBar.getSelectedPlaylistType().contentEquals(lastGetActivePlayerResult.type) ||
            holder == null)
            return;

        List<ListType.ItemsAll> playlistItems = holder.getPlaylistResult.items;
        for (int i = 0; i < playlistItems.size(); i++) {
            if ((playlistItems.get(i).id == lastGetItemResult.id) &&
                (playlistItems.get(i).type.equals(lastGetItemResult.type))) {

                //When user is dragging an item it is very annoying when we change the list position
                if (!binding.playlist.isItemBeingDragged()) {
                    binding.playlist.setSelection(i);
                }

                binding.playlist.setItemChecked(i, true);
            }
        }
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        if (panelResId == R.id.info_panel) {
            binding.includeInfoPanel.infoPanel.setVisibility(View.VISIBLE);
            binding.playlist.setVisibility(View.GONE);
        } else if (panelResId == R.id.playlist) {
            binding.includeInfoPanel.infoPanel.setVisibility(View.GONE);
            binding.playlist.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Displays empty playlist
     */
    private void displayEmptyPlaylistMessage() {
        HostInfo hostInfo = hostManager.getHostInfo();
        switchToPanel(R.id.info_panel);
        binding.includeInfoPanel.infoTitle.setText(R.string.playlist_empty);
        binding.includeInfoPanel.infoMessage.setText(String.format(getString(R.string.connected_to), hostInfo.getName()));
    }

    /**
     * Adapter used to show the hosts in the ListView
     */
    private class PlayListAdapter extends BaseAdapter
            implements DynamicListView.DynamicListAdapter {
        private final View.OnClickListener playlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();

                final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.playlist_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_remove_playlist_item) {
                        // Remove this item from the playlist
                        PlaylistHolder holder = playlists.get(binding.playlistsBar.getSelectedPlaylistType());
                        if (holder != null) {
                            int playlistId = holder.getPlaylistId();
                            Playlist.Remove action = new Playlist.Remove(playlistId, position);
                            action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                            return true;
                        }
                    }
                    return false;
                });
                popupMenu.show();
            }
        };

        /**
         * The playlist items
         */
        List<ListType.ItemsAll> playlistItems;
        int artWidth, artHeight;
        int selectedRowColor;
        ColorStateList selectedRowColorStateList;
        Drawable backgroundDrawable;

        public PlayListAdapter(List<ListType.ItemsAll> playlistItems) {
            super();
            this.playlistItems = playlistItems;

            selectedRowColor = MaterialColors.getColor(requireContext(), R.attr.colorSurfaceVariant, null);
            selectedRowColorStateList = ColorStateList.valueOf(selectedRowColor);
            backgroundDrawable = getResources().getDrawable(R.drawable.background_playlist, null);
            artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
            artHeight = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);
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

            if (lastGetItemResult != null &&
                playlistItems.get(finalPosition).id == lastGetItemResult.id) {
                Toast.makeText(getActivity(), R.string.cannot_move_playing_item, Toast.LENGTH_SHORT)
                     .show();
                rollbackSwappedItems(originalPosition, finalPosition);
                notifyDataSetChanged();
                return;
            }

            PlaylistHolder holder = playlists.get(binding.playlistsBar.getSelectedPlaylistType());
            if (holder == null) return;
            final int playlistId = holder.getPlaylistId();
            Playlist.Remove remove = new Playlist.Remove(playlistId, originalPosition);
            remove.execute(hostConnection, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Playlist.Insert insert = new Playlist.Insert(playlistId, finalPosition, createPlaylistTypeItem(playlistItems.get(finalPosition)));
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
                                            .inflate(R.layout.item_playlist, parent, false);
                // ViewHolder pattern
                viewHolder = new ViewHolder();
                viewHolder.art = convertView.findViewById(R.id.art);
                viewHolder.title = convertView.findViewById(R.id.playlist_item_title);
                viewHolder.details = convertView.findViewById(R.id.details);
                viewHolder.contextMenu = convertView.findViewById(R.id.list_context_menu);
                viewHolder.duration = convertView.findViewById(R.id.duration);
                viewHolder.container = convertView.findViewById(R.id.container);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ListType.ItemsAll item = this.getItem(position);
            if (item == null) return null;

            // Differentiate between media
            String title, details, artUrl;
            int duration;
            switch (item.type) {
                case ListType.ItemsAll.TYPE_MOVIE:
                    title = TextUtils.isEmpty(item.title)? item.label : item.title;
                    details = item.tagline;
                    artUrl = item.art.poster;
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
                case ListType.ItemsAll.TYPE_PICTURE:
                    title = TextUtils.isEmpty(item.label)? item.file : item.label;
                    details = item.type;
                    artUrl = item.thumbnail;
                    duration = 0;
                    break;
                default:
                    // Don't yet recognize this type
                    title = TextUtils.isEmpty(item.label)? item.file : item.label;
                    details = item.type;
                    artUrl = item.thumbnail;
                    duration = item.runtime;
                    break;
            }

            viewHolder.title.setText(UIUtils.applyMarkup(getContext(), title));
            viewHolder.details.setText(details);
            viewHolder.duration.setText((duration > 0) ? UIUtils.formatTime(duration) : "");
            viewHolder.position = position;

            if (position == binding.playlist.getCheckedItemPosition()) {
                viewHolder.container.setBackground(backgroundDrawable);
                viewHolder.container.setBackgroundTintList(selectedRowColorStateList);
            } else {
                viewHolder.container.setBackground(null);
            }

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 artUrl, title,
                                                 viewHolder.art, artWidth, artHeight);

            if (!item.type.contentEquals(ListType.ItemsAll.TYPE_PICTURE)) {
                // For the popupmenu
                viewHolder.contextMenu.setVisibility(View.VISIBLE);
                viewHolder.contextMenu.setTag(position);
                viewHolder.contextMenu.setOnClickListener(playlistItemMenuClickListener);
            } else {
                viewHolder.contextMenu.setVisibility(View.INVISIBLE);
            }

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
            View container;
            int position;
        }
    }

    private static class PlaylistHolder {
        private GetPlaylist.GetPlaylistResult getPlaylistResult;
        private int top;
        private int index;

        private PlaylistHolder() {}

        public void setPlaylist(GetPlaylist.GetPlaylistResult getPlaylistResult) {
            this.getPlaylistResult = getPlaylistResult;
        }

        public GetPlaylist.GetPlaylistResult getPlaylist() {
            return getPlaylistResult;
        }

        void setListViewPosition(int index, int top) {
            this.index = index;
            this.top = top;
        }

        public int getTop() {
            return top;
        }

        public int getIndex() {
            return index;
        }

        public int getPlaylistId() { return getPlaylistResult.id; }
    }
}
