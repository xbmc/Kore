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

package org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;

/**
 * Serverside JSON RPC responses in Playlist.*
 */
public class Playlist {

    public enum playlistID {
        AUDIO, VIDEO, PICTURE
    }

    /**
     * JSON response for Playlist.GetItems request
     *
     * * Example:
     * Query:   {"jsonrpc":"2.0","method":"Playlist.GetItems","id":48,"params":
     *              {"playlistid":0,"properties":["art","artist","albumartist","album",
     *                                            "displayartist","episode","fanart","file","season",
     *                                            "showtitle","studio","tagline","thumbnail","title",
     *                                            "track","duration","runtime"]
     *              }
     *          }
     * Answer:  {"id":1,"jsonrpc":"2.0","result":{"items":
     *                                            [
     *                                             {"album":"My Time Is the Right Time",
     *                                             "albumartist":[],
     *                                             "art":{"artist.fanart":"image://http%3a%2f%2fmedia.theaudiodb.com%2fimages%2fmedia%2fartist%2ffanart%2fxpptss1381301172.jpg/"},
     *                                             "artist":["Alton Ellis"],
     *                                             "displayartist":"Alton Ellis",
     *                                             "duration":5,
     *                                             "fanart":"image://http%3a%2f%2fmedia.theaudiodb.com%2fimages%2fmedia%2fartist%2ffanart%2fxpptss1381301172.jpg/",
     *                                             "file":"/Users/martijn/Projects/dummymediafiles/media/music/Alton Ellis/My Time Is The Right Time/17-Black Man's Word.mp3",
     *                                             "id":41,
     *                                             "label":"Black Man's Word",
     *                                             "thumbnail":"",
     *                                             "title":"Black Man's Word",
     *                                             "track":17,
     *                                             "type":"song"}
     *                                            ],
     *                                     "limits":{"end":1,"start":0,"total":1}}}
     *
     * Playlist empty answer : {"id":48,"jsonrpc":"2.0","result":{"limits":{"end":0,"start":0,"total":0}}}
     *
     * @return JSON string
     */
    public static class GetItems extends JsonResponse {
        public final static String METHOD_NAME = "Playlist.GetItems";

        int limitsEnd;

        public GetItems(int id) {
            super(id);
        }

        @Override
        public String toJsonString() {
            setLimits(0, limitsEnd, limitsEnd);
            return super.toJsonString();
        }

        public void addItem(Player.GetItem playerItem) {
            ObjectNode resultNode = (ObjectNode) getResultNode(TYPE.OBJECT);
            JsonNode item = playerItem.getResultNode().get(Player.GetItem.ITEM);
            addToArrayNode(resultNode, "items", item);

            limitsEnd++;
        }
    }

    /**
     * JSON response for Playlist.GetPlaylists response
     *
     * Example:
     * Query:       {"jsonrpc":"2.0","method":"Playlist.GetPlaylists","id":31}
     * Response:    {"id":31,"jsonrpc":"2.0","result":[{"playlistid":0,"type":"audio"},{"playlistid":1,"type":"video"},{"playlistid":2,"type":"picture"}]}
     */
    public static class GetPlaylists extends JsonResponse {
        public final static String METHOD_NAME = "Playlist.GetPlaylists";

        public GetPlaylists(int id) {
            super(id);

            ArrayNode playlists = createArrayNode();
            playlists.add(createPlaylistNode(playlistID.AUDIO.ordinal(), "audio"));
            playlists.add(createPlaylistNode(playlistID.VIDEO.ordinal(), "video"));
            playlists.add(createPlaylistNode(playlistID.PICTURE.ordinal(), "picture"));

            setResultToResponse(playlists);
        }

        private ObjectNode createPlaylistNode(int id, String type) {
            ObjectNode playlistNode = createObjectNode();
            playlistNode.put("playlistid", id);
            playlistNode.put("type", type);
            return playlistNode;
        }
    }
}
