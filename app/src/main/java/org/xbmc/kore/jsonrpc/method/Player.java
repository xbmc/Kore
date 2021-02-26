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
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlayerType.GetActivePlayersReturnType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.JsonUtils;

import java.util.ArrayList;

/**
 * All JSON RPC methods in Playyer.*
 */
public class Player {

    /**
     * Returns all active players.
     */
    public static final class GetActivePlayers extends ApiMethod<ArrayList<GetActivePlayersReturnType>> {
        public final static String METHOD_NAME = "Player.GetActivePlayers";

        /**
         * Returns all active players.
         */
        public GetActivePlayers() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public ArrayList<GetActivePlayersReturnType> resultFromJson(ObjectNode jsonObject) throws ApiException {
            ArrayNode resultNode = (ArrayNode)jsonObject.get(RESULT_NODE);
            ArrayList<GetActivePlayersReturnType> res = new ArrayList<GetActivePlayersReturnType>();
            if (resultNode != null) {
                for (JsonNode node : resultNode) {
                    res.add(new GetActivePlayersReturnType(node));
                }
            }
            return res;
        }
    }

    /**
     * Retrieves the currently played item
     */
    public static final class GetItem extends ApiMethod<ListType.ItemsAll> {
        public final static String METHOD_NAME = "Player.GetItem";

        /**
         * Retrieves the currently played item
         * @param playerId Player id for which to retrieve the item
         * @param properties Properties to retrieve.
         *                   See {@link ListType.FieldsAll} for a list of accepted values
         */
        public GetItem(int playerId, String... properties) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public ListType.ItemsAll resultFromJson(ObjectNode jsonObject) throws ApiException {
            return new ListType.ItemsAll(jsonObject.get(RESULT_NODE).get("item"));
        }
    }

    /**
     * Retrieves the values of the given properties
     */
    public static final class GetProperties extends ApiMethod<PlayerType.PropertyValue> {
        public final static String METHOD_NAME = "Player.GetProperties";

        /**
         * Retrieves the values of the given properties
         * @param playerId Player id for which to retrieve the item
         * @param properties Properties to retrieve.
         *                   See {@link PlayerType.PropertyName} constants for a list of accepted values
         */
        public GetProperties(int playerId, String... properties) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public PlayerType.PropertyValue resultFromJson(ObjectNode jsonObject) throws ApiException {
            return new PlayerType.PropertyValue(jsonObject.get(RESULT_NODE));
        }
    }

    /**
     * Pauses or unpause playback and returns the new state
     */
    public static final class PlayPause extends ApiMethod<Integer> {
        public final static String METHOD_NAME = "Player.PlayPause";

        /**
         * Pauses or unpause playback and returns the new state
         * @param playerId Player id for which to toggle the state
         */
        public PlayPause(int playerId) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("play", "toggle");
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public Integer resultFromJson(ObjectNode jsonObject) throws ApiException {
            return JsonUtils.intFromJsonNode(jsonObject.get(RESULT_NODE), "speed");
        }
    }

    /**
     * Set the speed of the current playback
     */
    public static final class SetSpeed extends ApiMethod<Integer> {
        public final static String METHOD_NAME = "Player.SetSpeed";

        /**
         * Set the speed of the current playback
         * @param playerId Player id for which to toggle the state
         * @param speed String enum in {@link org.xbmc.kore.jsonrpc.type.GlobalType.IncrementDecrement}
         */
        public SetSpeed(int playerId, String speed) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("speed", speed);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public Integer resultFromJson(ObjectNode jsonObject) throws ApiException {
            return JsonUtils.intFromJsonNode(jsonObject.get(RESULT_NODE), "speed");
        }
    }

    /**
     * Stops playback
     */
    public static final class Stop extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.Stop";

        /**
         * Stops playback
         * @param playerId Player id for which to stop playback
         */
        public Stop(int playerId) {
            super();
            addParameterToRequest("playerid", playerId);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Go to previous/next/specific item in the playlist.
     */
    public static final class GoTo extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.GoTo";

        /**
         * Go to constants
         */
        public static final String PREVIOUS = "previous";
        public static final String NEXT = "next";

        /**
         * Go to previous/next/specific item in the playlist.
         * @param playerId Player id for which to stop playback
         * @param to Where to go. See this class constants for values
         */
        public GoTo(int playerId, String to) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("to", to);
        }

        /**
         * Go to previous/next/specific item in the playlist.
         * @param playerId Player id for which to stop playback
         * @param to position in playlist
         */
        public GoTo(int playerId, int to) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("to", to);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Seek through the playing item
     */
    public static final class Seek extends ApiMethod<PlayerType.SeekReturnType> {
        public final static String METHOD_NAME = "Player.Seek";

        /**
         * Seek constants
         */
        public static final String BACKWARD = "smallbackward";
        public static final String FORWARD = "smallforward";

        /**
         * Seek through the playing item (by time)
         * @param playerId Player id for which to stop playback
         * @param value Where to seek
         */
        public Seek(int playerId, PlayerType.PositionTime value) {
            super();
            addParameterToRequest("playerid", playerId);
            ObjectNode valueObject = objectMapper.createObjectNode();
            if (value != null)
                valueObject.put("time", value.toJsonNode());
            addParameterToRequest("value", valueObject);
        }

        /**
         * Seek through the playing item (by percentage)
         * @param playerId Player id for which to stop playback
         * @param value Percentage
         */
        public Seek(int playerId, int value) {
            super();
            addParameterToRequest("playerid", playerId);
            ObjectNode valueObject = objectMapper.createObjectNode();
            valueObject.put("percentage", value);
            addParameterToRequest("value", valueObject);
        }

        /**
         * Seek through the playing item (by step)
         * @param playerId Player id for which to stop playback
         * @param value step (smallbackward/smallforward)
         */
        public Seek(int playerId, String value) {
            super();
            addParameterToRequest("playerid", playerId);
            ObjectNode valueObject = objectMapper.createObjectNode();
            valueObject.put("step", value);
            addParameterToRequest("value", valueObject);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public PlayerType.SeekReturnType resultFromJson(ObjectNode jsonObject) throws ApiException {
            return new PlayerType.SeekReturnType(jsonObject.get(RESULT_NODE));
        }
    }

    /**
     * Set the subtitle displayed by the player
     */
    public static final class SetSubtitle extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.SetSubtitle";

        /**
         * SetSubtitle constants
         */
        public static final String PREVIOUS = "previous";
        public static final String NEXT = "next";
        public static final String OFF = "off";
        public static final String ON = "on";

        /**
         * Set the subtitle displayed by the player
         * @param playerId Player id for which to stop playback
         * @param subtitle One of the constanstants of this class
         * @param enable Whether to enable subtitles to be displayed after setting the new subtitle
         */
        public SetSubtitle(int playerId, String subtitle, boolean enable) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("subtitle", subtitle);
            addParameterToRequest("enable", enable);
        }

        /**
         * Set the subtitle displayed by the player
         * @param playerId Player id for which to stop playback
         * @param subtitle Index of the subtitle to display
         * @param enable Whether to enable subtitles to be displayed after setting the new subtitle
         */
        public SetSubtitle(int playerId, int subtitle, boolean enable) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("subtitle", subtitle);
            addParameterToRequest("enable", enable);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Set the audio stream played by the player
     */
    public static final class SetAudioStream extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.SetAudioStream";

        /**
         * SetAudioStream constants
         */
        public static final String PREVIOUS = "previous";
        public static final String NEXT = "next";

        /**
         * Set the audio stream played by the player
         * @param playerId Player id for which to stop playback
         * @param stream One of the constanstants of this class
         */
        public SetAudioStream(int playerId, String stream) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("stream", stream);
        }

        /**
         * Set the audio stream played by the player
         * @param playerId Player id for which to stop playback
         * @param stream Index of the audio stream to play
         */
        public SetAudioStream(int playerId, int stream) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("stream", stream);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Set the repeat mode of the player
     */
    public static final class SetRepeat extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.SetRepeat";

        /**
         * Set the repeat mode of the player
         * @param playerId Player id for which to stop playback
         * @param repeat Repeat mode, see {@link org.xbmc.kore.jsonrpc.type.PlayerType.Repeat}
         */
        public SetRepeat(int playerId, String repeat) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("repeat", repeat);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Shuffle/Unshuffle items in the player
     */
    public static final class SetShuffle extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.SetShuffle";

        /**
         * Shuffle/Unshuffle items in the player
         * @param playerId Player id for which to shuffle
         * @param shuffle True/false
         */
        public SetShuffle(int playerId, boolean shuffle) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("shuffle", shuffle);
        }

        /**
         * Shuffle/Unshuffle items in the player
         * @param playerId Player id for which to shuffle
         */
        public SetShuffle(int playerId) {
            super();
            addParameterToRequest("playerid", playerId);
            addParameterToRequest("shuffle", "toggle");
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Start playback of either the playlist with the given ID, a slideshow with the pictures
     * from the given directory or a single file or an item from the database.
     */
    public static final class Open extends ApiMethod<String> {
        public final static String METHOD_NAME = "Player.Open";

        public final static String TYPE_PLAYLIST = "playlist",
                TYPE_CHANNEL = "channel",
                TYPE_RECORDING = "recording";

        /**
         * Start playback of either the playlist with the given ID, a slideshow with the pictures
         * from the given directory or a single file or an item from the database.
         * @param itemType This should always be TYPE_PLAYLIST
         * @param playlistId Id
         * @param position Position to start
         */
        public Open(String itemType, int playlistId, int position) {
            super();
            final ObjectNode item = objectMapper.createObjectNode();
            item.put("playlistid", playlistId);
            item.put("position", position);
            addParameterToRequest("item", item);
        }

        /**
         * Start playback of either the playlist with the given ID, a slideshow with the pictures
         * from the given directory or a single file or an item from the database.
         * @param playlistItem Item to play
         */
        public Open(PlaylistType.Item playlistItem) {
            super();
            addParameterToRequest("item", playlistItem.toJsonNode());
        }

        /**
         * Starts playing a playlist or channel
         * @param itemType TYPE_PLAYLIST or TYPE_CHANNEL
         * @param itemId Corresponding ID to open
         */
        public Open(String itemType, int itemId) {
            super();
            final ObjectNode item = objectMapper.createObjectNode();
            switch (itemType) {
                case TYPE_PLAYLIST:
                    item.put("playlistid", itemId);
                    break;
                case TYPE_CHANNEL:
                    item.put("channelid", itemId);
                    break;
                case TYPE_RECORDING:
                    item.put("recordingid", itemId);
                    break;
            }
            addParameterToRequest("item", item);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Send notification message to XBMC/Kodi
     */
    public static final class Notification extends ApiMethod<String> {
        public final static String METHOD_NAME = "GUI.ShowNotification";

        /**
         * Sends a text notification message to XBMC/Kodi
         * @param title The title of the notification
         * @param message The text message of the notification
         */
        public Notification(String title, String message) {
            super(false);
            addParameterToRequest("title", title);
            addParameterToRequest("message", message);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }
}
