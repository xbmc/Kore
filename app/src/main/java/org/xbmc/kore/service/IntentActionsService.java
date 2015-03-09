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
package org.xbmc.kore.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.utils.LogUtils;

/**
 * Service that implements some player actions
 * Used to support the notifications actions
 */
public class IntentActionsService extends Service {
    public static final String TAG = LogUtils.makeLogTag(IntentActionsService.class);

    public static final String EXTRA_PLAYER_ID = "extra_player_id";

    public static final String ACTION_PLAY_PAUSE = "play_pause",
            ACTION_REWIND = "rewind",
            ACTION_FAST_FORWARD = "fast_forward",
            ACTION_PREVIOUS = "previous",
            ACTION_NEXT = "next";

    @Override
    public void onCreate() { }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We won't create a new thread because the request to the host are
        // already done in a separate thread. Just fire the request and forget
        HostConnection hostConnection = HostManager.getInstance(this).getConnection();

        String action = intent.getAction();
        int playerId = intent.getIntExtra(EXTRA_PLAYER_ID, -1);

        if ((hostConnection != null) && (playerId != -1)) {
            switch (action) {
                case ACTION_PLAY_PAUSE:
                    hostConnection.execute(
                            new Player.PlayPause(playerId),
                            null, null);
                    break;
                case ACTION_REWIND:
                    hostConnection.execute(
                            new Player.SetSpeed(playerId, GlobalType.IncrementDecrement.DECREMENT),
                            null, null);
                    break;
                case ACTION_FAST_FORWARD:
                    hostConnection.execute(
                            new Player.SetSpeed(playerId, GlobalType.IncrementDecrement.INCREMENT),
                            null, null);
                    break;
                case ACTION_PREVIOUS:
                    hostConnection.execute(
                            new Player.GoTo(playerId, Player.GoTo.PREVIOUS),
                            null, null);
                    break;
                case ACTION_NEXT:
                    hostConnection.execute(
                            new Player.GoTo(playerId, Player.GoTo.NEXT),
                            null, null);
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
