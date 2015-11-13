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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import com.squareup.okhttp.internal.Util;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Presents movie details
 */
public class MovieDetailsFragment extends AbstractDetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(MovieDetailsFragment.class);

    public static final String MOVIEID = "movie_id";
    public static final String POSTER_TRANS_NAME = "POSTER_TRANS_NAME";

    // Loader IDs
    private static final int LOADER_MOVIE = 0,
            LOADER_CAST = 1;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    // Displayed movie id
    private int movieId = -1;
    private String movieTitle;

    private ArrayList<VideoType.Cast> castArrayList;

    // Info for downloading the movie
    private FileDownloadHelper.MovieInfo movieDownloadInfo = null;

    // Controls whether a automatic sync refresh has been issued for this show
    private static boolean hasIssuedOutdatedRefresh = false;

    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

    @InjectView(R.id.exit_transition_view) View exitTransitionView;
    // Buttons
    @InjectView(R.id.fab) ImageButton fabButton;
    @InjectView(R.id.add_to_playlist) ImageButton addToPlaylistButton;
    @InjectView(R.id.go_to_imdb) ImageButton imdbButton;
    @InjectView(R.id.download) ImageButton downloadButton;
    @InjectView(R.id.seen) ImageButton seenButton;

    // Detail views
    @InjectView(R.id.media_panel) ScrollView mediaPanel;

    @InjectView(R.id.art) ImageView mediaArt;
    @InjectView(R.id.poster) ImageView mediaPoster;

    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;

    @InjectView(R.id.rating) TextView mediaRating;
    @InjectView(R.id.max_rating) TextView mediaMaxRating;
    @InjectView(R.id.year) TextView mediaYear;
    @InjectView(R.id.genres) TextView mediaGenres;
    @InjectView(R.id.rating_votes) TextView mediaRatingVotes;

    @InjectView(R.id.media_description) TextView mediaDescription;
    @InjectView(R.id.directors) TextView mediaDirectors;
    @InjectView(R.id.cast_list) GridLayout videoCastList;

    /**
     * Create a new instance of this, initialized to show the movie movieId
     */
    public static MovieDetailsFragment newInstance(MovieListFragment.ViewHolder vh) {
        MovieDetailsFragment fragment = new MovieDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(MOVIEID, vh.movieId);

        if( Utils.isLollipopOrLater()) {
            args.putString(POSTER_TRANS_NAME, vh.artView.getTransitionName());
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        movieId = getArguments().getInt(MOVIEID, -1);

        if (movieId == -1) {
            // There's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_movie_details, container, false);
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

        FloatingActionButton fab = (FloatingActionButton)fabButton;
        fab.attachToScrollView((ObservableScrollView) mediaPanel);

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
        return LibrarySyncService.SYNC_SINGLE_MOVIE;
    }

    @Override
    protected String getSyncID() {
        return LibrarySyncService.SYNC_MOVIEID;
    }

    @Override
    protected int getSyncItemID() {
        return movieId;
    }

    @Override
    protected SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipeRefreshLayout;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hasIssuedOutdatedRefresh = false;

        // Start the loaders
        getLoaderManager().initLoader(LOADER_MOVIE, null, this);
        getLoaderManager().initLoader(LOADER_CAST, null, this);
    }

    @Override
    public void onResume() {
        // Force the exit view to invisible
        exitTransitionView.setVisibility(View.INVISIBLE);

        //As we make mediaPoster invisible in onStop() we need to make it visible here.
        mediaPoster.setVisibility(View.VISIBLE);
        super.onResume();
    }

    @Override
    public void onStop() {
        //TODO: for some reason poster is included in the bottom slide animation, by making it invisible it is not noticeable for the user
        mediaPoster.setVisibility(View.INVISIBLE);
        super.onStop();
    }

    @Override
    protected void onSyncProcessEnded(MediaSyncEvent event) {
        if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
            getLoaderManager().restartLoader(LOADER_MOVIE, null, this);
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
            case LOADER_MOVIE:
                uri = MediaContract.Movies.buildMovieUri(getHostInfo().getId(), movieId);
                return new CursorLoader(getActivity(), uri,
                        MovieDetailsQuery.PROJECTION, null, null, null);
            case LOADER_CAST:
                uri = MediaContract.MovieCast.buildMovieCastListUri(getHostInfo().getId(), movieId);
                return new CursorLoader(getActivity(), uri,
                        MovieCastListQuery.PROJECTION, null, null, MovieCastListQuery.SORT);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_MOVIE:
                    displayMovieDetails(cursor);
                    checkOutdatedMovieDetails(cursor);
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

    /**
     * Callbacks for button bar
     */
    @OnClick(R.id.fab)
    public void onFabClicked(View v) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.movieid = movieId;
        Player.Open action = new Player.Open(item);
        action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
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
                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                        .show();
            }
        }, callbackHandler);
    }

    @OnClick(R.id.add_to_playlist)
    public void onAddToPlaylistClicked(View v) {
        Playlist.GetPlaylists getPlaylists = new Playlist.GetPlaylists();

        getPlaylists.execute(getHostManager().getConnection(), new ApiCallback<ArrayList<PlaylistType.GetPlaylistsReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlaylistType.GetPlaylistsReturnType> result) {
                if (!isAdded()) return;
                // Ok, loop through the playlists, looking for the video one
                int videoPlaylistId = -1;
                for (PlaylistType.GetPlaylistsReturnType playlist : result) {
                    if (playlist.type.equals(PlaylistType.GetPlaylistsReturnType.VIDEO)) {
                        videoPlaylistId = playlist.playlistid;
                        break;
                    }
                }
                // If found, add to playlist
                if (videoPlaylistId != -1) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.movieid = movieId;
                    Playlist.Add action = new Playlist.Add(videoPlaylistId, item);
                    action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            if (!isAdded()) return;
                            // Got an error, show toast
                            Toast.makeText(getActivity(), R.string.item_added_to_playlist, Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onError(int errorCode, String description) {
                            if (!isAdded()) return;
                            // Got an error, show toast
                            Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, callbackHandler);
                } else {
                    Toast.makeText(getActivity(), R.string.no_suitable_playlist, Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                        .show();
            }
        }, callbackHandler);
    }

    @OnClick(R.id.go_to_imdb)
    public void onImdbClicked(View v) {
        String imdbNumber = (String)v.getTag();

        if (imdbNumber != null) {
            Utils.openImdbForMovie(getActivity(), imdbNumber);
        }
    }

    @OnClick(R.id.seen)
    public void onSeenClicked(View v) {
        // Set the playcount
        Integer playcount = (Integer)v.getTag();
        int newPlaycount = (playcount > 0) ? 0 : 1;

        VideoLibrary.SetMovieDetails action =
                new VideoLibrary.SetMovieDetails(movieId, newPlaycount, null);
        action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                // Force a refresh, but don't show a message
                startSync(true);
            }

            @Override
            public void onError(int errorCode, String description) { }
        }, callbackHandler);

        // Change the button, to provide imeddiate feedback, even if it isn't yet stored in the db
        // (will be properly updated and refreshed after the refresh callback ends)
        setupSeenButton(newPlaycount);
    }

    @OnClick(R.id.download)
    public void onDownloadClicked(View v) {
        if (movieDownloadInfo == null) {
            // Nothing to download
            Toast.makeText(getActivity(), R.string.no_files_to_download, Toast.LENGTH_SHORT).show();
            return;
        }

        DialogInterface.OnClickListener noopClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }
                };

        // Check if the directory exists and whether to overwrite it
        File file = new File(movieDownloadInfo.getAbsoluteFilePath());
        if (file.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                    .setMessage(R.string.download_file_exists)
                    .setPositiveButton(R.string.overwrite,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                            movieDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                            callbackHandler);
                                }
                            })
                    .setNeutralButton(R.string.download_with_new_name,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                            movieDownloadInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                            callbackHandler);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, noopClickListener)
                    .show();
        } else {
            // Confirm that the user really wants to download the file
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                    .setMessage(R.string.confirm_movie_download)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                            movieDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                            callbackHandler);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, noopClickListener)
                    .show();
        }
    }

    /**
     * Display the movie details
     *
     * @param cursor Cursor with the data
     */
    private void displayMovieDetails(Cursor cursor) {
        LogUtils.LOGD(TAG, "Refreshing movie details");
        cursor.moveToFirst();
        movieTitle = cursor.getString(MovieDetailsQuery.TITLE);
        mediaTitle.setText(movieTitle);
        mediaUndertitle.setText(cursor.getString(MovieDetailsQuery.TAGLINE));

        int runtime = cursor.getInt(MovieDetailsQuery.RUNTIME) / 60;
        String durationYear =  runtime > 0 ?
                String.format(getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                        "  |  " + String.valueOf(cursor.getInt(MovieDetailsQuery.YEAR)) :
                String.valueOf(cursor.getInt(MovieDetailsQuery.YEAR));
        mediaYear.setText(durationYear);
        mediaGenres.setText(cursor.getString(MovieDetailsQuery.GENRES));

        double rating = cursor.getDouble(MovieDetailsQuery.RATING);
        if (rating > 0) {
            mediaRating.setVisibility(View.VISIBLE);
            mediaMaxRating.setVisibility(View.VISIBLE);
            mediaRatingVotes.setVisibility(View.VISIBLE);
            mediaRating.setText(String.format("%01.01f", rating));
            mediaMaxRating.setText(getString(R.string.max_rating_video));
            String votes = cursor.getString(MovieDetailsQuery.VOTES);
            mediaRatingVotes.setText((TextUtils.isEmpty(votes)) ?
                    "" : String.format(getString(R.string.votes), votes));
        } else {
            mediaRating.setVisibility(View.INVISIBLE);
            mediaMaxRating.setVisibility(View.INVISIBLE);
            mediaRatingVotes.setVisibility(View.INVISIBLE);
        }

        mediaDescription.setText(cursor.getString(MovieDetailsQuery.PLOT));
        mediaDirectors.setText(cursor.getString(MovieDetailsQuery.DIRECTOR));

        // IMDB button
        imdbButton.setTag(cursor.getString(MovieDetailsQuery.IMDBNUMBER));

        setupSeenButton(cursor.getInt(MovieDetailsQuery.PLAYCOUNT));

        // Images
        Resources resources = getActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
        int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height);
        UIUtils.loadImageWithCharacterAvatar(getActivity(), getHostManager(),
                cursor.getString(MovieDetailsQuery.THUMBNAIL), movieTitle,
                mediaPoster, posterWidth, posterHeight);
        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height);
        UIUtils.loadImageIntoImageview(getHostManager(),
                cursor.getString(MovieDetailsQuery.FANART),
                mediaArt, displayMetrics.widthPixels, artHeight);

        // Setup movie download info
        movieDownloadInfo = new FileDownloadHelper.MovieInfo(
                movieTitle, cursor.getString(MovieDetailsQuery.FILE));

        // Check if downloaded file exists
        downloadButton.setVisibility(View.VISIBLE);
        if (movieDownloadInfo.downloadFileExists()) {
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[]{
                    R.attr.colorAccent});
            downloadButton.setColorFilter(
                    styledAttributes.getColor(0,
                            getActivity().getResources().getColor(R.color.accent_default)));
            styledAttributes.recycle();
        } else {
            downloadButton.clearColorFilter();
        }
    }

    private void setupSeenButton(int playcount) {
        // Seen button
        if (playcount > 0) {
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.colorAccent});
            seenButton.setColorFilter(styledAttributes.getColor(0,
                    getActivity().getResources().getColor(R.color.accent_default)));
            styledAttributes.recycle();
        } else {
            seenButton.clearColorFilter();
        }
        // Save the playcount
        seenButton.setTag(playcount);
    }

    /**
     * Display the cast details
     *
     * @param cursor Cursor with the data
     */
    private void displayCastList(Cursor cursor) {
        // Transform the cursor into a List<VideoType.Cast>

        if (cursor.moveToFirst()) {
            castArrayList = new ArrayList<VideoType.Cast>(cursor.getCount());
            do {
                castArrayList.add(new VideoType.Cast(cursor.getString(MovieCastListQuery.NAME),
                        cursor.getInt(MovieCastListQuery.ORDER),
                        cursor.getString(MovieCastListQuery.ROLE),
                        cursor.getString(MovieCastListQuery.THUMBNAIL)));
            } while (cursor.moveToNext());

            UIUtils.setupCastInfo(getActivity(), castArrayList, videoCastList,
                    AllCastActivity.buildLaunchIntent(getActivity(), movieTitle, castArrayList));
        }
    }

    /**
     * Checks wether we should refresh the movie details with the info on XBMC
     * The details will be updated if the last update is older than what is configured in the
     * settings
     *
     * @param cursor Cursor with the data
     */
    private void checkOutdatedMovieDetails(Cursor cursor) {
        if (hasIssuedOutdatedRefresh)
            return;

        cursor.moveToFirst();
        long lastUpdated = cursor.getLong(MovieDetailsQuery.UPDATED);

        if (System.currentTimeMillis() > lastUpdated + Settings.DB_UPDATE_INTERVAL) {
            // Trigger a silent refresh
            hasIssuedOutdatedRefresh = true;
            startSync(true);
        }
    }

    /**
     * Movie details query parameters.
     */
    private interface MovieDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Movies.TITLE,
                MediaContract.Movies.TAGLINE,
                MediaContract.Movies.THUMBNAIL,
                MediaContract.Movies.FANART,
                MediaContract.Movies.YEAR,
                MediaContract.Movies.GENRES,
                MediaContract.Movies.RUNTIME,
                MediaContract.Movies.RATING,
                MediaContract.Movies.VOTES,
                MediaContract.Movies.PLOT,
                MediaContract.Movies.PLAYCOUNT,
                MediaContract.Movies.DIRECTOR,
                MediaContract.Movies.IMDBNUMBER,
                MediaContract.Movies.FILE,
                MediaContract.SyncColumns.UPDATED,
        };

        final int ID = 0;
        final int TITLE = 1;
        final int TAGLINE = 2;
        final int THUMBNAIL = 3;
        final int FANART = 4;
        final int YEAR = 5;
        final int GENRES = 6;
        final int RUNTIME = 7;
        final int RATING = 8;
        final int VOTES = 9;
        final int PLOT = 10;
        final int PLAYCOUNT = 11;
        final int DIRECTOR = 12;
        final int IMDBNUMBER = 13;
        final int FILE = 14;
        final int UPDATED = 15;
    }

    /**
     * Movie cast list query parameters.
     */
    public interface MovieCastListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.MovieCast.NAME,
                MediaContract.MovieCast.ORDER,
                MediaContract.MovieCast.ROLE,
                MediaContract.MovieCast.THUMBNAIL,
        };

        String SORT = MediaContract.MovieCast.ORDER + " ASC";

        final int ID = 0;
        final int NAME = 1;
        final int ORDER = 2;
        final int ROLE = 3;
        final int THUMBNAIL = 4;
    }
}
