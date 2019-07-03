/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonUtils;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.nodes.AudioDetailsNode;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.nodes.SubtitleDetailsNode;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.nodes.VideoDetailsNode;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;

/**
 * Serverside JSON RPC responses in Methods.Player.*
 */
public class Player {

    /**
     * JSON response for Player.Open request
     *
     * Example:
     * Query:   {"jsonrpc":"2.0","method":"Player.Open","id":77,"params":{"item":{"playlistid":0,"position":2}}}
     * Answer:  {"id":77,"jsonrpc":"2.0","result":"OK"}
     *
     * @return JSON string
     */
    public static class Open extends JsonResponse {
        public final static String METHOD_NAME = "Player.Open";

        public Open(int methodId) {
            super(methodId);
            setResultToResponse("OK");
        }
    }

    /**
     * JSON response for Player.Seek request
     *
     * Example:
     * Query:   {"jsonrpc":"2.0","method":"Player.Seek","id":41,"params":{"playerid":0,"value":{"hours":0,"milliseconds":0,"minutes":0,"seconds":2}}}
     * Answer:  {"id":41,"jsonrpc":"2.0","result":{"percentage":16.570009231567382812,"time":{"hours":0,"milliseconds":0,"minutes":0,"seconds":2},"totaltime":{"hours":0,"milliseconds":70,"minutes":0,"seconds":12}}}
     *
     * @return JSON string
     */
    public static class Seek extends JsonResponse {
        public final static String METHOD_NAME = "Player.Seek";

        public Seek(int methodId, double percentage, long timeSec, long totalTime) {
            super(methodId);
            ObjectNode resultNode = (ObjectNode) getResultNode(TYPE.OBJECT);
            resultNode.put("percentage", percentage);
            resultNode.set("time", JsonUtils.createTimeNode(createObjectNode(), timeSec));
            resultNode.set("totalTime", JsonUtils.createTimeNode(createObjectNode(), totalTime));
        }
    }

    public static class SetShuffle extends JsonResponse {
        public final static String METHOD_NAME = "Player.SetShuffle";

        public SetShuffle(int methodId, String result) {
            super(methodId);
            setResultToResponse(result);
        }
    }

    public static class SetRepeat extends JsonResponse {
        public final static String METHOD_NAME = "Player.SetRepeat";

        public SetRepeat(int methodId, String result) {
            super(methodId);
            setResultToResponse(result);
        }
    }

    public static class PlayPause extends JsonResponse {
        public final static String METHOD_NAME = "Player.PlayPause";

        public PlayPause(int methodId, int speed) {
            super(methodId);
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put("speed", speed);
        }
    }

    public static class Stop extends JsonResponse {
        public final static String METHOD_NAME = "Player.Stop";
    }

    public static class GetActivePlayers extends JsonResponse {
        public final static String METHOD_NAME = "Player.GetActivePlayers";

        public GetActivePlayers(int methodId) {
            super(methodId);
            getResultNode(TYPE.ARRAY);
        }

        public GetActivePlayers(int methodId, int playerId, String type) {
            super(methodId);
            ObjectNode objectNode = createObjectNode();
            objectNode.put("playerid", playerId);
            objectNode.put("type", type);
            ((ArrayNode) getResultNode(TYPE.ARRAY)).add(objectNode);
        }
    }


    public static class GetProperties extends JsonResponse {
        public final static String METHOD_NAME = "Player.GetProperties";

        final static String SPEED = PlayerType.PropertyName.SPEED;
        final static String PERCENTAGE = PlayerType.PropertyName.PERCENTAGE;
        final static String POSITION = PlayerType.PropertyName.POSITION;
        final static String TIME = PlayerType.PropertyName.TIME;
        final static String TOTALTIME = PlayerType.PropertyName.TOTALTIME;
        final static String REPEAT = PlayerType.PropertyName.REPEAT;
        final static String SHUFFLED = PlayerType.PropertyName.SHUFFLED;
        final static String CURRENTAUDIOSTREAM = PlayerType.PropertyName.CURRENTAUDIOSTREAM;
        final static String CURRENTSUBTITLE = PlayerType.PropertyName.CURRENTSUBTITLE;
        final static String AUDIOSTREAMS = PlayerType.PropertyName.AUDIOSTREAMS;
        final static String SUBTITLES = PlayerType.PropertyName.SUBTITLES;
        final static String PLAYLISTID = PlayerType.PropertyName.PLAYLISTID;

        public GetProperties(int methodId) {
            super(methodId);
        }

        public void addSpeed(int value) {
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put(SPEED, value);
        }

        public void addPercentage(int value) {
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put(PERCENTAGE, value);
        }

        public void addPosition(int value) {
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put(POSITION, value);
        }

        public void addTime(int hours, int minutes, int seconds, int milliseconds) {
            ObjectNode timeNode = JsonUtils.createTimeNode(createObjectNode(), hours, minutes, seconds, milliseconds);
            ((ObjectNode) getResultNode(TYPE.OBJECT)).putObject(TIME).setAll(timeNode);
        }

        public void addTotaltime(int hours, int minutes, int seconds, int milliseconds) {
            ObjectNode timeNode = JsonUtils.createTimeNode(createObjectNode(), hours, minutes, seconds, milliseconds);
            ((ObjectNode) getResultNode(TYPE.OBJECT)).putObject(TOTALTIME).setAll(timeNode);
        }

        public void addRepeat(String value) {
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put(REPEAT, value);
        }

        public void addShuffled(boolean value) {
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put(SHUFFLED, value);
        }

        public void addCurrentAudioStream(int channels, String codec, int bitrate) {
            ObjectNode objectNode = createAudioStreamNode(channels, codec, bitrate);
            ((ObjectNode) getResultNode(TYPE.OBJECT)).putObject(CURRENTAUDIOSTREAM).setAll(objectNode);
        }

        public void addCurrentSubtitle(int index, String language, String name) {
            ObjectNode objectNode = createSubtitleNode(index, language, name);
            ((ObjectNode) getResultNode(TYPE.OBJECT)).putObject(CURRENTSUBTITLE).setAll(objectNode);
        }

        public void addAudioStream(int channels, String codec, int bitrate) {
            ObjectNode objectNode = createAudioStreamNode(channels, codec, bitrate);
            addObjectToArray(AUDIOSTREAMS, objectNode);
        }

        public void addSubtitle(int index, String language, String name) {
            ObjectNode objectNode = createSubtitleNode(index, language, name);
            addObjectToArray(SUBTITLES, objectNode);
        }

        public void addPlaylistId(int value) {
            ((ObjectNode) getResultNode(TYPE.OBJECT)).put(PLAYLISTID, value);
        }

        private ObjectNode createAudioStreamNode(int channels, String codec, int bitrate) {
            ObjectNode audioNode = createObjectNode();
            audioNode.put("channels", channels);
            audioNode.put("codec", codec);
            audioNode.put("bitrate", bitrate);
            return audioNode;
        }

        private ObjectNode createSubtitleNode(int index, String language, String name) {
            ObjectNode subtitleNode = createObjectNode();
            subtitleNode.put("index", index);
            subtitleNode.put("language", language);
            subtitleNode.put("name", name);
            return subtitleNode;
        }

        private void addObjectToArray(String key, ObjectNode objectNode) {
            ObjectNode resultNode = (ObjectNode) getResultNode(TYPE.OBJECT);
            JsonNode jsonNode = resultNode.get(key);

            if(jsonNode == null) {
                ArrayNode arrayNode = createArrayNode().add(objectNode);
                resultNode.set(key, arrayNode);
            } else if(jsonNode.isArray()) {
                ((ArrayNode) jsonNode).add(objectNode);
            } else {
                LogUtils.LOGW("Player", "JsonNode at " + key + " is not of type ArrayNode");
            }
        }
    }

    /**
     * Example:
     * query: {"jsonrpc":"2.0","method":"Player.GetItem","id":4119,"params":{"playerid":0,"properties":["art","artist","albumartist","album","cast","director","displayartist","duration","episode","fanart","file","firstaired","genre","imdbnumber","plot","premiered","rating","resume","runtime","season","showtitle","streamdetails","studio","tagline","thumbnail","title","top250","track","votes","writer","year","description"]}}
     * answer: {"id":4119,"jsonrpc":"2.0","result":{"item":{"album":"My Time Is the Right Time","albumartist":["Alton Ellis"],"art":{"artist.fanart":"image://http%3a%2f%2fmedia.theaudiodb.com%2fimages%2fmedia%2fartist%2ffanart%2fxpptss1381301172.jpg/"},"artist":["Alton Ellis"],"displayartist":"Alton Ellis","duration":5,"fanart":"image://http%3a%2f%2fmedia.theaudiodb.com%2fimages%2fmedia%2fartist%2ffanart%2fxpptss1381301172.jpg/","file":"/Users/martijn/Projects/dummymediafiles/media/music/Alton Ellis/My Time Is The Right Time/06-Rock Steady.mp3","genre":["Reggae"],"id":14769,"label":"Rock Steady","rating":0,"thumbnail":"","title":"Rock Steady","track":6,"type":"song","votes":0,"year":2000}}}
     */
    public static class GetItem extends JsonResponse {
        public final static String METHOD_NAME = "Player.GetItem";

        final static String ITEM = "item";
        final static String TYPE = "type";
        final static String ART = "art";
        final static String ARTIST = "artist";
        final static String ALBUMARTIST = "albumartist";
        final static String ALBUM = "album";
        final static String CAST = "cast";
        final static String DIRECTOR = "director";
        final static String DISPLAYARTIST = "displayartist";
        final static String DURATION = "duration";
        final static String EPISODE = "episode";
        final static String FANART = "fanart";
        final static String FILE = "file";
        final static String FIRSTAIRED = "firstaired";
        final static String GENRE = "genre";
        final static String IMDBNUMBER = "imdbnumber";
        final static String PLOT = "plot";
        final static String PREMIERED = "premiered";
        final static String RATING = "rating";
        final static String RESUME = "resume";
        final static String RUNTIME = "runtime";
        final static String SEASON = "season";
        final static String SHOWTITLE = "showtitle";
        final static String STREAMDETAILS = "streamdetails";
        final static String STUDIO = "studio";
        final static String TAGLINE = "tagline";
        final static String THUMBNAIL = "thumbnail";
        final static String TITLE = "title";
        final static String TOP250 = "top250";
        final static String TRACK = "track";
        final static String VOTES = "votes";
        final static String WRITER = "writer";
        final static String YEAR = "year";
        final static String DESCRIPTION = "description";
        final static String LABEL = "label";

        public enum TYPE { unknown,
            movie,
            episode,
            musicvideo,
            song,
            picture,
            channel
        }

        private ObjectNode itemNode;

        public GetItem() {
            super();
            setupItemNode();
        }

        public GetItem(int methodId) {
            super(methodId);
            setupItemNode();
        }

        public GetItem(int methodId, String jsonString) throws IOException {
            super(methodId, jsonString);
            ObjectNode resultNode = ((ObjectNode) getResultNode(JsonResponse.TYPE.OBJECT));
            if (resultNode.has(ITEM)) {
                itemNode = (ObjectNode) resultNode.get(ITEM);
            } else {
                setupItemNode();
            }
        }

        private void setupItemNode() {
            ObjectNode resultNode = ((ObjectNode) getResultNode(JsonResponse.TYPE.OBJECT));
            itemNode = createObjectNode();
            resultNode.set(ITEM, itemNode);
        }

        public void addLibraryId(int id) {
            itemNode.put(ID_NODE, id);
        }

        /**
         * @return library identifier or -1 if not set
         */
        public int getLibraryId() {
            JsonNode idNode = itemNode.get(ID_NODE);
            if (idNode != null)
                return idNode.asInt();
            else
                return -1;
        }

        public void addType(TYPE type) {
            itemNode.put(TYPE, type.name());
        }

        public String getType() {
            return itemNode.get(TYPE).textValue();
        }

        public void addArt(String banner, String poster, String fanart, String thumbnail) {
            ObjectNode objectNode = createArtNode(banner, poster, fanart, thumbnail);
            itemNode.putObject(ART).setAll(objectNode);
        }

        public void addArtist(String artist) {
            addToArrayNode(itemNode, ARTIST, artist);
        }

        public void addAlbumArtist(String artist) {
            addToArrayNode(itemNode, ALBUMARTIST, artist);
        }

        public void addAlbum(String album) {
            itemNode.put(ALBUM, album);
        }

        public void addCast(String thumbnail, String name, String role) {
            addToArrayNode(itemNode, CAST, createCastNode(thumbnail, name, role));
        }

        public void addDirector(String director) {
            addToArrayNode(itemNode, DIRECTOR, director);
        }

        public void addDisplayartist(String displayartist) {
            itemNode.put(DISPLAYARTIST, displayartist);
        }

        public void addDuration(int duration) {
            itemNode.put(DURATION, duration);
        }

        public int getDuration() {
            return itemNode.get(DURATION).asInt();
        }

        public void addEpisode(int episode) {
            itemNode.put(EPISODE, episode);
        }

        public void addFanart(String fanart) {
            itemNode.put(FANART, fanart);
        }

        public void addFile(String file) {
            itemNode.put(FILE, file);
        }

        public void addFirstaired(String firstaired) {
            itemNode.put(FIRSTAIRED, firstaired);
        }

        public void addGenre(String genre) {
            itemNode.put(GENRE, genre);
        }

        public void addImdbnumber(String imdbnumber) {
            itemNode.put(IMDBNUMBER, imdbnumber);
        }

        public void addPlot(String plot) {
            itemNode.put(PLOT, plot);
        }

        public void addPremiered(String premiered) {
            itemNode.put(PREMIERED, premiered);
        }

        public void addRating(int rating) {
            itemNode.put(RATING, rating);
        }

        public void addResume(int position, int total) {
            itemNode.putObject(RESUME).setAll(createResumeNode(position, total));
        }

        public int getRuntime() {
            return itemNode.get(RUNTIME).asInt();
        }

        public void addRuntime(int runtime) {
            itemNode.put(RUNTIME, runtime);
        }

        public void addSeason(int season) {
            itemNode.put(SEASON, season);
        }

        public void addShowtitle(String showtitle) {
            itemNode.put(SHOWTITLE, showtitle);
        }

        public void addStreamdetails(AudioDetailsNode audioDetailsNode,
                                     VideoDetailsNode videoDetailsNode,
                                     SubtitleDetailsNode subtitleDetailsNode) {
            ObjectNode objectNode = createObjectNode();
            objectNode.putObject("audio").setAll(audioDetailsNode.getResponseNode());
            objectNode.putObject("video").setAll(videoDetailsNode.getResponseNode());
            objectNode.putObject("subtitle").setAll(subtitleDetailsNode.getResponseNode());

            itemNode.set(STREAMDETAILS, objectNode);
        }

        public void addStudio(String studio) {
            addToArrayNode(itemNode, STUDIO, studio);
        }

        public void addTagline(String tagline) {
            itemNode.put(TAGLINE, tagline);
        }

        public void addThumbnail(String thumbnail) {
            itemNode.put(THUMBNAIL, thumbnail);
        }

        public void addTitle(String title) {
            itemNode.put(TITLE, title);
        }

        public String getTitle() {
            JsonNode jsonNode = itemNode.get(TITLE);
            if (jsonNode != null)
                return jsonNode.asText();
            else
                return null;
        }

        public void addTop250(int top250) {
            itemNode.put(TOP250, top250);
        }

        public void addTrack(int track) {
            itemNode.put(TRACK, track);
        }

        public void addVotes(String votes) {
            itemNode.put(VOTES, votes);
        }

        public void addWriter(String writer) {
            addToArrayNode(itemNode, WRITER, writer);
        }

        public void addYear(int year) {
            itemNode.put(YEAR, year);
        }

        public void addDescription(String description) {
            itemNode.put(DESCRIPTION, description);
        }

        public void addLabel(String label) {
            itemNode.put(LABEL, label);
        }

        private ObjectNode createArtNode(String banner,
                                         String poster,
                                         String fanart,
                                         String thumbnail) {
            ObjectNode objectNode = createObjectNode();
            objectNode.put("poster", poster);
            objectNode.put("fanart", fanart);
            objectNode.put("thumbnail", thumbnail);
            objectNode.put("banner", banner);
            return objectNode;
        }

        private ObjectNode createArtworkNode(String banner, String poster, String fanart, String thumbnail) {
            ObjectNode objectNode = createObjectNode();
            objectNode.put("poster", poster);
            objectNode.put("fanart", fanart);
            objectNode.put("thumbnail", thumbnail);
            return objectNode;
        }

        private ObjectNode createCastNode(String thumbnail, String name, String role) {
            ObjectNode objectNode = createObjectNode();
            objectNode.put("thumbnail", thumbnail);
            objectNode.put("name", name);
            objectNode.put("role", role);
            return objectNode;
        }

        private ObjectNode createResumeNode(int position, int total) {
            ObjectNode objectNode = createObjectNode();
            objectNode.put("position", position);
            objectNode.put("total", total);
            return objectNode;
        }
    }
}
