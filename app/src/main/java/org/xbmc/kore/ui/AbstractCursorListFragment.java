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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.service.library.SyncItem;
import org.xbmc.kore.service.library.SyncUtils;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import org.greenrobot.eventbus.EventBus;

public abstract class AbstractCursorListFragment extends AbstractListFragment
		implements LoaderManager.LoaderCallbacks<Cursor>,
		SyncUtils.OnServiceListener,
		SearchView.OnQueryTextListener {
    private static final String TAG = LogUtils.makeLogTag(AbstractCursorListFragment.class);

	private final String BUNDLE_KEY_SEARCH_QUERY = "search_query";

	private ServiceConnection serviceConnection;

	private EventBus bus;

	// Loader IDs
	private static final int LOADER = 0;

	// The search filter to use in the loader
	private String searchFilter = null;
	private boolean loaderLoading;
	private String savedSearchFilter;
	private boolean supportsSearch;

	private SearchView searchView;
	private boolean isPaused;

	abstract protected void onListItemClicked(View view);
	abstract protected CursorLoader createCursorLoader();
	abstract protected RecyclerViewCursorAdapter createCursorAdapter();

	@TargetApi(16)
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		bus = EventBus.getDefault();

		if (savedInstanceState != null) {
			savedSearchFilter = savedInstanceState.getString(BUNDLE_KEY_SEARCH_QUERY);
		}
		searchFilter = savedSearchFilter;

		return root;
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();
		serviceConnection = SyncUtils.connectToLibrarySyncService(getActivity(), this);
	}

	@Override
	public void onResume() {
		// TODO: Eventbus
		bus.register(this);
		super.onResume();
		isPaused = false;
	}

	@Override
	public void onPause() {
		// TODO: Eventbus
		bus.unregister(this);
		super.onPause();
		isPaused = true;
	}

	@Override
	public void onStop() {
		super.onStop();
		SyncUtils.disconnectFromLibrarySyncService(getActivity(), serviceConnection);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (!TextUtils.isEmpty(searchFilter)) {
			savedSearchFilter = searchFilter;
		}
		outState.putString(BUNDLE_KEY_SEARCH_QUERY, savedSearchFilter);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener() {
		return (view, position) -> {
			saveSearchState();
			onListItemClicked(view);
		};
	}

	@Override
	final protected RecyclerView.Adapter createAdapter() {
		return createCursorAdapter();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.abstractcursorlistfragment, menu);

		if (supportsSearch) {
			setupSearchMenuItem(menu, inflater);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.action_refresh:
				onRefresh();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Should return the {@link LibrarySyncService} SyncType that
	 * this list initiates
	 * @return {@link LibrarySyncService} SyncType
	 */
	abstract protected String getListSyncType();

	/**
	 * Should return the {@link LibrarySyncService} syncID if this fragment
	 * synchronizes a single item. The itemId that should be synced must returned by {@link #getSyncItemID()}
	 * @return {@link LibrarySyncService} SyncID if syncing a single item. Null if not aplicable
	 */
	protected String getSyncID() {
		return null;
	}

	/**
	 * Should return the item ID for SyncID returned by {@link #getSyncID()}
	 * @return -1 if not used.
	 */
	protected int getSyncItemID() {
		return -1;
	}

	/**
	 * Event bus post. Called when the syncing process ended
	 *
	 * @param event Refreshes data
	 */
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEventMainThread(MediaSyncEvent event) {
		onSyncProcessEnded(event);
	}

	/**
	 * Called each time a MediaSyncEvent is received.
	 * @param event
	 */
	protected void onSyncProcessEnded(MediaSyncEvent event) {
        boolean silentSync = false;
        if (event.syncExtras != null) {
            silentSync = event.syncExtras.getBoolean(LibrarySyncService.SILENT_SYNC, false);
        }

		if (event.syncType.equals(getListSyncType())) {
			hideRefreshAnimation();
			if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
				refreshList();
                if (!silentSync) {
                    Toast.makeText(getActivity(), R.string.sync_successful, Toast.LENGTH_SHORT)
                        .show();
                }
            } else if (!silentSync) {
			    String msg = (event.errorCode == ApiException.API_ERROR) ?
					String.format(getString(R.string.error_while_syncing), event.errorMessage) :
					getString(R.string.unable_to_connect_to_xbmc);
				Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
			}
		}
	}

    @Override
    public void onServiceConnected(LibrarySyncService librarySyncService) {
        HostInfo hostInfo = HostManager.getInstance(getActivity()).getHostInfo();
        SyncItem syncItem = SyncUtils.getCurrentSyncItem(librarySyncService, hostInfo, getListSyncType());
        if (syncItem != null) {
            boolean silentRefresh = (syncItem.getSyncExtras() != null) &&
                syncItem.getSyncExtras().getBoolean(LibrarySyncService.SILENT_SYNC, false);
            if (!silentRefresh)
                UIUtils.showRefreshAnimation(binding.swipeRefreshLayout);
        }
    }

	/**
	 * Use this to indicate your fragment supports search queries.
	 * Get the entered search query using {@link #getSearchFilter()}
	 * <br/>
	 * Note: make sure this is set before {@link #onCreateOptionsMenu(Menu, MenuInflater)} is called.
	 * For instance in {@link #onAttach(Activity)}
	 * @param supportsSearch true if you support search queries, false otherwise
	 */
	public void setSupportsSearch(boolean supportsSearch) {
		this.supportsSearch = supportsSearch;
	}

	@Override
    public void onRefresh() {
		UIUtils.showRefreshAnimation(binding.swipeRefreshLayout);
		Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
        syncIntent.putExtra(getListSyncType(), true);

        String syncID = getSyncID();
        int itemId = getSyncItemID();
        if ((syncID != null) && (itemId != -1)) {
            syncIntent.putExtra(syncID, itemId);
        }

        getActivity().startService(syncIntent);
    }

    /**
     * Search view callbacks
     */
	/** {@inheritDoc} */
	@Override
	public boolean onQueryTextChange(String newText) {
		if ((!searchView.hasFocus()) && TextUtils.isEmpty(newText)) {
			//onQueryTextChange called as a result of manually expanding the searchView in setupSearchMenuItem(...)
			return true;
		}

		/**
		 * When this fragment is paused, onQueryTextChange is called with an empty string.
		 * This causes problems restoring the list fragment when returning. On return the fragment
		 * is recreated, which will cause the cursor adapter to be recreated. Although
		 * the search view will have the query restored to its saved state, the CursorAdapter
		 * will use the empty search filter. This is due to the fact that we don't restart the
		 * loader when it is still loading after its been created.
		 */
		if (isPaused)
			return true;

		searchFilter = newText;

		restartLoader();

		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean onQueryTextSubmit(String newText) {
		// All is handled in onQueryTextChange
		return true;
	}

	/**
	 * Loader callbacks
	 */
	/** {@inheritDoc} */
	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		loaderLoading = true;
		return createCursorLoader();
	}

	/** {@inheritDoc} */
	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
		((RecyclerViewCursorAdapter) getAdapter()).swapCursor(cursor);
		if (TextUtils.isEmpty(searchFilter)) {
			// To prevent the empty text from appearing on the first load, set it now
			getEmptyView().setText(getString(R.string.swipe_down_to_refresh));
		}
		loaderLoading = false;
	}

	/** {@inheritDoc} */
	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
		((RecyclerViewCursorAdapter) getAdapter()).swapCursor(null);
	}

	/**
	 * Save the search state of the list fragment
	 */
	public void saveSearchState() {
		savedSearchFilter = searchFilter;
	}

	/**
	 * @return text entered in searchview
	 */
	public String getSearchFilter() {
		return searchFilter;
	}

	/**
	 * Use this to reload the items in the list
	 */
	public void refreshList() {
		restartLoader();
	}

	private void setupSearchMenuItem(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.media_search, menu);
		MenuItem searchMenuItem = menu.findItem(R.id.action_search);
		if (searchMenuItem != null) {
			searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
			searchView.setOnQueryTextListener(this);
			searchView.setQueryHint(getString(R.string.action_search));
			if (!TextUtils.isEmpty(savedSearchFilter)) {
				searchMenuItem.expandActionView();
				searchView.setQuery(savedSearchFilter, false);
				//noinspection RestrictedApi
				searchView.clearFocus();
			}

			MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem item) {
					searchFilter = savedSearchFilter = null;
					restartLoader();
					return true;
				}
			});
		}

		//Handle clearing search query using the close button (X button).
		View view = searchView.findViewById(R.id.search_close_btn);
		if (view != null) {
			view.setOnClickListener(v -> {
				EditText editText = searchView.findViewById(R.id.search_src_text);
				editText.setText("");
				searchView.setQuery("", false);
				searchFilter = savedSearchFilter = "";
				restartLoader();
			});
		}
	}

	private void restartLoader() {
		//When loader is restarted while current loader hasn't finished yet,
		//it may result in none of the loaders finishing.
		if(!loaderLoading) {
			getLoaderManager().restartLoader(LOADER, null, this);
		}
	}
}
