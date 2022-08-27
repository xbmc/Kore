package org.xbmc.kore.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.media.VolumeProviderCompat;

import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.service.MediaSessionService;

/**
 * This is a ProviderCompat for handling volume adjustments via hardware buttons when Kore is not active in foreground.
 * It's attached to the MediaSession created inside {@link MediaSessionService}. Volume adjustments
 * are not registered, when the screen is locked and turned off or when no media is playing.
 */
public class RemoteVolumeProviderCompat extends VolumeProviderCompat implements HostConnectionObserver.ApplicationEventsObserver {
    private static final String TAG = LogUtils.makeLogTag(RemoteVolumeProviderCompat.class);
    private static final int KODI_MAX_VOLUME = 100;
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());
    private HostConnection hostConnection;

    public RemoteVolumeProviderCompat(HostConnection hostConnection) {
        super(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, KODI_MAX_VOLUME, 0);
        this.hostConnection = hostConnection;
    }

    /**
     * handles hardware volume button usage
     *
     * @param direction +1 for increment, -1 for decrement, 0 is not documented
     */
    @Override
    public void onAdjustVolume(int direction) {
        if (direction != 0) {
            new Application.SetVolume(direction > 0
                    ? GlobalType.IncrementDecrement.INCREMENT
                    : GlobalType.IncrementDecrement.DECREMENT)
                    .execute(hostConnection, null, callbackHandler);
        }
    }

    /**
     * handles volume bar slides
     *
     * @param volume target volume
     */
    @Override
    public void onSetVolumeTo(int volume) {
        new Application.SetVolume(volume).execute(hostConnection, null, callbackHandler);
        setCurrentVolume(volume);
    }

    @Override
    public void onApplicationVolumeChanged(int volume, boolean muted) {
        setCurrentVolume(volume);
    }
}

