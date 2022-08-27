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
package org.xbmc.kore.host;

import android.os.Handler;
import android.os.Looper;

import org.xbmc.kore.host.actions.GetPlaylist;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.JSONRPC;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.notification.Application;
import org.xbmc.kore.jsonrpc.notification.Input;
import org.xbmc.kore.jsonrpc.notification.Player.NotificationsData;
import org.xbmc.kore.jsonrpc.notification.Playlist;
import org.xbmc.kore.jsonrpc.notification.System;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Object that listens to a connection and notifies observers about changes in its state
 * This class serves as an adpater to the {@link HostConnection.PlayerNotificationsObserver},
 * to enable to get notifications not only through TCP but also through HTTP.
 * Depending on the connection protocol this class registers itself as an observer for
 * {@link HostConnection.PlayerNotificationsObserver} and forwards the notifications it gets,
 * or, if through HTTP, starts a periodic polling of Kodi, and tries to discern when a change in
 * the player has occurred, notifying the listeners
 *
 * NOTE: An object of this class should always be called from the same thread.
 */
public class HostConnectionObserver
        implements HostConnection.PlayerNotificationsObserver,
                   HostConnection.SystemNotificationsObserver,
                   HostConnection.InputNotificationsObserver,
                   HostConnection.ApplicationNotificationsObserver,
                   HostConnection.PlaylistNotificationsObserver {
    public static final String TAG = LogUtils.makeLogTag(HostConnectionObserver.class);

    public interface PlaylistEventsObserver {
        /**
         * Notifies that a playlist has been cleared
         * @param playlistId of playlist that has been cleared
         */
        void onPlaylistClear(int playlistId);

        /**
         * Notifies about the available playlists on Kodi
         * @param playlists Available playlists
         */
        void onPlaylistsAvailable(ArrayList<GetPlaylist.GetPlaylistResult> playlists);

        /**
         * Notifies that an error occured when fetching playlists
         * @param errorCode Error code
         * @param description Error description
         */
        void onPlaylistError(int errorCode, String description);
    }

    /**
     * Interface that an observer has to implement to receive playlist events
     */
    public interface ApplicationEventsObserver {
        /**
         * Notifies the observer that volume has changed
         * @param volume Volume level
         * @param muted Is muted
         */
        void onApplicationVolumeChanged(int volume, boolean muted);
    }

    /**
     * Interface that an observer has to implement to receive player events
     */
    public interface PlayerEventsObserver {
        /**
         * Constants for possible events. Useful to save the last event and compare with the
         * current one to check for differences
         */
        int PLAYER_NO_RESULT = 0,
                PLAYER_CONNECTION_ERROR = 1,
                PLAYER_IS_PLAYING = 2,
                PLAYER_IS_PAUSED = 3,
                PLAYER_IS_STOPPED = 4;

        void onPlayerPropertyChanged(NotificationsData notificationsData);

        /**
         * Notifies that something is playing
         * @param getActivePlayerResult Active player obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetActivePlayers}
         * @param getPropertiesResult Properties obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetProperties}
         * @param getItemResult Currently playing item, obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
         */
        void onPlayerPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                          PlayerType.PropertyValue getPropertiesResult,
                          ListType.ItemsAll getItemResult);

        /**
         * Notifies that something is paused
         * @param getActivePlayerResult Active player obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetActivePlayers}
         * @param getPropertiesResult Properties obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetProperties}
         * @param getItemResult Currently paused item, obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
         */
        void onPlayerPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                           PlayerType.PropertyValue getPropertiesResult,
                           ListType.ItemsAll getItemResult);

        /**
         * Notifies that media is stopped/nothing is playing
         */
        void onPlayerStop();

        /**
         * Called when we get a connection error
         * @param errorCode Code
         * @param description Description
         */
        void onPlayerConnectionError(int errorCode, String description);

        /**
         * Notifies that we don't have a result yet
         */
        void onPlayerNoResultsYet();

        /**
         * Notifies that Kodi has quit/shutdown/sleep
         */
        void onSystemQuit();

        /**
         * Notifies that Kodi has requested input
         */
        void onInputRequested(String title, String type, String value);

        /**
         * Notifies the observer that it this is stopping
         */
        void onObserverStopObserving();
    }

    /**
     * Interface that an observer has to implement to receive the connection status
     */
    public interface ConnectionStatusObserver {
        /**
         * Constants for the status
         */
        int CONNECTION_NO_RESULT = 0,
                CONNECTION_ERROR = 1,
                CONNECTION_SUCCESS = 2;

        /**
         * Notifies that we don't have a result yet
         */
        void onConnectionStatusNoResultsYet();

        /**
         * Notifies that we're successfully connected
         */
        void onConnectionStatusSuccess();

        /**
         * Called when we get a connection error
         * @param errorCode Code
         * @param description Description
         */
        void onConnectionStatusError(int errorCode, String description);
    }

    /**
     * The connection on which to listen
     */
    private final HostConnection connection;

    /**
     * The list of observers
     */
    private final List<PlayerEventsObserver> playerEventsObservers = new ArrayList<>();
    private final List<ApplicationEventsObserver> applicationEventsObservers = new ArrayList<>();
    private final List<PlaylistEventsObserver> playlistEventsObservers = new ArrayList<>();
    private final List<ConnectionStatusObserver> connectionStatusObservers = new ArrayList<>();

    // This controls the frequency with wich the playlist is checked.
    // It's checked everytime it reaches 0, being reset afterwards
    private int checkPlaylistFrequencyCounter = 0;

    // Associate the Handler with the UI thread
    private final Handler checkerHandler = new Handler(Looper.getMainLooper());
    private final Runnable httpCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            final int HTTP_NOTIFICATION_CHECK_INTERVAL = 2000;
            // If no one is listening to this, just exit
            if (playerEventsObservers.isEmpty() &&
                applicationEventsObservers.isEmpty() &&
                playlistEventsObservers.isEmpty() &&
                connectionStatusObservers.isEmpty())
                return;

            if (!playerEventsObservers.isEmpty())
                checkWhatsPlaying();

            if (!applicationEventsObservers.isEmpty())
                getApplicationProperties();

            if (!playlistEventsObservers.isEmpty()) {
                if (checkPlaylistFrequencyCounter <= 0) {
                    // Check playlist and reset the frequency counter
                    checkPlaylist();
                    checkPlaylistFrequencyCounter = 1;
                } else {
                    checkPlaylistFrequencyCounter--;
                }
            }

            if (!connectionStatusObservers.isEmpty()) {
                checkConnectionStatus();
            }

            checkerHandler.postDelayed(this, HTTP_NOTIFICATION_CHECK_INTERVAL);
        }
    };

    private final Runnable tcpCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            // If no one is listening to this, just exit
            if (playerEventsObservers.isEmpty() &&
                applicationEventsObservers.isEmpty() &&
                playlistEventsObservers.isEmpty() &&
                connectionStatusObservers.isEmpty())
                return;

            final int PING_AFTER_ERROR_CHECK_INTERVAL = 2000,
                    PING_AFTER_SUCCESS_CHECK_INTERVAL = 5000;
            JSONRPC.Ping ping = new JSONRPC.Ping();
            ping.execute(connection, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Ok, we've got a ping, if there are playerEventsObservers and
                    // we were in a error or uninitialized state, update
                    if ((!playerEventsObservers.isEmpty()) &&
                        ((hostState.lastPlayerEventsResult == PlayerEventsObserver.PLAYER_NO_RESULT) ||
                         (hostState.lastPlayerEventsResult == PlayerEventsObserver.PLAYER_CONNECTION_ERROR))) {
                        LogUtils.LOGD(TAG, "Checking what's playing because we don't have info about it");
                        checkWhatsPlaying();
                    }

                    if ((!playlistEventsObservers.isEmpty()) &&
                        (hostState.lastPlayerEventsResult == PlayerEventsObserver.PLAYER_CONNECTION_ERROR)) {
                        checkPlaylist();
                    }

                    if (!connectionStatusObservers.isEmpty()) {
                        notifyConnectionStatusSucess(connectionStatusObservers);
                    }

                    checkerHandler.postDelayed(tcpCheckerRunnable, PING_AFTER_SUCCESS_CHECK_INTERVAL);
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Notify a connection error
                    notifyConnectionError(errorCode, description, playerEventsObservers);
                    notifyConnectionStatusError(errorCode, description, connectionStatusObservers);
                    checkerHandler.postDelayed(tcpCheckerRunnable, PING_AFTER_ERROR_CHECK_INTERVAL);
                }
            }, checkerHandler);
        }
    };

    private static class HostState {
        int lastPlayerEventsResult = PlayerEventsObserver.PLAYER_NO_RESULT;
        int lastPlayerEventsErrorCode;
        String lastPlayerEventsErrorDescription;
        PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult = null;
        PlayerType.PropertyValue lastGetPropertiesResult = null;
        ListType.ItemsAll lastGetItemResult = null;
        boolean volumeMuted = false;
        int volumeLevel = -1;  // -1 indicates no volumeLevel known
        ArrayList<GetPlaylist.GetPlaylistResult> lastGetPlaylistResults = null;
        int lastConnectionStatusResult = ConnectionStatusObserver.CONNECTION_NO_RESULT;
        int lastConnectionStatusErrorCode;
        String lastConnectionStatusErrorDescription;
    }
    private HostState hostState;

    public HostConnectionObserver(HostConnection connection) {
        this.hostState = new HostState();
        this.connection = connection;
    }

    /**
     * Registers a new observer that will be notified about player events
     * @param observer Observer
     */
    public void registerPlayerObserver(PlayerEventsObserver observer) {
        if (this.connection == null || observer == null) return;

        if (!playerEventsObservers.contains(observer))
            playerEventsObservers.add(observer);

        LogUtils.LOGD(TAG, "Register Player Observer " + observer.getClass().getSimpleName() +
                            ". Got " + playerEventsObservers.size() + " observers.");

        // Reply immediatelly
        replyWithLastResult(observer);

        if (playerEventsObservers.size() == 1) {
            // If this is the first observer, start checking through HTTP or register us
            // as a connection observer, which we will pass to the "real" observer
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.registerPlayerNotificationsObserver(this, checkerHandler);
                connection.registerSystemNotificationsObserver(this, checkerHandler);
                connection.registerInputNotificationsObserver(this, checkerHandler);
            }
            startCheckerHandler();
        }
    }

    /**
     * Unregisters a previously registered observer
     * @param observer Observer to unregister
     */
    public void unregisterPlayerObserver(PlayerEventsObserver observer) {
        if (this.connection == null || observer == null) return;

        playerEventsObservers.remove(observer);

        LogUtils.LOGD(TAG, "Unregister Player Observer " + observer.getClass().getSimpleName() +
                           ((playerEventsObservers.size() > 0) ?
                            ". Got " + playerEventsObservers.size() + " observers." :
                            ". No observers left."));

        if (playerEventsObservers.isEmpty()) {
            // No more observers. If through TCP unregister us from the host connection
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.unregisterPlayerNotificationsObserver(this);
                connection.unregisterSystemNotificationsObserver(this);
                connection.unregisterInputNotificationsObserver(this);
            }
            hostState.lastPlayerEventsResult = PlayerEventsObserver.PLAYER_NO_RESULT;
        }
    }

    /**
     * Registers a new observer that will be notified about application events
     * @param observer Observer
     */
    public void registerApplicationObserver(ApplicationEventsObserver observer) {
        if (this.connection == null || observer == null) return;

        if (!applicationEventsObservers.contains(observer))
            applicationEventsObservers.add(observer);

        LogUtils.LOGD(TAG, "Register Application Observer " + observer.getClass().getSimpleName() +
                           ". Got " + applicationEventsObservers.size() + " observers.");

        // Reply immediatelly
        replyWithLastResult(observer);

        if (applicationEventsObservers.size() == 1) {
            // If this is the first observer, start checking through HTTP or register us
            // as a connection observer, which we will pass to the "real" observer
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.registerApplicationNotificationsObserver(this, checkerHandler);
            }
            startCheckerHandler();
        }
    }

    /**
     * Unregisters a previously registered observer
     * @param observer Observer to unregister
     */
    public void unregisterApplicationObserver(ApplicationEventsObserver observer) {
        if (this.connection == null || observer == null) return;

        applicationEventsObservers.remove(observer);

        LogUtils.LOGD(TAG, "Unregister Application Observer " + observer.getClass().getSimpleName() +
                           ((applicationEventsObservers.size() > 0) ?
                            ". Got " + applicationEventsObservers.size() + " observers." :
                            ". No observers left."));

        if (applicationEventsObservers.isEmpty()) {
            // No more observers. If through TCP unregister us from the host connection
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.unregisterApplicationNotificationsObserver(this);
            }
        }
    }

    /**
     * Registers a new observer that will be notified about playlist events
     * @param observer Observer
     */
    public void registerPlaylistObserver(PlaylistEventsObserver observer) {
        if (this.connection == null || observer == null) return;

        if (!playlistEventsObservers.contains(observer) ) {
            playlistEventsObservers.add(observer);
        }

        LogUtils.LOGD(TAG, "Register Playlist Observer " + observer.getClass().getSimpleName() +
                           ". Got " + playlistEventsObservers.size() + " observers.");

        // Reply immediatelly
        replyWithLastResult(observer);

        if (playlistEventsObservers.size() == 1) {
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.registerPlaylistNotificationsObserver(this, checkerHandler);
            }
            startCheckerHandler();
        }
    }

    public void unregisterPlaylistObserver(PlaylistEventsObserver observer) {
        if (this.connection == null || observer == null) return;

        playlistEventsObservers.remove(observer);

        LogUtils.LOGD(TAG, "Unregister Playlist Observer " + observer.getClass().getSimpleName() +
                           ((playlistEventsObservers.size() > 0) ?
                            ". Got " + playlistEventsObservers.size() + " observers." :
                            ". No observers left."));

        if (playlistEventsObservers.isEmpty()) {
            // No more observers. If through TCP unregister us from the host connection
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.unregisterPlaylistNotificationsObserver(this);
            }
            hostState.lastGetPlaylistResults = null;
        }
    }

    /**
     * Registers a new observer that will be notified about connection status
     * @param observer Observer
     */
    public void registerConnectionStatusObserver(ConnectionStatusObserver observer) {
        if (this.connection == null || observer == null) return;

        if (!connectionStatusObservers.contains(observer))
            connectionStatusObservers.add(observer);

        LogUtils.LOGD(TAG, "Register Connection Status Observer " + observer.getClass().getSimpleName() +
                           ". Got " + connectionStatusObservers.size() + " observers.");

        // Reply immediatelly
        replyWithLastResult(observer);
        startCheckerHandler();
    }

    /**
     * Unregisters a previously registered observer
     * @param observer Observer to unregister
     */
    public void unregisterConnectionStatusObserver(ConnectionStatusObserver observer) {
        if (this.connection == null || observer == null) return;

        connectionStatusObservers.remove(observer);

        LogUtils.LOGD(TAG, "Unregister Connection Status Observer " + observer.getClass().getSimpleName() +
                           ((connectionStatusObservers.size() > 0) ?
                            ". Got " + connectionStatusObservers.size() + " observers." :
                            ". No observers left."));
    }

    /**
     * Unregisters all observers
     */
    public void stopObserving() {
        for (final PlayerEventsObserver observer : playerEventsObservers)
            observer.onObserverStopObserving();

        playerEventsObservers.clear();
        playlistEventsObservers.clear();
        applicationEventsObservers.clear();
        connectionStatusObservers.clear();

        if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
            connection.unregisterPlayerNotificationsObserver(this);
            connection.unregisterSystemNotificationsObserver(this);
            connection.unregisterInputNotificationsObserver(this);
            connection.unregisterApplicationNotificationsObserver(this);
            connection.unregisterPlaylistNotificationsObserver(this);
            checkerHandler.removeCallbacks(tcpCheckerRunnable);
        }
        hostState = new HostState();
    }

    @Override
    public void onPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.OnPropertyChanged notification) {
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.onPlayerPropertyChanged(notification.data);
        }
    }

    /**
     * The {@link HostConnection.PlayerNotificationsObserver} interface methods
     * Start the chain calls to get whats playing
     */
    public void onPlay(org.xbmc.kore.jsonrpc.notification.Player.OnPlay notification) {
        // Ignore this if Kodi is Leia or higher, as we'll be properly notified via OnAVStart
        // See https://github.com/xbmc/Kore/issues/602 and https://github.com/xbmc/xbmc/pull/13726
        // Note: OnPlay is still required for picture items.
        if (connection.getHostInfo().isLeiaOrLater() &&
            ! notification.data.item.type.contentEquals("picture") ) {
            LogUtils.LOGD(TAG, "OnPlay notification ignored. Will wait for OnAVStart.");
            return;
        }
        checkWhatsPlaying();
    }

    public void onResume(org.xbmc.kore.jsonrpc.notification.Player.OnResume notification) {
        checkWhatsPlaying();
    }

    public void onPause(org.xbmc.kore.jsonrpc.notification.Player.OnPause notification) {
        checkWhatsPlaying();
    }

    public void onSpeedChanged(org.xbmc.kore.jsonrpc.notification.Player.OnSpeedChanged notification) {
        checkWhatsPlaying();
    }

    public void onSeek(org.xbmc.kore.jsonrpc.notification.Player.OnSeek notification) {
        checkWhatsPlaying();
    }

    public void onStop(org.xbmc.kore.jsonrpc.notification.Player.OnStop notification) {
        // We could directly notify that nothing is playing here, but in Kodi Leia everytime
        // there's a playlist change, onStop is triggered, which caused the UI to display
        // that nothing was being played. Checking what's playing prevents this.
        checkWhatsPlaying();
    }

    public void onAVStart(org.xbmc.kore.jsonrpc.notification.Player.OnAVStart notification) {
        checkWhatsPlaying();
    }

    public void onAVChange(org.xbmc.kore.jsonrpc.notification.Player.OnAVChange notification) {
        // Just ignore this, as it is fired by Kodi very often, and we're only
        // interested in play/resume/stop changes
    }

    /**
     * The {@link HostConnection.SystemNotificationsObserver} interface methods
     */
    public void onQuit(System.OnQuit notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.onSystemQuit();
        }
    }

    public void onRestart(System.OnRestart notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.onSystemQuit();
        }
    }

    public void onSleep(System.OnSleep notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.onSystemQuit();
        }
    }

    public void onInputRequested(Input.OnInputRequested notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.onInputRequested(notification.title, notification.type, notification.value);
        }
    }

    @Override
    public void onVolumeChanged(Application.OnVolumeChanged notification) {
        hostState.volumeMuted = notification.muted;
        hostState.volumeLevel = notification.volume;

        for (ApplicationEventsObserver observer : applicationEventsObservers) {
            observer.onApplicationVolumeChanged(notification.volume, notification.muted);
        }
    }

    @Override
    public void onPlaylistCleared(Playlist.OnClear notification) {
        if (hostState.lastGetPlaylistResults != null)
            hostState.lastGetPlaylistResults.clear();
        else
            hostState.lastGetPlaylistResults = new ArrayList<>();

        for (PlaylistEventsObserver observer : playlistEventsObservers) {
            observer.onPlaylistClear(notification.playlistId);
        }
    }

    @Override
    public void onPlaylistItemAdded(Playlist.OnAdd notification) {
        checkPlaylist();
    }

    @Override
    public void onPlaylistItemRemoved(Playlist.OnRemove notification) {
        checkPlaylist();
    }

    private void startCheckerHandler() {
        // Check if checkerHandler is already running, to prevent multiple runnables to be posted
        // when multiple observers are registered.
        if (checkerHandler.hasMessages(0))
            return;

        if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
            checkerHandler.post(tcpCheckerRunnable);
        } else {
            checkerHandler.post(httpCheckerRunnable);
        }
    }

    private void getApplicationProperties() {
        org.xbmc.kore.jsonrpc.method.Application.GetProperties getProperties =
                new org.xbmc.kore.jsonrpc.method.Application.GetProperties(org.xbmc.kore.jsonrpc.method.Application.GetProperties.VOLUME,
                                                                           org.xbmc.kore.jsonrpc.method.Application.GetProperties.MUTED);
        getProperties.execute(connection, new ApiCallback<ApplicationType.PropertyValue>() {
            @Override
            public void onSuccess(ApplicationType.PropertyValue result) {
                hostState.volumeMuted = result.muted;
                hostState.volumeLevel = result.volume;

                for (ApplicationEventsObserver observer : applicationEventsObservers) {
                    observer.onApplicationVolumeChanged(result.volume, result.muted);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Could not get application properties");
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    private boolean isCheckingPlaylist = false;
    private void checkPlaylist() {
        if (isCheckingPlaylist)
            return;

        if (HostConnection.LOG_REQUESTS) LogUtils.LOGD(TAG, "Checking playlists");
        isCheckingPlaylist = true;

        new GetPlaylist().execute(connection, new ApiCallback<ArrayList<GetPlaylist.GetPlaylistResult>>() {
            @Override
            public void onSuccess(ArrayList<GetPlaylist.GetPlaylistResult> result) {
                isCheckingPlaylist = false;

                if (result.isEmpty()) {
                    callPlaylistsOnClear(hostState.lastGetPlaylistResults);
                    hostState.lastGetPlaylistResults = result;
                    return;
                }

                if (!(hostState.lastGetPlaylistResults != null &&
                    hostState.lastGetPlaylistResults.equals(result))) {
                    for (PlaylistEventsObserver observer : playlistEventsObservers) {
                        observer.onPlaylistsAvailable(result);
                    }
                }

                // Handle cleared playlists
                if (hostState.lastGetPlaylistResults != null) {
                    for (GetPlaylist.GetPlaylistResult getPlaylistResult : result) {
                        for (int i = 0; i < hostState.lastGetPlaylistResults.size(); i++) {
                            if (getPlaylistResult.id == hostState.lastGetPlaylistResults.get(i).id) {
                                hostState.lastGetPlaylistResults.remove(i);
                                break;
                            }
                        }
                    }
                    callPlaylistsOnClear(hostState.lastGetPlaylistResults);
                }
                hostState.lastGetPlaylistResults = result;
            }

            @Override
            public void onError(int errorCode, String description) {
                isCheckingPlaylist = false;

                for (PlaylistEventsObserver observer : playlistEventsObservers) {
                    observer.onPlaylistError(errorCode, description);
                }
            }
        }, checkerHandler);
    }

    private void callPlaylistsOnClear(ArrayList<GetPlaylist.GetPlaylistResult> clearedPlaylists) {
        if (clearedPlaylists == null) return;
        for (GetPlaylist.GetPlaylistResult getPlaylistResult : clearedPlaylists) {
            for (PlaylistEventsObserver observer : playlistEventsObservers) {
                observer.onPlaylistClear(getPlaylistResult.id);
            }
        }
    }

    /**
     * Checks the connection status and notifies observers
     */
    private void checkConnectionStatus() {
        if (HostConnection.LOG_REQUESTS) LogUtils.LOGD(TAG, "Checking connection status");
        JSONRPC.Ping ping = new JSONRPC.Ping();
        ping.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                notifyConnectionStatusSucess(connectionStatusObservers);
            }

            @Override
            public void onError(int errorCode, String description) {
                notifyConnectionStatusError(errorCode, description, connectionStatusObservers);
            }
        }, checkerHandler);
    }

    private void notifyConnectionStatusSucess(List<ConnectionStatusObserver> observers) {
        // Reply if different from last result
        if (hostState.lastConnectionStatusResult != ConnectionStatusObserver.CONNECTION_SUCCESS) {
            hostState.lastConnectionStatusResult = ConnectionStatusObserver.CONNECTION_SUCCESS;
            for (final ConnectionStatusObserver observer : observers) {
                observer.onConnectionStatusSuccess();
            }
        }
    }

    private void notifyConnectionStatusError(int errorCode, String description, List<ConnectionStatusObserver> observers) {
        // Reply if different from last result
        if (hostState.lastConnectionStatusResult != ConnectionStatusObserver.CONNECTION_ERROR ||
            hostState.lastConnectionStatusErrorCode != errorCode) {
            hostState.lastConnectionStatusResult = ConnectionStatusObserver.CONNECTION_ERROR;
            hostState.lastConnectionStatusErrorCode = errorCode;
            hostState.lastConnectionStatusErrorDescription = description;
            for (final ConnectionStatusObserver observer : observers) {
                observer.onConnectionStatusError(errorCode, description);
            }
        }
    }

    /**
     * Indicator set when we are calling Kodi to check what's playing, so that we don't call it
     * while there are still pending calls
     */
    private boolean checkingWhatsPlaying = false;

    /**
     * Checks whats playing and notifies observers
     */
    private void checkWhatsPlaying() {
        // We don't properly protect this against race conditions because it's
        // not worth the trouble - we can safely call Kodi multiple times.
        if (checkingWhatsPlaying) {
            LogUtils.LOGD(TAG, "Already checking what's playing, returning");
            return;
        }
        checkingWhatsPlaying = true;
        if (HostConnection.LOG_REQUESTS) LogUtils.LOGD(TAG, "Checking what's playing");

        // Start the calls: Player.GetActivePlayers -> Player.GetProperties -> Player.GetItem
        chainCallGetActivePlayers();
    }

    /**
     * Calls Player.GetActivePlayers
     * On success chains execution to chainCallGetPlayerProperties
     */
    private void chainCallGetActivePlayers() {
        Player.GetActivePlayers getActivePlayers = new Player.GetActivePlayers();
        getActivePlayers.execute(connection, new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result) {
                if (result.isEmpty()) {
                    if (HostConnection.LOG_REQUESTS) LogUtils.LOGD(TAG, "Nothing is playing");
                    notifyNothingIsPlaying(playerEventsObservers);
                    return;
                }
                chainCallGetPlayerProperties(result.get(0));
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Notifying error");
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    /**
     * Calls Player.GetProperties
     * On success chains execution to chainCallGetItem
     */
    private void chainCallGetPlayerProperties(final PlayerType.GetActivePlayersReturnType getActivePlayersResult) {
        String[] propertiesToGet = new String[] {
                // Check is something more is needed
                PlayerType.PropertyName.SPEED,
                PlayerType.PropertyName.PERCENTAGE,
                PlayerType.PropertyName.POSITION,
                PlayerType.PropertyName.TIME,
                PlayerType.PropertyName.TOTALTIME,
                PlayerType.PropertyName.REPEAT,
                PlayerType.PropertyName.SHUFFLED,
                PlayerType.PropertyName.CURRENTAUDIOSTREAM,
                PlayerType.PropertyName.CURRENTSUBTITLE,
                PlayerType.PropertyName.AUDIOSTREAMS,
                PlayerType.PropertyName.SUBTITLES,
                PlayerType.PropertyName.PLAYLISTID,
                PlayerType.PropertyName.SUBTITLEENABLED,
                PlayerType.PropertyName.PARTYMODE,
                };

        Player.GetProperties getProperties = new Player.GetProperties(getActivePlayersResult.playerid, propertiesToGet);
        getProperties.execute(connection, new ApiCallback<PlayerType.PropertyValue>() {
            @Override
            public void onSuccess(PlayerType.PropertyValue result) {
                chainCallGetItem(getActivePlayersResult, result);
            }

            @Override
            public void onError(int errorCode, String description) {
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    /**
     * Calls Player.GetItem
     * On success notifies observers
     */
    private void chainCallGetItem(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                  final PlayerType.PropertyValue getPropertiesResult) {
//        COMMENT, LYRICS, MUSICBRAINZTRACKID, MUSICBRAINZARTISTID, MUSICBRAINZALBUMID,
//        MUSICBRAINZALBUMARTISTID, TRAILER, ORIGINALTITLE, LASTPLAYED, MPAA, COUNTRY,
//        PRODUCTIONCODE, SET, SHOWLINK, FILE,
//        ARTISTID, ALBUMID, TVSHOW_ID, SETID, WATCHEDEPISODES, DISC, TAG, GENREID,
//        ALBUMARTISTID, DESCRIPTION, THEME, MOOD, STYLE, ALBUMLABEL, SORTTITLE, UNIQUEID,
//        DATEADDED, CHANNEL, CHANNELTYPE, HIDDEN, LOCKED, CHANNELNUMBER, STARTTIME, ENDTIME,
//        EPISODEGUIDE, ORIGINALTITLE, PLAYCOUNT, PLOTOUTLINE, SET,
        String[] propertiesToGet = new String[] {
                ListType.FieldsAll.ART,
                ListType.FieldsAll.ARTIST,
                ListType.FieldsAll.ALBUMARTIST,
                ListType.FieldsAll.ALBUM,
                ListType.FieldsAll.CAST,
                ListType.FieldsAll.DIRECTOR,
                ListType.FieldsAll.DISPLAYARTIST,
                ListType.FieldsAll.DURATION,
                ListType.FieldsAll.EPISODE,
                ListType.FieldsAll.FANART,
                ListType.FieldsAll.FILE,
                ListType.FieldsAll.FIRSTAIRED,
                ListType.FieldsAll.GENRE,
                ListType.FieldsAll.IMDBNUMBER,
                ListType.FieldsAll.PLOT,
                ListType.FieldsAll.PREMIERED,
                ListType.FieldsAll.RATING,
                ListType.FieldsAll.RESUME,
                ListType.FieldsAll.RUNTIME,
                ListType.FieldsAll.SEASON,
                ListType.FieldsAll.SHOWTITLE,
                ListType.FieldsAll.STREAMDETAILS,
                ListType.FieldsAll.STUDIO,
                ListType.FieldsAll.TAGLINE,
                ListType.FieldsAll.THUMBNAIL,
                ListType.FieldsAll.TITLE,
                ListType.FieldsAll.TOP250,
                ListType.FieldsAll.TRACK,
                ListType.FieldsAll.VOTES,
                ListType.FieldsAll.WRITER,
                ListType.FieldsAll.YEAR,
                ListType.FieldsAll.DESCRIPTION,
                };
//        propertiesToGet = ListType.FieldsAll.allValues;
        Player.GetItem getItem = new Player.GetItem(getActivePlayersResult.playerid, propertiesToGet);
        getItem.execute(connection, new ApiCallback<ListType.ItemsAll>() {
            @Override
            public void onSuccess(ListType.ItemsAll result) {
                // Ok, now we got a result
                notifySomethingIsPlaying(getActivePlayersResult, getPropertiesResult, result, playerEventsObservers);
            }

            @Override
            public void onError(int errorCode, String description) {
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    // Whether to force a reply or if the results are equal to the last one, don't reply
    private boolean forceReply = false;

    /**
     * Notifies a list of observers of a connection error
     * Only notifies them if the result is different from the last one
     * @param errorCode Error code to report
     * @param description Description to report
     * @param observers List of observers
     */
    private void notifyConnectionError(final int errorCode, final String description, List<PlayerEventsObserver> observers) {
        checkingWhatsPlaying = false;
        // Reply if different from last result
        if (forceReply ||
            (hostState.lastPlayerEventsResult != PlayerEventsObserver.PLAYER_CONNECTION_ERROR) ||
            (hostState.lastPlayerEventsErrorCode != errorCode)) {
            hostState.lastPlayerEventsResult = PlayerEventsObserver.PLAYER_CONNECTION_ERROR;
            hostState.lastPlayerEventsErrorCode = errorCode;
            hostState.lastPlayerEventsErrorDescription = description;
            forceReply = false;
            // Copy list to prevent ConcurrentModificationExceptions
            List<PlayerEventsObserver> allObservers = new ArrayList<>(observers);
            for (final PlayerEventsObserver observer : allObservers) {
                notifyConnectionError(errorCode, description, observer);
            }
        }
    }

    /**
     * Notifies a specific observer of a connection error
     * Always notifies the observer, and doesn't save results in last call
     * @param errorCode Error code to report
     * @param description Description to report
     * @param observer Observers
     */
    private void notifyConnectionError(final int errorCode, final String description, PlayerEventsObserver observer) {
        observer.onPlayerConnectionError(errorCode, description);
    }

    /**
     * Nothing is playing, notify observers calling onPlayerStop
     * Only notifies them if the result is different from the last one
     * @param observers List of observers
     */
    private void notifyNothingIsPlaying(List<PlayerEventsObserver> observers) {
        checkingWhatsPlaying = false;
        // Reply if forced or different from last result
        if (forceReply ||
            (hostState.lastPlayerEventsResult != PlayerEventsObserver.PLAYER_IS_STOPPED)) {
            hostState.lastPlayerEventsResult = PlayerEventsObserver.PLAYER_IS_STOPPED;
            forceReply = false;
            // Copy list to prevent ConcurrentModificationExceptions
            List<PlayerEventsObserver> allObservers = new ArrayList<>(observers);
            for (final PlayerEventsObserver observer : allObservers) {
                notifyNothingIsPlaying(observer);
            }
        }
    }

    /**
     * Notifies a specific observer
     * Always notifies the observer, and doesn't save results in last call
     * @param observer Observer
     */
    private void notifyNothingIsPlaying(PlayerEventsObserver observer) {
        observer.onPlayerStop();
    }

    private boolean getPropertiesResultChanged(PlayerType.PropertyValue getPropertiesResult) {
        return (hostState.lastGetPropertiesResult == null) ||
               (hostState.lastGetPropertiesResult.speed != getPropertiesResult.speed) ||
               (hostState.lastGetPropertiesResult.shuffled != getPropertiesResult.shuffled) ||
               (!hostState.lastGetPropertiesResult.repeat.equals(getPropertiesResult.repeat));
    }

    private boolean getItemResultChanged(ListType.ItemsAll getItemResult) {
        return (hostState.lastGetItemResult == null) ||
               (hostState.lastGetItemResult.id != getItemResult.id) ||
               ((hostState.lastGetItemResult.label != null &&
                 !hostState.lastGetItemResult.label.equals(getItemResult.label)));
    }

    /**
     * Something is playing or paused, notify observers
     * Only notifies them if the result is different from the last one
     * @param getActivePlayersResult Previous call result
     * @param getPropertiesResult Previous call result
     * @param getItemResult Previous call result
     * @param observers List of observers
     */
    private void notifySomethingIsPlaying(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                          final PlayerType.PropertyValue getPropertiesResult,
                                          final ListType.ItemsAll getItemResult,
                                          List<PlayerEventsObserver> observers) {
        checkingWhatsPlaying = false;
        int currentCallResult = (getPropertiesResult.speed == 0) ?
                                PlayerEventsObserver.PLAYER_IS_PAUSED : PlayerEventsObserver.PLAYER_IS_PLAYING;

        if (forceReply ||
            (hostState.lastPlayerEventsResult != currentCallResult) ||
            getPropertiesResultChanged(getPropertiesResult) ||
            getItemResultChanged(getItemResult)) {
            hostState.lastPlayerEventsResult = currentCallResult;
            hostState.lastGetActivePlayerResult = getActivePlayersResult;
            hostState.lastGetPropertiesResult = getPropertiesResult;
            hostState.lastGetItemResult = getItemResult;
            forceReply = false;
            // Copy list to prevent ConcurrentModificationExceptions
            List<PlayerEventsObserver> allObservers = new ArrayList<>(observers);

            for (final PlayerEventsObserver observer : allObservers) {
                notifySomethingIsPlaying(getActivePlayersResult, getPropertiesResult, getItemResult, observer);
            }
        }

        // Workaround for when playing has started but time info isn't updated yet.
        // See https://github.com/xbmc/Kore/issues/78#issuecomment-104148064
        // If the playing time returned is 0sec, we'll schedule another check
        // to give Kodi some time to report the correct playing time
        if ((currentCallResult == PlayerEventsObserver.PLAYER_IS_PLAYING) &&
            (connection.getProtocol() == HostConnection.PROTOCOL_TCP) &&
            (getPropertiesResult.time.toSeconds() == 0)) {
            LogUtils.LOGD(TAG, "Scheduling new call to check what's playing because time is 0.");
            final int RECHECK_INTERVAL = 3000;
            checkerHandler.postDelayed(() -> {
                forceReply = true;
                checkWhatsPlaying();
            }, RECHECK_INTERVAL);
        }
    }

    /**
     * Something is playing or paused, notify a specific observer
     * Always notifies the observer, and doesn't save results in last call
     * @param getActivePlayersResult Previous call result
     * @param getPropertiesResult Previous call result
     * @param getItemResult Previous call result
     * @param observer Specific observer
     */
    private void notifySomethingIsPlaying(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                          final PlayerType.PropertyValue getPropertiesResult,
                                          final ListType.ItemsAll getItemResult,
                                          PlayerEventsObserver observer) {
        if (getPropertiesResult.speed == 0) {
            // Paused
            observer.onPlayerPause(getActivePlayersResult, getPropertiesResult, getItemResult);
        } else {
            // Playing
            observer.onPlayerPlay(getActivePlayersResult, getPropertiesResult, getItemResult);
        }
    }

    /**
     * Replies to the player observer with the last result we got.
     * If we have no result, nothing will be called on the observer interface.
     * @param observer Player observer to call with last result
     */
    private void replyWithLastResult(PlayerEventsObserver observer) {
        switch (hostState.lastPlayerEventsResult) {
            case PlayerEventsObserver.PLAYER_CONNECTION_ERROR:
                notifyConnectionError(hostState.lastPlayerEventsErrorCode, hostState.lastPlayerEventsErrorDescription, observer);
                break;
            case PlayerEventsObserver.PLAYER_IS_STOPPED:
                notifyNothingIsPlaying(observer);
                break;
            case PlayerEventsObserver.PLAYER_IS_PAUSED:
            case PlayerEventsObserver.PLAYER_IS_PLAYING:
                notifySomethingIsPlaying(hostState.lastGetActivePlayerResult, hostState.lastGetPropertiesResult, hostState.lastGetItemResult, observer);
                break;
            case PlayerEventsObserver.PLAYER_NO_RESULT:
                observer.onPlayerNoResultsYet();
                break;
        }
    }

    /**
     * Replies to the application observer with the last result we got.
     * If we have no result, nothing will be called on the observer interface.
     * @param observer Application observer to call with last result
     */
    private void replyWithLastResult(ApplicationEventsObserver observer) {
        if (hostState.volumeLevel == -1) {
            getApplicationProperties();
        } else {
            observer.onApplicationVolumeChanged(hostState.volumeLevel, hostState.volumeMuted);
        }
    }

    /**
     * Replies to the playlist observer with the last result we got.
     * If we have no result, nothing will be called on the observer interface.
     * @param observer Playlist observer to call with last result
     */
    private void replyWithLastResult(PlaylistEventsObserver observer) {
        if (hostState.lastGetPlaylistResults != null && !hostState.lastGetPlaylistResults.isEmpty())
            observer.onPlaylistsAvailable(hostState.lastGetPlaylistResults);
        else
            checkPlaylist();
    }

    /**
     * Replies to the connection status observer with the last result we got.
     * @param observer Connection Status observer to call with last result
     */
    private void replyWithLastResult(ConnectionStatusObserver observer) {
        switch (hostState.lastConnectionStatusResult) {
            case ConnectionStatusObserver.CONNECTION_ERROR:
                observer.onConnectionStatusError(hostState.lastConnectionStatusErrorCode, hostState.lastConnectionStatusErrorDescription);
                break;
            case ConnectionStatusObserver.CONNECTION_SUCCESS:
                observer.onConnectionStatusSuccess();
                break;
            case PlayerEventsObserver.PLAYER_NO_RESULT:
                observer.onConnectionStatusNoResultsYet();
                break;
        }
    }

    /**
     * Forces a refresh of the current cached results
     */
    public void refreshWhatsPlaying() {
        LogUtils.LOGD(TAG, "Forcing a refresh of what's playing");
        forceReply = true;
        checkWhatsPlaying();
    }

    public void refreshPlaylists() {
        LogUtils.LOGD(TAG, "Forcing a refresh of playlists");
        checkPlaylist();
    }
}
