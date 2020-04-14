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
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.VideoType;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON RPC methods in VideoLibrary.*
 */
public class VideoLibrary {

    /**
     * Cleans the video library from non-existent items.
     */
    public static class Clean extends ApiMethod<String> {
        public final static String METHOD_NAME = "VideoLibrary.Clean";

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
     * Scans the video sources for new library items.
     */
    public static class Scan extends ApiMethod<String> {
        public final static String METHOD_NAME = "VideoLibrary.Scan";

        /**
         * Scans the video sources for new library items.
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
     * Retrieve all movies
     */
    public static class GetMovies extends ApiMethod<ApiList<VideoType.DetailsMovie>> {
        public final static String METHOD_NAME = "VideoLibrary.GetMovies";

        private final static String LIST_NODE = "movies";

        /**
         * Retrieve all movies, without limits
         * Caution, this can break in large libraries
         *
         * @param properties Properties to retrieve. See {@link VideoType.FieldsMovie} for a list of
         *                   accepted values
         */
        public GetMovies(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        /**
         * Retrieve all movies, with limits
         *
         * @param limits Limits to retrieve. See {@link ListType.Limits}
         * @param properties Properties to retrieve. See {@link VideoType.FieldsMovie} for a list of
         *                   accepted values
         */
        public GetMovies(ListType.Limits limits, String... properties) {
            super();
            addParameterToRequest("properties", properties);
            addParameterToRequest("limits", limits);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public ApiList<VideoType.DetailsMovie> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            ListType.LimitsReturned limits = new ListType.LimitsReturned(jsonObject);

            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ApiList<>(new ArrayList<>(0), limits);
            }
            ArrayList<VideoType.DetailsMovie> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new VideoType.DetailsMovie(item));
            }

            return new ApiList<>(result, limits);
        }
    }

    /**
     * Retrieve details about a specific movie
     */
    public static class GetMovieDetails extends ApiMethod<VideoType.DetailsMovie> {
        public final static String METHOD_NAME = "VideoLibrary.GetMovieDetails";

        /**
         * Retrieve details about a specific movie
         *
         * @param movieId Movie id
         * @param properties Properties to retrieve. See {@link VideoType.FieldsMovie} for a list of
         *                   accepted values
         */
        public GetMovieDetails(int movieId, String... properties) {
            super();
            addParameterToRequest("movieid", movieId);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public VideoType.DetailsMovie resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            return  new VideoType.DetailsMovie(jsonObject.get(RESULT_NODE).get("moviedetails"));
        }
    }

    /**
     * Update the given movie with the given details
     * Just the parameters we can change in the gui
     */
    public static class SetMovieDetails extends ApiMethod<String> {
        public final static String METHOD_NAME = "VideoLibrary.SetMovieDetails";

        /**
         * Update the given movie with the given details
         *
         * @param movieid Movie id
         */
        public SetMovieDetails(int movieid, Integer playcount, Double rating) {
            super();
            addParameterToRequest("movieid", movieid);
            if (playcount != null) addParameterToRequest("playcount", playcount);
            if (rating != null) addParameterToRequest("rating", rating);
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
     * Retrieve all TV Shows
     */
    public static class GetTVShows extends ApiMethod<ApiList<VideoType.DetailsTVShow>> {
        public final static String METHOD_NAME = "VideoLibrary.GetTVShows";

        private final static String LIST_NODE = "tvshows";

        /**
         * Retrieve all tv shows
         *
         * @param properties Properties to retrieve. See {@link VideoType.FieldsTVShow} for a
         *                   list of accepted values
         */
        public GetTVShows(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        /**
         * Retrieve all tv shows, with limits
         *
         * @param limits Limits to retrieve. See {@link ListType.Limits}
         * @param properties Properties to retrieve. See {@link VideoType.FieldsMovie} for a list of
         *                   accepted values
         */
        public GetTVShows(ListType.Limits limits, String... properties) {
            super();
            addParameterToRequest("properties", properties);
            addParameterToRequest("limits", limits);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public ApiList<VideoType.DetailsTVShow> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            ListType.LimitsReturned limits = new ListType.LimitsReturned(jsonObject);
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ApiList<>(new ArrayList<>(0), limits);
            }
            ArrayList<VideoType.DetailsTVShow> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new VideoType.DetailsTVShow(item));
            }

            return new ApiList<>(result, limits);
        }
    }

    /**
     * Retrieve details about a specific tv show
     */
    public static class GetTVShowDetails extends ApiMethod<VideoType.DetailsTVShow> {
        public final static String METHOD_NAME = "VideoLibrary.GetTVShowDetails";

        /**
         * Retrieve details about a specific tv show
         *
         * @param tvshowId Show id
         * @param properties Properties to retrieve. See {@link VideoType.FieldsTVShow} for a
         *                   list of accepted values
         */
        public GetTVShowDetails(int tvshowId, String... properties) {
            super();
            addParameterToRequest("tvshowid", tvshowId);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public VideoType.DetailsTVShow resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            return  new VideoType.DetailsTVShow(jsonObject.get(RESULT_NODE).get("tvshowdetails"));
        }
    }

    /**
     * Update the given episode with the given details
     * Just the parameters we can change in the gui
     */
    public static class SetEpisodeDetails extends ApiMethod<String> {
        public final static String METHOD_NAME = "VideoLibrary.SetEpisodeDetails";

        /**
         * Update the given episode with the given details
         *
         * @param episodeid Episode id
         */
        public SetEpisodeDetails(int episodeid, Integer playcount, Double rating) {
            super();
            addParameterToRequest("episodeid", episodeid);
            if (playcount != null) addParameterToRequest("playcount", playcount);
            if (rating != null) addParameterToRequest("rating", rating);
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
     * Retrieve all tv seasons
     */
    public static class GetSeasons extends ApiMethod<List<VideoType.DetailsSeason>> {
        public final static String METHOD_NAME = "VideoLibrary.GetSeasons";

        private final static String LIST_NODE = "seasons";

        /**
         * Retrieve all tv seasons
         *
         * @param tvshowid TV Show id
         * @param properties Properties to retrieve. See {@link VideoType.FieldsSeason} for a
         *                   list of accepted values
         */
        public GetSeasons(int tvshowid, String... properties) {
            super();
            addParameterToRequest("tvshowid", tvshowid);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<VideoType.DetailsSeason> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<>(0);
            }
            ArrayList<VideoType.DetailsSeason> result = new ArrayList<>(items
                    .size());

            for (JsonNode item : items) {
                result.add(new VideoType.DetailsSeason(item));
            }

            return result;
        }
    }

    /**
     * Retrieve all tv show episodes
     */
    public static class GetEpisodes extends ApiMethod<List<VideoType.DetailsEpisode>> {
        public final static String METHOD_NAME = "VideoLibrary.GetEpisodes";

        private final static String LIST_NODE = "episodes";

        /**
         * Retrieve all tv show episodes
         *
         * @param tvshowid TV Show id
         * @param properties Properties to retrieve. See {@link VideoType.FieldsEpisode} for a
         *                   list of accepted values
         */
        public GetEpisodes(int tvshowid, String... properties) {
            super();
            addParameterToRequest("tvshowid", tvshowid);
            addParameterToRequest("properties", properties);
        }

        /**
         * Retrieve all tv show episodes
         *
         * @param tvshowid TV Show id
         * @param season Season
         * @param properties Properties to retrieve. See {@link VideoType.FieldsEpisode} for a
         *                   list of accepted values
         */
        public GetEpisodes(int tvshowid, int season, String... properties) {
            super();
            addParameterToRequest("tvshowid", tvshowid);
            addParameterToRequest("season", season);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<VideoType.DetailsEpisode> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<>(0);
            }
            ArrayList<VideoType.DetailsEpisode> result = new ArrayList<>(items
                    .size());

            for (JsonNode item : items) {
                result.add(new VideoType.DetailsEpisode(item));
            }

            return result;
        }
    }

    /**
     * Retrieve all music videos
     */
    public static class GetMusicVideos extends ApiMethod<List<VideoType.DetailsMusicVideo>> {
        public final static String METHOD_NAME = "VideoLibrary.GetMusicVideos";

        private final static String LIST_NODE = "musicvideos";

        /**
         * Retrieve all music videos
         *
         * @param properties Properties to retrieve. See {@link VideoType.FieldsMusicVideo} for a
         *                   list of accepted values
         */
        public GetMusicVideos(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<VideoType.DetailsMusicVideo> resultFromJson(ObjectNode jsonObject)
                throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<>(0);
            }
            ArrayList<VideoType.DetailsMusicVideo> result =
                    new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new VideoType.DetailsMusicVideo(item));
            }

            return result;
        }
    }
}