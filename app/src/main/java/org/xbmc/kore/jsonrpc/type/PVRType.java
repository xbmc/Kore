package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;

import org.xbmc.kore.utils.JsonUtils;

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
        
        public final static String[] allValues = new String[] {
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
        public final String endtime;
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
        public final String starttime;
        public final String thumbnail;
        public final String title;
        public final boolean wasactive;

        /**
         * Constructor
         * @param node JSON object representing a DetailsBroadcast object
         */
        public DetailsBroadcast(JsonNode node) {
            super(node);
            broadcastid = JsonUtils.intFromJsonNode(node, BROADCASTID);
            endtime = JsonUtils.stringFromJsonNode(node, ENDTIME);
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
            starttime = JsonUtils.stringFromJsonNode(node, STARTTIME);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL);
            title = JsonUtils.stringFromJsonNode(node, TITLE);
            wasactive = JsonUtils.booleanFromJsonNode(node, WASACTIVE, false);
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

        public final static String[] allValues = new String[] {
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

}
