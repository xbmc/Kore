/*
 * Copyright 2018 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.tcpserver.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Playlist;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Playlist.OnAdd;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Playlist.OnClear;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates Playlist JSON-RPC API
 */
public class PlaylistHandler extends ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(PlaylistHandler.class);

    private static final String ID_NODE = "id";
    private static final String PARAMS_NODE = "params";
    private static final String PLAYLISTID_NODE = "playlistid";

    private ArrayList<PlaylistHolder> playlists = new ArrayList<>();

    @Override
    public void reset() {
        playlists.clear();
    }

    @Override
    public String[] getType() {
        return new String[]{Playlist.GetItems.METHOD_NAME, Playlist.GetPlaylists.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> createResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();


        int methodId = jsonRequest.get(ID_NODE).asInt(-1);

        switch (method) {
            case Playlist.GetItems.METHOD_NAME:
                int playlistId = jsonRequest.get(PARAMS_NODE).get(PLAYLISTID_NODE).asInt(-1);
                jsonResponses.add(createPlaylist(methodId, playlistId));
                break;
            case Playlist.GetPlaylists.METHOD_NAME:
                jsonResponses.add(new Playlist.GetPlaylists(methodId));
                break;
            default:
                LogUtils.LOGD(TAG, "method: " + method + ", not implemented");
        }
        return jsonResponses;
    }

    private Playlist.GetItems createPlaylist(int methodId, int playlistId) {
        Playlist.GetItems playlistGetItems = new Playlist.GetItems(methodId);

        if (playlists.size() > playlistId) {
            for (Player.GetItem getItem : playlists.get(playlistId).getItems()) {
                playlistGetItems.addItem(getItem);
            }
        }

        return playlistGetItems;
    }

    public ArrayList<PlaylistHolder> getPlaylists() {
        return playlists;
    }

    public List<Player.GetItem> getPlaylist(Playlist.playlistID id) {
        int playlistId = id.ordinal();

        if (playlistId < playlists.size())
            return playlists.get(playlistId).getItems();
        else
            return null;
    }

    /**
     * Clears the playlist and sends the OnClear notification
     */
    public void clearPlaylist(Playlist.playlistID id) {
        int playlistId = id.ordinal();

        if (playlistId >= playlists.size())
            return;

        OnClear onClearNotification = new OnClear(playlistId);
        addNotification(onClearNotification);

        playlists.get(playlistId).clear();
    }

    public void addItemToPlaylist(Playlist.playlistID id, Player.GetItem item) {
        int playlistId = id.ordinal();

        while (playlists.size() <= playlistId) {
            playlists.add(null);
        }

        PlaylistHolder playlist = playlists.get(playlistId);
        if (playlist == null) {
            playlist = new PlaylistHolder(playlistId);
            playlists.set(playlistId, playlist);
        }
        playlist.add(item);

        OnAdd onAddNotification = new OnAdd(item.getLibraryId(), item.getType(), playlistId, playlist.getIndexOf(item));
        addNotification(onAddNotification);
    }
}
