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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.FragmentMediaListBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.ui.viewgroups.GridRecyclerView;
import org.xbmc.kore.utils.LogUtils;

import de.greenrobot.event.EventBus;

public abstract class AbstractListFragment
		extends AbstractFragment
		implements SwipeRefreshLayout.OnRefreshListener,
				   HostConnectionObserver.ConnectionStatusObserver {
	private static final String TAG = LogUtils.makeLogTag(AbstractListFragment.class);
	private static final String BUNDLE_KEY_HAS_NAVIGATED_TO_DETAILS = "BUNDLE_KEY_HAS_NAVIGATED_TO_DETAILS";

	private RecyclerView.Adapter<?> adapter;

	protected FragmentMediaListBinding binding;

	// Whether we've nacigated to the details view, maybe to force a refresh of this list on reenter
	protected boolean hasNavigatedToDetail = false;

	abstract protected void onListItemClicked(View view, int position);
	abstract protected GridRecyclerView.Adapter<?> createAdapter();
	abstract protected String getEmptyResultsTitle();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = createAdapter();
		if (savedInstanceState != null) {
			hasNavigatedToDetail = savedInstanceState.getBoolean(BUNDLE_KEY_HAS_NAVIGATED_TO_DETAILS);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentMediaListBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasOptionsMenu(true);

		binding.swipeRefreshLayout.setOnRefreshListener(this);
		binding.list.setEmptyView(binding.includeEmptyView.statusPanel);
		binding.list.setOnItemClickListener((v, position) -> {
			hasNavigatedToDetail = true;
			onListItemClicked(v, position);
		});

		if (PreferenceManager.getDefaultSharedPreferences(requireContext())
							 .getBoolean(Settings.KEY_PREF_SINGLE_COLUMN, Settings.DEFAULT_PREF_SINGLE_COLUMN)) {
			binding.list.setColumnCount(1);
		}

		binding.list.setAdapter(adapter);
		if (shouldPostponeReenterTransition) {
			// If this is set, we should be in a reenter transition, where we need to postpone the return shared
			// element transition, until the list is loaded
			postponeEnterTransition();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		HostManager.getInstance(requireContext())
				   .getHostConnectionObserver()
				   .registerConnectionStatusObserver(this);
	}

	@Override
	public void onStop() {
		HostManager.getInstance(requireContext())
				   .getHostConnectionObserver()
				   .unregisterConnectionStatusObserver(this);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(BUNDLE_KEY_HAS_NAVIGATED_TO_DETAILS, hasNavigatedToDetail);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.abstractlistfragment, menu);

		if(binding.list.isMultiColumnSupported()) {
			if (PreferenceManager
					.getDefaultSharedPreferences(requireContext())
					.getBoolean(Settings.KEY_PREF_SINGLE_COLUMN,
								Settings.DEFAULT_PREF_SINGLE_COLUMN)) {
				binding.list.setColumnCount(1);
				adapter.notifyDataSetChanged();

				MenuItem item = menu.findItem(R.id.action_multi_single_columns);
				item.setTitle(R.string.multi_column);
			}
		} else {
			//Disable menu item when mult-column is not supported
			MenuItem item = menu.findItem(R.id.action_multi_single_columns);
			item.setTitle(R.string.multi_column);
			item.setEnabled(false);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_multi_single_columns) {
			toggleAmountOfColumns(item);
		}
		return super.onOptionsItemSelected(item);
	}

	private void toggleAmountOfColumns(MenuItem item) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(requireContext()).edit();
		if (binding.list.getColumnCount() == 1) {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, false);
			item.setTitle(R.string.single_column);
			binding.list.setColumnCount(GridRecyclerView.AUTO_FIT);
		} else {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, true);
			item.setTitle(R.string.multi_column);
			binding.list.setColumnCount(1);
		}
		editor.apply();
		adapter.notifyDataSetChanged(); //force gridView to redraw
	}

	public void showRefreshAnimation() {
		binding.swipeRefreshLayout.setRefreshing(true);
	}

	public void hideRefreshAnimation() {
		binding.swipeRefreshLayout.setRefreshing(false);
	}

	public RecyclerView.Adapter<?> getAdapter() {
		return adapter;
	}

	/**
	 * Sets the information displayed by the empty view, whether to show an error or info situation or when
	 * there's no items to show on the adapter
	 * @param title Title
	 * @param message Message
	 */
	protected void setupEmptyView(String title, String message) {
		binding.includeEmptyView.statusTitle.setText(title);
		binding.includeEmptyView.statusMessage.setText(message);
	}

	/**
	 * Shows a status message on the info view, setting up a clickListener that refreshes the list
	 * @param title Title
	 * @param message Message
	 */
	protected void showStatusMessage(String title, String message) {
		binding.list.showEmptyView();
		setupEmptyView(title, message);
	}

	/**
	 * Call this when the list view has been fully setup, with all the items added, to notify any listeners
	 * that setup is complete, and give them the change to start any postponed transitions
	 */
	protected void notifyListSetupComplete() {
		EventBus.getDefault().post(new ListFragmentSetupComplete());
	}

	protected int lastConnectionStatusResult = CONNECTION_NO_RESULT;
	/**
	 * Disable Swipe refresh, hide the list and show an error message. By default, this is what make sense without a
	 * connection. Override in subclasses if this isn't the intended behaviour
	 */
	@Override
	public void onConnectionStatusError(int errorCode, String description) {
		lastConnectionStatusResult = CONNECTION_ERROR;
		binding.swipeRefreshLayout.setEnabled(false);
		HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
		showStatusMessage(getString(R.string.not_connected),
						  String.format(getString(R.string.connecting_to), hostInfo.getName(), hostInfo.getAddress()));
	}

	/**
	 * Enable swipe refresh and show the list when there's a connection
	 * In subclasses make sure you populate the list
	 */
	@Override
	public void onConnectionStatusSuccess() {
		// Only update views if transitioning from error state.
		// If transitioning from Sucess or No results the enabled UI is already being shown
		if (lastConnectionStatusResult == CONNECTION_ERROR) {
			binding.swipeRefreshLayout.setEnabled(true);
			binding.list.hideEmptyView();
		}
		// To prevent the empty text from appearing on the first load, only set it now
		setupEmptyView(getEmptyResultsTitle(), getString(R.string.pull_to_refresh));
		lastConnectionStatusResult = CONNECTION_SUCCESS;
	}

	@Override
	public void onConnectionStatusNoResultsYet() {
		// Do nothing, by default the enabled UI is shown while there are no results
		lastConnectionStatusResult = CONNECTION_NO_RESULT;
		showStatusMessage(getString(R.string.connecting), null);
	}
}
