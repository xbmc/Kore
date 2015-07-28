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
package org.xbmc.kore.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Activity that presents all cast of a movie or TV Show
 */
public class AllCastActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(AllCastActivity.class);

    // Extras to be passed to this activity: type (0 - movie, 1 -tv_show) and the corresponding id
    public static final String EXTRA_CAST_TYPE = "EXTRA_CAST_TYPE";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";

    public static final int EXTRA_TYPE_MOVIE = 0;
    public static final int EXTRA_TYPE_TVSHOW = 1;

    // Loader IDs
    private static final int LOADER_CAST = 0;

    // Passed arguments
    private int cast_type;
    private int movie_tvshow_id = -1;
    private String movie_tvshow_title;

    private CursorAdapter adapter;
    NavigationDrawerFragment navigationDrawerFragment;

    @InjectView(R.id.cast_list) GridView castGridView;
    @InjectView(android.R.id.empty) TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_cast);
        ButterKnife.inject(this);

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        if (savedInstanceState == null) {
            cast_type = getIntent().getIntExtra(EXTRA_CAST_TYPE, EXTRA_TYPE_MOVIE);
            movie_tvshow_id = getIntent().getIntExtra(EXTRA_ID, -1);
            movie_tvshow_title = getIntent().getStringExtra(EXTRA_TITLE);
        } else {
            cast_type = savedInstanceState.getInt(EXTRA_CAST_TYPE);
            movie_tvshow_id = savedInstanceState.getInt(EXTRA_ID);
            movie_tvshow_title = savedInstanceState.getString(EXTRA_TITLE);
        }

        LogUtils.LOGD(TAG, "Showing cast for: " + movie_tvshow_title);

        // Configure the adapter and start the loader
        castGridView.setEmptyView(emptyView);
        castGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the name from the tag
                Utils.openImdbForPerson(AllCastActivity.this, (String) ((ViewHolder)view.getTag()).castName);
            }
        });

        adapter = new CastAdapter(this, cast_type);
        castGridView.setAdapter(adapter);

        getLoaderManager().initLoader(LOADER_CAST, null, this);

        setupActionBar(movie_tvshow_title);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_CAST_TYPE, cast_type);
        outState.putInt(EXTRA_ID, movie_tvshow_id);
        outState.putString(EXTRA_TITLE, movie_tvshow_title);
    }

    private void setupActionBar(String title) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);

        navigationDrawerFragment.setDrawerIndicatorEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.cast) + " - " + title);
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        int hostId = HostManager.getInstance(this).getHostInfo().getId();

        switch (cast_type) {
            case EXTRA_TYPE_MOVIE:
                uri = MediaContract.MovieCast.buildMovieCastListUri(hostId, movie_tvshow_id);
                return new CursorLoader(this, uri,
                                        MovieDetailsFragment.MovieCastListQuery.PROJECTION,
                                        null, null,
                                        MovieDetailsFragment.MovieCastListQuery.SORT);
            case EXTRA_TYPE_TVSHOW:
                uri = MediaContract.TVShowCast.buildTVShowCastListUri(hostId, movie_tvshow_id);
                return new CursorLoader(this, uri,
                                        TVShowOverviewFragment.TVShowCastListQuery.PROJECTION,
                                        null, null,
                                        TVShowOverviewFragment.TVShowCastListQuery.SORT);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LogUtils.LOGD(TAG, "cursor count: " + cursor.getCount());
        adapter.swapCursor(cursor);
        // To prevent the empty text from appearing on the first load, set it now
        emptyView.setText(getString(R.string.no_cast_info));
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Release loader's data
        adapter.swapCursor(null);
    }

    private static class CastAdapter extends CursorAdapter {
        Context context;
        private HostManager hostManager;
        private int artWidth = -1, artHeight = -1;

        private int name_idx;
        private int role_idx = 3;
        private int thumbnail_idx = 4;

        public CastAdapter(Context context, int castType) {
            super(context, null, false);
            this.context = context;
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
//            Resources resources = context.getResources();
//            artWidth = (int)(resources.getDimension(R.dimen.movielist_art_width) /
//                             UIUtils.IMAGE_RESIZE_FACTOR);
//            artHeight = (int)(resources.getDimension(R.dimen.movielist_art_heigth) /
//                              UIUtils.IMAGE_RESIZE_FACTOR);

            name_idx = (castType == EXTRA_TYPE_MOVIE) ?
                    MovieDetailsFragment.MovieCastListQuery.NAME :
                    TVShowOverviewFragment.TVShowCastListQuery.NAME;
            role_idx = (castType == EXTRA_TYPE_MOVIE) ?
                    MovieDetailsFragment.MovieCastListQuery.ROLE :
                    TVShowOverviewFragment.TVShowCastListQuery.ROLE;
            thumbnail_idx = (castType == EXTRA_TYPE_MOVIE) ?
                    MovieDetailsFragment.MovieCastListQuery.THUMBNAIL :
                    TVShowOverviewFragment.TVShowCastListQuery.THUMBNAIL;

        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                                            .inflate(R.layout.grid_item_cast, parent, false);

            if (artWidth == -1) {
                Resources resources = context.getResources();
                int imageMarginPx = resources.getDimensionPixelSize(R.dimen.small_padding);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);

                int numColumns = resources.getInteger(R.integer.cast_grid_view_columns);

                artWidth = (displayMetrics.widthPixels - (2 + numColumns - 1) * imageMarginPx) / numColumns;
                artHeight = (int) (artWidth * 1.5);
                LogUtils.LOGD(TAG, "width: " + artWidth);
            }

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.roleView = (TextView)view.findViewById(R.id.role);
            viewHolder.nameView = (TextView)view.findViewById(R.id.name);
            viewHolder.pictureView = (ImageView)view.findViewById(R.id.picture);

            view.setTag(viewHolder);

            view.getLayoutParams().width = artWidth;
            view.getLayoutParams().height = artHeight;
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            String name = cursor.getString(name_idx);
            viewHolder.roleView.setText(cursor.getString(role_idx));
            viewHolder.nameView.setText(name);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 cursor.getString(thumbnail_idx), name,
                                                 viewHolder.pictureView, artWidth, artHeight);
            viewHolder.castName = name;
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
