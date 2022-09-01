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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentHostListBinding;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.JSONRPC;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to manage the list of registered hosts.
 */
public class HostListFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(HostListFragment.class);

    private final ArrayList<HostInfoRow> hostInfoRows = new ArrayList<>();
    private HostListAdapter adapter = null;
    private Context context;
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private FragmentHostListBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHostListBinding.inflate(inflater, container, false);

        context = inflater.getContext();

        // Get the host list
        // TODO: This is being done synchronously !!!
        final HostManager hostManager = HostManager.getInstance(context);
        ArrayList<HostInfo> hosts = hostManager.getHosts();
        HostInfo currentHost = hostManager.getHostInfo();
        int currentHostPosition = 0;
        for (int i = 0; i < hosts.size(); i++) {
            if ((currentHost != null) && (hosts.get(i).getId() == currentHost.getId())) {
                currentHostPosition = i;
            }
            hostInfoRows.add(new HostInfoRow(hosts.get(i), HostInfoRow.HOST_STATUS_CONNECTING));
        }

        // Setup the adapter
        binding.list.setEmptyView(binding.empty);
        adapter = new HostListAdapter(context, R.layout.item_host, hostInfoRows);
        binding.list.setAdapter(adapter);
        binding.list.setOnItemClickListener((parent, view, position, itemId) -> {
            HostInfoRow clickedHostRow = hostInfoRows.get(position);

            // Set the clicked host active
            hostManager.switchHost(clickedHostRow.hostInfo);
            Intent intent = new Intent(context, RemoteActivity.class);
            context.startActivity(intent);
        });
        binding.fabAddHost.setOnClickListener(this::onAddHostClicked);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Launch check on each host
        for (HostInfoRow hostInfoRow : hostInfoRows ) {
            updateHostStatus(hostInfoRow);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Pings the host to checks it status.
     * <br/>
     * Depending on whether it gets a response or not, updates {@link HostInfoRow} status, and
     * notifies the adapter of the change
     *
     * @param hostInfoRow Host to check
     */
    private void updateHostStatus(final HostInfoRow hostInfoRow) {
        final HostConnection hostConnection = new HostConnection(hostInfoRow.hostInfo);
        JSONRPC.Ping ping = new JSONRPC.Ping();
        ping.execute(hostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                hostInfoRow.status = HostInfoRow.HOST_STATUS_AVAILABLE;
                hostConnection.disconnect();
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(int errorCode, String description) {
                hostInfoRow.status = HostInfoRow.HOST_STATUS_UNAVAILABLE;
                hostConnection.disconnect();
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }
        }, callbackHandler);
    }

    public void onAddHostClicked(View v) {
        startAddHostWizard();
    }

    /**
     * Starts add host activity
     */
    public void startAddHostWizard() {
        Intent launchIntent = new Intent(getActivity(), AddHostActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launchIntent);
		requireActivity().overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
    }

    /**
     * Auxiliary class to represent a host row in the list view
     */
    private static class HostInfoRow {

        public HostInfo hostInfo;

        public static final int HOST_STATUS_CONNECTING = 0,
                HOST_STATUS_AVAILABLE = 1,
                HOST_STATUS_UNAVAILABLE = 2;
        /** Host status */
        public int status;

        public HostInfoRow(HostInfo host, int status) {
            this.hostInfo = host;
            this.status = status;
        }
    }

    private final View.OnClickListener hostlistItemMenuClickListener = v -> {
        final HostInfo hostInfo = (HostInfo)v.getTag();

        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.hostlist_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_remove_host) {
                DialogFragment confirmDelete = ConfirmDeleteDialogFragment
                        .getInstance(getDeleteDialogListener(hostInfo.getId()));
                confirmDelete.show(getParentFragmentManager(), "confirmdelete");
                return true;
            } else if (itemId == R.id.action_edit_host) {
                Intent launchIntent = new Intent(getActivity(), EditHostActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(HostFragmentManualConfiguration.HOST_ID, hostInfo.getId());
                startActivity(launchIntent);
                requireActivity().overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
                return true;
            } else if (itemId == R.id.action_wake_up) {
                // Send WoL magic packet on a new thread
                UIUtils.sendWolAsync(getActivity(), hostInfo);
                return true;
            }
            return false;
        });
        popupMenu.show();
    };

    /**
     * Adapter used to show the hosts in the {@link GridView}
     */
    private class HostListAdapter extends ArrayAdapter<HostInfoRow> {
        private final int kodiStatusConnectingColor, kodiStatusConnectedColor, kodiStatusUnavailableColor;
        public HostListAdapter(Context context, int resource, List<HostInfoRow> objects) {
            super(context, resource, objects);

            kodiStatusConnectingColor = MaterialColors.getColor(requireContext(), R.attr.kodiStatusConnecting, null);
            kodiStatusConnectedColor = MaterialColors.getColor(requireContext(), R.attr.kodiStatusConnected, null);
            kodiStatusUnavailableColor = MaterialColors.getColor(requireContext(), R.attr.kodiStatusUnavailable, null);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.item_host, parent, false);
            }

            final HostInfoRow item = this.getItem(position);
            ((TextView)convertView.findViewById(R.id.host_name)).setText(item.hostInfo.getName());
            String hostInfo = item.hostInfo.getAddress() + ":" + item.hostInfo.getHttpPort() + " - " +
                              item.hostInfo.getKodiVersionDesc();
            ((TextView)convertView.findViewById(R.id.host_info)).setText(hostInfo);

            ImageView statusIndicator = convertView.findViewById(R.id.status_indicator);
            int statusColor, statusDescription;
            switch (item.status) {
                case HostInfoRow.HOST_STATUS_CONNECTING:
                    statusColor = kodiStatusConnectingColor;
                    statusDescription = R.string.connecting;
                    break;
                case HostInfoRow.HOST_STATUS_UNAVAILABLE:
                    statusColor = kodiStatusUnavailableColor;
                    statusDescription = R.string.not_connected;
                    break;
                case HostInfoRow.HOST_STATUS_AVAILABLE:
                    statusColor = kodiStatusConnectedColor;
                    statusDescription = R.string.connected_to_xbmc;
                    break;
                default:
                    throw new RuntimeException("Invalid host status");
            }
            statusIndicator.setColorFilter(statusColor);

            TextView hostConnectionStatus = convertView.findViewById(R.id.host_connection_status);
            hostConnectionStatus.setText(statusDescription);
            hostConnectionStatus.setTextColor(statusColor);

            // For the popupmenu
            ImageView contextMenu = convertView.findViewById(R.id.list_context_menu);
            contextMenu.setTag(item.hostInfo);
            contextMenu.setOnClickListener(hostlistItemMenuClickListener);

            return convertView;
        }
    }

    private ConfirmDeleteDialogFragment.ConfirmDeleteDialogListener getDeleteDialogListener(final int hostId) {

        return new ConfirmDeleteDialogFragment.ConfirmDeleteDialogListener() {
            @Override
            public void onDialogPositiveClick() {
                HostManager hostManager = HostManager.getInstance(requireContext());
                hostManager.deleteHost(hostId);

                for (int i = 0; i < hostInfoRows.size(); i++) {
                    if (hostInfoRows.get(i).hostInfo.getId() == hostId) {
                        hostInfoRows.remove(i);
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onDialogNegativeClick() {
                // Do nothing
            }
        };
    }
    /**
     * Confirm host delete fragment
     */
    public static class ConfirmDeleteDialogFragment extends DialogFragment {

        /** Interface for communication with the enclosing fragment */
        public interface ConfirmDeleteDialogListener {
            void onDialogPositiveClick();
            void onDialogNegativeClick();
        }

        private ConfirmDeleteDialogListener mListener;

        public static ConfirmDeleteDialogFragment getInstance(ConfirmDeleteDialogListener listener) {
            ConfirmDeleteDialogFragment frag = new ConfirmDeleteDialogFragment();
            frag.mListener = listener;
            return frag;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.delete_xbmc)
                    .setMessage(R.string.delete_xbmc_confirm)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> mListener.onDialogPositiveClick())
                    .setNegativeButton(android.R.string.cancel, (dialog, id) -> mListener.onDialogNegativeClick())
                    .create();
        }
    }
}

