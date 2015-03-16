/*
 * Copyright 2015 DanhDroid. All rights reserved.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by danhdroid on 3/16/15.
 */
public class SortType {
    public static class Sort implements ApiParameter {
        public static final String METHOD = "method";
        public static final String IGNORE_ARTICLE = "ignorearticle";
        public static final String ORDER = "order";
        public static final String ASCENDING_ORDER = "ascending";
        public static final String DESCENDING_ORDER = "descending";
        protected static final ObjectMapper objectMapper = new ObjectMapper();

        public final boolean ignore_article;
        public final boolean ascending_order;
        public final String sort_method;

        public Sort(String method, boolean ascending, boolean ignore_article) {
            this.sort_method = method;
            this.ascending_order = ascending;
            this.ignore_article = ignore_article;
        }

        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put(IGNORE_ARTICLE, ignore_article);
            node.put(METHOD, sort_method);
            node.put(ORDER, ascending_order ? ASCENDING_ORDER : DESCENDING_ORDER);
            return node;
        }
    }
}
