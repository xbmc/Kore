/*
 * Copyright 2016 Tomer Froumin. All rights reserved.
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

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;

/**
 * This listener handles changes to the phone state, such as receiving a
 * call or hanging up, and synchronizes Kodi's currently playing state
 * in order to prevent missing the movie (or what's playing) while the
 * viewer is talking on the phone.
 *
 * The listener query Kodi's state on phone state changed event.
 * When a call ends we only resume if it was paused by the listener.
 */
public class PauseCallObserver extends PhoneStateListener
        implements HostConnectionObserver.PlayerEventsObserver {
    public static final String TAG = LogUtils.makeLogTag(PauseCallObserver.class);

    private int currentActivePlayerId = -1;
    private boolean isPlaying = false;
    private boolean shouldResume = false;

    private final Context context;
    private final HostManager hostManager;

    public PauseCallObserver(Context context) {
        this.context = context;
        this.hostManager = HostManager.getInstance(context);

        ((TelephonyManager) this.context
                .getSystemService(Context.TELEPHONY_SERVICE))
                .listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        if (state == TelephonyManager.CALL_STATE_OFFHOOK && isPlaying) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), null, null);
            shouldResume = true;
        } else if (state == TelephonyManager.CALL_STATE_IDLE && !isPlaying && shouldResume) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), null, null);
            shouldResume = false;
        } else if (state == TelephonyManager.CALL_STATE_RINGING) {
            Player.Notification action = new Player.Notification(
                    context.getResources().getString(R.string.pause_call_incoming_title),
                    context.getResources().getString(R.string.pause_call_incoming_message));
            action.execute(hostManager.getConnection(), null, null);
        }
    }

    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {

    }

    @Override
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        currentActivePlayerId = getActivePlayerResult.playerid;
        isPlaying = true;
    }

    @Override
    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        if(currentActivePlayerId != getActivePlayerResult.playerid) {
            shouldResume = false;
        }
        currentActivePlayerId = getActivePlayerResult.playerid;
        isPlaying = false;
    }

    private void stopListener() {
        ((TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE))
                .listen(this, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void playerOnStop() {
        currentActivePlayerId = -1;
        isPlaying = false;
        shouldResume = false;
        stopListener();
    }

    @Override
    public void playerOnConnectionError(int errorCode, String description) {
        playerOnStop();
    }

    @Override
    public void playerNoResultsYet() {
        playerOnStop();
    }

    @Override
    public void systemOnQuit() {
        playerOnStop();
    }

    @Override
    public void inputOnInputRequested(String title, String type, String value) {}

    @Override
    public void observerOnStopObserving() {
        playerOnStop();
    }
}