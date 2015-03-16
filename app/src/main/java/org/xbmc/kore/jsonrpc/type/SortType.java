package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by danhdroid on 3/14/15.
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
