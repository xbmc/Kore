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

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Addons;
import org.xbmc.kore.jsonrpc.type.AddonType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Presents addon details
 */
public class AddonDetailsFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(AddonDetailsFragment.class);

    public static final String ADDONID = "addon_id";

    private HostManager hostManager;
    private HostInfo hostInfo;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    // Displayed addon id
    private String addonId;

    // Buttons
    @InjectView(R.id.fab) ImageButton fabButton;
    @InjectView(R.id.enable_disable) ImageButton enabledButton;

    // Detail views
    @InjectView(R.id.media_panel) ScrollView mediaPanel;

    @InjectView(R.id.art) ImageView mediaArt;
    @InjectView(R.id.poster) ImageView mediaPoster;

    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;

    @InjectView(R.id.author) TextView mediaAuthor;
    @InjectView(R.id.version) TextView mediaVersion;

    @InjectView(R.id.media_description) TextView mediaDescription;

    /**
     * Create a new instance of this, initialized to show the addon addonId
     */
    public static AddonDetailsFragment newInstance(String addonId) {
        AddonDetailsFragment fragment = new AddonDetailsFragment();

        Bundle args = new Bundle();
        args.putString(ADDONID, addonId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        addonId = getArguments().getString(ADDONID, null);

        if ((container == null) || (addonId == null)) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_addon_details, container, false);
        ButterKnife.inject(this, root);

        hostManager = HostManager.getInstance(getActivity());
        hostInfo = hostManager.getHostInfo();

        // Setup dim the fanart when scroll changes. Full dim on 4 * iconSize dp
        Resources resources = getActivity().getResources();
        final int pixelsToTransparent  = 4 * resources.getDimensionPixelSize(R.dimen.default_icon_size);
        mediaPanel.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                float y = mediaPanel.getScrollY();
                float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
                mediaArt.setAlpha(newAlpha);
            }
        });

        FloatingActionButton fab = (FloatingActionButton)fabButton;
        fab.attachToScrollView((ObservableScrollView) mediaPanel);

        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), mediaPanel, false, false, true);
//        mediaPanel.setClipToPadding(false);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);

        // Get the addon details, this is done asyhnchronously
        String[] properties = new String[] {
                AddonType.Fields.NAME, AddonType.Fields.VERSION, AddonType.Fields.SUMMARY,
                AddonType.Fields.DESCRIPTION,  AddonType.Fields.PATH, AddonType.Fields.AUTHOR,
                AddonType.Fields.THUMBNAIL, AddonType.Fields.DISCLAIMER, AddonType.Fields.FANART,
                //AddonType.Fields.DEPENDENCIES, AddonType.Fields.BROKEN, AddonType.Fields.EXTRAINFO,
                AddonType.Fields.RATING, AddonType.Fields.ENABLED
        };
        Addons.GetAddonDetails action = new Addons.GetAddonDetails(addonId, properties);
        action.execute(hostManager.getConnection(), new ApiCallback<AddonType.Details>() {
            @Override
            public void onSuccess(AddonType.Details result) {
                if (!isAdded()) return;
                displayAddonDetails(result);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_addon_info), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putInt(ADDONID, addonId);
    }

    /**
     * Callbacks for button bar
     */
    @OnClick(R.id.fab)
    public void onFabClicked(View v) {
        Addons.ExecuteAddon action = new Addons.ExecuteAddon(addonId);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Do nothing
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }

    @OnClick(R.id.enable_disable)
    public void onEnabledClicked(View v) {
        final Boolean isEnabled = (Boolean)v.getTag();
        Addons.SetAddonEnabled action = new Addons.SetAddonEnabled(addonId, !isEnabled);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                int messageResId = (!isEnabled) ? R.string.addon_enabled : R.string.addon_disabled;
                Toast.makeText(getActivity(), messageResId, Toast.LENGTH_SHORT).show();
                setupEnableButton(!isEnabled);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.general_error_executing_action), description),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }, callbackHandler);
    }

    /**
     * Display the addon details
     *
     * @param addonDetails Addon details
     */
    private void displayAddonDetails(AddonType.Details addonDetails) {
        mediaTitle.setText(addonDetails.name);
        mediaUndertitle.setText(addonDetails.summary);

        mediaAuthor.setText(addonDetails.author);
        mediaVersion.setText(addonDetails.version);

        mediaDescription.setText(addonDetails.description);

        // Images
        Resources resources = getActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        if (!TextUtils.isEmpty(addonDetails.fanart)) {
            int posterWidth = resources.getDimensionPixelOffset(R.dimen.addondetail_poster_width);
            int posterHeight = resources.getDimensionPixelOffset(R.dimen.addondetail_poster_heigth);
            mediaPoster.setVisibility(View.VISIBLE);
            UIUtils.loadImageIntoImageview(hostManager,
                    addonDetails.thumbnail,
                    mediaPoster, posterWidth, posterHeight);
            UIUtils.loadImageIntoImageview(hostManager,
                    addonDetails.fanart,
                    mediaArt, artWidth, artHeight);
        } else {
            // No fanart, just present the poster
            mediaPoster.setVisibility(View.GONE);
            UIUtils.loadImageIntoImageview(hostManager,
                    addonDetails.thumbnail,
                    mediaArt, artWidth, artHeight);
            // Reset padding
            int paddingLeft = mediaTitle.getPaddingRight(),
                    paddingRight = mediaTitle.getPaddingRight(),
                    paddingTop = mediaTitle.getPaddingTop(),
                    paddingBottom = mediaTitle.getPaddingBottom();
            mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }

        setupEnableButton(addonDetails.enabled);
    }

    private void setupEnableButton(boolean enabled) {
        // Enabled button
        if (enabled) {
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorAccent});
            enabledButton.setColorFilter(styledAttributes.getColor(0, R.color.accent_default));
            styledAttributes.recycle();

            fabButton.setVisibility(View.VISIBLE);
        } else {
            enabledButton.clearColorFilter();
            fabButton.setVisibility(View.GONE);
        }
        enabledButton.setTag(enabled);
    }
}
