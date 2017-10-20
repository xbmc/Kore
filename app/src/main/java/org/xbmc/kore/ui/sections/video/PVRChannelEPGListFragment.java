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
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment that presents the Guide for a channel
 */
public class PVRChannelEPGListFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(PVRChannelEPGListFragment.class);

    private HostManager hostManager;
    private int channelId;

    @InjectView(R.id.list) ListView listView;
    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(android.R.id.empty) TextView emptyView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_list, container, false);
        ButterKnife.inject(this, root);

        Bundle bundle = getArguments();
        channelId = bundle.getInt(BUNDLE_KEY_CHANNELID, -1);

        if (channelId == -1) {
            // There's nothing to show
            return null;
        }

        hostManager = HostManager.getInstance(getActivity());

        swipeRefreshLayout.setOnRefreshListener(this);

        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        listView.setEmptyView(emptyView);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        browseEPG();
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
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
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
                emptyView.setText(getString(R.string.no_broadcasts_found_refresh));
                setupEPGListview(result);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting broadcasts: " + description);
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
     * Called when we get the Guide
     *
     * @param result Broadcasts obtained
     */
    private void setupEPGListview(List<PVRType.DetailsBroadcast> result) {
        if (boadcastsAdapter == null) {
            boadcastsAdapter = new BoadcastsAdapter(getActivity(), R.layout.list_item_broadcast);
        }

        listView.setAdapter(boadcastsAdapter);
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
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            EPGListRow row = this.getItem(position);

            // For a broadcast
            if (row.rowType == EPGListRow.TYPE_BROADCAST) {
                if (convertView == null) {
                    convertView = LayoutInflater
                            .from(getActivity())
                            .inflate(R.layout.list_item_broadcast, parent, false);

                    // Setup View holder pattern
                    BroadcastViewHolder viewHolder = new BroadcastViewHolder();
                    viewHolder.titleView = (TextView) convertView.findViewById(R.id.title);
                    viewHolder.detailsView = (TextView) convertView.findViewById(R.id.details);
                    viewHolder.startTimeView = (TextView) convertView.findViewById(R.id.start_time);
                    viewHolder.endTimeView = (TextView) convertView.findViewById(R.id.end_time);
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
                    viewHolder.dayView = (TextView) convertView.findViewById(R.id.day);
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
