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
package com.syncedsynapse.kore2.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
import android.widget.RemoteViews;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostConnectionObserver;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.type.ListType;
import com.syncedsynapse.kore2.jsonrpc.type.PlayerType;
import com.syncedsynapse.kore2.ui.RemoteActivity;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.Utils;

/**
 * This service maintains a notification in the notification area while
 * something is playing, and keeps running while it is playing.
 * This service stops itself as soon as the playing stops or there's no
 * connection. Thus, this should only be started if something is already
 * playing, otherwise it will shutdown automatically.
 * It doesn't try to mirror Kodi's state at all times, because that would
 * imply running at all times which can be resource consuming.
 *
 * A {@link HostConnectionObserver} singleton is used to keep track of Kodi's
 * state. This singleton should be the same as used in the app's activities
 */
public class NotificationService extends Service
        implements HostConnectionObserver.PlayerEventsObserver {
    public static final String TAG = LogUtils.makeLogTag(NotificationService.class);

    private static final int NOTIFICATION_ID = 1;

    private HostConnectionObserver mHostConnectionObserver = null;

    private PendingIntent mRemoteStartPendingIntent;

    @Override
    public void onCreate() {
        // We do not create any thread because all the works is supposed to
        // be done on the main thread, so that the connection observer
        // can be shared with the app, and notify it on the UI thread
        LogUtils.LOGD(TAG, "onCreate");

        // Create the intent to start the remote when the user taps the notification
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(RemoteActivity.class);
        stackBuilder.addNextIntent(new Intent(this, RemoteActivity.class));
        mRemoteStartPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.LOGD(TAG, "onStartCommand");
        // Get the connection observer here, not on create to check if
        // there has been a change in hosts, and if so unregister the previous one
        HostConnectionObserver connectionObserver = HostManager.getInstance(this).getHostConnectionObserver();

        // If we are already initialized and the same host, exit
        if (mHostConnectionObserver == connectionObserver) {
            LogUtils.LOGD(TAG, "Already initialized");
            return START_NOT_STICKY;
        }

        // If there's a change in hosts, unregister from the previous one
        if (mHostConnectionObserver != null) {
            mHostConnectionObserver.unregisterPlayerObserver(this);
        }

        // Register us on the connection observer
        mHostConnectionObserver = connectionObserver;
        mHostConnectionObserver.registerPlayerObserver(this, true);

        // If we get killed, after returning from here, don't restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }


    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */

    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        buildNotification(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        buildNotification(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    public void playerOnStop() {
        removeNotification();
        // Stop service
        LogUtils.LOGD(TAG, "Shutting down notification service - Player stopped");
        mHostConnectionObserver.unregisterPlayerObserver(this);
        stopSelf();
    }

    public void playerNoResultsYet() {
        removeNotification();
    }

    public void playerOnConnectionError(int errorCode, String description) {
        removeNotification();
        // Stop service
        LogUtils.LOGD(TAG, "Shutting down notification service - Connection error");
        mHostConnectionObserver.unregisterPlayerObserver(this);
        stopSelf();
    }

    public void systemOnQuit() {
        removeNotification();
        // Stop service
        LogUtils.LOGD(TAG, "Shutting down notification service - System quit");
        mHostConnectionObserver.unregisterPlayerObserver(this);
        stopSelf();
    }

    // Ignore this
    public void inputOnInputRequested(String title, String type, String value) { }

    public void observerOnStopObserving() {
        // Called when the user changes host
        removeNotification();
        LogUtils.LOGD(TAG, "Shutting down notification service - System quit");
        stopSelf();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void buildNotification(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   PlayerType.PropertyValue getPropertiesResult,
                                   ListType.ItemsAll getItemResult) {
        String title, underTitle, poster;
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
                String seasonEpisode = String.format(getString(R.string.season_episode_abbrev),
                        getItemResult.season, getItemResult.episode);
                underTitle = String.format("%s | %s", getItemResult.showtitle, seasonEpisode);
                poster = getItemResult.art.poster;
                smallIcon = R.drawable.ic_tv_white_24dp;
                break;
            case ListType.ItemsAll.TYPE_SONG:
                title = getItemResult.title;
                underTitle = getItemResult.displayartist + " - " + getItemResult.album;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_headset_white_24dp;
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                title = getItemResult.title;
                underTitle = Utils.listStringConcat(getItemResult.artist, ", ") + " - " + getItemResult.album;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_headset_white_24dp;
                break;
            default:
                // We don't know what this is, forget it
                return;
        }

        switch (getPropertiesResult.speed) {
            case 1:
                playPauseIcon = R.drawable.ic_pause_white_24dp;
                break;
            default:
                playPauseIcon = R.drawable.ic_play_arrow_white_24dp;
                break;
        }

        // Create the actions, depending on the type of media
        PendingIntent rewindPendingItent, ffPendingItent, playPausePendingIntent;
        playPausePendingIntent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_PLAY_PAUSE);
        if (getItemResult.type.equals(ListType.ItemsAll.TYPE_SONG)) {
            rewindPendingItent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_PREVIOUS);
            rewindIcon = R.drawable.ic_skip_previous_white_24dp;
            ffPendingItent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_NEXT);
            ffIcon = R.drawable.ic_skip_next_white_24dp;
        } else {
            rewindPendingItent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_REWIND);
            rewindIcon = R.drawable.ic_fast_rewind_white_24dp;
            ffPendingItent = buildActionPendingIntent(getActivePlayerResult.playerid, IntentActionsService.ACTION_FAST_FORWARD);
            ffIcon = R.drawable.ic_fast_forward_white_24dp;
        }

        // Setup the collpased and expanded notifications
        RemoteViews collapsedRV = new RemoteViews(this.getPackageName(), R.layout.notification_colapsed);
        collapsedRV.setImageViewResource(R.id.rewind, rewindIcon);
        collapsedRV.setOnClickPendingIntent(R.id.rewind, rewindPendingItent);
        collapsedRV.setImageViewResource(R.id.play, playPauseIcon);
        collapsedRV.setOnClickPendingIntent(R.id.play, playPausePendingIntent);
        collapsedRV.setImageViewResource(R.id.fast_forward, ffIcon);
        collapsedRV.setOnClickPendingIntent(R.id.fast_forward, ffPendingItent);
        collapsedRV.setTextViewText(R.id.title, title);
        collapsedRV.setTextViewText(R.id.text2, underTitle);

        RemoteViews expandedRV = new RemoteViews(this.getPackageName(), R.layout.notification_expanded);
        expandedRV.setImageViewResource(R.id.rewind, rewindIcon);
        expandedRV.setOnClickPendingIntent(R.id.rewind, rewindPendingItent);
        expandedRV.setImageViewResource(R.id.play, playPauseIcon);
        expandedRV.setOnClickPendingIntent(R.id.play, playPausePendingIntent);
        expandedRV.setImageViewResource(R.id.fast_forward, ffIcon);
        expandedRV.setOnClickPendingIntent(R.id.fast_forward, ffPendingItent);
        expandedRV.setTextViewText(R.id.title, title);
        expandedRV.setTextViewText(R.id.text2, underTitle);
        int expandedIconResId;
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notification = builder
                .setSmallIcon(smallIcon)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(mRemoteStartPendingIntent)
                .setContent(collapsedRV)
                .build();

        // Load images. Use the same dimensions as the remote to hit the cache both times
        Resources resources = this.getResources();
        int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
        int posterHeight = isVideo?
                resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height):
                posterWidth;

        HostManager hostManager = HostManager.getInstance(this);
        hostManager.getPicasso()
                .load(hostManager.getHostInfo().getImageUrl(poster))
                .resize(posterWidth, posterHeight)
                .into(collapsedRV, R.id.icon, NOTIFICATION_ID, notification);

        if (Utils.isJellybeanOrLater()) {
            notification.bigContentView = expandedRV;
            hostManager.getPicasso()
                    .load(hostManager.getHostInfo().getImageUrl(poster))
                    .resize(posterWidth, posterHeight)
                    .into(expandedRV, expandedIconResId, NOTIFICATION_ID, notification);
        }

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    private PendingIntent buildActionPendingIntent(int playerId, String action) {
        LogUtils.LOGD(TAG, "Build action: " + action);
        Intent intent = new Intent(this, IntentActionsService.class)
                .setAction(action)
                .putExtra(IntentActionsService.EXTRA_PLAYER_ID, playerId);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void removeNotification() {
        NotificationManager  notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
