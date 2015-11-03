package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.PVRType;

import java.util.ArrayList;
import java.util.List;

/**
 * All JSON RPC methods in PVR.*
 */
public class PVR {

    /**
     * Retrieves the channel groups for the specified type
     */
    public static class GetChannelGroups extends ApiMethod<List<PVRType.DetailsChannelGroup>> {
        public final static String METHOD_NAME = "PVR.GetChannelGroups";
        private final static String LIST_NODE = "channelgroups";

        /**
         * Retrieves the channel groups for the specified type
         *
         * @param channeltype Channel type. See {@link org.xbmc.kore.jsonrpc.type.PVRType.ChannelType}
         */
        public GetChannelGroups(String channeltype) {
            super();
            addParameterToRequest("channeltype", channeltype);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<PVRType.DetailsChannelGroup> resultFromJson(ObjectNode jsonObject)
              throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                  (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<>(0);
            }
            ArrayList<PVRType.DetailsChannelGroup> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new PVRType.DetailsChannelGroup(item));
            }

            return result;
        }
    }

    /**
     * Retrieves the channel list
     */
    public static class GetChannels extends ApiMethod<List<PVRType.DetailsChannel>> {
        public final static String METHOD_NAME = "PVR.GetChannels";
        private final static String LIST_NODE = "channels";

        /**
         * Retrieves the channel list
         *
         * @param channelgroupid Group id, required
         * @param properties Properties to retrieve. See {@link PVRType.FieldsChannel} for a list of
         *                   accepted values
         */
        public GetChannels(int channelgroupid, String... properties) {
            super();
            addParameterToRequest("channelgroupid", channelgroupid);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<PVRType.DetailsChannel> resultFromJson(ObjectNode jsonObject)
              throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                  (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<>(0);
            }
            ArrayList<PVRType.DetailsChannel> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new PVRType.DetailsChannel(item));
            }

            return result;
        }
    }

    /**
     * Retrieves the program of a specific channel
     */
    public static class GetBroadcasts extends ApiMethod<List<PVRType.DetailsBroadcast>> {
        public final static String METHOD_NAME = "PVR.GetBroadcasts";
        private final static String LIST_NODE = "broadcasts";

        /**
         * Retrieves the program of a specific channel
         *
         * @param channelid Channel id, required
         * @param properties Properties to retrieve. See {@link PVRType.FieldsBroadcast} for a list of
         *                   accepted values
         */
        public GetBroadcasts(int channelid, String... properties) {
            super();
            addParameterToRequest("channelid", channelid);
            addParameterToRequest("properties", properties);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public List<PVRType.DetailsBroadcast> resultFromJson(ObjectNode jsonObject)
              throws ApiException {
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                  (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<>(0);
            }
            ArrayList<PVRType.DetailsBroadcast> result = new ArrayList<>(items.size());

            for (JsonNode item : items) {
                result.add(new PVRType.DetailsBroadcast(item));
            }

            return result;
        }
    }

    /**
     * Toggle recording of a channel
     */
    public static final class Record extends ApiMethod<String> {
        public final static String METHOD_NAME = "PVR.Record";

        /**
         * Records a channel
         */
        public Record(boolean record) {
            super();
            addParameterToRequest("record", record);
        }

        /**
         * Toggle recording of a channel
         */
        public Record() {
            super();
            addParameterToRequest("record", "toggle");
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }


    /**
     * Starts a channel scan
     */
    public static final class Scan extends ApiMethod<String> {
        public final static String METHOD_NAME = "PVR.Shutdown";

        /**
         * Starts a channel scan
         */
        public Scan() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

}
