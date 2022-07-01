/*
 * Copyright 2016 Synced Synapse. All rights reserved.
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

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.core.content.ContextCompat;

import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.notification.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This service is a wrapper over {@link HostConnectionObserver} that
 * manages connection observers, and does it in a service that keeps running
 * until the connection is lost.
 * The observers are created here.
 * This service stops itself as soon as there's no connection.
 *
 * A {@link HostConnectionObserver} singleton is used to keep track of Kodi's
 * state. This singleton should be the same as used in the app's activities
 */
public class ConnectionObserversManagerService extends Service
        implements HostConnectionObserver.PlayerEventsObserver {
    public static final String TAG = LogUtils.makeLogTag(ConnectionObserversManagerService.class);

    private HostConnectionObserver hostConnectionObserver = null;

    private List<HostConnectionObserver.PlayerEventsObserver> observers = new ArrayList<>();
    private NotificationObserver notificationObserver;

    private boolean somethingIsPlaying = false;
    private final Handler stopHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        // We do not create any thread because all the works is supposed to
        // be done on the main thread, so that the connection observer
        // can be shared with the app, and notify it on the UI thread
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.LOGD(TAG, "onStartCommand");

        // Create the observers we are managing and immediatelly call
        // startForeground() to avoid ANRs
        if (observers.isEmpty()) {
            createObservers();
        }
        startForeground(NotificationObserver.NOTIFICATION_ID,
                notificationObserver.getCurrentNotification());

        HostConnectionObserver connectionObserver =
                HostManager.getInstance(this).getHostConnectionObserver();

        if (hostConnectionObserver == null) {
            hostConnectionObserver = connectionObserver;
            hostConnectionObserver.registerPlayerObserver(this);
        } else if (hostConnectionObserver != connectionObserver) {
            // There has been a change in hosts.
            // Unregister the previous one and register the current one
            hostConnectionObserver.unregisterPlayerObserver(this);
            hostConnectionObserver = connectionObserver;
            hostConnectionObserver.registerPlayerObserver(this);
        }

        // If we get killed after returning from here, restart
        return START_STICKY;
    }

    private void createObservers() {
        observers = new ArrayList<>();

        // Always show a notification
        notificationObserver = new NotificationObserver(this);
        observers.add(notificationObserver);

        // Check whether we should react to phone state changes and wether
        // we have permissions to do so
        boolean shouldPause = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_PAUSE_DURING_CALLS,
                            Settings.DEFAULT_PREF_PAUSE_DURING_CALLS);
        boolean hasPhonePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED;
        if (shouldPause && hasPhonePermission) {
            observers.add(new PauseCallObserver(this));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onTaskRemoved (Intent rootIntent) {
        // Gracefully stop
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerOnConnectionError(0, "Task removed");
        }

        LogUtils.LOGD(TAG, "Shutting down observer service - Task removed");
        if (hostConnectionObserver != null) {
            hostConnectionObserver.unregisterPlayerObserver(this);
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // Gracefully stop
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerOnConnectionError(0, "Service destroyed");
        }
        LogUtils.LOGD(TAG, "Shutting down observer service - destroyed");
        if (hostConnectionObserver != null) {
            hostConnectionObserver.unregisterPlayerObserver(this);
        }
    }

    @Override
    public void playerOnPropertyChanged(Player.NotificationsData notificationsData) {

    }

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerOnPlay(getActivePlayerResult, getPropertiesResult, getItemResult);
        }
        somethingIsPlaying = true;
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerOnPause(getActivePlayerResult, getPropertiesResult, getItemResult);
        }
        somethingIsPlaying = true;
    }

    public void playerOnStop() {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerOnStop();
        }

        somethingIsPlaying = false;

        // Stop service if nothing starts in a couple of seconds
        stopHandler.postDelayed(() -> {
            if (!somethingIsPlaying) {
                LogUtils.LOGD(TAG, "Stopping service");
                if (hostConnectionObserver != null) {
                    hostConnectionObserver.unregisterPlayerObserver(ConnectionObserversManagerService.this);
                }
                stopForeground(true);
                stopSelf();
            }
        }, 5000);
    }

    public void playerNoResultsYet() {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerNoResultsYet();
        }
        somethingIsPlaying = false;
    }

    public void playerOnConnectionError(int errorCode, String description) {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.playerOnConnectionError(errorCode, description);
        }
        somethingIsPlaying = false;

        // Stop service
        LogUtils.LOGD(TAG, "Shutting down observer service - Connection error");
        if (hostConnectionObserver != null) {
            hostConnectionObserver.unregisterPlayerObserver(this);
        }
        stopForeground(true);
        stopSelf();
    }

    public void systemOnQuit() {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.systemOnQuit();
        }
        somethingIsPlaying = false;

        // Stop service
        LogUtils.LOGD(TAG, "Shutting down observer service - System quit");
        if (hostConnectionObserver != null) {
            hostConnectionObserver.unregisterPlayerObserver(this);
        }
        stopForeground(true);
        stopSelf();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.inputOnInputRequested(title, type, value);
        }
    }

    public void observerOnStopObserving() {
        for (HostConnectionObserver.PlayerEventsObserver observer : observers) {
            observer.observerOnStopObserving();
        }
        // Called when the user changes host
        LogUtils.LOGD(TAG, "Shutting down observer service - Stop observing");
        stopForeground(true);
        stopSelf();
    }
}
