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

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.method.JSONRPC;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.JSONRPC.Ping;

import java.util.ArrayList;

/**
 * Simulates JSON RPC JSON-RPC API
 */
public class JSONRPCHandler extends ConnectionHandler {

    @Override
    public String[] getType() {
        return new String[] {JSONRPC.Ping.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> createResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses =  new ArrayList<>();
        jsonResponses.add(new Ping(jsonRequest.get("id").asInt()));
        return jsonResponses;
    }
}
