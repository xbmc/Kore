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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.AddonType;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON RPC methods in Addons.*
 */
public class Addons {

    /**
     * Executes the given addon with the given parameters (if possible)
     */
    public static final class ExecuteAddon extends ApiMethod<String> {
        public final static String METHOD_NAME = "Addons.ExecuteAddon";

        /**
         * Known addon ids
         */
        public final static String ADDON_SUBTITLES = "script.xbmc.subtitles";

        /**
         * Executes the given addon with the given parameters (if possible)
         */
        public ExecuteAddon(String addonId) {
            super();
            addParameterToRequest("addonid", addonId);
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
     * Gets all available addons
     */
    public static final class GetAddons extends ApiMethod<List<AddonType.Details>> {
        public final static String METHOD_NAME = "Addons.GetAddons";

        private final static String LIST_NODE = "addons";

        /**
         * Gets all available addons
         * @param enabled Whether to get enabled addons
         * @param properties Properties to retrieve. See {AddonType.Fields}
         */
        public GetAddons(boolean enabled, String... properties) {
            super();
            addParameterToRequest("enabled", enabled);
            addParameterToRequest("properties", properties);
        }

        /**
         * Gets all available addons
         * @param properties Properties to retrieve. See {AddonType.Fields}
         */
        public GetAddons(String... properties) {
            super();
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<AddonType.Details> resultFromJson(ObjectNode jsonObject) throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                              (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<AddonType.Details>(0);
            }
            ArrayList<AddonType.Details> result = new ArrayList<AddonType.Details>(items.size());

            for (JsonNode item : items) {
                result.add(new AddonType.Details(item));
            }

            return result;
        }
    }

    /**
     * Gets the details of a specific addon
     */
    public static final class GetAddonDetails extends ApiMethod<AddonType.Details> {
        public final static String METHOD_NAME = "Addons.GetAddonDetails";

        /**
         * Gets the details of a specific addon
         * @param addonid Addon id
         * @param properties Properties to retrieve. See {AddonType.Fields}
         */
        public GetAddonDetails(String addonid, String... properties) {
            super();
            addParameterToRequest("addonid", addonid);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public AddonType.Details resultFromJson(ObjectNode jsonObject) throws ApiException {
            return new AddonType.Details(jsonObject.get(RESULT_NODE).get("addon"));
        }
    }

    /**
     * Enables/Disables a specific addon
     */
    public static final class SetAddonEnabled extends ApiMethod<String> {
        public final static String METHOD_NAME = "Addons.SetAddonEnabled";

        /**
         * Enables/Disables a specific addon
         */
        public SetAddonEnabled(String addonId, boolean enabled) {
            super();
            addParameterToRequest("addonid", addonId);
            addParameterToRequest("enabled", enabled);
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

}
