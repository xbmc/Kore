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

/**
 * System.* notifications
 */
public class System {

    /**
     * System.OnQuit notification
     * XBMC will be closed
     */
    public static class OnQuit extends ApiNotification {
        public static final String  NOTIFICATION_NAME = "System.OnQuit";

        public OnQuit(ObjectNode node) {
            super(node);
        }

        public String getNotificationName() { return NOTIFICATION_NAME; }
    }

    /**
     * System.OnRestart notification
     * The system will be restarted.
     */
    public static class OnRestart extends ApiNotification {
        public static final String  NOTIFICATION_NAME = "System.OnRestart";

        public OnRestart(ObjectNode node) {
            super(node);
        }

        public String getNotificationName() { return NOTIFICATION_NAME; }
    }

    /**
     * System.OnSleep notification
     * The system will be suspended.
     */
    public static class OnSleep extends ApiNotification {
        public static final String  NOTIFICATION_NAME = "System.OnSleep";

        public OnSleep(ObjectNode node) {
            super(node);
        }

        public String getNotificationName() { return NOTIFICATION_NAME; }
    }
}
