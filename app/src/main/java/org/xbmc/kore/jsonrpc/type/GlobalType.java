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
package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import org.xbmc.kore.utils.JsonUtils;

/**
 * Types from Global.*
 */
public class GlobalType {

    /**
     * Global.Time
     */
    public static class Time {
        public static final String HOURS = "hours";
        public static final String MILLISECONDS = "milliseconds";
        public static final String MINUTES = "minutes";
        public static final String SECONDS = "seconds";

        public final int hours;
        public final int milliseconds;
        public final int minutes;
        public final int seconds;

        public Time(JsonNode node) {
            hours = JsonUtils.intFromJsonNode(node, HOURS, 0);
            milliseconds = JsonUtils.intFromJsonNode(node, MILLISECONDS, 0);
            minutes = JsonUtils.intFromJsonNode(node, MINUTES, 0);
            seconds = JsonUtils.intFromJsonNode(node, SECONDS, 0);
        }

        /**
         * Returns the seconds from midnight that this time object represents
         * @return Seconds from midnight
         */
        public int ToSeconds() {
            return hours * 3600 + minutes * 60 + seconds;
        }
    }

    /**
     * Global.IncrementDecrement
     */
    public interface IncrementDecrement {
        public final String INCREMENT = "increment";
        public final String DECREMENT = "decrement";
    }

}

