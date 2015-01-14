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
package com.syncedsynapse.kore2.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncedsynapse.kore2.utils.JsonUtils;

/**
 * Types defined in Application.*
 */
public class ApplicationType {

    /**
     * Application.Property.Value
     */
    public static class PropertyValue {
        public static final String MUTED = "muted";
        public static final String NAME = "name";
        public static final String VERSION = "version";
        public static final String VOLUME = "volume";

        // class members
        public final Boolean muted;
        public final String name;
        public final Version version;
        public final Integer volume;

        /**
         * Contructor
         * @param node JSON object representing a PropertyValue
         */
        public PropertyValue(JsonNode node) {
            muted = JsonUtils.booleanFromJsonNode(node, MUTED, false);
            name = JsonUtils.stringFromJsonNode(node, NAME);
            version = new Version(node.get(VERSION));
            volume = JsonUtils.intFromJsonNode(node, VOLUME, 0);
        }

        /**
         * Version
         */
        public static class Version {
            public static final String MAJOR = "major";
            public static final String MINOR = "minor";
            public static final String REVISION = "revision";
            public static final String TAG = "tag";

            public final Integer major;
            public final Integer minor;
            public final String revision;
            public final String tag;

            /**
             * Constructor
             * @param node JSON object representing a Version
             */
            public Version(JsonNode node) {
                major = JsonUtils.intFromJsonNode(node, MAJOR, 0);
                minor = JsonUtils.intFromJsonNode(node, MINOR, 0);
                revision = JsonUtils.stringFromJsonNode(node, REVISION);
                tag = JsonUtils.stringFromJsonNode(node, TAG);
            }

        }
    }
}
