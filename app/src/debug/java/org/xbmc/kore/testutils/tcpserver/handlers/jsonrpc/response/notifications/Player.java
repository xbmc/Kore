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

package org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonUtils;

public class Player {

    abstract public static class PlayPause extends JsonResponse {
        private PlayPause(String methodName, int itemId, String itemType, int playerId, int speed) {
            addMethodToResponse(methodName);

            ObjectNode itemNode = createObjectNode();
            itemNode.put("id", itemId);
            if (itemType != null)
                itemNode.put("type", itemType);
            addDataToResponse("item", itemNode);

            itemNode = createObjectNode();
            itemNode.put("playerid", playerId);
            itemNode.put("speed", speed);
            addDataToResponse("player", itemNode);

            addParameterToResponse("sender", "xbmc");
        }
    }

    /**
     * JSON response for Player.OnSpeedChanged notification
     *
     * Example:
     * Answer: {"jsonrpc":"2.0","method":"Player.OnSpeedChanged","params":{"data":{"item":{"id":94,"type":"song"},"player":{"playerid":0,"speed":0}},"sender":"xbmc"}}
     */
    public static class OnSpeedChanged extends PlayPause {
        public final static String METHOD_NAME = "Player.OnSpeedChanged";

        public OnSpeedChanged(int itemId, String itemType, int playerId, int speed) {
            super(METHOD_NAME, itemId, itemType, playerId, speed);
        }
    }

    /**
     * JSON response for Player.OnPause notification
     *
     * Example:
     * Answer: {"jsonrpc":"2.0","method":"Player.OnPause","params":{"data":{"item":{"id":94,"type":"song"},"player":{"playerid":0,"speed":0}},"sender":"xbmc"}}
     */
    public static class OnPause extends PlayPause {
        public final static String METHOD_NAME = "Player.OnPause";

        public OnPause(int itemId, String itemType, int playerId, int speed) {
            super(METHOD_NAME, itemId, itemType, playerId, speed);
        }
    }

    /**
     * JSON response for Player.OnPlay notification
     *
     * Example:
     * Answer:  {"jsonrpc":"2.0","method":"Player.OnPlay","params":{"data":{"item":{"id":1580,"type":"song"},"player":{"playerid":0,"speed":1}},"sender":"xbmc"}}
     */
    public static class OnPlay extends PlayPause {
        public final static String METHOD_NAME = "Player.OnPlay";

        public OnPlay(int itemId, String itemType, int playerId, int speed) {
            super(METHOD_NAME, itemId, itemType, playerId, speed);
        }
    }

    /**
     * JSON response for Player.OnStop notification
     *
     * Example:
     * {"jsonrpc":"2.0","method":"Player.OnStop","params":{"data":{"end":false,"item":{"id":14765,"type":"song"}},"sender":"xbmc"}}
     */
    public static class OnStop extends JsonResponse {
        public final static String METHOD_NAME = "Player.OnStop";

        public OnStop(int itemId, String itemType, boolean ended) {
            super();
            addMethodToResponse(METHOD_NAME);

            addDataToResponse("end", false);

            ObjectNode itemNode = createObjectNode();
            itemNode.put("id", itemId);
            itemNode.put("type", itemType);
            addDataToResponse("item", itemNode);

            addParameterToResponse("sender", "xbmc");
        }
    }

    /**
     * JSON response for Player.OnPropertyChanged notification
     *
     * Example:
     * {"jsonrpc":"2.0","method":"Player.OnPropertyChanged","params":{"data":{"player":{"playerid":0},"property":{"repeat":"all"}},"sender":"xbmc"}}
     */
    public static class OnPropertyChanged extends JsonResponse {
        public final static String METHOD_NAME = "Player.OnPropertyChanged";

        public OnPropertyChanged(String repeatType, Boolean shuffled, int playerId) {
            super();
            addMethodToResponse(METHOD_NAME);

            ObjectNode playerIdNode = createObjectNode();
            playerIdNode.put("playerid", playerId);
            addDataToResponse("player", playerIdNode);

            if (repeatType != null) {
                ObjectNode repeatNode = createObjectNode();
                repeatNode.put("repeat", repeatType);
                addDataToResponse("property", repeatNode);
            }

            if (shuffled != null) {
                ObjectNode repeatNode = createObjectNode();
                repeatNode.put("shuffled", shuffled);
                addDataToResponse("property", repeatNode);
            }

            addParameterToResponse("sender", "xbmc");
        }
    }

    /**
     * JSON response for Player.OnSeek notification
     *
     * Example:
     * {"jsonrpc":"2.0","method":"Player.OnSeek", "params":{ "data":{"item":{ "id":127,"type":"episode" },"player":{ "playerid":1,"seekoffset":{ "hours":0,"milliseconds":0, "minutes":0,"seconds":-14 },"speed":0, "time":{"hours":0, "milliseconds":0,"minutes":0, "seconds":2} }},"sender":"xbmc" }}
     */
    public static class OnSeek extends JsonResponse {
        public final static String METHOD_NAME = "Player.OnSeek";

        public OnSeek(int itemId, String type, int playerId, int speed, long seekOffsetSecs, long timeSecs) {
            super();
            addMethodToResponse(METHOD_NAME);

            ObjectNode itemNode = createObjectNode();
            itemNode.put("id", itemId);
            itemNode.put("type", type);
            addDataToResponse("item", itemNode);

            ObjectNode playerNode = createObjectNode();
            playerNode.put("playerid", playerId);
            playerNode.set("seekoffset", JsonUtils.createTimeNode(createObjectNode(), seekOffsetSecs));
            playerNode.set("time", JsonUtils.createTimeNode(createObjectNode(), timeSecs));
            playerNode.put("speed", speed);
            addDataToResponse("player", playerNode);

            addParameterToResponse("sender", "xbmc");
        }
    }

    /**
     * JSON response for Player.OnAVStart notification
     *
     * Example:
     * {"jsonrpc":"2.0","method":"Player.OnAVStart",
     *  "params":{"data":{
     *              "item":{"id":1502,"type":"song"},
     *              "player":{"playerid":0,"speed":1}},
     *              "sender":"xbmc"}}
     */
    public static class OnAVStart extends PlayPause {
        public final static String METHOD_NAME = "Player.OnAVStart";

        public OnAVStart(int itemId, String itemType, int playerId, int speed) {
            super(METHOD_NAME, itemId, itemType, playerId, speed);
        }
    }
}
