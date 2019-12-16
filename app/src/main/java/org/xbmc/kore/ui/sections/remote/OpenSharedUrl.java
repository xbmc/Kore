package org.xbmc.kore.ui.sections.remote;

/*
 * This file is a part of the Kore project.
 */

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.LogUtils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Sends a series of commands to Kodi in a background thread to open the video.
 * <p>
 * This is meant to be passed to {@link java.util.concurrent.Executor}
 * and the resulting future should be awaited in a background thread as well (if you're
 * interested in the result).
 */
public class OpenSharedUrl implements Callable<Boolean> {

    /**
     * Indicates the stage where the error happened.
     */
    public static class Error extends Exception {
        public final int stage;

        public Error(int stage, Throwable cause) {
            super(cause);
            this.stage = stage;
        }
    }

    private static final String TAG = LogUtils.makeLogTag(OpenSharedUrl.class);
    private final HostConnection hostConnection;
    private final String url;
    private final String notificationTitle;
    private final String notificationText;
    private boolean queue;
    private int playlistType;

    /**
     * @param url The url to play
     * @param notificationTitle The title of the notification to be shown when the
     *                          host is currently playing a video
     * @param notificationText The notification to be shown when the host is currently
     *                         playing a video
     */
    public OpenSharedUrl(HostConnection hostConnection, String url, String notificationTitle, String notificationText, boolean queue, int playlistType) {
        this.hostConnection = hostConnection;
        this.url = url;
        this.notificationTitle = notificationTitle;
        this.notificationText = notificationText;
        this.queue = queue;
        this.playlistType = playlistType;
    }

    /**
     * @return whether the host is currently playing a video. If so, the shared url
     * is added to the playlist and not played immediately.
     * @throws Error when any of the commands sent fails
     * @throws InterruptedException when {@code cancel(true)} is called on the resulting
     * future while waiting on one of the internal futures.
     */
    @Override
    public Boolean call() throws Error, InterruptedException {
        int stage = R.string.error_get_active_player;
        try {
            List<PlayerType.GetActivePlayersReturnType> players =
                    hostConnection.execute(new Player.GetActivePlayers())
                        .get();
            boolean mediaIsPlaying = false;
            for (PlayerType.GetActivePlayersReturnType player : players) {
                if (player.type.equals(PlayerType.GetActivePlayersReturnType.VIDEO)) {
                    mediaIsPlaying = true;
                    break;
                }
            }

            stage = R.string.error_queue_media_file;
            if (!mediaIsPlaying) {
                LogUtils.LOGD(TAG, "Clearing playlist number " + playlistType);
                hostConnection.execute(new Playlist.Clear(playlistType))
                    .get();
            }

            if (queue) {
                // Queue media file to playlist:

                LogUtils.LOGD(TAG, "Queueing file");
                PlaylistType.Item item = new PlaylistType.Item();
                item.file = url;
                hostConnection.execute(new Playlist.Add(playlistType, item))
                        .get();

                if (!mediaIsPlaying) {
                    stage = R.string.error_play_media_file;
                    hostConnection.execute(new Player.Open(Player.Open.TYPE_PLAYLIST, playlistType))
                            .get();
                } else {
                    // no get() to ignore the exception that will be thrown by OkHttp
                    hostConnection.execute(new Player.Notification(notificationTitle, notificationText));
                }
            } else {
                // Don't queue, just play the media file directly:
                PlaylistType.Item item = new PlaylistType.Item();
                item.file = url;
                hostConnection.execute(new Player.Open(item));
            }

            return mediaIsPlaying;
        } catch (ExecutionException e) {
            throw new Error(stage, e.getCause());
        } catch (RuntimeException e) {
            throw new Error(stage, e);
        }
    }
}
