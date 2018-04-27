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

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;

public class Application {
    /**
     * JSON response for Application.OnVolumeChanged notification
     *
     * Example:
     * Answer:  {"jsonrpc":"2.0","method":"Application.OnVolumeChanged","params":{"data":{"muted":false,"volume":100},"sender":"xbmc"}}
     *
     * @return JSON string
     */
    public static class OnVolumeChanged extends JsonResponse {
        public final static String METHOD_NAME = "Application.OnVolumeChanged";

        public OnVolumeChanged(boolean muteState, int volume) {
            super();
            addMethodToResponse(METHOD_NAME);
            addDataToResponse("volume", volume);
            addDataToResponse("muted", muteState);
            addParameterToResponse("sender", "xbmc");
        }
    }
}
