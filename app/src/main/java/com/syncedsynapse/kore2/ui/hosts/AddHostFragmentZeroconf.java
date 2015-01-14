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
package com.syncedsynapse.kore2.ui.hosts;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.NetUtils;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment that searchs foor XBMCs using Zeroconf
 */
public class AddHostFragmentZeroconf extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(AddHostFragmentZeroconf.class);

    // See http://sourceforge.net/p/xbmc/mailman/message/28667703/
    // _xbmc-jsonrpc-http._tcp
    // _xbmc-jsonrpc-h._tcp
    // _xbmc-jsonrpc-tcp._tcp
    // _xbmc-jsonrpc._tcp
    private static final String MDNS_XBMC_SERVICENAME = "_xbmc-jsonrpc-h._tcp.local.";
    private static final int DISCOVERY_TIMEOUT = 5000;

    /**
     * Callback interface to communicate with the enclosing activity
     */
    public interface AddHostZeroconfListener {
        public void onAddHostZeroconfNoHost();
        public void onAddHostZeroconfFoundHost(HostInfo hostInfo);
    }

    private AddHostZeroconfListener listener;

    @InjectView(R.id.search_host_title) TextView titleTextView;
    @InjectView(R.id.search_host_message) TextView messageTextView;
    @InjectView(R.id.next) Button nextButton;
    @InjectView(R.id.previous) Button previousButton;

    @InjectView(R.id.progress_bar) ProgressBar progressBar;
    @InjectView(R.id.list) GridView hostListGridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_host_zeroconf, container, false);
        ButterKnife.inject(this, root);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getView() == null)
            return;

        // Launch discovery thread
        startSearching();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (AddHostZeroconfListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement AddHostZeroconfListener interface.");
        }
    }

    // Whether the user cancelled the search
    private boolean searchCancelled = false;
    private final Object lock = new Object();

    /**
     * Starts the service discovery, setting up the UI accordingly
     */
    public void startSearching() {
        LogUtils.LOGD(TAG, "Starting service discovery...");
        searchCancelled = false;
        final Handler handler = new Handler();
        final Thread searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                WifiManager wifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
                WifiManager.MulticastLock multicastLock = null;
                try {
                    // Get wifi ip address
                    int wifiIpAddress = wifiManager.getConnectionInfo().getIpAddress();
                    InetAddress wifiInetAddress = NetUtils.intToInetAddress(wifiIpAddress);

                    // Acquire multicast lock
                    multicastLock = wifiManager.createMulticastLock("kore2.multicastlock");
                    multicastLock.setReferenceCounted(false);
                    multicastLock.acquire();

                    JmDNS jmDns = (wifiInetAddress != null)?
                                  JmDNS.create(wifiInetAddress) :
                                  JmDNS.create();

                    // Get the json rpc service list
                    final ServiceInfo[] serviceInfos =
                            jmDns.list(MDNS_XBMC_SERVICENAME, DISCOVERY_TIMEOUT);

                    synchronized (lock) {
                        // If the user didn't cancel the search, and we are sill in the activity
                        if (!searchCancelled && isAdded()) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if ((serviceInfos == null) || (serviceInfos.length == 0)) {
                                        noHostFound();
                                    } else {
                                        foundHosts(serviceInfos);
                                    }
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    LogUtils.LOGD(TAG, "Got an IO Exception", e);
                } finally {
                    if (multicastLock != null)
                        multicastLock.release();
                }
            }
        });

        titleTextView.setText(R.string.searching);
        messageTextView.setText(Html.fromHtml(getString(R.string.wizard_search_message)));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

        progressBar.setVisibility(View.VISIBLE);
        hostListGridView.setVisibility(View.GONE);

        // Setup buttons
        nextButton.setVisibility(View.INVISIBLE);
        previousButton.setVisibility(View.VISIBLE);
        previousButton.setText(android.R.string.cancel);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (lock) {
                    searchCancelled = true;
                    noHostFound();
                }
            }
        });

        searchThread.start();
    }

    /**
     * No host was found, present messages and buttons
     */
    public void noHostFound() {
        titleTextView.setText(R.string.no_xbmc_found);
        messageTextView.setText(Html.fromHtml(getString(R.string.wizard_search_no_host_found)));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

        progressBar.setVisibility(View.GONE);
        hostListGridView.setVisibility(View.GONE);

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText(R.string.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAddHostZeroconfNoHost();
            }
        });

        previousButton.setVisibility(View.VISIBLE);
        previousButton.setText(R.string.search_again);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearching();
            }
        });
    }

    /**
     * Found hosts, present them
     * @param serviceInfos Service infos found
     */
    public void foundHosts(final ServiceInfo[] serviceInfos) {
        LogUtils.LOGD(TAG, "Found hosts: " + serviceInfos.length);
        titleTextView.setText(R.string.xbmc_found);
        messageTextView.setText(Html.fromHtml(getString(R.string.wizard_search_host_found)));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText(R.string.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAddHostZeroconfNoHost();
            }
        });

        previousButton.setVisibility(View.VISIBLE);
        previousButton.setText(R.string.search_again);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearching();
            }
        });

        progressBar.setVisibility(View.GONE);
        hostListGridView.setVisibility(View.VISIBLE);

        HostListAdapter adapter = new HostListAdapter(getActivity(), R.layout.grid_item_host, serviceInfos);
        hostListGridView.setAdapter(adapter);
        hostListGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                ServiceInfo selectedServiceInfo = serviceInfos[position];

                String hostName = selectedServiceInfo.getName();
                String hostAddress = selectedServiceInfo.getHostAddresses()[0];
                int hostHttpPort = selectedServiceInfo.getPort();
                HostInfo selectedHostInfo = new HostInfo(hostName, hostAddress, hostHttpPort,
                        HostInfo.DEFAULT_TCP_PORT, null, null);

                listener.onAddHostZeroconfFoundHost(selectedHostInfo);
            }
        });

    }

    /**
     * Adapter used to show the hosts in the {@link GridView}
     */
    private class HostListAdapter extends ArrayAdapter<ServiceInfo> {
        public HostListAdapter(Context context, int resource, ServiceInfo[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                                            .inflate(R.layout.grid_item_host, parent, false);
            }

            final ServiceInfo item = this.getItem(position);
            ((TextView)convertView.findViewById(R.id.host_name)).setText(item.getName());
            String[] addresses = item.getHostAddresses();
            if (addresses.length > 0) {
                String hostAddress = addresses[0] + ":" + item.getPort();
                ((TextView) convertView.findViewById(R.id.host_address)).setText(hostAddress);
            }

            ImageView statusIndicator = (ImageView)convertView.findViewById(R.id.status_indicator);
            int statusColor = getActivity().getResources().getColor(R.color.host_status_available);
            statusIndicator.setColorFilter(statusColor);

            // Remove context menu
            ImageView contextMenu = (ImageView)convertView.findViewById(R.id.list_context_menu);
            contextMenu.setVisibility(View.GONE);

            return convertView;
        }
    }
}
