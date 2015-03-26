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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.utils.JsonUtils;

import java.util.List;

/**
 * Types defined in List.*
 */
public class ListType {

    /**
     * List.Item.Base type
     */
    public static class ItemBase {
        public static final String TYPE_MOVIE = "movie";
        public static final String TYPE_EPISODE = "episode";
        public static final String TYPE_SONG = "song";
        public static final String TYPE_MUSIC_VIDEO = "musicvideo";

        // From List.Item.Base
        public static final String ALBUM = "album";
        public static final String ALBUMARTIST = "albumartist";
        public static final String ALBUMARTISTID = "albumartistid";
        public static final String ALBUMID = "albumid";
        public static final String ALBUMLABEL = "albumlabel";
        public static final String CAST = "cast";
        public static final String COMMENT = "comment";
        public static final String COUNTRY = "country";
        public static final String DESCRIPTION = "description";
        public static final String DISC = "disc";
        public static final String DURATION = "duration";
        public static final String EPISODE = "episode";
        public static final String EPISODEGUIDE = "episodeguide";
        public static final String FIRSTAIRED = "firstaired";
        public static final String ID = "id";
        public static final String IMDBNUMBER = "imdbnumber";
        public static final String LYRICS = "lyrics";
        public static final String MOOD = "mood";
        public static final String MPAA = "mpaa";
        public static final String MUSICBRAINZARTISTID = "musicbrainzartistid";
        public static final String MUSICBRAINZTRACKID = "musicbrainztrackid";
        public static final String ORIGINALTITLE = "originaltitle";
        public static final String PLOTOUTLINE = "plotoutline";
        public static final String PREMIERED = "premiered";
        public static final String PRODUCTIONCODE = "productioncode";
        public static final String SEASON = "season";
        public static final String SET = "set";
        public static final String SETID = "setid";
        public static final String SHOWLINK = "showlink";
        public static final String SHOWTITLE = "showtitle";
        public static final String SORTTITLE = "sorttitle";
        public static final String STUDIO = "studio";
        public static final String STYLE = "style";
        public static final String TAG = "tag";
        public static final String TAGLINE = "tagline";
        public static final String THEME = "theme";
        public static final String TOP250 = "top250";
        public static final String TRACK = "track";
        public static final String TRAILER = "trailer";
        public static final String TVSHOWID = "tvshowid";
        public static final String TYPE = "type";
        public static final String UNIQUEID = "uniqueid";
        public static final String VOTES = "votes";
        public static final String WATCHEDEPISODES = "watchedepisodes";
        public static final String WRITER = "writer";

        public final String album;
        public final List<String> albumartist;
        public final List<Integer> albumartistid;
        public final int albumid;
        public final String albumlabel;
        public final List<VideoType.Cast> cast;
        public final String comment;
        public final List<String> country;
        public final String description;
        public final int disc;
        public final int duration;
        public final int episode;
        public final String episodeguide;
        public final String firstaired;
        public final int id;
        public final String imdbnumber;
        public final String lyrics;
        public final List<String> mood;
        public final String mpaa;
        public final String musicbrainzartistid;
        public final String musicbrainztrackid;
        public final String originaltitle;
        public final String plotoutline;
        public final String premiered;
        public final String productioncode;
        public final int season;
        public final String set;
        public final int setid;
        public final List<String> showlink;
        public final String showtitle;
        public final String sorttitle;
        public final List<String> studio;
        public final List<String> style;
        public final List<String> tag;
        public final String tagline;
        public final List<String> theme;
        public final int top250;
        public final int track;
        public final String trailer;
        public final int tvshowid;
        public final String type;
        //        public final HashMap<String, String> uniqueid;
        public final String votes;
        public final int watchedepisodes;
        public final List<String> writer;


        // From Video.Details.Base
        public static final String ART = "art";
        public static final String PLAYCOUNT = "playcount";

        public MediaType.Artwork art;
        public int playcount;


        // From Audio.Details.Media
        public static final String ARTIST = "artist";
        public static final String ARTISTID = "artistid";
        public static final String DISPLAYARTIST = "displayartist";
        public static final String GENREID = "genreid";
        public static final String MUSICBRAINZALBUMARTISTID = "musicbrainzalbumartistid";
        public static final String MUSICBRAINZALBUMID = "musicbrainzalbumid";
        public static final String RATING = "rating";
        public static final String TITLE = "title";
        public static final String YEAR = "year";

        public final List<String> artist;
        public final List<Integer> artistid;
        public final String displayartist;
        public final List<Integer> genreid;
        public final String musicbrainzalbumartistid;
        public final String musicbrainzalbumid;
        public final double rating;
        public final String title;
        public final int year;


        // From Video.Details.Item
        public static final String DATEADDED = "dateadded";
        public static final String FILE = "file";
        public static final String LASTPLAYED = "lastplayed";
        public static final String PLOT = "plot";

        public final String dateadded;
        public final String file;
        public final String lastplayed;
        public final String plot;


        // From Video.Details.File
        public static final String DIRECTOR = "director";
        public static final String RESUME = "resume";
        public static final String RUNTIME = "runtime";
        public static final String STREAMDETAILS = "streamdetails";

        public final List<String> director;
        public final VideoType.Resume resume;
        public final int runtime;
        public final VideoType.Streams streamdetails;


        // From Media.Details.Base
        public static final String FANART = "fanart";
        public static final String THUMBNAIL = "thumbnail";

        public final String fanart;
        public final String thumbnail;


        // From Audio.Details.Base
        public static final String GENRE = "genre";

        public final List<String> genre;


        // From <tt>Item.Details.Base</tt>.
        public static final String LABEL = "label";

        public final String label;


        /**
         * Constructor.
         *
         * @param node JSON object
         */
        public ItemBase(JsonNode node) {
            album = JsonUtils.stringFromJsonNode(node, ALBUM, null);
            albumartist = JsonUtils.stringListFromJsonNode(node, ALBUMARTIST);
            albumartistid = JsonUtils.integerListFromJsonNode(node, ALBUMARTISTID);
            albumid = JsonUtils.intFromJsonNode(node, ALBUMID, -1);
            albumlabel = JsonUtils.stringFromJsonNode(node, ALBUMLABEL, null);
            art = new MediaType.Artwork(node.get(ART));
            artist = JsonUtils.stringListFromJsonNode(node, ARTIST);
            artistid = JsonUtils.integerListFromJsonNode(node, ARTISTID);
            cast = VideoType.Cast.castListFromJsonNode(node, CAST);
            comment = JsonUtils.stringFromJsonNode(node, COMMENT, null);
            country = JsonUtils.stringListFromJsonNode(node, COUNTRY);
            dateadded = JsonUtils.stringFromJsonNode(node, DATEADDED, null);
            description = JsonUtils.stringFromJsonNode(node, DESCRIPTION, null);
            director = JsonUtils.stringListFromJsonNode(node, DIRECTOR);
            disc = JsonUtils.intFromJsonNode(node, DISC, 0);
            displayartist = JsonUtils.stringFromJsonNode(node, DISPLAYARTIST, null);
            duration = JsonUtils.intFromJsonNode(node, DURATION, 0);
            episode = JsonUtils.intFromJsonNode(node, EPISODE, 0);
            episodeguide = JsonUtils.stringFromJsonNode(node, EPISODEGUIDE, null);
            fanart = JsonUtils.stringFromJsonNode(node, FANART, null);
            file = JsonUtils.stringFromJsonNode(node, FILE, null);
            firstaired = JsonUtils.stringFromJsonNode(node, FIRSTAIRED, null);
            genre = JsonUtils.stringListFromJsonNode(node, GENRE);
            genreid = JsonUtils.integerListFromJsonNode(node, GENREID);
            id = JsonUtils.intFromJsonNode(node, ID, -1);
            imdbnumber = JsonUtils.stringFromJsonNode(node, IMDBNUMBER, null);
            label = JsonUtils.stringFromJsonNode(node, LABEL, null);
            lastplayed = JsonUtils.stringFromJsonNode(node, LASTPLAYED, null);
            lyrics = JsonUtils.stringFromJsonNode(node, LYRICS, null);
            mood = JsonUtils.stringListFromJsonNode(node, MOOD);
            mpaa = JsonUtils.stringFromJsonNode(node, MPAA, null);
            musicbrainzalbumartistid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZALBUMARTISTID, null);
            musicbrainzalbumid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZALBUMID, null);
            musicbrainzartistid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZARTISTID, null);
            musicbrainztrackid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZTRACKID, null);
            originaltitle = JsonUtils.stringFromJsonNode(node, ORIGINALTITLE, null);
            playcount = JsonUtils.intFromJsonNode(node, PLAYCOUNT, 0);
            plot = JsonUtils.stringFromJsonNode(node, PLOT, null);
            plotoutline = JsonUtils.stringFromJsonNode(node, PLOTOUTLINE, null);
            premiered = JsonUtils.stringFromJsonNode(node, PREMIERED, null);
            productioncode = JsonUtils.stringFromJsonNode(node, PRODUCTIONCODE, null);
            rating = JsonUtils.doubleFromJsonNode(node, RATING, 0);
            resume = node.has(RESUME) ? new VideoType.Resume(node.get(RESUME)) : null;
            runtime = JsonUtils.intFromJsonNode(node, RUNTIME, -1);
            season = JsonUtils.intFromJsonNode(node, SEASON, 0);
            set = JsonUtils.stringFromJsonNode(node, SET, null);
            setid = JsonUtils.intFromJsonNode(node, SETID, -1);
            showlink = JsonUtils.stringListFromJsonNode(node, SHOWLINK);
            showtitle = JsonUtils.stringFromJsonNode(node, SHOWTITLE, null);
            sorttitle = JsonUtils.stringFromJsonNode(node, SORTTITLE, null);
            streamdetails = node.has(STREAMDETAILS) ? new VideoType.Streams(node.get(STREAMDETAILS)) : null;
            studio = JsonUtils.stringListFromJsonNode(node, STUDIO);
            style = JsonUtils.stringListFromJsonNode(node, STYLE);
            tag = JsonUtils.stringListFromJsonNode(node, TAG);
            tagline = JsonUtils.stringFromJsonNode(node, TAGLINE, null);
            theme = JsonUtils.stringListFromJsonNode(node, THEME);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL, null);
            title = JsonUtils.stringFromJsonNode(node, TITLE, null);
            top250 = JsonUtils.intFromJsonNode(node, TOP250, 0);
            track = JsonUtils.intFromJsonNode(node, TRACK, 0);
            trailer = JsonUtils.stringFromJsonNode(node, TRAILER, null);
            tvshowid = JsonUtils.intFromJsonNode(node, TVSHOWID, -1);
            type = JsonUtils.stringFromJsonNode(node, TYPE, null);
//            uniqueid = getStringMap(node, UNIQUEID);
            votes = JsonUtils.stringFromJsonNode(node, VOTES, null);
            watchedepisodes = JsonUtils.intFromJsonNode(node, WATCHEDEPISODES, -1);
            writer = JsonUtils.stringListFromJsonNode(node, WRITER);
            year = JsonUtils.intFromJsonNode(node, YEAR, -1);
        }
    }

    public static class ItemsAll extends ItemBase {
        public static final String CHANNEL = "channel";
        public static final String CHANNELNUMBER = "channelnumber";
        public static final String CHANNELTYPE = "channeltype";
        public static final String ENDTIME = "endtime";
        public static final String HIDDEN = "hidden";
        public static final String LOCKED = "locked";
        public static final String STARTTIME = "starttime";

        // class members
        public final String channel;
        public final int channelnumber;
        public final String channeltype;
        public final String endtime;
        public final boolean hidden;
        public final boolean locked;
        public final String starttime;

        public ItemsAll(JsonNode node) {
            super(node);

            channel = JsonUtils.stringFromJsonNode(node, CHANNEL, null);
            channelnumber = JsonUtils.intFromJsonNode(node, CHANNELNUMBER, 0);
            channeltype = JsonUtils.stringFromJsonNode(node, CHANNELTYPE, "tv");
            endtime = JsonUtils.stringFromJsonNode(node, ENDTIME, null);
            hidden = JsonUtils.booleanFromJsonNode(node, HIDDEN, false);
            locked = JsonUtils.booleanFromJsonNode(node, LOCKED, false);
            starttime = JsonUtils.stringFromJsonNode(node, STARTTIME, null);
        }
    }

    /**
     * List.Limits
     */
    public static class Limits
            implements ApiParameter {
        public static final String START = "start";
        public static final String END = "end";

        protected static final ObjectMapper objectMapper = new ObjectMapper();

        public final int start;
        public final int end;

        public Limits(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put(START, start);
            node.put(END, end);
            return node;
        }
    }


    /**
     * Enums for List.Fields.All
     */
    public interface FieldsAll {
        // We are ignoring Item.Fields.Base as it seems to have nothing useful
        public final String TITLE = "title";
        public final String ARTIST = "artist";
        public final String ALBUMARTIST = "albumartist";
        public final String GENRE = "genre";
        public final String YEAR = "year";
        public final String RATING = "rating";
        public final String ALBUM = "album";
        public final String TRACK = "track";
        public final String DURATION = "duration";
        public final String COMMENT = "comment";
        public final String LYRICS = "lyrics";
        public final String MUSICBRAINZTRACKID = "musicbrainztrackid";
        public final String MUSICBRAINZARTISTID = "musicbrainzartistid";
        public final String MUSICBRAINZALBUMID = "musicbrainzalbumid";
        public final String MUSICBRAINZALBUMARTISTID = "musicbrainzalbumartistid";
        public final String PLAYCOUNT = "playcount";
        public final String FANART = "fanart";
        public final String DIRECTOR = "director";
        public final String TRAILER = "trailer";
        public final String TAGLINE = "tagline";
        public final String PLOT = "plot";
        public final String PLOTOUTLINE = "plotoutline";
        public final String ORIGINALTITLE = "originaltitle";
        public final String LASTPLAYED = "lastplayed";
        public final String WRITER = "writer";
        public final String STUDIO = "studio";
        public final String MPAA = "mpaa";
        public final String CAST = "cast";
        public final String COUNTRY = "country";
        public final String IMDBNUMBER = "imdbnumber";
        public final String PREMIERED = "premiered";
        public final String PRODUCTIONCODE = "productioncode";
        public final String RUNTIME = "runtime";
        public final String SET = "set";
        public final String SHOWLINK = "showlink";
        public final String STREAMDETAILS = "streamdetails";
        public final String TOP250 = "top250";
        public final String VOTES = "votes";
        public final String FIRSTAIRED = "firstaired";
        public final String SEASON = "season";
        public final String EPISODE = "episode";
        public final String SHOWTITLE = "showtitle";
        public final String THUMBNAIL = "thumbnail";
        public final String FILE = "file";
        public final String RESUME = "resume";
        public final String ARTISTID = "artistid";
        public final String ALBUMID = "albumid";
        public final String TVSHOWID = "tvshowid";
        public final String SETID = "setid";
        public final String WATCHEDEPISODES = "watchedepisodes";
        public final String DISC = "disc";
        public final String TAG = "tag";
        public final String ART = "art";
        public final String GENREID = "genreid";
        public final String DISPLAYARTIST = "displayartist";
        public final String ALBUMARTISTID = "albumartistid";
        public final String DESCRIPTION = "description";
        public final String THEME = "theme";
        public final String MOOD = "mood";
        public final String STYLE = "style";
        public final String ALBUMLABEL = "albumlabel";
        public final String SORTTITLE = "sorttitle";
        public final String EPISODEGUIDE = "episodeguide";
        public final String UNIQUEID = "uniqueid";
        public final String DATEADDED = "dateadded";
        public final String CHANNEL = "channel";
        public final String CHANNELTYPE = "channeltype";
        public final String HIDDEN = "hidden";
        public final String LOCKED = "locked";
        public final String CHANNELNUMBER = "channelnumber";
        public final String STARTTIME = "starttime";
        public final String ENDTIME = "endtime";

        public final String[] allValues = new String[] {
                TITLE, ARTIST, ALBUMARTIST, GENRE, YEAR, RATING, ALBUM, TRACK, DURATION, COMMENT,
                LYRICS, MUSICBRAINZTRACKID, MUSICBRAINZARTISTID, MUSICBRAINZALBUMID,
                MUSICBRAINZALBUMARTISTID, PLAYCOUNT, FANART, DIRECTOR, TRAILER, TAGLINE, PLOT,
                PLOTOUTLINE, ORIGINALTITLE, LASTPLAYED, WRITER, STUDIO, MPAA, CAST, COUNTRY,
                IMDBNUMBER, PREMIERED, PRODUCTIONCODE, RUNTIME, SET, SHOWLINK, STREAMDETAILS,
                TOP250, VOTES, FIRSTAIRED, SEASON, EPISODE, SHOWTITLE, THUMBNAIL, FILE, RESUME,
                ARTISTID, ALBUMID, TVSHOWID, SETID, WATCHEDEPISODES, DISC, TAG, ART, GENREID,
                DISPLAYARTIST, ALBUMARTISTID, DESCRIPTION, THEME, MOOD, STYLE, ALBUMLABEL,
                SORTTITLE, EPISODEGUIDE, UNIQUEID, DATEADDED, CHANNEL, CHANNELTYPE, HIDDEN,
                LOCKED, CHANNELNUMBER, STARTTIME, ENDTIME
        };
    }

    /**
     * Enums for List.Fields.Files
     */
    public interface FieldsFiles {
        // We are ignoring Item.Fields.Base as it seems to have nothing useful
        public final String TITLE = "title";
        public final String ARTIST = "artist";
        public final String ALBUMARTIST = "albumartist";
        public final String GENRE = "genre";
        public final String YEAR = "year";
        public final String RATING = "rating";
        public final String ALBUM = "album";
        public final String TRACK = "track";
        public final String DURATION = "duration";
        public final String COMMENT = "comment";
        public final String LYRICS = "lyrics";
        public final String MUSICBRAINZTRACKID = "musicbrainztrackid";
        public final String MUSICBRAINZARTISTID = "musicbrainzartistid";
        public final String MUSICBRAINZALBUMID = "musicbrainzalbumid";
        public final String MUSICBRAINZALBUMARTISTID = "musicbrainzalbumartistid";
        public final String PLAYCOUNT = "playcount";
        public final String FANART = "fanart";
        public final String DIRECTOR = "director";
        public final String TRAILER = "trailer";
        public final String TAGLINE = "tagline";
        public final String PLOT = "plot";
        public final String PLOTOUTLINE = "plotoutline";
        public final String ORIGINALTITLE = "originaltitle";
        public final String LASTPLAYED = "lastplayed";
        public final String WRITER = "writer";
        public final String STUDIO = "studio";
        public final String MPAA = "mpaa";
        public final String CAST = "cast";
        public final String COUNTRY = "country";
        public final String IMDBNUMBER = "imdbnumber";
        public final String PREMIERED = "premiered";
        public final String PRODUCTIONCODE = "productioncode";
        public final String RUNTIME = "runtime";
        public final String SET = "set";
        public final String SHOWLINK = "showlink";
        public final String STREAMDETAILS = "streamdetails";
        public final String TOP250 = "top250";
        public final String VOTES = "votes";
        public final String FIRSTAIRED = "firstaired";
        public final String SEASON = "season";
        public final String EPISODE = "episode";
        public final String SHOWTITLE = "showtitle";
        public final String THUMBNAIL = "thumbnail";
        public final String FILE = "file";
        public final String RESUME = "resume";
        public final String ARTISTID = "artistid";
        public final String ALBUMID = "albumid";
        public final String TVSHOWID = "tvshowid";
        public final String SETID = "setid";
        public final String WATCHEDEPISODES = "watchedepisodes";
        public final String DISC = "disc";
        public final String TAG = "tag";
        public final String ART = "art";
        public final String GENREID = "genreid";
        public final String DISPLAYARTIST = "displayartist";
        public final String ALBUMARTISTID = "albumartistid";
        public final String DESCRIPTION = "description";
        public final String THEME = "theme";
        public final String MOOD = "mood";
        public final String STYLE = "style";
        public final String ALBUMLABEL = "albumlabel";
        public final String SORTTITLE = "sorttitle";
        public final String EPISODEGUIDE = "episodeguide";
        public final String UNIQUEID = "uniqueid";
        public final String DATEADDED = "dateadded";
        public final String SIZE = "size";
        public final String LASTMODIFIED = "lastmodified";
        public final String MIMETYPE = "mimetype";

        public final String[] allValues = new String[] {
                TITLE, ARTIST, ALBUMARTIST, GENRE, YEAR, RATING, ALBUM, TRACK, DURATION, COMMENT,
                LYRICS, MUSICBRAINZTRACKID, MUSICBRAINZARTISTID, MUSICBRAINZALBUMID,
                MUSICBRAINZALBUMARTISTID, PLAYCOUNT, FANART, DIRECTOR, TRAILER, TAGLINE, PLOT,
                PLOTOUTLINE, ORIGINALTITLE, LASTPLAYED, WRITER, STUDIO, MPAA, CAST, COUNTRY,
                IMDBNUMBER, PREMIERED, PRODUCTIONCODE, RUNTIME, SET, SHOWLINK, STREAMDETAILS,
                TOP250, VOTES, FIRSTAIRED, SEASON, EPISODE, SHOWTITLE, THUMBNAIL, FILE, RESUME,
                ARTISTID, ALBUMID, TVSHOWID, SETID, WATCHEDEPISODES, DISC, TAG, ART, GENREID,
                DISPLAYARTIST, ALBUMARTISTID, DESCRIPTION, THEME, MOOD, STYLE, ALBUMLABEL,
                SORTTITLE, EPISODEGUIDE, UNIQUEID, DATEADDED, SIZE, LASTMODIFIED, MIMETYPE
        };
    }

    /**
     * List.Item.File
     */
    public static class ItemFile extends ItemBase {
        public static final String FILE = "file";
        public static final String FILETYPE = "filetype";
        public static final String LASTMODIFIED = "lastmodified";
        public static final String MIMETYPE = "mimetype";
        public static final String SIZE = "size";

        public static final String FILETYPE_FILE = "file";
        public static final String FILETYPE_DIRECTORY = "directory";

        public final String file;
        public final String filetype;
        public final String lastmodified;
        public final String mimetype;
        public final int size;

        public ItemFile(JsonNode node) {
            super(node);
            file = JsonUtils.stringFromJsonNode(node, FILE, null);
            filetype = JsonUtils.stringFromJsonNode(node, FILETYPE, null);
            lastmodified = JsonUtils.stringFromJsonNode(node, LASTMODIFIED, null);
            mimetype = JsonUtils.stringFromJsonNode(node, MIMETYPE, null);
            size = JsonUtils.intFromJsonNode(node, SIZE, 0);
        }
    }

    /**
     * List.Sort
     */
    public static class Sort implements ApiParameter {
        public static final String SORT_METHOD_NONE = "none";
        public static final String SORT_METHOD_LABEL = "label";
        public static final String SORT_METHOD_DATE = "date";
        public static final String SORT_METHOD_SIZE = "size";
        public static final String SORT_METHOD_FILE = "file";
        public static final String SORT_METHOD_PATH = "path";
        public static final String SORT_METHOD_DRIVETYPE = "drivetype";
        public static final String SORT_METHOD_TYPE = "title";
        public static final String SORT_METHOD_TRACK = "track";
        public static final String SORT_METHOD_TIME = "time";
        public static final String SORT_METHOD_ARTIST = "artist";
        public static final String SORT_METHOD_ALBUM = "album";
        public static final String SORT_METHOD_ALBUMTYPE = "albumtype";
        public static final String SORT_METHOD_GENRE = "genre";
        public static final String SORT_METHOD_COUNTRY = "country";
        public static final String SORT_METHOD_YEAR = "year";
        public static final String SORT_METHOD_RATING = "rating";
        public static final String SORT_METHOD_VOTES = "votes";
        public static final String SORT_METHOD_TOP250 = "top250";
        public static final String SORT_METHOD_PROGRAMCOUNT = "programcount";
        public static final String SORT_METHOD_PLAYLIST = "playlist";
        public static final String SORT_METHOD_EPISODE = "episode";
        public static final String SORT_METHOD_SEASON = "season";
        public static final String SORT_METHOD_TOTALEPISODES = "totalepisodes";
        public static final String SORT_METHOD_WATCHEDEPISODES = "watchedepisodes";
        public static final String SORT_METHOD_TVSHOWSTATUS = "tvshowstatus";
        public static final String SORT_METHOD_TVSHOWTITLE = "tvshowtitle";
        public static final String SORT_METHOD_SORTTITLE = "sorttitle";
        public static final String SORT_METHOD_PRODUCTIONCODE = "productioncode";
        public static final String SORT_METHOD_MPAA = "mpaa";
        public static final String SORT_METHOD_STUDIO = "studio";
        public static final String SORT_METHOD_DATEADDED = "dateadded";
        public static final String SORT_METHOD_LASTPLAYED = "lastplayed";
        public static final String SORT_METHOD_PLAYCOUNT = "playcount";
        public static final String SORT_METHOD_LISTENERS = "listeners";
        public static final String SORT_METHOD_BITRATE = "bitrate";
        public static final String SORT_METHOD_RANDOM = "random";

        static final String METHOD = "method";
        static final String IGNORE_ARTICLE = "ignorearticle";
        static final String ORDER = "order";
        static final String ASCENDING_ORDER = "ascending";
        static final String DESCENDING_ORDER = "descending";

        protected static final ObjectMapper objectMapper = new ObjectMapper();
        public final boolean ignore_article;
        public final boolean ascending_order;
        public final String sort_method;

        public Sort(String method, boolean ascending, boolean ignore_article) {
            this.sort_method = method;
            this.ascending_order = ascending;
            this.ignore_article = ignore_article;
        }

        public JsonNode toJsonNode() {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put(ORDER, ascending_order ? ASCENDING_ORDER : DESCENDING_ORDER);
            node.put(IGNORE_ARTICLE, ignore_article);
            node.put(METHOD, sort_method);
            return node;
        }
    }
}