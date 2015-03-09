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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.utils.JsonUtils;

/**
 * Exception class for errors on JSON API.
 * Some communication exceptions are catched and casted to this type.
 * Response error from the JSON API are also returned as an instance of this exception.
 */
public class ApiException extends Exception {

	/**
	 * We got an invalid JSON response
	 */
	public static final int INVALID_JSON_RESPONSE_FROM_HOST = 0;

	/**
	 * IO Exception while connecting
	 */
	public static final int IO_EXCEPTION_WHILE_CONNECTING = 1;

	/**
	 * IO Exception while sending
	 */
	public static final int IO_EXCEPTION_WHILE_SENDING_REQUEST = 2;

	/**
	 * IO Exception while sending
	 */
	public static final int IO_EXCEPTION_WHILE_READING_RESPONSE = 3;

	/**
	 * HTTP response code unknown/unhandled
	 */
	public static final int HTTP_RESPONSE_CODE_UNKNOWN = 4;

	/**
	 * HTTP response code unknown/unhandled
	 */
	public static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = 5;

	/**
	 * HTTP response code unknown/unhandled
	 */
	public static final int HTTP_RESPONSE_CODE_NOT_FOUND = 6;

	/**
	 * API returned an error
	 */
	public static int API_ERROR = 100;

    /**
     * Attempted to send a method while not connected to host
     */
    public static int API_NO_CONNECTION = 101;

    /**
     * Attempted to execute a method with the same id of another already running
     */
    public static int API_METHOD_WITH_SAME_ID_ALREADY_EXECUTING = 102;

    private int code;

	/**
	 * Constructor
	 * @param code Exception code
	 * @param message Message
	 */
	public ApiException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Construct exception from other exception
	 * @param code Exception code
	 * @param originalException Original exception
	 */
	public ApiException(int code, Exception originalException) {
		super(originalException);
		this.code = code;
	}

	/**
	 * Construct exception from JSON response
	 * @param code Exception code
	 * @param jsonResponse Json response, with an Error node
	 */
	public ApiException(int code, ObjectNode jsonResponse) {
        super((jsonResponse.get(ApiMethod.ERROR_NODE) != null) ?
                JsonUtils.stringFromJsonNode(jsonResponse.get(ApiMethod.ERROR_NODE), "message") :
                "No message returned");
		this.code = code;
	}

	/**
	 * Internal code of the exception
	 * @return Code of the exception
	 */
	public int getCode() {
		return code;
	}
}
