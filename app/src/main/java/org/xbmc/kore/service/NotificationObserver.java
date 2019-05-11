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
package org.xbmc.kore.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.notification.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.CharacterDrawable;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * This class mantains a notification on the notification area while something is playing.
 * It is meant to be used in conjunction with {@link ConnectionObserversManagerService},
 * which should create an instance of this and manage it
 */
public class NotificationObserver
        implements HostConnectionObserver.PlayerEventsObserver {
    public static final String TAG = LogUtils.makeLogTag(NotificationObserver.class);

    public static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL = "KORE";

    private PendingIntent remoteStartPendingIntent;
    private Service service;

    private Notification nothingPlayingNotification;
    private Notification currentNotification = null;

    public NotificationObserver(Service service) {
        this.service = service;

        // Create the intent to start the remote when the user taps the notification
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.service);
        stackBuilder.addParentStack(RemoteActivity.class);
        stackBuilder.addNextIntent(new Intent(this.service, RemoteActivity.class));
        remoteStartPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the notification channel
        if (Utils.isOreoOrLater()) {
            buildNotificationChannel();
        }
        nothingPlayingNotification = buildNothingPlayingNotification();
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
        notifyPlaying(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        notifyPlaying(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnStop() {
        notifyNothingPlaying();
    }

    public void playerNoResultsYet() {
        notifyNothingPlaying();
    }

    public void playerOnConnectionError(int errorCode, String description) {
        removeNotification();
    }

    public void systemOnQuit() {
        removeNotification();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) { }

    public void observerOnStopObserving() {
        // Called when the user changes host
        removeNotification();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void buildNotificationChannel() {

        NotificationChannel channel =
                new NotificationChannel(NOTIFICATION_CHANNEL,
                        service.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        NotificationManager notificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.createNotificationChannel(channel);
    }

    // Picasso target that will be used to load images
    private static Target picassoTarget = null;

    private Notification buildNothingPlayingNotification() {
        int smallIcon = R.drawable.ic_devices_white_24dp;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL);
        return builder
                .setSmallIcon(smallIcon)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(remoteStartPendingIntent)
                .setContentTitle(String.format(service.getString(R.string.connected_to),
                        HostManager.getInstance(service).getHostInfo().getName()))
                .setContentText(service.getString(R.string.nothing_playing))
                .build();
    }

    public Notification getCurrentNotification() {
        if (currentNotification == null) {
            if (nothingPlayingNotification == null) {
                nothingPlayingNotification = buildNothingPlayingNotification();
            }
            currentNotification = nothingPlayingNotification;
        }
        return currentNotification;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void notifyPlaying(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                               PlayerType.PropertyValue getPropertiesResult,
                               ListType.ItemsAll getItemResult) {
        final String title, underTitle, poster;
        int smallIcon, playPauseIcon, rewindIcon, ffIcon;

        boolean isVideo = ((getItemResult.type.equals(ListType.ItemsAll.TYPE_MOVIE)) ||
                (getItemResult.type.equals(ListType.ItemsAll.TYPE_EPISODE)));

        switch (getItemResult.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                title = getItemResult.title;
                underTitle = getItemResult.tagline;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_movie_white_24dp;
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                title = getItemResult.title;
                String seasonEpisode = String.format(service.getString(R.string.season_episode_abbrev),
                                                     getItemResult.season, getItemResult.episode);
                underTitle = String.format("%s | %s", getItemResult.showtitle, seasonEpisode);
                poster = getItemResult.art.poster;
                smallIcon = R.drawable.ic_tv_white_24dp;
                break;
            case ListType.ItemsAll.TYPE_SONG:
                title = getItemResult.title;
                underTitle = getItemResult.displayartist + " | " + getItemResult.album;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_headset_white_24dp;
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                title = getItemResult.title;
                underTitle = Utils.listStringConcat(getItemResult.artist, ", ") + " | " + getItemResult.album;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_headset_white_24dp;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                title = getItemResult.label;
                underTitle = getItemResult.title;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_dvr_white_24dp;
                break;
            default:
                title = getItemResult.label;
                underTitle = getItemResult.title;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_devices_white_24dp;
                break;
        }

        switch (getPropertiesResult.speed) {
            case 1:
                playPauseIcon = R.drawable.ic_pause_white_24dp;
                break;
            default:
                playPauseIcon = R.drawable.ic_play_arrow_white_24dp;
                break;
        }

        // Create the actions, depending on the type of media and the user's preference
        PendingIntent rewindPendingIntent, ffPendingIntent, playPausePendingIntent;
        playPausePendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_PLAY_PAUSE);
        boolean useSeekJump = PreferenceManager
                .getDefaultSharedPreferences(this.service)
                .getBoolean(Settings.KEY_PREF_NOTIFICATION_SEEK_JUMP, Settings.DEFAULT_PREF_NOTIFICATION_SEEK_JUMP);
        if (getItemResult.type.equals(ListType.ItemsAll.TYPE_SONG)) {
            rewindPendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_PREVIOUS);
            rewindIcon = R.drawable.ic_skip_previous_white_24dp;
            ffPendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_NEXT);
            ffIcon = R.drawable.ic_skip_next_white_24dp;
        } else if (useSeekJump) {
            rewindPendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_JUMP_BACKWARD);
            rewindIcon = R.drawable.ic_skip_backward_white_24dp;
            ffPendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_JUMP_FORWARD);
            ffIcon = R.drawable.ic_skip_forward_white_24dp;
        } else {
            rewindPendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_REWIND);
            rewindIcon = R.drawable.ic_fast_rewind_white_24dp;
            ffPendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_FAST_FORWARD);
            ffIcon = R.drawable.ic_fast_forward_white_24dp;
        }

        final NotificationCompat.Builder builder =
            new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(smallIcon)
                .setShowWhen(false)
                .setOngoing(true)
                .addAction(rewindIcon, service.getString(R.string.rewind), rewindPendingIntent) // #0
                .addAction(playPauseIcon, service.getString(R.string.play), playPausePendingIntent)  // #1
                .addAction(ffIcon, service.getString(R.string.fast_forward), ffPendingIntent)     // #2
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                              .setShowActionsInCompactView(0, 1, 2))
                .setContentIntent(remoteStartPendingIntent)
                .setContentTitle(title)
                .setContentText(underTitle);

        // This is a convoluted way of loading the image and showing the
        // notification, but it's what works with Picasso and is efficient.
        // Here's what's going on:
        //
        // 1. The image is loaded asynchronously into a Target, and only after
        // it is loaded is the notification shown. Using targets is a lot more
        // efficient than letting Picasso load it directly into the
        // notification imageview, which causes a lot of flickering
        //
        // 2. The target needs to be static, because Picasso only keeps a weak
        // reference to it, so we need to keed a strong reference and reset it
        // to null when we're done. We also need to check if it is not null in
        // case a previous request hasn't finished yet.
        //
        // 3. We can only show the notification after the bitmap is loaded into
        // the target, so it is done in the callback
        //
        // 4. We specifically resize the image to the same dimensions used in
        // the remote, so that Picasso reuses it in the remote and here from the cache
        Resources resources = service.getResources();
        final int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
        final int posterHeight = isVideo?
                resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height):
                posterWidth;
        if (picassoTarget == null ) {
            picassoTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    showNotification(bitmap);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    CharacterDrawable avatarDrawable = UIUtils.getCharacterAvatar(service, title);
                    showNotification(Utils.drawableToBitmap(avatarDrawable, posterWidth, posterHeight));
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) { }

                private void showNotification(Bitmap bitmap) {
                    builder.setLargeIcon(bitmap);
                    NotificationManager notificationManager =
                            (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        currentNotification = builder.build();
                        notificationManager.notify(NOTIFICATION_ID, currentNotification);
                    }
                    picassoTarget = null;
                }
            };

            // Load the image
            HostManager hostManager = HostManager.getInstance(service);
            hostManager.getPicasso()
                    .load(hostManager.getHostInfo().getImageUrl(poster))
                    .resize(posterWidth, posterHeight)
                    .into(picassoTarget);
        }
    }

    private PendingIntent buildActionPendingIntent(int playerId, String action) {
        Intent intent = new Intent(service, IntentActionsService.class)
                .setAction(action)
                .putExtra(IntentActionsService.EXTRA_PLAYER_ID, playerId);

        return PendingIntent.getService(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void removeNotification() {
        NotificationManager notificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
            currentNotification = null;
        }
    }

    private void notifyNothingPlaying() {
        NotificationManager notificationManager =
            (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, nothingPlayingNotification);
            currentNotification = nothingPlayingNotification;
        }
    }
}
