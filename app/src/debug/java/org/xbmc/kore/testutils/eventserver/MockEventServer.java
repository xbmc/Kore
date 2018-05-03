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

import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MockEventServer extends Thread {
    private static final String TAG = LogUtils.makeLogTag(MockEventServer.class);

    private int listenPort = 9997;
    private boolean keepRunning;
    private EventPacket packet;
    private DatagramSocket datagramSocket;

    public MockEventServer() {
    }

    public void setListenPort(int portNumber) {
        this.listenPort = portNumber;
    }

    public void run() {
        try {
            datagramSocket = new DatagramSocket(this.listenPort);
        } catch (SocketException e) {
            System.out.println("MockEventServer: Failed to open socket: " + e.getMessage());
            return;
        }

        keepRunning = true;
        while(keepRunning) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            try {
                datagramSocket.receive(datagramPacket);
                packet = new EventPacketBUTTON(datagramPacket.getData());
            } catch (IOException e) {
                System.out.println("MockEventServer: error receiving packet: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the last received packet
     * @return
     */
    public EventPacket getEventPacket() {
        return packet;
    }

    /**
     * Stops the server from listening for new packets
     */
    public void shutdown() {
        keepRunning = false;
        datagramSocket.close();
    }

    /**
     * Resets the state of the event server
     */
    public void reset() {
        packet = null;
    }
}
