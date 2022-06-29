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
package org.xbmc.kore.ui.sections.video;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.xbmc.kore.R;
import org.xbmc.kore.databinding.ActivityAllCastBinding;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;

/**
 * Activity that presents all cast of a movie or TV Show
 * Accepts the title to be shown on the action bar and a ArrayList<Cast> to display
 */
public class AllCastActivity extends BaseActivity {
    private static final String TAG = LogUtils.makeLogTag(AllCastActivity.class);

    // Extras to be passed to this activity: title and the cast list
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_CAST_LIST = "EXTRA_CAST_LIST";

    // Passed arguments
    private String movie_tvshow_title;
    private ArrayList<VideoType.Cast> castArrayList;

    NavigationDrawerFragment navigationDrawerFragment;

    ActivityAllCastBinding binding;

    /**
     * Returns an intent that can be used to start this activity, with all the correct parameters
     *
     * @param context Calling activity's context
     * @param title Title to show on action bar
     * @param castArrayList Cast list to show
     * @return Intent to start this activity
     */
    public static Intent buildLaunchIntent(Context context,
                                           String title, ArrayList<VideoType.Cast> castArrayList) {
        return new Intent(context, AllCastActivity.class)
                .putExtra(EXTRA_TITLE, title)
                .putParcelableArrayListExtra(EXTRA_CAST_LIST, castArrayList);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAllCastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        if (navigationDrawerFragment != null)
            navigationDrawerFragment.setUp(R.id.navigation_drawer, findViewById(R.id.drawer_layout));

        if (savedInstanceState == null) {
            movie_tvshow_title = getIntent().getStringExtra(EXTRA_TITLE);
            castArrayList = getIntent().getParcelableArrayListExtra(EXTRA_CAST_LIST);
        } else {
            movie_tvshow_title = savedInstanceState.getString(EXTRA_TITLE);
            castArrayList = savedInstanceState.getParcelableArrayList(EXTRA_CAST_LIST);
        }

        //LogUtils.LOGD(TAG, "Showing cast for: " + movie_tvshow_title);

        // Configure the grid
        binding.castList.setEmptyView(binding.includeEmptyView.empty);
        binding.castList.setOnItemClickListener((parent, view, position, id) -> {
            // Get the name from the tag
            Utils.openImdbForPerson(AllCastActivity.this, ((ViewHolder)view.getTag()).castName);
        });

        CastArrayAdapter arrayAdapter = new CastArrayAdapter(this, castArrayList);
        binding.castList.setAdapter(arrayAdapter);

        setupActionBar(movie_tvshow_title);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_TITLE, movie_tvshow_title);
        outState.putParcelableArrayList(EXTRA_CAST_LIST, castArrayList);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar(String title) {
        Toolbar toolbar = findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);

        navigationDrawerFragment.setDrawerIndicatorEnabled(false);
        actionBar.setTitle((title != null) ?
                                   getResources().getString(R.string.cast) + " - " + title :
                                   getResources().getString(R.string.cast));
    }

    public static class CastArrayAdapter extends ArrayAdapter<VideoType.Cast> {
        private final HostManager hostManager;
        private int artWidth = -1, artHeight = -1;

        public CastArrayAdapter(Context context, ArrayList<VideoType.Cast> castArrayList) {
            super(context, 0, castArrayList);
            this.hostManager = HostManager.getInstance(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                                            .inflate(R.layout.grid_item_cast, parent, false);

                if (artWidth == -1) {
                    Resources resources = getContext().getResources();
                    int imageMarginPx = resources.getDimensionPixelSize(R.dimen.small_padding);

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                    windowManager.getDefaultDisplay().getMetrics(displayMetrics);

                    int numColumns = resources.getInteger(R.integer.cast_grid_view_columns);

                    artWidth = (displayMetrics.widthPixels - (2 + numColumns - 1) * imageMarginPx) / numColumns;
                    artHeight = (int) (artWidth * 1.5);
                    LogUtils.LOGD(TAG, "width: " + artWidth);
                }

                // Setup View holder pattern
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.roleView = convertView.findViewById(R.id.role);
                viewHolder.nameView = convertView.findViewById(R.id.name);
                viewHolder.pictureView = convertView.findViewById(R.id.picture);

                convertView.setTag(viewHolder);

                convertView.getLayoutParams().width = artWidth;
                convertView.getLayoutParams().height = artHeight;
            }

            final ViewHolder viewHolder = (ViewHolder)convertView.getTag();
            VideoType.Cast cast = getItem(position);

            viewHolder.roleView.setText(cast.role);
            viewHolder.nameView.setText(cast.name);
            UIUtils.loadImageWithCharacterAvatar(getContext(), hostManager,
                                                 cast.thumbnail, cast.name,
                                                 viewHolder.pictureView, artWidth, artHeight);
            viewHolder.castName = cast.name;

            return convertView;

        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder {
        TextView roleView;
        TextView nameView;
        ImageView pictureView;

        String castName;
    }
}
