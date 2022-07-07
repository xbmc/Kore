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

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Playlist;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnAVStart;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnPause;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnPlay;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnPropertyChanged;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnSeek;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnSpeedChanged;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnStop;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler.PLAY_STATE.PAUSED;
import static org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler.PLAY_STATE.PLAYING;
import static org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler.PLAY_STATE.STOPPED;

/**
 * Simulates Player JSON-RPC API
 */
public class PlayerHandler extends ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(PlayerHandler.class);

    public static String[] repeatModes = {
            "off",
            "one",
            "all"
    };

    public enum PLAY_STATE {PLAYING, STOPPED, PAUSED}
    private PLAY_STATE playState = STOPPED;
    private int currentRepeatMode;
    private boolean shuffled;
    private int elapsedTime;

    private Player.GetItem mediaItem;
    private List<PlaylistHolder> playlists = new ArrayList<>();
    private Playlist.playlistID activePlaylistId = Playlist.playlistID.AUDIO;
    private String playerType = PlayerType.GetActivePlayersReturnType.AUDIO;

    @Override
    public void reset() {
        super.reset();
        this.shuffled = false;
        this.currentRepeatMode = 0;
        this.elapsedTime = 0;
        this.playState = STOPPED;
        playerType = PlayerType.GetActivePlayersReturnType.AUDIO;
        playlists = null;
        setMediaType(Player.GetItem.TYPE.unknown);
    }

    @Override
    public String[] getType() {
        return new String[] {Player.GetActivePlayers.METHOD_NAME,
                Player.GetProperties.METHOD_NAME,
                Player.GetItem.METHOD_NAME,
                Player.SetRepeat.METHOD_NAME,
                Player.SetShuffle.METHOD_NAME,
                Player.Seek.METHOD_NAME,
                Player.PlayPause.METHOD_NAME,
                Player.Stop.METHOD_NAME,
                Player.Open.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> createResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();
        JsonResponse response = null;

        int methodId = jsonRequest.get("id").asInt();

        switch (method) {
            case Player.GetActivePlayers.METHOD_NAME:
                response = handleGetActivePlayers(methodId);
                break;
            case Player.GetProperties.METHOD_NAME:
                response = updatePlayerProperties(createPlayerProperties(methodId));
                break;
            case Player.GetItem.METHOD_NAME:
                response = handleGetItem(methodId);
                break;
            case Player.SetRepeat.METHOD_NAME:
                response = handleSetRepeat(methodId, jsonRequest);
                break;
            case Player.SetShuffle.METHOD_NAME:
                response = handleSetShuffle(methodId, jsonRequest);
                break;
            case Player.Open.METHOD_NAME:
                response = handleOpen(methodId, jsonRequest);
                break;
            case Player.PlayPause.METHOD_NAME:
                response = handlePlayPause(methodId, jsonRequest);
                break;
            case Player.Seek.METHOD_NAME:
                response = handleSeek(methodId, jsonRequest);
                break;
            case Player.Stop.METHOD_NAME:
                handleStop();
                break;
            default:
                LogUtils.LOGD(TAG, "getResponse: unknown method received: "+method);
        }

        if (response != null)
            jsonResponses.add(response);

        return jsonResponses;
    }

    private void setMediaType(Player.GetItem.TYPE mediaType) {
        switch (mediaType) {
            case movie:
                playerType = PlayerType.GetActivePlayersReturnType.VIDEO;
                break;
            case song:
                playerType = PlayerType.GetActivePlayersReturnType.AUDIO;
                break;
            case unknown:
                playerType = PlayerType.GetActivePlayersReturnType.AUDIO;
                break;
            case musicvideo:
                playerType = PlayerType.GetActivePlayersReturnType.VIDEO;
                break;
            case picture:
                playerType = PlayerType.GetActivePlayersReturnType.PICTURE;
                break;
            case channel:
                playerType = PlayerType.GetActivePlayersReturnType.VIDEO;
                break;
        }
    }

    /**
     * Starts playing current item in the playlist
     */
    public void startPlay() {
        if (playlists != null && playlists.size() > 0 && activePlaylistId != null) {
            mediaItem = playlists.get(activePlaylistId.ordinal()).getCurrentItem();

            if (mediaItem != null) {
                setMediaType(Player.GetItem.TYPE.valueOf(getMediaItemType()));

                addNotification(new OnPlay(mediaItem.getLibraryId(), getMediaItemType(), getPlayerId(), 1));
                addNotification(new OnAVStart(mediaItem.getLibraryId(), getMediaItemType(), getPlayerId(), 1));
                if (playState == PAUSED) {
                    addNotification(new OnSpeedChanged(mediaItem.getLibraryId(), getMediaItemType(), getPlayerId(), 1));
                }

                playState = PLAYING;
            }
        }
    }

    public void startPlay(Playlist.playlistID playlistId, int playlistPosition) {
        if (playlists == null) return;

        activePlaylistId = playlistId;

        PlaylistHolder playlistHolder = playlists.get(playlistId.ordinal());
        playlistHolder.setPlaylistIndex(playlistPosition);

        startPlay();
    }

    public void stopPlay() {
        handleStop();
        addNotification(new OnStop(mediaItem.getLibraryId(), getMediaItemType(), false));
        this.playState = STOPPED;
        mediaItem = null;
    }

    public void setPlaylists(List<PlaylistHolder> playlists) {
        this.playlists = playlists;
    }

    /**
     * Returns the current media item for the media type set through {@link #setMediaType(Player.GetItem.TYPE)}
     * @return
     */
    public Player.GetItem getMediaItem() {
        return mediaItem;
    }

    /**
     * Returns the play position of the current media item
     * @return the time elapsed in seconds
     */
    public long getTimeElapsed() {
        return elapsedTime;
    }

    public PLAY_STATE getPlayState() {
        return playState;
    }

    private String getMediaItemType() {
        return mediaItem.getType();
    }

    private int getPlayerId() {
        switch (playerType) {
            case PlayerType.GetActivePlayersReturnType.VIDEO:
                return 0;
            case PlayerType.GetActivePlayersReturnType.AUDIO:
                return 1;
            case PlayerType.GetActivePlayersReturnType.PICTURE:
                return 2;
            default:
                return 1;
        }
    }

    private Player.GetProperties updatePlayerProperties(Player.GetProperties playerProperties) {
        if (playState == PLAYING)
            elapsedTime++;

        if ( mediaItem != null ) {
            if ( elapsedTime > mediaItem.getDuration() && currentRepeatMode != 0 ) {
                elapsedTime = 0;
            }

            playerProperties.addPercentage((elapsedTime * 100 ) / mediaItem.getDuration());
        }

        playerProperties.addPosition(elapsedTime);
        playerProperties.addTime(0, 0, elapsedTime, 767);

        playerProperties.addShuffled(shuffled);
        playerProperties.addRepeat(repeatModes[currentRepeatMode]);

        playerProperties.addPlaylistId(activePlaylistId.ordinal());

        return playerProperties;
    }

    private Player.GetProperties createPlayerProperties(int id) {
        Player.GetProperties properties = new Player.GetProperties(id);
        properties.addPlaylistId(activePlaylistId.ordinal());
        properties.addRepeat(repeatModes[currentRepeatMode]);
        properties.addShuffled(false);
        properties.addSpeed(playState == PLAYING ? 1 : 0);

        int duration = mediaItem != null ? mediaItem.getDuration() : 0;
        int hours = duration / 3600;
        int remainder = (duration - (hours * 3600));
        int minutes =  remainder / 60;
        int seconds = remainder - (minutes * 60);
        properties.addTotaltime(hours,minutes, seconds,0);

        return properties;
    }

    private JsonResponse handleGetItem(int methodId) {
        if (playlists != null && playlists.size() > 0) {
            mediaItem = playlists.get(activePlaylistId.ordinal()).getCurrentItem();
        }

        try {
            mediaItem = new Player.GetItem(methodId, mediaItem.toJsonString());
        } catch (IOException e) {
            LogUtils.LOGE(TAG, "handleGetItem: Error creating new Player.GetItem object");
        }
        return mediaItem;
    }

    private JsonResponse handleGetActivePlayers(int methodId) {
        if (playState == STOPPED) {
            return new Player.GetActivePlayers(methodId);
        } else {
            return new Player.GetActivePlayers(methodId, getPlayerId(), playerType);
        }
    }

    private JsonResponse handleSetRepeat(int methodId, ObjectNode jsonRequest) {
        int playerId = getPlayerIdFromJsonRequest(jsonRequest);
        currentRepeatMode = ++currentRepeatMode % 3;
        addNotification(new OnPropertyChanged(repeatModes[currentRepeatMode], null, playerId));
        return new Player.SetRepeat(methodId, "OK");
    }

    private JsonResponse handleSetShuffle(int methodId, ObjectNode jsonRequest) {
        int playerId = getPlayerIdFromJsonRequest(jsonRequest);
        shuffled = !shuffled;
        addNotification(new OnPropertyChanged(null, shuffled, playerId));
        return new Player.SetShuffle(methodId, "OK");
    }

    private JsonResponse handleOpen(int methodId, ObjectNode jsonRequest) {
        int playlistId = jsonRequest.get("params").get("item").get("playlistid").asInt();
        int playlistIndex = jsonRequest.get("params").get("item").get("position").asInt();

        startPlay(Playlist.playlistID.values()[playlistId], playlistIndex);

        return new Player.Open(methodId);
    }

    private JsonResponse handlePlayPause(int methodId, ObjectNode jsonRequest) {
        playState = playState == PLAYING ? PAUSED : PLAYING; //toggle playstate

        int speed = playState == PLAYING ? 1 : 0;
        int itemId = mediaItem.getLibraryId();
        int playerId = getPlayerIdFromJsonRequest(jsonRequest);

        if (playState == PLAYING)
            addNotification(new OnPlay(itemId, getMediaItemType(), playerId, speed));
        else
            addNotification(new OnPause(itemId, getMediaItemType(), playerId, speed));

        addNotification(new OnSpeedChanged(itemId, getMediaItemType(), playerId, speed));

        return new Player.PlayPause(methodId, speed);
    }

    private JsonResponse handleSeek(int methodId, ObjectNode jsonRequest) {
        if (mediaItem == null)
            return new Player.Seek(methodId, 0, 0, 0);

        elapsedTime = new GlobalType.Time(jsonRequest.get("params").get("value")).toSeconds();
        int playerId = getPlayerIdFromJsonRequest(jsonRequest);

        addNotification(new OnSeek(methodId, getMediaItemType(), playerId,
                playState == PLAYING ? 1 : 0, 0, elapsedTime));
        return new Player.Seek(methodId, (100 * elapsedTime) / (double) mediaItem.getDuration(),
                               elapsedTime, mediaItem.getDuration());
    }

    private void handleStop() {
        addNotification(new OnStop(mediaItem.getLibraryId(), getMediaItemType(), false));
        playState = STOPPED;
    }

    private int getPlayerIdFromJsonRequest(ObjectNode jsonRequest) {
        return jsonRequest.get("params").get("playerid").asInt();
    }
}
