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

import java.util.List;

/**
 * Types from Audio.*
 */
public class AudioType {

    /**
     * Enums for Video.Fields.Artists
     */
    public interface FieldsArtists {
        String INSTRUMENT = "instrument";
        String STYLE = "style";
        String MOOD = "mood";
        String BORN = "born";
        String FORMED = "formed";
        String DESCRIPTION = "description";
        String GENRE = "genre";
        String DIED = "died";
        String DISBANDED = "disbanded";
        String YEARSACTIVE = "yearsactive";
        String MUSICBRAINZARTISTID = "musicbrainzartistid";
        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String COMPILATIONARTIST = "compilationartist";

        String[] allValues = new String[]{
                INSTRUMENT, STYLE, MOOD, BORN, FORMED, DESCRIPTION, GENRE, DIED, DISBANDED,
                YEARSACTIVE, MUSICBRAINZARTISTID, FANART, THUMBNAIL, COMPILATIONARTIST
        };
    }

    /**
     * Audio.Details.Base
     */
    public static class DetailsBase extends MediaType.DetailsBase {
        public static final String GENRE = "genre";

        public final List<String> genre;

        /**
         * Constructor
         * @param node Json node
         */
        public DetailsBase(JsonNode node) {
            super(node);
            genre = JsonUtils.stringListFromJsonNode(node, GENRE);
        }
    }

    /**
     * Audio.Details.Media
     */
    public static class DetailsMedia extends DetailsBase {
        public static final String ARTIST = "artist";
        public static final String ARTISTID = "artistid";
        public static final String DISPLAYARTIST = "displayartist";
        public static final String GENREID = "genreid";
        public static final String MUSICBRAINZALBUMARTISTID = "musicbrainzalbumartistid";
        public static final String MUSICBRAINZALBUMID = "musicbrainzalbumid";
        public static final String RATING = "rating";
        public static final String TITLE = "title";
        public static final String YEAR = "year";

        // class members
        public final List<String> artist;
        public final List<Integer> artistid;
        public final String displayartist;
        public final List<Integer> genreid;
        public final String musicbrainzalbumartistid;
        public final String musicbrainzalbumid;
        public final int rating;
        public final String title;
        public final int year;

        public DetailsMedia(JsonNode node) {
            super(node);
            artist = JsonUtils.stringListFromJsonNode(node, ARTIST);
            artistid = JsonUtils.integerListFromJsonNode(node, ARTISTID);
            displayartist = JsonUtils.stringFromJsonNode(node, DISPLAYARTIST);
            genreid = JsonUtils.integerListFromJsonNode(node, GENREID);
            musicbrainzalbumartistid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZALBUMARTISTID);
            musicbrainzalbumid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZALBUMID);
            rating = JsonUtils.intFromJsonNode(node, RATING);
            title = JsonUtils.stringFromJsonNode(node, TITLE);
            year = JsonUtils.intFromJsonNode(node, YEAR);
        }
    }

    /**
     * Audio.Details.Artist
     */
    public static class DetailsArtist extends DetailsBase {
        public static final String ARTIST = "artist";
        public static final String ARTISTID = "artistid";
        public static final String BORN = "born";
        public static final String COMPILATIONARTIST = "compilationartist";
        public static final String DESCRIPTION = "description";
        public static final String DIED = "died";
        public static final String DISBANDED = "disbanded";
        public static final String FORMED = "formed";
        public static final String INSTRUMENT = "instrument";
        public static final String MOOD = "mood";
        public static final String MUSICBRAINZARTISTID = "musicbrainzartistid";
        public static final String STYLE = "style";
        public static final String YEARSACTIVE = "yearsactive";

        public final String artist;
        public final int artistid;
        public final String born;
        public final boolean compilationartist;
        public final String description;
        public final String died;
        public final String disbanded;
        public final String formed;
        public final List<String> instrument;
        public final List<String> mood;
        public final String musicbrainzartistid;
        public final List<String> style;
        public final List<String> yearsactive;


        /**
         * Constructor
         * @param node Json node
         */
        public DetailsArtist(JsonNode node) {
            super(node);
            artist = JsonUtils.stringFromJsonNode(node, ARTIST);
            artistid = JsonUtils.intFromJsonNode(node, ARTISTID);
            born = JsonUtils.stringFromJsonNode(node, BORN);
            compilationartist = JsonUtils.booleanFromJsonNode(node, COMPILATIONARTIST, false);
            description = JsonUtils.stringFromJsonNode(node, DESCRIPTION);
            died = JsonUtils.stringFromJsonNode(node, DIED);
            disbanded = JsonUtils.stringFromJsonNode(node, DISBANDED);
            formed = JsonUtils.stringFromJsonNode(node, FORMED);
            instrument = JsonUtils.stringListFromJsonNode(node, INSTRUMENT);
            mood = JsonUtils.stringListFromJsonNode(node, MOOD);
            musicbrainzartistid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZARTISTID);
            style = JsonUtils.stringListFromJsonNode(node, STYLE);
            yearsactive = JsonUtils.stringListFromJsonNode(node, YEARSACTIVE);
        }
    }

    /**
     * Enums for Audio.Fields.Album
     */
    public interface FieldsAlbum {
        String TITLE = "title";
        String DESCRIPTION = "description";
        String ARTIST = "artist";
        String GENRE = "genre";
        String THEME = "theme";
        String MOOD = "mood";
        String STYLE = "style";
        String TYPE = "type";
        String ALBUMLABEL = "albumlabel";
        String RATING = "rating";
        String YEAR = "year";
        String MUSICBRAINZALBUMID = "musicbrainzalbumid";
        String MUSICBRAINZALBUMARTISTID = "musicbrainzalbumartistid";
        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String PLAYCOUNT = "playcount";
        String GENREID = "genreid";
        String ARTISTID = "artistid";
        String DISPLAYARTIST = "displayartist";

        String[] allValues = new String[]{
                TITLE, DESCRIPTION, ARTIST, GENRE, THEME, MOOD, STYLE, TYPE, ALBUMLABEL, RATING,
                YEAR, MUSICBRAINZALBUMID, MUSICBRAINZALBUMARTISTID, FANART, THUMBNAIL,
                PLAYCOUNT, GENREID, ARTISTID, DISPLAYARTIST
        };
    }

    /**
     * Audio.Details.Album
     */
    public static class DetailsAlbum extends DetailsMedia {
        public static final String ALBUMID = "albumid";
        public static final String ALBUMLABEL = "albumlabel";
        public static final String DESCRIPTION = "description";
        public static final String MOOD = "mood";
        public static final String PLAYCOUNT = "playcount";
        public static final String STYLE = "style";
        public static final String THEME = "theme";
        public static final String TYPE = "type";

        public final int albumid;
        public final String albumlabel;
        public final String description;
        public final List<String> mood;
        public final int playcount;
        public final List<String> style;
        public final List<String> theme;
        public final String type;

        /**
         * Constructor
         * @param node Json node
         */
        public DetailsAlbum(JsonNode node) {
            super(node);
            albumid = JsonUtils.intFromJsonNode(node, ALBUMID);
            albumlabel = JsonUtils.stringFromJsonNode(node, ALBUMLABEL);
            description = JsonUtils.stringFromJsonNode(node, DESCRIPTION);
            mood = JsonUtils.stringListFromJsonNode(node, MOOD);
            playcount = JsonUtils.intFromJsonNode(node, PLAYCOUNT);
            style = JsonUtils.stringListFromJsonNode(node, STYLE);
            theme = JsonUtils.stringListFromJsonNode(node, THEME);
            type = JsonUtils.stringFromJsonNode(node, TYPE);
        }
    }

    /**
     * Enums for Audio.Fields.Song
     */
    public interface FieldsSong {
        String TITLE = "title";
        String ARTIST = "artist";
        String ALBUMARTIST = "albumartist";
        String GENRE = "genre";
        String YEAR = "year";
        String RATING = "rating";
        String ALBUM = "album";
        String TRACK = "track";
        String DURATION = "duration";
        String COMMENT = "comment";
        String LYRICS = "lyrics";
        String MUSICBRAINZTRACKID = "musicbrainztrackid";
        String MUSICBRAINZARTISTID = "musicbrainzartistid";
        String MUSICBRAINZALBUMID = "musicbrainzalbumid";
        String MUSICBRAINZALBUMARTISTID = "musicbrainzalbumartistid";
        String PLAYCOUNT = "playcount";
        String FANART = "fanart";
        String THUMBNAIL = "thumbnail";
        String FILE = "file";
        String ALBUMID = "albumid";
        String LASTPLAYED = "lastplayed";
        String DISC = "disc";
        String GENREID = "genreid";
        String ARTISTID = "artistid";
        String DISPLAYARTIST = "displayartist";
        String ALBUMARTISTID = "albumartistid";

        String[] allValues = new String[]{
                TITLE, ARTIST, ALBUMARTIST, GENRE, YEAR, RATING, ALBUM, TRACK, DURATION,
                COMMENT, LYRICS, MUSICBRAINZTRACKID, MUSICBRAINZARTISTID, MUSICBRAINZALBUMID,
                MUSICBRAINZALBUMARTISTID, PLAYCOUNT, FANART, THUMBNAIL, FILE, ALBUMID,
                LASTPLAYED, DISC, GENREID, ARTISTID, DISPLAYARTIST, ALBUMARTISTID
        };
    }

    /**
     * Audio.Details.Song
     */
    public static class DetailsSong extends DetailsMedia {
        public static final String ALBUM = "album";
        public static final String ALBUMARTIST = "albumartist";
        public static final String ALBUMARTISTID = "albumartistid";
        public static final String ALBUMID = "albumid";
        public static final String COMMENT = "comment";
        public static final String DISC = "disc";
        public static final String DURATION = "duration";
        public static final String FILE = "file";
        public static final String LASTPLAYED = "lastplayed";
        public static final String LYRICS = "lyrics";
        public static final String MUSICBRAINZARTISTID = "musicbrainzartistid";
        public static final String MUSICBRAINZTRACKID = "musicbrainztrackid";
        public static final String PLAYCOUNT = "playcount";
        public static final String SONGID = "songid";
        public static final String TRACK = "track";

        public final String album;
        public final List<String> albumartist;
        public final List<Integer> albumartistid;
        public final int albumid;
        public final String comment;
        public final int disc;
        public final int duration;
        public final String file;
        public final String lastplayed;
        public final String lyrics;
        public final String musicbrainzartistid;
        public final String musicbrainztrackid;
        public final int playcount;
        public final int songid;
        public final int track;

        /**
         * Constructor
         * @param node Json node
         */
        public DetailsSong(JsonNode node) {
            super(node);
            album = JsonUtils.stringFromJsonNode(node, ALBUM);
            albumid = JsonUtils.intFromJsonNode(node, ALBUMID);
            albumartist = JsonUtils.stringListFromJsonNode(node, ALBUMARTIST);
            albumartistid = JsonUtils.integerListFromJsonNode(node, ALBUMARTISTID);
            comment = JsonUtils.stringFromJsonNode(node, COMMENT);
            disc = JsonUtils.intFromJsonNode(node, DISC);
            duration = JsonUtils.intFromJsonNode(node, DURATION);
            file = JsonUtils.stringFromJsonNode(node, FILE);
            lastplayed = JsonUtils.stringFromJsonNode(node, LASTPLAYED);
            lyrics= JsonUtils.stringFromJsonNode(node, LYRICS);
            musicbrainzartistid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZARTISTID);
            musicbrainztrackid = JsonUtils.stringFromJsonNode(node, MUSICBRAINZTRACKID);
            playcount = JsonUtils.intFromJsonNode(node, PLAYCOUNT);
            songid = JsonUtils.intFromJsonNode(node, SONGID);
            track = JsonUtils.intFromJsonNode(node, TRACK);
        }
    }
}
