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
 * Types from Library.*
 */
public class LibraryType {
    /**
     * Enums for Library.Fields.Genre
     */
    public interface FieldsGenre {
        public final String TITLE = "title";
        public final String THUMBNAIL = "thumbnail";

        public final static String[] allValues = new String[]{
                TITLE, THUMBNAIL
        };
    }

    /**
     * Library.Details.Genre
     */
    public static class DetailsGenre extends ItemType.DetailsBase {
        public static final String GENREID = "genreid";
        public static final String THUMBNAIL = "thumbnail";
        public static final String TITLE = "title";

        // class members
        public final Integer genreid;
        public final String thumbnail;
        public final String title;

        /**
         * Constructor
         * @param node Json node
         */
        public DetailsGenre(JsonNode node) {
            super(node);
            genreid = JsonUtils.intFromJsonNode(node, GENREID);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
            title = JsonUtils.stringFromJsonNode(node, TITLE);
        }
    }

}
