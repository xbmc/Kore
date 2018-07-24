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
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Fragment that presents the PVR recordings list
 */
public class PVRRecordingsListFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(PVRRecordingsListFragment.class);

    private HostManager hostManager;

    @BindView(R.id.list) GridView gridView;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(android.R.id.empty) TextView emptyView;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    private RecordingsAdapter recordingsAdapter = null;

    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_pvr_list, container, false);
        unbinder = ButterKnife.bind(this, root);

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
        browseRecordings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostManager.getHostInfo() != null) {
            browseRecordings();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * Get the recording list and setup the gridview
     */
    private void browseRecordings() {
        PVR.GetRecordings action = new PVR.GetRecordings(PVRType.FieldsRecording.allValues);
        action.execute(hostManager.getConnection(), new ApiCallback<List<PVRType.DetailsRecording>>() {
            @Override
            public void onSuccess(List<PVRType.DetailsRecording> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got recordings");

                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_recordings_found_refresh));
                setupRecordingsGridview(result);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting recordings: " + description);

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
     * Called when we get the recordings
     *
     * @param result Recordings obtained
     */
    private void setupRecordingsGridview(List<PVRType.DetailsRecording> result) {
        if (recordingsAdapter == null) {
            recordingsAdapter = new RecordingsAdapter(getActivity(), R.layout.grid_item_recording);
        }
        gridView.setAdapter(recordingsAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the id from the tag
                RecordingViewHolder tag = (RecordingViewHolder) view.getTag();

                // Start the recording
                Toast.makeText(getActivity(),
                               String.format(getString(R.string.starting_recording), tag.title),
                               Toast.LENGTH_SHORT).show();
                Player.Open action = new Player.Open(Player.Open.TYPE_RECORDING, tag.recordingId);
                action.execute(hostManager.getConnection(), new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        if (!isAdded()) return;
                        LogUtils.LOGD(TAG, "Started recording");
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        if (!isAdded()) return;
                        LogUtils.LOGD(TAG, "Error starting recording: " + description);

                        Toast.makeText(getActivity(),
                                       String.format(getString(R.string.error_starting_recording), description),
                                       Toast.LENGTH_SHORT).show();

                    }
                }, callbackHandler);

            }
        });

        recordingsAdapter.clear();
        recordingsAdapter.addAll(result);
        recordingsAdapter.notifyDataSetChanged();
    }

    private class RecordingsAdapter extends ArrayAdapter<PVRType.DetailsRecording> {
        private HostManager hostManager;
        private int artWidth, artHeight;

        public RecordingsAdapter(Context context, int resource) {
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
                                            .inflate(R.layout.grid_item_recording, parent, false);

                // Setup View holder pattern
                RecordingViewHolder viewHolder = new RecordingViewHolder();
                viewHolder.titleView = (TextView)convertView.findViewById(R.id.title);
                viewHolder.detailsView = (TextView)convertView.findViewById(R.id.details);
                viewHolder.artView = (ImageView)convertView.findViewById(R.id.art);
                viewHolder.durationView = (TextView)convertView.findViewById(R.id.duration);
                convertView.setTag(viewHolder);
            }

            final RecordingViewHolder viewHolder = (RecordingViewHolder)convertView.getTag();
            PVRType.DetailsRecording recordingDetails = this.getItem(position);

            viewHolder.recordingId = recordingDetails.recordingid;
            viewHolder.title = recordingDetails.title;

            Context context = getContext();
            viewHolder.titleView.setText(UIUtils.applyMarkup(context, recordingDetails.title));
            viewHolder.detailsView.setText(UIUtils.applyMarkup(context, recordingDetails.channel));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 (recordingDetails.art != null) ?
                                                         recordingDetails.art.poster : recordingDetails.icon,
                                                 recordingDetails.channel,
                                                 viewHolder.artView, artWidth, artHeight);
            int runtime = recordingDetails.runtime / 60;
            String duration =
                    recordingDetails.starttime + " | " +
                    context.getString(R.string.minutes_abbrev, String.valueOf(runtime));
            viewHolder.durationView.setText(duration);

            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class RecordingViewHolder {
        ImageView artView;
        TextView titleView, detailsView, durationView;

        int recordingId;
        String title;
    }

}
