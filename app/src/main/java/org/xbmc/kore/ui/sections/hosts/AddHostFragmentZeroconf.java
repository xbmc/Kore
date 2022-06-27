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
package org.xbmc.kore.ui.sections.hosts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentAddHostZeroconfBinding;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.NetUtils;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

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
        void onAddHostZeroconfNoHost();
        void onAddHostZeroconfFoundHost(HostInfo hostInfo);
    }

    private AddHostZeroconfListener listener;
    private FragmentAddHostZeroconfBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddHostZeroconfBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Launch discovery thread
        startSearching();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddHostZeroconfListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement AddHostZeroconfListener interface.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Whether the user cancelled the search
    private boolean searchCancelled = false;
    private final Object lock = new Object();

    /**
     * Starts the service discovery, setting up the UI accordingly
     */
    public void startSearching() {
        if(!isNetworkConnected()) {
            noNetworkConnection();
            return;
        }

        LogUtils.LOGD(TAG, "Starting service discovery...");
        searchCancelled = false;
        final Handler handler = new Handler();
        final Thread searchThread = new Thread(() -> {
            WifiManager wifiManager = (WifiManager)requireActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

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
                        handler.post(() -> {
                            if ((serviceInfos == null) || (serviceInfos.length == 0)) {
                                noHostFound();
                            } else {
                                foundHosts(serviceInfos);
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
        });

        binding.searchHostTitle.setText(R.string.searching);
        binding.searchHostMessage.setText(Html.fromHtml(getString(R.string.wizard_search_message)));
        binding.searchHostMessage.setMovementMethod(LinkMovementMethod.getInstance());

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.list.setVisibility(View.GONE);

        // Setup buttons
        binding.includeWizardButtonBar.next.setVisibility(View.INVISIBLE);
        binding.includeWizardButtonBar.previous.setVisibility(View.VISIBLE);
        binding.includeWizardButtonBar.previous.setText(android.R.string.cancel);
        binding.includeWizardButtonBar.previous.setOnClickListener(v -> {
            synchronized (lock) {
                searchCancelled = true;
                noHostFound();
            }
        });

        searchThread.start();
    }

    /**
     * No host was found, present messages and buttons
     */
    public void noHostFound() {
        if (!isAdded()) return;

        binding.searchHostTitle.setText(R.string.no_xbmc_found);
        binding.searchHostMessage.setText(Html.fromHtml(getString(R.string.wizard_search_no_host_found)));
        binding.searchHostMessage.setMovementMethod(LinkMovementMethod.getInstance());

        binding.progressBar.setVisibility(View.GONE);
        binding.list.setVisibility(View.GONE);

        binding.includeWizardButtonBar.next.setVisibility(View.VISIBLE);
        binding.includeWizardButtonBar.next.setText(R.string.next);
        binding.includeWizardButtonBar.next.setOnClickListener(v -> listener.onAddHostZeroconfNoHost());

        binding.includeWizardButtonBar.previous.setVisibility(View.VISIBLE);
        binding.includeWizardButtonBar.previous.setText(R.string.search_again);
        binding.includeWizardButtonBar.previous.setOnClickListener(v -> startSearching());
    }

    /**
     * Found hosts, present them
     * @param serviceInfos Service infos found
     */
    public void foundHosts(final ServiceInfo[] serviceInfos) {
        if (!isAdded()) return;

        LogUtils.LOGD(TAG, "Found hosts: " + serviceInfos.length);
        binding.searchHostTitle.setText(R.string.xbmc_found);
        binding.searchHostMessage.setText(Html.fromHtml(getString(R.string.wizard_search_host_found)));
        binding.searchHostMessage.setMovementMethod(LinkMovementMethod.getInstance());

        binding.includeWizardButtonBar.next.setVisibility(View.VISIBLE);
        binding.includeWizardButtonBar.next.setText(R.string.next);
        binding.includeWizardButtonBar.next.setOnClickListener(v -> listener.onAddHostZeroconfNoHost());

        binding.includeWizardButtonBar.previous.setVisibility(View.VISIBLE);
        binding.includeWizardButtonBar.previous.setText(R.string.search_again);
        binding.includeWizardButtonBar.previous.setOnClickListener(v -> startSearching());

        binding.progressBar.setVisibility(View.GONE);
        binding.list.setVisibility(View.VISIBLE);

        HostListAdapter adapter = new HostListAdapter(getActivity(), R.layout.grid_item_host, serviceInfos);
        binding.list.setAdapter(adapter);
        binding.list.setOnItemClickListener((parent, view, position, itemId) -> {
            ServiceInfo selectedServiceInfo = serviceInfos[position];

            String[] addresses = selectedServiceInfo.getHostAddresses();
            if (addresses.length == 0) {
                // Couldn't get any address
                Toast.makeText(getActivity(), R.string.wizard_zeroconf_cant_connect_no_host_address, Toast.LENGTH_LONG)
                        .show();
                return;
            }
            String hostName = selectedServiceInfo.getName();
            String hostAddress = addresses[0];
            int hostHttpPort = selectedServiceInfo.getPort();
            HostInfo selectedHostInfo = new HostInfo(hostName, hostAddress, HostConnection.PROTOCOL_TCP,
                    hostHttpPort, HostInfo.DEFAULT_TCP_PORT, null, null, true, HostInfo.DEFAULT_EVENT_SERVER_PORT, false, true);

            listener.onAddHostZeroconfFoundHost(selectedHostInfo);
        });

    }

    private void noNetworkConnection() {
        binding.searchHostTitle.setText(R.string.no_network_connection);
        binding.searchHostMessage.setText(Html.fromHtml(getString(R.string.wizard_search_no_network_connection)));
        binding.searchHostMessage.setMovementMethod(LinkMovementMethod.getInstance());

        binding.progressBar.setVisibility(View.GONE);
        binding.list.setVisibility(View.GONE);

        binding.includeWizardButtonBar.next.setVisibility(View.GONE);

        binding.includeWizardButtonBar.previous.setVisibility(View.VISIBLE);
        binding.includeWizardButtonBar.previous.setText(R.string.search_again);
        binding.includeWizardButtonBar.previous.setOnClickListener(v -> startSearching());
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
            String hostAddress;
            if (addresses.length > 0) {
                hostAddress = addresses[0] + ":" + item.getPort();
            } else {
                hostAddress = getString(R.string.wizard_zeroconf_no_host_address);
            }
            ((TextView) convertView.findViewById(R.id.host_address)).setText(hostAddress);

            ImageView statusIndicator = (ImageView)convertView.findViewById(R.id.status_indicator);
            int statusColor = requireActivity().getResources().getColor(R.color.host_status_available);
            statusIndicator.setColorFilter(statusColor);

            // Remove context menu
            ImageView contextMenu = (ImageView)convertView.findViewById(R.id.list_context_menu);
            contextMenu.setVisibility(View.GONE);

            return convertView;
        }
    }
}
