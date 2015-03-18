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

/**
 * Types from Item.*
 */
public class ItemType {
    /**
     * Item.Details.Base
     */
    public static class DetailsBase {
        public static final String LABEL = "label";

        public final String label;

        public DetailsBase(JsonNode node) {
            JsonNode labelNode = node.get(LABEL);
            if (labelNode != null)
                label = labelNode.asText();
            else
                label = null;
        }
    }

    /**
     * Item.Details.Source
     */
    public static class Source extends DetailsBase {
        public static final String FILE = "file";

        public final String file;

        public Source(JsonNode node) {
            super(node);
            JsonNode fileNode = node.get(FILE);
            if (fileNode != null)
                file = fileNode.asText();
            else
                file = null;
        }
    }
}
