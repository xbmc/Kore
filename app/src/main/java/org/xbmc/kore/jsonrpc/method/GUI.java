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
        public final static String PICTURES = "pictures";
        public final static String SETTINGS = "settings";
        public final static String MUSIC = "music";
        public final static String VIDEOS = "videos";
        public final static String TVCHANNELS = "tvchannels";
        public final static String ADDONBROWSER = "addonbrowser";
        public final static String WEATHER = "weather";

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
         * For use in params, to go to root screen
         */
        public final static String PARAM_ROOT = "Root";

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
