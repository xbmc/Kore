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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Misc util methods for use with plugin URL
 */
public class PluginUrlUtils {

    public static boolean isHostArte(String host) {
        return host.equals("www.arte.tv");
    }

    public static String toPluginUrlArte(Uri playUri) {
        Pattern pattern = Pattern.compile("^https://www.arte.tv/[a-z]{2}/videos/([0-9]{6}-[0-9]{3}-[A-Z])/.*$");
        Matcher matcher = pattern.matcher(playUri.toString());
        if (matcher.matches()) {
            String kind="SHOW";
            String program_id=matcher.group(1);
            return "plugin://plugin.video.arteplussept/play/" + kind + "/" + program_id;
        }
        return null;
    }

    public static String toPluginUrlTwitch(Uri playUri) {
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

}
