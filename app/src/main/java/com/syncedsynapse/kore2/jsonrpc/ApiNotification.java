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
package com.syncedsynapse.kore2.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syncedsynapse.kore2.jsonrpc.notification.Player;

/**
 * Abstract class, based of all the JSON RPC notifications
 *
 * Each specific notification should be a subclass of this.
 */
public abstract class ApiNotification {
    protected static final String METHOD_NODE = "method";
    protected static final String PARAMS_NODE = "params";

    public final String sender;

    /**
     * Constructor from a notification node (starting on "params" node)
     * @param node node
     */
    public ApiNotification(ObjectNode node) {
        sender = node.get("sender").textValue();
    }

    /**
     * Returns this notification name
     */
    public abstract String getNotificationName();

    /**
     * Returns a specific notification present in the Json Node
     *
     * @param node Json node with notification
     * @return Specific notification object
     */
    public static ApiNotification notificationFromJsonNode(JsonNode node) {
        String method = node.get(METHOD_NODE).asText();
        ObjectNode params = (ObjectNode)node.get(PARAMS_NODE);

        ApiNotification result = null;
        if (method.equals(Player.OnPause.NOTIFICATION_NAME)) {
            result = new Player.OnPause(params);
        } else if (method.equals(Player.OnPlay.NOTIFICATION_NAME)) {
            result = new Player.OnPlay(params);
        } else if (method.equals(Player.OnSeek.NOTIFICATION_NAME)) {
            result = new Player.OnSeek(params);
        } else if (method.equals(Player.OnSpeedChanged.NOTIFICATION_NAME)) {
            result = new Player.OnSpeedChanged(params);
        } else if (method.equals(Player.OnStop.NOTIFICATION_NAME)) {
            result = new Player.OnStop(params);
        }

        return result;
    }
}
