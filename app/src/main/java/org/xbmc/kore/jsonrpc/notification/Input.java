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
 * Input.* notifications
 */
public class Input {

    /**
     * Input.OnInputRequested
     * The user is requested to provide some information
     */
    public static class OnInputRequested extends ApiNotification {
        public static final String  NOTIFICATION_NAME = "Input.OnInputRequested";

        public static final String DATA_NODE = "data";

        public final String title;
        public final String type;
        public final String value;

        public OnInputRequested(ObjectNode node) {
            super(node);
            ObjectNode dataNode = (ObjectNode)node.get(DATA_NODE);
            title = JsonUtils.stringFromJsonNode(dataNode, "title");
            type = JsonUtils.stringFromJsonNode(dataNode, "type");
            value = JsonUtils.stringFromJsonNode(dataNode, "value");
        }

        public String getNotificationName() { return NOTIFICATION_NAME; }
    }
}
