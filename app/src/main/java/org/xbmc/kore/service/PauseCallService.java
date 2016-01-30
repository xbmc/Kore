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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;

/**
 * This service maintains a notification in the notification area while
 * something is playing, and keeps running while it is playing.
 * This service stops itself as soon as the playing stops or there's no
 * connection. Thus, this should only be started if something is already
 * playing, otherwise it will shutdown automatically.
 * It doesn't try to mirror Kodi's state at all times, because that would
 * imply running at all times which can be resource consuming.
 *
 * A {@link HostConnectionObserver} singleton is used to keep track of Kodi's
 * state. This singleton should be the same as used in the app's activities
 */
public class PauseCallService extends BroadcastReceiver
        implements HostConnectionObserver.PlayerEventsObserver {
    public static final String TAG = LogUtils.makeLogTag(PauseCallService.class);
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static HostConnectionObserver mHostConnectionObserver = null;
    private static int currentActivePlayerId = -1;
    private static boolean isPlaying = false;
    private static boolean shouldResume = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check whether we should react to phone state changes
        boolean shouldPause = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(Settings.KEY_PREF_PAUSE_DURING_CALLS, Settings.DEFAULT_PREF_PAUSE_DURING_CALLS);
        if(!shouldPause) return;

        int state = 0;
        String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
        LogUtils.LOGD(TAG, "onReceive " + stateStr);

        // The phone state changed from in call to idle
        if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            state = TelephonyManager.CALL_STATE_IDLE;
        }
        // The phone state changed from idle to in call
        else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            state = TelephonyManager.CALL_STATE_OFFHOOK;
        }
        // The phone state changed from idle to ringing
        else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            state = TelephonyManager.CALL_STATE_RINGING;
        }

        if(state == lastState) return;
        handleState(context, state);
        lastState = state;
    }

    protected void handleState(Context context, int state) {
        // We won't create a new thread because the request to the host are
        // already done in a separate thread. Just fire the request and forget
        HostManager hostManager = HostManager.getInstance(context);
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }
        mHostConnectionObserver = hostManager.getHostConnectionObserver();
        mHostConnectionObserver.registerPlayerObserver(this, true);

        if(state == TelephonyManager.CALL_STATE_OFFHOOK && isPlaying) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), null, null);
            shouldResume = true;
        }
        else if(state == TelephonyManager.CALL_STATE_IDLE && !isPlaying && shouldResume) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), null, null);
            shouldResume = false;
        }
        else if(state == TelephonyManager.CALL_STATE_RINGING) {
            Player.Notification action = new Player.Notification(
                    context.getResources().getString(R.string.pause_call_incoming_title),
                    context.getResources().getString(R.string.pause_call_incoming_message));
            action.execute(hostManager.getConnection(), null, null);
        }
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
        currentActivePlayerId = getActivePlayerResult.playerid;
        isPlaying = false;
    }

    @Override
    public void playerOnStop() {
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }
        currentActivePlayerId = -1;
        isPlaying = false;
    }

    @Override
    public void playerOnConnectionError(int errorCode, String description) {
        playerOnStop();
    }

    @Override
    public void playerNoResultsYet() {}

    @Override
    public void systemOnQuit() {
        playerOnStop();
    }

    @Override
    public void inputOnInputRequested(String title, String type, String value) {}

    @Override
    public void observerOnStopObserving() {}
}