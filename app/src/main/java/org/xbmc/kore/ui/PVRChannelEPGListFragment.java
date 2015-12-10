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

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    @InjectView(R.id.list) GridView gridView;
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
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
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
        gridView.setEmptyView(emptyView);

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
     * Get the EPF for the channel and setup the gridview
     */
    private void browseEPG() {
        PVR.GetBroadcasts action = new PVR.GetBroadcasts(channelId, PVRType.FieldsBroadcast.allValues);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsBroadcast>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsBroadcast> result) {
                if (!isAdded()) return;
                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_broadcasts_found_refresh));
                setupEPGGridview(result);
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
    private void setupEPGGridview(List<PVRType.DetailsBroadcast> result) {
        if (boadcastsAdapter == null) {
            boadcastsAdapter = new BoadcastsAdapter(getActivity(), R.layout.grid_item_broadcast);
        }

        gridView.setAdapter(boadcastsAdapter);
        boadcastsAdapter.clear();
        boadcastsAdapter.addAll(result);
        boadcastsAdapter.notifyDataSetChanged();
    }

    private class BoadcastsAdapter extends ArrayAdapter<PVRType.DetailsBroadcast> {
        private HostManager hostManager;
        private int artWidth, artHeight;

        public BoadcastsAdapter(Context context, int resource) {
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
                convertView = LayoutInflater.from(getActivity())
                                            .inflate(R.layout.grid_item_broadcast, parent, false);

                // Setup View holder pattern
                BroadcastViewHolder viewHolder = new BroadcastViewHolder();
                viewHolder.titleView = (TextView)convertView.findViewById(R.id.title);
                viewHolder.detailsView = (TextView)convertView.findViewById(R.id.details);
                viewHolder.startTimeView = (TextView)convertView.findViewById(R.id.start_time);
                viewHolder.endTimeView = (TextView)convertView.findViewById(R.id.end_time);
                convertView.setTag(viewHolder);
            }

            final BroadcastViewHolder viewHolder = (BroadcastViewHolder)convertView.getTag();
            PVRType.DetailsBroadcast broadcastDetails = this.getItem(position);

            viewHolder.broadcastId = broadcastDetails.broadcastid;
            viewHolder.title = broadcastDetails.title;

            viewHolder.titleView.setText(broadcastDetails.title);
            viewHolder.detailsView.setText(broadcastDetails.plot);
            int runtime = broadcastDetails.runtime / 60;
            String duration =
                    broadcastDetails.starttime + " | " +
                    String.format(this.getContext().getString(R.string.minutes_abbrev), String.valueOf(runtime));

            // Parse dates
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date startTime, endTime;
            try {
                startTime = sdf.parse(broadcastDetails.starttime);
                endTime = sdf.parse(broadcastDetails.endtime);
            } catch (ParseException exc) {
                startTime = new Date();
                endTime = new Date();
            }

            int flags = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME;
            viewHolder.startTimeView.setText(DateUtils.formatDateTime(getActivity(), startTime.getTime(), flags));
            viewHolder.endTimeView.setText(DateUtils.formatDateTime(getActivity(), endTime.getTime(), flags));

//            DateFormat dfLocal = DateFormat.getTimeInstance(DateFormat.SHORT);
//            viewHolder.startTimeView.setText(dfLocal.format(startTime));
//            viewHolder.endTimeView.setText(dfLocal.format(endTime));

            //viewHolder.durationView.setText(duration);

            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class BroadcastViewHolder {
        TextView titleView, detailsView,
                startTimeView, endTimeView;

        int broadcastId;
        String title;
    }

}
