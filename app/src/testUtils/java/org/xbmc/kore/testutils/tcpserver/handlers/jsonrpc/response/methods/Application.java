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

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;

/**
 * Serverside JSON RPC responses in Application.*
 */
public class Application {

    /**
     * JSON response for Application.SetMute request
     *
     * Example:
     * Query:             {"jsonrpc":"2.0","method":"Application.SetMute","id":1,"params":{"mute":"toggle"}}
     * Answer: muted:     {"id":1,"jsonrpc":"2.0","result":false}
     *         not muted: {"id":1,"jsonrpc":"2.0","result":true}
     *
     * @return JSON string
     */
    public static class SetMute extends JsonResponse {
        public final static String METHOD_NAME = "Application.SetMute";

        public SetMute(int id, boolean muteState) {
            super(id);
            setResultToResponse(muteState);
        }
    }

    /**
     * JSON response for GetProperties requests
     *
     * Example:
     * Query: {"jsonrpc":"2.0","method":"Application.GetProperties","id":1,"params":{"properties":["muted"]}}
     * Answer: {"id":1,"jsonrpc":"2.0","result":{"muted":true}}
     *
     * @return JSON string
     */
    public static class GetProperties extends JsonResponse {
        public final static String METHOD_NAME = "Application.GetProperties";

        public final static String MUTED = "muted";
        public final static String VOLUME = "volume";

        private ObjectNode node = null;

        public GetProperties(int id) {
            super(id);
        }

        public void addMuteState(boolean muteState) {
            node = (ObjectNode) getResultNode(TYPE.OBJECT);
            node.put(MUTED, muteState);
        }

        public void addVolume(int volume) {
            node = (ObjectNode) getResultNode(TYPE.OBJECT);
            node.put(VOLUME, volume);
        }
    }

    /**
     * JSON response for Application.SetVolume request
     *
     * Examples:
     * Query:             {"jsonrpc":"2.0","method":"Application.SetVolume","id":1,"params":{"volume":100}}
     * Answer:            {"id":1,"jsonrpc":"2.0","result":100}
     *
     * Query:             {"jsonrpc":"2.0","method":"Application.SetVolume","id":1,"params":{"volume":"decrement"}}
     * Answer:            {"id":1,"jsonrpc":"2.0","result":99}
     *
     * @return JSON string
     */
    public static class SetVolume extends JsonResponse {
        public final static String METHOD_NAME = "Application.SetVolume";

        public SetVolume(int id, int volume) {
            super(id);
            setResultToResponse(volume);
        }
    }
}
