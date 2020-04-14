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
package org.xbmc.kore.ui.sections.video;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentPvrListBinding;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.ui.AbstractSearchableFragment;
import org.xbmc.kore.ui.OnBackPressedListener;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that presents the movie list
 */
public class PVRChannelsListFragment extends AbstractSearchableFragment
        implements SwipeRefreshLayout.OnRefreshListener, OnBackPressedListener {
    private static final String TAG = LogUtils.makeLogTag(PVRChannelsListFragment.class);

    public static final String CHANNELGROUPID = "channelgroupid";
    public static final String SINGLECHANNELGROUP = "singlechannelgroup";

    public interface OnPVRChannelSelectedListener {
        void onChannelGuideSelected(int channelId, String channelTitle, boolean singleChannelGroup);
        void onChannelGroupSelected(int channelGroupId, String channelGroupTitle);
    }

    // Activity listener
    private OnPVRChannelSelectedListener listenerActivity;

    private HostManager hostManager;

    //@BindView(android.R.id.empty) TextView emptyView;*/

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    private ChannelGroupAdapter channelGroupAdapter = null;
    private ChannelAdapter channelAdapter = null;

    private int selectedChannelGroupId = -1;
    private int currentListType;
    private boolean singleChannelGroup = false;

    private FragmentPvrListBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPvrListBinding.inflate(inflater, container, false);

        if (savedInstanceState != null) {
            selectedChannelGroupId = savedInstanceState.getInt(CHANNELGROUPID);
            singleChannelGroup = savedInstanceState.getBoolean(SINGLECHANNELGROUP);
        }

        hostManager = HostManager.getInstance(getActivity());

        currentListType = getArguments().getInt(PVRListFragment.PVR_LIST_TYPE_KEY, PVRListFragment.LIST_TV_CHANNELS);

        binding.swipeRefreshLayout.setOnRefreshListener(this);

        // TODO: emptyView in ViewBinding
        //emptyView.setOnClickListener(v -> onRefresh());
        binding.list.setEmptyView(binding.list.getEmptyView());

        super.onCreateView(inflater, container, savedInstanceState);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setSupportsSearch(true);

        if (selectedChannelGroupId == -1) {
            if ((channelGroupAdapter == null) ||
                (channelGroupAdapter.getCount() == 0))
                browseChannelGroups();
        } else {
            browseChannels(selectedChannelGroupId);
        }
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnPVRChannelSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPVRChannelSelectedListener");
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
    public void onSaveInstanceState (@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CHANNELGROUPID, selectedChannelGroupId);
        outState.putBoolean(SINGLECHANNELGROUP, singleChannelGroup);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    protected void refreshList() {
        onRefresh();
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
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * Called by the viewpager fragment
     *
     * @return True if back eas handled, false if it wasn't
     */
    public boolean onBackPressed() {
        if (!singleChannelGroup && (selectedChannelGroupId != -1)) {
            selectedChannelGroupId = -1;
            browseChannelGroups();
            return true;
        }
        return false;
    }

    /**
     * Get the channel groups list and setup the gridview
     */
    private void browseChannelGroups() {
        LogUtils.LOGD(TAG, "Getting channel groups");
        String channelType = (currentListType == PVRListFragment.LIST_TV_CHANNELS)?
                PVRType.ChannelType.TV : PVRType.ChannelType.RADIO;
        PVR.GetChannelGroups action = new PVR.GetChannelGroups(channelType);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsChannelGroup>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsChannelGroup> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got channel groups");

                if (result.size() == 1) {
                    // Single channel group, go directly to channel list
                    singleChannelGroup = true;
                    selectedChannelGroupId = result.get(0).channelgroupid;
                    browseChannels(selectedChannelGroupId);
                } else {
                    // To prevent the empty text from appearing on the first load, set it now
                    // TODO: ersetzen  emptyView für ViewBinding
                    //emptyView.setText(getString(R.string.no_channel_groups_found_refresh));
                    setupChannelGroupsGridview(result);
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting channel groups: " + description);

                if (errorCode == ApiException.API_ERROR) {
                    // TODO: ersetzen  emptyView für ViewBinding
                    //emptyView.setText(getString(R.string.might_not_have_pvr));
                } else {
                    // TODO: ersetzen  emptyView für ViewBinding
                    //emptyView.setText(String.format(getString(R.string.error_getting_pvr_info), description));
                }
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.error_getting_pvr_info), description),
                               Toast.LENGTH_SHORT).show();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }, callbackHandler);
    }

    private List<PVRType.DetailsChannel> filter(List<PVRType.DetailsChannel> itemList) {
        String searchFilter = getSearchFilter();

        if (TextUtils.isEmpty(searchFilter)) {
            return itemList;
        }

        // Split searchFilter to multiple lowercase words
        String[] lcWords = searchFilter.toLowerCase().split(" ");;

        List<PVRType.DetailsChannel> result = new ArrayList<>(itemList.size());
        for (PVRType.DetailsChannel item:itemList) {
            // Require all words to match the item:
            boolean allWordsMatch = true;
            for (String lcWord:lcWords) {
                if (!searchFilterWordMatches(lcWord, item)) {
                    allWordsMatch = false;
                    break;
                }
            }
            if (!allWordsMatch) {
                continue; // skip this item
            }

            result.add(item);
        }

        return result;
    }

    public boolean searchFilterWordMatches(String lcWord, PVRType.DetailsChannel item) {
        if (item.label.toLowerCase().contains(lcWord)) {
            return true;
        }
        if (item.broadcastnow != null && item.broadcastnow.title.toLowerCase().contains(lcWord)){
            return true;
        }
        return false;
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
        binding.list.setAdapter(channelGroupAdapter);
        binding.list.setOnItemClickListener((parent, view, position, id) -> {
            // Get the id from the tag
            ChannelGroupViewHolder tag = (ChannelGroupViewHolder) view.getTag();
            selectedChannelGroupId = tag.channelGroupId;
            // Notify the activity and show the channels
            listenerActivity.onChannelGroupSelected(tag.channelGroupId, tag.channelGroupName);
            browseChannels(tag.channelGroupId);
        });

        channelGroupAdapter.clear();
        channelGroupAdapter.addAll(result);
        channelGroupAdapter.notifyDataSetChanged();
    }

    /**
     * Gets and displays the channels of a channelgroup
     * @param channelGroupId id
     */
    private void browseChannels(final int channelGroupId) {
        String[] properties = PVRType.FieldsChannel.allValues;
        LogUtils.LOGD(TAG, "Getting channels");

        PVR.GetChannels action = new PVR.GetChannels(channelGroupId, properties);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsChannel>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsChannel> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got channels");

                // To prevent the empty text from appearing on the first load, set it now
                // TODO: ersetzen  emptyView für ViewBinding
                //emptyView.setText(getString(R.string.no_channels_found_refresh));

                List<PVRType.DetailsChannel> finalResult = filter(result);

                setupChannelsGridview(finalResult);
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting channels: " + description);

                // To prevent the empty text from appearing on the first load, set it now
                // TODO: ersetzen  emptyView für ViewBinding
                //emptyView.setText(String.format(getString(R.string.error_getting_pvr_info), description));
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.error_getting_pvr_info), description),
                               Toast.LENGTH_SHORT).show();
                binding.swipeRefreshLayout.setRefreshing(false);
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
        binding.list.setAdapter(channelAdapter);
        binding.list.setOnItemClickListener((parent, view, position, id) -> {
            // Get the id from the tag
            ChannelViewHolder tag = (ChannelViewHolder) view.getTag();

            // Start the channel
            Toast.makeText(getActivity(),
                           String.format(getString(R.string.channel_switching), tag.channelName),
                           Toast.LENGTH_SHORT).show();
            Player.Open action = new Player.Open(Player.Open.TYPE_CHANNEL, tag.channelId);
            action.execute(hostManager.getConnection(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String result1) {
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
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                                            .inflate(R.layout.grid_item_channel_group, parent, false);

                // Setup View holder pattern
                ChannelGroupViewHolder viewHolder = new ChannelGroupViewHolder();
                viewHolder.titleView = convertView.findViewById(R.id.title);
                convertView.setTag(viewHolder);
            }

            final ChannelGroupViewHolder viewHolder = (ChannelGroupViewHolder)convertView.getTag();
            PVRType.DetailsChannelGroup channelGroupDetails = this.getItem(position);

            viewHolder.channelGroupId = channelGroupDetails.channelgroupid;
            viewHolder.channelGroupName = channelGroupDetails.label;

            viewHolder.titleView.setText(UIUtils.applyMarkup(getContext(), viewHolder.channelGroupName));
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

        private View.OnClickListener channelItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ChannelViewHolder viewHolder = (ChannelViewHolder)v.getTag();
                final int channelId = viewHolder.channelId;
                final String channelName = viewHolder.channelName;

                final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.pvr_channel_list_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.action_record_item:
                            PVR.Record action = new PVR.Record(channelId);
                            action.execute(hostManager.getConnection(), new ApiCallback<String>() {
                                @Override
                                public void onSuccess(String result) {
                                    if (!isAdded()) return;
                                    LogUtils.LOGD(TAG, "Started recording");
                                }

                                @Override
                                public void onError(int errorCode, String description) {
                                    if (!isAdded()) return;
                                    LogUtils.LOGD(TAG, "Error starting to record: " + description);

                                    Toast.makeText(getActivity(),
                                                   String.format(getString(R.string.error_starting_to_record), description),
                                                   Toast.LENGTH_SHORT).show();

                                }
                            }, callbackHandler);
                            return true;
                        case R.id.action_epg_item:
                            listenerActivity.onChannelGuideSelected(channelId, channelName, singleChannelGroup);
                            return true;
                    }
                    return false;
                });
                popupMenu.show();
            }
        };

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
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(getActivity())
                        .inflate(R.layout.grid_item_channel, parent, false);

                // Setup View holder pattern
                ChannelViewHolder viewHolder = new ChannelViewHolder();
                viewHolder.titleView = convertView.findViewById(R.id.title);
                viewHolder.detailsView = convertView.findViewById(R.id.details);
                viewHolder.artView = convertView.findViewById(R.id.art);
                viewHolder.contextMenu = convertView.findViewById(R.id.list_context_menu);
                convertView.setTag(viewHolder);
            }

            final ChannelViewHolder viewHolder = (ChannelViewHolder)convertView.getTag();
            PVRType.DetailsChannel channelDetails = this.getItem(position);

            viewHolder.channelId = channelDetails.channelid;
            viewHolder.channelName = channelDetails.channel;

            Context context = getContext();
            viewHolder.titleView.setText(UIUtils.applyMarkup(context, channelDetails.channel));
            String details = (channelDetails.broadcastnow != null)?
                    channelDetails.broadcastnow.title : null;
            viewHolder.detailsView.setText(UIUtils.applyMarkup(context, details));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 channelDetails.thumbnail, channelDetails.channel,
                                                 viewHolder.artView, artWidth, artHeight);

            // For the popupmenu
            viewHolder.contextMenu.setTag(viewHolder);
            viewHolder.contextMenu.setOnClickListener(channelItemMenuClickListener);

            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class ChannelViewHolder {
        TextView titleView, detailsView;
        ImageView artView, contextMenu;

        int channelId;
        String channelName;
    }
}
