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
package org.xbmc.kore.ui.sections.addon;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.type.AddonType;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.AbstractListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment that presents the movie list
 */
public class AddonListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(AddonListFragment.class);

    public interface OnAddonSelectedListener {
        void onAddonSelected(ViewHolder vh);
    }

    // Activity listener
    private OnAddonSelectedListener listenerActivity;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    @Override
    protected AdapterView.OnItemClickListener createOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onAddonSelected(tag);
            }
        };
    }


    @Override
    protected BaseAdapter createAdapter() {
        return new AddonsAdapter(getActivity(), R.layout.grid_item_addon);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);

        if (getAdapter().getCount() == 0)
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
    public void onRefresh () {
        if (HostManager.getInstance(getActivity()).getHostInfo() != null) {
            callGetAddonsAndSetup();
        } else {
            hideRefreshAnimation();
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }
    public class AddonNameComparator implements Comparator<AddonType.Details>
    {
        public int compare(AddonType.Details left, AddonType.Details right) {
            return left.name.toLowerCase().compareTo(right.name.toLowerCase());
        }
    }
    /**
     * Get the addons list and setup the gridview
     */
    private void callGetAddonsAndSetup() {
        final AddonsAdapter adapter = (AddonsAdapter) getAdapter();

        UIUtils.showRefreshAnimation(swipeRefreshLayout);

        // Get the addon list, this is done asyhnchronously
        String[] properties = new String[] {
                AddonType.Fields.NAME, AddonType.Fields.VERSION, AddonType.Fields.SUMMARY,
                AddonType.Fields.DESCRIPTION,  AddonType.Fields.PATH, AddonType.Fields.AUTHOR,
                AddonType.Fields.THUMBNAIL, AddonType.Fields.DISCLAIMER, AddonType.Fields.FANART,
                //AddonType.Fields.DEPENDENCIES, AddonType.Fields.BROKEN, AddonType.Fields.EXTRAINFO,
                AddonType.Fields.RATING, AddonType.Fields.ENABLED
        };
        Addons.GetAddons action = new Addons.GetAddons(properties);
        action.execute(HostManager.getInstance(getActivity()).getConnection(),
            new ApiCallback<List<AddonType.Details>>() {
                @Override
                public void onSuccess(List<AddonType.Details> result) {
                    if (!isAdded()) return;

                    for (AddonType.Details addon : result) {
                        String regex = "\\[.*?\\]";
                        addon.name = addon.name.replaceAll(regex, "");
                        addon.description = addon.description.replaceAll(regex, "");
                        addon.summary = addon.summary.replaceAll(regex, "");
                        addon.author = addon.author.replaceAll(regex, "");
                    }
                    Collections.sort(result, new AddonNameComparator());

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

                    adapter.notifyDataSetChanged();
                    hideRefreshAnimation();

                    if (adapter.getCount() == 0) {
                        getEmptyView().setText(R.string.no_addons_found_refresh);
                    }
                }

                @Override
                public void onError(int errorCode, String description) {
                    if (!isAdded()) return;

                    Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_addon_info), description),
                        Toast.LENGTH_SHORT).show();
                    hideRefreshAnimation();
                }
            },
            callbackHandler);
    }

    private class AddonsAdapter extends ArrayAdapter<AddonType.Details> {

        private HostManager hostManager;
        private int artWidth, artHeight;
        private String author;
        private String version;

        public AddonsAdapter(Context context, int resource) {
            super(context, resource);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = resources.getDimensionPixelOffset(R.dimen.detail_poster_width_square);
            artHeight = resources.getDimensionPixelOffset(R.dimen.detail_poster_height_square);

            author = context.getString(R.string.author);
            version = context.getString(R.string.version);
        }

        /** {@inheritDoc} */
        @TargetApi(21)
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

            viewHolder.dataHolder.setTitle(addonDetails.name);
            viewHolder.dataHolder.setDescription(addonDetails.description);
            viewHolder.dataHolder.setUndertitle(addonDetails.summary);
            viewHolder.dataHolder.setFanArtUrl(addonDetails.fanart);
            viewHolder.dataHolder.setPosterUrl(addonDetails.thumbnail);
            viewHolder.dataHolder.setDetails(author + " " + addonDetails.author + "\n" +
                                             version + " " +addonDetails.version);
            viewHolder.dataHolder.getBundle().putString(AddonInfoFragment.BUNDLE_KEY_ADDONID, addonDetails.addonid);
            viewHolder.dataHolder.getBundle().putBoolean(AddonInfoFragment.BUNDLE_KEY_BROWSABLE,
                                                         AddonType.Types.XBMC_PYTHON_PLUGINSOURCE.equals(addonDetails.type));

            viewHolder.titleView.setText(viewHolder.dataHolder.getTitle());
            viewHolder.detailsView.setText(addonDetails.summary);

            UIUtils.loadImageWithCharacterAvatar(getContext(), hostManager,
                                                 addonDetails.thumbnail, viewHolder.dataHolder.getTitle(),
                                                 viewHolder.artView, artWidth, artHeight);

            if(Utils.isLollipopOrLater()) {
                viewHolder.artView.setTransitionName("a"+addonDetails.addonid);
            }
            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder {
        TextView titleView;
        TextView detailsView;
        ImageView artView;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);
    }
}
