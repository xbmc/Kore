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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import org.xbmc.kore.Settings;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.NetUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Manages XBMC Hosts
 * Singleton that loads the list of registered hosts, keeps a
 * {@link HostConnection} to the active host
 * and allows for creation and removal of hosts
 */
public class HostManager {
	private static final String TAG = LogUtils.makeLogTag(HostManager.class);

	// Singleton instance
	private static volatile HostManager instance = null;

	private Context context;

	/**
	 * Arraylist that will hold all the hosts in the database
	 */
	private ArrayList<HostInfo> hosts = new ArrayList<HostInfo>();

	/**
	 * Current host
	 */
	private HostInfo currentHostInfo = null;
    /**
     * Current host connection
     */
	private HostConnection currentHostConnection = null;

    /**
     * Picasso to download images from current XBMC
     */
    private Picasso currentPicasso = null;

    /**
     * Current connection observer
     */
    private HostConnectionObserver currentHostConnectionObserver = null;

    /**
     * Singleton constructor
     * @param context Context (can pass Activity context, will get App Context)
     */
	protected HostManager(Context context) {
		this.context = context.getApplicationContext();
	}

	/**
	 * Singleton access method
	 * @param context Android app context
	 * @return HostManager singleton
	 */
	public static HostManager getInstance(Context context) {
		if (instance == null) {
            synchronized (HostManager.class) {
                if (instance == null) {
                    instance = new HostManager(context);
                }
            }
		}
		return instance;
	}

	/**
	 * Returns the current host list
	 * @return Host list
	 */
    public ArrayList<HostInfo> getHosts() {
        return getHosts(false);
    }

    /**
     * Returns the current host list, maybe forcing a reload from the database
     * @param forcedReload Whether to force a reload from the database
     * @return Host list
     */
	public ArrayList<HostInfo> getHosts(boolean forcedReload) {
        if (forcedReload || (hosts.isEmpty())) {
            hosts.clear();

            Cursor cursor = context.getContentResolver()
                                   .query(MediaContract.Hosts.CONTENT_URI,
                                           MediaContract.Hosts.ALL_COLUMNS,
                                           null, null, null);
            if (cursor == null) return hosts;

            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int idx = 0;
                    int id = cursor.getInt(idx++);
                    long updated = cursor.getLong(idx++);
                    String name = cursor.getString(idx++);
                    String address = cursor.getString(idx++);
                    int protocol = cursor.getInt(idx++);
                    int httpPort = cursor.getInt(idx++);
                    int tcpPort = cursor.getInt(idx++);
                    String username = cursor.getString(idx++);
                    String password = cursor.getString(idx++);
                    String macAddress = cursor.getString(idx++);
                    int wolPort = cursor.getInt(idx++);
                    boolean useEventServer = (cursor.getInt(idx++) != 0);
                    int eventServerPort = cursor.getInt(idx++);

                    int kodiVersionMajor = cursor.getInt(idx++);
                    int kodiVersionMinor = cursor.getInt(idx++);
                    String kodiVersionRevision = cursor.getString(idx++);
                    String kodiVersionTag = cursor.getString(idx++);
                    boolean isHttps = (cursor.getInt(idx++) != 0);

                    hosts.add(new HostInfo(
                            id, name, address, protocol, httpPort, tcpPort,
                            username, password, macAddress, wolPort, useEventServer, eventServerPort,
                            kodiVersionMajor, kodiVersionMinor, kodiVersionRevision, kodiVersionTag,
                            updated, isHttps));
                }
            }
            cursor.close();
        }
		return hosts;
	}

	/**
	 * Returns the current active host info
	 * @return Active host info
	 */
	public HostInfo getHostInfo() {
        if (currentHostInfo == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int currentHostId = prefs.getInt(Settings.KEY_PREF_CURRENT_HOST_ID, Settings.DEFAULT_PREF_CURRENT_HOST_ID);

            ArrayList<HostInfo> hosts = getHosts();

            // No host selected. Check if there are hosts configured and default to the first one
            if (currentHostId == -1) {
                if (!hosts.isEmpty()) {
                    currentHostInfo = hosts.get(0);
                    currentHostId = currentHostInfo.getId();
                    prefs.edit()
                            .putInt(Settings.KEY_PREF_CURRENT_HOST_ID, currentHostId)
                            .apply();
                }
            } else {
                for (HostInfo host : hosts) {
                    if (host.getId() == currentHostId) {
                        currentHostInfo = host;
                        break;
                    }
                }
            }
        }
		return currentHostInfo;
	}

	/**
	 * Returns the current active host connection
 	 * @return Active host connection
	 */
	public HostConnection getConnection() {
        if (currentHostConnection == null) {
            currentHostInfo = getHostInfo();

            if (currentHostInfo != null) {
                currentHostConnection = new HostConnection(currentHostInfo);
            }
        }
		return currentHostConnection;
	}

    /**
     * Returns the current host {@link Picasso} image downloader
     * @return {@link Picasso} instance suitable to download images from the current xbmc
     */
    public Picasso getPicasso() {
        if (currentPicasso == null) {
            currentHostInfo = getHostInfo();
            if (currentHostInfo != null) {
//                currentPicasso = new Picasso.Builder(context)
//                        .downloader(new BasicAuthUrlConnectionDownloader(context,
//                                currentHostInfo.getUsername(), currentHostInfo.getPassword()))
//                        .indicatorsEnabled(BuildConfig.DEBUG)
//                        .build();

                // Http client should already handle authentication
                OkHttpClient picassoClient = getConnection().getOkHttpClient().clone();

//                OkHttpClient picassoClient = new OkHttpClient();
//                // Set authentication on the client
//                if (!TextUtils.isEmpty(currentHostInfo.getUsername())) {
//                    picassoClient.interceptors().add(new Interceptor() {
//                        @Override
//                        public Response intercept(Chain chain) throws IOException {
//
//                            String creds = currentHostInfo.getUsername() + ":" + currentHostInfo.getPassword();
//                            Request newRequest = chain.request().newBuilder()
//                                    .addHeader("Authorization",
//                                            "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP))
//                                    .build();
//                            return chain.proceed(newRequest);
//                        }
//                    });
//                }

                // Set cache
                File cacheDir = NetUtils.createDefaultCacheDir(context);
                long cacheSize = NetUtils.calculateDiskCacheSize(cacheDir);
                picassoClient.setCache(new com.squareup.okhttp.Cache(cacheDir,cacheSize));

                currentPicasso = new Picasso.Builder(context)
                        .downloader(new OkHttpDownloader(picassoClient))
//                        .indicatorsEnabled(BuildConfig.DEBUG)
                        .build();
            }
        }

        return currentPicasso;
    }

    /**
     * Returns the current {@link HostConnectionObserver} for the current connection
     * @return The {@link HostConnectionObserver} for the current connection
     */
    public HostConnectionObserver getHostConnectionObserver() {
        if (currentHostConnectionObserver == null) {
            currentHostConnection = getConnection();
            if (currentHostConnection != null) {
                currentHostConnectionObserver = new HostConnectionObserver(currentHostConnection);
            }
        }
        return currentHostConnectionObserver;
    }

    /**
	 * Sets the current host.
	 * @param hostInfo Host info
	 */
	public void switchHost(HostInfo hostInfo) {
        releaseCurrentHost();

        currentHostInfo = hostInfo;
        if (currentHostInfo != null) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putInt(Settings.KEY_PREF_CURRENT_HOST_ID, currentHostInfo.getId())
                    .apply();
        }
	}

//	/**
//	 * Sets the current host.
//	 * Throws {@link java.lang.IllegalArgumentException} if the host doesn't exist
//	 * @param hostId Host id
//	 */
//	public void switchHost(int hostId) {
//		ArrayList<HostInfo> hosts = getHosts();
//		HostInfo newHostInfo = null;
//
//		for (HostInfo host : hosts) {
//			if (host.getId() == hostId) {
//				newHostInfo = host;
//				break;
//			}
//		}
//
//		if (newHostInfo == null) {
//			throw new IllegalArgumentException("Host doesn't exist!");
//		}
//		switchHost(newHostInfo);
//	}

    /**
     * Adds a new XBMC host to the database
     * @param hostInfo Host to add
     * @return Newly created {@link org.xbmc.kore.host.HostInfo}
     */
    public HostInfo addHost(HostInfo hostInfo) {
        return addHost(hostInfo.getName(), hostInfo.getAddress(), hostInfo.getProtocol(),
                       hostInfo.getHttpPort(), hostInfo.getTcpPort(),
                       hostInfo.getUsername(), hostInfo.getPassword(),
                       hostInfo.getMacAddress(), hostInfo.getWolPort(),
                       hostInfo.getUseEventServer(), hostInfo.getEventServerPort(),
                       hostInfo.getKodiVersionMajor(), hostInfo.getKodiVersionMinor(),
                       hostInfo.getKodiVersionRevision(), hostInfo.getKodiVersionTag(),
                       hostInfo.isHttps);
    }

    /**
	 * Adds a new XBMC host to the database
	 * @param name Name of this instance
	 * @param address Hostname or IP Address
	 * @param protocol Protocol to use
	 * @param httpPort HTTP port
	 * @param tcpPort TCP port
	 * @param username Username for HTTP
	 * @param password Password for HTTP
	 * @return Newly created {@link org.xbmc.kore.host.HostInfo}
	 */
	public HostInfo addHost(String name, String address, int protocol, int httpPort, int tcpPort,
						   String username, String password, String macAddress, int wolPort,
                            boolean useEventServer, int eventServerPort,
                            int kodiVersionMajor, int kodiVersionMinor, String kodiVersionRevision, String kodiVersionTag,
                            boolean isHttps) {

		ContentValues values = new ContentValues();
		values.put(MediaContract.HostsColumns.NAME, name);
		values.put(MediaContract.HostsColumns.ADDRESS, address);
		values.put(MediaContract.HostsColumns.PROTOCOL, protocol);
		values.put(MediaContract.HostsColumns.HTTP_PORT, httpPort);
		values.put(MediaContract.HostsColumns.TCP_PORT, tcpPort);
		values.put(MediaContract.HostsColumns.USERNAME, username);
		values.put(MediaContract.HostsColumns.PASSWORD, password);
        values.put(MediaContract.HostsColumns.MAC_ADDRESS, macAddress);
        values.put(MediaContract.HostsColumns.WOL_PORT, wolPort);
        values.put(MediaContract.HostsColumns.USE_EVENT_SERVER, useEventServer);
        values.put(MediaContract.HostsColumns.EVENT_SERVER_PORT, eventServerPort);
        values.put(MediaContract.HostsColumns.KODI_VERSION_MAJOR, kodiVersionMajor);
        values.put(MediaContract.HostsColumns.KODI_VERSION_MINOR, kodiVersionMinor);
        values.put(MediaContract.HostsColumns.KODI_VERSION_REVISION, kodiVersionRevision);
        values.put(MediaContract.HostsColumns.KODI_VERSION_TAG, kodiVersionTag);
        values.put(MediaContract.HostsColumns.IS_HTTPS, isHttps);


        Uri newUri = context.getContentResolver()
                            .insert(MediaContract.Hosts.CONTENT_URI, values);
        long newId = Long.valueOf(MediaContract.Hosts.getHostId(newUri));

		// Refresh the list and return the created host
		hosts = getHosts(true);
		HostInfo newHost = null;
		for (HostInfo host : hosts) {
			if (host.getId() == newId) {
				newHost = host;
				break;
			}
		}
		return newHost;
	}

    /**
     * Edits a host on the database
     * @param hostId Id of the host to edit
     * @param newHostInfo New values to update
     * @return New {@link HostInfo} object
     */
    public HostInfo editHost(int hostId, HostInfo newHostInfo) {
        ContentValues values = new ContentValues();
        values.put(MediaContract.HostsColumns.NAME, newHostInfo.getName());
        values.put(MediaContract.HostsColumns.ADDRESS, newHostInfo.getAddress());
        values.put(MediaContract.HostsColumns.PROTOCOL, newHostInfo.getProtocol());
        values.put(MediaContract.HostsColumns.HTTP_PORT, newHostInfo.getHttpPort());
        values.put(MediaContract.HostsColumns.TCP_PORT, newHostInfo.getTcpPort());
        values.put(MediaContract.HostsColumns.USERNAME, newHostInfo.getUsername());
        values.put(MediaContract.HostsColumns.PASSWORD, newHostInfo.getPassword());
        values.put(MediaContract.HostsColumns.MAC_ADDRESS, newHostInfo.getMacAddress());
        values.put(MediaContract.HostsColumns.WOL_PORT, newHostInfo.getWolPort());
        values.put(MediaContract.HostsColumns.USE_EVENT_SERVER, newHostInfo.getUseEventServer());
        values.put(MediaContract.HostsColumns.EVENT_SERVER_PORT, newHostInfo.getEventServerPort());
        values.put(MediaContract.HostsColumns.KODI_VERSION_MAJOR, newHostInfo.getKodiVersionMajor());
        values.put(MediaContract.HostsColumns.KODI_VERSION_MINOR, newHostInfo.getKodiVersionMinor());
        values.put(MediaContract.HostsColumns.KODI_VERSION_REVISION, newHostInfo.getKodiVersionRevision());
        values.put(MediaContract.HostsColumns.KODI_VERSION_TAG, newHostInfo.getKodiVersionTag());
        values.put(MediaContract.HostsColumns.IS_HTTPS, newHostInfo.isHttps);

        context.getContentResolver()
               .update(MediaContract.Hosts.buildHostUri(hostId), values, null, null);

        // Refresh the list and return the created host
        hosts = getHosts(true);
        HostInfo newHost = null;
        for (HostInfo host : hosts) {
            if (host.getId() == hostId) {
                newHost = host;
                break;
            }
        }
        return newHost;
    }

    /**
     * Deletes a host from the database.
     * If the delete host is the current one, we will try too change the current one to another
     * or set it to null if there's no other
     * @param hostId Id of the host to delete
     */
	public void deleteHost(final int hostId) {
        // Async call delete. The triggers to delete all host information can take some time
        new Thread(new Runnable() {
            @Override
            public void run() {
                context.getContentResolver()
                       .delete(MediaContract.Hosts.buildHostUri(hostId), null, null);
            }
        }).start();

        // Refresh information
        int index = -1;
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).getId() == hostId) {
                index = i;
                break;
            }
        }
        if (index != -1)
            hosts.remove(index);
        // If we just deleted the current connection, switch to another
        if ((currentHostInfo != null) && (currentHostInfo.getId() == hostId)) {
            releaseCurrentHost();
            if (!hosts.isEmpty())
                switchHost(hosts.get(0));
        }
	}

    /**
     * Releases all state related to the current connection
     */
    private void releaseCurrentHost() {
        if (currentHostConnectionObserver != null) {
            currentHostConnectionObserver.stopObserving();
            currentHostConnectionObserver = null;
        }

        if (currentHostConnection != null) {
            currentHostConnection.disconnect();
            currentHostConnection = null;
        }

        if (currentPicasso != null) {
            // Calling shutdown here causes a picasso error:
            // Handler (com.squareup.picasso.Stats$StatsHandler) {41b13d40} sending message to a Handler on a dead thread
            // Check: https://github.com/square/picasso/issues/445
            // So, for now, just let it be...
//            currentPicasso.shutdown();
            currentPicasso = null;
        }
    }

    // Check Kodi's version every 2 days
    private final static long KODI_VERSION_CHECK_INTERVAL_MILLIS = 2 * DateUtils.DAY_IN_MILLIS;

    /**
     * Periodic checks Kodi's version and updates the DB to reflect that.
     * This should be called somewhere that gets executed periodically
     *
     * @param hostInfo Host for which to check version
     */
    public void checkAndUpdateKodiVersion(final HostInfo hostInfo) {
        if (hostInfo == null) return;

        if (hostInfo.getUpdated() + KODI_VERSION_CHECK_INTERVAL_MILLIS < java.lang.System.currentTimeMillis()) {
            LogUtils.LOGD(TAG, "Checking Kodi version...");
            final HostConnection hostConnection = new HostConnection(hostInfo);
            final Application.GetProperties getProperties = new Application.GetProperties(Application.GetProperties.VERSION);
            getProperties.execute(hostConnection, new ApiCallback<ApplicationType.PropertyValue>() {
                @Override
                public void onSuccess(ApplicationType.PropertyValue result) {
                    LogUtils.LOGD(TAG, "Successfully checked Kodi version.");
                    hostInfo.setKodiVersionMajor(result.version.major);
                    hostInfo.setKodiVersionMinor(result.version.minor);
                    hostInfo.setKodiVersionRevision(result.version.revision);
                    hostInfo.setKodiVersionTag(result.version.tag);

                    editHost(hostInfo.getId(), hostInfo);

                    hostConnection.disconnect();
                }

                @Override
                public void onError(int errorCode, String description) {
                    // Couldn't get Kodi version... Ignore
                    LogUtils.LOGD(TAG, "Couldn't get Kodi version. Error: " + description);
                    hostConnection.disconnect();
                }
            }, new Handler());
        }
    }
}
