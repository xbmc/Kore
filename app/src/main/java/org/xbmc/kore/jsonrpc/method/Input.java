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

/**
 * All JSON RPC methods in Input.*
 */
public class Input {

    /**
     * Execute action
     * Executes general actions on XBMC. See class constants for available actions.
     */
    public static final class ExecuteAction extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.ExecuteAction";

        /** Available actions */
        public final static String LEFT = "left";
        public final static String RIGHT = "right";
        public final static String UP = "up";
        public final static String DOWN = "down";
        public final static String PAGEUP = "pageup";
        public final static String PAGEDOWN = "pagedown";
        public final static String SELECT = "select";
        public final static String HIGHLIGHT = "highlight";
        public final static String PARENTDIR = "parentdir";
        public final static String PARENTFOLDER = "parentfolder";
        public final static String BACK = "back";
        public final static String PREVIOUSMENU = "previousmenu";
        public final static String INFO = "info";
        public final static String PAUSE = "pause";
        public final static String STOP = "stop";
        public final static String SKIPNEXT = "skipnext";
        public final static String SKIPPREVIOUS = "skipprevious";
        public final static String FULLSCREEN = "fullscreen";
        public final static String ASPECTRATIO = "aspectratio";
        public final static String STEPFORWARD = "stepforward";
        public final static String STEPBACK = "stepback";
        public final static String BIGSTEPFORWARD = "bigstepforward";
        public final static String BIGSTEPBACK = "bigstepback";
        public final static String OSD = "osd";
        public final static String SHOWSUBTITLES = "showsubtitles";
        public final static String NEXTSUBTITLE = "nextsubtitle";
        public final static String CODECINFO = "codecinfo";
        public final static String PLAYERPROCESSINFO = "playerprocessinfo";
        public final static String NEXTPICTURE = "nextpicture";
        public final static String PREVIOUSPICTURE = "previouspicture";
        public final static String ZOOMOUT = "zoomout";
        public final static String ZOOMIN = "zoomin";
        public final static String PLAYLIST = "playlist";
        public final static String QUEUE = "queue";
        public final static String ZOOMNORMAL = "zoomnormal";
        public final static String ZOOMLEVEL1 = "zoomlevel1";
        public final static String ZOOMLEVEL2 = "zoomlevel2";
        public final static String ZOOMLEVEL3 = "zoomlevel3";
        public final static String ZOOMLEVEL4 = "zoomlevel4";
        public final static String ZOOMLEVEL5 = "zoomlevel5";
        public final static String ZOOMLEVEL6 = "zoomlevel6";
        public final static String ZOOMLEVEL7 = "zoomlevel7";
        public final static String ZOOMLEVEL8 = "zoomlevel8";
        public final static String ZOOMLEVEL9 = "zoomlevel9";
        public final static String NEXTCALIBRATION = "nextcalibration";
        public final static String RESETCALIBRATION = "resetcalibration";
        public final static String ANALOGMOVE = "analogmove";
        public final static String ROTATE = "rotate";
        public final static String ROTATECCW = "rotateccw";
        public final static String CLOSE = "close";
        public final static String SUBTITLEDELAYMINUS = "subtitledelayminus";
        public final static String SUBTITLEDELAY = "subtitledelay";
        public final static String SUBTITLEDELAYPLUS = "subtitledelayplus";
        public final static String AUDIODELAYMINUS = "audiodelayminus";
        public final static String AUDIODELAY = "audiodelay";
        public final static String AUDIODELAYPLUS = "audiodelayplus";
        public final static String SUBTITLESHIFTUP = "subtitleshiftup";
        public final static String SUBTITLESHIFTDOWN = "subtitleshiftdown";
        public final static String SUBTITLEALIGN = "subtitlealign";
        public final static String AUDIONEXTLANGUAGE = "audionextlanguage";
        public final static String VERTICALSHIFTUP = "verticalshiftup";
        public final static String VERTICALSHIFTDOWN = "verticalshiftdown";
        public final static String NEXTRESOLUTION = "nextresolution";
        public final static String AUDIOTOGGLEDIGITAL = "audiotoggledigital";
        public final static String NUMBER0 = "number0";
        public final static String NUMBER1 = "number1";
        public final static String NUMBER2 = "number2";
        public final static String NUMBER3 = "number3";
        public final static String NUMBER4 = "number4";
        public final static String NUMBER5 = "number5";
        public final static String NUMBER6 = "number6";
        public final static String NUMBER7 = "number7";
        public final static String NUMBER8 = "number8";
        public final static String NUMBER9 = "number9";
        public final static String OSDLEFT = "osdleft";
        public final static String OSDRIGHT = "osdright";
        public final static String OSDUP = "osdup";
        public final static String OSDDOWN = "osddown";
        public final static String OSDSELECT = "osdselect";
        public final static String OSDVALUEPLUS = "osdvalueplus";
        public final static String OSDVALUEMINUS = "osdvalueminus";
        public final static String SMALLSTEPBACK = "smallstepback";
        public final static String FASTFORWARD = "fastforward";
        public final static String REWIND = "rewind";
        public final static String PLAY = "play";
        public final static String PLAYPAUSE = "playpause";
        public final static String DELETE = "delete";
        public final static String COPY = "copy";
        public final static String MOVE = "move";
        public final static String MPLAYEROSD = "mplayerosd";
        public final static String HIDESUBMENU = "hidesubmenu";
        public final static String SCREENSHOT = "screenshot";
        public final static String RENAME = "rename";
        public final static String TOGGLEWATCHED = "togglewatched";
        public final static String SCANITEM = "scanitem";
        public final static String RELOADKEYMAPS = "reloadkeymaps";
        public final static String VOLUMEUP = "volumeup";
        public final static String VOLUMEDOWN = "volumedown";
        public final static String MUTE = "mute";
        public final static String BACKSPACE = "backspace";
        public final static String SCROLLUP = "scrollup";
        public final static String SCROLLDOWN = "scrolldown";
        public final static String ANALOGFASTFORWARD = "analogfastforward";
        public final static String ANALOGREWIND = "analogrewind";
        public final static String MOVEITEMUP = "moveitemup";
        public final static String MOVEITEMDOWN = "moveitemdown";
        public final static String CONTEXTMENU = "contextmenu";
        public final static String SHIFT = "shift";
        public final static String SYMBOLS = "symbols";
        public final static String CURSORLEFT = "cursorleft";
        public final static String CURSORRIGHT = "cursorright";
        public final static String SHOWTIME = "showtime";
        public final static String ANALOGSEEKFORWARD = "analogseekforward";
        public final static String ANALOGSEEKBACK = "analogseekback";
        public final static String SHOWPRESET = "showpreset";
        public final static String PRESETLIST = "presetlist";
        public final static String NEXTPRESET = "nextpreset";
        public final static String PREVIOUSPRESET = "previouspreset";
        public final static String LOCKPRESET = "lockpreset";
        public final static String RANDOMPRESET = "randompreset";
        public final static String INCREASEVISRATING = "increasevisrating";
        public final static String DECREASEVISRATING = "decreasevisrating";
        public final static String SHOWVIDEOMENU = "showvideomenu";
        public final static String ENTER = "enter";
        public final static String INCREASERATING = "increaserating";
        public final static String DECREASERATING = "decreaserating";
        public final static String TOGGLEFULLSCREEN = "togglefullscreen";
        public final static String NEXTSCENE = "nextscene";
        public final static String PREVIOUSSCENE = "previousscene";
        public final static String NEXTLETTER = "nextletter";
        public final static String PREVLETTER = "prevletter";
        public final static String JUMPSMS2 = "jumpsms2";
        public final static String JUMPSMS3 = "jumpsms3";
        public final static String JUMPSMS4 = "jumpsms4";
        public final static String JUMPSMS5 = "jumpsms5";
        public final static String JUMPSMS6 = "jumpsms6";
        public final static String JUMPSMS7 = "jumpsms7";
        public final static String JUMPSMS8 = "jumpsms8";
        public final static String JUMPSMS9 = "jumpsms9";
        public final static String FILTER = "filter";
        public final static String FILTERCLEAR = "filterclear";
        public final static String FILTERSMS2 = "filtersms2";
        public final static String FILTERSMS3 = "filtersms3";
        public final static String FILTERSMS4 = "filtersms4";
        public final static String FILTERSMS5 = "filtersms5";
        public final static String FILTERSMS6 = "filtersms6";
        public final static String FILTERSMS7 = "filtersms7";
        public final static String FILTERSMS8 = "filtersms8";
        public final static String FILTERSMS9 = "filtersms9";
        public final static String FIRSTPAGE = "firstpage";
        public final static String LASTPAGE = "lastpage";
        public final static String GUIPROFILE = "guiprofile";
        public final static String RED = "red";
        public final static String GREEN = "green";
        public final static String YELLOW = "yellow";
        public final static String BLUE = "blue";
        public final static String INCREASEPAR = "increasepar";
        public final static String DECREASEPAR = "decreasepar";
        public final static String VOLAMPUP = "volampup";
        public final static String VOLAMPDOWN = "volampdown";
        public final static String CHANNELUP = "channelup";
        public final static String CHANNELDOWN = "channeldown";
        public final static String PREVIOUSCHANNELGROUP = "previouschannelgroup";
        public final static String NEXTCHANNELGROUP = "nextchannelgroup";
        public final static String LEFTCLICK = "leftclick";
        public final static String RIGHTCLICK = "rightclick";
        public final static String MIDDLECLICK = "middleclick";
        public final static String DOUBLECLICK = "doubleclick";
        public final static String WHEELUP = "wheelup";
        public final static String WHEELDOWN = "wheeldown";
        public final static String MOUSEDRAG = "mousedrag";
        public final static String MOUSEMOVE = "mousemove";
        public final static String NOOP = "noop";

        /**
         * Executes general actions on XBMC. See class constants for available actions.
         */
        public ExecuteAction(String action) {
            super();
            addParameterToRequest("action", action);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Goes to home window in GUI
     */
    public static final class Home extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Home";
        /**
         * Goes to home window in GUI
         */
        public Home() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Navigate left in GUI
     */
    public static final class Left extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Left";
        /**
         * Navigate left in GUI
         */
        public Left() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Navigate right in GUI
     */
    public static final class Right extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Right";
        /**
         * Navigate right in GUI
         */
        public Right() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Navigate up in GUI
     */
    public static final class Up extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Up";
        /**
         * Navigate up in GUI
         */
        public Up() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Navigate down in GUI
     */
    public static final class Down extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Down";
        /**
         * Navigate down in GUI
         */
        public Down() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Navigate back in GUI
     */
    public static final class Back extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Back";
        /**
         * Navigate down in GUI
         */
        public Back() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Select in GUI
     */
    public static final class Select extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.Select";
        /**
         * Select in GUI
         */
        public Select() {
            super();
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Send a generic (unicode) text
     */
    public static final class SendText extends ApiMethod<String> {
        public final static String METHOD_NAME = "Input.SendText";
        /**
         * Send a generic (unicode) text
         */
        public SendText(String text, boolean done) {
            super();
            addParameterToRequest("text", text);
            addParameterToRequest("done", done);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

}
