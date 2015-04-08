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
package org.xbmc.kore.utils;

import android.content.Context;
import android.os.Handler;

import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlayerType;

import java.util.ArrayList;

/**
 * Common jsonrpc method calls, that appear more than once on the code
 */
public class jsonrpcCommonCalls {

    /**
     * Starts a playlist if no active players are playing
     *
     * @param context Context
     * @param playlistId PlaylistID
     * @param callbackHandler Handler on which to post method callbacks
     */
    public static void startPlaylistIfNoActivePlayers(final Context context, final int playlistId, final Handler callbackHandler) {
        final HostConnection connection = HostManager.getInstance(context).getConnection();
        Player.GetActivePlayers action = new Player.GetActivePlayers();
        action.execute(connection, new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result ) {
                // find out if any player is running. If it is not, start one
                if (result.size() == 0) {
                    startPlaying(connection, playlistId, callbackHandler);
                }
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);

    }

    private static void startPlaying(final HostConnection connection, final int playlistId, final Handler callbackHandler) {
        Player.Open action = new Player.Open(playlistId);
        action.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);
    }

}
