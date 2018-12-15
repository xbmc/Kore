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
package org.xbmc.kore.jsonrpc.notification;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.ApiNotification;
import org.xbmc.kore.utils.JsonUtils;

/**
 * All Playlist.* notifications
 */
public class Playlist {

    /**
     * Player.OnClear notification
     * Playlist has been cleared
     */
    public static class OnClear extends ApiNotification {
        public static final String  NOTIFICATION_NAME = "Playlist.OnClear";

        public final int playlistId;

        public OnClear(ObjectNode node) {
            super(node);
            ObjectNode dataNode = (ObjectNode)node.get("data");
            playlistId = JsonUtils.intFromJsonNode(dataNode, "playlistid");
        }

        public String getNotificationName() { return NOTIFICATION_NAME; }
    }

    public static class OnAdd extends ApiNotification {
        public static final String NOTIFICATION_NAME = "Playlist.OnAdd";

        public final int playlistId;

        public OnAdd(ObjectNode node) {
            super(node);
            ObjectNode dataNode = (ObjectNode)node.get("data");
            playlistId = JsonUtils.intFromJsonNode(dataNode, "playlistid");
        }

        @Override
        public String getNotificationName() {
            return NOTIFICATION_NAME;
        }
    }

    public static class OnRemove extends ApiNotification {
        public static final String NOTIFICATION_NAME = "Playlist.OnRemove";

        public final int playlistId;

        public OnRemove(ObjectNode node) {
            super(node);
            ObjectNode dataNode = (ObjectNode)node.get("data");
            playlistId = JsonUtils.intFromJsonNode(dataNode, "playlistid");
        }

        @Override
        public String getNotificationName() {
            return NOTIFICATION_NAME;
        }
    }
}
