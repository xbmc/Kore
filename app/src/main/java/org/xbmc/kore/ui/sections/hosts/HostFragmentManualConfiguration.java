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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentAddHostManualConfigurationBinding;
import org.xbmc.kore.eventclient.EventServerConnection;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.method.JSONRPC;
import org.xbmc.kore.jsonrpc.type.ApplicationType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.NetUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fragment that presents the welcome message
 */
public class HostFragmentManualConfiguration extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(HostFragmentManualConfiguration.class);

    /**
     * Fragment arguments
     */
    private static final String PREFIX = "org.xbmc.kore";
    public static final String HOST_ID = PREFIX + ".host_id",
            HOST_NAME = PREFIX + ".host_name",
            HOST_ADDRESS = PREFIX + ".host_address",
            HOST_HTTP_PORT = PREFIX + ".host_http_port",
            HOST_TCP_PORT = PREFIX + ".host_tcp_post",
            HOST_USERNAME = PREFIX + ".host_username",
            HOST_PASSWORD = PREFIX + ".host_password",
            HOST_MAC_ADDRESS = PREFIX + ".host_mac_address",
            HOST_WOL_PORT = PREFIX + ".host_wol_port",
            HOST_DIRECT_SHARE = PREFIX + ".host_direct_share",
            HOST_PROTOCOL = PREFIX + ".host_protocol",
            HOST_USE_EVENT_SERVER = PREFIX + ".host_use_event_server",
            HOST_EVENT_SERVER_PORT = PREFIX + ".host_event_server_port";
    public static final String GO_STRAIGHT_TO_TEST = PREFIX + ".go_straight_to_test";

    /**
     * Callback interface to communicate with the encolsing activity
     */
    public interface HostManualConfigurationListener {
        void onHostManualConfigurationNext(HostInfo hostInfo);
        void onHostManualConfigurationCancel();
    }

    public static String CANCEL_BUTTON_LABEL_ARG = PREFIX + ".cancel_button_label";
    private HostManualConfigurationListener listener;
    private ProgressDialog progressDialog;

    private FragmentAddHostManualConfigurationBinding binding;

    // Handler for callbacks
    final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddHostManualConfigurationBinding.inflate(inflater, container, false);
       // By default, use TCP
        binding.kodiUseTcp.setChecked(true);
        binding.kodiUseTcp.setOnCheckedChangeListener((buttonView, isChecked) -> binding.kodiTcpPort.setEnabled(isChecked));

        binding.kodiUseEventServer.setChecked(true);
        binding.kodiUseEventServer.setOnCheckedChangeListener((buttonView, isChecked) -> binding.kodiEventServerPort.setEnabled(isChecked));

        // Check if we were given a host info
        assert getArguments() != null;
        String hostName = getArguments().getString(HOST_NAME);
        String hostAddress = getArguments().getString(HOST_ADDRESS);
        int hostHttpPort = getArguments().getInt(HOST_HTTP_PORT, HostInfo.DEFAULT_HTTP_PORT);
        int hostTcpPort = getArguments().getInt(HOST_TCP_PORT, HostInfo.DEFAULT_TCP_PORT);
        String hostUsername = getArguments().getString(HOST_USERNAME);
        String hostPassword = getArguments().getString(HOST_PASSWORD);
        int hostProtocol = getArguments().getInt(HOST_PROTOCOL, HostConnection.PROTOCOL_TCP);
        String hostMacAddress = getArguments().getString(HOST_MAC_ADDRESS);
        int hostWolPort = getArguments().getInt(HOST_WOL_PORT, HostInfo.DEFAULT_WOL_PORT);
        boolean directShare = getArguments().getBoolean(HOST_DIRECT_SHARE, true);
        boolean hostUseEventServer = getArguments().getBoolean(HOST_USE_EVENT_SERVER, true);
        int hostEventServerPort = getArguments().getInt(HOST_EVENT_SERVER_PORT, HostInfo.DEFAULT_EVENT_SERVER_PORT);

        if (hostAddress != null) {
            binding.kodiName.setText(hostName);
            binding.kodiIpAddress.setText(hostAddress);
            binding.kodiHttpPort.setText(String.valueOf(hostHttpPort));
            if (!TextUtils.isEmpty(hostUsername))
                binding.kodiUsername.setText(hostUsername);
            if (!TextUtils.isEmpty(hostPassword))
                binding.kodiPassword.setText(hostPassword);

            binding.kodiUseTcp.setChecked(!(hostProtocol == HostConnection.PROTOCOL_HTTP));
            binding.kodiTcpPort.setEnabled(binding.kodiUseTcp.isChecked());

            if (hostTcpPort != HostInfo.DEFAULT_TCP_PORT)
                binding.kodiTcpPort.setText(String.valueOf(hostTcpPort));
            if (!TextUtils.isEmpty(hostMacAddress))
                binding.kodiMacAddress.setText(hostMacAddress);
            if (hostWolPort != HostInfo.DEFAULT_WOL_PORT)
                binding.kodiWolPort.setText(String.valueOf(hostWolPort));

            binding.kodiDirectShare.setChecked(directShare);

            binding.kodiUseEventServer.setChecked(hostUseEventServer);
            binding.kodiEventServerPort.setEnabled(binding.kodiUseEventServer.isChecked());
            if (hostEventServerPort != HostInfo.DEFAULT_EVENT_SERVER_PORT)
                binding.kodiEventServerPort.setText(String.valueOf(hostEventServerPort));
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressDialog = new ProgressDialog(requireContext());

        // Next button
        binding.includeWizardButtonBar.next.setText(R.string.test_connection);
        binding.includeWizardButtonBar.next.setOnClickListener(v -> testConnection());

        // Previous button
        if (getArguments() != null && getArguments().getString(CANCEL_BUTTON_LABEL_ARG, null) != null) {
            binding.includeWizardButtonBar.previous.setText(getArguments().getString(CANCEL_BUTTON_LABEL_ARG));
        } else {
            binding.includeWizardButtonBar.previous.setText(android.R.string.cancel);
        }
        binding.includeWizardButtonBar.previous.setOnClickListener(v -> listener.onHostManualConfigurationCancel());

        // Check if the activity wants us to go straight to test
        boolean goStraighToTest = getArguments() != null && getArguments().getBoolean(GO_STRAIGHT_TO_TEST, false);
        if (goStraighToTest) {
            testConnection();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (HostManualConfigurationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement AddHostManualConfigurationListener interface.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    private void showMessage(@StringRes int messageResId) {
        if (getView() == null) return;
        UIUtils.showSnackbar(getView(), messageResId);
    }

    /**
     * Tests a connection with the values set in the UI.
     * Checks whether the values are correctly set, and then tries to make
     * a ping call. First through HTTP, and if it succeeds, through TCP to
     * check availability. Finally adds the host and advances the wizard
     */
    private void testConnection() {
        String kodiName = binding.kodiName.getText().toString();

        boolean isHttps = false;
        String kodiAddress = binding.kodiIpAddress.getText().toString().trim();
        if (kodiAddress.startsWith("https://")) {
            kodiAddress = kodiAddress.substring("https://".length());
            LogUtils.LOGD(TAG, "Stripped https:// on address to get: " + kodiAddress);
            isHttps = true;
        } else if (kodiAddress.startsWith("http://")) {
            kodiAddress = kodiAddress.substring("http://".length());
            LogUtils.LOGD(TAG, "Stripped http:// on address to get: " + kodiAddress);
        }
        int kodiHttpPort = isHttps ? HostInfo.DEFAULT_HTTPS_PORT : HostInfo.DEFAULT_HTTP_PORT;

        Integer implicitPort = null;
        Matcher m = Pattern.compile("^.*:(\\d{1,5})\\z").matcher(kodiAddress);
        if (m.matches()) {
            // Minus one character for the colon
            kodiAddress = kodiAddress.substring(0, m.start(1) - 1);
            LogUtils.LOGD(TAG, "Stripped port on address to get: " + kodiAddress);
            try {
                implicitPort = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                LogUtils.LOGW(TAG, "Value matching port regex couldn't be parsed as integer: " + m.group(1));
                implicitPort = -1;
            }
        }

        Integer explicitPort = null;
        String aux = binding.kodiHttpPort.getText().toString();
        if (!TextUtils.isEmpty(aux)) {
            try {
                explicitPort = Integer.valueOf(aux);
            } catch (NumberFormatException e) {
                explicitPort = -1;
            }
        }

        if (implicitPort != null) {
            if (!isValidPort(implicitPort)) {
                showMessage(R.string.wizard_invalid_http_port_specified);
                binding.kodiHttpPort.requestFocus();
                return;
            }
            kodiHttpPort = implicitPort;
        } else if (explicitPort != null) {
            if (!isValidPort(explicitPort)) {
                showMessage(R.string.wizard_invalid_http_port_specified);
                binding.kodiHttpPort.requestFocus();
                return;
            }
            kodiHttpPort = explicitPort;
        }

        String kodiUsername = binding.kodiUsername.getText().toString();
        String kodiPassword = binding.kodiPassword.getText().toString();
        aux = binding.kodiTcpPort.getText().toString();
        int kodiTcpPort;
        try {
            kodiTcpPort = TextUtils.isEmpty(aux) ? HostInfo.DEFAULT_TCP_PORT : Integer.parseInt(aux);
        } catch (NumberFormatException exc) {
            kodiTcpPort = -1;
        }

        int kodiProtocol = binding.kodiUseTcp.isChecked()? HostConnection.PROTOCOL_TCP : HostConnection.PROTOCOL_HTTP;

        String macAddress = binding.kodiMacAddress.getText().toString();
        aux = binding.kodiWolPort.getText().toString();

        int kodiWolPort = HostInfo.DEFAULT_WOL_PORT;
        try {
            kodiWolPort = TextUtils.isEmpty(aux) ? HostInfo.DEFAULT_WOL_PORT : Integer.parseInt(aux);
        } catch (NumberFormatException exc) {
            // Ignoring this exception and keeping WoL port at the default value
        }

        boolean kodiUseEventServer = binding.kodiUseEventServer.isChecked();
        aux = binding.kodiEventServerPort.getText().toString();
        int kodiEventServerPort;
        try {
            kodiEventServerPort = TextUtils.isEmpty(aux) ? HostInfo.DEFAULT_EVENT_SERVER_PORT : Integer.parseInt(aux);
        } catch (NumberFormatException exc) {
            kodiEventServerPort = -1;
        }

        // Check Kodi name and address
        if (TextUtils.isEmpty(kodiName)) {
            showMessage(R.string.wizard_no_name_specified);
            binding.kodiName.requestFocus();
            return;
        } else if (TextUtils.isEmpty(kodiAddress)) {
            showMessage(R.string.wizard_no_address_specified);
            binding.kodiIpAddress.requestFocus();
            return;
        } else if (kodiTcpPort <= 0) {
            showMessage(R.string.wizard_invalid_tcp_port_specified);
            binding.kodiTcpPort.requestFocus();
            return;
        } else if (kodiEventServerPort <= 0) {
            showMessage(R.string.wizard_invalid_tcp_port_specified);
            binding.kodiEventServerPort.requestFocus();
            return;
        }

        // If username or password empty, set it to null
        if (TextUtils.isEmpty(kodiUsername))
            kodiUsername = null;
        if (TextUtils.isEmpty(kodiPassword))
            kodiPassword = null;

        // Ok, let's try to ping the host
        final HostInfo checkedHostInfo = new HostInfo(kodiName, kodiAddress, kodiProtocol,
                                                      kodiHttpPort, kodiTcpPort,
                                                      kodiUsername, kodiPassword,
                                                      kodiUseEventServer, kodiEventServerPort, isHttps,
                                true);
        checkedHostInfo.setMacAddress(macAddress);
        checkedHostInfo.setWolPort(kodiWolPort);
        checkedHostInfo.setShowAsDirectShareTarget(binding.kodiDirectShare.isChecked());

        progressDialog.setTitle(String.format(getResources().getString(R.string.wizard_connecting_to_xbmc_title), kodiName));
        progressDialog.setMessage(getResources().getString(R.string.wizard_connecting_to_xbmc_message));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setOnShowListener(dialog -> {
            // Let's ping the host through HTTP
            chainCallCheckHttpConnection(checkedHostInfo);
        });
        progressDialog.show();
    }

    private void chainCallCheckHttpConnection(final HostInfo hostInfo) {
        // Let's ping the host through HTTP
        final HostConnection hostConnection = new HostConnection(hostInfo);
        hostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);
        final JSONRPC.Ping httpPing = new JSONRPC.Ping();
        httpPing.execute(hostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                LogUtils.LOGD(TAG, "Successfully connected to new host through HTTP.");
                // Great, we managed to connect through HTTP, let's check through tcp
                if (hostInfo.getProtocol() == HostConnection.PROTOCOL_TCP) {
                    chainCallCheckTcpConnection(hostConnection, hostInfo);
                } else {
                    // No TCP, check EventServer
                    hostConnection.disconnect();
                    chainCallCheckEventServerConnection(hostInfo);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                // Couldn't connect through HTTP, abort, and initialize checkedHostInfo
                hostConnection.disconnect();
                hostConnectionError(errorCode, description);
            }
        }, handler);
    }

    private void chainCallCheckTcpConnection(final HostConnection hostConnection, final HostInfo hostInfo) {
        final JSONRPC.Ping tcpPing = new JSONRPC.Ping();
        hostConnection.setProtocol(HostConnection.PROTOCOL_TCP);
        tcpPing.execute(hostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Great, we managed to connect through HTTP and TCP
                LogUtils.LOGD(TAG, "Successfully connected to new host through TCP.");
                hostConnection.disconnect();
                // Check EventServer
                chainCallCheckEventServerConnection(hostInfo);
            }

            @Override
            public void onError(int errorCode, String description) {
                // We only managed to connect through HTTP, revert checkedHostInfo to use HTTP
                LogUtils.LOGD(TAG, "Couldn't connect to host through TCP. Message: " + description);
                hostConnection.disconnect();
                hostInfo.setProtocol(HostConnection.PROTOCOL_HTTP);
                // Check EventServer
                chainCallCheckEventServerConnection(hostInfo);
            }
        }, handler);
    }

    private void chainCallCheckEventServerConnection(final HostInfo hostInfo) {
        if (hostInfo.getUseEventServer()) {
            EventServerConnection.testEventServerConnection(
                    hostInfo,
                    success -> {
                        LogUtils.LOGD(TAG, "Check ES connection: " + success);
                        if (!success) {
                            hostInfo.setUseEventServer(false);
                        }
                        chainCallCheckKodiVersion(hostInfo);
                    },
                    handler);
        } else {
            chainCallCheckKodiVersion(hostInfo);
        }
    }

    private void chainCallCheckKodiVersion(final HostInfo hostInfo) {
        final HostConnection hostConnection = new HostConnection(hostInfo);
        hostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        final Application.GetProperties getProperties = new Application.GetProperties(Application.GetProperties.VERSION);
        getProperties.execute(hostConnection, new ApiCallback<ApplicationType.PropertyValue>() {
            @Override
            public void onSuccess(ApplicationType.PropertyValue result) {
                LogUtils.LOGD(TAG, "Successfully checked Kodi version.");
                hostInfo.setKodiVersionMajor(result.version.major);
                hostInfo.setKodiVersionMinor(result.version.minor);
                hostInfo.setKodiVersionRevision(result.version.revision);
                hostInfo.setKodiVersionTag(result.version.tag);

                hostConnection.disconnect();
                hostConnectionChecked(hostInfo);
            }

            @Override
            public void onError(int errorCode, String description) {
                // Couldn't get Kodi version... Odd, but let's proceed anyway with the defaults
                hostConnection.disconnect();
                hostConnectionChecked(hostInfo);
            }
        }, handler);
    }

    /**
     * The connection was checked, and hostInfo has all the correct parameters to communicate
     * with it
     * @param hostInfo {@link HostInfo} to add
     */
    private void hostConnectionChecked(final HostInfo hostInfo) {
        // Let's get the MAC Address, if we don't have one
        if (TextUtils.isEmpty(hostInfo.getMacAddress())) {
            new Thread(() -> {
                String localMacAddress = NetUtils.getMacAddress(hostInfo.getAddress());
                hostInfo.setMacAddress(localMacAddress);
                handler.post(() -> {
                    if (isAdded()) {
                        progressDialog.dismiss();
                        listener.onHostManualConfigurationNext(hostInfo);
                    }
                });
            }).start();
        } else {
            // Mac address was supplied
            if (isAdded()) {
                progressDialog.dismiss();
                listener.onHostManualConfigurationNext(hostInfo);
            }
        }
    }

    /**
     * Treats errors occurred during the connection check
     * @param errorCode Error code
     * @param description Description
     */
    private void hostConnectionError(int errorCode, String description) {
        if (!isAdded()) return;

        progressDialog.dismiss();
        LogUtils.LOGD(TAG, "An error occurred during connection testint. Message: " + description);
        switch (errorCode) {
            case ApiException.HTTP_RESPONSE_CODE_UNAUTHORIZED:
                String username = binding.kodiUsername.getText().toString(),
                        password = binding.kodiPassword.getText().toString();
                int messageResourceId;
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    messageResourceId = R.string.wizard_empty_authentication;
                } else {
                    messageResourceId = R.string.wizard_incorrect_authentication;
                }
                showMessage(messageResourceId);
                binding.kodiUsername.requestFocus();
                break;
            default:
                showMessage(R.string.wizard_error_connecting);
                break;
        }
    }
}
