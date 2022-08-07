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
import androidx.preference.PreferenceManager;

import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.PVR;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PVRType;
import org.xbmc.kore.ui.AbstractSearchableFragment;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that presents the PVR recordings list
 */
public class PVRRecordingsListFragment extends AbstractSearchableFragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(PVRRecordingsListFragment.class);

    private HostManager hostManager;

    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    @Override
    protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener() {
        return (view, position) -> {
            // Get the id from the tag
            RecordingViewHolder tag = (RecordingViewHolder) view.getTag();

            // Start the recording
            Toast.makeText(requireContext(),
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

                    Toast.makeText(requireContext(),
                            String.format(getString(R.string.error_starting_recording), description),
                            Toast.LENGTH_SHORT).show();

                }
            }, callbackHandler);

        };
    }

    @Override
    protected RecyclerView.Adapter<RecordingViewHolder> createAdapter() {
        return new RecordingsAdapter(requireContext());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setSupportsSearch(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        hostManager = HostManager.getInstance(requireContext());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        browseRecordings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int itemId = item.getItemId();
        if (itemId == R.id.action_hide_watched) {
            item.setChecked(!item.isChecked());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED, item.isChecked())
                       .apply();
            refreshList();
        } else if (itemId == R.id.action_sort_by_name_and_date_added) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.SORT_BY_NAME)
                       .apply();
            refreshList();
        } else if (itemId == R.id.action_sort_by_date_added) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                       .apply();
            refreshList();
        } else if (itemId == R.id.action_unsorted) {
            item.setChecked(true);
            preferences.edit()
                       .putInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.UNSORTED)
                       .apply();
            refreshList();
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostManager.getHostInfo() != null) {
            browseRecordings();
        } else {
            hideRefreshAnimation();
            Toast.makeText(requireContext(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
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
                TextView emptyView = getEmptyView();
                emptyView.setText(getString(R.string.no_recordings_found_refresh));

                // As the JSON RPC API does not support sorting or filter parameters for PVR.GetRecordings
                // we apply the sorting and filtering right here.
                // See https://kodi.wiki/view/JSON-RPC_API/v9#PVR.GetRecordings
                List<PVRType.DetailsRecording> finalResult = filter(result);
                sort(finalResult);

                setupRecordingsGridview(finalResult);
                hideRefreshAnimation();
            }

            private List<PVRType.DetailsRecording> filter(List<PVRType.DetailsRecording> itemList) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                boolean hideWatched = preferences.getBoolean(Settings.KEY_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_PVR_RECORDINGS_FILTER_HIDE_WATCHED);

                String searchFilter = getSearchFilter();
                boolean hasSearchFilter = !TextUtils.isEmpty(searchFilter);
                // Split searchFilter to multiple lowercase words
                String[] lcWords = hasSearchFilter ? searchFilter.toLowerCase(Locale.getDefault()).split(" ") : null;

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
                return item.title.toLowerCase(Locale.getDefault()).contains(lcWord) ||
                       item.channel.toLowerCase(Locale.getDefault()).contains(lcWord);
            }

            private void sort(List<PVRType.DetailsRecording> itemList) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

                int sortOrder = preferences.getInt(Settings.KEY_PREF_PVR_RECORDINGS_SORT_ORDER, Settings.DEFAULT_PREF_PVR_RECORDINGS_SORT_ORDER);

                Comparator<PVRType.DetailsRecording> comparator;
                switch (sortOrder) {
                    case Settings.SORT_BY_DATE_ADDED:
                        // sort by recording start time descending (most current first)
                        // luckily the starttime is in sortable format yyyy-MM-dd hh:mm:ss
                        comparator = (a, b) -> b.starttime.compareTo(a.starttime);
                        Collections.sort(itemList, comparator);
                        break;
                    case Settings.SORT_BY_NAME:
                        // sort by recording title and start time
                        comparator = (a, b) -> {
                            int result = a.title.compareToIgnoreCase(b.title);
                            if (0 == result) { // note the yoda condition ;)
                                // sort by starttime descending (most current first)
                                result = b.starttime.compareTo(a.starttime);
                            }
                            return result;
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
                TextView emptyView = getEmptyView();
                emptyView.setText(String.format(getString(R.string.error_getting_pvr_info), description));
                Toast.makeText(requireContext(),
                               String.format(getString(R.string.error_getting_pvr_info), description),
                               Toast.LENGTH_SHORT).show();
                hideRefreshAnimation();
            }
        }, callbackHandler);
    }

    /**
     * Called when we get the recordings
     *
     * @param result Recordings obtained
     */
    private void setupRecordingsGridview(List<PVRType.DetailsRecording> result) {
        RecordingsAdapter recordingsAdapter = (RecordingsAdapter) getAdapter();
        recordingsAdapter.setItems(result);
    }

    private class RecordingsAdapter extends RecyclerView.Adapter<RecordingViewHolder> {
        private final HostManager hostManager;
        private final int artWidth, artHeight;
        private final Context context;
        private List<PVRType.DetailsRecording> items;

        public RecordingsAdapter(Context context) {
            super();

            this.hostManager = HostManager.getInstance(context);
            this.context = context;

            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.channellist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.channellist_art_heigth) /
                              UIUtils.IMAGE_RESIZE_FACTOR);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
            PVRType.DetailsRecording item = this.getItem(position);
            assert item != null;
            holder.bindView(item, getContext(), hostManager, artWidth, artHeight);
        }

        @NonNull
        @Override
        public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(context)
                                      .inflate(R.layout.item_pvr_recording, viewGroup, false);
            return new RecordingViewHolder(view);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param details list of files/directories
         */
        public void setItems(List<PVRType.DetailsRecording> details) {
            this.items = details;

            notifyDataSetChanged();
        }

        public PVRType.DetailsRecording getItem(int position) {
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
    private static class RecordingViewHolder extends RecyclerView.ViewHolder {
        ImageView artView;
        TextView titleView, detailsView, durationView;

        int recordingId;
        String title;

        public RecordingViewHolder(View itemView) {
            super(itemView);

            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            artView = itemView.findViewById(R.id.art);
            durationView = itemView.findViewById(R.id.duration);
        }

        public void bindView(PVRType.DetailsRecording recordingDetails, Context context, HostManager hostManager, int artWidth, int artHeight) {
            recordingId = recordingDetails.recordingid;
            title = recordingDetails.title;

            titleView.setText(UIUtils.applyMarkup(context, recordingDetails.title));
            detailsView.setText(UIUtils.applyMarkup(context, recordingDetails.channel));
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    (recordingDetails.art != null) ?
                            recordingDetails.art.poster : recordingDetails.icon,
                    recordingDetails.channel,
                    artView, artWidth, artHeight);
            int runtime = recordingDetails.runtime / 60;
            String duration =
                    recordingDetails.starttime + " | " +
                            context.getString(R.string.minutes_abbrev, String.valueOf(runtime));
            durationView.setText(duration);

            itemView.setTag(this);
        }
    }

}
