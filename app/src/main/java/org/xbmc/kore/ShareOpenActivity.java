package org.xbmc.kore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.preference.PreferenceManager;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.host.actions.OpenSharedUrls;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.sections.localfile.HttpApp;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.PluginUrlUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
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

    // ACTION to be used with the shortcut API that directly opens the remote
    public static final String DEFAULT_OPEN_ACTION = "org.xbmc.kore.OPEN_REMOTE_VIEW";
    // CATEGORY for dynamic Share Targets
    public static final String SHARE_TARGET_CATEGORY = "org.xbmc.kore.SHARE_TARGET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleStartIntent(getIntent());
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
        LogUtils.LOGD(TAG, "Got Share Intent: " + intent);
        final HostManager hostManager = HostManager.getInstance(this);

        // If a host was passed from the intent switch to it
        String shortcutId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID);
        if (shortcutId != null) {
            int hostId = Integer.parseInt(shortcutId);
            for (HostInfo host : hostManager.getHosts()) {
                if (host.getId() == hostId) {
                    LogUtils.LOGD(TAG, "Switching hosts");
                    hostManager.switchHost(host);
                    break;
                }
            }
        }

        final String action = intent.getAction();
        final String intentType = intent.getType();
        // Check action: open the Remote activity if no action specified, no host connection (no hosts configured?),
        // default open specified (switch host?) or any other action other than Send or View
        if (action == null ||
            hostManager.getConnection() == null ||
            action.equals(DEFAULT_OPEN_ACTION) ||
            !(action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE) ||
              action.equals(Intent.ACTION_VIEW))) {
            startActivity(new Intent(this, RemoteActivity.class));
            finish();
            return;
        }

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
            // Generic links? Default to video:
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
