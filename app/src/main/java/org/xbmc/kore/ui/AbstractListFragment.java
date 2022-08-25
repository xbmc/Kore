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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.FragmentMediaListBinding;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.LogUtils;

public abstract class AbstractListFragment
		extends Fragment
		implements SwipeRefreshLayout.OnRefreshListener,
				   HostConnectionObserver.ConnectionStatusObserver {
	private static final String TAG = LogUtils.makeLogTag(AbstractListFragment.class);
	private RecyclerView.Adapter<?> adapter;

	protected FragmentMediaListBinding binding;

	abstract protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener();
	abstract protected RecyclerViewEmptyViewSupport.Adapter<?> createAdapter();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = createAdapter();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentMediaListBinding.inflate(inflater, container, false);

		binding.swipeRefreshLayout.setOnRefreshListener(this);

		binding.list.setEmptyView(binding.includeEmptyView.empty);
		binding.list.setOnItemClickListener(createOnItemClickListener());

		if (PreferenceManager
				.getDefaultSharedPreferences(requireContext())
				.getBoolean(Settings.KEY_PREF_SINGLE_COLUMN,
							Settings.DEFAULT_PREF_SINGLE_COLUMN)) {
			binding.list.setColumnCount(1);
		}

		binding.list.setAdapter(adapter);

		setHasOptionsMenu(true);

		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		HostManager.getInstance(requireContext())
				   .getHostConnectionObserver()
				   .registerConnectionStatusObserver(this);
	}

	@Override
	public void onDestroyView() {
		HostManager.getInstance(requireContext())
				   .getHostConnectionObserver()
				   .unregisterConnectionStatusObserver(this);
		super.onDestroyView();
		binding = null;
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
			binding.list.setColumnCount(RecyclerViewEmptyViewSupport.AUTO_FIT);
		} else {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, true);
			item.setTitle(R.string.multi_column);
			binding.list.setColumnCount(1);
		}
		editor.apply();
		adapter.notifyDataSetChanged(); //force gridView to redraw
	}

	public void hideRefreshAnimation() {
		binding.swipeRefreshLayout.setRefreshing(false);
	}

	public RecyclerView.Adapter<?> getAdapter() {
		return adapter;
	}

	/**
	 * Returns the view that is displayed when the gridview has no items to show
	 * @return Empty view
	 */
	public TextView getEmptyView() {
		return binding.includeEmptyView.empty;
	}


	private int lastConnectionStatusResult = CONNECTION_NO_RESULT;
	/**
	 * Disable Swipe refresh as, by default it doesn't make sense without a connection
	 * Override in subclasses if this isn't the intended behaviour
	 */
	@Override
	public void connectionStatusOnError(int errorCode, String description) {
		lastConnectionStatusResult = CONNECTION_ERROR;
		binding.swipeRefreshLayout.setEnabled(false);
	}

	/**
	 * Enable swipe refresh when there's a connection
	 */
	@Override
	public void connectionStatusOnSuccess() {
		// Only update views if transitioning from error state.
		// If transitioning from Sucess or No results the enabled UI is already being shown
		if (lastConnectionStatusResult == CONNECTION_ERROR) {
			binding.swipeRefreshLayout.setEnabled(true);
		}
		lastConnectionStatusResult = CONNECTION_SUCCESS;
	}

	@Override
	public void connectionStatusNoResultsYet() {
		// Do nothing, by default the enabled UI is shown while there are no results
		lastConnectionStatusResult = CONNECTION_NO_RESULT;
	}
}
