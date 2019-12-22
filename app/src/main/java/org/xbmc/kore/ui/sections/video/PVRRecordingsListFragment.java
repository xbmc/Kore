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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.ui.AbstractSearchableFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Fragment that presents the PVR recordings list
 */
public class PVRRecordingsListFragment extends AbstractSearchableFragment
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

        super.onCreateView(inflater, container, savedInstanceState);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setSupportsSearch(true);
        browseRecordings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            // HACK: Fix crash reported on Play Store. Why does this is necessary is beyond me
            // copied from MovieListFragment#onCreateOptionsMenu
            super.onCreateOptionsMenu(menu, inflater);
            return;
        }

        inflater.inflate(R.menu.pvr_recording_list, menu);

        // Setup filters
        MenuItem hideWatched = menu.findItem(R.id.action_hide_watched),
                sortByNameAndDate = menu.findItem(R.id.action_sort_by_name_and_date_added),
                sortByDateAdded = menu.findItem(R.id.action_sort_by_date_added),
                unsorted = menu.findItem(R.id.action_unsorted);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED));

        int sortOrder = preferences.getInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.DEFAULT_PREF_PVR_RECORDINGS_SORT_ORDER);
        switch (sortOrder) {
            case Settings.SORT_BY_DATE_ADDED:
                sortByDateAdded.setChecked(true);
                break;
            case Settings.SORT_BY_NAME:
                sortByNameAndDate.setChecked(true);
                break;
            default:
                unsorted.setChecked(true);
                break;
        }

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void refreshList() {
       onRefresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        switch (item.getItemId()) {
            case R.id.action_hide_watched:
                item.setChecked(!item.isChecked());
                preferences.edit()
                        .putBoolean(Settings.KEY_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED, item.isChecked())
                        .apply();
                refreshList();
                break;
            case R.id.action_sort_by_name_and_date_added:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.SORT_BY_NAME)
                        .apply();
                refreshList();
                break;
            case R.id.action_sort_by_date_added:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                        .apply();
                refreshList();
                break;
            case R.id.action_unsorted:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.UNSORTED)
                        .apply();
                refreshList();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
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

                // As the JSON RPC API does not support sorting or filter parameters for PVR.GetRecordings
                // we apply the sorting and filtering right here.
                // See https://kodi.wiki/view/JSON-RPC_API/v9#PVR.GetRecordings
                List<PVRType.DetailsRecording> finalResult = filter(result);
                sort(finalResult);

                setupRecordingsGridview(finalResult);
                swipeRefreshLayout.setRefreshing(false);
            }

            private List<PVRType.DetailsRecording> filter(List<PVRType.DetailsRecording> itemList) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean hideWatched = preferences.getBoolean(Settings.KEY_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED);

                String searchFilter = getSearchFilter();
                boolean hasSearchFilter = !TextUtils.isEmpty(searchFilter);
                // Split searchFilter to multiple lowercase words
                String[] lcWords = hasSearchFilter ? searchFilter.toLowerCase().split(" ") : null;

                if (!(hideWatched || hasSearchFilter)) {
                    return itemList;
                }

                List<PVRType.DetailsRecording> result = new ArrayList<>(itemList.size());
                for (PVRType.DetailsRecording item:itemList) {
                    if (hideWatched) {
                        if (item.playcount > 0) {
                            continue; // Skip this item as it is played.
                        } else {
                            // Heuristic: Try to guess if it's play from resume timestamp.
                            double resumePosition = item.resume.position;
                            int runtime = item.runtime;
                            if (runtime < resumePosition) {
                                // Tv show duration is smaller than resume position.
                                // The tv show likely has been watched.
                                // It's still possible some minutes have not yet been watched
                                // at the end of the show as some minutes at the
                                // recording start do not belong to the show.
                                // Never the less skip this item.
                                continue;
                            }
                        }
                    }

                    if (hasSearchFilter) {
                        // Require all lowercase words to match the item:
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
                    }

                    result.add(item);
                }

                return result;
            }

            private boolean searchFilterWordMatches(String lcWord, PVRType.DetailsRecording item) {
                if (item.title.toLowerCase().contains(lcWord)
                        || item.channel.toLowerCase().contains(lcWord)) {
                    return true;
                }
                return false;
            }

            private void sort(List<PVRType.DetailsRecording> itemList) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                int sortOrder = preferences.getInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.DEFAULT_PREF_PVR_RECORDINGS_SORT_ORDER);

                Comparator<PVRType.DetailsRecording> comparator;
                switch (sortOrder) {
                    case Settings.SORT_BY_DATE_ADDED:
                        // sort by recording start time descending (most current first)
                        // luckily the starttime is in sortable format yyyy-MM-dd hh:mm:ss
                        comparator = new Comparator<PVRType.DetailsRecording>() {
                            @Override
                            public int compare(PVRType.DetailsRecording a, PVRType.DetailsRecording b) {
                                return  b.starttime.compareTo(a.starttime);
                            }
                        };
                        Collections.sort(itemList, comparator);
                        break;
                    case Settings.SORT_BY_NAME:
                        // sort by recording title and start time
                        comparator = new Comparator<PVRType.DetailsRecording>() {
                            @Override
                            public int compare(PVRType.DetailsRecording a, PVRType.DetailsRecording b) {
                                int result = a.title.compareToIgnoreCase(b.title);
                                if (0 == result) { // note the yoda condition ;)
                                    // sort by starttime descending (most current first)
                                    result = b.starttime.compareTo(a.starttime);
                                }
                                return result;
                            }
                        };
                        Collections.sort(itemList, comparator);
                        break;
                }
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
