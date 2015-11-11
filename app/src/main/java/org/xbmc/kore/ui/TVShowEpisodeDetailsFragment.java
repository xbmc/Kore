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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Presents movie details
 */
public class TVShowEpisodeDetailsFragment extends AbstractDetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(TVShowEpisodeDetailsFragment.class);

    public static final String TVSHOWID = "tvshow_id";
    public static final String EPISODEID = "episode_id";

    // Loader IDs
    private static final int LOADER_EPISODE = 0;
//    private static final int LOADER_CAST = 1;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    // Displayed episode
    private int tvshowId = -1;
    private int episodeId = -1;

    // Info for downloading the episode
    private FileDownloadHelper.TVShowInfo tvshowDownloadInfo = null;

    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

    @InjectView(R.id.exit_transition_view) View exitTransitionView;
    // Buttons
    @InjectView(R.id.fab) ImageButton fabButton;
    @InjectView(R.id.add_to_playlist) ImageButton addToPlaylistButton;
    @InjectView(R.id.download) ImageButton downloadButton;
    @InjectView(R.id.seen) ImageButton seenButton;

    // Detail views
    @InjectView(R.id.media_panel) ScrollView mediaPanel;

    @InjectView(R.id.art) ImageView mediaArt;
//    @InjectView(R.id.poster) ImageView mediaPoster;

    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;

    @InjectView(R.id.rating) TextView mediaRating;
    @InjectView(R.id.max_rating) TextView mediaMaxRating;
    @InjectView(R.id.premiered) TextView mediaPremiered;
    @InjectView(R.id.season) TextView mediaSeason;

    @InjectView(R.id.media_description) TextView mediaDescription;
    @InjectView(R.id.directors) TextView mediaDirectors;
//    @InjectView(R.id.cast_list) GridLayout videoCastList;
//    @InjectView(R.id.additional_cast_list) TextView videoAdditionalCastList;
//    @InjectView(R.id.additional_cast_title) TextView videoAdditionalCastTitle;

    /**
     * Create a new instance of this, initialized to show the episode episodeId
     */
    public static TVShowEpisodeDetailsFragment newInstance(final int tvshowId, final int episodeId) {
        TVShowEpisodeDetailsFragment fragment = new TVShowEpisodeDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(TVSHOWID, tvshowId);
        args.putInt(EPISODEID, episodeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        tvshowId = getArguments().getInt(TVSHOWID, -1);
        episodeId = getArguments().getInt(EPISODEID, -1);

        if(episodeId == -1) {
            // There's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_episode_details, container, false);
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
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Start the loaders
        getLoaderManager().initLoader(LOADER_EPISODE, null, this);
//        getLoaderManager().initLoader(LOADER_CAST, null, this);

        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        // Force the exit view to invisible
        exitTransitionView.setVisibility(View.INVISIBLE);
        super.onResume();
    }

    @Override
    protected void onSyncProcessEnded(MediaSyncEvent event) {
        if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
            getLoaderManager().restartLoader(LOADER_EPISODE, null, this);
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
            case LOADER_EPISODE:
                uri = MediaContract.Episodes.buildTVShowEpisodeUri(getHostInfo().getId(), tvshowId, episodeId);
                return new CursorLoader(getActivity(), uri,
                        EpisodeDetailsQuery.PROJECTION, null, null, null);
//            case LOADER_CAST:
//                uri = MediaContract.MovieCast.buildMovieCastListUri(hostInfo.getId(), episodeId);
//                return new CursorLoader(getActivity(), uri,
//                        MovieCastListQuery.PROJECTION, null, null, MovieCastListQuery.SORT_BY_NAME_IGNORE_ARTICLES);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_EPISODE:
                    displayEpisodeDetails(cursor);
                    break;
//                case LOADER_CAST:
//                    displayCastList(cursor);
//                    break;
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
        item.episodeid = episodeId;
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
                    item.episodeid = episodeId;
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

    @OnClick(R.id.seen)
    public void onSeenClicked(View v) {
        // Set the playcount
        Integer playcount = (Integer)v.getTag();
        int newPlaycount = (playcount > 0) ? 0 : 1;

        VideoLibrary.SetEpisodeDetails action =
                new VideoLibrary.SetEpisodeDetails(episodeId, newPlaycount, null);
        action.execute(getHostManager().getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Force a refresh, but don't show a message
                if (!isAdded()) return;
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
        if (tvshowDownloadInfo == null) {
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
        File file = new File(tvshowDownloadInfo.getAbsoluteFilePath());
        if (file.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                    .setMessage(R.string.download_file_exists)
                    .setPositiveButton(R.string.overwrite,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                            tvshowDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                            callbackHandler);
                                }
                            })
                    .setNeutralButton(R.string.download_with_new_name,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                            tvshowDownloadInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                            callbackHandler);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, noopClickListener)
                    .show();
        } else {
            // Confirm that the user really wants to download the file
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                    .setMessage(R.string.confirm_episode_download)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), getHostInfo(),
                                            tvshowDownloadInfo, FileDownloadHelper.OVERWRITE_FILES,
                                            callbackHandler);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, noopClickListener)
                    .show();
        }
    }

    /**
     * Display the episode details
     *
     * @param cursor Cursor with the data
     */
    private void displayEpisodeDetails(Cursor cursor) {
        cursor.moveToFirst();
        mediaTitle.setText(cursor.getString(EpisodeDetailsQuery.TITLE));
        mediaUndertitle.setText(cursor.getString(EpisodeDetailsQuery.SHOWTITLE));

        int runtime = cursor.getInt(EpisodeDetailsQuery.RUNTIME) / 60;
        String durationPremiered =  runtime > 0 ?
                String.format(getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                        "  |  " + cursor.getString(EpisodeDetailsQuery.FIRSTAIRED) :
                cursor.getString(EpisodeDetailsQuery.FIRSTAIRED);
        mediaPremiered.setText(durationPremiered);
        String season = String.format(getString(R.string.season_episode),
                cursor.getInt(EpisodeDetailsQuery.SEASON),
                cursor.getInt(EpisodeDetailsQuery.EPISODE));
        mediaSeason.setText(season);

        double rating = cursor.getDouble(EpisodeDetailsQuery.RATING);
        if (rating > 0) {
            mediaRating.setVisibility(View.VISIBLE);
            mediaMaxRating.setVisibility(View.VISIBLE);
            mediaRating.setText(String.format("%01.01f", rating));
            mediaMaxRating.setText(getString(R.string.max_rating_video));
        } else {
            mediaRating.setVisibility(View.INVISIBLE);
            mediaMaxRating.setVisibility(View.INVISIBLE);
        }

        mediaDescription.setText(cursor.getString(EpisodeDetailsQuery.PLOT));
        mediaDirectors.setText(cursor.getString(EpisodeDetailsQuery.DIRECTOR));

        setupSeenButton(cursor.getInt(EpisodeDetailsQuery.PLAYCOUNT));

        // Images
        Resources resources = getActivity().getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int posterWidth = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_width);
        int posterHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_poster_height);
//        UIUtils.loadImageIntoImageview(hostManager,
//                cursor.getString(EpisodeDetailsQuery.THUMBNAIL),
//                mediaPoster, posterWidth, posterHeight);
        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height);
        UIUtils.loadImageIntoImageview(getHostManager(),
                cursor.getString(EpisodeDetailsQuery.THUMBNAIL),
                mediaArt, displayMetrics.widthPixels, artHeight);

        // Setup movie download info
        tvshowDownloadInfo = new FileDownloadHelper.TVShowInfo(
                cursor.getString(EpisodeDetailsQuery.SHOWTITLE),
                cursor.getInt(EpisodeDetailsQuery.SEASON),
                cursor.getInt(EpisodeDetailsQuery.EPISODE),
                cursor.getString(EpisodeDetailsQuery.TITLE),
                cursor.getString(EpisodeDetailsQuery.FILE));

        // Check if downloaded file exists
        downloadButton.setVisibility(View.VISIBLE);
        if (tvshowDownloadInfo.downloadFileExists()) {
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[]{
                    R.attr.colorAccent});
            downloadButton.setColorFilter(
                    styledAttributes.getColor(0, getResources().getColor(R.color.accent_default)));
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
            seenButton.setColorFilter(styledAttributes.getColor(0, getResources().getColor(R.color.accent_default)));
            styledAttributes.recycle();
        } else {
            seenButton.clearColorFilter();
        }
        // Save the playcount
        seenButton.setTag(playcount);
    }
//
//    /**
//     * Display the cast details
//     *
//     * @param cursor Cursor with the data
//     */
//    private void displayCastList(Cursor cursor) {
//        // Transform the cursor into a List<VideoType.Cast>
//
//        if (cursor.moveToFirst()) {
//            List<VideoType.Cast> castList = new ArrayList<VideoType.Cast>(cursor.getCount());
//            do {
//                castList.add(new VideoType.Cast(cursor.getString(MovieCastListQuery.NAME),
//                        cursor.getInt(MovieCastListQuery.ORDER),
//                        cursor.getString(MovieCastListQuery.ROLE),
//                        cursor.getString(MovieCastListQuery.THUMBNAIL)));
//            } while (cursor.moveToNext());
//
//            UIUtils.setupCastInfo(getActivity(), castList, videoCastList,
//                    videoAdditionalCastTitle, videoAdditionalCastList);
//        }
//    }
//
    /**
     * Episode details query parameters.
     */
    private interface EpisodeDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Episodes.TITLE,
                MediaContract.Episodes.SHOWTITLE,
                MediaContract.Episodes.SEASON,
                MediaContract.Episodes.EPISODE,
                MediaContract.Episodes.THUMBNAIL,
                MediaContract.Episodes.FANART,
                MediaContract.Episodes.FIRSTAIRED,
                MediaContract.Episodes.RUNTIME,
                MediaContract.Episodes.RATING,
                MediaContract.Episodes.PLOT,
                MediaContract.Episodes.PLAYCOUNT,
                MediaContract.Episodes.DIRECTOR,
                MediaContract.Episodes.WRITER,
                MediaContract.Episodes.FILE,
        };

        final int ID = 0;
        final int TITLE = 1;
        final int SHOWTITLE = 2;
        final int SEASON = 3;
        final int EPISODE = 4;
        final int THUMBNAIL = 5;
        final int FANART = 6;
        final int FIRSTAIRED = 7;
        final int RUNTIME = 8;
        final int RATING = 9;
        final int PLOT = 10;
        final int PLAYCOUNT = 11;
        final int DIRECTOR = 12;
        final int WRITER = 13;
        final int FILE = 14;
    }

//    /**
//     * Movie cast list query parameters.
//     */
//    private interface MovieCastListQuery {
//        String[] PROJECTION = {
//                MediaContract.MovieCast.ID,
//                MediaContract.MovieCast.NAME,
//                MediaContract.MovieCast.ORDER,
//                MediaContract.MovieCast.ROLE,
//                MediaContract.MovieCast.THUMBNAIL,
//        };
//
//        String SORT_BY_NAME_IGNORE_ARTICLES = MediaContract.MovieCast.ORDER + " ASC";
//
//        final int ID = 0;
//        final int NAME = 1;
//        final int ORDER = 2;
//        final int ROLE = 3;
//        final int THUMBNAIL = 4;
//    }
}
