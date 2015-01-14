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
package com.syncedsynapse.kore2.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syncedsynapse.kore2.utils.JsonUtils;

/**
 * Return types for methods in Playlist.*
 */
public class PlaylistType {

    /**
     * GetPlaylists return type
     */
    public static final class GetPlaylistsReturnType {
        private final static String PLAYLISTID = "playlistid";
        private final static String TYPE = "type";

        public final static String UNKNOWN = "unknown";
        public final static String VIDEO = "video";
        public final static String AUDIO = "audio";
        public final static String PICTURE = "picture";
        public final static String MIXED = "mixed";

        /**
         * Playlist id
         */
        public final int playlistid;
        /**
         * Type of playlist. See this class public constants
         */
        public final String type;

        public GetPlaylistsReturnType(JsonNode node) {
            playlistid = node.get(PLAYLISTID).asInt(-1);
            type = JsonUtils.stringFromJsonNode(node, TYPE, UNKNOWN);
        }
    }

    /**
     * Playlist.Item
     */
    public static class Item implements ApiParameter {

        protected static final ObjectMapper objectMapper = new ObjectMapper();

        // class members
        public int albumid = -1;
        public int artistid = -1;
        public String directory = null;
        public int episodeid = -1;
        public String file = null;
        public int genreid = -1;
        public int movieid = -1;
        public int musicvideoid = -1;
        public int songid = -1;

        /**
         * Constructors
         */
        public Item() {
        }

        @Override
        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            if (albumid != -1) {
                node.put("albumid", albumid);
            }
            if (artistid != -1) {
                node.put("artistid", artistid);
            }
            if (directory != null) {
                node.put("directory", directory);
            }
            if (episodeid != -1) {
                node.put("episodeid", episodeid);
            }
            if (file != null) {
                node.put("file", file);
            }
            if (genreid != -1) {
                node.put("genreid", genreid);
            }
            if (movieid != -1) {
                node.put("movieid", movieid);
            }
            if (musicvideoid != -1) {
                node.put("musicvideoid", musicvideoid);
            }
            if (songid != -1) {
                node.put("songid", songid);
            }
            return node;
        }
    }
}
