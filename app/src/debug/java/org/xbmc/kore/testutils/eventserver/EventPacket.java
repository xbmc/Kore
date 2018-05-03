/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.eventserver;

import org.xbmc.kore.eventclient.Packet;
import org.xbmc.kore.utils.LogUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Class that implements a single event packet.
 * <pre>
 *   -----------------------------
 *   | -H1 Signature ("XBMC")    | - 4  x CHAR                4B
 *   | -H2 Version (eg. 2.0)     | - 2  x UNSIGNED CHAR       2B
 *   | -H3 PacketType            | - 1  x UNSIGNED SHORT      2B
 *   | -H4 Sequence number       | - 1  x UNSIGNED LONG       4B
 *   | -H5 No. of packets in msg | - 1  x UNSIGNED LONG       4B
 *   | -H6 Payloadsize of packet | - 1  x UNSIGNED SHORT      2B
 *   | -H7 Client's unique token | - 1  x UNSIGNED LONG       4B
 *   | -H8 Reserved              | - 10 x UNSIGNED CHAR      10B
 *   |---------------------------|
 *   | -P1 payload               | -
 *   -----------------------------
 * </pre>
 */
abstract public class EventPacket {

    private static final String TAG = LogUtils.makeLogTag(EventPacket.class);

    //Package types
    public final static byte PT_BUTTON        = 0x03;

    private String signature;
    private String version;
    private int packetType;
    private long sequenceNumber;
    private long numberOfPackets;
    private int payloadSize;
    private long token;

    private byte[] payload;

    private EventPacket() {}

    EventPacket(byte[] packet) {
        signature = new String(new byte[] {packet[0], packet[1], packet[2], packet[3]});
        version = ((int) packet[4]) + "." + ((int) packet[5]);
        packetType = ByteBuffer.wrap(packet, 6, 2).getChar();
        sequenceNumber = ByteBuffer.wrap(packet, 8, 4).getInt();
        numberOfPackets = ByteBuffer.wrap(packet, 12, 4).getInt();
        payloadSize = ByteBuffer.wrap(packet, 16, 2).getChar();
        token = ByteBuffer.wrap(packet, 18, 4).getInt();
        //Reserved 22 - 32
        payload = new byte[payloadSize];
        ByteBuffer.wrap(packet, 32, payloadSize).get(payload);
    }

    @Override
    public String toString() {
        return signature + ":" +
               version + ":" +
               packetType + ":" +
               sequenceNumber + ":" +
               numberOfPackets + ":" +
               payloadSize + ":" +
               token+ ":" +
               payload;
    }

    public int getPacketType() {
        return packetType;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Returns the packet type from a {@link Packet} as a single byte.
     * <br/>
     * Note that, although the specification specifies two bytes,
     * we only use a single byte in {@link Packet} for the packet types.
     * @param packet
     * @return second byte of packet type
     */
    static public byte getPacketType(byte[] packet) {
        return packet[7];
    }

    /**
     * Gets the string from payload terminated by 0x00.
     * @param payload byte array holding the characters
     * @param offset starting offset of string
     * @return string from payload or null if not found
     */
    String getStringFromPayload(byte[] payload, int offset) {
        int strTerminatorIndex = offset;
        for (; strTerminatorIndex < payload.length; strTerminatorIndex++) {
            if (payload[strTerminatorIndex] == 0x00)
                break;
        }

        if (strTerminatorIndex == payload.length)
            return null;

        int stringLength = strTerminatorIndex - offset;
        byte[] bytes = new byte[stringLength];
        System.arraycopy(payload, offset, bytes, 0, stringLength);

        return new String(bytes);
    }
}
