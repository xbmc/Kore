/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.tcpserver.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;

/**
 * Simulates Input JSON-RPC API
 */
public class InputHandler extends ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(InputHandler.class);

    private static final String ACTION = "action";
    private static final String PARAMS_NODE = "params";

    private String action;
    private String methodName;

    @Override
    public String[] getType() {
        return new String[]{Input.ExecuteAction.METHOD_NAME,
                            Input.Back.METHOD_NAME,
                            Input.Up.METHOD_NAME,
                            Input.Down.METHOD_NAME,
                            Input.Left.METHOD_NAME,
                            Input.Right.METHOD_NAME,
                            Input.Select.METHOD_NAME,
        };
    }

    @Override
    public ArrayList<JsonResponse> createResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();

        methodName = method;

        switch (method) {
            case Input.ExecuteAction.METHOD_NAME:
                action = jsonRequest.get(PARAMS_NODE).get(ACTION).asText();
                break;
            case Input.Left.METHOD_NAME:
            case Input.Right.METHOD_NAME:
            case Input.Up.METHOD_NAME:
            case Input.Down.METHOD_NAME:
            case Input.Select.METHOD_NAME:
                // These inputs do not have an action
                break;
            default:
                LogUtils.LOGD(TAG, "method: " + method + ", not implemented");
        }
        return jsonResponses;
    }

    /**
     * Returns the last received action
     * @return
     */
    public String getAction() {
        return action;
    }

    /**
     * Returns the last received method name
     * @return
     */
    public String getMethodName() {
        return methodName;
    }
}
