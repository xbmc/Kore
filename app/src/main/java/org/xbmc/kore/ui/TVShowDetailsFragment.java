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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Presents a TV Show overview
 */
public class TVShowDetailsFragment extends AbstractDetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(TVShowDetailsFragment.class);

    public static final String POSTER_TRANS_NAME = "POSTER_TRANS_NAME";
    public static final String BUNDLE_KEY_TVSHOWID = "tvshow_id";
    public static final String BUNDLE_KEY_TITLE = "title";
    public static final String BUNDLE_KEY_PREMIERED = "premiered";
    public static final String BUNDLE_KEY_STUDIO = "studio";
    public static final String BUNDLE_KEY_EPISODE = "episode";
    public static final String BUNDLE_KEY_WATCHEDEPISODES = "watchedepisodes";
    public static final String BUNDLE_KEY_RATING = "rating";
    public static final String BUNDLE_KEY_PLOT = "plot";
    public static final String BUNDLE_KEY_GENRES = "genres";

    public interface OnSeasonSelectedListener {
        void onSeasonSelected(int tvshowId, int season);
    }

    // Activity listener
    private OnSeasonSelectedListener listenerActivity;

    // Loader IDs
    private static final int LOADER_TVSHOW = 0,
            LOADER_SEASONS = 1,
            LOADER_CAST = 2;

    // Displayed movie id
    private int tvshowId = -1;
    private String tvshowTitle;

    // Controls whether a automatic sync refresh has been issued for this show
    private static boolean hasIssuedOutdatedRefresh = false;

    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

//    // Buttons
//    @InjectView(R.id.go_to_imdb) ImageButton imdbButton;

    // Detail views
    @InjectView(R.id.media_panel) ScrollView mediaPanel;

    @InjectView(R.id.art) ImageView mediaArt;
    @InjectView(R.id.poster) ImageView mediaPoster;

    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;

    @InjectView(R.id.rating) TextView mediaRating;
    @InjectView(R.id.max_rating) TextView mediaMaxRating;
    @InjectView(R.id.premiered) TextView mediaPremiered;
    @InjectView(R.id.genres) TextView mediaGenres;

    @InjectView(R.id.media_description) TextView mediaDescription;
    @InjectView(R.id.cast_list) GridLayout videoCastList;

    @InjectView(R.id.seasons_title) TextView seasonsListTitle;
    @InjectView(R.id.seasons_list) GridLayout seasonsList;

    @InjectView(R.id.media_description_container) LinearLayout mediaDescriptionContainer;
    @InjectView(R.id.show_all) ImageView mediaShowAll;

    private boolean isDescriptionExpanded = false;

    /**
     * Create a new instance of this, initialized to show tvshowId
     */
    @TargetApi(21)
    public static TVShowDetailsFragment newInstance(TVShowListFragment.ViewHolder vh) {
        TVShowDetailsFragment fragment = new TVShowDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_TVSHOWID, vh.tvshowId);
        args.putInt(BUNDLE_KEY_EPISODE, vh.episode);
        args.putString(BUNDLE_KEY_GENRES, vh.genres);
        args.putString(BUNDLE_KEY_PLOT, vh.plot);
        args.putString(BUNDLE_KEY_PREMIERED, vh.premiered);
        args.putDouble(BUNDLE_KEY_RATING, vh.rating);
        args.putString(BUNDLE_KEY_STUDIO, vh.studio);
        args.putString(BUNDLE_KEY_TITLE, vh.tvshowTitle);
        args.putInt(BUNDLE_KEY_WATCHEDEPISODES, vh.watchedEpisodes);
        if( Utils.isLollipopOrLater()) {
            args.putString(POSTER_TRANS_NAME, vh.artView.getTransitionName());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @TargetApi(21)
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        Bundle bundle = getArguments();
        tvshowId = bundle.getInt(BUNDLE_KEY_TVSHOWID, -1);

        if (tvshowId == -1) {
            // There's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_tvshow_overview, container, false);
        ButterKnife.inject(this, root);

        //UIUtils.setSwipeRefreshLayoutColorScheme(swipeRefreshLayout);

        // Setup dim the fanart when scroll changes. Full dim on 4 * iconSize dp
        Resources resources = getActivity().getResources();
        final int pixelsToTransparent  = 4 * resources.getDimensionPixelSize(R.dimen.default_icon_size);
        mediaPanel.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                float y = mediaPanel.getScrollY();
                float newAlpha = Math.min(1, Math.max(0, 1 - (y / pixelsToTransparent)));
                mediaArt.setAlpha(newAlpha);
            }
        });

        tvshowTitle = bundle.getString(BUNDLE_KEY_TITLE);

        mediaTitle.setText(tvshowTitle);
        setMediaUndertitle(bundle.getInt(BUNDLE_KEY_EPISODE), bundle.getInt(BUNDLE_KEY_WATCHEDEPISODES));
        setMediaPremiered(bundle.getString(BUNDLE_KEY_PREMIERED), bundle.getString(BUNDLE_KEY_STUDIO));
        mediaGenres.setText(bundle.getString(BUNDLE_KEY_GENRES));
        setMediaRating(bundle.getDouble(BUNDLE_KEY_RATING));
        mediaDescription.setText(bundle.getString(BUNDLE_KEY_PLOT));

        if(Utils.isLollipopOrLater()) {
            mediaPoster.setTransitionName(getArguments().getString(POSTER_TRANS_NAME));
        }
        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), mediaPanel, false, false, true);
//        mediaPanel.setClipToPadding(false);

        return root;
    }

    @Override
    protected String getSyncType() {
        return LibrarySyncService.SYNC_SINGLE_TVSHOW;
    }

    @Override
    protected String getSyncID() {
        return LibrarySyncService.SYNC_TVSHOWID;
    }

    @Override
    protected int getSyncItemID() {
        return tvshowId;
    }

    @Override
    protected SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipeRefreshLayout;
    }

    @Override
    protected void onDownload() { }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hasIssuedOutdatedRefresh = false;

        // Start the loaders
        getLoaderManager().initLoader(LOADER_TVSHOW, null, this);
        getLoaderManager().initLoader(LOADER_SEASONS, null, this);
        getLoaderManager().initLoader(LOADER_CAST, null, this);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnSeasonSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSeasonSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    protected void onSyncProcessEnded(MediaSyncEvent event) {
        if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
            getLoaderManager().restartLoader(LOADER_TVSHOW, null, this);
            getLoaderManager().restartLoader(LOADER_SEASONS, null, this);
            getLoaderManager().restartLoader(LOADER_CAST, null, this);
        }
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;

        switch (i) {
            case LOADER_TVSHOW:
                uri = MediaContract.TVShows.buildTVShowUri(getHostInfo().getId(), tvshowId);
                return new CursorLoader(getActivity(), uri,
                                        TVShowDetailsQuery.PROJECTION, null, null, null);
            case LOADER_SEASONS:
                // Load seasons
                uri = MediaContract.Seasons.buildTVShowSeasonsListUri(getHostInfo().getId(), tvshowId);
                return new CursorLoader(getActivity(), uri,
                                        SeasonsListQuery.PROJECTION, null, null, SeasonsListQuery.SORT);
            case LOADER_CAST:
                uri = MediaContract.TVShowCast.buildTVShowCastListUri(getHostInfo().getId(), tvshowId);
                return new CursorLoader(getActivity(), uri,
                                        TVShowCastListQuery.PROJECTION, null, null, TVShowCastListQuery.SORT);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LogUtils.LOGD(TAG, "onLoadFinished");
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_TVSHOW:
                    displayTVShowDetails(cursor);
                    checkOutdatedTVShowDetails(cursor);
                    break;
                case LOADER_SEASONS:
                    displaySeasonList(cursor);
                    break;
                case LOADER_CAST:
                    displayCastList(cursor);
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Release loader's data
    }

//    /**
//     * Callbacks for button bar
//     */
//    @OnClick(R.id.go_to_imdb)
//    public void onImdbClicked(View v) {
//        String imdbNumber = (String)v.getTag();
//
//        if (imdbNumber != null) {
//            Utils.openImdbForMovie(getActivity(), imdbNumber);
//        }
//    }

    /**
     * Display the tv show details
     *
     * @param cursor Cursor with the data
     */
    private void displayTVShowDetails(Cursor cursor) {
        LogUtils.LOGD(TAG, "displayTVShowDetails");
        cursor.moveToFirst();
        tvshowTitle = cursor.getString(TVShowDetailsQuery.TITLE);
        mediaTitle.setText(tvshowTitle);
        int numEpisodes = cursor.getInt(TVShowDetailsQuery.EPISODE),
                watchedEpisodes = cursor.getInt(TVShowDetailsQuery.WATCHEDEPISODES);
        setMediaUndertitle(numEpisodes, watchedEpisodes);

        setMediaPremiered(cursor.getString(TVShowDetailsQuery.PREMIERED), cursor.getString(TVShowDetailsQuery.STUDIO));

        mediaGenres.setText(cursor.getString(TVShowDetailsQuery.GENRES));

        setMediaRating(cursor.getDouble(TVShowDetailsQuery.RATING));

        mediaDescription.setText(cursor.getString(TVShowDetailsQuery.PLOT));

        Resources resources = getActivity().getResources();
        final int maxLines = resources.getInteger(R.integer.description_max_lines);
        TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(new int[] {
                R.attr.iconExpand,
                R.attr.iconCollapse
        });
        final int iconCollapseResId =
                styledAttributes.getResourceId(styledAttributes.getIndex(0), R.drawable.ic_expand_less_white_24dp);
        final int iconExpandResId =
                styledAttributes.getResourceId(styledAttributes.getIndex(1), R.drawable.ic_expand_more_white_24dp);
        styledAttributes.recycle();
        mediaDescriptionContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDescriptionExpanded) {
                    mediaDescription.setMaxLines(Integer.MAX_VALUE);
                    mediaShowAll.setImageResource(iconExpandResId);
                } else {
                    mediaDescription.setMaxLines(maxLines);
                    mediaShowAll.setImageResource(iconCollapseResId);
                }
                isDescriptionExpanded = !isDescriptionExpanded;
            }
        });


//        // IMDB button
//        imdbButton.setTag(cursor.getString(TVShowDetailsQuery.IMDBNUMBER));

        // Images
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
        int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height);
        UIUtils.loadImageWithCharacterAvatar(getActivity(), getHostManager(),
                cursor.getString(TVShowDetailsQuery.THUMBNAIL), tvshowTitle,
                mediaPoster, posterWidth, posterHeight);
        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height);
        UIUtils.loadImageIntoImageview(getHostManager(),
                cursor.getString(TVShowDetailsQuery.FANART),
                mediaArt, displayMetrics.widthPixels, artHeight);
    }

    private void setMediaUndertitle(int numEpisodes, int watchedEpisodes) {
        String episodes = String.format(getString(R.string.num_episodes),
                numEpisodes, numEpisodes - watchedEpisodes);
        mediaUndertitle.setText(episodes);
    }

    private void setMediaPremiered(String premiered, String studio) {
        mediaPremiered.setText(String.format(getString(R.string.premiered), premiered) + "  |  " + studio);
    }

    private void setMediaRating(double rating) {
        if (rating > 0) {
            mediaRating.setVisibility(View.VISIBLE);
            mediaMaxRating.setVisibility(View.VISIBLE);
            mediaRating.setText(String.format("%01.01f", rating));
            mediaMaxRating.setText(getString(R.string.max_rating_video));
        } else {
            mediaRating.setVisibility(View.INVISIBLE);
            mediaMaxRating.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Display the cast details
     *
     * @param cursor Cursor with the data
     */
    private void displayCastList(Cursor cursor) {
        // Transform the cursor into a List<VideoType.Cast>
        if (cursor.moveToFirst()) {
            ArrayList<VideoType.Cast> castArrayList = new ArrayList<VideoType.Cast>(cursor.getCount());
            do {
                castArrayList.add(new VideoType.Cast(cursor.getString(TVShowCastListQuery.NAME),
                        cursor.getInt(TVShowCastListQuery.ORDER),
                        cursor.getString(TVShowCastListQuery.ROLE),
                        cursor.getString(TVShowCastListQuery.THUMBNAIL)));
            } while (cursor.moveToNext());

            UIUtils.setupCastInfo(getActivity(), castArrayList, videoCastList,
                    AllCastActivity.buildLaunchIntent(getActivity(), tvshowTitle, castArrayList));
        }
    }

    /**
     * Display the seasons list
     *
     * @param cursor Cursor with the data
     */
    private void displaySeasonList(Cursor cursor) {
        if (cursor.moveToFirst()) {
            HostManager hostManager = HostManager.getInstance(getActivity());

            View.OnClickListener seasonListClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listenerActivity.onSeasonSelected(tvshowId, (int)v.getTag());
                }
            };

            // Get the art dimensions
            Resources resources = getActivity().getResources();
            int artWidth = (int)(resources.getDimension(R.dimen.seasonlist_art_width) /
                                 UIUtils.IMAGE_RESIZE_FACTOR);
            int artHeight = (int)(resources.getDimension(R.dimen.seasonlist_art_heigth) /
                                  UIUtils.IMAGE_RESIZE_FACTOR);

            seasonsList.removeAllViews();
            do {
                int seasonNumber = cursor.getInt(SeasonsListQuery.SEASON);
                String thumbnail = cursor.getString(SeasonsListQuery.THUMBNAIL);
                int numEpisodes = cursor.getInt(SeasonsListQuery.EPISODE);
                int watchedEpisodes = cursor.getInt(SeasonsListQuery.WATCHEDEPISODES);

                View seasonView = LayoutInflater.from(getActivity()).inflate(R.layout.grid_item_season, seasonsList, false);

                ImageView seasonPictureView = (ImageView) seasonView.findViewById(R.id.art);
                TextView seasonNumberView = (TextView) seasonView.findViewById(R.id.season);
                TextView seasonEpisodesView = (TextView) seasonView.findViewById(R.id.episodes);

                seasonNumberView.setText(String.format(getActivity().getString(R.string.season_number), seasonNumber));
                seasonEpisodesView.setText(String.format(getActivity().getString(R.string.num_episodes),
                                                         numEpisodes, numEpisodes - watchedEpisodes));

                UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                                     thumbnail,
                                                     String.valueOf(seasonNumber),
                                                     seasonPictureView, artWidth, artHeight);

                seasonView.setTag(seasonNumber);
                seasonView.setOnClickListener(seasonListClickListener);
                seasonsList.addView(seasonView);
            } while (cursor.moveToNext());
        } else {
            // No seasons, hide views
            seasonsListTitle.setVisibility(View.GONE);
            seasonsList.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * Checks wether we should refresh the TV Show details with the info on XBMC
     * The details will be updated if the last update is older than what is configured in the
     * settings
     *
     * @param cursor Cursor with the data
     */
    private void checkOutdatedTVShowDetails(Cursor cursor) {
        if (hasIssuedOutdatedRefresh)
            return;

        cursor.moveToFirst();
        long lastUpdated = cursor.getLong(TVShowDetailsQuery.UPDATED);

        if (System.currentTimeMillis() > lastUpdated + Settings.DB_UPDATE_INTERVAL) {
            // Trigger a silent refresh
            hasIssuedOutdatedRefresh = true;
            startSync(true);
        }
    }

    /**
     * Returns the shared element if visible
     * @return View if visible, null otherwise
     */
    public View getSharedElement() {
        if (UIUtils.isViewInBounds(mediaPanel, mediaPoster)) {
            return mediaPoster;
        }

        return null;
    }

    /**
     * TV Show details query parameters.
     */
    private interface TVShowDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.TVShows.TITLE,
                MediaContract.TVShows.THUMBNAIL,
                MediaContract.TVShows.FANART,
                MediaContract.TVShows.PREMIERED,
                MediaContract.TVShows.STUDIO,
                MediaContract.TVShows.EPISODE,
                MediaContract.TVShows.WATCHEDEPISODES,
                MediaContract.TVShows.RATING,
                MediaContract.TVShows.PLOT,
                MediaContract.TVShows.PLAYCOUNT,
                MediaContract.TVShows.IMDBNUMBER,
                MediaContract.TVShows.GENRES,
                MediaContract.SyncColumns.UPDATED,
        };

        int ID = 0;
        int TITLE = 1;
        int THUMBNAIL = 2;
        int FANART = 3;
        int PREMIERED = 4;
        int STUDIO = 5;
        int EPISODE = 6;
        int WATCHEDEPISODES = 7;
        int RATING = 8;
        int PLOT = 9;
        int PLAYCOUNT = 10;
        int IMDBNUMBER = 11;
        int GENRES = 12;
        int UPDATED = 13;
    }

    /**
     * Seasons list query parameters.
     */
    private interface SeasonsListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Seasons.SEASON,
                MediaContract.Seasons.THUMBNAIL,
                MediaContract.Seasons.EPISODE,
                MediaContract.Seasons.WATCHEDEPISODES
        };

        String SORT = MediaContract.Seasons.SEASON + " ASC";

        int ID = 0;
        int SEASON = 1;
        int THUMBNAIL = 2;
        int EPISODE = 3;
        int WATCHEDEPISODES = 4;
    }

    /**
     * Movie cast list query parameters.
     */
    public interface TVShowCastListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.TVShowCast.NAME,
                MediaContract.TVShowCast.ORDER,
                MediaContract.TVShowCast.ROLE,
                MediaContract.TVShowCast.THUMBNAIL,
        };

        String SORT = MediaContract.TVShowCast.ORDER + " ASC";

        int ID = 0;
        int NAME = 1;
        int ORDER = 2;
        int ROLE = 3;
        int THUMBNAIL = 4;
    }
}
