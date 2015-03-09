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
package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import org.xbmc.kore.utils.JsonUtils;

/**
 * Types in Addon.*
 */
public class AddonType {
    /**
     * Enums for Addon.Fields
     */
    public interface Fields {
        public final String NAME = "name";
        public final String VERSION = "version";
        public final String SUMMARY = "summary";
        public final String DESCRIPTION = "description";
        public final String PATH = "path";
        public final String AUTHOR = "author";
        public final String THUMBNAIL = "thumbnail";
        public final String DISCLAIMER = "disclaimer";
        public final String FANART = "fanart";
        public final String DEPENDENCIES = "dependencies";
        public final String BROKEN = "broken";
        public final String EXTRAINFO = "extrainfo";
        public final String RATING = "rating";
        public final String ENABLED = "enabled";

        public final static String[] allValues = new String[] {
                NAME, VERSION, SUMMARY, DESCRIPTION, PATH, AUTHOR, THUMBNAIL, DISCLAIMER,
                FANART, DEPENDENCIES, BROKEN, EXTRAINFO, RATING, ENABLED
        };
    }

    /**
     * Enums for Addon.Types
     */
    public interface Types {
        public final String UNKNOWN = "unknown";
        public final String XBMC_METADATA_SCRAPER_ALBUMS = "xbmc.metadata.scraper.albums";
        public final String XBMC_METADATA_SCRAPER_ARTISTS = "xbmc.metadata.scraper.artists";
        public final String XBMC_METADATA_SCRAPER_MOVIES = "xbmc.metadata.scraper.movies";
        public final String XBMC_METADATA_SCRAPER_MUSICVIDEOS = "xbmc.metadata.scraper.musicvideos";
        public final String XBMC_METADATA_SCRAPER_TVSHOWS = "xbmc.metadata.scraper.tvshows";
        public final String XBMC_UI_SCREENSAVER = "xbmc.ui.screensaver";
        public final String XBMC_PLAYER_MUSICVIZ = "xbmc.player.musicviz";
        public final String XBMC_PYTHON_PLUGINSOURCE = "xbmc.python.pluginsource";
        public final String XBMC_PYTHON_SCRIPT = "xbmc.python.script";
        public final String XBMC_PYTHON_WEATHER = "xbmc.python.weather";
        public final String XBMC_PYTHON_SUBTITLES = "xbmc.python.subtitles";
        public final String XBMC_PYTHON_LYRICS = "xbmc.python.lyrics";
        public final String XBMC_GUI_SKIN = "xbmc.gui.skin";
        public final String XBMC_GUI_WEBINTERFACE = "xbmc.gui.webinterface";
        public final String XBMC_PVRCLIENT = "xbmc.pvrclient";
        public final String XBMC_ADDON_VIDEO = "xbmc.addon.video";
        public final String XBMC_ADDON_AUDIO = "xbmc.addon.audio";
        public final String XBMC_ADDON_IMAGE = "xbmc.addon.image";
        public final String XBMC_ADDON_EXECUTABLE = "xbmc.addon.executable";
        public final String XBMC_SERVICE = "xbmc.service";

        public final static String[] allValues = new String[]{
                UNKNOWN, XBMC_METADATA_SCRAPER_ALBUMS, XBMC_METADATA_SCRAPER_ARTISTS,
                XBMC_METADATA_SCRAPER_MOVIES, XBMC_METADATA_SCRAPER_MUSICVIDEOS,
                XBMC_METADATA_SCRAPER_TVSHOWS, XBMC_UI_SCREENSAVER, XBMC_PLAYER_MUSICVIZ,
                XBMC_PYTHON_PLUGINSOURCE, XBMC_PYTHON_SCRIPT, XBMC_PYTHON_WEATHER,
                XBMC_PYTHON_SUBTITLES, XBMC_PYTHON_LYRICS, XBMC_GUI_SKIN, XBMC_GUI_WEBINTERFACE,
                XBMC_PVRCLIENT, XBMC_ADDON_VIDEO, XBMC_ADDON_AUDIO, XBMC_ADDON_IMAGE,
                XBMC_ADDON_EXECUTABLE, XBMC_SERVICE
        };
    }

    public static class Details extends ItemType.DetailsBase {
        public static final String ADDONID = "addonid";
        public static final String AUTHOR = "author";
        public static final String BROKEN = "broken";
//        public static final String DEPENDENCIES = "dependencies";
        public static final String DESCRIPTION = "description";
        public static final String DISCLAIMER = "disclaimer";
        public static final String ENABLED = "enabled";
//        public static final String EXTRAINFO = "extrainfo";
        public static final String FANART = "fanart";
        public static final String NAME = "name";
        public static final String PATH = "path";
        public static final String RATING = "rating";
        public static final String SUMMARY = "summary";
        public static final String THUMBNAIL = "thumbnail";
        public static final String TYPE = "type";
        public static final String VERSION = "version";

        public final String addonid;
        public final String author;
        public final boolean broken;
        public final String description;
        public final String disclaimer;
        public final Boolean enabled;
        public final String fanart;
        public final String name;
        public final String path;
        public final int rating;
        public final String summary;
        public final String thumbnail;
        public final String type;
        public final String version;

        /**
         * Constructor
         * @param node JSON object representing a Detail object
         */
        public Details(JsonNode node) {
            super(node);
            addonid = JsonUtils.stringFromJsonNode(node, ADDONID);
            author = JsonUtils.stringFromJsonNode(node, AUTHOR);
            broken = JsonUtils.booleanFromJsonNode(node, BROKEN, false);
            description = JsonUtils.stringFromJsonNode(node, DESCRIPTION);
            disclaimer = JsonUtils.stringFromJsonNode(node, DISCLAIMER);
            enabled = JsonUtils.booleanFromJsonNode(node, ENABLED, false);
            fanart = JsonUtils.stringFromJsonNode(node, FANART);
            name = JsonUtils.stringFromJsonNode(node, NAME);
            path = JsonUtils.stringFromJsonNode(node, PATH);
            rating = JsonUtils.intFromJsonNode(node, RATING, 0);
            summary = JsonUtils.stringFromJsonNode(node, SUMMARY);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
            type = JsonUtils.stringFromJsonNode(node, TYPE);
            version = JsonUtils.stringFromJsonNode(node, VERSION);
        }

    }
}
