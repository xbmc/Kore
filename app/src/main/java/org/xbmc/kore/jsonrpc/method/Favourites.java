package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiList;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.FavouriteType;
import org.xbmc.kore.jsonrpc.type.ListType;

import java.util.ArrayList;
import java.util.Collections;

/**
 * All JSON RPC methods in Favourites.*
 */
public class Favourites {

    /**
     * Retrieves the Details of the Favourites.
     */
    public static class GetFavourites extends ApiMethod<ApiList<FavouriteType.DetailsFavourite>> {
        public static final String METHOD_NAME = "Favourites.GetFavourites";
        private static final String LIST_NODE = "favourites";

        /**
         * Default ctor, gets all the properties by default.
         */
        public GetFavourites() {
            addParameterToRequest("properties", new String[]{
                    FavouriteType.DetailsFavourite.WINDOW, FavouriteType.DetailsFavourite.WINDOW_PARAMETER,
                    FavouriteType.DetailsFavourite.THUMBNAIL, FavouriteType.DetailsFavourite.PATH
            });
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public ApiList<FavouriteType.DetailsFavourite> resultFromJson(ObjectNode jsonObject) throws ApiException {
            ListType.LimitsReturned limits = new ListType.LimitsReturned(jsonObject);

            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                    (ArrayNode) resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ApiList<>(Collections.<FavouriteType.DetailsFavourite>emptyList(), limits);
            }
            ArrayList<FavouriteType.DetailsFavourite> result = new ArrayList<>(items.size());
            for (JsonNode item : items) {
                result.add(new FavouriteType.DetailsFavourite(item));
            }
            return new ApiList<>(result, limits);
        }
    }
}
