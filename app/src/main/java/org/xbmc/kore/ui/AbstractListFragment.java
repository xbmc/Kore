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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public abstract class AbstractListFragment extends Fragment implements
															SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LogUtils.makeLogTag(AbstractListFragment.class);
	private BaseAdapter adapter;

	private final String BUNDLE_SAVEDINSTANCE_LISTPOSITION = "lposition";

	private boolean gridViewUsesMultipleColumns;

	@InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
	@InjectView(R.id.list) GridView gridView;
	@InjectView(android.R.id.empty) TextView emptyView;

	abstract protected AdapterView.OnItemClickListener createOnItemClickListener();
	abstract protected BaseAdapter createAdapter();

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
		ButterKnife.inject(this, root);

		swipeRefreshLayout.setOnRefreshListener(this);

		gridView.setEmptyView(emptyView);
		gridView.setOnItemClickListener(createOnItemClickListener());
		gridView.setAdapter(adapter);

		if (savedInstanceState != null) {
			final int listPosition = savedInstanceState.getInt(BUNDLE_SAVEDINSTANCE_LISTPOSITION);
			gridView.post(new Runnable() {
				@Override
				public void run() {
					gridView.setSelection(listPosition);
				}
			});
		}

		//Listener added to be able to determine if multiple-columns is at all possible for the current grid
		//We use this information to enable/disable the menu item to switch between multiple and single columns
		gridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (gridView.getNumColumns() > 1) {
					gridViewUsesMultipleColumns = true;
				}

				if (Utils.isJellybeanOrLater()) {
					gridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				} else {
					gridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}

				//Make sure menu is update	d if it was already created
				getActivity().invalidateOptionsMenu();
			}
		});

		setHasOptionsMenu(true);

		return root;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (gridView != null) {
			outState.putInt(BUNDLE_SAVEDINSTANCE_LISTPOSITION, gridView.getFirstVisiblePosition());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.abstractlistfragment, menu);

		if(gridViewUsesMultipleColumns) {
			if (PreferenceManager
					.getDefaultSharedPreferences(getActivity())
					.getBoolean(Settings.KEY_PREF_SINGLE_COLUMN,
								Settings.DEFAULT_PREF_SINGLE_COLUMN)) {
				gridView.setNumColumns(1);
				adapter.notifyDataSetChanged();

				MenuItem item = menu.findItem(R.id.action_multi_single_columns);
				item.setTitle(R.string.multi_column);
			}
		} else {
			//Default number of columns for GridView is set to AUTO_FIT.
			//When this leads to a single column it is not possible
			//to switch to multiple columns. We therefore disable
			//the menu item.
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
		if (gridView.getNumColumns() == 1) {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, false);
			item.setTitle(R.string.single_column);
			gridView.setNumColumns(GridView.AUTO_FIT);
		} else {
			editor.putBoolean(Settings.KEY_PREF_SINGLE_COLUMN, true);
			item.setTitle(R.string.multi_column);
			gridView.setNumColumns(1);
		}
		editor.apply();
		adapter.notifyDataSetChanged(); //force gridView to redraw
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

	public void hideRefreshAnimation() {
		swipeRefreshLayout.setRefreshing(false);
	}

	public BaseAdapter getAdapter() {
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
