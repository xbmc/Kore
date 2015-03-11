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

import android.text.format.DateUtils;

/**
 * Interface that contains various constants and the keys for settings stored in shared preferences
 */
public interface Settings {
    /**
     * The update interval for the records in the DB. If the last update is older than this value
     * a refresh will be triggered. Applicable to TV Shows and Movies.
     */
//    public static final long DB_UPDATE_INTERVAL = 12 * DateUtils.HOUR_IN_MILLIS;
    public static final long DB_UPDATE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    // Maximum pictures to show on cast list (-1 to show all)
    public static final int DEFAULT_MAX_CAST_PICTURES = 12;

    // Sort orders
    public static final int SORT_BY_NAME = 0,
            SORT_BY_DATE_ADDED = 1;

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

    // Show notifications
    public static final String KEY_PREF_SHOW_NOTIFICATION = "pref_show_notification";
    public static final boolean DEFAULT_PREF_SHOW_NOTIFICATION = false;

    // Other keys used in preferences.xml
    public static final String KEY_PREF_ABOUT = "pref_about";

    // Filter watched movies on movie list
    public static final String KEY_PREF_MOVIES_FILTER_HIDE_WATCHED = "movies_filter_hide_watched";
    public static final boolean DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED = false;

    // Sort order on movies
    public static final String KEY_PREF_MOVIES_SORT_ORDER = "movies_sort_order";
    public static final int DEFAULT_PREF_MOVIES_SORT_ORDER = SORT_BY_NAME;

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

    // Use hardware volume keys to control volume
    public static final String KEY_PREF_USE_HARDWARE_VOLUME_KEYS = "pref_use_hardware_volume_keys";
    public static final boolean DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS = true;

    // Current host id
    public static final String KEY_PREF_CURRENT_HOST_ID = "current_host_id";
    public static final int DEFAULT_PREF_CURRENT_HOST_ID = -1;
}
