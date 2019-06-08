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

package org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;

public class Playlist {

    /**
     * JSON response for Playlist.OnClear notification
     *
     * Example:
     * {"jsonrpc":"2.0","method":"Playlist.OnClear","params":{"data":{"playlistid":0},"sender":"xbmc"}}
     */
    public static class OnClear extends JsonResponse {
        public final static String METHOD_NAME = "Playlist.OnClear";

        public OnClear(int playlistId) {
            super();
            addMethodToResponse(METHOD_NAME);

            addDataToResponse("playlistid", playlistId);

            addParameterToResponse("sender", "xbmc");
        }
    }

    /**
     * JSON response for Playlist.OnAdd notification
     *
     * Example:
     * {"jsonrpc":"2.0","method":"Playlist.OnAdd","params":{"data":{"item":{"id":1502,"type":"song"},"playlistid":0,"position":0},"sender":"xbmc"}}
     */
    public static class OnAdd extends JsonResponse {
        public final static String METHOD_NAME = "Playlist.OnAdd";

        public OnAdd(int itemId, String type, int playlistId, int playlistPosition) {
            addMethodToResponse(METHOD_NAME);

            ObjectNode item = createObjectNode();
            item.put("id", itemId);
            item.put("type", type);
            addDataToResponse("item", item);

            addDataToResponse("playlistid", playlistId);
            addDataToResponse("position", playlistPosition);

            addParameterToResponse("sender", "xbmc");
        }
    }
}
