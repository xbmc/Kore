/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.utils.LogUtils;

public abstract class JsonResponse {
    private final String TAG = LogUtils.makeLogTag(JsonResponse.class);

    private final ObjectNode jsonResponse;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RESULT_NODE = "result";
    private static final String PARAMS_NODE = "params";
    private static final String METHOD_NODE = "method";
    private static final String DATA_NODE = "data";
    private static final String ID_NODE = "id";
    private static final String JSONRPC_NODE = "jsonrpc";

    public enum TYPE {
        OBJECT,
        ARRAY
    };

    public JsonResponse() {
        jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put(JSONRPC_NODE, "2.0");
    }

    public JsonResponse(int id) {
        this();
        jsonResponse.put(ID_NODE, id);
    }

    protected ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    protected ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    /**
     * Returns the node used to hold the result. First call will create the
     * result node for the given type
     * @param type that result node should be when first created
     * @return result node
     */
    protected JsonNode getResultNode(TYPE type) {
        JsonNode result;
        if(jsonResponse.has(RESULT_NODE)) {
            result = jsonResponse.get(RESULT_NODE);
            if( result.isArray() && type != TYPE.ARRAY ) {
                LogUtils.LOGE(TAG, "requested result node of type Object but response contains result node of type Array");
                return null;
            }
        } else {
            switch (type) {
                case ARRAY:
                    result = objectMapper.createArrayNode();
                    break;
                case OBJECT:
                default:
                    result = objectMapper.createObjectNode();
                    break;
            }
            jsonResponse.set(RESULT_NODE, result);
        }

        return result;
    }

    /**
     * Returns the parameters node of the json request object
     * Creates one if necessary
     * @return Parameters node
     */
    private ObjectNode getParametersNode() {
        ObjectNode params;
        if (jsonResponse.has(PARAMS_NODE)) {
            params = (ObjectNode)jsonResponse.get(PARAMS_NODE);
        } else {
            params = objectMapper.createObjectNode();
            jsonResponse.set(PARAMS_NODE, params);
        }

        return params;
    }

    private ObjectNode getDataNode() {
        ObjectNode data = null;
        if (jsonResponse.has(PARAMS_NODE)) {
            ObjectNode params = (ObjectNode)jsonResponse.get(PARAMS_NODE);
            if(params.has(DATA_NODE)) {
                data = (ObjectNode) params.get(DATA_NODE);
            }
        }

        if ( data == null ) {
            data = objectMapper.createObjectNode();
            ObjectNode params = getParametersNode();
            params.set(DATA_NODE, data);
        }

        return data;
    }

    protected void setResultToResponse(boolean value) {
        jsonResponse.put(RESULT_NODE, value);
    }

    protected void setResultToResponse(int value) {
        jsonResponse.put(RESULT_NODE, value);
    }

    protected void setResultToResponse(String value) {
        jsonResponse.put(RESULT_NODE, value);
    }

    /**
     * Adds the value to the array in node with the given key.
     * If the array does not exist it will be created
     * and added.
     * @param node ObjectNode that should contain an entry with key with an array as value
     * @param key the key of the item in ObjectNode that should hold the array
     * @param value the value to be added to the array
     */
    protected void addToArrayNode(ObjectNode node, String key, String value) {
        JsonNode jsonNode = node.get(key);
        if (jsonNode == null) {
            jsonNode = objectMapper.createArrayNode();
            node.set(key, jsonNode);
        }

        if (jsonNode.isArray()) {
            ((ArrayNode) jsonNode).add(value);
        } else {
            LogUtils.LOGE(TAG, "JsonNode at key: " + key + " not of type ArrayNode." );
        }
    }

    protected void addToArrayNode(ObjectNode node, String key, ObjectNode value) {
        JsonNode jsonNode = node.get(key);
        if (jsonNode == null) {
            jsonNode = objectMapper.createArrayNode();
            node.set(key, jsonNode);
        }

        if (jsonNode.isArray()) {
            ((ArrayNode) jsonNode).add(value);
        } else {
            LogUtils.LOGE(TAG, "JsonNode at key: " + key + " not of type ArrayNode." );
        }
    }

    protected void addDataToResponse(String parameter, boolean value) {
        getDataNode().put(parameter, value);
    }

    protected void addDataToResponse(String parameter, int value) {
        getDataNode().put(parameter, value);
    }

    protected void addParameterToResponse(String parameter, String value) {
        getParametersNode().put(parameter, value);
    }

    protected void addMethodToResponse(String method) {
        jsonResponse.put(METHOD_NODE, method);
    }

    public ObjectNode getResponseNode() {
        return jsonResponse;
    }

    public String toJsonString() {
        return jsonResponse.toString();
    }
}
