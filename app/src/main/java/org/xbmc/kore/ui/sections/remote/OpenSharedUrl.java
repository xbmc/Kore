package org.xbmc.kore.ui.sections.remote;

/*
 * This file is a part of the Kore project.
 */

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.LogUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Sends a series of commands to Kodi in a background thread to open the video.
 * <p>
 * This is meant to be passed to {@link HostManager#withCurrentHost(HostManager.Session)}
 * and the resulting future should be awaited in a background thread as well (if you're
 * interested in the result), either in an {@link android.os.AsyncTask} or another
 * {@link HostManager.Session}.
 */
public class OpenSharedUrl implements HostManager.Session<Boolean> {

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
     * @throws Error when any of the commands sent fails
     * @throws InterruptedException when {@code cancel(true)} is called on the resulting
     * future while waiting on one of the internal futures.
     */
    @Override
    public Boolean using(HostConnection host) throws Error, InterruptedException {
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
        } catch (ExecutionException e) {
            throw new Error(stage, e.getCause());
        } catch (RuntimeException e) {
            throw new Error(stage, e);
        }
    }
}
