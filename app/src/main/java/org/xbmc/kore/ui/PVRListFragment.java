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
package org.xbmc.kore.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment that presents the movie list
 */
public class PVRListFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(PVRListFragment.class);

    public static final String CHANNELGROUPID = "channel_group_id";

    public interface OnPVRSelectedListener {
        public void onChannelGroupSelected(int channelGroupId, String channelGroupTitle);
        public void onChannelGuideSelected(int channelId, String channelTitle);
    }

    // Activity listener
    private OnPVRSelectedListener listenerActivity;

    private HostManager hostManager;

    @InjectView(R.id.list) GridView gridView;
    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(android.R.id.empty) TextView emptyView;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    private ChannelGroupAdapter channelGroupAdapter = null;
    private ChannelAdapter channelAdapter = null;

    private int selectedChannelGroupId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
        ButterKnife.inject(this, root);

        if (savedInstanceState != null) {
            selectedChannelGroupId = savedInstanceState.getInt(CHANNELGROUPID);
        }

        hostManager = HostManager.getInstance(getActivity());

        swipeRefreshLayout.setOnRefreshListener(this);

        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        gridView.setEmptyView(emptyView);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);

        if (selectedChannelGroupId == -1) {
            if ((channelGroupAdapter == null) ||
                (channelGroupAdapter.getCount() == 0))
                browseChannelGroups();
        } else {
            browseChannels(selectedChannelGroupId);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnPVRSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPVRSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CHANNELGROUPID, selectedChannelGroupId);
    }

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostManager.getHostInfo() != null) {
            if (selectedChannelGroupId == -1) {
                browseChannelGroups();
            } else {
                browseChannels(selectedChannelGroupId);
            }
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * Called by the enclosing activity
     */
    public void onBackPressed() {
        selectedChannelGroupId = -1;
        browseChannelGroups();
    }

    /**
     * Get the channel groups list and setup the gridview
     */
    private void browseChannelGroups() {
        // TODO: Make the channel type selectable
        LogUtils.LOGD(TAG, "Getting channel groups");
        PVR.GetChannelGroups action = new PVR.GetChannelGroups(PVRType.ChannelType.TV);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsChannelGroup>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsChannelGroup> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got channel groups");

                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_channel_groups_found_refresh));
                setupChannelGroupsGridview(result);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting channel groups: " + description);

                if (errorCode == ApiException.API_ERROR) {
                    emptyView.setText(String.format(getString(R.string.might_not_have_pvr), description));
                } else {
                    emptyView.setText(String.format(getString(R.string.error_getting_pvr_info), description));
                }
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.error_getting_pvr_info), description),
                               Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        }, callbackHandler);
    }

    /**
     * Called when we get the channel groups
     *
     * @param result ChannelGroups obtained
     */
    private void setupChannelGroupsGridview(List<PVRType.DetailsChannelGroup> result) {
        if (channelGroupAdapter == null) {
            channelGroupAdapter = new ChannelGroupAdapter(getActivity(), R.layout.grid_item_channel_group);
        }
        gridView.setAdapter(channelGroupAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the id from the tag
                ChannelGroupViewHolder tag = (ChannelGroupViewHolder) view.getTag();
                selectedChannelGroupId = tag.channelGroupId;
                // Notify the activity and show the channels
                listenerActivity.onChannelGroupSelected(tag.channelGroupId, tag.channelGroupName);
                browseChannels(tag.channelGroupId);
            }
        });

        channelGroupAdapter.clear();
        channelGroupAdapter.addAll(result);
        channelGroupAdapter.notifyDataSetChanged();
    }

    /**
     * Gets and displays the channels of a channelgroup
     * @param channelGroupId id
     */
    private void browseChannels(int channelGroupId) {
        String[] properties = PVRType.FieldsChannel.allValues;
        LogUtils.LOGD(TAG, "Getting channels");

        PVR.GetChannels action = new PVR.GetChannels(channelGroupId, properties);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsChannel>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsChannel> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got channels");

                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_channels_found_refresh));
                setupChannelsGridview(result);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting channels: " + description);

                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(String.format(getString(R.string.error_getting_pvr_info), description));
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.error_getting_pvr_info), description),
                               Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        }, callbackHandler);

    }

    /**
     * Called when we get the channels
     *
     * @param result Channels obtained
     */
    private void setupChannelsGridview(List<PVRType.DetailsChannel> result) {
        if (channelAdapter == null) {
            channelAdapter = new ChannelAdapter(getActivity(), R.layout.grid_item_channel);
        }
        gridView.setAdapter(channelAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the id from the tag
                ChannelViewHolder tag = (ChannelViewHolder) view.getTag();

                // Start the channel
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.channel_switching), tag.channelName),
                               Toast.LENGTH_SHORT).show();
                Player.Open action = new Player.Open(Player.Open.TYPE_CHANNEL, tag.channelId);
                action.execute(hostManager.getConnection(), new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        if (!isAdded()) return;
                        LogUtils.LOGD(TAG, "Started channel");
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        if (!isAdded()) return;
                        LogUtils.LOGD(TAG, "Error starting channel: " + description);

                        Toast.makeText(getActivity(),
                                       String.format(getString(R.string.error_starting_channel), description),
                                       Toast.LENGTH_SHORT).show();

                    }
                }, callbackHandler);

            }
        });

        channelAdapter.clear();
        channelAdapter.addAll(result);
        channelAdapter.notifyDataSetChanged();
    }

    private class ChannelGroupAdapter extends ArrayAdapter<PVRType.DetailsChannelGroup> {

        public ChannelGroupAdapter(Context context, int resource) {
            super(context, resource);
        }

        /** {@inheritDoc} */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                                            .inflate(R.layout.grid_item_channel_group, parent, false);

                // Setup View holder pattern
                ChannelGroupViewHolder viewHolder = new ChannelGroupViewHolder();
                viewHolder.titleView = (TextView)convertView.findViewById(R.id.title);
                convertView.setTag(viewHolder);
            }

            final ChannelGroupViewHolder viewHolder = (ChannelGroupViewHolder)convertView.getTag();
            PVRType.DetailsChannelGroup channelGroupDetails = this.getItem(position);

            viewHolder.channelGroupId = channelGroupDetails.channelgroupid;
            viewHolder.channelGroupName = channelGroupDetails.label;

            viewHolder.titleView.setText(viewHolder.channelGroupName);
            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class ChannelGroupViewHolder {
        TextView titleView;

        int channelGroupId;
        String channelGroupName;
    }

    private class ChannelAdapter extends ArrayAdapter<PVRType.DetailsChannel> {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public ChannelAdapter(Context context, int resource) {
            super(context, resource);
            this.hostManager = HostManager.getInstance(context);

            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.channellist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.channellist_art_heigth) /
                              UIUtils.IMAGE_RESIZE_FACTOR);
        }

        /** {@inheritDoc} */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(getActivity())
                        .inflate(R.layout.grid_item_channel, parent, false);

                // Setup View holder pattern
                ChannelViewHolder viewHolder = new ChannelViewHolder();
                viewHolder.titleView = (TextView)convertView.findViewById(R.id.title);
                viewHolder.detailsView = (TextView)convertView.findViewById(R.id.details);
                viewHolder.artView = (ImageView)convertView.findViewById(R.id.art);
                convertView.setTag(viewHolder);
            }

            final ChannelViewHolder viewHolder = (ChannelViewHolder)convertView.getTag();
            PVRType.DetailsChannel channelDetails = this.getItem(position);

            viewHolder.channelId = channelDetails.channelid;
            viewHolder.channelName = channelDetails.channel;

            viewHolder.titleView.setText(channelDetails.channel);
            String details = (channelDetails.broadcastnow != null)?
                    channelDetails.broadcastnow.title : null;
            viewHolder.detailsView.setText(details);
            UIUtils.loadImageWithCharacterAvatar(getContext(), hostManager,
                                                 channelDetails.thumbnail, channelDetails.channel,
                                                 viewHolder.artView, artWidth, artHeight);
            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class ChannelViewHolder {
        TextView titleView, detailsView;
        ImageView artView;

        int channelId;
        String channelName;
    }
}
