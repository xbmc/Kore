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
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

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

    private HostConnectionObserver mHostConnectionObserver = null;

    private List<HostConnectionObserver.PlayerEventsObserver> mConnectionObservers = new ArrayList<>();
    private NotificationObserver mNotificationObserver;

    private boolean somethingPlaying = false;
    private Handler mStopHandler = new Handler();

    @Override
    public void onCreate() {
        // We do not create any thread because all the works is supposed to
        // be done on the main thread, so that the connection observer
        // can be shared with the app, and notify it on the UI thread
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.LOGD(TAG, "onStartCommand");
        // Create the observers we are managing
        createObservers();

        // If no observers created, stop immediately
        if (mConnectionObservers.isEmpty()) {
            LogUtils.LOGD(TAG, "No observers, stopping observer service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Get the connection observer here, not on create to check if
        // there has been a change in hosts, and if so unregister the previous one
        HostConnectionObserver connectionObserver = HostManager.getInstance(this).getHostConnectionObserver();

        // If we are already initialized and the same host, exit
        if (mHostConnectionObserver == connectionObserver) {
            LogUtils.LOGD(TAG, "Already initialized");
            return START_STICKY;
        }

        // Create the observers we are managing
        createObservers();
        if (mConnectionObservers.isEmpty()) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NotificationObserver.NOTIFICATION_ID, mNotificationObserver.getNothingPlayingNotification());

        // If there's a change in hosts, unregister from the previous one
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }

        // Register us on the connection observer
        mHostConnectionObserver = connectionObserver;
        mHostConnectionObserver.registerPlayerObserver(this, true);

        // If we get killed, after returning from here, don't restart
        return START_STICKY;
    }

    private void createObservers() {
        mConnectionObservers = new ArrayList<>();

        // Always show a notification
//        boolean showNotification = PreferenceManager
//                .getDefaultSharedPreferences(this)
//                .getBoolean(Settings.KEY_PREF_SHOW_NOTIFICATION,
//                            Settings.DEFAULT_PREF_SHOW_NOTIFICATION);
        mNotificationObserver = new NotificationObserver(this);
        mConnectionObservers.add(mNotificationObserver);

        // Check whether we should react to phone state changes and wether
        // we have permissions to do so
        boolean shouldPause = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_PAUSE_DURING_CALLS,
                            Settings.DEFAULT_PREF_PAUSE_DURING_CALLS);
        boolean hasPhonePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        if (shouldPause && hasPhonePermission) {
            mConnectionObservers.add(new PauseCallObserver(this));
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
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerOnConnectionError(0, "Task removed");
        }

        LogUtils.LOGD(TAG, "Shutting down observer service - Task removed");
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // Gracefully stop
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerOnConnectionError(0, "Service destroyed");
        }
        LogUtils.LOGD(TAG, "Shutting down observer service - destroyed");
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
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
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerOnPlay(getActivePlayerResult, getPropertiesResult, getItemResult);
        }
        somethingPlaying = true;
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerOnPause(getActivePlayerResult, getPropertiesResult, getItemResult);
        }
        somethingPlaying = true;
    }

    public void playerOnStop() {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerOnStop();
        }

        somethingPlaying = false;

        // Stop service if nothing starts in a couple of seconds
        mStopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!somethingPlaying) {
                    LogUtils.LOGD(TAG, "Stopping service");
                    if (mHostConnectionObserver != null) {
                        mHostConnectionObserver.unregisterPlayerObserver(ConnectionObserversManagerService.this);
                    }
                    stopForeground(true);
                    stopSelf();
                }
            }
        }, 5000);
    }

    public void playerNoResultsYet() {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerNoResultsYet();
        }
        somethingPlaying = false;
    }

    public void playerOnConnectionError(int errorCode, String description) {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.playerOnConnectionError(errorCode, description);
        }
        somethingPlaying = false;

        // Stop service
        LogUtils.LOGD(TAG, "Shutting down observer service - Connection error");
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }
        stopForeground(true);
        stopSelf();
    }

    public void systemOnQuit() {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.systemOnQuit();
        }
        somethingPlaying = false;

        // Stop service
        LogUtils.LOGD(TAG, "Shutting down observer service - System quit");
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }
        stopForeground(true);
        stopSelf();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.inputOnInputRequested(title, type, value);
        }
    }

    public void observerOnStopObserving() {
        for (HostConnectionObserver.PlayerEventsObserver observer : mConnectionObservers) {
            observer.observerOnStopObserving();
        }
        // Called when the user changes host
        LogUtils.LOGD(TAG, "Shutting down observer service - Stop observing");
        stopForeground(true);
        stopSelf();
    }
}
