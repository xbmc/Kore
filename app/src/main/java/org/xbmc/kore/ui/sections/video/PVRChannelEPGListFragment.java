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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.FragmentGenericListBinding;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.ui.AbstractSearchableFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Fragment that presents the Guide for a channel
 */
public class PVRChannelEPGListFragment extends AbstractSearchableFragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(PVRChannelEPGListFragment.class);

    private HostManager hostManager;
    private int channelId;

    private FragmentGenericListBinding binding;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    private BoadcastsAdapter boadcastsAdapter = null;

    private static final String BUNDLE_KEY_CHANNELID = "bundle_key_channelid";

    /**
     * Create a new instance of this, initialized to show the current channel
     */
    public static PVRChannelEPGListFragment newInstance(Integer channelId) {
        PVRChannelEPGListFragment fragment = new PVRChannelEPGListFragment();

        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_CHANNELID, channelId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentGenericListBinding.inflate(inflater, container, false);

        Bundle bundle = getArguments();
        channelId = bundle.getInt(BUNDLE_KEY_CHANNELID, -1);

        if (channelId == -1) {
            // There's nothing to show
            return null;
        }

        hostManager = HostManager.getInstance(getActivity());

        binding.swipeRefreshLayout.setOnRefreshListener(this);

        // TODO: set emptyView to ViewBinding
        /*emptyView.setOnClickListener(v -> onRefresh());*/
        binding.list.setEmptyView(binding.list.getEmptyView());

        super.onCreateView(inflater, container, savedInstanceState);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setSupportsSearch(true);
        browseEPG();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostManager.getHostInfo() != null) {
            browseEPG();
        } else {
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    @Override
    protected void refreshList() {
        onRefresh();
    }

    /**
     * Get the EPF for the channel and setup the listview
     */
    private void browseEPG() {
        PVR.GetBroadcasts action = new PVR.GetBroadcasts(channelId, PVRType.FieldsBroadcast.allValues);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsBroadcast>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsBroadcast> result) {
                if (!isAdded()) return;
                // To prevent the empty text from appearing on the first load, set it now
                // TODO: set emptyView to ViewBinding
                //emptyView.setText(getString(R.string.no_broadcasts_found_refresh));

                List<PVRType.DetailsBroadcast> finalResult = filter(result);

                setupEPGListview(finalResult);
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting broadcasts: " + description);
                // To prevent the empty text from appearing on the first load, set it now
                // TODO: set emptyView to ViewBinding
                //emptyView.setText(String.format(getString(R.string.error_getting_pvr_info), description));
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.error_getting_pvr_info), description),
                               Toast.LENGTH_SHORT).show();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }, callbackHandler);
    }


    private List<PVRType.DetailsBroadcast> filter(List<PVRType.DetailsBroadcast> itemList) {
        String searchFilter = getSearchFilter();

        if (TextUtils.isEmpty(searchFilter)) {
            return itemList;
        }

        // Split searchFilter to multiple lowercase words
        String[] lcWords = searchFilter.toLowerCase().split(" ");;

        List<PVRType.DetailsBroadcast> result = new ArrayList<>(itemList.size());
        for (PVRType.DetailsBroadcast item:itemList) {
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

    public boolean searchFilterWordMatches(String lcWord, PVRType.DetailsBroadcast item) {
        if (item.title.toLowerCase().contains(lcWord)) {
            return true;
        }
        if (item.plot.toLowerCase().contains(lcWord)){
            return true;
        }
        return false;
    }

    /**
     * Called when we get the Guide
     *
     * @param result Broadcasts obtained
     */
    private void setupEPGListview(List<PVRType.DetailsBroadcast> result) {
        if (boadcastsAdapter == null) {
            boadcastsAdapter = new BoadcastsAdapter(getActivity(), R.layout.list_item_broadcast);
        }

        binding.list.setAdapter(boadcastsAdapter);
        boadcastsAdapter.clear();
        boadcastsAdapter.addAll(EPGListRow.buildFromBroadcastList(result));
        boadcastsAdapter.notifyDataSetChanged();
    }

    private class BoadcastsAdapter extends ArrayAdapter<EPGListRow> {
        public BoadcastsAdapter(Context context, int resource) {
            super(context, resource);
        }

        /** {@inheritDoc} */
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        /** {@inheritDoc} */
        @Override
        public int getItemViewType(int position) {
            return this.getItem(position).rowType;
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            EPGListRow row = this.getItem(position);

            // For a broadcast
            if (row.rowType == EPGListRow.TYPE_BROADCAST) {
                if (convertView == null) {
                    convertView = LayoutInflater
                            .from(getActivity())
                            .inflate(R.layout.list_item_broadcast, parent, false);

                    // Setup View holder pattern
                    BroadcastViewHolder viewHolder = new BroadcastViewHolder();
                    viewHolder.titleView = convertView.findViewById(R.id.title);
                    viewHolder.detailsView = convertView.findViewById(R.id.details);
                    viewHolder.startTimeView = convertView.findViewById(R.id.start_time);
                    viewHolder.endTimeView = convertView.findViewById(R.id.end_time);
                    convertView.setTag(viewHolder);
                }

                final BroadcastViewHolder viewHolder = (BroadcastViewHolder) convertView.getTag();
                PVRType.DetailsBroadcast broadcastDetails = row.detailsBroadcast;

                viewHolder.broadcastId = broadcastDetails.broadcastid;
                viewHolder.title = broadcastDetails.title;

                Context context = getContext();
                viewHolder.titleView.setText(UIUtils.applyMarkup(context, broadcastDetails.title));
                viewHolder.detailsView.setText(UIUtils.applyMarkup(context, broadcastDetails.plot));
                String duration = context.getString(R.string.minutes_abbrev2,
                                                    String.valueOf(broadcastDetails.runtime));

                int flags = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME;
                viewHolder.startTimeView.setText(DateUtils.formatDateTime(getActivity(), broadcastDetails.starttime.getTime(), flags));
                viewHolder.endTimeView.setText(duration);
            } else {
                // For a day
                if (convertView == null) {
                    convertView = LayoutInflater
                            .from(getActivity())
                            .inflate(R.layout.list_item_day, parent, false);

                    // Setup View holder pattern
                    BroadcastViewHolder viewHolder = new BroadcastViewHolder();
                    viewHolder.dayView = convertView.findViewById(R.id.day);
                    convertView.setTag(viewHolder);
                }
                final BroadcastViewHolder viewHolder = (BroadcastViewHolder) convertView.getTag();
                viewHolder.dayView.setText(
                        DateUtils.formatDateTime(getActivity(), row.date.getTime(),
                                                 DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY));
            }
            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class BroadcastViewHolder {
        TextView titleView, detailsView,
                startTimeView, endTimeView,
                dayView;

        int broadcastId;
        String title;
    }


    /**
     * Class that represents a row in the EPG list
     * Can either represent a day or a broadcast
     */
    private static class EPGListRow {
        static final int TYPE_DAY = 0,
                TYPE_BROADCAST = 1;

        public int rowType;
        public Date date;
        public PVRType.DetailsBroadcast detailsBroadcast;

        public EPGListRow(PVRType.DetailsBroadcast detailsBroadcast) {
            this.rowType = TYPE_BROADCAST;
            this.detailsBroadcast = detailsBroadcast;
        }

        public EPGListRow(Date date) {
            this.rowType = TYPE_DAY;
            this.date = date;
        }

        /**
         * Build the list of rows to show
         * @param broadcasts Broadcast list returned. Assuming it is ordered by date
         * @return List of rows to show
         */
        public static List<EPGListRow> buildFromBroadcastList(List<PVRType.DetailsBroadcast> broadcasts) {
            Date currentTime = new Date();
            int previousDayIdx = 0, dayIdx;
            Calendar cal = Calendar.getInstance();

            List<EPGListRow> result = new ArrayList<>(broadcasts.size() + 5);

            for (PVRType.DetailsBroadcast broadcast: broadcasts) {
                // Ignore if before current time
                if (broadcast.endtime.before(currentTime)) {
                    continue;
                }

                cal.setTime(broadcast.starttime);
                dayIdx = cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DATE);
                if (dayIdx > previousDayIdx) {
                    // New day, add a row representing it to the list
                    previousDayIdx = dayIdx;
                    result.add(new EPGListRow(broadcast.starttime));
                }
                result.add(new EPGListRow(broadcast));
            }
            return result;
        }
    }
}
