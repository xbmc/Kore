package org.xbmc.kore.ui.generic;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.ui.widgets.HighlightButton;
import org.xbmc.kore.ui.widgets.VolumeLevelIndicator;
import org.xbmc.kore.utils.LogUtils;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.Unbinder;

public class VolumeControllerDialogFragmentListener extends AppCompatDialogFragment
        implements HostConnectionObserver.ApplicationEventsObserver,
        VolumeLevelIndicator.VolumeBarTouchTrackerListener {

    private static final String TAG = LogUtils.makeLogTag(VolumeControllerDialogFragmentListener.class);
    private static final int AUTO_DISMISS_DELAY = 2000;

    @BindView(R.id.vcd_volume_mute) HighlightButton volumeMuteButton;
    @BindView(R.id.vcd_volume_muted_indicator) HighlightButton volumeMutedIndicatorButton;
    @BindView(R.id.vcd_volume_level_indicator) VolumeLevelIndicator volumeLevelIndicator;

    private Unbinder unbinder;
    private Handler callbackHandler = new Handler();
    private HostManager hostManager = null;
    private ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();
    private View.OnClickListener onMuteToggleOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
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
        }
    };
    private long lastVolumeChangeInteractionEvent;
    private Runnable dismissDialog = new Runnable() {
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
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface dialog, int keyCode,
                    android.view.KeyEvent event) {
                boolean handled = handleVolumeKeyEvent(getContext(), event);
                if (handled) {
                    delayedDismissDialog();
                }
                return handled;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.volume_controller_dialog, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        delayedDismissDialog();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
        unbinder.unbind();
    }

    private void registerObserver() {
        HostConnectionObserver hostConnectionObserver = hostManager.getHostConnectionObserver();
        if (hostConnectionObserver == null) {
            return;
        }

        hostConnectionObserver.registerApplicationObserver(this, true);
        hostConnectionObserver.forceRefreshResults();
    }

    private void setListeners() {
        volumeMuteButton.setOnClickListener(onMuteToggleOnClickListener);
        volumeMutedIndicatorButton.setOnClickListener(onMuteToggleOnClickListener);

        volumeLevelIndicator.setOnVolumeChangeListener(
                new VolumeLevelIndicator.OnVolumeChangeListener() {
                    @Override
                    public void onVolumeChanged(int volume) {
                        cancelDismissDialog();
                        new Application.SetVolume(volume).execute(hostManager.getConnection(),
                                defaultIntActionCallback, callbackHandler);
                    }
                });
        volumeLevelIndicator.setVolumeBarTouchTrackerListener(this);
    }

    @Override
    public void applicationOnVolumeChanged(int volume, boolean muted) {
        volumeLevelIndicator.setVolume(muted, volume);

        volumeMutedIndicatorButton.setVisibility(muted ? View.VISIBLE : View.GONE);
        volumeMutedIndicatorButton.setHighlight(muted);

        volumeMuteButton.setHighlight(muted);
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
                android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(context)
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
