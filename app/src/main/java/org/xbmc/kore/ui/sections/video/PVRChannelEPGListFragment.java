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
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
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
import java.util.Locale;

/**
 * Fragment that presents the Guide for a channel
 */
public class PVRChannelEPGListFragment
        extends AbstractSearchableFragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(PVRChannelEPGListFragment.class);

    private HostManager hostManager;
    private int channelId;

    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    public static final String BUNDLE_KEY_CHANNELID = "bundle_key_channelid";

    @Override
    protected void onListItemClicked(View view, int position) {
    }

    @Override
    protected RecyclerView.Adapter<BroadcastViewHolder> createAdapter() {
        return new BoadcastsAdapter(requireContext());
    }

    @Override
    protected String getEmptyResultsTitle() { return getString(R.string.no_broadcasts_found_refresh); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        Bundle bundle = getArguments();
        channelId = (bundle == null) ? -1 : bundle.getInt(BUNDLE_KEY_CHANNELID, -1);
        if (channelId == -1) {
            // There's nothing to show
            return null;
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        setSupportsSearch(true);
        browseEPG();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostManager.getHostInfo() != null) {
            browseEPG();
        } else {
            hideRefreshAnimation();
            UIUtils.showSnackbar(getView(), R.string.no_xbmc_configured);
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

                List<PVRType.DetailsBroadcast> finalResult = filter(result);

                setupEPGListview(finalResult);
                hideRefreshAnimation();
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting broadcasts: " + description);
                showStatusMessage(null, String.format(getString(R.string.error_getting_pvr_info), description));
                hideRefreshAnimation();
            }
        }, callbackHandler);
    }


    private List<PVRType.DetailsBroadcast> filter(List<PVRType.DetailsBroadcast> itemList) {
        String searchFilter = getSearchFilter();

        if (TextUtils.isEmpty(searchFilter)) {
            return itemList;
        }

        // Split searchFilter to multiple lowercase words
        String[] lcWords = searchFilter.toLowerCase(Locale.getDefault()).split(" ");

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
        return item.title.toLowerCase(Locale.getDefault()).contains(lcWord) ||
               item.plot.toLowerCase(Locale.getDefault()).contains(lcWord);
    }

    /**
     * Called when we get the Guide
     *
     * @param result Broadcasts obtained
     */
    private void setupEPGListview(List<PVRType.DetailsBroadcast> result) {
        BoadcastsAdapter boadcastsAdapter = (BoadcastsAdapter) getAdapter();
        boadcastsAdapter.setItems(result);
    }

    private class BoadcastsAdapter extends RecyclerView.Adapter<BroadcastViewHolder> {

        private List<EPGListRow>  items;
        private final Context context;

        public BoadcastsAdapter(Context context) {
            super();

            this.context = context;
        }

        @Override
        public void onBindViewHolder(@NonNull BroadcastViewHolder holder, int position) {
            EPGListRow item = this.getItem(position);
            if (item != null)
                holder.bindView(item, getContext());
        }

        /** {@inheritDoc} */
        @Override
        public int getItemViewType(int position) {
            EPGListRow item = this.getItem(position);
            return (item == null) ? -1 : item.rowType;
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public BroadcastViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int rowType) {
            View view;
            // For a broadcast
            if (rowType == EPGListRow.TYPE_BROADCAST) {
                view = LayoutInflater
                        .from(context)
                        .inflate(R.layout.item_pvr_broadcast, viewGroup, false);
            } else {
                // For a day
                view = LayoutInflater
                        .from(context)
                        .inflate(R.layout.item_pvr_day, viewGroup, false);
            }
            return new BroadcastViewHolder(view);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param details list of files/directories
         */
        public void setItems(List<PVRType.DetailsBroadcast> details) {
            this.items = EPGListRow.buildFromBroadcastList(details);

            notifyDataSetChanged();
        }

        public EPGListRow getItem(int position) {
            if (items == null) {
                return null;
            } else {
                return items.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if (items == null) {
                return 0;
            } else {
                return items.size();
            }
        }
    }

    /**
     * View holder pattern
     */
    private static class BroadcastViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, detailsView,
                startTimeView, endTimeView,
                dayView;

        int broadcastId;
        String title;

        public BroadcastViewHolder(View itemView) {
            super(itemView);

            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            startTimeView = itemView.findViewById(R.id.start_time);
            endTimeView = itemView.findViewById(R.id.end_time);
            dayView = itemView.findViewById(R.id.day);
        }

        public void bindView(EPGListRow epgListRow, Context context) {
            if (epgListRow.rowType == EPGListRow.TYPE_BROADCAST) {
                PVRType.DetailsBroadcast broadcastDetails = epgListRow.detailsBroadcast;

                broadcastId = broadcastDetails.broadcastid;
                title = broadcastDetails.title;

                titleView.setText(UIUtils.applyMarkup(context, broadcastDetails.title));
                detailsView.setText(UIUtils.applyMarkup(context, broadcastDetails.plot));
                String duration = context.getString(R.string.minutes_abbrev2,
                        String.valueOf(broadcastDetails.runtime));

                int flags = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME;
                startTimeView.setText(DateUtils.formatDateTime(context, broadcastDetails.starttime.getTime(), flags));
                endTimeView.setText(duration);
            } else {
                // For a day
                dayView.setText(
                        DateUtils.formatDateTime(context, epgListRow.date.getTime(),
                                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY));
            }
        }
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
