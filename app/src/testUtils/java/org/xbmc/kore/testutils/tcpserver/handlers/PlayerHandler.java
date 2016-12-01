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
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnPause;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnPlay;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnPropertyChanged;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnSeek;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Player.OnSpeedChanged;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;

import static org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler.TYPE.MUSIC;

/**
 * Simulates Player JSON-RPC API
 */
public class PlayerHandler implements JSONConnectionHandlerManager.ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(PlayerHandler.class);

    public enum TYPE {
        MUSIC,
        MOVIE,
        EPISODE,
        MUSICVIDEO,
        UNKNOWN,
        PICTURE,
        CHANNEL
    }

    public static String[] repeatModes = {
        "off",
        "one",
        "all"
    };

    private int currentRepeatMode;
    private boolean shuffled;
    private boolean playing;
    private int position;
    private long totalTimeSec = 240; // default value

    private TYPE mediaType = MUSIC;

    private Player.GetItem mediaItem = createSongItem();
    private String playerType = PlayerType.GetActivePlayersReturnType.AUDIO;

    private ArrayList<JsonResponse> notifications = new ArrayList<>();

    @Override
    public ArrayList<JsonResponse> getNotifications() {
        ArrayList<JsonResponse> list = new ArrayList<>(notifications);
        notifications.clear();
        return list;
    }

    @Override
    public void reset() {
        this.shuffled = false;
        this.currentRepeatMode = 0;
        this.position = 0;
        this.playing = false;
        setMediaType(MUSIC);
    }

    @Override
    public String[] getType() {
        return new String[] {Player.GetActivePlayers.METHOD_NAME,
                             Player.GetProperties.METHOD_NAME,
                             Player.GetItem.METHOD_NAME,
                             Player.SetRepeat.METHOD_NAME,
                             Player.SetShuffle.METHOD_NAME,
                             Player.Seek.METHOD_NAME,
                             Player.PlayPause.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> getResponse(String method, ObjectNode jsonRequest) {
        LogUtils.LOGD(TAG, "getResponse: method="+method);

        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();
        JsonNode node = jsonRequest.get("id");
        JsonResponse response = null;
        int playerId;
        switch (method) {
            case Player.GetActivePlayers.METHOD_NAME:
                response = new Player.GetActivePlayers(node.asInt(), 0, playerType);
                break;
            case Player.GetProperties.METHOD_NAME:
                response = updatePlayerProperties(createPlayerProperties(node.asInt()));
                break;
            case Player.GetItem.METHOD_NAME:
                mediaItem.setMethodId(node.asInt());
                response = mediaItem;
                break;
            case Player.SetRepeat.METHOD_NAME:
                response = new Player.SetRepeat(node.asInt(), "OK");
                playerId = jsonRequest.get("params").get("playerid").asInt();
                currentRepeatMode = ++currentRepeatMode % 3;
                notifications.add(new OnPropertyChanged(repeatModes[currentRepeatMode], null, playerId));
                break;
            case Player.SetShuffle.METHOD_NAME:
                response = new Player.SetShuffle(node.asInt(), "OK");
                playerId = jsonRequest.get("params").get("playerid").asInt();
                shuffled = !shuffled;
                notifications.add(new OnPropertyChanged(null, shuffled, playerId));
                break;
            case Player.PlayPause.METHOD_NAME:
                playing = !playing;
                int speed = playing ? 1 : 0;
                response = new Player.PlayPause(node.asInt(), speed);
                playerId = jsonRequest.get("params").get("playerid").asInt();
                if (playing)
                    notifications.add(new OnPlay(1580, getMediaItemType(), playerId, speed));
                else
                    notifications.add(new OnPause(1580, getMediaItemType(), playerId, speed));
                notifications.add(new OnSpeedChanged(1580, getMediaItemType(), playerId, speed));
                break;
            case Player.Seek.METHOD_NAME:
                position = new GlobalType.Time(jsonRequest.get("params").get("value")).ToSeconds();
                response = new Player.Seek(node.asInt(), (100 * position) / (double) totalTimeSec, position,
                                           totalTimeSec);
                playerId = jsonRequest.get("params").get("playerid").asInt();

                notifications.add(new OnSeek(node.asInt(), getMediaItemType(), playerId,
                                             playing ? 1 : 0, 0, position));
                break;
        }

        jsonResponses.add(response);

        return jsonResponses;
    }

    /**
     * Sets the returned media type
     * @param mediaType
     */
    public void setMediaType(TYPE mediaType) {
        switch (mediaType) {
            case MOVIE:
                mediaItem = createMovieItem();
                playerType = PlayerType.GetActivePlayersReturnType.VIDEO;
                break;
            case MUSIC:
                mediaItem = createSongItem();
                playerType = PlayerType.GetActivePlayersReturnType.AUDIO;
                break;
            case UNKNOWN:
                mediaItem = createUnknownItem();
                playerType = PlayerType.GetActivePlayersReturnType.AUDIO;
                break;
            case MUSICVIDEO:
                mediaItem = createMusicVideoItem();
                playerType = PlayerType.GetActivePlayersReturnType.VIDEO;
                break;
            case PICTURE:
                mediaItem = createPictureItem();
                playerType = PlayerType.GetActivePlayersReturnType.PICTURE;
                break;
            case CHANNEL:
                mediaItem = createChannelItem();
                playerType = PlayerType.GetActivePlayersReturnType.VIDEO;
                break;
        }
    }

    public void startPlay() {
        OnPlay onPlay = new OnPlay(1580, getMediaItemType(), 0, 1);
        notifications.add(onPlay);
        playing = true;
    }

    /**
     * Returns the current media item for the media type set through {@link #setMediaType(TYPE)}
     * @return
     */
    public Player.GetItem getMediaItem() {
        return mediaItem;
    }

    /**
     * Returns the play position of the current media item
     * @return the time elapsed in seconds
     */
    public long getPosition() {
        return position;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setTotalTimeSec(long totalTimeSec) {
        this.totalTimeSec = totalTimeSec;
    }

    private String getMediaItemType() {
        switch (mediaType) {
            case MOVIE:
                return OnPlay.TYPE_MOVIE;
            case MUSIC:
                return OnPlay.TYPE_SONG;
            case UNKNOWN:
                return OnPlay.TYPE_UNKNOWN;
            case MUSICVIDEO:
                return OnPlay.TYPE_MUSICVIDEO;
            case PICTURE:
                return OnPlay.TYPE_PICTURE;
            case CHANNEL:
                return OnPlay.TYPE_MOVIE;
            default:
                return OnPlay.TYPE_SONG;
        }
    }

    private Player.GetProperties updatePlayerProperties(Player.GetProperties playerProperties) {
        if (playing)
            position++;

        if ( ( position > totalTimeSec ) && currentRepeatMode != 0 )
            position = 0;

        playerProperties.addPosition(position);
        playerProperties.addPercentage((int) ((position * 100 ) / totalTimeSec));
        playerProperties.addTime(0, 0, position, 767);

        playerProperties.addShuffled(shuffled);
        playerProperties.addRepeat(repeatModes[currentRepeatMode]);

        return playerProperties;
    }

    private Player.GetProperties createPlayerProperties(int id) {
        Player.GetProperties properties = new Player.GetProperties(id);
        properties.addPlaylistId(0);
        properties.addRepeat(repeatModes[currentRepeatMode]);
        properties.addShuffled(false);
        properties.addSpeed(playing ? 1 : 0);
        properties.addTotaltime(0,0,240,41);
        return properties;
    }

    private Player.GetItem createSongItem() {
        Player.GetItem item = new Player.GetItem();
        item.addAlbum("My Time Is The Right Time");
        item.addAlbumArtist("Alton Ellis");
        item.addArtist("Alton Ellis");
        item.addDisplayartist("Alton Ellis");
        item.addDuration(240);
        item.addFile("/Users/martijn/Projects/dummymediafiles/media/music/Alton Ellis/My Time Is The Right Time/11-I Can't Stand It.mp3");
        item.addGenre("Reggae");
        item.addLabel("I Can't Stand It");
        item.addRating(0);
        item.addTitle("I Can't Stand It");
        item.addTrack(11);
        item.addType(Player.GetItem.TYPE.SONG);
        item.addYear(2000);

        return item;
    }

    private Player.GetItem createMovieItem() {
        Player.GetItem item = new Player.GetItem();
        item.addTitle("Elephants Dream");
        item.addCast("", "Cas Jansen", "Emo");
        item.addCast("", "Tygo Gernandt", "Proog");
        item.addDuration(660);
        item.addFile("/Users/martijn/Projects/dummymediafiles/media/movies/Elephants Dream (2006).mp4");
        item.addGenre("Animation");
        item.addRating(0);
        item.addType(Player.GetItem.TYPE.MOVIE);
        item.addYear(2006);

        return item;
    }

    private Player.GetItem createEpisodeItem() {
        Player.GetItem item = new Player.GetItem();
        item.addShowtitle("According to Jim");
        item.addCast("image://http%3a%2f%2fthetvdb.com%2fbanners%2factors%2f41995.jpg/", "James Belushi", "Jim");
        item.addCast("image://http%3a%2f%2fthetvdb.com%2fbanners%2factors%2f41994.jpg/", "Courtney Thorne-Smith", "Cheryl");
        item.addDuration(1800);
        item.addFile("/Users/martijn/Projects/dummymediafiles/media/movies/Elephants Dream (2006).mp4");
        item.addGenre("Comedy");
        item.addRating(7);
        item.addType(Player.GetItem.TYPE.EPISODE);
        item.addFirstaired("2001-10-03");
        item.addEpisode(1);
        item.addSeason(1);
        item.addDirector("Andy Cadiff");
        item.addTitle("Pilot");
        return item;
    }

    private Player.GetItem createMusicVideoItem() {
        Player.GetItem item = new Player.GetItem();
        item.addType(Player.GetItem.TYPE.MUSICVIDEO);
        item.addAlbum("...Baby One More Time");
        item.addDirector("Nigel Dick");
        item.addThumbnail("image://http%3a%2f%2fwww.theaudiodb.com%2fimages%2fmedia%2falbum%2fthumb%2fbaby-one-more-time-4dcff7453745a.jpg/");
        item.addYear(1999);
        item.addTitle("(You Drive Me) Crazy");
        item.addLabel("(You Drive Me) Crazy");
        item.addRuntime(12);
        item.addGenre("Pop");
        item.addPremiered("1999-01-01");
        return item;
    }

    private Player.GetItem createChannelItem() {
        Player.GetItem item = new Player.GetItem();
        item.addShowtitle("According to Jim");
        item.addCast("image://http%3a%2f%2fthetvdb.com%2fbanners%2factors%2f41995.jpg/", "James Belushi", "Jim");
        item.addCast("image://http%3a%2f%2fthetvdb.com%2fbanners%2factors%2f41994.jpg/", "Courtney Thorne-Smith", "Cheryl");
        item.addDuration(1800);
        item.addFile("/Users/martijn/Projects/dummymediafiles/media/movies/Elephants Dream (2006).mp4");
        item.addGenre("Comedy");
        item.addRating(7);
        item.addType(Player.GetItem.TYPE.EPISODE);
        item.addFirstaired("2001-10-03");
        item.addEpisode(1);
        item.addSeason(1);
        item.addDirector("Andy Cadiff");
        item.addTitle("Pilot");
        item.addType(Player.GetItem.TYPE.CHANNEL);

        return item;
    }

    private Player.GetItem createUnknownItem() {
        Player.GetItem item = new Player.GetItem();
        item.addTitle("Dumpert");
        item.addCast("", "Martijn Kaiser", "himself");
        item.addCast("", "", "Skipmode A1");
        item.addCast("", "", "Sparkline");
        item.addGenre("Addon");
        item.addType(Player.GetItem.TYPE.UNKNOWN);

        return item;
    }

    private Player.GetItem createPictureItem() {
        Player.GetItem item = new Player.GetItem();
        item.addTitle("Kore Artwork");
        item.addFile("/Users/martijn/Projects/Kore/art/screenshots/Kore_Artwork_Concept_2.png");
        item.addType(Player.GetItem.TYPE.PICTURE);
        return item;
    }
}
