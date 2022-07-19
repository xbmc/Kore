package org.xbmc.kore.service;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

/**
 * Helper class that listens to Phone Call state, and pauses or resumes playback on Kodi when a call is received
 */
public class CallStateListener {
    public static final String TAG = LogUtils.makeLogTag(CallStateListener.class);

    private final Context context;

    private int currentActivePlayerId = -1;
    private boolean isPlaying = false;
    private boolean shouldResume = false;

    private final HostManager hostManager;

    private final TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private TelephonyCallback telephonyCallback;

    @RequiresApi(api = Build.VERSION_CODES.S)
    private abstract static class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
    }

    public CallStateListener(Context context) {
        this.context = context;
        this.hostManager = HostManager.getInstance(context);
        this.telephonyManager = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE));
    }

    public void startListening() {
        if (!Utils.isSOrLater()) {
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    handleCallStateChanged(state);
                }
            };
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            telephonyCallback = new MyTelephonyCallback() {
                @Override
                public void onCallStateChanged(int state) {
                    handleCallStateChanged(state);
                }
            };
            telephonyManager.registerTelephonyCallback(context.getMainExecutor(), telephonyCallback);
        }
    }

    private void handleCallStateChanged(int state) {
        if (state == TelephonyManager.CALL_STATE_OFFHOOK && isPlaying) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), null, null);
            shouldResume = true;
        } else if (state == TelephonyManager.CALL_STATE_IDLE && !isPlaying && shouldResume) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), null, null);
            shouldResume = false;
        } else if (state == TelephonyManager.CALL_STATE_RINGING) {
            Player.Notification action = new Player.Notification(
                    context.getResources().getString(R.string.pause_call_incoming_title),
                    context.getResources().getString(R.string.pause_call_incoming_message));
            action.execute(hostManager.getConnection(), null, null);
        }
    }

    public void stopListening() {
        currentActivePlayerId = -1;
        isPlaying = false;
        shouldResume = false;

        if (!Utils.isSOrLater()) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        } else {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback);
        }
    }

    public void onPlay(int activePlayerId) {
        currentActivePlayerId = activePlayerId;
        isPlaying = true;
    }

    public void onPause(int activePlayerId) {
        if(currentActivePlayerId != activePlayerId) {
            shouldResume = false;
        }
        currentActivePlayerId = activePlayerId;
        isPlaying = false;
    }
}
