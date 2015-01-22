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
package com.syncedsynapse.kore2.ui;

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

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.ApiCallback;
import com.syncedsynapse.kore2.jsonrpc.method.Addons;
import com.syncedsynapse.kore2.jsonrpc.type.AddonType;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.UIUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment that presents the movie list
 */
public class AddonListFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(AddonListFragment.class);

    public interface OnAddonSelectedListener {
        public void onAddonSelected(String addonId, String addonTitle);
    }

    // Activity listener
    private OnAddonSelectedListener listenerActivity;

    private HostManager hostManager;

    @InjectView(R.id.list) GridView addonsGridView;
    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(android.R.id.empty) TextView emptyView;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    private AddonsAdapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
        ButterKnife.inject(this, root);

        hostManager = HostManager.getInstance(getActivity());

        swipeRefreshLayout.setOnRefreshListener(this);

        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        addonsGridView.setEmptyView(emptyView);
        addonsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onAddonSelected(tag.addonId, tag.addonName);
            }
        });

        if (adapter == null) {
            adapter = new AddonsAdapter(getActivity(), R.layout.grid_item_addon);
        }
        addonsGridView.setAdapter(adapter);

        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), addonsGridView, false, false, true);
//        addonsGridView.setClipToPadding(false);

        return root;
    }


    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);

        if (adapter.getCount() == 0)
            callGetAddonsAndSetup();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnAddonSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAddonSelectedListener");
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

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostManager.getHostInfo() != null) {
            callGetAddonsAndSetup();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * Get the addons list and setup the gridview
     */
    private void callGetAddonsAndSetup() {
        swipeRefreshLayout.setRefreshing(true);
        // Get the addon list, this is done asyhnchronously
        String[] properties = new String[] {
                AddonType.Fields.NAME, AddonType.Fields.VERSION, AddonType.Fields.SUMMARY,
                AddonType.Fields.DESCRIPTION,  AddonType.Fields.PATH, AddonType.Fields.AUTHOR,
                AddonType.Fields.THUMBNAIL, AddonType.Fields.DISCLAIMER, AddonType.Fields.FANART,
                //AddonType.Fields.DEPENDENCIES, AddonType.Fields.BROKEN, AddonType.Fields.EXTRAINFO,
                AddonType.Fields.RATING, AddonType.Fields.ENABLED
        };
        Addons.GetAddons action = new Addons.GetAddons(properties);
        action.execute(hostManager.getConnection(), new ApiCallback<List<AddonType.Details>>() {
            @Override
            public void onSucess(List<AddonType.Details> result) {
                if (!isAdded()) return;

                adapter.clear();
                for (AddonType.Details addon : result) {
                    if (addon.type.equals(AddonType.Types.UNKNOWN) ||
                        addon.type.equals(AddonType.Types.XBMC_PYTHON_PLUGINSOURCE) ||
                        addon.type.equals(AddonType.Types.XBMC_PYTHON_SCRIPT) ||
                        addon.type.equals(AddonType.Types.XBMC_ADDON_AUDIO) ||
                        addon.type.equals(AddonType.Types.XBMC_ADDON_EXECUTABLE) ||
                        addon.type.equals(AddonType.Types.XBMC_ADDON_VIDEO) ||
                        addon.type.equals(AddonType.Types.XBMC_ADDON_IMAGE)) {
                        adapter.add(addon);
                    }
                }
                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_addons_found_refresh));
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;

                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_addons_found_refresh));
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_addon_info), description),
                        Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        }, callbackHandler);
    }

    private class AddonsAdapter extends ArrayAdapter<AddonType.Details> {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public AddonsAdapter(Context context, int resource) {
            super(context, resource);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.addonlist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.addonlist_art_heigth) /
                              UIUtils.IMAGE_RESIZE_FACTOR);
        }

        /** {@inheritDoc} */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                                            .inflate(R.layout.grid_item_addon, parent, false);

                // Setup View holder pattern
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.titleView = (TextView)convertView.findViewById(R.id.title);
                viewHolder.detailsView = (TextView)convertView.findViewById(R.id.details);
                viewHolder.artView = (ImageView)convertView.findViewById(R.id.art);
                convertView.setTag(viewHolder);
            }

            final ViewHolder viewHolder = (ViewHolder)convertView.getTag();
            AddonType.Details addonDetails = this.getItem(position);

            // Save the movie id
            viewHolder.addonId = addonDetails.addonid;
            viewHolder.addonName = addonDetails.name;

            viewHolder.titleView.setText(viewHolder.addonName);
            viewHolder.detailsView.setText(addonDetails.summary);

            UIUtils.loadImageWithCharacterAvatar(getContext(), hostManager,
                    addonDetails.thumbnail, viewHolder.addonName,
                    viewHolder.artView, artWidth, artHeight);
            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder {
        TextView titleView;
        TextView detailsView;
        ImageView artView;

        String addonId;
        String addonName;
    }
}
