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
package org.xbmc.kore.jsonrpc;


import android.os.Handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.type.ApiParameter;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;

/**
 * Abstract class base of all the JSON RPC API calls
 *
 * Every subclass represents a method on the JSON RPC API.
 *
 * Each subclass should implement constructors to represent each of the API call variations, and
 * call this class {@link #execute(HostConnection, ApiCallback, android.os.Handler) execute()}  to send
 * the call to the server.
 *
 * This class is a template which should be typed with the return type of specific the method call.
 */
public abstract class ApiMethod<T> {
	private static final String TAG = LogUtils.makeLogTag(ApiMethod.class);

	public static final String RESULT_NODE = "result";
	public static final String ERROR_NODE = "error";
	public static final String ID_NODE = "id";
	public static final String METHOD_NODE = "method";
	public static final String PARAMS_NODE = "params";

	/**
	 * Id of the method call. Autoincremented for each method call
	 */
	private static int lastId = 0;
	protected final int id;

	protected static final ObjectMapper objectMapper = new ObjectMapper();
	/**
	 * Json object that will be used to generate the json representation of the current method call
	 */
	protected final ObjectNode jsonRequest;

	/**
	 * Constructor, sets up the necessary items to make the call later
	 */
	public ApiMethod() {
		this(true);
	}

	/**
	 * Constructor, sets up the necessary items to make the call later
	 */
	public ApiMethod(boolean sendId) {
		// Create the rpc request object with the common fields according to JSON RPC spec
		jsonRequest = objectMapper.createObjectNode();
		jsonRequest.put("jsonrpc", "2.0");
		jsonRequest.put(METHOD_NODE, getMethodName());

		if(sendId) {
			synchronized (this) {
				this.id = (++lastId % 10000);
			}
			jsonRequest.put(ID_NODE, id);
		}
		else {
			id = -1;
		}
	}

    /**
     * Returns the parameters node of the json request object
     * Creates one if necessary
     * @return Parameters node
     */
    protected ObjectNode getParametersNode() {
        ObjectNode params;
        if (jsonRequest.has(PARAMS_NODE)) {
            params = (ObjectNode)jsonRequest.get(PARAMS_NODE);
        } else {
            params = objectMapper.createObjectNode();
            jsonRequest.set(PARAMS_NODE, params);
        }

        return params;
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, int value) {
        getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, String value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }


    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, Integer value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, Double value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, boolean value) {
        getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param values Values to add
     */
    protected void addParameterToRequest(String parameter, String[] values) {
        if (values != null) {
            final ArrayNode arrayNode = objectMapper.createArrayNode();
			for (String value : values) {
				arrayNode.add(value);
			}
            getParametersNode().set(parameter, arrayNode);
        }
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, ApiParameter value) {
        if (value != null)
            getParametersNode().set(parameter, value.toJsonNode());
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, JsonNode value) {
        if (value != null)
            getParametersNode().set(parameter, value);
    }

    /**
	 * Returns the id to identify the current method call.
	 * An id is generated for each object that is created.
	 * @return Method call id
	 */
	public int getId() {
		return id;
	}

    /**
	 * Returns the string json representation of the current method.
	 * @return Json string representation of the current method
	 */
	public String toJsonString() { return jsonRequest.toString(); }

	/**
	 * Returns the json object representation of the current method.
	 * @return JsonObject representation of the current method
	 */
	public ObjectNode toJsonObject() { return jsonRequest; }

//	/**
//	 * Calls the method represented by this object on the server.
//	 * This call is always asynchronous. The results will be posted, through the callback parameter,
//	 * on the same thread that is calling this method.
//	 * Note: The current thread must have a Looper prepared, otherwise this will fail because we
//	 * try to get handler on the thread.
//	 *
//	 * @param hostConnection Host connection on which to call the method
//	 * @param callback Callbacks to post the response to
//	 */
//	public void execute(HostConnection hostConnection, ApiCallback<T> callback) {
//		execute(hostConnection, callback, new Handler(Looper.myLooper()));
//	}

	/**
	 * Calls the method represented by this object on the server.
	 * This call is always asynchronous. The results will be posted, through the callback parameter,
	 * on the specified handler.
	 *
	 * @param hostConnection Host connection on which to call the method
	 * @param callback Callbacks to post the response to
	 * @param handler Handler to invoke callbacks on
	 */
	public void execute(HostConnection hostConnection, ApiCallback<T> callback, Handler handler) {
        if (hostConnection != null) {
            hostConnection.execute(this, callback, handler);
        } else {
            callback.onError(ApiException.API_NO_CONNECTION, "No connection specified.");
        }
	}

	/**
	 * Returns the current method name
	 * @return Current method name
	 */
	public abstract String getMethodName();

	/**
	 * Constructs an object of this method's return type from a json response.
	 * This method must be implemented by each subcall to parse the json reponse and create
	 * an return object of the appropriate type for this api method.
	 *
	 * @param jsonResult Json response obtained from a call
	 * @return Result object of the appropriate type for this api method
	 */
	public T resultFromJson(String jsonResult) throws ApiException{
		try {
			return resultFromJson((ObjectNode)objectMapper.readTree(jsonResult));
		} catch (IOException e) {
			throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
		}
	}

	/**
	 * Constructs an object of this method's return type from a json response.
	 * This method must be implemented by each subcall to parse the json reponse and create
	 * an return object of the appropriate type for this api method.
	 *
	 * @param jsonObject Json response obtained from a call
	 * @return Result object of the appropriate type for this api method
	 */
	public abstract T resultFromJson(ObjectNode jsonObject) throws ApiException;

    /**
     * Default callback for methods which the result doesnt matter
     */
    public static <T> ApiCallback<T> getDefaultActionCallback() {

        return new ApiCallback<T>() {
            @Override
            public void onSuccess(T result) {
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Got an error calling a method. Error code: " + errorCode + ", description: " + description);
            }
        };
    }

}
