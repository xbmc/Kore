package org.xbmc.kore.ui.volumecontrollers;

import android.view.KeyEvent;

import org.xbmc.kore.ui.BaseActivity;

public abstract class VolumeControllerActivity extends BaseActivity
        implements OnHardwareVolumeKeyPressedCallback {

    private VolumeKeyActionHandler volumeKeyActionHandler;

    /**
     * Override hardware volume keys and send to Kodi
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (volumeKeyActionHandler == null) {
            volumeKeyActionHandler = new VolumeKeyActionHandler(hostManager, this, this);
        }
        return volumeKeyActionHandler.handleDispatchKeyEvent(event) || super.dispatchKeyEvent(
                event);
    }

    public void onHardwareVolumeKeyPressed() {
        showVolumeChangeDialog();
    }

    private void showVolumeChangeDialog() {
        VolumeControllerDialogFragment volumeControllerDialogFragment =
                new VolumeControllerDialogFragment();
        volumeControllerDialogFragment.show(getSupportFragmentManager(),
                VolumeControllerDialogFragment.class.getName());
    }

}
