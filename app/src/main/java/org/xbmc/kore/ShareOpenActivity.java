package org.xbmc.kore;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.host.actions.OpenSharedUrls;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.sections.localfile.HttpApp;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.utils.PluginUrlUtils;
import org.xbmc.kore.utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auxiliary activity with no UI that handles share intents to Play or Queue an item on Kodi.
 * Decodes the passed intent, determine which methods to call on Kodi, sends the appropriate calls
 * and opens the {@link RemoteActivity} if necessary.
 */
public class ShareOpenActivity extends Activity {
    private static final String TAG = LogUtils.makeLogTag(ShareOpenActivity.class);
    private HttpApp httpApp;
    private Dialog hostCheckProgressDialog;
    private Dialog hostPickerDialog;

    // ACTION to be used with the shortcut API that directly opens the remote
    public static final String DEFAULT_OPEN_ACTION = "org.xbmc.kore.OPEN_REMOTE_VIEW";
    // CATEGORY for dynamic Share Targets
    public static final String SHARE_TARGET_CATEGORY = "org.xbmc.kore.SHARE_TARGET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyAppTheme();
        super.onCreate(savedInstanceState);

        handleStartIntent(getIntent());
    }

    /**
     * Applies the user selected Kore theme, keeping the window transparent, so dialogs match the app
     */
    private void applyAppTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themeColor = prefs.getString(Settings.KEY_PREF_THEME_COLOR, Settings.DEFAULT_PREF_THEME_COLOR);
        String themeVariant = prefs.getString(Settings.KEY_PREF_THEME_VARIANT, Settings.DEFAULT_PREF_THEME_VARIANT);
        setTheme(Settings.getThemeResourceId(themeColor, themeVariant));
        getTheme().applyStyle(R.style.ThemeOverlay_Kore_ShareTranslucent, true);
        if (Utils.isSOrLater() && themeColor.equals(Settings.THEME_COLOR_SYSTEM)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (hostCheckProgressDialog != null && hostCheckProgressDialog.isShowing()) {
            hostCheckProgressDialog.dismiss();
        }
        if (hostPickerDialog != null && hostPickerDialog.isShowing()) {
            hostPickerDialog.dismiss();
        }
        super.onDestroy();
    }

    /**
     * Handles the intent that started this activity, namely to start playing something on Kodi
     * @param intent Start intent for the activity
     */
    protected void handleStartIntent(Intent intent) {
        handleStartIntent(intent, false);
    }

    /**
     * Handles the intent that started this activity, namely to start playing something on Kodi
     * @param intent Start intent for the activity
     * @param queue Whether to queue the item
     */
    protected void handleStartIntent(Intent intent, boolean queue) {
        final HostManager hostManager = HostManager.getInstance(this);

        // If a host was passed from the intent switch to it
        String shortcutId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID);
        HostInfo shortcutHost = null;
        if (shortcutId != null) {
            int hostId = Integer.parseInt(shortcutId);
            for (HostInfo host : hostManager.getHosts()) {
                if (host.getId() == hostId) {
                    hostManager.switchHost(host);
                    shortcutHost = host;
                    break;
                }
            }
        }

        final String action = intent.getAction();
        final String intentType = intent.getType();
        // Open the Remote activity for non-share actions and host shortcuts.
        if (action == null ||
            action.equals(DEFAULT_OPEN_ACTION) ||
            !(action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE) ||
              action.equals(Intent.ACTION_VIEW))) {
            openRemote();
            return;
        }

        List<HostInfo> hosts = hostManager.getHosts();
        if (hosts.isEmpty()) {
            openRemote();
            return;
        }

        List<HostInfo> hostsToCheck = shortcutHost == null ? hosts : List.of(shortcutHost);
        checkAvailableHosts(intent, queue, hostManager, hostsToCheck);
    }

    private void checkAvailableHosts(Intent intent, boolean queue, HostManager hostManager,
                                     List<HostInfo> hostsToCheck) {
        hostCheckProgressDialog = new MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_checking_hosts)
                .setOnCancelListener(dialog -> finish())
                .show();
        List<HostInfo> reachableHosts = new ArrayList<>();
        AtomicInteger pendingChecks = new AtomicInteger(hostsToCheck.size());
        Handler mainHandler = new Handler(Looper.getMainLooper());

        for (HostInfo host : hostsToCheck) {
            HostConnection connection = new HostConnection(host);
            connection.execute(new Application.GetProperties(Application.GetProperties.NAME),
                               new ApiCallback<>() {
                                   @Override
                                   public void onSuccess(ApplicationType.PropertyValue result) {
                                       completeHostCheck(connection, host, true, reachableHosts, pendingChecks,
                                                        intent, queue, hostManager);
                                   }

                                   @Override
                                   public void onError(int errorCode, String description) {
                                       completeHostCheck(connection, host, false, reachableHosts, pendingChecks,
                                                        intent, queue, hostManager);
                                   }
                               }, mainHandler);
        }
    }

    private void completeHostCheck(HostConnection connection, HostInfo host, boolean reachable,
                                   List<HostInfo> reachableHosts, AtomicInteger pendingChecks,
                                   Intent intent, boolean queue, HostManager hostManager) {
        if (connection.getProtocol() == HostConnection.PROTOCOL_TCP) {
            connection.disconnect();
        }

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (reachable) {
            reachableHosts.add(host);
        }

        if (pendingChecks.decrementAndGet() != 0) {
            return;
        }

        if (hostCheckProgressDialog != null && hostCheckProgressDialog.isShowing()) {
            hostCheckProgressDialog.dismiss();
        }
        hostCheckProgressDialog = null;

        // Keep the configured host order regardless of which probe answered first
        List<HostInfo> configuredHosts = hostManager.getHosts();
        reachableHosts.sort(Comparator.comparingInt(h -> {
            for (int i = 0; i < configuredHosts.size(); i++) {
                if (configuredHosts.get(i).getId() == h.getId()) return i;
            }
            return Integer.MAX_VALUE;
        }));

        if (reachableHosts.isEmpty()) {
            Toast.makeText(this, R.string.no_kodi_instances_available, Toast.LENGTH_LONG).show();
            finish();
        } else if (reachableHosts.size() == 1) {
            selectHostAndShare(reachableHosts.get(0), intent, queue, hostManager);
        } else if (shouldUseConnectedHost(hostManager, reachableHosts)) {
            selectHostAndShare(hostManager.getHostInfo(), intent, queue, hostManager);
        } else {
            showHostPicker(reachableHosts, intent, queue, hostManager);
        }
    }

    /**
     * Kore is considered actively open and connected when one of its screens is open
     * and the current host is reachable. Users can opt out through the "always ask" setting.
     */
    private boolean shouldUseConnectedHost(HostManager hostManager, List<HostInfo> reachableHosts) {
        boolean alwaysAsk = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_ALWAYS_ASK_SHARE_TARGET,
                            Settings.DEFAULT_PREF_ALWAYS_ASK_SHARE_TARGET);
        HostInfo current = hostManager.getHostInfo();
        boolean currentReachable = false;
        for (HostInfo host : reachableHosts) {
            if (current != null && host.getId() == current.getId()) {
                currentReachable = true;
                break;
            }
        }
        return !alwaysAsk && BaseActivity.isAppOpen() && currentReachable;
    }

    private void showHostPicker(List<HostInfo> hosts, Intent intent, boolean queue, HostManager hostManager) {
        String[] hostNames = new String[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            hostNames[i] = hosts.get(i).getName();
        }

        hostPickerDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_kodi_instance)
                .setItems(hostNames, (dialog, which) ->
                        selectHostAndShare(hosts.get(which), intent, queue, hostManager))
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void selectHostAndShare(HostInfo host, Intent intent, boolean queue, HostManager hostManager) {
        hostManager.switchHost(host);
        handleShareIntent(intent, queue, hostManager);
    }

    private void openRemote() {
        startActivity(new Intent(this, RemoteActivity.class));
        finish();
    }

    private void handleShareIntent(Intent intent, boolean queue, HostManager hostManager) {
        final String action = intent.getAction();
        final String intentType = intent.getType();

        // Extract one or more uris from the intent, and store them in a list
        List<Uri> uris = null;
        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        } else {
            Uri uri = extractUriFromIntent(intent);
            if (uri != null) {
                uris = List.of(uri);
            }
        }

        if (uris == null || uris.isEmpty()) {
            finish();
            return;
        }

        // Convert URIs to a list of URLs
        List<String> urls = new ArrayList<>();
        for (Uri uri : uris) {
            String url = toUrl(uri);
            if (url == null) {
                Toast.makeText(getApplicationContext(),
                               getString(R.string.error_starting_http_server),
                               Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            urls.add(url);
        }

        // Determine which playlist to use
        int playlistType;
        if (intentType == null) {
            playlistType = PlaylistType.VIDEO_PLAYLISTID;
        } else if (intentType.matches("audio.*")) {
            playlistType = PlaylistType.MUSIC_PLAYLISTID;
        } else if (intentType.matches("video.*")) {
            playlistType = PlaylistType.VIDEO_PLAYLISTID;
        } else if (intentType.matches("image.*")) {
            playlistType = PlaylistType.PICTURE_PLAYLISTID;
        } else {
            // Generic link or unknown type? Default to video:
            playlistType = PlaylistType.VIDEO_PLAYLISTID;
        }

        String title = getString(R.string.app_name);
        String text = getString(R.string.item_added_to_playlist);
        final Context context = this;
        new OpenSharedUrls(this, urls, title, text, queue, playlistType)
                .execute(hostManager.getConnection(),
                        new ApiCallback<>() {
                            @Override
                            public void onSuccess(Boolean wasAlreadyPlaying) {
                                String msg = queue && wasAlreadyPlaying ? getString(R.string.item_added_to_playlist)
                                                                        : getString(R.string.item_sent_to_kodi);
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                        .show();
                                // Don't display Kore after queueing from another app, otherwise start the remote
                                if (!queue)
                                    startActivity(new Intent(context, RemoteActivity.class)
                                                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                // Always finish as we don't have anything to show
                                finish();
                            }

                            @Override
                            public void onError(int errorCode, String description) {
                                LogUtils.LOGE(TAG, "Share failed: " + description);
                                Toast.makeText(context, description, Toast.LENGTH_SHORT)
                                        .show();
                                finish();
                            }
                        }, new Handler(Looper.getMainLooper()));
    }

    /**
     * Returns a shared URI from the given intent, if present. Since
     * intents can store URIs in different ways, multiple extraction
     * strategies are tried.
     *
     * @param intent The intent to extract a shared URI from
     * @return The URI extracted from the intent, or null if none was found
     */
    private Uri extractUriFromIntent(Intent intent) {
        Uri uri = null;

        if ("text/plain".equals(intent.getType())) {
            uri = getPlainTextUri(intent.getStringExtra(Intent.EXTRA_TEXT));
        } else {
            uri = intent.getData();
        }

        if (uri == null) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        if (uri == null) {
            uri = getUrlInsideIntent(intent);
        }

        return uri;
    }

    private Uri getUrlInsideIntent(Intent intent) {
        // Some apps hide the link in the clip, try to detect any link by casting the intent
        // to string a looking with a regular expression:

        Matcher matcher = Pattern.compile("https?://[^\\s]+").matcher(intent.toString());
        String matchedString;
        if (matcher.find()) {
            matchedString = matcher.group(0);
            if (matchedString != null && matchedString.endsWith("}")) {
                matchedString = matchedString.substring(0, matchedString.length() - 1);
            }
            return Uri.parse(matchedString);
        }
        return null;
    }

    /**
     * Returns the Uri that the some apps passes in EXTRA_TEXT
     * YouTube sends something like: [Video title]: [YouTube URL] so we need
     * to get the second part
     *
     * @param extraText EXTRA_TEXT passed in the intent
     * @return Uri present in extraText if present
     */
    private Uri getPlainTextUri(String extraText) {
        if (extraText == null) return null;

        for (String word : extraText.split(" ")) {
            if (word.startsWith("http://") || word.startsWith("https://")) {
                try {
                    URL validUri = new URL(word);
                    return Uri.parse(word);
                } catch (MalformedURLException exc) {
                    LogUtils.LOGD(TAG, "Got a malformed URL in an intent: " + word);
                    return null;
                }

            }
        }
        return null;
    }


    /**
     * Transform a given URI to a URL. Initiates a HTTP server to
     * serve content:// URIs, such as local files.
     *
     * @param uri Uri to transform to a URL
     * @return String A http(s) URL, or null if the HTTP server encountered an error
     */
    private String toUrl(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            HttpApp httpApp = getHttpApp();
            if (httpApp == null) return null;

            httpApp.addUri(uri);
            return httpApp.getLinkToFile();
        } else {
            String url = toPluginUrl(uri);
            if (url == null) {
                url = uri.toString();
            }
            return url;
        }
    }

    /**
     * Converts a video url to a Kodi plugin URL.
     *
     * @param playuri some URL
     * @return plugin URL
     */
    private String toPluginUrl(Uri playuri) {
        String host = playuri.getHost();
        String extension = MimeTypeMap.getFileExtensionFromUrl(playuri.toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (host == null)
            return null;

        boolean alwaysSendToKodi = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                                    .getBoolean(Settings.KEY_PREF_ALWAYS_SENDTOKODI_ADDON,
                                                                Settings.DEFAULT_PREF_ALWAYS_SENDTOKODI_ADDON);

        if (!alwaysSendToKodi) {
            if (host.endsWith("youtube.com") || host.endsWith("youtu.be")) {
                String preferredYouTubeAddonId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getString(Settings.KEY_PREF_YOUTUBE_ADDON_ID, Settings.DEFAULT_PREF_YOUTUBE_ADDON_ID);
                if (preferredYouTubeAddonId.equals("plugin.video.invidious")) {
                    return PluginUrlUtils.toInvidiousYouTubePluginUrl(playuri);
                } else if (preferredYouTubeAddonId.equals("plugin.video.piped")) {
                    return PluginUrlUtils.toPipedYouTubePluginUrl(playuri);
                } else {
                    return PluginUrlUtils.toDefaultYouTubePluginUrl(playuri);
                }
            } else if (host.endsWith("vimeo.com")) {
                return PluginUrlUtils.toVimeoPluginUrl(playuri);
            } else if (host.endsWith("svtplay.se")) {
                return PluginUrlUtils.toSvtPlayPluginUrl(playuri);
            } else if (host.endsWith("soundcloud.com")) {
                return PluginUrlUtils.toSoundCloudPluginUrl(playuri);
            } else if (host.endsWith("twitch.tv")) {
                return PluginUrlUtils.toTwitchPluginUrl(playuri);
            } else if (PluginUrlUtils.isHostArte(host)) {
                return PluginUrlUtils.toArtePluginUrl(playuri);
            }
        }
        if (host.startsWith("app.primevideo.com")) {
            // Prime Video cannot be handled by SendToKodi as it requires authentication:
            Matcher amazonMatcher = Pattern.compile("gti=([^&]+)").matcher(playuri.toString());
            if (amazonMatcher.find()) {
                String gti = amazonMatcher.group(1);
                return "plugin://plugin.video.amazon-test/?asin=" + gti + "&mode=PlayVideo&adult=0&name=&trailer=0&selbitrate=0";
            }
        } else if (!isMediaFile(mimeType)) {
            // SendToKodi is a Kodi addon that is able to extract URLs from generic
            // web URIs using the Python library "youtube-dl".
            // Use it as a last resort, unless the URI extension is a known media file
            // (in that case Kodi does not require an addon to play the link):
            return "plugin://plugin.video.sendtokodi/?" + playuri;
        }
        return null;
    }

    boolean isMediaFile(String mimeType) {
        if (mimeType == null) {
            return false;
        } else if (mimeType.startsWith("audio")) {
            return true;
        } else if (mimeType.startsWith("image")) {
            return true;
        } else if (mimeType.startsWith("video")) {
            return true;
        }
        return false;
    }

    private HttpApp getHttpApp() {
        if (httpApp == null) {
            try {
                httpApp = HttpApp.getInstance(this, 8080);
            } catch (IOException ioe) {
                return null;
            }
        }
        return httpApp;
    }
}
