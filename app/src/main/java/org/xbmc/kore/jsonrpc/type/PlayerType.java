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
package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Return types for methods in Player.*
 */
public class PlayerType {

    /**
     * GetActivePlayers return type
     */
    public static final class GetActivePlayersReturnType {
        private final static String PLAYERID = "playerid";
        private final static String TYPE = "type";

        public final static String VIDEO = "video";
        public final static String AUDIO = "audio";
        public final static String PICTURE = "picture";

        /**
         * Player id currently active
         */
        public final int playerid;
        /**
         * Type of player. See this class public constants
         */
        public final String type;

        public GetActivePlayersReturnType(JsonNode node) {
            playerid = node.has(PLAYERID) ? node.get(PLAYERID).asInt(-1) : -1;
            type = node.has(TYPE) ? node.get(TYPE).textValue() : null;
        }
    }

    /**
     * Enums for Player.Property.Name
     */
    public interface PropertyName {
        public final String TYPE = "type";
        public final String PARTYMODE = "partymode";
        public final String SPEED = "speed";
        public final String TIME = "time";
        public final String PERCENTAGE = "percentage";
        public final String TOTALTIME = "totaltime";
        public final String PLAYLISTID = "playlistid";
        public final String POSITION = "position";
        public final String REPEAT = "repeat";
        public final String SHUFFLED = "shuffled";
        public final String CANSEEK = "canseek";
        public final String CANCHANGESPEED = "canchangespeed";
        public final String CANMOVE = "canmove";
        public final String CANZOOM = "canzoom";
        public final String CANROTATE = "canrotate";
        public final String CANSHUFFLE = "canshuffle";
        public final String CANREPEAT = "canrepeat";
        public final String CURRENTAUDIOSTREAM = "currentaudiostream";
        public final String AUDIOSTREAMS = "audiostreams";
        public final String SUBTITLEENABLED = "subtitleenabled";
        public final String CURRENTSUBTITLE = "currentsubtitle";
        public final String SUBTITLES = "subtitles";
        public final String LIVE = "live";

        public final String[] allValues = new String[]{
                TYPE, PARTYMODE, SPEED, TIME, PERCENTAGE, TOTALTIME, PLAYLISTID, POSITION, REPEAT,
                SHUFFLED, CANSEEK, CANCHANGESPEED, CANMOVE, CANZOOM, CANROTATE, CANSHUFFLE,
                CANREPEAT, CURRENTAUDIOSTREAM, AUDIOSTREAMS, SUBTITLEENABLED, CURRENTSUBTITLE,
                SUBTITLES, LIVE
        };
    }

    /**
     * Player.Property.Value
     */
    public static class PropertyValue {
        /**
         * Player.Type
         */
        public static final String TYPE_VIDEO = "video";
        public static final String TYPE_AUDIO = "audio";
        public static final String TYPE_PICTURE = "picture";

        /**
         * Properties
         */
        public static final String AUDIOSTREAMS = "audiostreams";
        public static final String CANCHANGESPEED = "canchangespeed";
        public static final String CANMOVE = "canmove";
        public static final String CANREPEAT = "canrepeat";
        public static final String CANROTATE = "canrotate";
        public static final String CANSEEK = "canseek";
        public static final String CANSHUFFLE = "canshuffle";
        public static final String CANZOOM = "canzoom";
        public static final String CURRENTAUDIOSTREAM = "currentaudiostream";
        public static final String CURRENTSUBTITLE = "currentsubtitle";
        public static final String LIVE = "live";
        public static final String PARTYMODE = "partymode";
        public static final String PERCENTAGE = "percentage";
        public static final String PLAYLISTID = "playlistid";
        public static final String POSITION = "position";
        public static final String REPEAT = "repeat";
        public static final String SHUFFLED = "shuffled";
        public static final String SPEED = "speed";
        public static final String SUBTITLEENABLED = "subtitleenabled";
        public static final String SUBTITLES = "subtitles";
        public static final String TIME = "time";
        public static final String TOTALTIME = "totaltime";
        public static final String TYPE = "type";

        public final List<AudioStream> audiostreams;
        public final boolean canchangespeed;
        public final boolean canmove;
        public final boolean canrepeat;
        public final boolean canrotate;
        public final boolean canseek;
        public final boolean canshuffle;
        public final boolean canzoom;
        public final AudioStreamExtended currentaudiostream;
        public final Subtitle currentsubtitle;
        public final boolean live;
        public final boolean partymode;
        public final double percentage;
        public final int playlistid;
        public final int position;
        public final String repeat;
        public final boolean shuffled;
        public final int speed;
        public final boolean subtitleenabled;
        public final List<Subtitle> subtitles;
        public final GlobalType.Time time;
        public final GlobalType.Time totaltime;
        public final String type;


        public PropertyValue(JsonNode node) {
            audiostreams = node.has(AUDIOSTREAMS) ? AudioStream.getListAudioStream(node.get(AUDIOSTREAMS)) : null;
            canchangespeed = JsonUtils.booleanFromJsonNode(node, CANCHANGESPEED, false);
            canmove = JsonUtils.booleanFromJsonNode(node, CANMOVE, false);
            canrepeat = JsonUtils.booleanFromJsonNode(node, CANREPEAT, false);
            canrotate = JsonUtils.booleanFromJsonNode(node, CANROTATE, false);
            canseek = JsonUtils.booleanFromJsonNode(node, CANSEEK, false);
            canshuffle = JsonUtils.booleanFromJsonNode(node, CANSHUFFLE, false);
            canzoom = JsonUtils.booleanFromJsonNode(node, CANZOOM, false);
            currentaudiostream = node.has(CURRENTAUDIOSTREAM) ? new AudioStreamExtended(node.get(CURRENTAUDIOSTREAM)) : null;
            currentsubtitle = node.has(CURRENTSUBTITLE) ? new Subtitle(node.get(CURRENTSUBTITLE)) : null;
            live = JsonUtils.booleanFromJsonNode(node, LIVE, false);
            partymode = JsonUtils.booleanFromJsonNode(node, PARTYMODE, false);
            percentage = JsonUtils.doubleFromJsonNode(node, PERCENTAGE, 0);
            playlistid = JsonUtils.intFromJsonNode(node, PLAYLISTID, -1);
            position = JsonUtils.intFromJsonNode(node, POSITION, -1);
            repeat = JsonUtils.stringFromJsonNode(node, REPEAT, "off");
            shuffled = JsonUtils.booleanFromJsonNode(node, SHUFFLED, false);
            speed = JsonUtils.intFromJsonNode(node, SPEED, 0);
            subtitleenabled = JsonUtils.booleanFromJsonNode(node, SUBTITLEENABLED, false);
            subtitles = node.has(SUBTITLES) ? Subtitle.getListSubtitle(node.get(SUBTITLES)) : null;
            time = node.has(TIME) ? new GlobalType.Time(node.get(TIME)) : null;
            totaltime = node.has(TOTALTIME) ? new GlobalType.Time(node.get(TOTALTIME)) : null;
            type = JsonUtils.stringFromJsonNode(node, TYPE, "video");
        }
    }

    /**
     * Player.Audio.Stream
     */
    public static class AudioStream {
        public static final String INDEX = "index";
        public static final String LANGUAGE = "language";
        public static final String NAME = "name";

        public final int index;
        public final String language;
        public final String name;

        public AudioStream(JsonNode node) {
            index = JsonUtils.intFromJsonNode(node, INDEX);
            language = JsonUtils.stringFromJsonNode(node, LANGUAGE);
            name = JsonUtils.stringFromJsonNode(node, NAME);
        }

        public static List<AudioStream> getListAudioStream(JsonNode node) {
            final ArrayNode arrayNode = (ArrayNode)node;
            final List<AudioStream> result = new ArrayList<AudioStream>(node.size());

            for (JsonNode audioStreamNode : arrayNode) {
                result.add(new AudioStream(audioStreamNode));
            }
            return result;
        }
    }

    /**
     * Player.Audio.Stream.Extended
     */
    public static class AudioStreamExtended extends AudioStream {
        public static final String BITRATE = "bitrate";
        public static final String CHANNELS = "channels";
        public static final String CODEC = "codec";

        public final int bitrate;
        public final int channels;
        public final String codec;

        public AudioStreamExtended(JsonNode node) {
            super(node);
            bitrate = JsonUtils.intFromJsonNode(node, BITRATE);
            channels = JsonUtils.intFromJsonNode(node, CHANNELS);
            codec = JsonUtils.stringFromJsonNode(node, CODEC);
        }
    }

    /**
     * Player.Subtitle
     */
    public static class Subtitle {
        public static final String INDEX = "index";
        public static final String LANGUAGE = "language";
        public static final String NAME = "name";

        public final int index;
        public final String language;
        public final String name;

        public Subtitle(JsonNode node) {
            index = JsonUtils.intFromJsonNode(node, INDEX);
            language = JsonUtils.stringFromJsonNode(node, LANGUAGE);
            name = JsonUtils.stringFromJsonNode(node, NAME);
        }

        public static List<Subtitle> getListSubtitle(JsonNode node) {
            final ArrayNode arrayNode = (ArrayNode)node;
            final List<Subtitle> result = new ArrayList<Subtitle>(node.size());

            for (JsonNode subtitleNode : arrayNode) {
                result.add(new Subtitle(subtitleNode));
            }
            return result;
        }
    }

    /**
     * Player.Position.Time
     */
    public static class PositionTime
            implements ApiParameter {
        public static final String HOURS = "hours";
        public static final String MILLISECONDS = "milliseconds";
        public static final String MINUTES = "minutes";
        public static final String SECONDS = "seconds";

        protected static final ObjectMapper objectMapper = new ObjectMapper();

        public final int hours;
        public final int milliseconds;
        public final int minutes;
        public final int seconds;

        public PositionTime(int hours, int minutes, int seconds, int milliseconds) {
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.milliseconds = milliseconds;
        }

        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put(HOURS, hours);
            node.put(MILLISECONDS, milliseconds);
            node.put(MINUTES, minutes);
            node.put(SECONDS, seconds);
            return node;
        }
    }

    /**
     * Player.Seek return type
     */
    public static final class SeekReturnType {
        private final static String PERCENTAGE = "percentage";
        private final static String TIME = "time";
        private final static String TOTAL_TIME = "totaltime";

        /**
         * Percentage
         */
        public final int percentage;
        /**
         * Time and Total time
         */
        public final GlobalType.Time time;
        public final GlobalType.Time totalTime;

        public SeekReturnType(JsonNode node) {
            percentage = JsonUtils.intFromJsonNode(node, PERCENTAGE);
            time = new GlobalType.Time(node.get(TIME));
            totalTime = new GlobalType.Time(node.get(TOTAL_TIME));
        }
    }

    /**
     * Player.Repeat constants
     */
    public interface Repeat {
        public final String OFF = "off";
        public final String ONE = "one";
        public final String ALL = "all";

        public final String CYCLE = "cycle";
    }

    /**
     * Player.Open type
     */

    public static final class ResumeMode implements ApiParameter {
        private static final String RESUME_MODE = "resume";
        public final boolean resume_mode;

        protected static final ObjectMapper objectMapper = new ObjectMapper();
        public ResumeMode(boolean resume) {
            this.resume_mode = resume;
        }
        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put(RESUME_MODE, resume_mode);
            return node;
        }
    }

    /**
     * PlayerType.Item
     */
    public static class Item implements ApiParameter {

        protected static final ObjectMapper objectMapper = new ObjectMapper();

        public final String uri;
        /**
         * Constructors
         */
        public Item(String uri) {
            this.uri = uri;
        }

        @Override
        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put("file", uri);
            return node;
        }
    }
}