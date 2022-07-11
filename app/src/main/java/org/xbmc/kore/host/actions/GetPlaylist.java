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

package org.xbmc.kore.host.actions;


import androidx.annotation.Nullable;

import org.xbmc.kore.host.HostCompositeAction;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Retrieves the playlist items for the first non-empty playlist or null if no playlists are available.
 */
public class GetPlaylist extends HostCompositeAction<ArrayList<GetPlaylist.GetPlaylistResult>> {
    private static final String TAG = LogUtils.makeLogTag(GetPlaylist.class);

    private final static String[] propertiesToGet = new String[] {
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

    static private HashMap<String, Integer> playlistsTypesAndIds;
    private String playlistType;
    private int playlistId = -1;

    /**
     * Use this to get the first non-empty playlist
     */
    public GetPlaylist() {}

    /**
     * Use this to get a playlist for a specific playlist type
     * @param playlistType should be one of the types from {@link org.xbmc.kore.jsonrpc.type.PlaylistType.GetPlaylistsReturnType}.
     *                     If null the first non-empty playlist is returned.
     */
    public GetPlaylist(String playlistType) {
        this.playlistType = playlistType;
    }

    /**
     * Use this to get a playlist for a specific playlist id
     * @param playlistId Kodi's playlist id
     */
    public GetPlaylist(int playlistId) {
        this.playlistId = playlistId;
    }

    @Override
    public ArrayList<GetPlaylistResult> execInBackground() throws ExecutionException, InterruptedException {
        if (playlistsTypesAndIds == null)
            playlistsTypesAndIds = getPlaylists(hostConnection);

        if (playlistType != null) {
            GetPlaylistResult getPlaylistResult = retrievePlaylistItemsForType(playlistType);
            ArrayList<GetPlaylistResult> playlists = new ArrayList<>();
            playlists.add(getPlaylistResult);
            return playlists;
        } else if (playlistId > -1 ) {
            GetPlaylistResult getPlaylistResult = retrievePlaylistItemsForId(playlistId);
            ArrayList<GetPlaylistResult> playlists = new ArrayList<>();
            playlists.add(getPlaylistResult);
            return playlists;
        } else
            return retrieveNonEmptyPlaylists();
    }

    private GetPlaylistResult retrievePlaylistItemsForId(int playlistId)
            throws InterruptedException, ExecutionException {
        List<ListType.ItemsAll> playlistItems = retrievePlaylistItems(hostConnection, playlistId);
        return new GetPlaylistResult(playlistId, getPlaylistType(playlistId), playlistItems);
    }

    private GetPlaylistResult retrievePlaylistItemsForType(String type)
            throws InterruptedException, ExecutionException {
        Integer id = playlistsTypesAndIds.get(type);
        if (id == null) id = -1;
        List<ListType.ItemsAll> playlistItems = retrievePlaylistItems(hostConnection, id);
        return new GetPlaylistResult(id, type, playlistItems);
    }

    private ArrayList<GetPlaylistResult> retrieveNonEmptyPlaylists()
            throws InterruptedException, ExecutionException {
        ArrayList<GetPlaylistResult> playlists = new ArrayList<>();

        for (String type : playlistsTypesAndIds.keySet()) {
            Integer id = playlistsTypesAndIds.get(type);
            if (id == null) id = -1;
            List<ListType.ItemsAll> playlistItems = retrievePlaylistItems(hostConnection, id);
            if (!playlistItems.isEmpty())
                playlists.add(new GetPlaylistResult(id, type, playlistItems));
        }
        return playlists;
    }

    private HashMap<String, Integer> getPlaylists(HostConnection hostConnection)
            throws ExecutionException, InterruptedException {
        HashMap<String, Integer> playlistsHashMap = new HashMap<>();
        ArrayList<PlaylistType.GetPlaylistsReturnType> playlistsReturnTypes = hostConnection.execute(new Playlist.GetPlaylists()).get();
        for (PlaylistType.GetPlaylistsReturnType type : playlistsReturnTypes) {
            playlistsHashMap.put(type.type, type.playlistid);
        }
        return playlistsHashMap;
    }

    private List<ListType.ItemsAll> retrievePlaylistItems(HostConnection hostConnection, int playlistId)
            throws InterruptedException, ExecutionException {
        ApiMethod<List<ListType.ItemsAll>> apiMethod = new Playlist.GetItems(playlistId, propertiesToGet);
        return hostConnection.execute(apiMethod).get();
    }

    private String getPlaylistType(int playlistId) {
        for (String key : playlistsTypesAndIds.keySet()) {
            Integer id = playlistsTypesAndIds.get(key);
            if (id != null && id == playlistId)
                return key;
        }
        return null;
    }

    public static class GetPlaylistResult {
        final public String type;
        final public int id;
        final public List<ListType.ItemsAll> items;

        private GetPlaylistResult(int playlistId, String type, List<ListType.ItemsAll> items) {
            this.id = playlistId;
            this.type = type;
            this.items = items;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof GetPlaylistResult &&
                   this.items.equals(((GetPlaylistResult) obj).items);
        }
    }
}
