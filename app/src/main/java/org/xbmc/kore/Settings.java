/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.text.format.DateUtils;

import org.xbmc.kore.utils.LogUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that contains various constants and the keys for settings stored in shared preferences
 */
public class Settings {
    private static final String TAG = LogUtils.makeLogTag(Settings.class);

    /**
     * The update interval for the records in the DB. If the last update is older than this value
     * a refresh will be triggered. Applicable to TV Shows and Movies.
     */
//    public static final long DB_UPDATE_INTERVAL = 12 * DateUtils.HOUR_IN_MILLIS;
    public static final long DB_UPDATE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    // Sort orders
    public static final int SORT_BY_NAME = 0,
            SORT_BY_DATE_ADDED = 1,
            SORT_BY_RATING = 2,
            SORT_BY_YEAR = 3,
            SORT_BY_LENGTH = 4,
            SORT_BY_ALBUM = 5,
            SORT_BY_ARTIST = 6,
            SORT_BY_ARTIST_YEAR = 7,
            SORT_BY_LAST_PLAYED = 8,
            UNSORTED = 9;

    /**
     * Preferences keys.
     * Some of these settings are automatically managed by the Preferences mechanism.
     * Make sure these are the same as in preferences.xml
     */

    // Theme
    public static final String KEY_PREF_THEME = "pref_theme";
    public static final String DEFAULT_PREF_THEME = "0";

    // Switch to remote
    public static final String KEY_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START = "pref_switch_to_remote_after_media_start";
    public static final boolean DEFAULT_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START = true;

    // Keep remote activity above lockscreen
    public static final String KEY_PREF_KEEP_REMOTE_ABOVE_LOCKSCREEN = "pref_keep_remote_above_lockscreen";
    public static final boolean DEFAULT_KEY_PREF_KEEP_REMOTE_ABOVE_LOCKSCREEN = false;

    // Keep screen on when on the remote activity
    public static final String KEY_PREF_KEEP_SCREEN_ON = "pref_keep_screen_on";
    public static final boolean DEFAULT_KEY_PREF_KEEP_SCREEN_ON = false;

    // Show now playing panel
    public static final String KEY_PREF_SHOW_NOW_PLAYING_PANEL = "pref_show_nowplayingpanel";
    public static final boolean DEFAULT_PREF_SHOW_NOW_PLAYING_PANEL = true;

    // Pause during calls
    public static final String KEY_PREF_PAUSE_DURING_CALLS = "pref_pause_during_calls";
    public static final boolean DEFAULT_PREF_PAUSE_DURING_CALLS = false;

    // Other keys used in preferences.xml
    public static final String KEY_PREF_ABOUT = "pref_about";

    // Filter watched movies on movie list
    public static final String KEY_PREF_MOVIES_FILTER_HIDE_WATCHED = "movies_filter_hide_watched";
    public static final boolean DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED = false;

    // Sort order on movies
    public static final String KEY_PREF_MOVIES_SORT_ORDER = "movies_sort_order";
    public static final int DEFAULT_PREF_MOVIES_SORT_ORDER = SORT_BY_NAME;

    // Show watched status on movie list
    public static final String KEY_PREF_MOVIES_SHOW_WATCHED_STATUS = "movies_show_watched_status";
    public static final boolean DEFAULT_PREF_MOVIES_SHOW_WATCHED_STATUS = true;

    // Show watched status on movie list
    public static final String KEY_PREF_MOVIES_SHOW_RATING = "movies_show_rating";
    public static final boolean DEFAULT_PREF_MOVIES_SHOW_RATING = true;

    // Sort order on albums
    public static final String KEY_PREF_ALBUMS_SORT_ORDER = "albums_sort_order";
    public static final int DEFAULT_PREF_ALBUMS_SORT_ORDER = SORT_BY_ALBUM;

    // Ignore articles on movie sorting
    public static final String KEY_PREF_MOVIES_IGNORE_PREFIXES = "movies_ignore_prefixes";
    public static final boolean DEFAULT_PREF_MOVIES_IGNORE_PREFIXES = false;

    // Filter watched tv shows on tvshows list
    public static final String KEY_PREF_TVSHOWS_FILTER_HIDE_WATCHED = "tvshows_filter_hide_watched";
    public static final boolean DEFAULT_PREF_TVSHOWS_FILTER_HIDE_WATCHED = false;

    // Filter watched episodes on episodes list
    public static final String KEY_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED = "tvshow_episodes_filter_hide_watched";
    public static final boolean DEFAULT_PREF_TVSHOW_EPISODES_FILTER_HIDE_WATCHED = false;

    // Sort order on tv shows
    public static final String KEY_PREF_TVSHOWS_SORT_ORDER = "tvshows_sort_order";
    public static final int DEFAULT_PREF_TVSHOWS_SORT_ORDER = SORT_BY_NAME;

    // Ignore articles on tv show sorting
    public static final String KEY_PREF_TVSHOWS_IGNORE_PREFIXES = "tvshows_ignore_prefixes";
    public static final boolean DEFAULT_PREF_TVSHOWS_IGNORE_PREFIXES = false;

    // Show watched status on movie list
    public static final String KEY_PREF_TVSHOWS_SHOW_WATCHED_STATUS = "tvshows_show_watched_status";
    public static final boolean DEFAULT_PREF_TVSHOWS_SHOW_WATCHED_STATUS = true;

    // Filter watched pvr recordings on movie list
    public static final String KEY_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED = "pvr_recordings_filter_hide_watched";
    public static final boolean DEFAULT_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED = false;

    // Sort order on pvr recordings
    public static final String KEY_PREF_PVR_RECORDINGS_SORT_ORDER = "pvr_recordings_sort_order";
    public static final int DEFAULT_PREF_PVR_RECORDINGS_SORT_ORDER = UNSORTED;

    // Filter disabled addons on addons list
    public static final String KEY_PREF_ADDONS_FILTER_HIDE_DISABLED = "addons_filter_hide_disabled";
    public static final boolean DEFAULT_PREF_ADDONS_FILTER_HIDE_DISABLED = false;

    // Use hardware volume keys to control volume
    public static final String KEY_PREF_USE_HARDWARE_VOLUME_KEYS = "pref_use_hardware_volume_keys";
    public static final boolean DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS = true;

    // Vibrate on remote button press
    public static final String KEY_PREF_VIBRATE_REMOTE_BUTTONS = "pref_vibrate_remote_buttons";
    public static final boolean DEFAULT_PREF_VIBRATE_REMOTE_BUTTONS = false;

    // Current host id
    public static final String KEY_PREF_CURRENT_HOST_ID = "current_host_id";
    public static final int DEFAULT_PREF_CURRENT_HOST_ID = -1;

    public static final String KEY_PREF_REMOTE_BAR_ITEMS = "pref_remote_bar_items";
    public static String getRemoteBarItemsPrefKey(int hostId) {
        return Settings.KEY_PREF_REMOTE_BAR_ITEMS + hostId;
    }

    public static final String KEY_PREF_ALWAYS_SENDTOKODI_ADDON = "pref_always_sendtokodi_addon";
    public static final boolean DEFAULT_PREF_ALWAYS_SENDTOKODI_ADDON = false;

    public static final String KEY_PREF_NAV_DRAWER_ITEMS = "pref_nav_drawer_items";
    public static String getNavDrawerItemsPrefKey(int hostId) {
        return Settings.KEY_PREF_NAV_DRAWER_ITEMS + hostId;
    }

    public static final String KEY_PREF_DOWNLOAD_TYPES = "pref_download_conn_types";

    public static final String KEY_PREF_SINGLE_COLUMN = "pref_single_multi_column";
    public static final boolean DEFAULT_PREF_SINGLE_COLUMN = false;

    public static final String KEY_PREF_LANGUAGE = "pref_language";
    public static final String KEY_PREF_SELECTED_LANGUAGE = "pref_selected_language";

    /**
     * Determines the bit flags used by {@link DownloadManager.Request} to correspond to the enabled network connections
     * from the settings screen.
     * @return {@link DownloadManager.Request} network types bit flags that are enabled or 0 if none are enabled
     */
    public static int allowedDownloadNetworkTypes(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> connPrefs = sharedPref.getStringSet(Settings.KEY_PREF_DOWNLOAD_TYPES,
                                                        new HashSet<>(Arrays.asList(new String[]{"0"})));
        int result = 0; // default none
        for(String pref : connPrefs) {
            switch( Integer.parseInt(pref) ) {
                case 0:
                    result |= DownloadManager.Request.NETWORK_WIFI;
                    break;
                case 1:
                    result |= DownloadManager.Request.NETWORK_MOBILE;
                    break;
                case 2: // currently -1 means all network types in DownloadManager
                    result |= ~0;
            }
        }
        return result;
    }

    /**
     * Keys for bookmarked addons stored in preferences
     */
    private static final String KEY_PREF_BOOKMARKED_ADDONS = "bookmarked";
    public static String getBookmarkedAddonsPrefKey(int hostId) {
        return Settings.KEY_PREF_BOOKMARKED_ADDONS + hostId;
    }
    private static final String KEY_PREF_NAME_BOOKMARKED_ADDON = "name_";
    public static String getNameBookmarkedAddonsPrefKey(int hostId) {
        return Settings.KEY_PREF_NAME_BOOKMARKED_ADDON + hostId + "_";
    }
    public static final String DEFAULT_PREF_NAME_BOOKMARKED_ADDON = "Content";

}
