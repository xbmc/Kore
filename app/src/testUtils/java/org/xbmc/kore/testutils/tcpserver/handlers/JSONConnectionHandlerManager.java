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

package org.xbmc.kore.testutils.tcpserver.handlers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.MockTcpServer;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import static org.xbmc.kore.jsonrpc.ApiMethod.ID_NODE;
import static org.xbmc.kore.jsonrpc.ApiMethod.METHOD_NODE;

public class JSONConnectionHandlerManager implements MockTcpServer.TcpServerConnectionHandler {
    public static final String TAG = LogUtils.makeLogTag(JSONConnectionHandlerManager.class);

    private HashMap<String, ConnectionHandler> handlersByType = new HashMap<>();
    private HashSet<ConnectionHandler> handlers = new HashSet<>();
    private StringBuffer buffer = new StringBuffer();
    private int amountOfOpenBrackets = 0;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private int responseCount;

    private HashMap<String, ArrayList<JsonResponse>> clientResponses = new HashMap<>();

    public interface ConnectionHandler {
        /**
         * Used to determine which methods the handler implements
         * @return list of JSON method names
         */
        String[] getType();

        /**
         * Used to get the response from a handler implementing the requested
         * method.
         * @param method requested method
         * @param jsonRequest json node containing the original request
         * @return {@link JsonResponse} that should be sent to the client
         */
        ArrayList<JsonResponse> getResponse(String method, ObjectNode jsonRequest);

        /**
         * Used to get any notifications from the handler.
         * @return {@link JsonResponse} that should be sent to the client
         */
        ArrayList<JsonResponse> getNotification();

        /**
         * Should set the state of the handler to its initial state
         */
        void reset();
    }

    public void addHandler(ConnectionHandler handler) throws Exception {
        for(String type : handler.getType()) {
            handlersByType.put(type, handler);
        }
        handlers.add(handler);
    }

    @Override
    public void processInput(char c) {
        buffer.append(c);
        if ( c == '{' ) {
            amountOfOpenBrackets++;
        } else if ( c == '}' ) {
            amountOfOpenBrackets--;
        }

        if ( amountOfOpenBrackets == 0 ) {
            String input = buffer.toString();
            buffer = new StringBuffer();
            processJSONInput(input);
        }
    }

    private void processJSONInput(String input) {
        try {
            JsonParser parser = objectMapper.getFactory().createParser(input);
            ObjectNode jsonRequest = objectMapper.readTree(parser);

            int methodId = jsonRequest.get(ID_NODE).asInt();
            String method = jsonRequest.get(METHOD_NODE).asText();
            ConnectionHandler connectionHandler = handlersByType.get(method);
            if ( connectionHandler != null ) {
                ArrayList<JsonResponse> responses = connectionHandler.getResponse(method, jsonRequest);
                if (responses != null) {
                    addResponse(methodId, responses);
                }
            }
        } catch (IOException e) {
            LogUtils.LOGE(getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public String getResponse() {
        StringBuffer stringBuffer = new StringBuffer();
        
        //Handle responses
        Collection<ArrayList<JsonResponse>> jsonResponses = clientResponses.values();
        for(ArrayList<JsonResponse> arrayList : jsonResponses) {
            for (JsonResponse response : arrayList) {
                stringBuffer.append(response.toJsonString() + "\n");
            }
        }
        clientResponses.clear();

        //Handle notifications
        for(ConnectionHandler handler : handlers) {
            ArrayList<JsonResponse> jsonNotifications = handler.getNotification();
            if (jsonNotifications != null) {
                for (JsonResponse jsonResponse : jsonNotifications) {
                    stringBuffer.append(jsonResponse.toJsonString() +"\n");
                }
            }
        }
        responseCount++;
        return stringBuffer.toString();
    }

    /**
     * Waits until at least one response has been processed before returning
     */
    public void waitForNextResponse(long timeOutMillis) throws TimeoutException {
        responseCount = 0;
        while ((responseCount < 2) && (timeOutMillis > 0)) {
            try {
                Thread.sleep(500);
                timeOutMillis -= 500;
            } catch (InterruptedException e) {

            }
        }
        if (timeOutMillis < 0)
            throw new TimeoutException();
    }

    private void addResponse(int id, ArrayList<JsonResponse> jsonResponses) {
        ArrayList<JsonResponse> responses = clientResponses.get(String.valueOf(id));
        if (responses == null) {
            responses = new ArrayList<>();
            clientResponses.put(String.valueOf(id), responses);
        }
        responses.addAll(jsonResponses);
    }
}
