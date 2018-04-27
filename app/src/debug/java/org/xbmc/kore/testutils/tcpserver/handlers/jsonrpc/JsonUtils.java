package org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc;


import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.utils.LogUtils;

public class JsonUtils {
    /**
     * Fills objectNode with time values
     * @param objectNode
     * @param timeSec
     * @return objectNode for chaining
     */
    public static ObjectNode createTimeNode(ObjectNode objectNode, long timeSec) {
        int hours = (int) timeSec / 3600;
        int minutes = (int) ( timeSec / 60 ) % 60;
        int seconds = (int)  timeSec % 60 ;
        return createTimeNode(objectNode, hours, minutes, seconds, 0);
    }

    /**
     * Fills objectNode with time values
     * @param objectNode
     * @param hours
     * @param minutes
     * @param seconds
     * @param milliseconds
     * @return objectNode for chaining
     */
    public static ObjectNode createTimeNode(ObjectNode objectNode, int hours, int minutes, int seconds, int milliseconds) {
        objectNode.put(GlobalType.Time.HOURS, hours);
        objectNode.put(GlobalType.Time.MINUTES, minutes);
        objectNode.put(GlobalType.Time.SECONDS, seconds);
        objectNode.put(GlobalType.Time.MILLISECONDS, milliseconds);
        return objectNode;
    }
}
