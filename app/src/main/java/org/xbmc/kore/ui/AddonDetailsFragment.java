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

import android.annotation.TargetApi;
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
import org.xbmc.kore.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Presents addon details
 */
public class AddonDetailsFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(AddonDetailsFragment.class);

    public static final String BUNDLE_KEY_ADDONID = "addon_id";
    public static final String POSTER_TRANS_NAME = "POSTER_TRANS_NAME";
    public static final String BUNDLE_KEY_NAME = "name";
    public static final String BUNDLE_KEY_AUTHOR = "author";
    public static final String BUNDLE_KEY_SUMMARY = "summary";
    public static final String BUNDLE_KEY_VERSION = "version";
    public static final String BUNDLE_KEY_DESCRIPTION = "description";
    public static final String BUNDLE_KEY_FANART = "fanart";
    public static final String BUNDLE_KEY_POSTER = "poster";
    public static final String BUNDLE_KEY_ENABLED = "enabled";

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
    @TargetApi(21)
    public static AddonDetailsFragment newInstance(AddonListFragment.ViewHolder vh) {
        AddonDetailsFragment fragment = new AddonDetailsFragment();

        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_ADDONID, vh.addonId);
        args.putString(BUNDLE_KEY_NAME, vh.addonName);
        args.putString(BUNDLE_KEY_AUTHOR, vh.author);
        args.putString(BUNDLE_KEY_VERSION, vh.version);
        args.putString(BUNDLE_KEY_SUMMARY, vh.summary);
        args.putString(BUNDLE_KEY_DESCRIPTION, vh.description);
        args.putString(BUNDLE_KEY_FANART, vh.fanart);
        args.putString(BUNDLE_KEY_POSTER, vh.poster);
        args.putBoolean(BUNDLE_KEY_ENABLED, vh.enabled);

        if( Utils.isLollipopOrLater()) {
            args.putString(POSTER_TRANS_NAME, vh.artView.getTransitionName());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @TargetApi(21)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        addonId = bundle.getString(BUNDLE_KEY_ADDONID, null);

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

        if(Utils.isLollipopOrLater()) {
            mediaPoster.setTransitionName(bundle.getString(POSTER_TRANS_NAME));
        }

        mediaTitle.setText(bundle.getString(BUNDLE_KEY_NAME));
        mediaUndertitle.setText(bundle.getString(BUNDLE_KEY_SUMMARY));
        mediaAuthor.setText(bundle.getString(BUNDLE_KEY_AUTHOR));
        mediaVersion.setText(bundle.getString(BUNDLE_KEY_VERSION));
        mediaDescription.setText(bundle.getString(BUNDLE_KEY_DESCRIPTION));

        setImages(bundle.getString(BUNDLE_KEY_POSTER), bundle.getString(BUNDLE_KEY_FANART));

        setupEnableButton(bundle.getBoolean(BUNDLE_KEY_ENABLED, false));

        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), mediaPanel, false, false, true);
//        mediaPanel.setClipToPadding(false);

        return root;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        updateEnabledButton();
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
        final Boolean isEnabled = (v.getTag() == null)? false : (Boolean)v.getTag();

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

    private void setImages(String poster, String fanart) {
        Resources resources = getActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        int posterWidth = resources.getDimensionPixelOffset(R.dimen.addondetail_poster_width);
        int posterHeight = resources.getDimensionPixelOffset(R.dimen.addondetail_poster_height);

        UIUtils.loadImageIntoImageview(hostManager,
                                       TextUtils.isEmpty(fanart)? poster : fanart,
                                       mediaArt, artWidth, artHeight);
        UIUtils.loadImageIntoImageview(hostManager,
                                       poster,
                                       mediaPoster, posterWidth, posterHeight);

    }

    private void setupEnableButton(boolean enabled) {
        // Enabled button
        if (enabled) {
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[]{
                    R.attr.colorAccent});
            enabledButton.setColorFilter(styledAttributes.getColor(0,
                                                                   getActivity().getResources().getColor(R.color.accent_default)));
            styledAttributes.recycle();

            fabButton.setVisibility(View.VISIBLE);
        } else {
            enabledButton.clearColorFilter();
            fabButton.setVisibility(View.GONE);
        }
        enabledButton.setTag(enabled);
    }

    /**
     * Returns the shared element if visible
     * @return View if visible, null otherwise
     */
    public View getSharedElement() {
        if (UIUtils.isViewInBounds(mediaPanel, mediaPoster)) {
            return mediaPoster;
        }

        return null;
    }

    private void updateEnabledButton() {
        // Get the addon details, this is done asyhnchronously
        String[] properties = new String[] {
                AddonType.Fields.ENABLED
        };
        Addons.GetAddonDetails action = new Addons.GetAddonDetails(addonId, properties);
        action.execute(hostManager.getConnection(), new ApiCallback<AddonType.Details>() {
            @Override
            public void onSuccess(AddonType.Details result) {
                if (!isAdded()) return;
                setupEnableButton(result.enabled);
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
}
