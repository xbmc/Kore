package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;

import org.xbmc.kore.utils.JsonUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Types from PVR.*
 */
public class PVRType {

    /**
     * Enums for File.Media
     */
    public interface ChannelType {
        String TV = "tv";
        String RADIO = "radio";
        String[] allValues = new String[] {
              TV, RADIO
        };
    }

    /**
     * PVR.Details.ChannelGroup
     */
    public static class DetailsChannelGroup extends ItemType.DetailsBase {
        public static final String CHANNELGROUPID = "channelgroupid";
        public static final String CHANNELTYPE = "channeltype";

        public final int channelgroupid;
        public final String channeltype;

        /**
         * Constructor
         * @param node JSON object representing a Detail object
         */
        public DetailsChannelGroup(JsonNode node) {
            super(node);
            channelgroupid = JsonUtils.intFromJsonNode(node, CHANNELGROUPID);
            channeltype = JsonUtils.stringFromJsonNode(node, CHANNELTYPE, ChannelType.TV);
        }
    }

    /**
     * Enums for PVR.Fields.Broadcast
     */
    public interface FieldsBroadcast {
        String TITLE = "title";
        String PLOT = "plot";
        String PLOTOUTLINE = "plotoutline";
        String STARTTIME = "starttime";
        String ENDTIME = "endtime";
        String RUNTIME = "runtime";
        String PROGRESS = "progress";
        String PROGRESSPERCENTAGE = "progresspercentage";
        String GENRE = "genre";
        String EPISODENAME = "episodename";
        String EPISODENUM = "episodenum";
        String EPISODEPART = "episodepart";
        String FIRSTAIRED = "firstaired";
        String HASTIMER = "hastimer";
        String ISACTIVE = "isactive";
        String PARENTALRATING = "parentalrating";
        String WASACTIVE = "wasactive";
        String THUMBNAIL = "thumbnail";
        String RATING = "rating";
        
        String[] allValues = new String[] {
              TITLE, PLOT, PLOTOUTLINE, STARTTIME, ENDTIME, RUNTIME, PROGRESS, PROGRESSPERCENTAGE, GENRE,
              EPISODENAME, EPISODENUM, EPISODEPART, FIRSTAIRED, HASTIMER, ISACTIVE, PARENTALRATING,
              WASACTIVE, THUMBNAIL, RATING
        };
    }

    /**
     * PVR.Details.Broadcast type
     */
    public static class DetailsBroadcast extends ItemType.DetailsBase {
        public static final String BROADCASTID = "broadcastid";
        public static final String ENDTIME = "endtime";
        public static final String EPISODENAME = "episodename";
        public static final String EPISODENUM = "episodenum";
        public static final String EPISODEPART = "episodepart";
        public static final String FIRSTAIRED = "firstaired";
        public static final String GENRE = "genre";
        public static final String HASTIMER = "hastimer";
        public static final String ISACTIVE = "isactive";
        public static final String PARENTALRATING = "parentalrating";
        public static final String PLOT = "plot";
        public static final String PLOTOUTLINE = "plotoutline";
        public static final String PROGRESS = "progress";
        public static final String PROGRESSPERCENTAGE = "progresspercentage";
        public static final String RATING = "rating";
        public static final String RUNTIME = "runtime";
        public static final String STARTTIME = "starttime";
        public static final String THUMBNAIL = "thumbnail";
        public static final String TITLE = "title";
        public static final String WASACTIVE = "wasactive";

        public final int broadcastid;
        public final String episodename;
        public final int episodenum;
        public final int episodepart;
        public final String firstaired;
        public final String genre;
        public final boolean hastimer;
        public final boolean isactive;
        public final int parentalrating;
        public final String plot;
        public final String plotoutline;
        public final int progress;
        public final double progresspercentage;
        public final int rating;
        public final int runtime;
        public final String thumbnail;
        public final String title;
        public final boolean wasactive;
        public Date starttime;
        public Date endtime;

        /**
         * Constructor
         * @param node JSON object representing a DetailsBroadcast object
         */
        public DetailsBroadcast(JsonNode node) {
            super(node);
            broadcastid = JsonUtils.intFromJsonNode(node, BROADCASTID);
            episodename = JsonUtils.stringFromJsonNode(node, EPISODENAME);
            episodenum = JsonUtils.intFromJsonNode(node, EPISODENUM, 0);
            episodepart = JsonUtils.intFromJsonNode(node, EPISODEPART, 0);
            firstaired = JsonUtils.stringFromJsonNode(node, FIRSTAIRED);
            genre = JsonUtils.stringFromJsonNode(node, GENRE);
            hastimer = JsonUtils.booleanFromJsonNode(node, HASTIMER, false);
            isactive = JsonUtils.booleanFromJsonNode(node, ISACTIVE, false);
            parentalrating = JsonUtils.intFromJsonNode(node, PARENTALRATING, 0);
            plot = JsonUtils.stringFromJsonNode(node, PLOT);
            plotoutline = JsonUtils.stringFromJsonNode(node, PLOTOUTLINE);
            progress = JsonUtils.intFromJsonNode(node, PROGRESS, 0);
            progresspercentage = JsonUtils.doubleFromJsonNode(node, PROGRESSPERCENTAGE, 0);
            rating = JsonUtils.intFromJsonNode(node, RATING, 0);
            runtime = JsonUtils.intFromJsonNode(node, RUNTIME, 0);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
            title = JsonUtils.stringFromJsonNode(node, TITLE);
            wasactive = JsonUtils.booleanFromJsonNode(node, WASACTIVE, false);

            // Get times. All in UTC
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                endtime = sdf.parse(JsonUtils.stringFromJsonNode(node, ENDTIME));
                starttime = sdf.parse(JsonUtils.stringFromJsonNode(node, STARTTIME));
            } catch (ParseException exc) {
                starttime = new Date();
                endtime = new Date();
            }
        }

        /**
         * Returns both the plot outline and the plot, leaving each empty if absent.
         *
         * @param delimiter character to insert between outline and plot, if both are present
         */
        public String getFullPlot(char delimiter) {
            StringBuilder builder = new StringBuilder();
            if (plotoutline != null) {
                builder.append(plotoutline);
            }
            if (plot != null) {
                if (builder.length() != 0) {
                    builder.append(delimiter);
                }
                builder.append(plot);
            }
            return builder.toString();
        }
    }

    /**
     * Enums for PVR.Fields.Channel
     */
    public interface FieldsChannel {
        String THUMBNAIL = "thumbnail";
        String CHANNELTYPE = "channeltype";
        String HIDDEN = "hidden";
        String LOCKED = "locked";
        String CHANNEL = "channel";
        String LASTPLAYED = "lastplayed";
        String BROADCASTNOW = "broadcastnow";
        String BROADCASTNEXT = "broadcastnext";

        String[] allValues = new String[] {
              THUMBNAIL, CHANNELTYPE, HIDDEN, LOCKED, CHANNEL, LASTPLAYED, BROADCASTNOW, BROADCASTNEXT
        };
    }

    /**
     * PVR.Details.Channel
     */
    public static class DetailsChannel extends ItemType.DetailsBase {
        public static final String BROADCASTNEXT = "broadcastnext";
        public static final String BROADCASTNOW = "broadcastnow";
        public static final String CHANNEL = "channel";
        public static final String CHANNELID = "channelid";
        public static final String CHANNELTYPE = "channeltype";
        public static final String HIDDEN = "hidden";
        public static final String LASTPLAYED = "lastplayed";
        public static final String LOCKED = "locked";
        public static final String THUMBNAIL = "thumbnail";

        public final DetailsBroadcast broadcastnext;
        public final DetailsBroadcast broadcastnow;
        public final String channel;
        public final int channelid;
        public final String channeltype;
        public final boolean hidden;
        public final String lastplayed;
        public final boolean locked;
        public final String thumbnail;

        /**
         * Constructor
         * @param node JSON object representing a Detail object
         */
        public DetailsChannel(JsonNode node) {
            super(node);
            broadcastnext = node.has(BROADCASTNEXT) ? new DetailsBroadcast(node.get(BROADCASTNEXT)) : null;
            broadcastnow = node.has(BROADCASTNOW) ? new DetailsBroadcast(node.get(BROADCASTNOW)) : null;
            channel = JsonUtils.stringFromJsonNode(node, CHANNEL);
            channelid = JsonUtils.intFromJsonNode(node, CHANNELID);
            channeltype = JsonUtils.stringFromJsonNode(node, CHANNELTYPE, ChannelType.TV);
            hidden = JsonUtils.booleanFromJsonNode(node, HIDDEN, false);
            lastplayed = JsonUtils.stringFromJsonNode(node, LASTPLAYED);
            locked = JsonUtils.booleanFromJsonNode(node, LOCKED, false);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
        }
    }

    /**
     * Enums for PVR.Fields.Recording
     */
    public interface FieldsRecording {

        String TITLE = "title";
        String PLOT = "plot";
        String PLOTOUTLINE = "plotoutline";
        String GENRE = "genre";
        String PLAYCOUNT = "playcount";
        String RESUME = "resume";
        String CHANNEL = "channel";
        String STARTTIME = "starttime";
        String ENDTIME = "endtime";
        String RUNTIME = "runtime";
        String LIFETIME = "lifetime";
        String ICON = "icon";
        String ART = "art";
        String STREAMURL = "streamurl";
        String FILE = "file";
        String DIRECTORY = "directory";

        String[] allValues = new String[] {
                TITLE, PLOT, PLOTOUTLINE, GENRE, PLAYCOUNT, RESUME, CHANNEL, STARTTIME, ENDTIME, RUNTIME,
                LIFETIME, ICON, ART, STREAMURL, FILE, DIRECTORY
        };
    }

    /**
     * PVR.Details.Recording
     */
    public static class DetailsRecording extends ItemType.DetailsBase {
        public static final String ART = "art";
        public static final String CHANNEL = "channel";
        public static final String DIRECTORY = "directory";
        public static final String ENDTIME = "endtime";
        public static final String FILE = "file";
        public static final String GENRE = "genre";
        public static final String ICON = "icon";
        public static final String LIFETIME = "lifetime";
        public static final String PLAYCOUNT = "playcount";
        public static final String PLOT = "plot";
        public static final String PLOTOUTLINE = "plotoutline";
        public static final String RECORDINGID = "recordingid";
        public static final String RESUME = "resume";
        public static final String RUNTIME = "runtime";
        public static final String STARTTIME = "starttime";
        public static final String STREAMURL = "streamurl";
        public static final String TITLE = "title";

        public final MediaType.Artwork art;
        public final String channel;
        public final String directory;
        public final String endtime;
        public final String file;
        public final String genre;
        public final String icon;
        public final int lifetime;
        public final int playcount;
        public final String plot;
        public final String plotoutline;
        public final int recordingid;
        public final VideoType.Resume resume;
        public final int runtime;
        public final String starttime;
        public final String streamurl;
        public final String title;

        /**
         * Constructor
         * @param node JSON object representing a Detail object
         */
        public DetailsRecording(JsonNode node) {
            super(node);
            art = node.has(ART) ? new MediaType.Artwork(node.get(ART)) : null;
            channel = JsonUtils.stringFromJsonNode(node, CHANNEL);
            directory = JsonUtils.stringFromJsonNode(node, DIRECTORY);
            endtime = JsonUtils.stringFromJsonNode(node, ENDTIME);
            file = JsonUtils.stringFromJsonNode(node, FILE);
            genre = JsonUtils.stringFromJsonNode(node, GENRE);
            icon = JsonUtils.stringFromJsonNode(node, ICON);
            lifetime = JsonUtils.intFromJsonNode(node, LIFETIME, 0);
            playcount = JsonUtils.intFromJsonNode(node, PLAYCOUNT, 0);
            plot = JsonUtils.stringFromJsonNode(node, PLOT);
            plotoutline = JsonUtils.stringFromJsonNode(node, PLOTOUTLINE);
            recordingid = JsonUtils.intFromJsonNode(node, RECORDINGID, 0);
            resume = node.has(RESUME) ? new VideoType.Resume(node.get(RESUME)) : null;
            runtime = JsonUtils.intFromJsonNode(node, RUNTIME, 0);
            starttime = JsonUtils.stringFromJsonNode(node, STARTTIME);
            streamurl = JsonUtils.stringFromJsonNode(node, STREAMURL);
            title = JsonUtils.stringFromJsonNode(node, TITLE);
        }
    }

}
