package org.xbmc.kore.ui.generic;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.VolumeControllerDialogBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.ui.widgets.VolumeLevelIndicator;
import org.xbmc.kore.utils.LogUtils;

public class VolumeControllerDialogFragmentListener extends AppCompatDialogFragment
        implements HostConnectionObserver.ApplicationEventsObserver,
        VolumeLevelIndicator.VolumeBarTouchTrackerListener {

    private static final String TAG = LogUtils.makeLogTag(VolumeControllerDialogFragmentListener.class);
    private static final int AUTO_DISMISS_DELAY = 2000;

    VolumeControllerDialogBinding binding;

    private final Handler callbackHandler = new Handler();
    private HostManager hostManager = null;
    private final ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();
    private final View.OnClickListener onMuteToggleOnClickListener = v -> {
        cancelDismissDialog();
        Application.SetMute action = new Application.SetMute();
        action.execute(hostManager.getConnection(), new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                //We depend on the listener to correct the mute button state
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGE(TAG,
                        "Got an error calling Application.SetMute. Error code: " + errorCode +
                                ", description: " + description);
            }
        }, callbackHandler);
    };
    private long lastVolumeChangeInteractionEvent;
    private final Runnable dismissDialog = new Runnable() {
        @Override
        public void run() {
            long timeSinceLastEvent = System.currentTimeMillis() - lastVolumeChangeInteractionEvent;
            if (timeSinceLastEvent >= AUTO_DISMISS_DELAY) {
                Dialog dialog = getDialog();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        requireDialog().setOnKeyListener((dialog, keyCode, event) -> {
            boolean handled = handleVolumeKeyEvent(getContext(), event);
            if (handled) {
                delayedDismissDialog();
            }
            return handled;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = VolumeControllerDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void show(@NonNull FragmentManager manager, String tag) {
        super.show(manager, tag);
        delayedDismissDialog();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hostManager = HostManager.getInstance(getContext());

        setListeners();

        registerObserver();
        // for orientation change
        delayedDismissDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        HostConnectionObserver hostConnectionObserver = hostManager.getHostConnectionObserver();
        if (hostConnectionObserver != null) {
            hostConnectionObserver.unregisterApplicationObserver(this);
        }
        binding = null;
    }

    private void registerObserver() {
        HostConnectionObserver hostConnectionObserver = hostManager.getHostConnectionObserver();
        if (hostConnectionObserver == null) {
            return;
        }

        hostConnectionObserver.registerApplicationObserver(this);
        hostConnectionObserver.refreshWhatsPlaying();
    }

    private void setListeners() {
        binding.vcdVolumeMute.setOnClickListener(onMuteToggleOnClickListener);
        binding.vcdVolumeMutedIndicator.setOnClickListener(onMuteToggleOnClickListener);

        binding.vcdVolumeLevelIndicator.setOnVolumeChangeListener(
                volume -> {
                    cancelDismissDialog();
                    new Application.SetVolume(volume).execute(hostManager.getConnection(),
                            defaultIntActionCallback, callbackHandler);
                });
        binding.vcdVolumeLevelIndicator.setVolumeBarTouchTrackerListener(this);
    }

    @Override
    public void applicationOnVolumeChanged(int volume, boolean muted) {
        binding.vcdVolumeLevelIndicator.setVolume(muted, volume);

        binding.vcdVolumeMutedIndicator.setVisibility(muted ? View.VISIBLE : View.GONE);
        binding.vcdVolumeMutedIndicator.setHighlight(muted);

        binding.vcdVolumeMute.setHighlight(muted);
    }

    private void delayedDismissDialog() {
        cancelDismissDialog();
        callbackHandler.postDelayed(dismissDialog, AUTO_DISMISS_DELAY);
    }

    private void cancelDismissDialog() {
        lastVolumeChangeInteractionEvent = System.currentTimeMillis();
        callbackHandler.removeCallbacks(dismissDialog);
    }

    @Override
    public void onStartTrackingTouch() {
        cancelDismissDialog();
    }

    @Override
    public void onStopTrackingTouch() {
        delayedDismissDialog();
    }

    public static boolean handleVolumeKeyEvent(Context context, KeyEvent event) {
        boolean shouldInterceptKey =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(Settings.KEY_PREF_USE_HARDWARE_VOLUME_KEYS,
                                Settings.DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS);

        if (shouldInterceptKey) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                String volume = (keyCode == KeyEvent.KEYCODE_VOLUME_UP)?
                        GlobalType.IncrementDecrement.INCREMENT:
                        GlobalType.IncrementDecrement.DECREMENT;
                if (action == KeyEvent.ACTION_DOWN) {
                    new Application.SetVolume(volume)
                            .execute(HostManager.getInstance(context).getConnection(), null, null);
                }
                return true;
            }
        }
        return false;
    }

}
