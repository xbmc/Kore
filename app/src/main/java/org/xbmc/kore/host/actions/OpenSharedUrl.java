/**
 * Copyright 2017 XBMC Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.host.actions;

import org.xbmc.kore.MainApp;
import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.LogUtils;

import java.util.List;

/**
 * Sends a series of commands to Kodi in a background thread to open the video.
 */
public class OpenSharedUrl extends HostAction<Boolean> {

    private static final String TAG = LogUtils.makeLogTag(OpenSharedUrl.class);
    private final String pluginUrl;
    private final String notificationTitle;
    private final String notificationText;

    /**
     * @param pluginUrl The url to play
     * @param notificationTitle The title of the notification to be shown when the
     *                          host is currently playing a video
     * @param notificationText The notification to be shown when the host is currently
     *                         playing a video
     */
    public OpenSharedUrl(String pluginUrl, String notificationTitle, String notificationText) {
        this.pluginUrl = pluginUrl;
        this.notificationTitle = notificationTitle;
        this.notificationText = notificationText;
    }

    /**
     * @param host The host to send the commands to
     * @return whether the host is currently playing a video. If so, the shared url
     * is added to the playlist and not played immediately.
     * @throws ApiException when any of the commands sent fails
     */
    @Override
    public Boolean using(HostConnection host) throws ApiException {
        int stage = R.string.error_get_active_player;
        try {
            List<PlayerType.GetActivePlayersReturnType> players =
                    host.execute(new Player.GetActivePlayers()).get();
            boolean videoIsPlaying = false;
            for (PlayerType.GetActivePlayersReturnType player : players) {
                if (player.type.equals(PlayerType.GetActivePlayersReturnType.VIDEO)) {
                    videoIsPlaying = true;
                    break;
                }
            }

            stage = R.string.error_queue_media_file;
            if (!videoIsPlaying) {
                LogUtils.LOGD(TAG, "Clearing video playlist");
                host.execute(new Playlist.Clear(PlaylistType.VIDEO_PLAYLISTID)).get();
            }

            LogUtils.LOGD(TAG, "Queueing file");
            PlaylistType.Item item = new PlaylistType.Item();
            item.file = pluginUrl;
            host.execute(new Playlist.Add(PlaylistType.VIDEO_PLAYLISTID, item)).get();

            if (!videoIsPlaying) {
                stage = R.string.error_play_media_file;
                host.execute(new Player
                        .Open(Player.Open.TYPE_PLAYLIST, PlaylistType.VIDEO_PLAYLISTID)).get();
            } else {
                // no get() to ignore the exception that will be thrown by OkHttp
                host.execute(new Player.Notification(notificationTitle, notificationText));
            }

            return videoIsPlaying;
        } catch (RuntimeException|InterruptedException e) {
            throw new ApiException(ApiException.API_ERROR, MainApp.getContext().getString(stage));
        }
    }
}
