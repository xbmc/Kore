/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.tcpserver.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.JsonResponse;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Addons;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Simulates Addons JSON-RPC API
 */
public class AddonsHandler extends ConnectionHandler {
    private static final String TAG = LogUtils.makeLogTag(AddonsHandler.class);

    private static final String ID_NODE = "id";

    public AddonsHandler() { }

    @Override
    public String[] getType() {
        return new String[]{Addons.GetAddons.METHOD_NAME};
    }

    @Override
    public ArrayList<JsonResponse> createResponse(String method, ObjectNode jsonRequest) {
        ArrayList<JsonResponse> jsonResponses = new ArrayList<>();

        int methodId = jsonRequest.get(ID_NODE).asInt(-1);

        switch (method) {
            case Addons.GetAddons.METHOD_NAME:
                try {
                    Addons.GetAddons getAddons = new Addons.GetAddons(methodId, jsonResult);
                    jsonResponses.add(getAddons);
                } catch (IOException e) {
                    LogUtils.LOGW(TAG, "Error creating GetAddons response: " + e.getMessage());
                }
                break;
            default:
                LogUtils.LOGD(TAG, "method: " + method + ", not implemented");
        }
        return jsonResponses;
    }

    private String jsonResult = "{\n" +
            "   \"jsonrpc\" : \"2.0\",\n" +
            "   \"id\" : \"libAddons\",\n" +
            "   \"result\" : {\n" +
            "      \"addons\" : [\n" +
            "         {\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.fanart.tv\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"Download backdrops from www.fanart.tv.com\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"3.1.4\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.fanart.tv%2ficon.png/\",\n" +
            "            \"summary\" : \"fanart.tv Scraper Library\",\n" +
            "            \"name\" : \"fanart.tv Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.fanart.tv\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.1.8\",\n" +
            "            \"type\" : \"kodi.resource.images\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"Weather Icons - Default\",\n" +
            "            \"addonid\" : \"resource.images.weathericons.default\",\n" +
            "            \"summary\" : \"Default Weather Icons\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fresource.images.weathericons.default%2ficon.png/\",\n" +
            "            \"description\" : \"Default set of Weather Icons shipped with Kodi\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/resource.images.weathericons.default\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"1.0.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"kodi.resource\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"author\" : \"Skipmode A1, Sparkline, Martijn\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"5.1.7\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"plugin.video.youtube\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"3.0.8\",\n" +
            "                  \"addonid\" : \"script.module.beautifulsoup\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"script.module.requests\",\n" +
            "                  \"version\" : \"2.4.3\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.14.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"disclaimer\" : \"For bugs, requests or general questions visit the Dumpert.nl thread on the XBMC forum.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"value\" : \"nl\",\n" +
            "                  \"key\" : \"language\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"value\" : \"video\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"Watch funny videos from Dumpert.nl (dutch)\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.dumpert\",\n" +
            "            \"name\" : \"Dumpert\",\n" +
            "            \"addonid\" : \"plugin.video.dumpert\",\n" +
            "            \"summary\" : \"Watch funny videos from Dumpert.nl (dutch)\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.dumpert%2ficon.png/\",\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.dumpert%2ffanart.jpg/\",\n" +
            "            \"version\" : \"1.1.4\",\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true\n" +
            "         },\n" +
            "         {\n" +
            "            \"name\" : \"Kodi Add-on repository\",\n" +
            "            \"addonid\" : \"repository.xbmc.org\",\n" +
            "            \"summary\" : \"Install Add-ons from Kodi.tv\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2frepository.xbmc.org%2ficon.png/\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"2.5.9\",\n" +
            "            \"type\" : \"xbmc.addon.repository\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.addon\",\n" +
            "                  \"version\" : \"12.0.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"disclaimer\" : \"Team Kodi did not make all the add-ons on this repository and are not responsible for their content\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"Download and install add-ons from the Official Kodi.tv add-on repository.[CR]  By using the official Repository you will be able to take advantage of our extensive file mirror service to help get you faster downloads from a region close to you.[CR]  All add-ons on this repository have under gone basic testing, if you find a broken or not working add-on please report it to Team Kodi so we can take any action needed.\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/repository.xbmc.org\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"jez500, Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.json\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"6.0.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"path\" : \"/usr/share/kodi/addons/webinterface.default\",\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"key\" : \"language\",\n" +
            "                  \"value\" : \"en\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"Browse and interact with your Music, Movies, TV Shows and more via a web browser. Stream music and videos to your browser. Edit and manage your Kodi media library.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"summary\" : \"Default web interface\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fwebinterface.default%2ficon.png/\",\n" +
            "            \"addonid\" : \"webinterface.default\",\n" +
            "            \"name\" : \"Kodi web interface - Chorus2\",\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.webinterface\",\n" +
            "            \"broken\" : false,\n" +
            "            \"version\" : \"2.4.4\",\n" +
            "            \"fanart\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"description\" : \"Black is a simple screensaver that will turn your screen black.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/screensaver.xbmc.builtin.black\",\n" +
            "            \"dependencies\" : [],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.0.31\",\n" +
            "            \"type\" : \"xbmc.ui.screensaver\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"Black\",\n" +
            "            \"addonid\" : \"screensaver.xbmc.builtin.black\",\n" +
            "            \"summary\" : \"Screensaver that turns your screen black\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fscreensaver.xbmc.builtin.black%2ficon.png/\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.albums.theaudiodb.com\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"TheAudioDB.com is a community driven database of audio releases. It is our aim to be the most simple, easy to use and accurate source for Music metadata on the web. We also provide an API to access our repository of data so it can be used in many popular HTPC and Mobile apps to give you the best possible audio experience without the hassle.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"3.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.fanart.tv\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"1.7.3\",\n" +
            "                  \"addonid\" : \"metadata.common.theaudiodb.com\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Olympia, Team Kodi\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.albums\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.2.0\",\n" +
            "            \"summary\" : \"TheAudioDb Album Scraper\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.albums.theaudiodb.com%2ficon.png/\",\n" +
            "            \"name\" : \"TheAudioDb Album Scraper\",\n" +
            "            \"addonid\" : \"metadata.albums.theaudiodb.com\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"addonid\" : \"audioencoder.xbmc.builtin.wma\",\n" +
            "            \"name\" : \"WMA encoder\",\n" +
            "            \"summary\" : \"WMA Audio Encoder\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2faudioencoder.xbmc.builtin.wma%2ficon.png/\",\n" +
            "            \"version\" : \"1.0.0\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.audioencoder\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.audioencoder\",\n" +
            "                  \"version\" : \"1.0.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"spiff\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"WMA Audio Encoder\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/audioencoder.xbmc.builtin.wma\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"path\" : \"/usr/share/kodi/addons/game.controller.default\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"The default media center controller is based on the Xbox 360 controller.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fgame.controller.default%2ficon.png/\",\n" +
            "            \"summary\" : \"Default Controller\",\n" +
            "            \"name\" : \"Default Controller\",\n" +
            "            \"addonid\" : \"game.controller.default\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"kodi.game.controller\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.0.3\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"dependencies\" : [],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"The Dim screensaver is a simple screensaver that will dim (fade out) your screen to a setable value between 20 and 100% .\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/screensaver.xbmc.builtin.dim\",\n" +
            "            \"addonid\" : \"screensaver.xbmc.builtin.dim\",\n" +
            "            \"name\" : \"Dim\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fscreensaver.xbmc.builtin.dim%2ficon.png/\",\n" +
            "            \"summary\" : \"Screensaver that dims your screen\",\n" +
            "            \"version\" : \"1.0.38\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.ui.screensaver\",\n" +
            "            \"broken\" : false\n" +
            "         },\n" +
            "         {\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fscript.module.beautifulsoup4%2ficon.png/\",\n" +
            "            \"summary\" : \"HTML/XML parser for quick-turnaround applications like screen-scraping\",\n" +
            "            \"addonid\" : \"script.module.beautifulsoup4\",\n" +
            "            \"name\" : \"BeautifulSoup4\",\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.python.module\",\n" +
            "            \"broken\" : false,\n" +
            "            \"version\" : \"4.5.3\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Leonard Richardson (leonardr@segfault.org)\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"version\" : \"2.25.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/script.module.beautifulsoup4\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"Beautiful Soup parses arbitrarily invalid SGML and provides a variety of methods and Pythonic idioms for iterating and searching the parse tree.\",\n" +
            "            \"extrainfo\" : []\n" +
            "         },\n" +
            "         {\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.imdb.com\",\n" +
            "                  \"version\" : \"2.7.8\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.themoviedb.org\",\n" +
            "                  \"version\" : \"2.13.1\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : true,\n" +
            "                  \"addonid\" : \"plugin.video.youtube\",\n" +
            "                  \"version\" : \"4.4.10\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"themoviedb.org is a free and open movie database. It's completely user driven by people like you. TMDb is currently used by millions of people every month and with their powerful API, it is also used by many popular media centers like Kodi to retrieve Movie Metadata, Posters and Fanart to enrich the user's experience.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.themoviedb.org\",\n" +
            "            \"addonid\" : \"metadata.themoviedb.org\",\n" +
            "            \"name\" : \"The Movie Database\",\n" +
            "            \"summary\" : \"TMDB Movie Scraper\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.themoviedb.org%2ficon.png/\",\n" +
            "            \"version\" : \"3.9.3\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.movies\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"disclaimer\" : \"Feel free to use this script. For information visit kodi.tv\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/service.xbmc.versioncheck\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"Kodi Version Check only supports a number of platforms/distros as releases may differ between them. For more information visit the kodi.tv website.\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"summary\" : \"Kodi Version Check checks if you are running latest released version.\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fservice.xbmc.versioncheck%2ficon.png/\",\n" +
            "            \"addonid\" : \"service.xbmc.versioncheck\",\n" +
            "            \"name\" : \"Version Check\",\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.service\",\n" +
            "            \"broken\" : false,\n" +
            "            \"version\" : \"0.3.22\",\n" +
            "            \"fanart\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"key\" : \"language\",\n" +
            "                  \"value\" : \"nl\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"value\" : \"video\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"Watch videos from Gamekings.nl (dutch)\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.gamekings\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"1.4.5\",\n" +
            "                  \"addonid\" : \"plugin.video.twitch\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"4.1.4\",\n" +
            "                  \"addonid\" : \"plugin.video.vimeo\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"plugin.video.youtube\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"5.1.7\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"script.module.beautifulsoup\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"3.0.8\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.4.3\",\n" +
            "                  \"addonid\" : \"script.module.requests\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"version\" : \"2.14.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Skipmode A1, Amelandbor\",\n" +
            "            \"disclaimer\" : \"For bugs, requests or general questions visit the Gamekings.nl thread on the Kodi forum.\",\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.gamekings%2ffanart.jpg/\",\n" +
            "            \"version\" : \"1.2.7\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"GameKings\",\n" +
            "            \"addonid\" : \"plugin.video.gamekings\",\n" +
            "            \"summary\" : \"Watch videos from Gamekings.nl (dutch)\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.gamekings%2ficon.png/\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fscript.module.beautifulsoup%2ficon.png/\",\n" +
            "            \"summary\" : \"HTML/XML parser for quick-turnaround applications like screen-scraping\",\n" +
            "            \"name\" : \"BeautifulSoup\",\n" +
            "            \"addonid\" : \"script.module.beautifulsoup\",\n" +
            "            \"type\" : \"xbmc.python.module\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"3.2.1\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Leonard Richardson (leonardr@segfault.org)\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/script.module.beautifulsoup\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"Beautiful Soup parses arbitrarily invalid SGML and provides a variety of methods and Pythonic idioms for iterating and searching the parse tree.\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1\n" +
            "         },\n" +
            "         {\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"PythonWare\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/script.module.pil\",\n" +
            "            \"addonid\" : \"script.module.pil\",\n" +
            "            \"name\" : \"Python Image Library\",\n" +
            "            \"summary\" : \"\",\n" +
            "            \"thumbnail\" : \"\",\n" +
            "            \"version\" : \"1.1.7\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.python.module\",\n" +
            "            \"broken\" : false\n" +
            "         },\n" +
            "         {\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.southpark_unofficial%2ffanart.jpg/\",\n" +
            "            \"version\" : \"0.4.5\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.southpark_unofficial%2ficon.png/\",\n" +
            "            \"summary\" : \"South Park Unofficial Player\",\n" +
            "            \"name\" : \"South Park\",\n" +
            "            \"addonid\" : \"plugin.video.southpark_unofficial\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.southpark_unofficial\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"key\" : \"language\",\n" +
            "                  \"value\" : \"en\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"value\" : \"video\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"Watch South Park episodes. The supported countries are the one that can view videos from http://southpark.cc.com or http://www.southpark.de.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"disclaimer\" : \"Some parts of this addon may not be legal in your country of residence - please check with your local laws before installing.\",\n" +
            "            \"author\" : \"Deroad\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         {\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.vimeo\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"Vimeo is a one of the biggest video-sharing websites of the world.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"key\" : \"provides\",\n" +
            "                  \"value\" : \"video\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"version\" : \"2.14.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"bromix\",\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.vimeo%2ffanart.jpg/\",\n" +
            "            \"version\" : \"4.1.4\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.vimeo%2ficon.png/\",\n" +
            "            \"summary\" : \"Plugin for Vimeo\",\n" +
            "            \"name\" : \"Vimeo\",\n" +
            "            \"addonid\" : \"plugin.video.vimeo\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.musicvideos\",\n" +
            "            \"broken\" : false,\n" +
            "            \"version\" : \"1.3.3\",\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.musicvideos.theaudiodb.com%2ffanart.jpg/\",\n" +
            "            \"summary\" : \"theaudiodb.com Music Video Scraper\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.musicvideos.theaudiodb.com%2ficon.png/\",\n" +
            "            \"addonid\" : \"metadata.musicvideos.theaudiodb.com\",\n" +
            "            \"name\" : \"TheAudioDb.com for Music Videos\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.musicvideos.theaudiodb.com\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"This scraper downloads Music Video information from TheAudioDB.com website. Due to various search difficulties the scraper currently expects the folder/filename to be formatted as 'artist - trackname' otherwise it will not return results. It is important to note the space between the hyphen.\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"3.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.fanart.tv\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"1.7.3\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.theaudiodb.com\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         {\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"addonid\" : \"script.module.requests\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.12.4\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"version\" : \"2.19.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"jdf76, bromix\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.youtube\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"YouTube is one of the biggest video-sharing websites of the world.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"value\" : \"video\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.youtube%2ficon.png/\",\n" +
            "            \"summary\" : \"Plugin for YouTube\",\n" +
            "            \"addonid\" : \"plugin.video.youtube\",\n" +
            "            \"name\" : \"YouTube\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"version\" : \"5.3.12\",\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.youtube%2ffanart.jpg/\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.3.1\",\n" +
            "            \"summary\" : \"Uitzendinggemist (NPO) - Watch free videos from Uitzendinggemist (only with a dutch ip-address)\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.uzg%2ficon.png/\",\n" +
            "            \"name\" : \"Uitzendinggemist (NPO)\",\n" +
            "            \"addonid\" : \"plugin.video.uzg\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.uzg\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"key\" : \"language\",\n" +
            "                  \"value\" : \"nl\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"value\" : \"video\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"Dutch Uitzendinggemist (NPO) videos NED1 / NED2 / NED3 (only with a dutch ip-address)\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Bas Magré (Opvolger)\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.4.0\",\n" +
            "                  \"addonid\" : \"script.module.xbmcswift2\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         {\n" +
            "            \"fanart\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fskin.estouchy%2fresources%2ffanart.jpg/\",\n" +
            "            \"version\" : \"1.1.9\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.gui.skin\",\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"Estouchy\",\n" +
            "            \"addonid\" : \"skin.estouchy\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fskin.estouchy%2fresources%2ficon.png/\",\n" +
            "            \"summary\" : \"Skin for touchscreen devices\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"Skin designed to be used on touchscreen devices like tablets and smartphones\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/skin.estouchy\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"5.12.0\",\n" +
            "                  \"addonid\" : \"xbmc.gui\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"addonid\" : \"script.module.requests\",\n" +
            "            \"name\" : \"requests\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fscript.module.requests%2ficon.png/\",\n" +
            "            \"summary\" : \"Python HTTP for Humans\",\n" +
            "            \"version\" : \"2.12.4\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.python.module\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.14.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"kennethreitz, beenje\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"Packed for KODI from https://github.com/kennethreitz/requests\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/script.module.requests\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"version\" : \"0.9.98\",\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fscript.lazytv%2ffanart.jpg/\",\n" +
            "            \"installed\" : true,\n" +
            "            \"type\" : \"xbmc.python.script\",\n" +
            "            \"broken\" : false,\n" +
            "            \"addonid\" : \"script.lazytv\",\n" +
            "            \"name\" : \"LazyTV\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fscript.lazytv%2ficon.png/\",\n" +
            "            \"summary\" : \"LazyTV\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"value\" : \"en\",\n" +
            "                  \"key\" : \"language\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"value\" : \"executable\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"You have a huge library of TV shows and you havent viewed half of it. So why does it feel like such a chore to sit down and watch something?\\nLazyTV is here to free you from your battles with indecision, instead letting you lean back and soak up content. With one click you can be channel-surfing your own library, or have what you probably want to watch pop up in a single window.\\nAfterall, you know you want to watch TV, so why do you also have to decide what specifically to watch?\\n\\nUnlike a smart playlist or skin widget, LazyTV doesnt just provide the first unwatched episode of a TV show. It provides the first unwatched episode AFTER the last watched one in your library. A small, but important, distinction.\\n\\nLazyTV offers two main functions:\\nThe first creates and launches a randomised playlist of the TV episodes. And not just any episodes, but the next episode it thinks you would want to watch. You also have the option to blend in your movies (both the watched and the unwatched) to complete the channel-surfing experience.\\nThe second main function populates a window with the next available episode for each of your TV Shows. One click and your viewing menu is there, immediately.\\n\\nCombine either of the main functions with a playlist of preselected shows to customise your experience even further.\\nSome TV shows, like cartoons or skit shows, can be viewed out of episodic order. So LazyTV gives you the ability to identify these shows and treat them differently. Those shows will be played in a random order.\\n\\nLazyTV also offers two minor functions that extend beyond the addon itself:\\nThe first is an option to be notified if you are about to watch an episode that has an unwatched episode before it. This function excludes the TV shows identified as able to be watched out of order.\\nThe second option posts a notification when you finish watching a TV episode telling you that the next show is available and asks if you want to view it now.\\n\\n\\nLazyTV contains a service that stores the next episodes' information and monitors your player to pre-empt database changes. This is my attempt to make the addon more responsive on my Raspberry Pi. The Pi still takes a while to \\\"warm-up\\\"; a full refresh of the episode data (which occurs at start-up and on a library update) takes about 30 seconds for my ~100 show library*. However, the show list window opens and the random player starts in less than 2 seconds.\\n\\n*The same update takes 2 seconds on my laptop with its i5 processor.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/script.lazytv\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"KodeKarnage\",\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"value\" : \"executable\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"The most powerful way to access content on Netflix and YouTube and Amazon Instant Video would be a web browser, if web browsers provided good native support for a 10-foot user interface. This add-on launches a browser and connects the arrow buttons on the remote control to the mouse pointer. This is the most user-friendly way to consume online content without needing a wireless keyboard.\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.program.remote.control.browser\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"4.3.2\",\n" +
            "                  \"addonid\" : \"script.module.beautifulsoup4\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"1.1.7\",\n" +
            "                  \"addonid\" : \"script.module.pil\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.24.0\",\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Chad Parry\",\n" +
            "            \"disclaimer\" : \"The experience will be degraded unless these external dependencies are installed: “psutil,” “pyalsaaudio,” “pylirc2,” and “Pillow.” Another helpful utility is “unclutter,” which automatically hides the mouse pointer. (On a Debian-based system, run “sudo apt-get install python-psutil python-alsaaudio python-pylirc python-pil unclutter”). Finally, a theme with a large mouse pointer will improve pointer visibility, (e.g., https://www.gnome-look.org/p/999574/).\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.0.5\",\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"Remote Control Browser\",\n" +
            "            \"addonid\" : \"plugin.program.remote.control.browser\",\n" +
            "            \"summary\" : \"Browse websites with a remote control\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.program.remote.control.browser%2ficon.png/\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"2.7.0\",\n" +
            "            \"type\" : \"xbmc.metadata.scraper.albums\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"Universal Album Scraper\",\n" +
            "            \"addonid\" : \"metadata.album.universal\",\n" +
            "            \"summary\" : \"Universal Scraper for Albums\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.album.universal%2ficon.png/\",\n" +
            "            \"description\" : \"This scraper collects information from the following supported sites: MusicBrainz, last.fm, allmusic.com and amazon.de, while grabs artwork from: fanart.tv, last.fm and allmusic.com. It can be set field by field that from which site you want that specific information.\\n\\nThe initial search is always done on MusicBrainz. In case allmusic and/or amazon.de links are not added on the MusicBrainz site, fields from allmusic.com and/or amazon.de cannot be fetched (very easy to add those missing links though).\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.album.universal\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.allmusic.com\",\n" +
            "                  \"version\" : \"3.1.0\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.fanart.tv\",\n" +
            "                  \"version\" : \"3.1.0\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.musicbrainz.org\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"1.8.1\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.theaudiodb.com\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Olympia, Team Kodi\",\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.0.0\",\n" +
            "            \"type\" : \"xbmc.audioencoder\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"AAC encoder\",\n" +
            "            \"addonid\" : \"audioencoder.xbmc.builtin.aac\",\n" +
            "            \"summary\" : \"AAC Audio Encoder\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2faudioencoder.xbmc.builtin.aac%2ficon.png/\",\n" +
            "            \"description\" : \"AAC Audio Encoder\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/audioencoder.xbmc.builtin.aac\",\n" +
            "            \"author\" : \"spiff\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"1.0.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.audioencoder\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.3.4\",\n" +
            "            \"summary\" : \"HTBackdrops Scraper Library\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.htbackdrops.com%2ficon.png/\",\n" +
            "            \"name\" : \"HTBackdrops Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.htbackdrops.com\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.htbackdrops.com\",\n" +
            "            \"description\" : \"Download backdrops from www.htbackdrops.com\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Team Kodi\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fresource.language.en_gb%2ficon.png/\",\n" +
            "            \"summary\" : \"English language pack\",\n" +
            "            \"name\" : \"English\",\n" +
            "            \"addonid\" : \"resource.language.en_gb\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"kodi.resource.language\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"2.0.1\",\n" +
            "            \"disclaimer\" : \"English is the default language for Kodi, removing it may cause issues\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"addonid\" : \"kodi.resource\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"1.0.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"path\" : \"/usr/share/kodi/addons/resource.language.en_gb\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"English version of all texts used in Kodi.\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1\n" +
            "         },\n" +
            "         {\n" +
            "            \"summary\" : \"AllMusic Music Scraper Library\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.allmusic.com%2ficon.png/\",\n" +
            "            \"name\" : \"AllMusic Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.allmusic.com\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"3.1.1\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.allmusic.com\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"Download Music information from www.allmusic.com\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"enabled\" : true\n" +
            "         },\n" +
            "         {\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"TheTVDB.com is a TV Scraper. The site is a massive open database that can be modified by anybody and contains full meta data for many shows in different languages. All content and images on the site have been contributed by their users for users and have a high standard or quality. The database schema and website are open source under the GPL.\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.tvdb.com\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.7.8\",\n" +
            "                  \"addonid\" : \"metadata.common.imdb.com\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.8.4\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.tvshows\",\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"The TVDB\",\n" +
            "            \"addonid\" : \"metadata.tvdb.com\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.tvdb.com%2ficon.png/\",\n" +
            "            \"summary\" : \"Fetch TV show metadata from TheTVDB.com\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"2.1.1\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"installed\" : true,\n" +
            "            \"name\" : \"MusicBrainz Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.musicbrainz.org\",\n" +
            "            \"summary\" : \"MusicBrainz Music Scraper Library\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.musicbrainz.org%2ficon.png/\",\n" +
            "            \"description\" : \"Download Music information from www.musicbrainz.org\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.musicbrainz.org\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.albums\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.0.0\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fmetadata.local%2ficon.png/\",\n" +
            "            \"summary\" : \"Local Infomation only pseudo-scraper\",\n" +
            "            \"name\" : \"Local information only\",\n" +
            "            \"addonid\" : \"metadata.local\",\n" +
            "            \"path\" : \"/usr/share/kodi/addons/metadata.local\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"description\" : \"Use local information only\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.metadata\"\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         {\n" +
            "            \"summary\" : \"TMDb Scraper Library\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.themoviedb.org%2ficon.png/\",\n" +
            "            \"name\" : \"The Movie Database Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.themoviedb.org\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"2.14.0\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.themoviedb.org\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"Download thumbs and fanarts from www.themoviedb.org\",\n" +
            "            \"enabled\" : true\n" +
            "         },\n" +
            "         {\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"kodi.resource\",\n" +
            "                  \"version\" : \"1.0.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"Kodi GUI sounds\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/usr/share/kodi/addons/resource.uisounds.kodi\",\n" +
            "            \"name\" : \"Kodi UI Sounds\",\n" +
            "            \"addonid\" : \"resource.uisounds.kodi\",\n" +
            "            \"summary\" : \"Kodi GUI sounds\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fresource.uisounds.kodi%2ficon.png/\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.0.0\",\n" +
            "            \"type\" : \"kodi.resource.uisounds\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true\n" +
            "         },\n" +
            "         {\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.theaudiodb.com\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"Download Music information from www.theaudiodb.com\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"broken\" : false,\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"version\" : \"1.9.0\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.theaudiodb.com%2ficon.png/\",\n" +
            "            \"summary\" : \"TheAudioDb Music Scraper Library\",\n" +
            "            \"name\" : \"TheAudioDb Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.theaudiodb.com\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"version\" : \"3.7.2\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.artists\",\n" +
            "            \"addonid\" : \"metadata.artists.universal\",\n" +
            "            \"name\" : \"Universal Artist Scraper\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.artists.universal%2ficon.png/\",\n" +
            "            \"summary\" : \"Universal Scraper for Artists\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"This scraper collects information from the following supported sites: TheAudioDb.com, MusicBrainz, last.fm, and allmusic.com, while grabs artwork from: fanart.tv, htbackdrops.com, last.fm and allmusic.com. It can be set field by field that from which site you want that specific information.\\n\\nThe initial search is always done on MusicBrainz. In case allmusic link is not added on the MusicBrainz site fields from allmusic.com cannot be fetched (very easy to add those missing links though).\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.artists.universal\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"3.1.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.allmusic.com\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"3.1.0\",\n" +
            "                  \"addonid\" : \"metadata.common.fanart.tv\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"1.3.2\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"metadata.common.htbackdrops.com\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"metadata.common.musicbrainz.org\",\n" +
            "                  \"optional\" : false\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"metadata.common.theaudiodb.com\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"1.8.1\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Olympia, Team Kodi\",\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/metadata.common.imdb.com\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"rating\" : -1,\n" +
            "            \"description\" : \"Download Movie information from www.imdb.com\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"author\" : \"Team Kodi\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"2.1.0\",\n" +
            "                  \"addonid\" : \"xbmc.metadata\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.metadata.scraper.library\",\n" +
            "            \"version\" : \"2.8.7\",\n" +
            "            \"fanart\" : \"\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fmetadata.common.imdb.com%2ficon.png/\",\n" +
            "            \"summary\" : \"IMDB Scraper Library\",\n" +
            "            \"addonid\" : \"metadata.common.imdb.com\",\n" +
            "            \"name\" : \"IMDB Scraper Library\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"disclaimer\" : \"\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"version\" : \"2.1.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"Jonathan Beluch (jbel)\",\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/script.module.xbmcswift2\",\n" +
            "            \"description\" : \"\",\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"summary\" : \"\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fscript.module.xbmcswift2%2ficon.png/\",\n" +
            "            \"addonid\" : \"script.module.xbmcswift2\",\n" +
            "            \"name\" : \"xbmcswift2\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.python.module\",\n" +
            "            \"version\" : \"2.4.0\",\n" +
            "            \"fanart\" : \"\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"path\" : \"/usr/share/kodi/addons/skin.estuary\",\n" +
            "            \"enabled\" : true,\n" +
            "            \"rating\" : -1,\n" +
            "            \"extrainfo\" : [],\n" +
            "            \"description\" : \"Estuary is the default skin for Kodi 17.0 and above. It attempts to be easy for first time Kodi users to understand and use.\",\n" +
            "            \"disclaimer\" : \"Estuary is the default skin for Kodi, removing it may cause issues\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"5.12.0\",\n" +
            "                  \"addonid\" : \"xbmc.gui\",\n" +
            "                  \"optional\" : false\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"phil65, Ichabod Fletchman\",\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.gui.skin\",\n" +
            "            \"installed\" : true,\n" +
            "            \"fanart\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fskin.estuary%2fresources%2ffanart.jpg/\",\n" +
            "            \"version\" : \"1.9.12\",\n" +
            "            \"thumbnail\" : \"image://%2fusr%2fshare%2fkodi%2faddons%2fskin.estuary%2fresources%2ficon.png/\",\n" +
            "            \"summary\" : \"Estuary skin by phil65. (Kodi's default skin)\",\n" +
            "            \"name\" : \"Estuary\",\n" +
            "            \"addonid\" : \"skin.estuary\"\n" +
            "         },\n" +
            "         {\n" +
            "            \"version\" : \"1.4.5\",\n" +
            "            \"fanart\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.twitch%2ffanart.jpg/\",\n" +
            "            \"installed\" : true,\n" +
            "            \"broken\" : false,\n" +
            "            \"type\" : \"xbmc.python.pluginsource\",\n" +
            "            \"addonid\" : \"plugin.video.twitch\",\n" +
            "            \"name\" : \"Twitch\",\n" +
            "            \"summary\" : \"Twitch video plugin\",\n" +
            "            \"thumbnail\" : \"image://%2fhome%2fmartijn%2f.kodi%2faddons%2fplugin.video.twitch%2ficon.png/\",\n" +
            "            \"extrainfo\" : [\n" +
            "               {\n" +
            "                  \"key\" : \"language\",\n" +
            "                  \"value\" : \"en\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"value\" : \"video\",\n" +
            "                  \"key\" : \"provides\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"description\" : \"Watch your favorite gaming streams!\",\n" +
            "            \"rating\" : -1,\n" +
            "            \"enabled\" : true,\n" +
            "            \"path\" : \"/home/martijn/.kodi/addons/plugin.video.twitch\",\n" +
            "            \"dependencies\" : [\n" +
            "               {\n" +
            "                  \"version\" : \"1.2.0\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"addonid\" : \"script.module.xbmcswift2\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"addonid\" : \"xbmc.python\",\n" +
            "                  \"optional\" : false,\n" +
            "                  \"version\" : \"2.20.0\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"author\" : \"A Talented Community\",\n" +
            "            \"disclaimer\" : \"\"\n" +
            "         }\n" +
            "      ],\n" +
            "      \"limits\" : {\n" +
            "         \"start\" : 0,\n" +
            "         \"end\" : 41,\n" +
            "         \"total\" : 41\n" +
            "      }\n" +
            "   }\n" +
            "}\n";
}
