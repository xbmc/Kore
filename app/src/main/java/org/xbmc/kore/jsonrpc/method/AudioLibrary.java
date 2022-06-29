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
import org.xbmc.kore.jsonrpc.ApiList;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.AudioType;
import org.xbmc.kore.jsonrpc.type.LibraryType;
import org.xbmc.kore.jsonrpc.type.ListType;

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
    public static class GetArtists extends ApiMethod<ApiList<AudioType.DetailsArtist>> {
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
        public ApiList<AudioType.DetailsArtist> resultFromJson(ObjectNode jsonObject) throws ApiException {
            ListType.LimitsReturned limits = new ListType.LimitsReturned(jsonObject);

            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                    (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ApiList<>(new ArrayList<>(0), limits);
            }
            ArrayList<AudioType.DetailsArtist> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new AudioType.DetailsArtist(item));
            }

            return new ApiList<>(result, limits);
        }
    }

    /**
     * Retrieve all albums from specified artist or genre
     */
    public static class GetAlbums extends ApiMethod<ApiList<AudioType.DetailsAlbum>> {
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
        public ApiList<AudioType.DetailsAlbum> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            ListType.LimitsReturned limits = new ListType.LimitsReturned(jsonObject);

            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ApiList<>(new ArrayList<>(0), limits);
            }
            ArrayList<AudioType.DetailsAlbum> result = new ArrayList<>(items.size());
            for (JsonNode item : items) {
                result.add(new AudioType.DetailsAlbum(item));
            }

            return new ApiList<>(result, limits);
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
                return new ArrayList<>(0);
            }
            ArrayList<LibraryType.DetailsGenre> result = new ArrayList<>(items.size());
            for (JsonNode item : items) {
                result.add(new LibraryType.DetailsGenre(item));
            }

            return result;
        }
    }

    /**
     * Retrieve all songs from specified album, artist or genre
     */
    public static class GetSongs extends ApiMethod<ApiList<AudioType.DetailsSong>> {
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
        public ApiList<AudioType.DetailsSong> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            ListType.LimitsReturned limits = new ListType.LimitsReturned(jsonObject);

            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ApiList<>(new ArrayList<>(0), limits);
            }
            ArrayList<AudioType.DetailsSong> result = new ArrayList<>(items.size());
            for (JsonNode item : items) {
                result.add(new AudioType.DetailsSong(item));
            }

            return new ApiList<>(result, limits);
        }
    }

}
