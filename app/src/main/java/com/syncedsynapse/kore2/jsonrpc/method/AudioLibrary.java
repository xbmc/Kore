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
package com.syncedsynapse.kore2.jsonrpc.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syncedsynapse.kore2.jsonrpc.ApiException;
import com.syncedsynapse.kore2.jsonrpc.ApiMethod;
import com.syncedsynapse.kore2.jsonrpc.type.AudioType;
import com.syncedsynapse.kore2.jsonrpc.type.LibraryType;
import com.syncedsynapse.kore2.jsonrpc.type.ListType;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON RPC methods in AudioLibrary.*
 */
public class AudioLibrary {

    /**
     * Cleans the audio library from non-existent items.
     */
    public static class Clean extends ApiMethod<String> {
        public final static String METHOD_NAME = "AudioLibrary.Clean";

        /**
         * Cleans the video library from non-existent items.
         */
        public Clean() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Scans the audio sources for new library items.
     */
    public static class Scan extends ApiMethod<String> {
        public final static String METHOD_NAME = "AudioLibrary.Scan";

        /**
         * Scans the audio sources for new library items.
         */
        public Scan() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Retrieve all artists
     */
    public static class GetArtists extends ApiMethod<List<AudioType.DetailsArtist>> {
        public final static String METHOD_NAME = "AudioLibrary.GetArtists";

        private final static String LIST_NODE = "artists";

        /**
         * Retrieve all artists
         *
         * @param albumartistsonly Whether or not to include artists only appearing in
         *                         compilations. If the parameter is not passed or is passed as
         *                         null the GUI setting will be used
         * @param properties Properties to retrieve. See {@link AudioType.FieldsArtists} for a
         *                   list of accepted values
         */
        public GetArtists(boolean albumartistsonly, String... properties) {
            super();
            addParameterToRequest("albumartistsonly", albumartistsonly);
            addParameterToRequest("properties", properties);
        }

        /**
         * Retrieve all artists with limits
         *
         * @param limits Limits to retrieve. See {@link ListType.Limits}
         * @param albumartistsonly Whether or not to include artists only appearing in
         *                         compilations. If the parameter is not passed or is passed as
         *                         null the GUI setting will be used
         * @param properties Properties to retrieve. See {@link AudioType.FieldsArtists} for a
         *                   list of accepted values
         */
        public GetArtists(ListType.Limits limits, boolean albumartistsonly, String... properties) {
            super();
            addParameterToRequest("limits", limits);
            addParameterToRequest("albumartistsonly", albumartistsonly);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<AudioType.DetailsArtist> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<AudioType.DetailsArtist>(0);
            }
            ArrayList<AudioType.DetailsArtist> result = new ArrayList<AudioType.DetailsArtist>(items.size());

            for (JsonNode item : items) {
                result.add(new AudioType.DetailsArtist(item));
            }

            return result;
        }
    }

    /**
     * Retrieve all albums from specified artist or genre
     */
    public static class GetAlbums extends ApiMethod<List<AudioType.DetailsAlbum>> {
        public final static String METHOD_NAME = "AudioLibrary.GetAlbums";

        private final static String LIST_NODE = "albums";

        /**
         * Retrieve all albums
         *
         * @param properties Properties to retrieve. See {@link AudioType.FieldsAlbum} for a
         *                   list of accepted values
         */
        public GetAlbums(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        /**
         * Retrieve all albums with limits
         *
         * @param limits Limits to retrieve. See {@link ListType.Limits}
         * @param properties Properties to retrieve. See {@link AudioType.FieldsAlbum} for a
         *                   list of accepted values
         */
        public GetAlbums(ListType.Limits limits, String... properties) {
            super();
            addParameterToRequest("limits", limits);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<AudioType.DetailsAlbum> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<AudioType.DetailsAlbum>(0);
            }
            ArrayList<AudioType.DetailsAlbum> result = new ArrayList<AudioType.DetailsAlbum>(items.size());
            for (JsonNode item : items) {
                result.add(new AudioType.DetailsAlbum(item));
            }

            return result;
        }
    }

    /**
     * Retrieve all genres
     */
    public static class GetGenres extends ApiMethod<List<LibraryType.DetailsGenre>> {
        public final static String METHOD_NAME = "AudioLibrary.GetGenres";

        private final static String LIST_NODE = "genres";

        /**
         * Retrieve all genres
         *
         * @param properties Properties to retrieve. See {@link LibraryType.FieldsGenre} for a
         *                   list of accepted values
         */
        public GetGenres(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<LibraryType.DetailsGenre> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<LibraryType.DetailsGenre>(0);
            }
            ArrayList<LibraryType.DetailsGenre> result = new ArrayList<LibraryType.DetailsGenre>(items.size());
            for (JsonNode item : items) {
                result.add(new LibraryType.DetailsGenre(item));
            }

            return result;
        }
    }

    /**
     * Retrieve all songs from specified album, artist or genre
     */
    public static class GetSongs extends ApiMethod<List<AudioType.DetailsSong>> {
        public final static String METHOD_NAME = "AudioLibrary.GetSongs";

        private final static String LIST_NODE = "songs";

        /**
         * Retrieve all songs
         *
         * @param properties Properties to retrieve. See {@link AudioType.FieldsSong} for a
         *                   list of accepted values
         */
        public GetSongs(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        /**
         * Retrieve all songs with limits
         *
         * @param limits Limits to retrieve. See {@link ListType.Limits}
         * @param properties Properties to retrieve. See {@link AudioType.FieldsSong} for a
         *                   list of accepted values
         */
        public GetSongs(ListType.Limits limits, String... properties) {
            super();
            addParameterToRequest("limits", limits);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<AudioType.DetailsSong> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);

            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<AudioType.DetailsSong>(0);
            }
            ArrayList<AudioType.DetailsSong> result = new ArrayList<AudioType.DetailsSong>(items.size());
            for (JsonNode item : items) {
                result.add(new AudioType.DetailsSong(item));
            }

            return result;
        }
    }

}
