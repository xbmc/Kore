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
import android.view.View;
import android.widget.RemoteViews;

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
    public static final String NOTIFICATION_CHANNEL = "KORE";

    private PendingIntent mRemoteStartPendingIntent;
    private Service mService;

    private Notification mNothingPlayingNotification;


    public NotificationObserver(Service service) {
        this.mService = service;

        // Create the intent to start the remote when the user taps the notification
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mService);
        stackBuilder.addParentStack(RemoteActivity.class);
        stackBuilder.addNextIntent(new Intent(mService, RemoteActivity.class));
        mRemoteStartPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the notification channel
        if (Utils.isOreoOrLater()) {
            buildNotificationChannel();
        }
        mNothingPlayingNotification = buildNothingPlayingNotification();
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
                        mService.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        NotificationManager notificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    // Picasso target that will be used to load images
    private static Target picassoTarget = null;

    private Notification buildNothingPlayingNotification() {
        int smallIcon = R.drawable.ic_devices_white_24dp;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL);
        return builder
                .setSmallIcon(smallIcon)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(mRemoteStartPendingIntent)
                .setContentTitle(String.format(mService.getString(R.string.connected_to),
                        HostManager.getInstance(mService).getHostInfo().getName()))
                .setContentText(mService.getString(R.string.nothing_playing))
                .build();
    }

    public Notification getNothingPlayingNotification() {
        if (mNothingPlayingNotification == null) {
            mNothingPlayingNotification = buildNothingPlayingNotification();
        }
        return mNothingPlayingNotification;
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
                String seasonEpisode = String.format(mService.getString(R.string.season_episode_abbrev),
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
                .getDefaultSharedPreferences(this.mService)
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

        // Setup the collpased and expanded notifications
        final RemoteViews collapsedRV = new RemoteViews(mService.getPackageName(), R.layout.notification_colapsed);
        collapsedRV.setImageViewResource(R.id.rewind, rewindIcon);
        collapsedRV.setOnClickPendingIntent(R.id.rewind, rewindPendingIntent);
        collapsedRV.setImageViewResource(R.id.play, playPauseIcon);
        collapsedRV.setOnClickPendingIntent(R.id.play, playPausePendingIntent);
        collapsedRV.setImageViewResource(R.id.fast_forward, ffIcon);
        collapsedRV.setOnClickPendingIntent(R.id.fast_forward, ffPendingIntent);
        collapsedRV.setTextViewText(R.id.title, title);
        collapsedRV.setTextViewText(R.id.text2, underTitle);

        final RemoteViews expandedRV = new RemoteViews(mService.getPackageName(), R.layout.notification_expanded);
        expandedRV.setImageViewResource(R.id.rewind, rewindIcon);
        expandedRV.setOnClickPendingIntent(R.id.rewind, rewindPendingIntent);
        expandedRV.setImageViewResource(R.id.play, playPauseIcon);
        expandedRV.setOnClickPendingIntent(R.id.play, playPausePendingIntent);
        expandedRV.setImageViewResource(R.id.fast_forward, ffIcon);
        expandedRV.setOnClickPendingIntent(R.id.fast_forward, ffPendingIntent);
        expandedRV.setTextViewText(R.id.title, title);
        expandedRV.setTextViewText(R.id.text2, underTitle);
        final int expandedIconResId;
        if (isVideo) {
            expandedIconResId = R.id.icon_slim;
            expandedRV.setViewVisibility(R.id.icon_slim, View.VISIBLE);
            expandedRV.setViewVisibility(R.id.icon_square, View.GONE);
        } else {
            expandedIconResId = R.id.icon_square;
            expandedRV.setViewVisibility(R.id.icon_slim, View.GONE);
            expandedRV.setViewVisibility(R.id.icon_square, View.VISIBLE);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL);
        final Notification notification = builder
                .setSmallIcon(smallIcon)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(mRemoteStartPendingIntent)
                .setContent(collapsedRV)
                .build();

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
        Resources resources = mService.getResources();
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
                    CharacterDrawable avatarDrawable = UIUtils.getCharacterAvatar(mService, title);
                    showNotification(Utils.drawableToBitmap(avatarDrawable, posterWidth, posterHeight));
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) { }

                private void showNotification(Bitmap bitmap) {
                    collapsedRV.setImageViewBitmap(R.id.icon, bitmap);
                    if (Utils.isJellybeanOrLater()) {
                        notification.bigContentView = expandedRV;
                        expandedRV.setImageViewBitmap(expandedIconResId, bitmap);
                    }

                    NotificationManager notificationManager =
                            (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, notification);
                    picassoTarget = null;
                }
            };

            // Load the image
            HostManager hostManager = HostManager.getInstance(mService);
            hostManager.getPicasso()
                    .load(hostManager.getHostInfo().getImageUrl(poster))
                    .resize(posterWidth, posterHeight)
                    .into(picassoTarget);
        }
    }

    private PendingIntent buildActionPendingIntent(int playerId, String action) {
        Intent intent = new Intent(mService, IntentActionsService.class)
                .setAction(action)
                .putExtra(IntentActionsService.EXTRA_PLAYER_ID, playerId);

        return PendingIntent.getService(mService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void removeNotification() {
        NotificationManager  notificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void notifyNothingPlaying() {
        NotificationManager notificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mNothingPlayingNotification);
    }
}
