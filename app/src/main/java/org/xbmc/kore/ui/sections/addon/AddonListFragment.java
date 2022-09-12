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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.type.AddonType;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.AbstractListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that presents the movie list
 */
public class AddonListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(AddonListFragment.class);

    public interface OnAddonSelectedListener {
        void onAddonSelected(AbstractFragment.DataHolder dataHolder, ImageView sharedImageView);
    }

    // Activity listener
    private OnAddonSelectedListener listenerActivity;

    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private static boolean hideDisabledAddons;

    @Override
    protected void onListItemClicked(View view, int position) {
        // Get the movie id from the tag
        ViewHolder tag = (ViewHolder) view.getTag();
        // Notify the activity
        listenerActivity.onAddonSelected(tag.dataHolder, tag.artView);
    }

    @Override
    protected RecyclerView.Adapter<ViewHolder> createAdapter() {
        return new AddonsAdapter(getActivity());
    }

    @Override
    protected String getEmptyResultsTitle() { return getString(R.string.no_addons_found_refresh); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listenerActivity = (OnAddonSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnAddonSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Show the addons
     */
    @Override
    public void onConnectionStatusSuccess() {
        boolean refresh = (lastConnectionStatusResult != CONNECTION_SUCCESS);
        super.onConnectionStatusSuccess();
        if (refresh || hasNavigatedToDetail) onRefresh();
    }

    @Override
    public void onRefresh () {
        if (HostManager.getInstance(requireContext()).getHostInfo() != null) {
            getAddonsAndSetup();
        } else {
            hideRefreshAnimation();
            UIUtils.showSnackbar(getView(), R.string.no_xbmc_configured);
        }
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.addon_list, menu);

        // Setup filters
        MenuItem hideDisabled = menu.findItem(R.id.action_hide_disabled);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        hideDisabledAddons = preferences.getBoolean(Settings.KEY_PREF_ADDONS_FILTER_HIDE_DISABLED, Settings.DEFAULT_PREF_ADDONS_FILTER_HIDE_DISABLED);
        hideDisabled.setChecked(hideDisabledAddons);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int itemId = item.getItemId();
        if (itemId == R.id.action_hide_disabled) {
            item.setChecked(!item.isChecked());
            preferences.edit()
                       .putBoolean(Settings.KEY_PREF_ADDONS_FILTER_HIDE_DISABLED, item.isChecked())
                       .apply();
            hideDisabledAddons = item.isChecked();
            getAddonsAndSetup();
        }

        return super.onOptionsItemSelected(item);
    }

    public static class AddonNameComparator implements Comparator<AddonType.Details>
    {
        public int compare(AddonType.Details left, AddonType.Details right) {
            return left.name.toLowerCase(Locale.getDefault()).compareTo(right.name.toLowerCase(Locale.getDefault()));
        }
    }
    /**
     * Get the addons list and setup the gridview
     */
    private void getAddonsAndSetup() {
        final AddonsAdapter adapter = (AddonsAdapter) getAdapter();

        // Get the addon list, this is done asyhnchronously
        String[] properties = new String[] {
                AddonType.Fields.NAME, AddonType.Fields.VERSION, AddonType.Fields.SUMMARY,
                AddonType.Fields.DESCRIPTION,  AddonType.Fields.PATH, AddonType.Fields.AUTHOR,
                AddonType.Fields.THUMBNAIL, AddonType.Fields.DISCLAIMER, AddonType.Fields.FANART,
                //AddonType.Fields.DEPENDENCIES, AddonType.Fields.BROKEN, AddonType.Fields.EXTRAINFO,
                AddonType.Fields.RATING, AddonType.Fields.ENABLED
        };
        Addons.GetAddons action = new Addons.GetAddons(properties);
        action.execute(HostManager.getInstance(requireContext()).getConnection(),
                       new ApiCallback<List<AddonType.Details>>() {
                @SuppressLint("NotifyDataSetChanged")
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
                        if (isAddonSupported(addon) && (!hideDisabledAddons || addon.enabled)) {
                            adapter.add(addon);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    hideRefreshAnimation();
                    // Notify parent that list view is setup
                    notifyListSetupComplete();
                }

                @Override
                public void onError(int errorCode, String description) {
                    if (!isAdded()) return;
                    LogUtils.LOGD(TAG, "Error getting addons: " + description);
                    hideRefreshAnimation();
                    showStatusMessage(null, getString(R.string.error_getting_addon_info, description));
                }
            },
                       callbackHandler);
    }

    private boolean isAddonSupported(AddonType.Details addon) {
        return (addon.type.equals(AddonType.Types.UNKNOWN) ||
                addon.type.equals(AddonType.Types.XBMC_PYTHON_PLUGINSOURCE) ||
                addon.type.equals(AddonType.Types.XBMC_PYTHON_SCRIPT) ||
                addon.type.equals(AddonType.Types.XBMC_ADDON_AUDIO) ||
                addon.type.equals(AddonType.Types.XBMC_ADDON_EXECUTABLE) ||
                addon.type.equals(AddonType.Types.XBMC_ADDON_VIDEO) ||
                addon.type.equals(AddonType.Types.XBMC_ADDON_IMAGE));
    }

    private static class AddonsAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final HostManager hostManager;
        private final int artWidth, artHeight;
        private final Context context;

        private final ArrayList<AddonType.Details> items = new ArrayList<>();

        public AddonsAdapter(Context context) {
            this.context = context;
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            // Use the same dimensions as in the details fragment, so that it hits Picasso's cache when
            // the user transitions to that fragment, avoiding another call and imediatelly showing the image
            Resources resources = context.getResources();
            artWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_width_square);
            artHeight = resources.getDimensionPixelOffset(R.dimen.info_poster_height_square);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                                      .inflate(R.layout.item_addon, parent, false);

            return new ViewHolder(view, context, hostManager, artWidth, artHeight);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AddonType.Details addonDetails = this.getItem(position);
            holder.onBind(addonDetails);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void clear() {
            items.clear();
        }

        public void add(AddonType.Details item) {
            items.add(item);
        }

        public AddonType.Details getItem(int position) {
            return items.get(position);
        }
    }

    /**
     * View holder pattern
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView detailsView;
        ImageView artView;
        ImageView disabledView;
        private static String author;
        private static String version;
        private final HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        AbstractInfoFragment.DataHolder dataHolder = new AbstractInfoFragment.DataHolder(0);

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            if( author == null ) {
                author = context.getString(R.string.author);
                version = context.getString(R.string.version);
            }

            titleView = itemView.findViewById(R.id.title);
            detailsView = itemView.findViewById(R.id.details);
            artView = itemView.findViewById(R.id.art);
            disabledView = itemView.findViewById(R.id.disabled);

            itemView.setTag(this);
        }

        public void onBind(AddonType.Details addonDetails) {
            dataHolder.setTitle(addonDetails.name);
            dataHolder.setDescription(addonDetails.description);
            dataHolder.setUndertitle(addonDetails.summary);
            dataHolder.setFanArtUrl(addonDetails.fanart);
            dataHolder.setPosterUrl(addonDetails.thumbnail);
            dataHolder.setDetails(author + " " + addonDetails.author + "\n" +
                                  version + " " +addonDetails.version);
            dataHolder.getBundle().putString(AddonInfoFragment.BUNDLE_KEY_ADDONID, addonDetails.addonid);
            dataHolder.getBundle().putBoolean(AddonInfoFragment.BUNDLE_KEY_BROWSABLE,
                                              AddonType.Types.XBMC_PYTHON_PLUGINSOURCE.equals(addonDetails.type));

            titleView.setText(dataHolder.getTitle());
            detailsView.setText(addonDetails.summary);
            disabledView.setVisibility(addonDetails.enabled ? View.INVISIBLE : View.VISIBLE);

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 addonDetails.thumbnail, dataHolder.getTitle(),
                                                 artView, artWidth, artHeight);

            artView.setTransitionName("addon" + addonDetails.addonid);
        }
    }
}
