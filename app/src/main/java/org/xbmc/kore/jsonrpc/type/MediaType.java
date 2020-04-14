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
 * Types from Media.*
 */
public class MediaType {

    /**
     * Media.Artwork
     */
    public static class Artwork {
        public static final String BANNER = "banner";
        public static final String TV_SHOW_BANNER = "tvshow.banner";
        public static final String FANART = "fanart";
        public static final String TV_SHOW_FANART = "tvshow.fanart";
        public static final String POSTER = "poster";
        public static final String TV_SHOW_POSTER = "tvshow.poster";
        public static final String THUMB = "thumb";
        public static final String ALBUM_THUMB = "album.thumb";

        public String banner;
        public String fanart;
        public String poster;
        public String thumb;

        public Artwork(JsonNode node) {
            if (node == null) {
                return;
            }

            banner = JsonUtils.stringFromJsonNode(node, BANNER, null);
            if (banner == null)
                banner = JsonUtils.stringFromJsonNode(node, TV_SHOW_BANNER, null);
            fanart = JsonUtils.stringFromJsonNode(node, FANART, null);
            if (fanart == null)
                poster = JsonUtils.stringFromJsonNode(node, TV_SHOW_FANART, null);
            poster = JsonUtils.stringFromJsonNode(node, POSTER, null);
            if (poster == null)
                poster = JsonUtils.stringFromJsonNode(node, TV_SHOW_POSTER, null);
            thumb = JsonUtils.stringFromJsonNode(node, THUMB, null);
            if (thumb == null)
                thumb = JsonUtils.stringFromJsonNode(node, ALBUM_THUMB, null);
        }
    }

    /**
     * Media.Details.Base
     */
    public static class DetailsBase extends ItemType.DetailsBase {
        public static final String FANART = "fanart";
        public static final String THUMBNAIL = "thumbnail";

        public final String fanart;
        public final String thumbnail;

        /**
         * Constructor from Json node
         * @param node Json node
         */
        public DetailsBase(JsonNode node) {
            super(node);
            fanart = JsonUtils.stringFromJsonNode(node, FANART, null);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL, null);
        }

    }

}