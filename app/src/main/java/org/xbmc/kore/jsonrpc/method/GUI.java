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
package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.utils.JsonUtils;

/**
 * All JSON RPC methods in GUI.*
 */
public class GUI {

    public static final class ActivateWindow extends ApiMethod<String> {
        public final static String METHOD_NAME = "GUI.ActivateWindow";

        /** All windows that we can navigate to */
        public final static String HOME = "home";
        public final static String PROGRAMS = "programs";
        public final static String PICTURES = "pictures";
        public final static String FILEMANAGER = "filemanager";
        public final static String FILES = "files";
        public final static String SETTINGS = "settings";
        public final static String MUSIC = "music";
        public final static String VIDEO = "video";
        public final static String VIDEOS = "videos";
        public final static String TV = "tv";
        public final static String PVR = "pvr";
        public final static String PVRGUIDEINFO = "pvrguideinfo";
        public final static String PVRRECORDINGINFO = "pvrrecordinginfo";
        public final static String PVRTIMERSETTING = "pvrtimersetting";
        public final static String PVRGROUPMANAGER = "pvrgroupmanager";
        public final static String PVRCHANNELMANAGER = "pvrchannelmanager";
        public final static String PVRGUIDESEARCH = "pvrguidesearch";
        public final static String PVRCHANNELSCAN = "pvrchannelscan";
        public final static String PVRUPDATEPROGRESS = "pvrupdateprogress";
        public final static String PVROSDCHANNELS = "pvrosdchannels";
        public final static String PVROSDGUIDE = "pvrosdguide";
        public final static String PVROSDDIRECTOR = "pvrosddirector";
        public final static String PVROSDCUTTER = "pvrosdcutter";
        public final static String PVROSDTELETEXT = "pvrosdteletext";
        public final static String SYSTEMINFO = "systeminfo";
        public final static String TESTPATTERN = "testpattern";
        public final static String SCREENCALIBRATION = "screencalibration";
        public final static String GUICALIBRATION = "guicalibration";
        public final static String PICTURESSETTINGS = "picturessettings";
        public final static String PROGRAMSSETTINGS = "programssettings";
        public final static String WEATHERSETTINGS = "weathersettings";
        public final static String MUSICSETTINGS = "musicsettings";
        public final static String SYSTEMSETTINGS = "systemsettings";
        public final static String VIDEOSSETTINGS = "videossettings";
        public final static String NETWORKSETTINGS = "networksettings";
        public final static String SERVICESETTINGS = "servicesettings";
        public final static String APPEARANCESETTINGS = "appearancesettings";
        public final static String PVRSETTINGS = "pvrsettings";
        public final static String TVSETTINGS = "tvsettings";
        public final static String SCRIPTS = "scripts";
        public final static String VIDEOFILES = "videofiles";
        public final static String VIDEOLIBRARY = "videolibrary";
        public final static String VIDEOPLAYLIST = "videoplaylist";
        public final static String LOGINSCREEN = "loginscreen";
        public final static String PROFILES = "profiles";
        public final static String SKINSETTINGS = "skinsettings";
        public final static String ADDONBROWSER = "addonbrowser";
        public final static String YESNODIALOG = "yesnodialog";
        public final static String PROGRESSDIALOG = "progressdialog";
        public final static String VIRTUALKEYBOARD = "virtualkeyboard";
        public final static String VOLUMEBAR = "volumebar";
        public final static String SUBMENU = "submenu";
        public final static String FAVOURITES = "favourites";
        public final static String CONTEXTMENU = "contextmenu";
        public final static String INFODIALOG = "infodialog";
        public final static String NUMERICINPUT = "numericinput";
        public final static String GAMEPADINPUT = "gamepadinput";
        public final static String SHUTDOWNMENU = "shutdownmenu";
        public final static String MUTEBUG = "mutebug";
        public final static String PLAYERCONTROLS = "playercontrols";
        public final static String SEEKBAR = "seekbar";
        public final static String MUSICOSD = "musicosd";
        public final static String ADDONSETTINGS = "addonsettings";
        public final static String VISUALISATIONSETTINGS = "visualisationsettings";
        public final static String VISUALISATIONPRESETLIST = "visualisationpresetlist";
        public final static String OSDVIDEOSETTINGS = "osdvideosettings";
        public final static String OSDAUDIOSETTINGS = "osdaudiosettings";
        public final static String VIDEOBOOKMARKS = "videobookmarks";
        public final static String FILEBROWSER = "filebrowser";
        public final static String NETWORKSETUP = "networksetup";
        public final static String MEDIASOURCE = "mediasource";
        public final static String PROFILESETTINGS = "profilesettings";
        public final static String LOCKSETTINGS = "locksettings";
        public final static String CONTENTSETTINGS = "contentsettings";
        public final static String SONGINFORMATION = "songinformation";
        public final static String SMARTPLAYLISTEDITOR = "smartplaylisteditor";
        public final static String SMARTPLAYLISTRULE = "smartplaylistrule";
        public final static String BUSYDIALOG = "busydialog";
        public final static String PICTUREINFO = "pictureinfo";
        public final static String ACCESSPOINTS = "accesspoints";
        public final static String FULLSCREENINFO = "fullscreeninfo";
        public final static String KARAOKESELECTOR = "karaokeselector";
        public final static String KARAOKELARGESELECTOR = "karaokelargeselector";
        public final static String SLIDERDIALOG = "sliderdialog";
        public final static String ADDONINFORMATION = "addoninformation";
        public final static String MUSICPLAYLIST = "musicplaylist";
        public final static String MUSICFILES = "musicfiles";
        public final static String MUSICLIBRARY = "musiclibrary";
        public final static String MUSICPLAYLISTEDITOR = "musicplaylisteditor";
        public final static String TELETEXT = "teletext";
        public final static String SELECTDIALOG = "selectdialog";
        public final static String MUSICINFORMATION = "musicinformation";
        public final static String OKDIALOG = "okdialog";
        public final static String MOVIEINFORMATION = "movieinformation";
        public final static String TEXTVIEWER = "textviewer";
        public final static String FULLSCREENVIDEO = "fullscreenvideo";
        public final static String FULLSCREENLIVETV = "fullscreenlivetv";
        public final static String VISUALISATION = "visualisation";
        public final static String SLIDESHOW = "slideshow";
        public final static String FILESTACKINGDIALOG = "filestackingdialog";
        public final static String KARAOKE = "karaoke";
        public final static String WEATHER = "weather";
        public final static String SCREENSAVER = "screensaver";
        public final static String VIDEOOSD = "videoosd";
        public final static String VIDEOMENU = "videomenu";
        public final static String VIDEOTIMESEEK = "videotimeseek";
        public final static String MUSICOVERLAY = "musicoverlay";
        public final static String VIDEOOVERLAY = "videooverlay";
        public final static String STARTWINDOW = "startwindow";
        public final static String STARTUP = "startup";
        public final static String PERIPHERALS = "peripherals";
        public final static String PERIPHERALSETTINGS = "peripheralsettings";
        public final static String EXTENDEDPROGRESSDIALOG = "extendedprogressdialog";
        public final static String MEDIAFILTER = "mediafilter";

        // Only on Gotham
        public final static String SUBTITLESEARCH = "subtitlesearch";

        /**
         * For use in params, to go directly to Movies
         */
        public final static String PARAM_MOVIE_TITLES = "MovieTitles";
        /**
         * For use in params, to go directly to TV shows
         */
        public final static String PARAM_TV_SHOWS_TITLES = "TvShowTitles";

        /**
         * Activates a window in XBMC. See class constants to check which windows are allowed.
         */
        public ActivateWindow(String window) {
            super();
            addParameterToRequest("window", window);
        }

        /**
         * Activates a window in XBMC. See class constants to check which windows are allowed.
         */
        public ActivateWindow(String window, String... parameters) {
            super();
            addParameterToRequest("window", window);
            addParameterToRequest("parameters", parameters);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Toggle fullscreen/GUI
     */
    public static final class SetFullscreen extends ApiMethod<Boolean> {
        public final static String METHOD_NAME = "GUI.SetFullscreen";

        /**
         * Toggle fullscreen/GUI
         */
        public SetFullscreen() {
            super();
            addParameterToRequest("fullscreen", "toggle");
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public Boolean resultFromJson(ObjectNode jsonObject) throws ApiException {
            return JsonUtils.booleanFromJsonNode(jsonObject, RESULT_NODE);
        }
    }

}
