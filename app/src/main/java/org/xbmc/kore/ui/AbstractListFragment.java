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

import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.service.SyncUtils;
import org.xbmc.kore.utils.LogUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public abstract class AbstractListFragment extends Fragment
		implements LoaderManager.LoaderCallbacks<Cursor>,
		SyncUtils.OnServiceListener,
		SearchView.OnQueryTextListener,
		SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(AbstractListFragment.class);

	private ServiceConnection serviceConnection;
	private CursorAdapter adapter;

	private HostInfo hostInfo;
	private EventBus bus;

	// Loader IDs
	private static final int LOADER = 0;

	// The search filter to use in the loader
	private String searchFilter = null;

	private boolean isSyncing;

	@InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
	@InjectView(R.id.list) GridView gridView;
	@InjectView(android.R.id.empty) TextView emptyView;

	abstract protected AdapterView.OnItemClickListener createOnItemClickListener();
	abstract protected CursorAdapter createAdapter();
	abstract protected CursorLoader createCursorLoader();

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
		ButterKnife.inject(this, root);

		bus = EventBus.getDefault();
		HostManager hostManager = HostManager.getInstance(getActivity());
		hostInfo = hostManager.getHostInfo();

		swipeRefreshLayout.setOnRefreshListener(this);

		return root;
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		gridView.setEmptyView(emptyView);
		gridView.setOnItemClickListener(createOnItemClickListener());

		// Configure the adapter and start the loader
		adapter = createAdapter();
		gridView.setAdapter(adapter);

		getLoaderManager().initLoader(LOADER, null, this);

		setHasOptionsMenu(true);
	}

	@Override
	public void onStart() {
		super.onStart();
		serviceConnection = SyncUtils.connectToLibrarySyncService(getActivity(), this);
	}

	@Override
	public void onResume() {
		bus.register(this);
		super.onResume();
	}

	@Override
	public void onPause() {
		bus.unregister(this);
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		SyncUtils.disconnectFromLibrarySyncService(getActivity(), serviceConnection);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.refresh_item, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
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
	public void onRefresh() {
		if (hostInfo != null) {
			showRefreshAnimation();
			onSwipeRefresh();
		} else {
			swipeRefreshLayout.setRefreshing(false);
			Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
					.show();
		}
	}

	/**
	 * Should return the {@link org.xbmc.kore.service.LibrarySyncService} SyncType that
	 * this list initiates
	 * @return {@link org.xbmc.kore.service.LibrarySyncService} SyncType
	 */
	abstract protected String getListSyncType();

	/**
	 * Event bus post. Called when the syncing process ended
	 *
	 * @param event Refreshes data
	 */
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
			swipeRefreshLayout.setRefreshing(false);
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
        if(SyncUtils.isLibrarySyncing(
              librarySyncService,
              HostManager.getInstance(getActivity()).getHostInfo(),
              getListSyncType())) {
            showRefreshAnimation();
        }
    }

    protected void onSwipeRefresh() {
		LogUtils.LOGD(TAG, "Swipe, starting sync for: " + getListSyncType());
        // Start the syncing process
        Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
        syncIntent.putExtra(getListSyncType(), true);
        getActivity().startService(syncIntent);
    }

    /**
     * Search view callbacks
     */
	/** {@inheritDoc} */
	@Override
	public boolean onQueryTextChange(String newText) {
		searchFilter = newText;
		getLoaderManager().restartLoader(LOADER, null, this);
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
	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return createCursorLoader();
	}

	/** {@inheritDoc} */
	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		adapter.swapCursor(cursor);
		// To prevent the empty text from appearing on the first load, set it now
		emptyView.setText(getString(R.string.swipe_down_to_refresh));
	}

	/** {@inheritDoc} */
	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		adapter.swapCursor(null);
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
		getLoaderManager().restartLoader(LOADER, null, this);
	}

	public void showRefreshAnimation() {
		/**
		 * Fixes issue with refresh animation not showing when using appcompat library (from version 20?)
		 * See https://code.google.com/p/android/issues/detail?id=77712
		 */
		swipeRefreshLayout.post(new Runnable() {
			@Override
			public void run() {
				swipeRefreshLayout.setRefreshing(true);
			}
		});
	}
}
