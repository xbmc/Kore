package org.xbmc.kore.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Gravity;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.CharacterDrawable;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.RemoteVolumeProviderCompat;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

/**
 * This service creates and updates a {@link android.support.v4.media.session.MediaSessionCompat} and a
 * {@link android.app.Notification} while playback is playing on Kodi.
 *
 * It should be started as soon as playback is detected (tipically by an activity that receives callbacks
 * from {@link HostConnectionObserver}).
 * The service keeps running in the foreground while something is playing, and stops itself as soon as playback
 * stops or a connection error is detected.
 *
 * A {@link HostConnectionObserver} singleton is used to keep track of Kodi's
 * state. This singleton should be the same as used in the app's activities
 */
public class MediaSessionService extends Service
        implements HostConnectionObserver.PlayerEventsObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = LogUtils.makeLogTag(MediaSessionService.class);

    public static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL = "KORE";
    private static final String MEDIA_SESSION_TAG = "Kore";

    private static boolean isRunning = false;

    private NotificationManager notificationManager;
    private HostConnection hostConnection = null;
    private HostConnectionObserver hostConnectionObserver = null;
    private Notification nothingPlayingNotification;
    private PendingIntent remoteStartPendingIntent;

    private int currentPlayerId = -1;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;

    private RemoteVolumeProviderCompat remoteVolumePC;

    private CallStateListener callStateListener = null;

    private final MediaSessionCompat.Callback mediaSessionController = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            new Player.PlayPause(currentPlayerId).execute(hostConnection, null, null);
        }

        @Override
        public void onPause() {
            new Player.PlayPause(currentPlayerId).execute(hostConnection, null, null);
        }

        @Override
        public void onSkipToNext() {
            new Player.GoTo(currentPlayerId, Player.GoTo.NEXT).execute(hostConnection, null, null);
        }

        @Override
        public void onSkipToPrevious() {
            new Player.GoTo(currentPlayerId, Player.GoTo.PREVIOUS).execute(hostConnection, null, null);
        }

        @Override
        public void onFastForward() {
            new Player.Seek(currentPlayerId, Player.Seek.FORWARD).execute(hostConnection, null, null);
        }

        @Override
        public void onRewind() {
            new Player.Seek(currentPlayerId, Player.Seek.BACKWARD).execute(hostConnection, null, null);
        }

        @Override
        public void onStop() {
            new Player.Stop(currentPlayerId).execute(hostConnection, null, null);
        }

        @Override
        public void onSeekTo(long pos) {
            new Player.Seek(currentPlayerId, new PlayerType.PositionTime((int) (pos / 1000)))
                    .execute(hostConnection, null, null);
        }
    };

    @Override
    public void onCreate() {
        // We do not create any thread because all the works is supposed to be done on the main thread, so that the
        // connection observer can be shared with the app, and notify it on the UI thread
        notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        hostConnection = HostManager.getInstance(this).getConnection();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        // Create the intent to start the remote when the user taps the notification
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(new Intent(this, RemoteActivity.class));
        int flags = Utils.isMOrLater() ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        remoteStartPendingIntent = stackBuilder.getPendingIntent(0, flags);

        // Create the notification channel and the default notification
        if (Utils.isOreoOrLater()) createNotificationChannel();
        nothingPlayingNotification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_round_devices_24)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(remoteStartPendingIntent)
                .setContentTitle(String.format(this.getString(R.string.connected_to),
                                               HostManager.getInstance(this).getHostInfo().getName()))
                .setContentText(this.getString(R.string.nothing_playing))
                .build();

        // Create the Media Session
        mediaSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG);
        mediaSession.setCallback(mediaSessionController);
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_STOP |
                            PlaybackStateCompat.ACTION_SEEK_TO |
                            PlaybackStateCompat.ACTION_FAST_FORWARD |
                            PlaybackStateCompat.ACTION_REWIND |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        mediaSession.setPlaybackState(stateBuilder.build());

        if (hardwareVolumeKeysEnabled()) {
            remoteVolumePC = new RemoteVolumeProviderCompat(hostConnection);
            mediaSession.setPlaybackToRemote(remoteVolumePC);
        }
        metadataBuilder = new MediaMetadataCompat.Builder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Given that the documentation for {@link MediaSessionCompat} and {@link MediaButtonReceiver} is somewhat
        // confusing, here's an explanation for the flow of creating/handling Media Button events from a notification
        // Media Button events are directly sent to {@link MediaSessionCompat}, calling its callbacks, but when creating
        // a notification, we need to set a {@link PendingIntent} with a Media Button event, and the way to do it is by
        // calling buildMediaButtonPendingIntent on MediaButtonReceiver. This will return a Pending Intent with the
        // given action, that directly calls the {@link MediaButtonReceiver} broadcast receiver (hence the need for
        // declaring androidx.media.session.MediaButtonReceiver on the Manifest), not routing throug MediaSession.
        // That broadcast receiver tries to find a service on the Application that handles Intent.ACTION_MEDIA_BUTTON,
        // which is this service (hence the intent filter on the Manifest), and starts it, passing the intent.
        // Here we need to forward it to the callbacks defined on the {@link MediaSessionCompat}, which can be done
        // by calling MediaButtonReceiver.handleIntent(mediaSession, intent)
        // Note that other Media Button events are directly sent to the MediaSession callbacks, like for instance
        // button presses from a Android Wear connected phone.
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent ke = MediaButtonReceiver.handleIntent(mediaSession, intent);
            LogUtils.LOGD(TAG, "Got a Media Button Event: " + ke.toString());
            return super.onStartCommand(intent, flags, startId);
        }

        // Request foreground and show default notification, will update later
        startForeground(NOTIFICATION_ID, nothingPlayingNotification);

        HostConnectionObserver connectionObserver = HostManager.getInstance(this).getHostConnectionObserver();
        if (hostConnectionObserver == null || hostConnectionObserver != connectionObserver) {
            // New connection or there has been a change in hosts, in which case we need to unregister the previous one
            if (hostConnectionObserver != null) unregisterObservers();
            hostConnectionObserver = connectionObserver;
            hostConnectionObserver.registerPlayerObserver(this);
            if (remoteVolumePC != null) hostConnectionObserver.registerApplicationObserver(remoteVolumePC);
        }


        // Check whether we should react to phone state changes and wether we have permissions to do so
        boolean shouldPause = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_PAUSE_DURING_CALLS, Settings.DEFAULT_PREF_PAUSE_DURING_CALLS);
        boolean hasPhonePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        if (shouldPause && hasPhonePermission) {
            callStateListener = new CallStateListener(this);
            callStateListener.startListening();
        }
        isRunning = true;
        // If we get killed after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        // Gracefully stop
        isRunning = false;
        mediaSession.release();
        if (hostConnectionObserver != null) {
            unregisterObservers();
        }
        if (callStateListener != null) {
            callStateListener.stopListening();
        }
    }

    /**
     * Starts this service if it is not already running
     * @param context Context
     */
    public static void startIfNotRunning(Context context) {
        if (isRunning) return;

        if (Utils.isOreoOrLater()) {
            context.startForegroundService(new Intent(context, MediaSessionService.class));
        } else {
            context.startService(new Intent(context, MediaSessionService.class));
        }
    }

    /* Ignore this */
    @Override
    public void onPlayerPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {}

    /**
     * HostConnectionObserver.PlayerEventsObserver interface callbacks
     */
    @Override
    public void onPlayerPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        notifyPlaying(getActivePlayerResult, getPropertiesResult, getItemResult);
        if (callStateListener != null) {
            callStateListener.onPlay(getActivePlayerResult.playerid);
        }
    }

    @Override
    public void onPlayerPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        notifyPlaying(getActivePlayerResult, getPropertiesResult, getItemResult);
        if (callStateListener != null) {
            callStateListener.onPause(getActivePlayerResult.playerid);
        }
    }

    private final Handler stopHandler = new Handler(Looper.myLooper());
    @Override
    public void onPlayerStop() {
        notifyNothingPlaying();

        // Stop service if nothing starts in a couple of seconds
        stopHandler.postDelayed(() -> {
            if (!mediaSession.isActive())
                stop("Player stopped");
        }, 5000);
    }

    @Override
    public void onPlayerNoResultsYet() {
        notifyNothingPlaying();
    }

    @Override
    public void onPlayerConnectionError(int errorCode, String description) {
        stop("Connection Error: " + description);
    }

    @Override
    public void onSystemQuit() {
        stop("System quit");
    }

    // Ignore
    @Override
    public void onInputRequested(String title, String type, String value) {}

    @Override
    public void onObserverStopObserving() {
        stop("Stop observing");
    }

    /**
     * Creates the notification channel for Android Oreo and later
     */
    private void createNotificationChannel() {
        if (!Utils.isOreoOrLater()) return;

        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                                                                  this.getString(R.string.app_name),
                                                                  NotificationManager.IMPORTANCE_LOW);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);

            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Stops this service gracefully
     * @param reason String to log
     */
    private void stop(String reason) {
        mediaSession.setActive(false);
        // Stop service
        LogUtils.LOGD(TAG, "Stopping media session service. Reason: " + reason);
        if (hostConnectionObserver != null) {
            unregisterObservers();
        }
        notificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
    }

    // Picasso target that will be used to load images
    private static Target picassoTarget = null;

    /**
     * Creates and updates the notification shown, when something is playing
     * @param getActivePlayerResult Result from GetActivePlayer call
     * @param getPropertiesResult Result from GetProperties call
     * @param getItemResult Result from GetItem call
     */
    private void notifyPlaying(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                               PlayerType.PropertyValue getPropertiesResult,
                               ListType.ItemsAll getItemResult) {
        final String title, underTitle, poster;
        int smallIcon, playPauseIcon;

        switch (getItemResult.type) {
            case ListType.ItemsAll.TYPE_MOVIE:
                title = getItemResult.title;
                underTitle = getItemResult.tagline;
                poster = getItemResult.art.poster;
                smallIcon = R.drawable.ic_round_movie_24;
                break;
            case ListType.ItemsAll.TYPE_EPISODE:
                title = getItemResult.title;
                String seasonEpisode = String.format(this.getString(R.string.season_episode_abbrev),
                                                     getItemResult.season, getItemResult.episode);
                underTitle = String.format("%s | %s", getItemResult.showtitle, seasonEpisode);
                poster = getItemResult.art.poster;
                smallIcon = R.drawable.ic_round_tv_24;
                break;
            case ListType.ItemsAll.TYPE_SONG:
                title = getItemResult.title;
                underTitle = getItemResult.displayartist + " | " + getItemResult.album;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_round_headphones_24;
                break;
            case ListType.ItemsAll.TYPE_MUSIC_VIDEO:
                title = getItemResult.title;
                underTitle = Utils.listStringConcat(getItemResult.artist, ", ") + " | " + getItemResult.album;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_round_headphones_24;
                break;
            case ListType.ItemsAll.TYPE_CHANNEL:
                title = getItemResult.label;
                underTitle = getItemResult.title;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_round_dvr_24;
                break;
            default:
                title = getItemResult.label;
                underTitle = getItemResult.title;
                poster = getItemResult.thumbnail;
                smallIcon = R.drawable.ic_round_devices_24;
                break;
        }

        currentPlayerId = getActivePlayerResult.playerid;

        stateBuilder.setState(getPropertiesResult.speed == 1 ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                              getPropertiesResult.time.toMiliseconds(),
                              getPropertiesResult.speed);
        mediaSession.setPlaybackState(stateBuilder.build());

        metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, Integer.toString(getItemResult.id))
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, underTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, getItemResult.description)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getItemResult.displayartist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getItemResult.album)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, getItemResult.displayartist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getPropertiesResult.totaltime.toMiliseconds())
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, poster);

        playPauseIcon = (getPropertiesResult.speed == 1) ? R.drawable.ic_round_pause_24 : R.drawable.ic_round_play_arrow_24;

        // See explanation of the creating/handling of this Pending Intents in onStartCommand
        PendingIntent skippreviousPI = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS),
                rewindPI = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_REWIND),
                playPausePI = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE),
                fastforwardPI = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_FAST_FORWARD),
                skipnextPI = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT);

        boolean isSong = getItemResult.type.equals(ListType.ItemsAll.TYPE_SONG);
        int[] actionsInCompactView = isSong ? new int[]{0, 2, 4} : new int[]{1, 2, 3};

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(smallIcon)
                .setShowWhen(false)
                .setOngoing(true)
                .setContentIntent(remoteStartPendingIntent)
                .setContentTitle(title)
                .setContentText(underTitle)
                .addAction(R.drawable.ic_round_skip_previous_24, this.getString(R.string.previous), skippreviousPI)
                .addAction(R.drawable.ic_round_fast_rewind_24, this.getString(R.string.rewind), rewindPI)
                .addAction(playPauseIcon, this.getString(R.string.play), playPausePI)
                .addAction(R.drawable.ic_round_fast_forward_24, this.getString(R.string.fast_forward), fastforwardPI)
                .addAction(R.drawable.ic_round_skip_next_24, this.getString(R.string.next), skipnextPI)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                                  .setMediaSession(mediaSession.getSessionToken())
                                  .setShowActionsInCompactView(actionsInCompactView));

        // This is a convoluted way of loading the image and showing the notification, but it's what works with Picasso
        // and is efficient. Here's what's going on:
        //
        // 1. The image is loaded asynchronously into a Target, and only after it is loaded is the notification shown.
        // Using targets is a lot more efficient than letting Picasso load it directly into the notification imageview,
        // which causes a lot of flickering
        //
        // 2. The target needs to be static, because Picasso only keeps a weak reference to it, so we need to keed a
        // strong reference and reset it to null when we're done. We also need to check if it is not null in case a
        // previous request hasn't finished yet.
        //
        // 3. We can only show the notification after the bitmap is loaded into the target, so it is done in the
        // callback
        //
        // 4. We specifically resize the image to the same dimensions used in the remote, so that Picasso reuses it in
        // the remote and here from the cache
        Resources resources = this.getResources();
        final int posterWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_width);
        final int posterHeight = Utils.isROrLater() || isSong ? posterWidth : resources.getDimensionPixelOffset(R.dimen.info_poster_height);
        if (picassoTarget == null ) {
            picassoTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    showNotification(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    CharacterDrawable avatarDrawable = UIUtils.getCharacterAvatar(MediaSessionService.this, title);
                    showNotification(Utils.drawableToBitmap(avatarDrawable, posterWidth, posterHeight));
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) { }

                private void showNotification(Bitmap bitmap) {
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                    mediaSession.setMetadata(metadataBuilder.build());
                    mediaSession.setActive(true);

                    notificationBuilder.setLargeIcon(bitmap);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    picassoTarget = null;
                }
            };

            // Load the image
            HostManager hostManager = HostManager.getInstance(this);
            hostManager.getPicasso()
                       .load(hostManager.getHostInfo().getImageUrl(poster))
                       .resize(posterWidth, posterHeight)
                       .centerCrop(Gravity.CENTER)
                       .into(picassoTarget);
        }
    }

    /**
     * Show the nothing playing notification
     */
    private void notifyNothingPlaying() {
        mediaSession.setActive(false);
        stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, 0 ,1);
        mediaSession.setPlaybackState(stateBuilder.build());

        notificationManager.notify(NOTIFICATION_ID, nothingPlayingNotification);
    }

    private boolean hardwareVolumeKeysEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_USE_HARDWARE_VOLUME_KEYS,
                        Settings.DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS);
    }

    /**
     * listen for changes on SharedPreferences and handle them
     * @param sharedPreferences preferences set by user
     * @param key key of the changed setting
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.KEY_PREF_USE_HARDWARE_VOLUME_KEYS.equals(key)) {
            if (hardwareVolumeKeysEnabled()) {
                remoteVolumePC = new RemoteVolumeProviderCompat(hostConnection);
                mediaSession.setPlaybackToRemote(remoteVolumePC);
                hostConnectionObserver.registerApplicationObserver(remoteVolumePC);
            } else {
                mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
                hostConnectionObserver.unregisterApplicationObserver(remoteVolumePC);
                remoteVolumePC = null;
            }
        }
    }

    private void unregisterObservers() {
        hostConnectionObserver.unregisterPlayerObserver(this);
        if (remoteVolumePC != null) hostConnectionObserver.unregisterApplicationObserver(remoteVolumePC);
    }
}