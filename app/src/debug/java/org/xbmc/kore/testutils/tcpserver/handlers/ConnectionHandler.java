/*
 * Copyright 2018 Martijn Brekhof. All rights reserved.
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

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

abstract class ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(ConnectionHandler.class);

    private ArrayList<JsonResponse> notifications = new ArrayList<>();
    private HashSet<String> methodsHandled = new HashSet<>();

    /**
     * Used to determine which methods the handler implements
     * @return list of JSON method names
     */
    abstract String[] getType();

    abstract ArrayList<JsonResponse> createResponse(String method, ObjectNode jsonRequest);

    /**
     * Used to get any notifications from the handler.
     * @return {@link JsonResponse} that should be sent to the client or null if there are no notifications
     */
    public ArrayList<JsonResponse> getNotifications() {
        ArrayList<JsonResponse> list = new ArrayList<>(notifications);
        notifications.clear();
        return list;
    }

    /**
     * Returns the response for the requested method.
     * @param method requested method
     * @param jsonRequest json node containing the original request
     * @return {@link JsonResponse} that should be sent to the client
     */
    public ArrayList<JsonResponse> getResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> responses = createResponse(method, jsonRequest);
        methodsHandled.add(method);
        return responses;
    }

    /**
     * Sets the state of the handler to its initial state
     */
    public void reset() {
        methodsHandled.clear();
    }

    /**
     * Waits for given method to be handled by this handler before returning.
     * @param method
     * @param timeOutMillis
     */
    public void waitForMethodHandled(String method, long timeOutMillis) throws TimeoutException {
        while ((!methodsHandled.contains(method)) && timeOutMillis > 0) {
            try {
                Thread.sleep(100);
                timeOutMillis -= 100;
            } catch (InterruptedException e) {
                LogUtils.LOGE(TAG, "Thread.sleep interrupted");
                return;
            }
        }
        if (timeOutMillis <= 0)
            throw new TimeoutException();
    }

    /**
     * Clears the list of methods handled by the connection handler.
     */
    public void clearMethodsHandled() {
        methodsHandled.clear();
    }

    void addNotification(JsonResponse notification) {
        notifications.add(notification);
    }
}
