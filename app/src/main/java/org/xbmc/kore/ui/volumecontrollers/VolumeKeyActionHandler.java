package org.xbmc.kore.ui.volumecontrollers;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.GlobalType;

class VolumeKeyActionHandler {

    private final HostManager hostManager;
    private final Context context;
    private final OnHardwareVolumeKeyPressedCallback onHardwareVolumeKeyPressedCallback;

    VolumeKeyActionHandler(HostManager hostManager, Context context,
            @Nullable OnHardwareVolumeKeyPressedCallback onHardwareVolumeKeyPressedCallback) {
        this.hostManager = hostManager;
        this.context = context;
        this.onHardwareVolumeKeyPressedCallback = onHardwareVolumeKeyPressedCallback;
    }

    boolean handleDispatchKeyEvent(KeyEvent event) {
        if (shouldInterceptKey()) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (action == KeyEvent.ACTION_DOWN) {
                    notifyCallback();
                    setVolume(GlobalType.IncrementDecrement.INCREMENT);
                }
                return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (action == KeyEvent.ACTION_DOWN) {
                    notifyCallback();
                    setVolume(GlobalType.IncrementDecrement.DECREMENT);
                }
                return true;
            }
        }
        return false;
    }

    private void setVolume(String volume) {
        new Application.SetVolume(volume).execute(hostManager.getConnection(), null, null);
    }

    private boolean shouldInterceptKey() {
        return android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Settings.KEY_PREF_USE_HARDWARE_VOLUME_KEYS,
                        Settings.DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS);
    }

    private void notifyCallback() {
        if (onHardwareVolumeKeyPressedCallback != null) {
            onHardwareVolumeKeyPressedCallback.onHardwareVolumeKeyPressed();
        }
    }
}
