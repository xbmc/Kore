/*
 * Copyright 2017 XBMC Foundation. All rights reserved.
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
 * Types from Favourite.*
 */
public class FavouriteType {

    /**
     * Favourite.Type
     */
    public interface FavouriteTypeEnum {
        String MEDIA = "media";
        String WINDOW = "window";
        String SCRIPT = "script";
        String UNKNOWN = "unknown";
    }

    /**
     * Favourite.Details.Favourite
     */
    public static class DetailsFavourite {
        public static final String PATH = "path";
        public static final String THUMBNAIL = "thumbnail";
        public static final String TITLE = "title";
        public static final String TYPE = "type";
        public static final String WINDOW = "window";
        public static final String WINDOW_PARAMETER = "windowparameter";

        public final String thumbnail;
        public final String path;
        public final String title;
        public final String type;
        public final String window;
        public final String windowParameter;

        public DetailsFavourite(JsonNode node) {
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
            path = JsonUtils.stringFromJsonNode(node, PATH);
            title = JsonUtils.stringFromJsonNode(node, TITLE);
            type = JsonUtils.stringFromJsonNode(node, TYPE, FavouriteTypeEnum.MEDIA);
            window = JsonUtils.stringFromJsonNode(node, WINDOW);
            windowParameter = JsonUtils.stringFromJsonNode(node, WINDOW_PARAMETER);
        }
    }
}
