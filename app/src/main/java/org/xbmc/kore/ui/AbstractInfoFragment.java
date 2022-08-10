/*
 * Copyright 2015 Martijn Brekhof. All rights reserved.
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.FragmentMediaInfoBinding;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.service.library.SyncItem;
import org.xbmc.kore.service.library.SyncUtils;
import org.xbmc.kore.ui.generic.RefreshItem;
import org.xbmc.kore.ui.widgets.fabspeeddial.FABSpeedDial;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedElementTransition;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.Locale;

abstract public class AbstractInfoFragment extends AbstractFragment
        implements SwipeRefreshLayout.OnRefreshListener,
                   SyncUtils.OnServiceListener,
                   SharedElementTransition.SharedElement,
                   ViewTreeObserver.OnScrollChangedListener {
    private static final String TAG = LogUtils.makeLogTag(AbstractInfoFragment.class);

    private static final String BUNDLE_KEY_APIMETHOD_PENDING = "pending_apimethod";

    private FragmentMediaInfoBinding binding;

    private HostManager hostManager;
    private HostInfo hostInfo;
    private ServiceConnection serviceConnection;
    private RefreshItem refreshItem;
    private boolean expandDescription;
    private int methodId; // Last Kodi Open method id executed
    private int pixelsToTransparent;

    protected String[] seenButtonLabels;
    protected String[] pinButtonLabels;
    protected String[] enableButtonLabels;

    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    // Permission check callback
    private final ActivityResultLauncher<String> downloadFilesPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    binding.infoActionDownload.performClick();
                } else {
                    Toast.makeText(getActivity(), R.string.write_storage_permission_denied, Toast.LENGTH_SHORT)
                         .show();
                }
            });

    /**
     * Use {@link #setDataHolder(DataHolder)}
     * to provide the required info after creating a new instance of this Fragment
     */
    public AbstractInfoFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(requireContext());
        hostInfo = hostManager.getHostInfo();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We're not being shown or there's nothing to show
            return null;
        }
        binding = FragmentMediaInfoBinding.inflate(inflater, container, false);

        Resources resources = requireActivity().getResources();

        DataHolder dataHolder = getDataHolder();

        if(!dataHolder.getSquarePoster()) {
            binding.poster.getLayoutParams().width =
                    resources.getDimensionPixelSize(R.dimen.info_poster_width);
            binding.poster.getLayoutParams().height =
                    resources.getDimensionPixelSize(R.dimen.info_poster_height);
        }

        if(getRefreshItem() != null) {
            binding.swipeRefreshLayout.setOnRefreshListener(this);
        } else {
            binding.swipeRefreshLayout.setEnabled(false);
        }

        binding.poster.setTransitionName(dataHolder.getPosterTransitionName());

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getChildFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.media_additional_info);
            if (fragment == null) {
                fragment = getAdditionalInfoFragment();
                if (fragment != null) {
                    fragmentManager.beginTransaction()
                                   .add(R.id.media_additional_info, fragment)
                                   .commit();
                }
            }
        }

        seenButtonLabels = new String[] { getString(R.string.unwatched_status), getString(R.string.watched_status) };
        pinButtonLabels = new String[] { getString(R.string.unpinned_status), getString(R.string.pinned_status) };
        enableButtonLabels = new String[] { getString(R.string.disabled_status), getString(R.string.enabled_status) };

        boolean hasButtons = setupInfoActionsBar();
        binding.infoActionsBar.setVisibility(hasButtons ? View.VISIBLE : View.GONE);

        if(setupFAB(binding.fab)) {
            binding.fab.setVisibility(View.VISIBLE);
        }

        updateView(dataHolder);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        if ((savedInstanceState != null) &&
            (savedInstanceState.containsKey(BUNDLE_KEY_APIMETHOD_PENDING))) {
            binding.fab.enableBusyAnimation(HostManager.getInstance(requireContext()).getConnection()
                       .updateClientCallback(savedInstanceState.getInt(BUNDLE_KEY_APIMETHOD_PENDING),
                                             createPlayItemOnKodiCallback(),
                                             callbackHandler));
        }

        /* Setup dim the fanart when scroll changes */
        pixelsToTransparent  = requireActivity().getResources().getDimensionPixelSize(R.dimen.info_art_height);
        binding.mediaPanel.getViewTreeObserver().addOnScrollChangedListener(this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_item, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        serviceConnection = SyncUtils.connectToLibrarySyncService(getActivity(), this);
    }

    @Override
    public void onResume() {
        // Force the exit view to invisible
        binding.exitTransitionView.setVisibility(View.INVISIBLE);
        if ( refreshItem != null ) {
            refreshItem.register();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if ( refreshItem != null ) {
            refreshItem.unregister();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        SyncUtils.disconnectFromLibrarySyncService(requireContext(), serviceConnection);
    }

    @Override
    public void onDestroyView() {
        binding.mediaPanel.getViewTreeObserver().removeOnScrollChangedListener(this);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(BUNDLE_KEY_APIMETHOD_PENDING, methodId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            onRefresh();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onScrollChanged() {
        if (binding == null) return;
        float y = binding.mediaPanel.getScrollY();
        float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
        binding.art.setAlpha(newAlpha);
    }

    /*
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (getRefreshItem() == null) {
            Toast.makeText(getActivity(), R.string.Refreshing_not_implemented_for_this_item,
                           Toast.LENGTH_SHORT).show();
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        refreshItem.setSwipeRefreshLayout(binding.swipeRefreshLayout);
        refreshItem.startSync(false);
    }

    @Override
    public void onServiceConnected(LibrarySyncService librarySyncService) {
        if (getRefreshItem() == null) {
            return;
        }

        SyncItem syncItem = SyncUtils.getCurrentSyncItem(librarySyncService,
            HostManager.getInstance(requireContext()).getHostInfo(),
            refreshItem.getSyncType());
        if (syncItem != null) {
            boolean silentRefresh = (syncItem.getSyncExtras() != null) &&
                syncItem.getSyncExtras().getBoolean(LibrarySyncService.SILENT_SYNC, false);
            if (!silentRefresh)
                UIUtils.showRefreshAnimation(binding.swipeRefreshLayout);
            refreshItem.setSwipeRefreshLayout(binding.swipeRefreshLayout);
            refreshItem.register();
        }
    }

    protected void setFabButtonState(boolean enable) {
        if(enable) {
            binding.fab.setVisibility(View.VISIBLE);
        } else {
            binding.fab.setVisibility(View.GONE);
        }
    }

    protected void playItemLocally(String url, String type) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, type);
        startActivity(intent);
    }

    protected void playItemOnKodi(PlaylistType.Item item) {
        if (item == null) {
            Toast.makeText(getActivity(), R.string.no_item_available_to_play, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.fab.enableBusyAnimation(true);
        Player.Open action = new Player.Open(item);
        methodId = action.getId();
        action.execute(HostManager.getInstance(requireContext()).getConnection(),
                       createPlayItemOnKodiCallback(),
                       callbackHandler);
    }

    @Override
    public boolean isSharedElementVisible() {
        return UIUtils.isViewInBounds(binding.mediaPanel, binding.poster);
    }

    protected void refreshAdditionInfoFragment() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.media_additional_info);
        if (fragment != null)
            ((AbstractAdditionalInfoFragment) fragment).refresh();
    }

    protected HostManager getHostManager() {
        return hostManager;
    }

    protected HostInfo getHostInfo() {
        return hostInfo;
    }

    /**
     * Call this when you are ready to provide the titleTextView, undertitle, details, descriptionExpandableTextView, etc. etc.
     */
    @SuppressLint("StringFormatInvalid")
    protected void updateView(DataHolder dataHolder) {
        binding.mediaTitle.setText(dataHolder.getTitle());
        binding.mediaTitle.post(UIUtils.getMarqueeToggleableAction(binding.mediaTitle));
        binding.mediaUndertitle.setText(dataHolder.getUnderTitle());

        // Images
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Resources resources = requireActivity().getResources();

        if (dataHolder.getPosterUrl() != null) {
            binding.poster.setVisibility(View.VISIBLE);
            int posterWidth;
            int posterHeight;
            if (dataHolder.getSquarePoster()) {
                posterWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_width_square);
                posterHeight = resources.getDimensionPixelOffset(R.dimen.info_poster_height_square);
            } else {
                posterWidth = resources.getDimensionPixelOffset(R.dimen.info_poster_width);
                posterHeight = resources.getDimensionPixelOffset(R.dimen.info_poster_height);
            }

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 dataHolder.getPosterUrl(), dataHolder.getTitle(),
                                                 binding.poster, posterWidth, posterHeight);
        } else {
            binding.poster.setVisibility(View.GONE);
            int padding = requireContext().getResources().getDimensionPixelSize(R.dimen.default_padding);
            binding.mediaTitle.setPadding(padding, padding, 0, 0);
            binding.mediaUndertitle.setPadding(padding, padding, 0, 0);
        }

        int artHeight = resources.getDimensionPixelOffset(R.dimen.info_art_height);
        int artWidth = displayMetrics.widthPixels;

        UIUtils.loadImageIntoImageview(hostManager,
                                       TextUtils.isEmpty(dataHolder.getFanArtUrl()) ?
                                       dataHolder.getPosterUrl() : dataHolder.getFanArtUrl(),
                                       binding.art, artWidth, artHeight);

        int sectionVisibility;
        // Description
        if (!TextUtils.isEmpty(dataHolder.getDescription())) {
            sectionVisibility = View.VISIBLE;

            final int iconCollapseResId = R.drawable.ic_expand_more_white_24dp;
            final int iconExpandResId = R.drawable.ic_expand_less_white_24dp;
            binding.mediaDescription.setOnClickListener(v -> {
                binding.mediaDescription.toggle();
                binding.showAll.setImageResource(binding.mediaDescription.isExpanded() ? iconCollapseResId : iconExpandResId);
            });
            binding.mediaDescription.setText(dataHolder.getDescription());
            if (expandDescription) {
                binding.mediaDescription.expand();
                binding.showAll.setImageResource(iconExpandResId);
            }
        } else {
            sectionVisibility = View.GONE;
        }
        binding.mediaDescription.setVisibility(sectionVisibility);
        binding.showAll.setVisibility(sectionVisibility);

        // Rating and details
        if (dataHolder.getRating() > 0) {
            sectionVisibility = View.VISIBLE;

            binding.rating.setText(String.format(Locale.getDefault(), "%01.01f", dataHolder.getRating()));
            if (dataHolder.getMaxRating() > 0) {
                binding.maxRating.setText(String.format(getString(R.string.max_rating),
                                                        String.valueOf(dataHolder.getMaxRating())));
            }
            if (dataHolder.getVotes() > 0 ) {
                binding.ratingVotes.setText(String.format(getString(R.string.votes),
                                                          String.valueOf(dataHolder.getVotes())));
            }
        } else {
            sectionVisibility = View.GONE;
        }
        binding.rating.setVisibility(sectionVisibility);
        binding.maxRating.setVisibility(sectionVisibility);
        binding.ratingVotes.setVisibility(sectionVisibility);

        if (!TextUtils.isEmpty(dataHolder.getDetails())) {
            sectionVisibility = View.VISIBLE;
            binding.mediaDetailsRight.setText(dataHolder.getDetails());
        } else {
            sectionVisibility = View.GONE;
        }
        binding.mediaDetailsRight.setVisibility(sectionVisibility);

        // Dividers
        if (binding.infoActionsBar.getVisibility() == View.VISIBLE) {
            binding.divider1.setVisibility(binding.rating.getVisibility());
            binding.divider2.setVisibility(binding.mediaDescription.getVisibility());
        } else {
            binding.divider1.setVisibility(View.GONE);
            binding.divider2.setVisibility(View.GONE);
        }
    }

    /**
     * Listener to set in the Download button. Setting this will add the button to the UI
     * @param listener Click listener to be called when user clicks the download button.
     * Use {@link #setDownloadButtonState(boolean)} to set the state of the button
     */
    protected void setOnDownloadClickListener(final View.OnClickListener listener) {
        binding.infoActionDownload.setVisibility(View.VISIBLE);
        binding.infoActionDownload.setOnClickListener(view -> {
            if (checkStoragePermission()) {
                if (Settings.allowedDownloadNetworkTypes(getActivity()) != 0) {
                    listener.onClick(view);
                    setToggleButtonState(binding.infoActionDownload, true);
                } else {
                    Toast.makeText(getActivity(), R.string.no_connection_type_selected, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Listener to set in the Queue button. Setting this will add the button to the UI
     * @param listener Click listener to call
     */
    protected void setOnQueueClickListener(View.OnClickListener listener) {
        binding.infoActionQueue.setVisibility(View.VISIBLE);
        binding.infoActionQueue.setOnClickListener(listener);
    }

    /**
     * Listener to set in the IMDb button. Setting this will add the button to the UI
     * @param listener Click listener to call
     */
    protected void setOnImdbClickListener(View.OnClickListener listener) {
        binding.infoActionImdb.setVisibility(View.VISIBLE);
        binding.infoActionImdb.setOnClickListener(listener);
    }

    /**
     * Listener to set in the Watched button. Setting this will add the button to the UI
     * Use {@link #setWatchedButtonState(boolean)} to set the state of the button
     * @param listener Click listener to call
     */
    protected void setOnWatchedClickListener(final View.OnClickListener listener) {
        setupToggleButton(binding.infoActionWatched, listener);
    }

    /**
     * Listener to set in the Pin button. Setting this will add the button to the UI
     * Use {@link #setPinButtonState(boolean)} to set the state of the button
     * @param listener Click listener to call
     */
    protected void setOnPinClickedListener(final View.OnClickListener listener) {
        setupToggleButton(binding.infoActionPin, listener);
    }

    /**
     * Listener to set in the Enable button. Setting this will add the button to the UI
     * Use {@link #setEnableButtonState(boolean)} to set the state of the button
     * @param listener Click listener to call
     */
    protected void setOnEnableClickListener(final View.OnClickListener listener) {
        setupToggleButton(binding.infoActionEnable, listener);
    }

    private void setupToggleButton(final MaterialButton button, final View.OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setTag(false);
        button.setOnClickListener(listener);
    }

    /**
     * Set the state of the Download button
     * @param state true if item has been downloaded, false otherwise
     */
    protected void setDownloadButtonState(boolean state) {
        setToggleButtonState(binding.infoActionDownload, state);
    }

    /**
     * Set the state of the Watched button
     * @param state true if item has been watched/listened too, false otherwise
     */
    protected void setWatchedButtonState(boolean state) {
        binding.infoActionWatched.setText(seenButtonLabels[state ? 1 : 0]);
        setToggleButtonState(binding.infoActionWatched, state);
    }

    /**
     * Set the state of the Pin button
     * @param state true if item has been pinned, false otherwise
     */
    protected void setPinButtonState(boolean state) {
        binding.infoActionPin.setText(pinButtonLabels[state ? 1 : 0]);
        setToggleButtonState(binding.infoActionPin, state);
    }

    /**
     * Set the state of the Enable button
     * @param state true if item has been enabled, false otherwise
     */
    protected void setEnableButtonState(boolean state) {
        binding.infoActionEnable.setText(enableButtonLabels[state ? 1 : 0]);
        setToggleButtonState(binding.infoActionEnable, state);
    }

    private void setToggleButtonState(MaterialButton button, boolean state) {
        button.setChecked(state);
        button.setTag(state);
    }

    private boolean checkStoragePermission() {
        // R or later (API Level 30+) doesn't need WRITE_EXTERNAL_STORAGE to write on the default external public dirs
        // Up until Q (API Level 29), we opt out of scoped storage and explicitly ask for `permission.WRITE_EXTERNAL_STORAGE`
        boolean hasStoragePermission =
                Utils.isROrLater() ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (!hasStoragePermission) {
            downloadFilesPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return false;
        }

        return true;
    }

    protected RefreshItem getRefreshItem() {
        if (refreshItem == null) {
            refreshItem = createRefreshItem();
        }
        return refreshItem;
    }

    protected void setExpandDescription(boolean expandDescription) {
        this.expandDescription = expandDescription;
    }

    public FABSpeedDial getFabButton() {
        return binding.fab;
    }

    private ApiCallback<String> createPlayItemOnKodiCallback() {
        return new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                binding.fab.enableBusyAnimation(false);

                // Check whether we should switch to the remote
                boolean switchToRemote = PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getBoolean(Settings.KEY_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START,
                                    Settings.DEFAULT_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START);
                if (switchToRemote) {
                    int cx = (binding.fab.getLeft() + binding.fab.getRight()) / 2;
                    int cy = (binding.fab.getTop() + binding.fab.getBottom()) / 2;
                    UIUtils.switchToRemoteWithAnimation(getActivity(), cx, cy, binding.exitTransitionView);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                binding.fab.enableBusyAnimation(false);

                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        };
    }

    abstract protected AbstractAdditionalInfoFragment getAdditionalInfoFragment();

    /**
     * Called when user commands the information to be renewed. Either through a swipe down
     * or a menu call.
     * <br/>
     * Note, that {@link AbstractAdditionalInfoFragment#refresh()} will be called for an
     * additional fragment, if available, automatically.
     */
    abstract protected RefreshItem createRefreshItem();

    /**
     * Called when the media action bar actions are available and
     * you can use {@link #setOnQueueClickListener(View.OnClickListener)},
     * {@link #setOnWatchedClickListener(View.OnClickListener)},
     * {@link #setOnDownloadClickListener(View.OnClickListener)},
     * {@link #setOnImdbClickListener(View.OnClickListener)},
     * {@link #setOnEnableClickListener(View.OnClickListener)},
     * and {@link #setOnPinClickedListener(View.OnClickListener)} to enable
     * one or more actions.
     * @return true if media action bar should be visible, false otherwise
     */
    abstract protected boolean setupInfoActionsBar();

    /**
     * Called when the fab button is available
     * @return true to enable the Floating Action Button, false otherwise
     */
    abstract protected boolean setupFAB(FABSpeedDial FAB);
}
