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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.LogUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class AbstractListFragment extends Fragment implements
															SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(AbstractListFragment.class);
	private RecyclerView.Adapter adapter;

	private Unbinder unbinder;

	protected @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.list) RecyclerViewEmptyViewSupport recyclerView;
	@BindView(android.R.id.empty) TextView emptyView;

	abstract protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener();
	abstract protected RecyclerViewEmptyViewSupport.Adapter createAdapter();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = createAdapter();
	}

	@TargetApi(16)
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
		unbinder = ButterKnife.bind(this, root);

		swipeRefreshLayout.setOnRefreshListener(this);

		recyclerView.setEmptyView(emptyView);
		recyclerView.setOnItemClickListener(createOnItemClickListener());

		if (PreferenceManager
				.getDefaultSharedPreferences(getActivity())
				.getBoolean(Settings.KEY_PREF_SINGLE_COLUMN,
							Settings.DEFAULT_PREF_SINGLE_COLUMN)) {
			recyclerView.setColumnCount(1);
		}

		recyclerView.setAdapter(adapter);
		
		setHasOptionsMenu(true);

		return root;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.abstractlistfragment, menu);

		if(recyclerView.isMultiColumnSupported()) {
			if (PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.getBoolean(Settings.KEY_PREF_SINGLE_COLUMN,
								Settings.DEFAULT_PREF_SINGLE_COLUMN)) {
				recyclerView.setColumnCount(1);
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
		switch(item.getItemId()) {
			case R.id.action_multi_single_columns:
				toggleAmountOfColumns(item);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void toggleAmountOfColumns(MenuItem item) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(getActivity()).edit();
		if (recyclerView.getColumnCount() == 1) {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, false);
			item.setTitle(R.string.single_column);
			recyclerView.setColumnCount(RecyclerViewEmptyViewSupport.AUTO_FIT);
		} else {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, true);
			item.setTitle(R.string.multi_column);
			recyclerView.setColumnCount(1);
		}
		editor.apply();
		adapter.notifyDataSetChanged(); //force gridView to redraw
	}

	public void hideRefreshAnimation() {
		swipeRefreshLayout.setRefreshing(false);
	}

	public RecyclerView.Adapter getAdapter() {
		return adapter;
	}

	/**
	 * Returns the view that is displayed when the gridview has no items to show
	 * @return
	 */
	public TextView getEmptyView() {
		return emptyView;
	}
}
