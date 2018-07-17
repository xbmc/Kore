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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Application;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;

import static org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Application.OnVolumeChanged;

/**
 * Simulates Application JSON-RPC API
 */
public class ApplicationHandler implements JSONConnectionHandlerManager.ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(ApplicationHandler.class);

    private boolean muted;
    private int volume;
    private static final String ID_NODE = "id";
    private static final String PARAMS_NODE = "params";
    private static final String PROPERTIES_NODE = "properties";

    private ArrayList<JsonResponse> jsonNotifications = new ArrayList<>();

    /**
     * Sets the muted state and sends a notification
     * @param muted
     * @param notify true if OnVolumeChanged should be sent, false otherwise
     */
    public void setMuted(boolean muted, boolean notify) {
        this.muted = muted;

        if (notify)
            jsonNotifications.add(new OnVolumeChanged(muted, volume));
    }

    /**
     * Sets the volume and sends a notification
     * @param volume
     * @param notify true if OnVolumeChanged should be sent, false otherwise
     */
    public void setVolume(int volume, boolean notify) {
        this.volume = volume;

        if (notify)
            jsonNotifications.add(new OnVolumeChanged(muted, volume));
    }

    public int getVolume() {
        return volume;
    }

    @Override
    public ArrayList<JsonResponse> getNotifications() {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>(jsonNotifications);
        jsonNotifications.clear();
        return jsonResponses;
    }

    @Override
    public void reset() {
        this.volume = 0;
        this.muted = false;
    }

    @Override
    public String[] getType() {
        return new String[]{Application.GetProperties.METHOD_NAME,
                            Application.SetMute.METHOD_NAME,
                            Application.SetVolume.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> getResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();

        int methodId = jsonRequest.get(ID_NODE).asInt(-1);

        switch (method) {
            case Application.GetProperties.METHOD_NAME:
                Application.GetProperties response = new Application.GetProperties(methodId);

                JsonNode jsonNode = jsonRequest.get(PARAMS_NODE).get(PROPERTIES_NODE);
                for (JsonNode node : jsonNode) {
                    switch(node.asText()) {
                        case Application.GetProperties.MUTED:
                            response.addMuteState(muted);
                            break;
                        case Application.GetProperties.VOLUME:
                            response.addVolume(volume);
                            break;
                    }
                }

                jsonResponses.add(response);
                break;
            case Application.SetMute.METHOD_NAME:
                setMuted(!muted, true);
                jsonResponses.add(new Application.SetMute(methodId, muted));
                break;
            case Application.SetVolume.METHOD_NAME:
                JsonNode params = jsonRequest.get(PARAMS_NODE);
                String value = params.get("volume").asText();
                switch (value) {
                    case GlobalType.IncrementDecrement.INCREMENT:
                        setVolume(volume + 1, true);
                        break;
                    case GlobalType.IncrementDecrement.DECREMENT:
                        setVolume(volume - 1, true);
                        break;
                    default:
                        setVolume(Integer.parseInt(value), true);
                        break;
                }
                jsonResponses.add(new Application.SetVolume(methodId, volume));
                break;
            default:
                LogUtils.LOGD(TAG, "method: " + method + ", not implemented");
        }
        return jsonResponses;
    }
}
