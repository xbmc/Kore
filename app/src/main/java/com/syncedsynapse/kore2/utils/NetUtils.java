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
package com.syncedsynapse.kore2.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Various utilities related to networking
 */
public class NetUtils {
    private static final String TAG = LogUtils.makeLogTag(NetUtils.class);

    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        if (hostAddress == 0)
            return null;

        byte[] addressBytes = { (byte)(0xff & hostAddress),
                                (byte)(0xff & (hostAddress >> 8)),
                                (byte)(0xff & (hostAddress >> 16)),
                                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    /**
     * Tries to return the MAC address of a host on the same subnet by looking at the ARP cache..
     * Note: This is a synchronous call, so it should only be called on a background thread
     *
     * @param hostAddress Hostname or IP address
     * @return MAC address if found or null
     */
    public static String getMacAddress(String hostAddress) {
        String ipHostAddress;
        LogUtils.LOGD(TAG, "Starting get Mac Address for: " + hostAddress);
        try {
            InetAddress inet = InetAddress.getByName(hostAddress);

            // Send some traffic, with a timeout
            boolean reachable = inet.isReachable(1000);

            ipHostAddress = inet.getHostAddress();
        } catch (UnknownHostException e) {
            LogUtils.LOGD(TAG, "Got an UnknownHostException for host: " + hostAddress, e);
            return null;
        } catch (IOException e) {
            LogUtils.LOGD(TAG, "Couldn't check reachability of host: " + hostAddress, e);
            return null;
        }

        try {
            // Read the arp cache
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));

            String arpLine;
            while ((arpLine = br.readLine()) != null) {
                if (arpLine.startsWith(ipHostAddress)) {
                    // Ok, this is the line, get the MAC Address
                    br.close();
                    return arpLine.split("\\s+")[3].toUpperCase(); // 4th element
                }
            }
            br.close();
        } catch (IOException e) {
            LogUtils.LOGD(TAG, "Couldn check ARP cache.", e);
        }
        return null;
    }

    /**
     * Sends a Wake On Lan magic packet to a host
     * Note: This is a synchronous call, so it should only be called on a background thread
     *
     * @param macAddress MAC address
     * @param hostAddress Hostname or IP address
     * @param port Port for Wake On Lan
     * @return Whether the packet was successfully sent
     */
    public static boolean sendWolMagicPacket(String macAddress, String hostAddress, int port) {
        if (macAddress == null) {
            return false;
        }

        // Get MAC adress bytes
        byte[] macAddressBytes = new byte[6];
        String[] hex = macAddress.split("(\\:|\\-)");
        if (hex.length != 6) {
            LogUtils.LOGD(TAG, "Send magic packet: got an invalid MAC address: " + macAddress);
            return false;
        }

        try {
            for (int i = 0; i < 6; i++) {
                macAddressBytes[i] = (byte)Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            LogUtils.LOGD(TAG, "Send magic packet: got an invalid MAC address: " + macAddress);
            return false;
        }

        byte[] bytes = new byte[6 + 16 * macAddressBytes.length];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte)0xff;
        }
        for (int i = 6; i < bytes.length; i += macAddressBytes.length) {
            System.arraycopy(macAddressBytes, 0, bytes, i, macAddressBytes.length);
        }

        try {
            InetAddress address = InetAddress.getByName(hostAddress);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            LogUtils.LOGD(TAG, "Exception while sending magic packet.", e);
            return false;
        }
        return true;
    }

    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }
}
