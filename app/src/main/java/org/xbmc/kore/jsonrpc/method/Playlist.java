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
package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.jsonrpc.type.PlaylistType.GetPlaylistsReturnType;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON RPC methods in Playlist.*
 */
public class Playlist {

    /**
     * Returns all existing playlists
     */
    public static final class GetPlaylists extends ApiMethod<ArrayList<GetPlaylistsReturnType>> {
        public final static String METHOD_NAME = "Playlist.GetPlaylists";

        /**
         * Returns all existing playlists
         */
        public GetPlaylists() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public ArrayList<GetPlaylistsReturnType> resultFromJson(ObjectNode jsonObject) throws ApiException {
            ArrayNode resultNode = (ArrayNode)jsonObject.get(RESULT_NODE);
            ArrayList<GetPlaylistsReturnType> res = new ArrayList<>();
            if (resultNode != null) {
                for (JsonNode node : resultNode) {
                    res.add(new GetPlaylistsReturnType(node));
                }
            }
            return res;
        }
    }

    /**
     * Get all items from playlist
     */
    public static final class GetItems extends ApiMethod<List<ListType.ItemsAll>> {
        public final static String METHOD_NAME = "Playlist.GetItems";

        /**
         * Get all items from playlist
         * @param playlistId Playlist id for which to get the items
         * @param properties Properties to retrieve.
         *                   See {@link ListType.FieldsAll} for a list of accepted values
         */
        public GetItems(int playlistId, String... properties) {
            super();
            addParameterToRequest("playlistid", playlistId);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public List<ListType.ItemsAll> resultFromJson(ObjectNode jsonObject) throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            if (!resultNode.has("items") || (!resultNode.get("items").isArray()) ||
                    ((resultNode.get("items")).size() == 0)) {
                return new ArrayList<>(0);
            }
            ArrayNode items = (ArrayNode)resultNode.get("items");
            ArrayList<ListType.ItemsAll> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new ListType.ItemsAll(item));
            }

            return result;
        }
    }

    /**
     * Clear playlist
     */
    public static final class Clear extends ApiMethod<String> {
        public final static String METHOD_NAME = "Playlist.Clear";

        /**
         * Clear playlist
         */
        public Clear(int playlistId) {
            super();
            addParameterToRequest("playlistid", playlistId);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Remove item from playlist. Does not work for picture playlists (aka slideshows).
     */
    public static final class Remove extends ApiMethod<String> {
        public final static String METHOD_NAME = "Playlist.Remove";

        /**
         * Remove item from playlist. Does not work for picture playlists (aka slideshows).
         */
        public Remove(int playlistId, int position) {
            super();
            addParameterToRequest("playlistid", playlistId);
            addParameterToRequest("position", position);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Add item(s) to playlist
     */
    public static final class Add extends ApiMethod<String> {
        public final static String METHOD_NAME = "Playlist.Add";

        /**
         * Add item(s) to playlist
         */
        public Add(int playlistId, PlaylistType.Item item) {
            super();
            addParameterToRequest("playlistid", playlistId);
            addParameterToRequest("item", item);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }


    public static final class Insert extends ApiMethod<String> {
        public final static String METHOD_NAME = "Playlist.Insert";

        /**
         * Add item(s) to playlist
         */
        public Insert(int playlistId, int position, PlaylistType.Item item) {
            super();
            addParameterToRequest("playlistid", playlistId);
            addParameterToRequest("position", position);
            addParameterToRequest("item", item);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }
}
