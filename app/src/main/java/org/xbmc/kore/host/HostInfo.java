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
package org.xbmc.kore.host;

import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.utils.LogUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * XBMC Host information container.
 */
public class HostInfo {
	private static final String TAG = LogUtils.makeLogTag(HostInfo.class);

	private static final String JSON_RPC_ENDPOINT = "/jsonrpc";

	/**
	 * Default HTTPS port
	 */
	public static final int DEFAULT_HTTPS_PORT = 443;

	/**
	 * Default HTTP port for XBMC (80 on Windows, 8080 on others)
 	 */
	public static final int DEFAULT_HTTP_PORT = 8080;

	/**
	 * Default TCP port for XBMC
	 */
	public static final int DEFAULT_TCP_PORT = 9090;

    /**
     * Default WoL port
     */
    public static final int DEFAULT_WOL_PORT = 9;

	/**
	 * Default EventServer port for Kodi
	 */
	public static final int DEFAULT_EVENT_SERVER_PORT = 9777;

	public static final int KODI_V12_FRODO = 12;
	public static final int KODI_V13_GOTHAM = 13;
	public static final int KODI_V14_HELIX = 14;
	public static final int KODI_V15_ISENGARD = 15;
	public static final int KODI_V16_JARVIS = 16;
	public static final int KODI_V17_KRYPTON = 17;
	public static final int KODI_V18_LEIA = 18;

	public static final int DEFAULT_KODI_VERSION_MAJOR = KODI_V16_JARVIS;
    public static final int DEFAULT_KODI_VERSION_MINOR = 1;
    public static final String DEFAULT_KODI_VERSION_REVISION = "Unknown";
    public static final String DEFAULT_KODI_VERSION_TAG = "stable";


    /**
	 * Internal id of the host
	 */
	private final int id;

	/**
	 * Friendly name of the host
	 */
	private final String name;

	/**
	 * Connection information
	 */
	private final String address;
	private final int httpPort;
	private final int tcpPort;
	public final boolean isHttps;

    private boolean useEventServer;
	private final int eventServerPort;

    /**
	 * Authentication information
	 */
	private final String username;
	private final String password;

    /**
     * Mac address and Wake On Lan port
     */
    private String macAddress;
    private int wolPort;

	/**
	 * Prefered protocol to communicate with this host
	 */
	private int protocol;


    /**
     * Kodi Version
     */
    private int kodiVersionMajor;
    private int kodiVersionMinor;
    private String kodiVersionRevision;
    private String kodiVersionTag;

    /**
     * Last time updated (in millis)
     */
    private final long updated;

    private final String auxImageHttpAddress;

    /**
	 * Full constructor. This constructor should be used when instantiating from the database
	 *
	 * @param name Friendly name of the host
	 * @param id ID
	 * @param address URL
	 * @param protocol Protocol
	 * @param httpPort HTTP Port
	 * @param tcpPort TCP Port
	 * @param username Username for basic auth
	 * @param password Password for basic auth
	 */
	public HostInfo(int id, String name, String address, int protocol, int httpPort, int tcpPort,
					String username, String password, String macAddress, int wolPort,
                    boolean useEventServer, int eventServerPort,
                    int kodiVersionMajor, int kodiVersionMinor, String kodiVersionRevision, String kodiVersionTag,
                    long updated, boolean isHttps) {
		this.id = id;
		this.name = name;
		this.address = address;
        if (!HostConnection.isValidProtocol(protocol)) {
            throw new IllegalArgumentException("Invalid protocol specified.");
        }
		this.protocol = protocol;
		this.httpPort = httpPort;
		this.isHttps = isHttps;
		this.tcpPort = tcpPort;
		this.username = username;
		this.password = password;
        this.macAddress = macAddress;
        this.wolPort = wolPort;

        this.useEventServer = useEventServer;
		this.eventServerPort = eventServerPort;

        this.kodiVersionMajor = kodiVersionMajor;
        this.kodiVersionMinor = kodiVersionMinor;
        this.kodiVersionRevision = kodiVersionRevision;
        this.kodiVersionTag = kodiVersionTag;
        this.updated = updated;

        // For performance reasons
        this.auxImageHttpAddress = getHttpURL() + "/image/";
	}

	/**
	 * Auxiliary constructor for HTTP protocol.
	 * This constructor should only be used to test connections. It doesn't represent an
	 * instance of the host in the database.
	 *
	 * @param name Friendly name of the host
	 * @param address URL
	 * @param httpPort HTTP Port
	 * @param username Username for basic auth
	 * @param password Password for basic auth
	 */
	public HostInfo(String name, String address, int protocol, int httpPort,
                    int tcpPort, String username, String password,
                    boolean useEventServer, int eventServerPort, boolean isHttps) {
        this(-1, name, address, protocol, httpPort, tcpPort, username,
             password, null, DEFAULT_WOL_PORT, useEventServer, eventServerPort,
             DEFAULT_KODI_VERSION_MAJOR, DEFAULT_KODI_VERSION_MINOR,
             DEFAULT_KODI_VERSION_REVISION, DEFAULT_KODI_VERSION_TAG,
             0, isHttps);
	}

    public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public int getTcpPort() {
		return tcpPort;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getWolPort() {
        return wolPort;
    }

    public void setWolPort(int wolPort) {
        this.wolPort = wolPort;
    }

    public int getProtocol() {
		return protocol;
	}

    public boolean getUseEventServer() {
        return useEventServer;
    }

	public int getEventServerPort() {
		return eventServerPort;
	}

    public int getKodiVersionMajor() {
        return kodiVersionMajor;
    }

    public int getKodiVersionMinor() {
        return kodiVersionMinor;
    }

    public String getKodiVersionRevision() {
        return kodiVersionRevision;
    }

    public String getKodiVersionTag() {
        return kodiVersionTag;
    }

    public long getUpdated() {
        return updated;
    }

    /**
     * Overrides the protocol for this host info
     * @param protocol Protocol
     */
    public void setProtocol(int protocol) {
        if (!HostConnection.isValidProtocol(protocol)) {
            throw new IllegalArgumentException("Invalid protocol specified.");
        }
        this.protocol = protocol;
    }

    /**
     * Overrides the use of EventServer
     * @param useEventServer Whether to use EventServer
     */
    public void setUseEventServer(boolean useEventServer) {
        this.useEventServer = useEventServer;
    }

    public void setKodiVersionMajor(int kodiVersionMajor) {
        this.kodiVersionMajor = kodiVersionMajor;
    }

    public void setKodiVersionMinor(int kodiVersionMinor) {
        this.kodiVersionMinor = kodiVersionMinor;
    }

    public void setKodiVersionRevision(String kodiVersionRevision) {
        this.kodiVersionRevision = kodiVersionRevision;
    }

    public void setKodiVersionTag(String kodiVersionTag) {
        this.kodiVersionTag = kodiVersionTag;
    }

    public boolean isGothamOrLater() {
        return kodiVersionMajor > KODI_V13_GOTHAM;
    }

    public boolean isKryptonOrLater() {
        return kodiVersionMajor > KODI_V17_KRYPTON;
    }

    /**
	 * Returns the URL of the host
	 * @return HTTP URL eg. http://192.168.1.1:8080
	 */
	public String getHttpURL() {
		String scheme = isHttps ? "https://" : "http://";
		return scheme + address + ":" + httpPort;
	}

	/**
	 * Returns the JSON RPC endpoint URL of the host
	 * @return HTTP URL eg. http://192.168.1.1:8080/jsonrpc
	 */
	public String getJsonRpcHttpEndpoint() {
		return getHttpURL() + JSON_RPC_ENDPOINT;
	}

    /**
     * Get the URL of an image, given the image identifier returned by XBMC
     * @param image image identifier stored in XBMC
     * @return URL on the XBMC host on which the image can be fetched
     */
    public String getImageUrl(String image) {
        if (image == null) {
            return null;
        }

        try {
//            return getHttpURL() + "/image/" + URLEncoder.encode(image, "UTF-8");
            return auxImageHttpAddress + URLEncoder.encode(image, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Ignore for now...
            return null;
        }
    }
}
