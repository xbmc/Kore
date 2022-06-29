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
 * All Player.* notifications
 */
public class Application {

    /**
     * Player.OnSpeedChanged notification
     * Speed of the playback of a media item has been changed. If there is no ID available extra information will be provided.
     * be provided.
     */
    public static class OnVolumeChanged extends ApiNotification {
        public static final String  NOTIFICATION_NAME = "Application.OnVolumeChanged";

        public final int volume;
        public final boolean muted;

        public OnVolumeChanged(ObjectNode node) {
            super(node);
            ObjectNode dataNode = (ObjectNode)node.get("data");
            volume = JsonUtils.intFromJsonNode(dataNode, "volume");
            muted = JsonUtils.booleanFromJsonNode(dataNode, "muted");
        }

        public String getNotificationName() { return NOTIFICATION_NAME; }
    }
}
