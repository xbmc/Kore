package org.xbmc.kore.ui.generic;


import android.app.Activity;

import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Input;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that presents a list of options related to subtitles.
 */
public class SubtitlesOptionsDialog
        implements HostConnectionObserver.PlayerEventsObserver,
                    GenericSelectDialog.GenericSelectDialogListener{


    // An interface to be implemented by everyone interested in RemoteRequired events
    public interface SubtitlesOptionsDialogListener {
        void onRemoteRequired();
    }

    private List<SubtitlesOptionsDialogListener> listeners = new ArrayList<SubtitlesOptionsDialogListener>();

    private static final String TAG = LogUtils.makeLogTag(SubtitlesOptionsDialog.class);

    /**
     * List of available subtitles
     */
    private List<PlayerType.Subtitle> availableSubtitles;
    private int currentSubtitleIndex = -1;

    // Number of explicitly added options for subtitles (to subtract from the
    // number of subtitles returned by Kodi)
    static final int ADDED_SUBTITLE_OPTIONS = 3;

    /**
     * Constants for the general select dialog
     */
    private final static int SELECT_SUBTITLES = -111;

    /**
     * Host manager from which to get info about the current XBMC
     */
    private HostManager hostManager;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    /**
     * Activity to communicate potential actions that change what's playing
     */
    private HostConnectionObserver hostConnectionObserver;

    /**
     * The current active player id
     */
    private int currentActivePlayerId = -1;

    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();



    private FragmentActivity dialogActivity;

    public SubtitlesOptionsDialog(Activity activity){
        hostManager =HostManager.getInstance(activity);
        hostConnectionObserver=hostManager.getHostConnectionObserver();
        hostConnectionObserver.registerPlayerObserver(this, true);
        dialogActivity =(FragmentActivity)activity;
    }


    /**
     * Display subtitles dialog
     *
     */
    public void Show(){
        int selectedItem = -1;

        String[] subtitles = new String[(availableSubtitles != null) ?
                availableSubtitles.size() + ADDED_SUBTITLE_OPTIONS : ADDED_SUBTITLE_OPTIONS];

        subtitles[0] = dialogActivity.getString(R.string.download_subtitle);
        subtitles[1] = dialogActivity.getString(R.string.subtitle_sync);
        subtitles[2] = dialogActivity.getString(R.string.none);

        if (availableSubtitles != null) {
            for (int i = 0; i < availableSubtitles.size(); i++) {
                PlayerType.Subtitle current = availableSubtitles.get(i);
                subtitles[i + ADDED_SUBTITLE_OPTIONS] = TextUtils.isEmpty(current.language) ?
                        current.name : current.language + " | " + current.name;
                if (current.index == currentSubtitleIndex) {
                    selectedItem = i + ADDED_SUBTITLE_OPTIONS;
                }
            }
        }

        GenericSelectDialog dialog = GenericSelectDialog.newInstance(this,
                SELECT_SUBTITLES, dialogActivity.getString(R.string.subtitles), subtitles, selectedItem);
        dialog.show(dialogActivity.getSupportFragmentManager(), null);

    }

    public void addListener(SubtitlesOptionsDialogListener toAdd) {
        listeners.add(toAdd);
    }

    public void RemoteRequired() {
        // Notify everybody that may be interested.
        for (SubtitlesOptionsDialogListener hl : listeners)
            hl.onRemoteRequired();
    }


    /**
     * Generic dialog select listener
     * @param token
     * @param which
     */
    public void onDialogSelect(int token, int which) {
        switch (token) {
            case SELECT_SUBTITLES:
                Player.SetSubtitle setSubtitle;
                // 0 is to download subtitles, 1 is for sync, 2 is for none, other is for a specific subtitle index
                switch (which) {
                    case 0:
                        // Download subtitles. First check host version to see which method to call
                        HostInfo hostInfo = hostManager.getHostInfo();
                        if (hostInfo.getKodiVersionMajor() < 13) {
                            showDownloadSubtitlesPreGotham();
                        } else {
                            showDownloadSubtitlesPostGotham();
                        }
                        break;
                    case 1:
                        Input.ExecuteAction syncSubtitleAction = new Input.ExecuteAction(Input.ExecuteAction.SUBTITLEDELAY);
                        syncSubtitleAction.execute(hostManager.getConnection(), new ApiCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                if (dialogActivity==null) return;
                                //Notify the listeners that the remote is required
                                RemoteRequired();
                            }

                            @Override
                            public void onError(int errorCode, String description) { }
                        }, callbackHandler);
                        break;
                    case 2:
                        setSubtitle = new Player.SetSubtitle(currentActivePlayerId, Player.SetSubtitle.OFF, true);
                        setSubtitle.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        break;
                    default:
                        setSubtitle = new Player.SetSubtitle(currentActivePlayerId, which - ADDED_SUBTITLE_OPTIONS, true);
                        setSubtitle.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
                        break;
                }
                break;
        }

    }

    private void showDownloadSubtitlesPreGotham() {
        // Pre-Gotham
        Addons.ExecuteAddon action = new Addons.ExecuteAddon(Addons.ExecuteAddon.ADDON_SUBTITLES);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (dialogActivity==null) return;
                //Notify the listeners that the remote is required
                RemoteRequired();
            }

            @Override
            public void onError(int errorCode, String description) {
                if (dialogActivity==null) return;
                Toast.makeText(dialogActivity,
                        String.format(dialogActivity.getString(R.string.error_executing_subtitles), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    private void showDownloadSubtitlesPostGotham() {
        // Post-Gotham - HACK, HACK
        // Apparently Gui.ActivateWindow with subtitlesearch blocks the TCP listener thread on XBMC
        // While the subtitles windows is showing, i get no response to any call. See:
        // http://forum.xbmc.org/showthread.php?tid=198156
        // Forcing this call through HTTP works, as it doesn't block the TCP listener thread on XBMC
        HostInfo currentHostInfo = hostManager.getHostInfo();
        HostConnection httpHostConnection = new HostConnection(currentHostInfo);
        httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        GUI.ActivateWindow action = new GUI.ActivateWindow(GUI.ActivateWindow.SUBTITLESEARCH);

        LogUtils.LOGD(TAG, "Activating subtitles window.");
        action.execute(httpHostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                LogUtils.LOGD(TAG, "Sucessfully activated subtitles window.");
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Got an error activating subtitles window. Error: " + description);
            }
        }, callbackHandler);
        //Notify the listeners that the remote is required
        RemoteRequired();
    }


    @Override
    public void playerOnPropertyChanged(org.xbmc.kore.jsonrpc.notification.Player.NotificationsData notificationsData) {

    }

    @Override
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult, PlayerType.PropertyValue getPropertiesResult, ListType.ItemsAll getItemResult) {

        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    @Override
    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult, PlayerType.PropertyValue getPropertiesResult, ListType.ItemsAll getItemResult) {

        setNowPlayingInfo(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    /**
     * Sets whats playing information
     * @param getItemResult Return from method {@link org.xbmc.kore.jsonrpc.method.Player.GetItem}
     */
    private void setNowPlayingInfo(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                                   PlayerType.PropertyValue getPropertiesResult,
                                   final ListType.ItemsAll getItemResult) {

        currentActivePlayerId = getActivePlayerResult.playerid;
        if ((getPropertiesResult.subtitles != null) &&
                (getPropertiesResult.subtitles.size() > 0)) {

            availableSubtitles = getPropertiesResult.subtitles;
            currentSubtitleIndex = getPropertiesResult.currentsubtitle.index;

        }

    }

    @Override
    public void playerOnStop() {

    }

    @Override
    public void playerOnConnectionError(int errorCode, String description) {

    }

    @Override
    public void playerNoResultsYet() {

    }

    @Override
    public void systemOnQuit() {

    }

    @Override
    public void inputOnInputRequested(String title, String type, String value) {

    }

    @Override
    public void observerOnStopObserving() {

    }

    @Override
    protected void finalize() throws Throwable {
        try {
            hostConnectionObserver.unregisterPlayerObserver(this);
        } finally {
            super.finalize();
        }
    }
}
