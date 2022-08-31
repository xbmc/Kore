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
import android.animation.Animator;
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
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.FragmentMediaInfoBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.service.library.SyncItem;
import org.xbmc.kore.service.library.SyncUtils;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedElementTransition;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.Locale;

import de.greenrobot.event.EventBus;

abstract public class AbstractInfoFragment
        extends AbstractFragment
        implements SwipeRefreshLayout.OnRefreshListener,
                   SyncUtils.OnServiceListener,
                   SharedElementTransition.SharedElement,
                   HostConnectionObserver.ConnectionStatusObserver {
    private static final String TAG = LogUtils.makeLogTag(AbstractInfoFragment.class);

    private FragmentMediaInfoBinding binding;

    private HostManager hostManager;
    private HostInfo hostInfo;
    private ServiceConnection serviceConnection;
    private EventBus eventBus;
    private boolean expandDescription;
    private ViewTreeObserver.OnScrollChangedListener onScrollChangedListener;

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
                    UIUtils.showSnackbar(getView(), R.string.write_storage_permission_denied);
                }
            });

    /**
     * Set args with {@link DataHolder} to provide the required info after creating a new instance of this Fragment
     */
    public AbstractInfoFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(requireContext());
        hostInfo = hostManager.getHostInfo();
        eventBus = EventBus.getDefault();

        seenButtonLabels = new String[] { getString(R.string.unwatched_status), getString(R.string.watched_status) };
        pinButtonLabels = new String[] { getString(R.string.unpinned_status), getString(R.string.pinned_status) };
        enableButtonLabels = new String[] { getString(R.string.disabled_status), getString(R.string.enabled_status) };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMediaInfoBinding.inflate(inflater, container, false);

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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        Resources resources = requireActivity().getResources();
        DataHolder dataHolder = getDataHolder();
        if(!dataHolder.getSquarePoster()) {
            binding.poster.getLayoutParams().width = resources.getDimensionPixelSize(R.dimen.info_poster_width);
            binding.poster.getLayoutParams().height = resources.getDimensionPixelSize(R.dimen.info_poster_height);
        }
        binding.poster.setTransitionName(dataHolder.getPosterTransitionName());

        if(getSyncType() != null) {
            binding.swipeRefreshLayout.setOnRefreshListener(this);
        } else {
            binding.swipeRefreshLayout.setEnabled(false);
        }

        boolean hasButtons = setupInfoActionsBar();
        binding.infoActionsBar.setVisibility(hasButtons ? View.VISIBLE : View.GONE);

        boolean hasFAB = setupFAB(binding.fabPlay);
        binding.fabPlay.setVisibility(hasFAB? View.VISIBLE : View.GONE);

        /* Setup dim the fanart when scroll changes */
        onScrollChangedListener = UIUtils.createInfoPanelScrollChangedListener(requireContext(), binding.mediaPanel, binding.art, binding.mediaPanelGroup);
        binding.mediaPanel.getViewTreeObserver().addOnScrollChangedListener(onScrollChangedListener);
        updateView(dataHolder);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_item, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        hostManager.getHostConnectionObserver().registerConnectionStatusObserver(this);
        serviceConnection = SyncUtils.connectToLibrarySyncService(getActivity(), this);
    }

    @Override
    public void onResume() {
        super.onResume();
        eventBus.register(this);
        // Force the exit view to invisible
        binding.exitTransitionView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause() {
        eventBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onStop() {
        hostManager.getHostConnectionObserver().unregisterConnectionStatusObserver(this);
        SyncUtils.disconnectFromLibrarySyncService(requireContext(), serviceConnection);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding.mediaPanel.getViewTreeObserver().removeOnScrollChangedListener(onScrollChangedListener);
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putInt(BUNDLE_KEY_APIMETHOD_PENDING, methodId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            onRefresh();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (getSyncType() == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }
        startSync(false);
    }

    protected void startSync(boolean silentRefresh) {
        LogUtils.LOGD(TAG, "Starting sync. Silent? " + silentRefresh);

        if (hostInfo == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            UIUtils.showSnackbar(getView(), R.string.no_xbmc_configured);
            return;
        }

        if (!silentRefresh)
            binding.swipeRefreshLayout.setRefreshing(true);
        // Start the syncing process
        Intent syncIntent = new Intent(requireContext(), LibrarySyncService.class);
        syncIntent.putExtra(getSyncType(), true);
        Bundle syncExtras = getSyncExtras();
        if (syncExtras != null) {
            syncIntent.putExtras(syncExtras);
        }

        Bundle syncItemParams = new Bundle();
        syncItemParams.putBoolean(LibrarySyncService.SILENT_SYNC, silentRefresh);
        syncIntent.putExtra(LibrarySyncService.SYNC_ITEM_PARAMS, syncItemParams);

        requireContext().startService(syncIntent);
    }

    /**
     * Should return the {@link LibrarySyncService} SyncType that a refresh initiates.
     * Setting it to null disables syncing
     * @return {@link LibrarySyncService} SyncType
     */
    protected abstract String getSyncType();

    /**
     * Should return the extras to pass to syncing process. Specifically, if syncing a sinle item, should return
     * the {@link LibrarySyncService} syncID and itemId for the item.
     * @return Extras to pass to {@link LibrarySyncService}
     */
    protected Bundle getSyncExtras() {
        return null;
    }

    /**
     * Called when a sync process ends, overwrite to refresh information
     * @param event MediaSyncEvent that just ended
     */
    protected void onSyncProcessEnded(MediaSyncEvent event) { }

    @Override
    public void onServiceConnected(LibrarySyncService librarySyncService) {
        if (getSyncType() == null)
            return;

        SyncItem syncItem = SyncUtils.getCurrentSyncItem(librarySyncService,
                                                         hostManager.getHostInfo(),
                                                         getSyncType());
        if (syncItem != null) {
            boolean silentRefresh = syncItem.getSyncParams() != null &&
                                    syncItem.getSyncParams().getBoolean(LibrarySyncService.SILENT_SYNC, false);
            if (!silentRefresh)
                binding.swipeRefreshLayout.setRefreshing(true);
        }
    }

    /**
     * Event bus post. Called when the syncing process ended
     * @param event Media Sync Event
     */
    public void onEventMainThread(MediaSyncEvent event) {
        if (!isResumed() || !event.syncType.equals(getSyncType()))
            return;

        boolean silentSync = false;
        if (event.syncExtras != null) {
            silentSync = event.syncExtras.getBoolean(LibrarySyncService.SILENT_SYNC, false);
        }

        binding.swipeRefreshLayout.setRefreshing(false);
        onSyncProcessEnded(event);

        if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
            if (!silentSync) {
                UIUtils.showSnackbar(getView(), R.string.sync_successful);
            }
        } else if (!silentSync) {
            String msg = (event.errorCode == ApiException.API_ERROR) ?
                         String.format(getString(R.string.error_while_syncing), event.errorMessage) :
                         getString(R.string.unable_to_connect_to_xbmc);
            UIUtils.showSnackbar(getView(), msg);
        }
    }

    private int lastConnectionStatusResult = CONNECTION_NO_RESULT;
    /**
     * Hide/Disable UI elements that don't make sense without a connection
     */
    @Override
    public void onConnectionStatusError(int errorCode, String description) {
        LogUtils.LOGD(TAG, "Connection Status Error, disabling buttons");
        lastConnectionStatusResult = CONNECTION_ERROR;
        binding.fabPlay.setEnabled(false);
        if (binding.fabPlay.isShown())
            binding.fabPlay.hide();
        binding.swipeRefreshLayout.setEnabled(false);
        setInfoActionButtonsEnabledState(false);
    }

    /**
     * Show/Enable UI elements relevant when there's a connection
     */
    @Override
    public void onConnectionStatusSuccess() {
        // Only update views if transitioning from error state.
        // If transitioning from Sucess or No results the enabled UI is already being shown
        if (lastConnectionStatusResult == CONNECTION_ERROR) {
            LogUtils.LOGD(TAG, "Connection Status Success, enabling buttons");
            updateView(getDataHolder());

            binding.fabPlay.setEnabled(true);
            if (!binding.fabPlay.isShown())
                binding.fabPlay.show();
            binding.swipeRefreshLayout.setEnabled(true);
            setInfoActionButtonsEnabledState(true);
        }
        lastConnectionStatusResult = CONNECTION_SUCCESS;
    }

    @Override
    public void onConnectionStatusNoResultsYet() {
        // Do nothing, by default the enabled UI is shown while there are no results
        lastConnectionStatusResult = CONNECTION_NO_RESULT;
    }

    private void setInfoActionButtonsEnabledState(boolean enabled) {
        setButtonEnabledState(binding.infoActionDownload, enabled);
        setButtonEnabledState(binding.infoActionEnable, enabled);
        setButtonEnabledState(binding.infoActionPin, enabled);
        setButtonEnabledState(binding.infoActionStream, enabled);
        setButtonEnabledState(binding.infoActionQueue, enabled);
        setButtonEnabledState(binding.infoActionWatched, enabled);
    }

    private void setButtonEnabledState(MaterialButton button, boolean enabled) {
        if (button.getVisibility() == View.VISIBLE) {
            button.setEnabled(enabled);
        }
    }

    protected void setFabState(boolean enabled) {
        binding.fabPlay.setEnabled(enabled);
    }

    protected void streamItemFromKodi(String url, String type) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, type);
        startActivity(intent);
    }

    protected void playItemOnKodi(PlaylistType.Item item) {
        if (item == null) {
            UIUtils.showSnackbar(getView(), R.string.no_item_available_to_play);
            return;
        }

        new Player.Open(item).execute(
                HostManager.getInstance(requireContext()).getConnection(),
                new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        if (!isAdded()) return;
                        animateSwitchToRemote();
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        if (!isResumed()) return;
                        UIUtils.showSnackbar(getView(), R.string.unable_to_connect_to_xbmc);
                    }
                },
                callbackHandler);
    }

    @Override
    public boolean isSharedElementVisible() {
        return UIUtils.isViewInBounds(binding.mediaPanel, binding.poster);
    }

    /**
     * Check wether the remote should be shown and animate the switch to it
     */
    protected void animateSwitchToRemote() {
        boolean switchToRemote = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(Settings.KEY_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START,
                            Settings.DEFAULT_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START);
        if (switchToRemote) {
            int cx = (binding.fabPlay.getLeft() + binding.fabPlay.getRight()) / 2;
            int cy = (binding.fabPlay.getTop() + binding.fabPlay.getBottom()) / 2;
            final Intent launchIntent = new Intent(requireContext(), RemoteActivity.class);

            // Show the animation
            int endRadius = Math.max(binding.exitTransitionView.getHeight(), binding.exitTransitionView.getWidth());
            Animator exitAnim = ViewAnimationUtils.createCircularReveal(binding.exitTransitionView,
                                                                        cx, cy, 0, endRadius);
            exitAnim.setDuration(200);
            exitAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) { requireContext().startActivity(launchIntent );}

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            binding.exitTransitionView.setVisibility(View.VISIBLE);
            exitAnim.start();
        }
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
     * Call this when ready to provide the title, undertitle, details, description, etc.
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

        if (!TextUtils.isEmpty(dataHolder.getPosterUrl())) {
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
        int artWidth = binding.art.getWidth(); // displayMetrics.widthPixels;

        UIUtils.loadImageIntoImageview(hostManager,
                                       TextUtils.isEmpty(dataHolder.getFanArtUrl()) ?
                                       dataHolder.getPosterUrl() : dataHolder.getFanArtUrl(),
                                       binding.art, artWidth, artHeight);

        if (!TextUtils.isEmpty(dataHolder.getSearchTerms())) {
            binding.poster.setOnClickListener(v -> {
                Utils.launchWebSearchForTerms(requireContext(), dataHolder.getSearchTerms());
            });
        }

        int sectionVisibility;
        // Description
        if (!TextUtils.isEmpty(dataHolder.getDescription())) {
            sectionVisibility = View.VISIBLE;

            final int iconCollapseResId = R.drawable.ic_round_expand_more_24;
            final int iconExpandResId = R.drawable.ic_round_expand_less_24;
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
            if (dataHolder.getVotes() != null) {
                binding.ratingVotes.setText(String.format(getString(R.string.votes), dataHolder.getVotes()));
            }
        } else {
            sectionVisibility = View.GONE;
        }
        binding.rating.setVisibility(sectionVisibility);
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
                    UIUtils.showSnackbar(getView(), R.string.no_connection_type_selected);
                }
            }
        });
    }

    /**
     * Listener to set in the Stream button. Setting this will add the button to the UI
     * @param listener Click listener to call
     */
    protected void setOnStreamClickListener(View.OnClickListener listener) {
        binding.infoActionStream.setVisibility(View.VISIBLE);
        binding.infoActionStream.setOnClickListener(listener);
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

    protected void setExpandDescription(boolean expandDescription) {
        this.expandDescription = expandDescription;
    }

    abstract protected AbstractAdditionalInfoFragment getAdditionalInfoFragment();

    /**
     * Called when the media action bar actions are available and
     * you can use {@link #setOnQueueClickListener(View.OnClickListener)},
     * {@link #setOnWatchedClickListener(View.OnClickListener)},
     * {@link #setOnDownloadClickListener(View.OnClickListener)},
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
    abstract protected boolean setupFAB(FloatingActionButton fab);
}
