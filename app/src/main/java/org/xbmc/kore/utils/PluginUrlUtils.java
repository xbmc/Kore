/*
 * Copyright 2020 STB Land. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.utils;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Misc util methods for use with plugin URL
 */
public class PluginUrlUtils {
    private static final String TAG = LogUtils.makeLogTag(PluginUrlUtils.class);

    /**
     * Converts a YouTube url to an URL for the default YouTube add-on (plugin.video.youtube)
     *
     * @param playUri some URL for YouTube
     * @return plugin URL
     */
    @Nullable
    public static String toDefaultYouTubePluginUrl(Uri playUri) {
        String host = playUri.getHost();
        String path = playUri.getPath();

        if (host.endsWith("youtube.com") && path.equals("/watch")) {
            String videoId = playUri.getQueryParameter("v");
            String playlistId = playUri.getQueryParameter("list");
            Uri.Builder pluginUri = new Uri.Builder()
                    .scheme("plugin")
                    .authority("plugin.video.youtube")
                    .path("play/");
            boolean valid = false;
            if (videoId != null) {
                valid = true;
                pluginUri.appendQueryParameter("video_id", videoId);
            }
            if (playlistId != null) {
                valid = true;
                pluginUri.appendQueryParameter("playlist_id", playlistId)
                        .appendQueryParameter("order", "default");
            }
            if (valid) {
                return pluginUri.build().toString();
            }
        } else if (host.endsWith("youtu.be") ||
            (host.endsWith("youtube.com") && (
                path.startsWith("/live/") || path.startsWith("/shorts/")))
        ) {
            return "plugin://plugin.video.youtube/play/?video_id="
                   + playUri.getLastPathSegment();
        }

        return null;
    }

    /**
     * Converts a YouTube url to an URL for the Invidious YouTube add-on (plugin.video.invidious)
     *
     * @param playUri some URL for YouTube
     * @return plugin URL
     */
    @Nullable
    public static String toInvidiousYouTubePluginUrl(Uri playUri) {
        String host = playUri.getHost();
        String path = playUri.getPath();

        Uri.Builder pluginUri = new Uri.Builder()
                .scheme("plugin")
                .authority("plugin.video.invidious")
                .path("/")
                .appendQueryParameter("action", "play_video");

        String videoIdParameterKey = "video_id";

        String videoId;
        if (host.endsWith("youtube.com") && path.equals("/watch")) {
            videoId = playUri.getQueryParameter("v");
        } else if (host.endsWith("youtu.be") ||
            (host.endsWith("youtube.com") && (
                path.startsWith("/live/") || path.startsWith("/shorts/")))) {
            videoId = playUri.getLastPathSegment();
        } else {
            return null;
        }

        if (videoId == null) {
            return null;
        }

        return pluginUri
                .appendQueryParameter(videoIdParameterKey, videoId)
                .build()
                .toString();
    }


    public static boolean isHostArte(String host) {
        return host.equals("www.arte.tv");
    }

    public static String toArtePluginUrl(Uri playUri) {
        Pattern pattern = Pattern.compile("^https://www.arte.tv/[a-z]{2}/videos/([0-9]{6}-[0-9]{3}-[A-Z])/.*$");
        Matcher matcher = pattern.matcher(playUri.toString());
        if (matcher.matches()) {
            String kind="SHOW";
            String program_id=matcher.group(1);
            return "plugin://plugin.video.arteplussept/play/" + kind + "/" + program_id;
        }
        return null;
    }

    public static String toTwitchPluginUrl(Uri playUri) {
        Matcher twitchStreamMatcher = Pattern.compile("twitch\\.tv/(\\w+)$").matcher(playUri.toString());
        if (twitchStreamMatcher.find()) {
            return "plugin://plugin.video.twitch/?mode=play&channel_name=" + twitchStreamMatcher.group(1);
        }
        Matcher twitchVodMatcher = Pattern.compile("twitch\\.tv/videos/(\\d+)$").matcher(playUri.toString());
        if (twitchVodMatcher.find()) {
            return "plugin://plugin.video.twitch/?mode=play&video_id=" + twitchVodMatcher.group(1);
        }
        return null;
    }

    public static String toVimeoPluginUrl(Uri playUri) {
        String route = playUri.getPath();
        String[] routePatterns = {
            "^\\/(?<id>\\d+)$",
            "^\\/(?<id>\\d+)\\/(?<hash>\\w+)$",
            "\\/(?<id>\\d+)$",
        };

        for (String routePattern: routePatterns) {
            Matcher routeMatcher = Pattern.compile(routePattern).matcher(route);

            if (routeMatcher.find()) {
                String videoId = routeMatcher.group(1);
                if (routeMatcher.groupCount() == 2) {
                    videoId = routeMatcher.group(1) + ":" + routeMatcher.group(2);
                }

                return "plugin://plugin.video.vimeo/play/?video_id=" + videoId;
            }
        }

        return null;
    }

    /**
     * Converts a SvtPlay uri to an uri for the the respective plugin
     *
     * @param playUri some URL for svtplay
     * @return plugin URI
     */
    public static String toSvtPlayPluginUrl(Uri playUri) {
        try {
            Pattern pattern = Pattern.compile(
                    "^(?:https?://)?(?:www\\.)?svtplay\\.se/video/(\\w+/.*)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(playUri.toString());
            if (matcher.matches()) {
                return "plugin://plugin.video.svtplay/?id=%2Fvideo%2F"
                       + URLEncoder.encode(matcher.group(1), StandardCharsets.UTF_8.name()) + "&mode=video";
            }
        } catch (UnsupportedEncodingException e) {
            LogUtils.LOGD(TAG, "Unsuported Encoding Exception: " + e);
        }
        return null;
    }

    /**
     * Converts a Soundcloud uri to an uri for the the respective plugin
     *
     * @param playUri some URL for soundcloud
     * @return plugin URI
     */
    public static String toSoundCloudPluginUrl(Uri playUri) {
        try {
            return "plugin://plugin.audio.soundcloud/play/?url="
                   + URLEncoder.encode(playUri.toString(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LogUtils.LOGD(TAG, "Unsuported Encoding Exception: " + e);
        }
        return null;
    }
}
