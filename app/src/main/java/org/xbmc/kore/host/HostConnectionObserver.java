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

import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.JSONRPC;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.notification.Application;
import org.xbmc.kore.jsonrpc.notification.Player.NotificationsData;
import org.xbmc.kore.jsonrpc.notification.Input;
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
 * or, if through HTTP, starts a periodic polling of XBMC, and tries to discern when a change in
 * the player has occurred, notifying the listeners
 *
 * NOTE: An object of this class should always be called from the same thread.
 */
public class HostConnectionObserver
        implements HostConnection.PlayerNotificationsObserver,
                   HostConnection.SystemNotificationsObserver,
                   HostConnection.InputNotificationsObserver,
                   HostConnection.ApplicationNotificationsObserver {
    public static final String TAG = LogUtils.makeLogTag(HostConnectionObserver.class);

    public interface ApplicationEventsObserver {
        /**
         * Notifies the observer that volume has changed
         * @param volume
         * @param muted
         */
        public void applicationOnVolumeChanged(int volume, boolean muted);
    }

    /**
     * Interface that an observer has to implement to receive player events
     */
    public interface PlayerEventsObserver {
        /**
         * Constants for possible events. Useful to save the last event and compare with the
         * current one to check for differences
         */
        public static final int PLAYER_NO_RESULT = 0,
                PLAYER_CONNECTION_ERROR = 1,
                PLAYER_IS_PLAYING = 2,
                PLAYER_IS_PAUSED = 3,
                PLAYER_IS_STOPPED = 4;

        public void playerOnPropertyChanged(NotificationsData notificationsData);

        /**
         * Notifies that something is playing
         * @param getActivePlayerResult Active player obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetActivePlayers}
         * @param getPropertiesResult Properties obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetProperties}
         * @param getItemResult Currently playing item, obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
         */
        public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                 PlayerType.PropertyValue getPropertiesResult,
                                 ListType.ItemsAll getItemResult);

        /**
         * Notifies that something is paused
         * @param getActivePlayerResult Active player obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetActivePlayers}
         * @param getPropertiesResult Properties obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetProperties}
         * @param getItemResult Currently paused item, obtained by a call to {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
         */
        public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                  PlayerType.PropertyValue getPropertiesResult,
                                  ListType.ItemsAll getItemResult);

        /**
         * Notifies that media is stopped/nothing is playing
         */
        public void playerOnStop();

        /**
         * Called when we get a connection error
         * @param errorCode
         * @param description
         */
        public void playerOnConnectionError(int errorCode, String description);

        /**
         * Notifies that we don't have a result yet
         */
        public void playerNoResultsYet();

        /**
         * Notifies that XBMC has quit/shutdown/sleep
         */
        public void systemOnQuit();

        /**
         * Notifies that XBMC has requested input
         */
        public void inputOnInputRequested(String title, String type, String value);

        /**
         * Notifies the observer that it this is stopping
         */
        public void observerOnStopObserving();
    }

    /**
     * The connection on which to listen
     */
    private HostConnection connection;

    /**
     * The list of observers
     */
    private List<PlayerEventsObserver> playerEventsObservers = new ArrayList<PlayerEventsObserver>();
    private List<ApplicationEventsObserver> applicationEventsObservers = new ArrayList<>();

//    /**
//     * Handlers for which observer, on which to notify them
//     */
//    private Map<PlayerEventsObserver, Handler> observerHandlerMap = new HashMap<PlayerEventsObserver, Handler>();

    private Handler checkerHandler = new Handler();
    private Runnable httpPlayerCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            final int HTTP_NOTIFICATION_CHECK_INTERVAL = 3000;
            // If no one is listening to this, just exit
            if (playerEventsObservers.isEmpty()) return;

            // Check whats playing
            checkWhatsPlaying();

            // Keep checking
            checkerHandler.postDelayed(this, HTTP_NOTIFICATION_CHECK_INTERVAL);
        }
    };

    private Runnable httpApplicationCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            final int HTTP_NOTIFICATION_CHECK_INTERVAL = 3000;
            // If no one is listening to this, just exit
            if (applicationEventsObservers.isEmpty()) return;

            getApplicationProperties();

            // Keep checking
            checkerHandler.postDelayed(this, HTTP_NOTIFICATION_CHECK_INTERVAL);
        }
    };

    private Runnable tcpCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            final int PING_AFTER_ERROR_CHECK_INTERVAL = 2000,
                    PING_AFTER_SUCCESS_CHECK_INTERVAL = 10000;
            // If no one is listening to this, just exit
            if (playerEventsObservers.isEmpty() && applicationEventsObservers.isEmpty()) return;

            JSONRPC.Ping ping = new JSONRPC.Ping();
            ping.execute(connection, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Ok, we've got a ping, if there are playerEventsObservers and
                    // we were in a error or uninitialized state, update
                    if ((! playerEventsObservers.isEmpty()) &&
                        ((hostState.lastCallResult == PlayerEventsObserver.PLAYER_NO_RESULT) ||
                        (hostState.lastCallResult == PlayerEventsObserver.PLAYER_CONNECTION_ERROR))) {
                        checkWhatsPlaying();
                    }
                    checkerHandler.postDelayed(tcpCheckerRunnable, PING_AFTER_SUCCESS_CHECK_INTERVAL);
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Notify a connection error
                    notifyConnectionError(errorCode, description, playerEventsObservers);
                    checkerHandler.postDelayed(tcpCheckerRunnable, PING_AFTER_ERROR_CHECK_INTERVAL);
                }
            }, checkerHandler);
//            if ((lastCallResult == PlayerEventsObserver.PLAYER_NO_RESULT) ||
//                (lastCallResult == PlayerEventsObserver.PLAYER_CONNECTION_ERROR)) {
//                checkerHandler.postDelayed(tcpCheckerRunnable, PING_AFTER_ERROR_CHECK_INTERVAL);
//            } else {
//                checkerHandler.postDelayed(tcpCheckerRunnable, PING_AFTER_SUCCESS_CHECK_INTERVAL);
//            }
        }
    };

    public class HostState {
        private int lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
        private PlayerType.GetActivePlayersReturnType lastGetActivePlayerResult = null;
        private PlayerType.PropertyValue lastGetPropertiesResult = null;
        private ListType.ItemsAll lastGetItemResult = null;
        private boolean volumeMuted = false;
        private int volumeLevel = -1;  // -1 indicates no volumeLevel known
        private int lastErrorCode;
        private String lastErrorDescription;

        public int getVolumeLevel() {
            return volumeLevel;
        }

        public boolean isVolumeMuted() {
            return volumeMuted;
        }
    }

    public HostState hostState;

    private HostConnectionObserver() {}

    public HostConnectionObserver(HostConnection connection) {
        this.hostState = new HostState();
        this.connection = connection;
    }

    /**
     * Registers a new observer that will be notified about player events
     * @param observer Observer
     */
    public void registerPlayerObserver(PlayerEventsObserver observer, boolean replyImmediately) {
        if (this.connection == null)
            return;

        playerEventsObservers.add(observer);

        if (replyImmediately) replyWithLastResult(observer);

        if (playerEventsObservers.size() == 1) {
            // If this is the first observer, start checking through HTTP or register us
            // as a connection observer, which we will pass to the "real" observer
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.registerPlayerNotificationsObserver(this, checkerHandler);
                connection.registerSystemNotificationsObserver(this, checkerHandler);
                connection.registerInputNotificationsObserver(this, checkerHandler);
                // Start the ping checker
                checkerHandler.post(tcpCheckerRunnable);
            } else {
                checkerHandler.post(httpPlayerCheckerRunnable);
            }
        }
    }

    /**
     * Unregisters a previously registered observer
     * @param observer Observer to unregister
     */
    public void unregisterPlayerObserver(PlayerEventsObserver observer) {
        playerEventsObservers.remove(observer);
//        observerHandlerMap.remove(observer);

        LogUtils.LOGD(TAG, "Unregistering player observer " + observer.getClass().getSimpleName() +
                           ". Still got " + playerEventsObservers.size() +
                           " observers.");

        if (playerEventsObservers.isEmpty()) {
            // No more observers, so unregister us from the host connection, or stop
            // the http checker thread
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.unregisterPlayerNotificationsObserver(this);
                connection.unregisterSystemNotificationsObserver(this);
                connection.unregisterInputNotificationsObserver(this);
                checkerHandler.removeCallbacks(tcpCheckerRunnable);
            } else {
                checkerHandler.removeCallbacks(httpPlayerCheckerRunnable);
            }
            hostState.lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
        }
    }

    /**
     * Registers a new observer that will be notified about application events
     * @param observer Observer
     * @param replyImmediately
     */
    public void registerApplicationObserver(ApplicationEventsObserver observer, boolean replyImmediately) {
        if (this.connection == null)
            return;

        applicationEventsObservers.add(observer);

        if (replyImmediately) {
            if( hostState.volumeLevel == -1 ) {
                getApplicationProperties();
            } else {
                observer.applicationOnVolumeChanged(hostState.volumeLevel, hostState.volumeMuted);
            }
        }

        if (applicationEventsObservers.size() == 1) {
            // If this is the first observer, start checking through HTTP or register us
            // as a connection observer, which we will pass to the "real" observer
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.registerApplicationNotificationsObserver(this, checkerHandler);
                checkerHandler.post(tcpCheckerRunnable);
            } else {
                checkerHandler.post(httpApplicationCheckerRunnable);
            }
        }
    }

    /**
     * Unregisters a previously registered observer
     * @param observer Observer to unregister
     */
    public void unregisterApplicationObserver(ApplicationEventsObserver observer) {
        applicationEventsObservers.remove(observer);

        LogUtils.LOGD(TAG, "Unregistering application observer " + observer.getClass().getSimpleName() +
                           ". Still got " + applicationEventsObservers.size() +
                           " observers.");

        if (applicationEventsObservers.isEmpty()) {
            // No more observers, so unregister us from the host connection, or stop
            // the http checker thread
            if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
                connection.unregisterApplicationNotificationsObserver(this);
            } else {
                checkerHandler.removeCallbacks(httpApplicationCheckerRunnable);
            }
        }
    }

    /**
     * Unregisters all observers
     */
    public void stopObserving() {
        for (final PlayerEventsObserver observer : playerEventsObservers)
            observer.observerOnStopObserving();

        playerEventsObservers.clear();

        if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
            connection.unregisterPlayerNotificationsObserver(this);
            connection.unregisterSystemNotificationsObserver(this);
            connection.unregisterInputNotificationsObserver(this);
            checkerHandler.removeCallbacks(tcpCheckerRunnable);
        } else {
            checkerHandler.removeCallbacks(httpPlayerCheckerRunnable);
        }
        hostState.lastCallResult = PlayerEventsObserver.PLAYER_NO_RESULT;
    }

    @Override
    public void onPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.OnPropertyChanged notification) {
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.playerOnPropertyChanged(notification.data);
        }
    }

    /**
     * The {@link HostConnection.PlayerNotificationsObserver} interface methods
     */
    public void onPlay(org.xbmc.kore.jsonrpc.notification.Player.OnPlay notification) {
        // Just start our chain calls
        chainCallGetActivePlayers();
    }

    public void onPause(org.xbmc.kore.jsonrpc.notification.Player.OnPause
                                notification) {
        // Just start our chain calls
        chainCallGetActivePlayers();
    }

    public void onSpeedChanged(org.xbmc.kore.jsonrpc.notification.Player
                                       .OnSpeedChanged notification) {
        // Just start our chain calls
        chainCallGetActivePlayers();
    }

    public void onSeek(org.xbmc.kore.jsonrpc.notification.Player.OnSeek notification) {
        // Just start our chain calls
        chainCallGetActivePlayers();
    }

    public void onStop(org.xbmc.kore.jsonrpc.notification.Player.OnStop notification) {
        // Just start our chain calls
        notifyNothingIsPlaying(playerEventsObservers);
    }

    /**
     * The {@link HostConnection.SystemNotificationsObserver} interface methods
     */
    public void onQuit(System.OnQuit notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.systemOnQuit();
        }
    }

    public void onRestart(System.OnRestart notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.systemOnQuit();
        }
    }

    public void onSleep(System.OnSleep notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.systemOnQuit();
        }
    }

    public void onInputRequested(Input.OnInputRequested notification) {
        // Copy list to prevent ConcurrentModificationExceptions
        List<PlayerEventsObserver> allObservers = new ArrayList<>(playerEventsObservers);
        for (final PlayerEventsObserver observer : allObservers) {
            observer.inputOnInputRequested(notification.title, notification.type, notification.value);
        }
    }

    @Override
    public void onVolumeChanged(Application.OnVolumeChanged notification) {
        hostState.volumeMuted = notification.muted;
        hostState.volumeLevel = notification.volume;

        for (ApplicationEventsObserver observer : applicationEventsObservers) {
            observer.applicationOnVolumeChanged(notification.volume, notification.muted);
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
                    observer.applicationOnVolumeChanged(result.volume, result.muted);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Could not get application properties");
                notifyConnectionError(errorCode, description, playerEventsObservers);
            }
        }, checkerHandler);
    }

    /**
     * Checks whats playing and notifies observers
     */
    private void checkWhatsPlaying() {
        LogUtils.LOGD(TAG, "Checking whats playing");

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
                    LogUtils.LOGD(TAG, "Nothing is playing");
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
        String propertiesToGet[] = new String[] {
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
        // Reply if different from last result
        if (forceReply ||
            (hostState.lastCallResult != PlayerEventsObserver.PLAYER_CONNECTION_ERROR) ||
            (hostState.lastErrorCode != errorCode)) {
            hostState.lastCallResult = PlayerEventsObserver.PLAYER_CONNECTION_ERROR;
            hostState.lastErrorCode = errorCode;
            hostState.lastErrorDescription = description;
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
        observer.playerOnConnectionError(errorCode, description);
//                Handler observerHandler = observerHandlerMap.get(observer);
//                observerHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        observer.playerOnConnectionError(errorCode, description);
//                    }
//                });
    }


    /**
     * Nothing is playing, notify observers calling playerOnStop
     * Only notifies them if the result is different from the last one
     * @param observers List of observers
     */
    private void notifyNothingIsPlaying(List<PlayerEventsObserver> observers) {
        // Reply if forced or different from last result
        if (forceReply ||
            (hostState.lastCallResult != PlayerEventsObserver.PLAYER_IS_STOPPED)) {
            hostState.lastCallResult = PlayerEventsObserver.PLAYER_IS_STOPPED;
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
        observer.playerOnStop();
    }

    /**
     * Something is playing or paused, notify observers
     * Only notifies them if the result is different from the last one
     * @param getActivePlayersResult
     * @param getPropertiesResult
     * @param getItemResult
     * @param observers List of observers
     */
    private void notifySomethingIsPlaying(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                          final PlayerType.PropertyValue getPropertiesResult,
                                          final ListType.ItemsAll getItemResult,
                                          List<PlayerEventsObserver> observers) {
        int currentCallResult = (getPropertiesResult.speed == 0) ?
                PlayerEventsObserver.PLAYER_IS_PAUSED : PlayerEventsObserver.PLAYER_IS_PLAYING;
        if (forceReply ||
                (hostState.lastCallResult != currentCallResult) ||
                (hostState.lastGetPropertiesResult.speed != getPropertiesResult.speed) ||
                (hostState.lastGetPropertiesResult.shuffled != getPropertiesResult.shuffled) ||
                (!hostState.lastGetPropertiesResult.repeat.equals(getPropertiesResult.repeat)) ||
                (hostState.lastGetItemResult.id != getItemResult.id) ||
                (!hostState.lastGetItemResult.label.equals(getItemResult.label))) {
            hostState.lastCallResult = currentCallResult;
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
            (getPropertiesResult.time.ToSeconds() == 0)) {
            LogUtils.LOGD(TAG, "Scheduling new call to check what's playing.");
            final int RECHECK_INTERVAL = 3000;
            checkerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    forceReply = true;
                    checkWhatsPlaying();
                }
            }, RECHECK_INTERVAL);
        }

    }

    /**
     * Something is playing or paused, notify a specific observer
     * Always notifies the observer, and doesn't save results in last call
     * @param getActivePlayersResult
     * @param getPropertiesResult
     * @param getItemResult
     * @param observer Specific observer
     */
    private void notifySomethingIsPlaying(final PlayerType.GetActivePlayersReturnType getActivePlayersResult,
                                          final PlayerType.PropertyValue getPropertiesResult,
                                          final ListType.ItemsAll getItemResult,
                                          PlayerEventsObserver observer) {
        if (getPropertiesResult.speed == 0) {
            // Paused
            observer.playerOnPause(getActivePlayersResult, getPropertiesResult, getItemResult);
        } else {
            // Playing
            observer.playerOnPlay(getActivePlayersResult, getPropertiesResult, getItemResult);
        }
    }

    /**
     * Replies to the observer with the last result we got.
     * If we have no result, nothing will be called on the observer interface.
     * @param observer Observer to call with last result
     */
    public void replyWithLastResult(PlayerEventsObserver observer) {
        switch (hostState.lastCallResult) {
            case PlayerEventsObserver.PLAYER_CONNECTION_ERROR:
                notifyConnectionError(hostState.lastErrorCode, hostState.lastErrorDescription, observer);
                break;
            case PlayerEventsObserver.PLAYER_IS_STOPPED:
                notifyNothingIsPlaying(observer);
                break;
            case PlayerEventsObserver.PLAYER_IS_PAUSED:
            case PlayerEventsObserver.PLAYER_IS_PLAYING:
                notifySomethingIsPlaying(hostState.lastGetActivePlayerResult, hostState.lastGetPropertiesResult, hostState.lastGetItemResult, observer);
                break;
            case PlayerEventsObserver.PLAYER_NO_RESULT:
                observer.playerNoResultsYet();
                break;
        }
    }

    /**
     * Forces a refresh of the current cached results
     */
    public void forceRefreshResults() {
        forceReply = true;
        chainCallGetActivePlayers();
    }

    public HostState getHostState() {
        return hostState;
    }
}
