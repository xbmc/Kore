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
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.xbmc.kore.jsonrpc.ApiMethod.ID_NODE;
import static org.xbmc.kore.jsonrpc.ApiMethod.METHOD_NODE;

public class JSONConnectionHandlerManager implements MockTcpServer.TcpServerConnectionHandler {
    public static final String TAG = LogUtils.makeLogTag(JSONConnectionHandlerManager.class);

    private final HashMap<String, ConnectionHandler> handlersByType = new HashMap<>();
    private int amountOfOpenBrackets = 0;
    private final ObjectMapper objectMapper = new ObjectMapper();

    //HashMap used to prevent adding duplicate responses for the same methodId when invoking
    //a handler multiple times.
    private final HashMap<String, ArrayList<JsonResponse>> clientResponses = new HashMap<>();

    private final HashMap<String, MethodPendingState> methodIdsHandled = new HashMap<>();
    private final HashSet<String> notificationsHandled = new HashSet<>();

    public void addHandler(ConnectionHandler handler) {
        synchronized (handlersByType) {
            for (String type : handler.getType()) {
                handlersByType.put(type, handler);
            }
        }
    }

    @Override
    public void processInput(Socket socket) {
        StringBuilder stringBuffer = new StringBuilder();

        try {
            InputStreamReader in = new InputStreamReader(socket.getInputStream());
            int i;
            while (!socket.isClosed() && (i = in.read()) != -1) {
                stringBuffer.append((char) i);
                if (isEndOfJSONStringReached((char) i)) {
                    processJSONInput(stringBuffer.toString());
                    stringBuffer = new StringBuilder();
                }
            }
        } catch (SocketException e) {
            // Socket closed
        } catch (IOException e) {
            LogUtils.LOGD(TAG, "processInput: error reading from socket: " + socket +
                               ", buffer holds: " + stringBuffer);
            LogUtils.LOGE(TAG, e.getMessage());
        }
    }

    /**
     * Processes JSON input on individual characters.
     * Each iteration should start with an opening accolade { and
     * end with a closing accolade to indicate a complete JSON string has been
     * fully processed.
     * @param c
     * @return true if a JSON string was fully processed, false otherwise
     */
    private boolean isEndOfJSONStringReached(char c) {
        //We simply assume well formed JSON input so it should always start with
        //a {. If we need to filter out other input we need to add an additional check
        //to detect the first opening accolade.
        if ( c == '{' ) {
            amountOfOpenBrackets++;
        } else if ( c == '}' ) {
            amountOfOpenBrackets--;
        }

        return amountOfOpenBrackets == 0;
    }

    private void processJSONInput(String input) {
        try {
            synchronized (clientResponses) {
                LogUtils.LOGD(TAG, "processJSONInput: " + input);
                JsonParser parser = objectMapper.getFactory().createParser(input);
                ObjectNode jsonRequest = objectMapper.readTree(parser);

                int methodId = jsonRequest.get(ID_NODE).asInt();
                String method = jsonRequest.get(METHOD_NODE).asText();

                methodIdsHandled.put(String.valueOf(methodId), new MethodPendingState(method));

                if (clientResponses.get(String.valueOf(methodId)) != null)
                    return;

                ConnectionHandler connectionHandler = handlersByType.get(method);
                if (connectionHandler != null) {
                    ArrayList<JsonResponse> responses = connectionHandler.getResponse(method, jsonRequest);
                    if (responses != null) {
                        clientResponses.put(String.valueOf(methodId), responses);
                    }
                }

                parser.close();
            }
        } catch (IOException e) {
            LogUtils.LOGD(TAG, "processJSONInput: error parsing: " + input);
            LogUtils.LOGE(TAG, e.getMessage());
        }
    }

    @Override
    public String getResponse() {
        StringBuilder stringBuilder = new StringBuilder();

        //Handle client responses
        synchronized (clientResponses) {
            for(Map.Entry<String, ArrayList <JsonResponse>> clientResponse : clientResponses.entrySet()) {
                for (JsonResponse jsonResponse : clientResponse.getValue()) {
                    LogUtils.LOGD(TAG, "sending response: " + jsonResponse.toJsonString());
                    try {
                        MethodPendingState methodPending = methodIdsHandled.get(jsonResponse.getId());
                        methodPending.handled = true;
                        stringBuilder.append(jsonResponse.toJsonString()).append("\n");
                    } catch (Exception e) {
                        LogUtils.LOGD(TAG, "getResponse: Error handling response: " + jsonResponse.toJsonString());
                        LogUtils.LOGW(TAG, "getResponse: " + e);
                    }
                }
            }
            clientResponses.clear();
        }

        synchronized (handlersByType) {
            //Build a new set to make sure we only handle each handler once, even if it handles
            //multiple types.
            HashSet<ConnectionHandler> uniqueHandlers = new HashSet<>(handlersByType.values());

            //Handle notifications
            for (ConnectionHandler handler : uniqueHandlers) {
                ArrayList<JsonResponse> jsonNotifications = handler.getNotifications();
                for (JsonResponse jsonResponse : jsonNotifications) {
                    try {
                        notificationsHandled.add(jsonResponse.getMethod());
                        stringBuilder.append(jsonResponse.toJsonString()).append("\n");
                    } catch (Exception e) {
                        LogUtils.LOGD(TAG, "getResponse: Error handling notification: " + jsonResponse.toJsonString());
                    }
                }
            }
        }
        if (stringBuilder.length() > 0) {
            return stringBuilder.toString();
        } else {
            return null;
        }
    }

    public void reset() {
        synchronized (clientResponses) {
            clearNotificationsHandled();
            clearMethodsHandled();
            clientResponses.clear();
        }
    }

    public void clearMethodsHandled() {
        methodIdsHandled.clear();
    }

    /**
     * Waits until at least one response has been processed before returning
     */
    public void waitForMethodHandled(String methodName, long timeOutMillis) throws TimeoutException {
        while (! isMethodHandled(methodName) && (timeOutMillis > 0)) {
            try {
                Thread.sleep(500);
                timeOutMillis -= 500;
            } catch (InterruptedException e) {
                LogUtils.LOGW(TAG, "waitForNextResponse got interrupted");
            }
        }
        if (timeOutMillis <= 0)
            throw new TimeoutException();
    }

    public void clearNotificationsHandled() {
        notificationsHandled.clear();
    }

    /**
     * Waits until at least one response has been processed before returning
     */
    public void waitForNotification(String methodName, long timeOutMillis) throws TimeoutException {
        while (! notificationsHandled.contains(methodName) && (timeOutMillis > 0)) {
            try {
                Thread.sleep(500);
                timeOutMillis -= 500;
            } catch (InterruptedException e) {
                LogUtils.LOGW(TAG, "waitForNextResponse got interrupted");
            }
        }
        if (timeOutMillis <= 0)
            throw new TimeoutException();
    }

    private void addResponse(int id, ArrayList<JsonResponse> jsonResponses) {

    }

    private boolean isMethodHandled(String methodName) {
        for(MethodPendingState methodPending : methodIdsHandled.values()) {
            if (methodName.contentEquals(methodPending.name)) {
                return methodPending.handled;
            }
        }
        return false;
    }

    private void setMethodHandled(String methodId) {

    }

    private static class MethodPendingState {
        boolean handled;
        String name;

        MethodPendingState(String name) {
            this.name = name;
        }
    }
}
