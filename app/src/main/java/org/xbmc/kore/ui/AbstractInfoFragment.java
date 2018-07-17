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
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
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

import at.blogc.android.views.ExpandableTextView;
import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.Unbinder;

import static android.view.View.GONE;

abstract public class AbstractInfoFragment extends AbstractFragment
        implements SwipeRefreshLayout.OnRefreshListener,
                   SyncUtils.OnServiceListener,
                   SharedElementTransition.SharedElement {
    private static final String TAG = LogUtils.makeLogTag(AbstractInfoFragment.class);

    private static final String BUNDLE_KEY_APIMETHOD_PENDING = "pending_apimethod";

    // Detail views
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.media_panel) NestedScrollView panelScrollView;
    @BindView(R.id.art) ImageView artImageView;
    @BindView(R.id.poster) ImageView posterImageView;
    @BindView(R.id.media_title) TextView titleTextView;
    @BindView(R.id.media_undertitle) TextView underTitleTextView;
    @BindView(R.id.rating_container) LinearLayout ratingContainer;
    @BindView(R.id.rating) TextView ratingTextView;
    @BindView(R.id.rating_votes) TextView ratingVotesTextView;
    @BindView(R.id.max_rating) TextView maxRatingTextView;
    @BindView(R.id.media_details_right) TextView detailsRightTextView;
    @BindView(R.id.media_details) LinearLayout mediaDetailsContainer;
    @BindView(R.id.media_action_download) ImageButton downloadButton;
    @BindView(R.id.media_action_pin_unpin) ImageButton pinUnpinButton;
    @BindView(R.id.media_action_add_to_playlist) ImageButton addToPlaylistButton;
    @BindView(R.id.media_action_seen) ImageButton seenButton;
    @BindView(R.id.media_action_go_to_imdb) ImageButton imdbButton;
    @BindView(R.id.media_actions_bar) LinearLayout mediaActionsBar;
    @BindView(R.id.media_description) ExpandableTextView descriptionExpandableTextView;
    @BindView(R.id.media_description_container) LinearLayout descriptionContainer;
    @BindView(R.id.show_all) ImageView expansionImage;
    @BindView(R.id.fab) FABSpeedDial fabButton;
    @BindView(R.id.exit_transition_view) View exitTransitionView;

    private HostManager hostManager;
    private HostInfo hostInfo;
    private ServiceConnection serviceConnection;
    private RefreshItem refreshItem;
    private boolean expandDescription;
    private int methodId;
    private Unbinder unbinder;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

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
        hostManager = HostManager.getInstance(getActivity());
        hostInfo = hostManager.getHostInfo();
    }

    @TargetApi(21)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_info, container, false);
        unbinder = ButterKnife.bind(this, root);

        Resources resources = getActivity().getResources();

        DataHolder dataHolder = getDataHolder();

        if(!dataHolder.getSquarePoster()) {
            posterImageView.getLayoutParams().width =
                    resources.getDimensionPixelSize(R.dimen.detail_poster_width_nonsquare);
            posterImageView.getLayoutParams().height =
                    resources.getDimensionPixelSize(R.dimen.detail_poster_height_nonsquare);
        }

        if(getRefreshItem() != null) {
            swipeRefreshLayout.setOnRefreshListener(this);
        } else {
            swipeRefreshLayout.setEnabled(false);
        }

        if(Utils.isLollipopOrLater()) {
            posterImageView.setTransitionName(dataHolder.getPosterTransitionName());
        }

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

        if(setupMediaActionBar()) {
            mediaActionsBar.setVisibility(View.VISIBLE);
        }

        if(setupFAB(fabButton)) {
            fabButton.setVisibility(View.VISIBLE);
        }

        updateView(dataHolder);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            int methodId = savedInstanceState.getInt(BUNDLE_KEY_APIMETHOD_PENDING);

            fabButton.enableBusyAnimation(HostManager.getInstance(getContext()).getConnection()
                       .updateClientCallback(methodId, createPlayItemOnKodiCallback(),
                                             callbackHandler));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
        exitTransitionView.setVisibility(View.INVISIBLE);
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
        SyncUtils.disconnectFromLibrarySyncService(getActivity(), serviceConnection);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(BUNDLE_KEY_APIMETHOD_PENDING, methodId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                onRefresh();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (getRefreshItem() == null) {
            Toast.makeText(getActivity(), R.string.Refreshing_not_implemented_for_this_item,
                           Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        refreshItem.setSwipeRefreshLayout(swipeRefreshLayout);
        refreshItem.startSync(false);
    }

    @Override
    public void onServiceConnected(LibrarySyncService librarySyncService) {
        if (getRefreshItem() == null) {
            return;
        }

        SyncItem syncItem = SyncUtils.getCurrentSyncItem(librarySyncService,
            HostManager.getInstance(getActivity()).getHostInfo(),
            refreshItem.getSyncType());
        if (syncItem != null) {
            boolean silentRefresh = (syncItem.getSyncExtras() != null) &&
                syncItem.getSyncExtras().getBoolean(LibrarySyncService.SILENT_SYNC, false);
            if (!silentRefresh)
                UIUtils.showRefreshAnimation(swipeRefreshLayout);
            refreshItem.setSwipeRefreshLayout(swipeRefreshLayout);
            refreshItem.register();
        }
    }

    protected void setFabButtonState(boolean enable) {
        if(enable) {
            fabButton.setVisibility(View.VISIBLE);
        } else {
            fabButton.setVisibility(GONE);
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

        fabButton.enableBusyAnimation(true);
        Player.Open action = new Player.Open(item);
        methodId = action.getId();
        action.execute(HostManager.getInstance(getActivity()).getConnection(),
                       createPlayItemOnKodiCallback(), callbackHandler);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.PERMISSION_REQUEST_WRITE_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.length > 0) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    downloadButton.performClick();
                } else {
                    Toast.makeText(getActivity(), R.string.write_storage_permission_denied, Toast.LENGTH_SHORT)
                         .show();
                }
                break;
        }
    }

    @Override
    @TargetApi(21)
    public boolean isSharedElementVisible() {
        return UIUtils.isViewInBounds(panelScrollView, posterImageView);
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
        titleTextView.setText(dataHolder.getTitle());
        underTitleTextView.setText(dataHolder.getUnderTitle());
        detailsRightTextView.setText(dataHolder.getDetails());

        if (!TextUtils.isEmpty(dataHolder.getDescription())) {
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[]{
                    R.attr.iconExpand,
                    R.attr.iconCollapse
            });
            final int iconCollapseResId =
                    styledAttributes.getResourceId(styledAttributes.getIndex(0), R.drawable.ic_expand_less_white_24dp);
            final int iconExpandResId =
                    styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_expand_more_white_24dp);
            styledAttributes.recycle();
            descriptionContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    descriptionExpandableTextView.toggle();
                    expansionImage.setImageResource(descriptionExpandableTextView.isExpanded() ? iconCollapseResId : iconExpandResId);
                }
            });
            descriptionExpandableTextView.setText(dataHolder.getDescription());
            if (expandDescription) {
                descriptionExpandableTextView.expand();
                expansionImage.setImageResource(iconExpandResId);
            }
            descriptionContainer.setVisibility(View.VISIBLE);
        } else {
            descriptionContainer.setVisibility(GONE);
        }

        // Images
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Resources resources = getActivity().getResources();

        if (dataHolder.getPosterUrl() != null) {
            int posterWidth;
            int posterHeight;
            if (dataHolder.getSquarePoster()) {
                posterWidth = resources.getDimensionPixelOffset(R.dimen.detail_poster_width_square);
                posterHeight = resources.getDimensionPixelOffset(R.dimen.detail_poster_height_square);
            } else {
                posterWidth = resources.getDimensionPixelOffset(R.dimen.detail_poster_width_nonsquare);
                posterHeight = resources.getDimensionPixelOffset(R.dimen.detail_poster_height_nonsquare);
            }

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                 dataHolder.getPosterUrl(), dataHolder.getTitle(),
                                                 posterImageView, posterWidth, posterHeight);
        } else {
            posterImageView.setVisibility(GONE);
            int padding = getActivity().getResources().getDimensionPixelSize(R.dimen.default_padding);
            titleTextView.setPadding(padding, padding, 0, 0);
            underTitleTextView.setPadding(padding, padding, 0, 0);
        }

        int artHeight = resources.getDimensionPixelOffset(R.dimen.detail_art_height);
        int artWidth = displayMetrics.widthPixels;

        UIUtils.loadImageIntoImageview(hostManager,
                                       TextUtils.isEmpty(dataHolder.getFanArtUrl()) ?
                                       dataHolder.getPosterUrl() : dataHolder.getFanArtUrl(),
                                       artImageView, artWidth, artHeight);

        if (dataHolder.getRating() > 0) {
            ratingTextView.setText(String.format(Locale.getDefault(), "%01.01f", dataHolder.getRating()));
            if (dataHolder.getMaxRating() > 0) {
                maxRatingTextView.setText(String.format(getString(R.string.max_rating),
                                                        String.valueOf(dataHolder.getMaxRating())));
            }
            if (dataHolder.getVotes() > 0 ) {
                ratingVotesTextView.setText(String.format(getString(R.string.votes),
                                                          String.valueOf(dataHolder.getVotes())));
            }
            ratingContainer.setVisibility(View.VISIBLE);
        } else if (TextUtils.isEmpty(dataHolder.getDetails())) {
            mediaDetailsContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Setting a listener for downloads will add the download button to the UI
     * @param listener to be called when user clicks the download button. Note that the View passed
     *                 into onClick from {@link android.view.View.OnClickListener} will be null
     *                 when the user is asked for storage permissions
     */
    protected void setOnDownloadListener(final View.OnClickListener listener) {
        downloadButton.setVisibility(View.VISIBLE);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkStoragePermission()) {
                    if (Settings.allowedDownloadNetworkTypes(getActivity()) != 0) {
                        listener.onClick(view);
                        UIUtils.highlightImageView(getActivity(), downloadButton, true);
                    } else {
                        Toast.makeText(getActivity(), R.string.no_connection_type_selected, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    protected void setOnAddToPlaylistListener(View.OnClickListener listener) {
        addToPlaylistButton.setVisibility(View.VISIBLE);
        addToPlaylistButton.setOnClickListener(listener);
    }

    protected void setOnGoToImdbListener(View.OnClickListener listener) {
        imdbButton.setVisibility(View.VISIBLE);
        imdbButton.setOnClickListener(listener);
    }

    /**
     * Use {@link #setSeenButtonState(boolean)} to set the state of the seen button
     * @param listener
     */
    protected void setOnSeenListener(final View.OnClickListener listener) {
        setupToggleButton(seenButton, listener);
    }

    protected void setOnPinClickedListener(final View.OnClickListener listener) {
        setupToggleButton(pinUnpinButton, listener);
    }

    /**
     * Uses colors to show to the user the item has been downloaded
     * @param state true if item has been downloaded, false otherwise
     */
    protected void setDownloadButtonState(boolean state) {
        UIUtils.highlightImageView(getActivity(), downloadButton, state);
    }

    /**
     * Uses colors to show the seen state to the user
     * @param state true if item has been watched/listened too, false otherwise
     */
    protected void setSeenButtonState(boolean state) {
        setToggleButtonState(seenButton, state);
    }

    protected void setPinButtonState(boolean state) {
        setToggleButtonState(pinUnpinButton, state);
    }

    private void setToggleButtonState(ImageButton button, boolean state) {
        UIUtils.highlightImageView(getActivity(), button, state);
        button.setTag(state);
    }

    private void setupToggleButton(final ImageButton button, final View.OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setTag(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(view);
                // Boldly invert the state. We depend on the observer to correct the state
                // if Kodi or other service didn't honour our request
                setToggleButtonState(button, ! (boolean) button.getTag());
            }
        });
    }

    private boolean checkStoragePermission() {
        boolean hasStoragePermission =
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (!hasStoragePermission) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                               Utils.PERMISSION_REQUEST_WRITE_STORAGE);
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
        return fabButton;
    }

    private ApiCallback<String> createPlayItemOnKodiCallback() {
        return new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                fabButton.enableBusyAnimation(false);

                // Check whether we should switch to the remote
                boolean switchToRemote = PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getBoolean(Settings.KEY_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START,
                                    Settings.DEFAULT_PREF_SWITCH_TO_REMOTE_AFTER_MEDIA_START);
                if (switchToRemote) {
                    int cx = (fabButton.getLeft() + fabButton.getRight()) / 2;
                    int cy = (fabButton.getTop() + fabButton.getBottom()) / 2;
                    UIUtils.switchToRemoteWithAnimation(getActivity(), cx, cy, exitTransitionView);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                fabButton.enableBusyAnimation(false);

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
     * @return
     */
    abstract protected RefreshItem createRefreshItem();

    /**
     * Called when the media action bar actions are available and
     * you can use {@link #setOnAddToPlaylistListener(View.OnClickListener)},
     * {@link #setOnSeenListener(View.OnClickListener)},
     * {@link #setOnDownloadListener(View.OnClickListener)},
     * {@link #setOnGoToImdbListener(View.OnClickListener)},
     * and {@link #setOnPinClickedListener(View.OnClickListener)} to enable
     * one or more actions.
     * @return true if media action bar should be visible, false otherwise
     */
    abstract protected boolean setupMediaActionBar();

    /**
     * Called when the fab button is available
     * @return true to enable the Floating Action Button, false otherwise
     */
    abstract protected boolean setupFAB(FABSpeedDial FAB);
}
