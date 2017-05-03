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
