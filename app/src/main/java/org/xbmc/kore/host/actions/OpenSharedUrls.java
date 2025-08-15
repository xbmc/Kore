package org.xbmc.kore.host.actions;

/*
 * This file is a part of the Kore project.
 */

import android.content.Context;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostCompositeAction;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.LogUtils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Opens or queues one or more URLs on Kodi.
 */
public class OpenSharedUrls extends HostCompositeAction<Boolean> {
    private static final String TAG = LogUtils.makeLogTag(OpenSharedUrls.class);

    private final Context context;
    private final List<String> urls;
    private final String notificationTitle;
    private final String notificationText;
    private final boolean queue;
    private final int playlistType;

    /**
     * Creates the composite action
     * @param context Context
     * @param urls The urls to play or queue
     * @param notificationTitle The title of the notification to be shown when Kodi is already playing something
     * @param notificationText The notification to be shown when Kodi is already playing something
     * @param queue Whether to open or queue the items
     * @param playlistType Playlist type to queue to
     */
    public OpenSharedUrls(Context context, List<String> urls, String notificationTitle, String notificationText, boolean queue, int playlistType) {
        this.context = context;
        this.urls = urls;
        this.notificationTitle = notificationTitle;
        this.notificationText = notificationText;
        this.queue = queue;
        this.playlistType = playlistType;
    }

    /**
     * @return whether Kodi is currently playing something. If so, the shared URLs
     * are added to the playlist and not played immediately.
     * @throws Error when any of the commands sent fails
     * @throws InterruptedException when {@code cancel(true)} is called on the resulting
     * future while waiting on one of the internal futures.
     */
    @Override
    public Boolean execInBackground() throws ExecutionException, InterruptedException {
        int stage = R.string.error_get_active_player;
        try {
            List<PlayerType.GetActivePlayersReturnType> players =
                    hostConnection.execute(new Player.GetActivePlayers())
                                  .get();
            boolean mediaIsPlaying = !players.isEmpty();

            stage = R.string.error_queue_media_file;
            if (!mediaIsPlaying) {
                LogUtils.LOGD(TAG, "Clearing playlist number " + playlistType);
                hostConnection.execute(new Playlist.Clear(playlistType))
                              .get();
            }

            if (queue) {
                LogUtils.LOGD(TAG, "Queueing " + urls.size() + " item(s)");
                for (String u : urls) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.file = u;
                    hostConnection.execute(new Playlist.Add(playlistType, item)).get();
                }

                // start playlist if nothing was alreay playing playlist; otherwise show notification
                if (!mediaIsPlaying) {
                    stage = R.string.error_play_media_file;
                    hostConnection.execute(new Player.Open(Player.Open.TYPE_PLAYLIST, playlistType)).get();
                } else {
                    // no get() to ignore the exception that will be thrown by OkHttp
                    hostConnection.execute(new Player.Notification(notificationTitle, notificationText));
                }
            } else {
                stage = R.string.error_play_media_file;
                LogUtils.LOGD(TAG, "Playing " + urls.size() + "item(s)");

                // play first url immediately, then queue the rest
                String url = urls.get(0);
                PlaylistType.Item item = new PlaylistType.Item();
                item.file = url;
                hostConnection.execute(new Player.Open(item)).get();

                // queue remaining
                for (int i = 1; i < urls.size(); i++) {
                    item = new PlaylistType.Item();
                    item.file = urls.get(i);
                    hostConnection.execute(new Playlist.Add(playlistType, item)).get();
                }
            }
            return mediaIsPlaying;
        } catch (ExecutionException e) {
            throw new ExecutionException(context.getString(stage, e.getMessage()), e.getCause());
        }
    }
}
