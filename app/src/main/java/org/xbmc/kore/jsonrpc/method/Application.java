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
package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.utils.JsonUtils;

/**
 * All JSON RPC methods in Application.*
 */
public class Application {

    /**
     * Quit application
     */
    public static final class Quit extends ApiMethod<String> {
        public final static String METHOD_NAME = "Application.Quit";

        /**
         * Quit application
         */
        public Quit() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Set the current volume
     */
    public static final class SetVolume extends ApiMethod<Integer> {
        public final static String METHOD_NAME = "Application.SetVolume";

        /**
         * Increment or decrement the volume
         * @param volume String enum in {@link org.xbmc.kore.jsonrpc.type.GlobalType.IncrementDecrement}
         */
        public SetVolume(String volume) {
            super();
            addParameterToRequest("volume", volume);
        }

        /**
         * Set the volume
         * @param volume volume between 0 and 100
         */
        public SetVolume(int volume) {
            super();
            addParameterToRequest("volume", volume);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public Integer resultFromJson(ObjectNode jsonObject) throws ApiException {
            return JsonUtils.intFromJsonNode(jsonObject, RESULT_NODE);
        }
    }

    /**
     * Toggle mute/unmute
     */
    public static final class SetMute extends ApiMethod<Boolean> {
        public final static String METHOD_NAME = "Application.SetMute";

        /**
         * Toggle mute/unmute
         */
        public SetMute() {
            super();
            addParameterToRequest("mute", "toggle");
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public Boolean resultFromJson(ObjectNode jsonObject) throws ApiException {
            return JsonUtils.booleanFromJsonNode(jsonObject, RESULT_NODE);
        }
    }

    /**
     * Retrieves the values of the given properties.
     */
    public static class GetProperties extends ApiMethod<ApplicationType.PropertyValue> {
        public final static String METHOD_NAME = "Application.GetProperties";

        /**
         * Properties
         */
        public final static String VOLUME = "volume";
        public final static String MUTED = "muted";
        public final static String NAME = "name";
        public final static String VERSION = "version";

        /**
         * Retrieves the values of the given properties.
         * @param properties  See this class constants.
         */
        public GetProperties(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public ApplicationType.PropertyValue resultFromJson(ObjectNode jsonObject) throws ApiException {
            return new ApplicationType.PropertyValue(jsonObject.get(RESULT_NODE));
        }
    }
}