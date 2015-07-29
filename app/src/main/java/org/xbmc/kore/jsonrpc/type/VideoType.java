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

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.xbmc.kore.utils.JsonUtils;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Types from Video.*
 */
public class VideoType {
    private static final String TAG = LogUtils.makeLogTag(VideoType.class);

    public static class Cast implements Parcelable {
        public static final String NAME = "name";
        public static final String ORDER = "order";
        public static final String ROLE = "role";
        public static final String THUMBNAIL = "thumbnail";

        public final String name;
        public final int order;
        public final String role;
        public final String thumbnail;

        public Cast(JsonNode node) {
            name = JsonUtils.stringFromJsonNode(node, NAME);
            order = JsonUtils.intFromJsonNode(node, ORDER, 0);
            role = JsonUtils.stringFromJsonNode(node, ROLE);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
        }

        public Cast(String name, int order, String role, String thumbnail) {
            this.name = name;
            this.order = order;
            this.role = role;
            this.thumbnail = thumbnail;
        }

        public static List<Cast> castListFromJsonNode(JsonNode node, String key) {
            if ((node == null) || (!node.has(key))) {
                return new ArrayList<Cast>(0);
            }

            JsonNode castNode = node.get(key);
            if (!castNode.isArray()) {
                LogUtils.LOGD(TAG, "Cast node isn't an array, it's a: " + castNode.getNodeType());
                return new ArrayList<Cast>(0);
            }

            ArrayNode arrayNode = (ArrayNode) castNode;
            ArrayList<Cast> castList = new ArrayList<Cast>(arrayNode.size());
            for (JsonNode innerNode : arrayNode) {
                castList.add(new Cast(innerNode));
            }
            return castList;
        }

        // Parcelable interface implementation
        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(name);
            out.writeInt(order);
            out.writeString(role);
            out.writeString(thumbnail);
        }

        public static final Parcelable.Creator<Cast> CREATOR
                = new Parcelable.Creator<Cast>() {
            public Cast createFromParcel(Parcel in) {
                return new Cast(in.readString(), in.readInt(), in.readString(), in.readString());
            }

            public Cast[] newArray(int size) {
                return new Cast[size];
            }
        };
    }

    public static class Resume {
        public static final String POSITION = "position";
        public static final String TOTAL = "total";

        public final double position;
        public final double total;

        public Resume(JsonNode node) {
            position = JsonUtils.doubleFromJsonNode(node, POSITION, 0);
            total = JsonUtils.doubleFromJsonNode(node, TOTAL, 0);
        }
    }

    public static class Streams {

        public static class Audio {
            public static final String CHANNELS = "channels";
            public static final String CODEC = "codec";
            public static final String LANGUAGE = "language";

            public final int channels;
            public final String codec;
            public final String language;

            public Audio(JsonNode node) {
                channels = JsonUtils.intFromJsonNode(node, CHANNELS, 0);
                codec = JsonUtils.stringFromJsonNode(node, CODEC);
                language = JsonUtils.stringFromJsonNode(node, LANGUAGE);
            }
        }

        public static class Subtitle {
            public static final String LANGUAGE = "language";

            public final String language;

            public Subtitle(JsonNode node) {
                language = JsonUtils.stringFromJsonNode(node, LANGUAGE, null);
            }
        }

        public static class Video {
            public static final String ASPECT = "aspect";
            public static final String CODEC = "codec";
            public static final String DURATION = "duration";
            public static final String HEIGHT = "height";
            public static final String WIDTH = "width";

            public final double aspect;
            public final String codec;
            public final int duration;
            public final int height;
            public final int width;

            public Video(JsonNode node) {
                aspect = JsonUtils.doubleFromJsonNode(node, ASPECT, 0);
                codec = JsonUtils.stringFromJsonNode(node, CODEC, null);
                duration = JsonUtils.intFromJsonNode(node, DURATION, -1);
                height = JsonUtils.intFromJsonNode(node, HEIGHT, -1);
                width = JsonUtils.intFromJsonNode(node, WIDTH, -1);
            }
        }

        public static final String AUDIO = "audio";
        public static final String SUBTITLE = "subtitle";
        public static final String VIDEO = "video";

        // class members
        public final List<Audio> audio;
        public final List<Subtitle> subtitle;
        public final List<Video> video;

        public Streams(JsonNode node) {
            audio = new ArrayList<Audio>();
            if (node.has(AUDIO)) {
                ArrayNode arrayNode = (ArrayNode)node.get(AUDIO);
                for (JsonNode innerNode : arrayNode) {
                    audio.add(new Audio(innerNode));
                }
            }

            subtitle = new ArrayList<Subtitle>();
            if (node.has(SUBTITLE)) {
                ArrayNode arrayNode = (ArrayNode)node.get(SUBTITLE);
                for (JsonNode innerNode : arrayNode) {
                    subtitle.add(new Subtitle(innerNode));
                }
            }

            video = new ArrayList<Video>();
            if (node.has(VIDEO)) {
                ArrayNode arrayNode = (ArrayNode)node.get(VIDEO);
                for (JsonNode innerNode : arrayNode) {
                    video.add(new Video(innerNode));
                }
            }
        }
    }

    /**
     * Enums for Video.Fields.Movie
     */
    public interface FieldsMovie {
        public final String TITLE = "title";
        public final String GENRE = "genre";
        public final String YEAR = "year";
        public final String RATING = "rating";
        public final String DIRECTOR = "director";
        public final String TRAILER = "trailer";
        public final String TAGLINE = "tagline";
        public final String PLOT = "plot";
        public final String PLOTOUTLINE = "plotoutline";
        public final String ORIGINALTITLE = "originaltitle";
        public final String LASTPLAYED = "lastplayed";
        public final String PLAYCOUNT = "playcount";
        public final String WRITER = "writer";
        public final String STUDIO = "studio";
        public final String MPAA = "mpaa";
        public final String CAST = "cast";
        public final String COUNTRY = "country";
        public final String IMDBNUMBER = "imdbnumber";
        public final String RUNTIME = "runtime";
        public final String SET = "set";
        public final String SHOWLINK = "showlink";
        public final String STREAMDETAILS = "streamdetails";
        public final String TOP250 = "top250";
        public final String VOTES = "votes";
        public final String FANART = "fanart";
        public final String THUMBNAIL = "thumbnail";
        public final String FILE = "file";
        public final String SORTTITLE = "sorttitle";
        public final String RESUME = "resume";
        public final String SETID = "setid";
        public final String DATEADDED = "dateadded";
        public final String TAG = "tag";
        public final String ART = "art";

        public final static String[] allValues = new String[]{
                TITLE, GENRE, YEAR, RATING, DIRECTOR, TRAILER, TAGLINE, PLOT, PLOTOUTLINE,
                ORIGINALTITLE, LASTPLAYED, PLAYCOUNT, WRITER, STUDIO, MPAA, CAST, COUNTRY,
                IMDBNUMBER, RUNTIME, SET, SHOWLINK, STREAMDETAILS, TOP250, VOTES, FANART,
                THUMBNAIL, FILE, SORTTITLE, RESUME, SETID, DATEADDED, TAG, ART
        };
    }

    /**
     * Video.Details.Base
     */
    public static class DetailsBase extends MediaType.DetailsBase {
        public static final String ART = "art";
        public static final String PLAYCOUNT = "playcount";

        public final MediaType.Artwork art;
        public final Integer playcount;

        /**
         * Constructor
         * @param node Json node
         */
        public DetailsBase(JsonNode node) {
            super(node);
            art = node.has(ART) ? new MediaType.Artwork(node.get(ART)) : null;
            playcount = JsonUtils.intFromJsonNode(node, PLAYCOUNT, 0);
        }
    }

    /**
     * Video.Details.Media
     */
    public static class DetailsMedia extends DetailsBase {
        public static final String TITLE = "title";

        public final String title;

        public DetailsMedia(JsonNode node) {
            super(node);
            title = JsonUtils.stringFromJsonNode(node, TITLE, null);
        }
    }

    /**
     * Video.Details.Item
     */
    public static class DetailsItem extends DetailsMedia {
        public static final String DATEADDED = "dateadded";
        public static final String FILE = "file";
        public static final String LASTPLAYED = "lastplayed";
        public static final String PLOT = "plot";

        public final String dateadded;
        public final String file;
        public final String lastplayed;
        public final String plot;

        public DetailsItem(JsonNode node) {
            super(node);
            dateadded = JsonUtils.stringFromJsonNode(node, DATEADDED, null);
            file = JsonUtils.stringFromJsonNode(node, FILE, null);
            lastplayed = JsonUtils.stringFromJsonNode(node, LASTPLAYED, null);
            plot = JsonUtils.stringFromJsonNode(node, PLOT, null);
        }
    }

    /**
     * Video.Details.File
     */
    public static class DetailsFile extends DetailsItem {
        // field names
        public static final String DIRECTOR = "director";
        public static final String RESUME = "resume";
        public static final String RUNTIME = "runtime";
        public static final String STREAMDETAILS = "streamdetails";

        // class members
        public final List<String> director;
        public final Resume resume;
        public final int runtime;
        public final Streams streamdetails;

        public DetailsFile(JsonNode node) {
            super(node);
            director = JsonUtils.stringListFromJsonNode(node, DIRECTOR);
            resume = node.has(RESUME) ? new Resume(node.get(RESUME)) : null;
            runtime = JsonUtils.intFromJsonNode(node, RUNTIME, 0);
            streamdetails = node.has(STREAMDETAILS) ? new Streams(node.get(STREAMDETAILS)) : null;
        }
    }

    /**
     * Video.Details.Movie
     */
    public static class DetailsMovie extends DetailsFile {
        public static final String CAST = "cast";
        public static final String COUNTRY = "country";
        public static final String GENRE = "genre";
        public static final String IMDBNUMBER = "imdbnumber";
        public static final String MOVIEID = "movieid";
        public static final String MPAA = "mpaa";
        public static final String ORIGINALTITLE = "originaltitle";
        public static final String PLOTOUTLINE = "plotoutline";
        public static final String RATING = "rating";
        public static final String SET = "set";
        public static final String SETID = "setid";
        public static final String SHOWLINK = "showlink";
        public static final String SORTTITLE = "sorttitle";
        public static final String STUDIO = "studio";
        public static final String TAG = "tag";
        public static final String TAGLINE = "tagline";
        public static final String TOP250 = "top250";
        public static final String TRAILER = "trailer";
        public static final String VOTES = "votes";
        public static final String WRITER = "writer";
        public static final String YEAR = "year";

        public final List<Cast> cast;
        public final List<String> country;
        public final List<String> genre;
        public final String imdbnumber;
        public final int movieid;
        public final String mpaa;
        public final String originaltitle;
        public final String plotoutline;
        public final double rating;
        public final String set;
        public final int setid;
        public final List<String> showlink;
        public final String sorttitle;
        public final List<String> studio;
        public final List<String> tag;
        public final String tagline;
        public final int top250;
        public final String trailer;
        public final String votes;
        public final List<String> writer;
        public final int year;

        public DetailsMovie(JsonNode node) {
            super(node);
            cast = Cast.castListFromJsonNode(node, CAST);
            country = JsonUtils.stringListFromJsonNode(node, COUNTRY);
            genre = JsonUtils.stringListFromJsonNode(node, GENRE);
            imdbnumber = JsonUtils.stringFromJsonNode(node, IMDBNUMBER);
            movieid = JsonUtils.intFromJsonNode(node, MOVIEID);
            mpaa = JsonUtils.stringFromJsonNode(node, MPAA);
            originaltitle = JsonUtils.stringFromJsonNode(node, ORIGINALTITLE);
            plotoutline = JsonUtils.stringFromJsonNode(node, PLOTOUTLINE);
            rating = JsonUtils.doubleFromJsonNode(node, RATING, 0);
            set = JsonUtils.stringFromJsonNode(node, SET);
            setid = JsonUtils.intFromJsonNode(node, SETID, -1);
            showlink = JsonUtils.stringListFromJsonNode(node, SHOWLINK);
            sorttitle = JsonUtils.stringFromJsonNode(node, SORTTITLE);
            studio = JsonUtils.stringListFromJsonNode(node, STUDIO);
            tag = JsonUtils.stringListFromJsonNode(node, TAG);
            tagline = JsonUtils.stringFromJsonNode(node, TAGLINE);
            top250 = JsonUtils.intFromJsonNode(node, TOP250, 0);
            trailer = JsonUtils.stringFromJsonNode(node, TRAILER);
            votes = JsonUtils.stringFromJsonNode(node, VOTES);
            writer = JsonUtils.stringListFromJsonNode(node, WRITER);
            year = JsonUtils.intFromJsonNode(node, YEAR, 0);
        }
    }

    /**
     * Enums for Video.Fields.TVShow
     */
    public interface FieldsTVShow {
        public final String TITLE = "title";
        public final String GENRE = "genre";
        public final String YEAR = "year";
        public final String RATING = "rating";
        public final String PLOT = "plot";
        public final String STUDIO = "studio";
        public final String MPAA = "mpaa";
        public final String CAST = "cast";
        public final String PLAYCOUNT = "playcount";
        public final String EPISODE = "episode";
        public final String IMDBNUMBER = "imdbnumber";
        public final String PREMIERED = "premiered";
        public final String VOTES = "votes";
        public final String LASTPLAYED = "lastplayed";
        public final String FANART = "fanart";
        public final String THUMBNAIL = "thumbnail";
        public final String FILE = "file";
        public final String ORIGINALTITLE = "originaltitle";
        public final String SORTTITLE = "sorttitle";
        public final String EPISODEGUIDE = "episodeguide";
        public final String SEASON = "season";
        public final String WATCHEDEPISODES = "watchedepisodes";
        public final String DATEADDED = "dateadded";
        public final String TAG = "tag";
        public final String ART = "art";

        public final static String[] allValues = new String[] {
                TITLE, GENRE, YEAR, RATING, PLOT, STUDIO, MPAA, CAST, PLAYCOUNT, EPISODE,
                IMDBNUMBER, PREMIERED, VOTES, LASTPLAYED, FANART, THUMBNAIL, FILE, ORIGINALTITLE,
                SORTTITLE, EPISODEGUIDE, SEASON, WATCHEDEPISODES, DATEADDED, TAG, ART
        };
    }

    /**
     * Video.Details.TVShow
     */
    public static class DetailsTVShow extends DetailsItem {
        public static final String CAST = "cast";
        public static final String EPISODE = "episode";
        public static final String EPISODEGUIDE = "episodeguide";
        public static final String GENRE = "genre";
        public static final String IMDBNUMBER = "imdbnumber";
        public static final String MPAA = "mpaa";
        public static final String ORIGINALTITLE = "originaltitle";
        public static final String PREMIERED = "premiered";
        public static final String RATING = "rating";
        public static final String SEASON = "season";
        public static final String SORTTITLE = "sorttitle";
        public static final String STUDIO = "studio";
        public static final String TAG = "tag";
        public static final String TVSHOWID = "tvshowid";
        public static final String VOTES = "votes";
        public static final String WATCHEDEPISODES = "watchedepisodes";
        public static final String YEAR = "year";

        public final List<Cast> cast;
        public final int episode;
        public final String episodeguide;
        public final List<String> genre;
        public final String imdbnumber;
        public final String mpaa;
        public final String originaltitle;
        public final String premiered;
        public final double rating;
        public final int season;
        public final String sorttitle;
        public final List<String> studio;
        public final List<String> tag;
        public final int tvshowid;
        public final String votes;
        public final int watchedepisodes;
        public final int year;

        public DetailsTVShow(JsonNode node) {
            super(node);
            cast = Cast.castListFromJsonNode(node, CAST);
            episode = JsonUtils.intFromJsonNode(node, EPISODE, 0);
            episodeguide = JsonUtils.stringFromJsonNode(node, EPISODEGUIDE);
            genre = JsonUtils.stringListFromJsonNode(node, GENRE);
            imdbnumber = JsonUtils.stringFromJsonNode(node, IMDBNUMBER);
            mpaa = JsonUtils.stringFromJsonNode(node, MPAA);
            originaltitle = JsonUtils.stringFromJsonNode(node, ORIGINALTITLE);
            premiered = JsonUtils.stringFromJsonNode(node, PREMIERED);
            rating = JsonUtils.doubleFromJsonNode(node, RATING, 0);
            season = JsonUtils.intFromJsonNode(node, SEASON, 0);
            sorttitle = JsonUtils.stringFromJsonNode(node, SORTTITLE);
            studio = JsonUtils.stringListFromJsonNode(node, STUDIO);
            tag = JsonUtils.stringListFromJsonNode(node, TAG);
            tvshowid = JsonUtils.intFromJsonNode(node, TVSHOWID, 0);
            votes = JsonUtils.stringFromJsonNode(node, VOTES);
            watchedepisodes = JsonUtils.intFromJsonNode(node, WATCHEDEPISODES, 0);
            year = JsonUtils.intFromJsonNode(node, YEAR, 0);
        }
    }

    /**
     * Enums for Video.Fields.Season
     */
    public interface FieldsSeason {
        public final String SEASON = "season";
        public final String SHOWTITLE = "showtitle";
        public final String PLAYCOUNT = "playcount";
        public final String EPISODE = "episode";
        public final String FANART = "fanart";
        public final String THUMBNAIL = "thumbnail";
        public final String TVSHOWID = "tvshowid";
        public final String WATCHEDEPISODES = "watchedepisodes";
        public final String ART = "art";

        public final static String[] allValues = new String[] {
                SEASON, SHOWTITLE, PLAYCOUNT, EPISODE, FANART, THUMBNAIL, TVSHOWID,
                WATCHEDEPISODES, ART
        };
    }

    /**
     * Video.Details.Season
     */
    public static class DetailsSeason extends DetailsBase {
        public static final String EPISODE = "episode";
        public static final String SEASON = "season";
        public static final String SHOWTITLE = "showtitle";
        public static final String TVSHOWID = "tvshowid";
        public static final String WATCHEDEPISODES = "watchedepisodes";

        // class members
        public final int episode;
        public final int season;
        public final String showtitle;
        public final int tvshowid;
        public final int watchedepisodes;

        public DetailsSeason(JsonNode node) {
            super(node);
            episode = JsonUtils.intFromJsonNode(node, EPISODE, 0);
            season = JsonUtils.intFromJsonNode(node, SEASON, 0);
            showtitle = JsonUtils.stringFromJsonNode(node, SHOWTITLE);
            tvshowid = JsonUtils.intFromJsonNode(node, TVSHOWID, -1);
            watchedepisodes = JsonUtils.intFromJsonNode(node, WATCHEDEPISODES, 0);
        }
    }

    /**
     * Enums for Video.Fields.Episoode
     */
    public interface FieldsEpisode {
        public final String TITLE = "title";
        public final String PLOT = "plot";
        public final String VOTES = "votes";
        public final String RATING = "rating";
        public final String WRITER = "writer";
        public final String FIRSTAIRED = "firstaired";
        public final String PLAYCOUNT = "playcount";
        public final String RUNTIME = "runtime";
        public final String DIRECTOR = "director";
        public final String PRODUCTIONCODE = "productioncode";
        public final String SEASON = "season";
        public final String EPISODE = "episode";
        public final String ORIGINALTITLE = "originaltitle";
        public final String SHOWTITLE = "showtitle";
        public final String CAST = "cast";
        public final String STREAMDETAILS = "streamdetails";
        public final String LASTPLAYED = "lastplayed";
        public final String FANART = "fanart";
        public final String THUMBNAIL = "thumbnail";
        public final String FILE = "file";
        public final String RESUME = "resume";
        public final String TVSHOWID = "tvshowid";
        public final String DATEADDED = "dateadded";
        public final String UNIQUEID = "uniqueid";
        public final String ART = "art";

        public final static String[] allValues = new String[] {
                TITLE, PLOT, VOTES, RATING, WRITER, FIRSTAIRED, PLAYCOUNT, RUNTIME, DIRECTOR,
                PRODUCTIONCODE, SEASON, EPISODE, ORIGINALTITLE,  SHOWTITLE, CAST, STREAMDETAILS,
                LASTPLAYED, FANART,  THUMBNAIL, FILE, RESUME, TVSHOWID, DATEADDED, UNIQUEID, ART
        };
    }


    /**
     * Video.Details.Episode
     */
    public static class DetailsEpisode extends DetailsFile {
        public static final String CAST = "cast";
        public static final String EPISODE = "episode";
        public static final String EPISODEID = "episodeid";
        public static final String FIRSTAIRED = "firstaired";
        public static final String ORIGINALTITLE = "originaltitle";
        public static final String PRODUCTIONCODE = "productioncode";
        public static final String RATING = "rating";
        public static final String SEASON = "season";
        public static final String SHOWTITLE = "showtitle";
        public static final String TVSHOWID = "tvshowid";
        // public static final String UNIQUEID = "uniqueid";
        public static final String VOTES = "votes";
        public static final String WRITER = "writer";

        public final List<Cast> cast;
        public final int episode;
        public final int episodeid;
        public final String firstaired;
        public final String originaltitle;
        public final String productioncode;
        public final double rating;
        public final int season;
        public final String showtitle;
        public final int tvshowid;
        public final String votes;
        public final List<String> writer;

        public DetailsEpisode(JsonNode node) {
            super(node);
            cast = Cast.castListFromJsonNode(node, CAST);
            episode = JsonUtils.intFromJsonNode(node, EPISODE, 0);
            episodeid = JsonUtils.intFromJsonNode(node, EPISODEID);
            firstaired = JsonUtils.stringFromJsonNode(node, FIRSTAIRED);
            originaltitle = JsonUtils.stringFromJsonNode(node, ORIGINALTITLE);
            productioncode = JsonUtils.stringFromJsonNode(node, PRODUCTIONCODE);
            rating = JsonUtils.doubleFromJsonNode(node, RATING, 0);
            season = JsonUtils.intFromJsonNode(node, SEASON, 0);
            showtitle = JsonUtils.stringFromJsonNode(node, SHOWTITLE);
            tvshowid = JsonUtils.intFromJsonNode(node, TVSHOWID);
            votes = JsonUtils.stringFromJsonNode(node, VOTES);
            writer = JsonUtils.stringListFromJsonNode(node, WRITER);
        }
    }

    /**
     * Enums for Video.Fields.MusicVideo
     */
    public interface FieldsMusicVideo {
        public final String TITLE = "title";
        public final String PLAYCOUNT = "playcount";
        public final String RUNTIME = "runtime";
        public final String DIRECTOR = "director";
        public final String STUDIO = "studio";
        public final String YEAR = "year";
        public final String PLOT = "plot";
        public final String ALBUM = "album";
        public final String ARTIST = "artist";
        public final String GENRE = "genre";
        public final String TRACK = "track";
        public final String STREAMDETAILS = "streamdetails";
        public final String LASTPLAYED = "lastplayed";
        public final String FANART = "fanart";
        public final String THUMBNAIL = "thumbnail";
        public final String FILE = "file";
        public final String RESUME = "resume";
        public final String DATEADDED = "dateadded";
        public final String TAG = "tag";
        public final String ART = "art";

        public final static String[] allValues = new String[] {
                TITLE, PLAYCOUNT, RUNTIME, DIRECTOR, STUDIO, YEAR, PLOT, ALBUM, ARTIST, GENRE,
                TRACK, STREAMDETAILS, LASTPLAYED, FANART, THUMBNAIL, FILE, RESUME, DATEADDED,
                TAG, ART
        };
    }

    /**
     * Video.Details.MusicVideo
     */
    public static class DetailsMusicVideo extends DetailsFile {
        public static final String ALBUM = "album";
        public static final String ARTIST = "artist";
        public static final String GENRE = "genre";
        public static final String MUSICVIDEOID = "musicvideoid";
        public static final String STUDIO = "studio";
        public static final String TAG = "tag";
        public static final String TRACK = "track";
        public static final String YEAR = "year";

        public final String album;
        public final List<String> artist;
        public final List<String> genre;
        public final int musicvideoid;
        public final List<String> studio;
        public final List<String> tag;
        public final int track;
        public final int year;

        /**
         * Constructor
         * @param node Json node
         */
        public DetailsMusicVideo(JsonNode node) {
            super(node);
            album = JsonUtils.stringFromJsonNode(node, ALBUM);
            artist = JsonUtils.stringListFromJsonNode(node, ARTIST);
            genre = JsonUtils.stringListFromJsonNode(node, GENRE);
            musicvideoid = JsonUtils.intFromJsonNode(node, MUSICVIDEOID);
            studio = JsonUtils.stringListFromJsonNode(node, STUDIO);
            tag = JsonUtils.stringListFromJsonNode(node, TAG);
            track = JsonUtils.intFromJsonNode(node, TRACK, 0);
            year = JsonUtils.intFromJsonNode(node, YEAR, 0);
        }
    }
}