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

import android.content.Context;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.FileUtils;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Addons;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Simulates Addons JSON-RPC API
 */
public class AddonsHandler implements JSONConnectionHandlerManager.ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(AddonsHandler.class);

    private static final String ID_NODE = "id";

    private Context context;

    public AddonsHandler(Context context) {
        this.context = context;
    }

    @Override
    public ArrayList<JsonResponse> getNotification() {
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public String[] getType() {
        return new String[]{Addons.GetAddons.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> getResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();

        int methodId = jsonRequest.get(ID_NODE).asInt(-1);

        switch (method) {
            case Addons.GetAddons.METHOD_NAME:
                try {
                    String result = FileUtils.readFile(context, "Addons.GetAddons.json");
                    Addons.GetAddons getAddons = new Addons.GetAddons(methodId, result);
                    jsonResponses.add(getAddons);
                } catch (IOException e) {
                    LogUtils.LOGW(TAG, "Error creating GetAddons response: " + e.getMessage());
                }
                break;
            default:
                LogUtils.LOGD(TAG, "method: " + method + ", not implemented");
        }
        return jsonResponses;
    }
}
