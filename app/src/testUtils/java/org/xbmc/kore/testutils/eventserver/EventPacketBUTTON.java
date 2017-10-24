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

import java.nio.ByteBuffer;

public class EventPacketBUTTON extends EventPacket {

    private short code;
    private String mapName;
    private String buttonName;
    private boolean repeat;
    private boolean down;
    private boolean queue;
    private short amount;
    private byte axis;
    private short flags;

    public EventPacketBUTTON(byte[] packet) {
        super(packet);

        byte[] payload = getPayload();
        code = ByteBuffer.wrap(payload, 0, 2).getShort();
        flags = ByteBuffer.wrap(payload, 2, 2).getShort();
        amount = ByteBuffer.wrap(payload, 4, 2).getShort();

        mapName = getStringFromPayload(payload, 6);

        int nextStringPosition = 6 + mapName.getBytes().length + 1;
        buttonName = getStringFromPayload(payload, nextStringPosition);
    }

    public String getButtonName() {
        return buttonName;
    }

    public String getMapName() {
        return mapName;
    }

    @Override
    public String toString() {
        return super.toString() +
               ", code: " + code +
               ", flags: " + flags +
               ", amount: " + amount +
               ", mapName: " + mapName +
               ", buttonName: " + buttonName;
    }
}
