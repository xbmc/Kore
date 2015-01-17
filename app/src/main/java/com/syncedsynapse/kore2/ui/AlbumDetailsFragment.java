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

package com.syncedsynapse.kore2.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.Settings;
import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.ApiCallback;
import com.syncedsynapse.kore2.jsonrpc.ApiMethod;
import com.syncedsynapse.kore2.jsonrpc.method.Player;
import com.syncedsynapse.kore2.jsonrpc.method.Playlist;
import com.syncedsynapse.kore2.jsonrpc.type.PlaylistType;
import com.syncedsynapse.kore2.provider.MediaContract;
import com.syncedsynapse.kore2.utils.FileDownloadHelper;
import com.syncedsynapse.kore2.utils.LogUtils;
import com.syncedsynapse.kore2.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Presents movie details
 */
public class AlbumDetailsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(AlbumDetailsFragment.class);

    public static final String ALBUMID = "album_id";

    // Loader IDs
    private static final int LOADER_ALBUM = 0,
            LOADER_SONGS = 1;

    private HostManager hostManager;
    private HostInfo hostInfo;
    private EventBus bus;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    // Displayed album id
    private int albumId = -1;

    // Album information
    private String albumDisplayArtist;
    private String albumTitle;
    private List<FileDownloadHelper.SongInfo> songInfoList = null;

    @InjectView(R.id.exit_transition_view) View exitTransitionView;
    // Buttons
    @InjectView(R.id.fab) ImageButton fabButton;
    @InjectView(R.id.add_to_playlist) ImageButton addToPlaylistButton;
    @InjectView(R.id.download) ImageButton downloadButton;

    // Detail views
    @InjectView(R.id.media_panel) ScrollView mediaPanel;

    @InjectView(R.id.art) ImageView mediaArt;
    @InjectView(R.id.poster) ImageView mediaPoster;

    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;

    @InjectView(R.id.rating) TextView mediaRating;
    @InjectView(R.id.max_rating) TextView mediaMaxRating;
    @InjectView(R.id.year) TextView mediaYear;
//    @InjectView(R.id.genres) TextView mediaGenres;

    @InjectView(R.id.media_description_container) LinearLayout mediaDescriptionContainer;
    @InjectView(R.id.media_description) TextView mediaDescription;
    @InjectView(R.id.show_all) ImageView mediaShowAll;

    @InjectView(R.id.song_list) LinearLayout songListView;

    /**
     * Create a new instance of this, initialized to show the album albumId
     */
    public static AlbumDetailsFragment newInstance(int albumId) {
        AlbumDetailsFragment fragment = new AlbumDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(ALBUMID, albumId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        albumId = getArguments().getInt(ALBUMID, -1);

        if ((container == null) || (albumId == -1)) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_album_details, container, false);
        ButterKnife.inject(this, root);

        bus = EventBus.getDefault();
        hostManager = HostManager.getInstance(getActivity());
        hostInfo = hostManager.getHostInfo();

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
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Start the loaders
        getLoaderManager().initLoader(LOADER_ALBUM, null, this);

        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        // Force the exit view to invisible
        exitTransitionView.setVisibility(View.INVISIBLE);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        switch (i) {
            case LOADER_ALBUM:
                uri = MediaContract.Albums.buildAlbumUri(hostInfo.getId(), albumId);
                return new CursorLoader(getActivity(), uri,
                        AlbumDetailsQuery.PROJECTION, null, null, null);
            case LOADER_SONGS:
                uri = MediaContract.Songs.buildSongsListUri(hostInfo.getId(), albumId);
                return new CursorLoader(getActivity(), uri,
                        AlbumSongsListQuery.PROJECTION, null, null, AlbumSongsListQuery.SORT);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_ALBUM:
                    displayAlbumDetails(cursor);
                    getLoaderManager().initLoader(LOADER_SONGS, null, this);
                    break;
                case LOADER_SONGS:
                    displaySongsList(cursor);
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
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();

    @OnClick(R.id.fab)
    public void onFabClicked(View v) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.albumid = albumId;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSucess(String result) {
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
                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }

    @OnClick(R.id.add_to_playlist)
    public void onAddToPlaylistClicked(View v) {
        addToPlaylist(TYPE_ALBUM, albumId);
    }

    @OnClick(R.id.download)
    public void onDownloadClicked(View v) {
        if ((albumTitle == null) || (albumDisplayArtist == null) ||
            (songInfoList == null) || (songInfoList.size() == 0)) {
            // Nothing to download
            Toast.makeText(getActivity(), R.string.no_files_to_download, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the directory exists and whether to overwrite it
        File file = new File(songInfoList.get(0).getAbsoluteDirectoryPath());
        if (file.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                   .setMessage(R.string.download_dir_exists)
                   .setPositiveButton(R.string.overwrite,
                           new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                                           songInfoList, FileDownloadHelper.OVERWRITE_FILES,
                                           callbackHandler);
                               }
                           })
                    .setNeutralButton(R.string.download_with_new_name,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                                            songInfoList, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                            callbackHandler);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Nothing to do
                                }
                            })
                    .show();
        } else {
            FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                    songInfoList, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                    callbackHandler);
        }
    }

    private boolean isDescriptionExpanded = false;
    /**
     * Display the album details
     *
     * @param cursor Cursor with the data
     */
    private void displayAlbumDetails(Cursor cursor) {
        final Resources resources = getActivity().getResources();

        cursor.moveToFirst();
        albumTitle = cursor.getString(AlbumDetailsQuery.TITLE);
        albumDisplayArtist = cursor.getString(AlbumDetailsQuery.DISPLAYARTIST);
        mediaTitle.setText(albumTitle);
        mediaUndertitle.setText(albumDisplayArtist);

        int year = cursor.getInt(AlbumDetailsQuery.YEAR);
        String genres = cursor.getString(AlbumDetailsQuery.GENRE);
        String label = (year > 0) ?
                       (!TextUtils.isEmpty(genres) ?
                        genres + "  |  " + String.valueOf(year) :
                        String.valueOf(year)) :
                       genres;
        mediaYear.setText(label);

        double rating = cursor.getDouble(AlbumDetailsQuery.RATING);
        if (rating > 0) {
            mediaRating.setVisibility(View.VISIBLE);
            mediaMaxRating.setVisibility(View.VISIBLE);
            mediaRating.setText(String.format("%01.01f", rating));
            mediaMaxRating.setText(getString(R.string.max_rating_music));
        } else {
            mediaRating.setVisibility(View.GONE);
            mediaMaxRating.setVisibility(View.GONE);
        }

        String description = cursor.getString(AlbumDetailsQuery.DESCRIPTION);
        if (!TextUtils.isEmpty(description)) {
            mediaDescription.setVisibility(View.VISIBLE);
            mediaDescription.setText(description);
            final int maxLines = resources.getInteger(R.integer.description_max_lines);
            Resources.Theme theme = getActivity().getTheme();
            TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
                    R.attr.iconExpand,
                    R.attr.iconCollapse
            });
            final int iconCollapseResId = styledAttributes.getResourceId(0,
                    R.drawable.ic_expand_less_white_24dp);
            final int iconExpandResId = styledAttributes.getResourceId(1,
                    R.drawable.ic_expand_more_white_24dp);
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
        } else {
            mediaDescriptionContainer.setVisibility(View.GONE);
        }

        // Images
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        String fanart = cursor.getString(AlbumDetailsQuery.FANART),
                poster = cursor.getString(AlbumDetailsQuery.THUMBNAIL);

        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        if (!TextUtils.isEmpty(fanart)) {
            int posterWidth = resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_width);
            int posterHeight = resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_heigth);
            mediaPoster.setVisibility(View.VISIBLE);
            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                    poster, albumTitle,
                    mediaPoster, posterWidth, posterHeight);
            UIUtils.loadImageIntoImageview(hostManager,
                    fanart,
                    mediaArt, artWidth, artHeight);
        } else {
            // No fanart, just present the poster
            mediaPoster.setVisibility(View.GONE);
            UIUtils.loadImageIntoImageview(hostManager,
                    poster,
                    mediaArt, artWidth, artHeight);
            // Reset padding
            int paddingLeft = mediaTitle.getPaddingRight(),
                    paddingRight = mediaTitle.getPaddingRight(),
                    paddingTop = mediaTitle.getPaddingTop(),
                    paddingBottom = mediaTitle.getPaddingBottom();
            mediaTitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            mediaUndertitle.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }
    }

    /**
     * Starts playing the song on XBMC
     * @param songId song to play
     */
    private void playSong(int songId) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.songid = songId;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), defaultStringActionCallback, callbackHandler);
    }

    private int TYPE_ALBUM = 0,
            TYPE_SONG = 1;
    /**
     * Adds an album or a song to the audio playlist
     * @param type Album or song
     * @param id albumId or songId
     */
    private void addToPlaylist(final int type, final int id) {
        Playlist.GetPlaylists getPlaylists = new Playlist.GetPlaylists();

        getPlaylists.execute(hostManager.getConnection(), new ApiCallback<ArrayList<PlaylistType.GetPlaylistsReturnType>>() {
            @Override
            public void onSucess(ArrayList<PlaylistType.GetPlaylistsReturnType> result) {
                // Ok, loop through the playlists, looking for the audio one
                int audioPlaylistId = -1;
                for (PlaylistType.GetPlaylistsReturnType playlist : result) {
                    if (playlist.type.equals(PlaylistType.GetPlaylistsReturnType.AUDIO)) {
                        audioPlaylistId = playlist.playlistid;
                        break;
                    }
                }
                // If found, add to playlist
                if (audioPlaylistId != -1) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    if (type == TYPE_ALBUM) {
                        item.albumid = id;
                    } else {
                        item.songid = id;
                    }
                    Playlist.Add action = new Playlist.Add(audioPlaylistId, item);
                    action.execute(hostManager.getConnection(), new ApiCallback<String>() {
                        @Override
                        public void onSucess(String result) {
                            // Got an error, show toast
                            Toast.makeText(getActivity(), R.string.item_added_to_playlist, Toast.LENGTH_SHORT)
                                 .show();
                        }

                        @Override
                        public void onError(int errorCode, String description) {
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
                // Got an error, show toast
                Toast.makeText(getActivity(), R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }

    View.OnClickListener songClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playSong(((FileDownloadHelper.SongInfo)v.getTag()).songId);
        }
    };

    private View.OnClickListener songItemMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final FileDownloadHelper.SongInfo songInfo = ((FileDownloadHelper.SongInfo)v.getTag());
            final int songId = songInfo.songId;

            final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.song_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_play_song:
                            playSong(songId);
                            return true;
                        case R.id.action_add_to_playlist:
                            addToPlaylist(TYPE_SONG, songId);
                            return true;
                        case R.id.download:
                            // Check if the file exists and whether to overwrite it
                            File file = new File(songInfo.getAbsoluteFilePath());
                            if (file.exists()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle(R.string.download)
                                       .setMessage(R.string.download_file_exists)
                                       .setPositiveButton(R.string.overwrite,
                                               new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(DialogInterface dialog, int which) {
                                                       FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                                                               songInfo, FileDownloadHelper.OVERWRITE_FILES,
                                                               callbackHandler);
                                                   }
                                               })
                                       .setNeutralButton(R.string.download_with_new_name,
                                               new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(DialogInterface dialog, int which) {
                                                       FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                                                               songInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                               callbackHandler);
                                                   }
                                               })
                                       .setNegativeButton(android.R.string.cancel,
                                               new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(DialogInterface dialog, int which) { }
                                               })
                                       .show();
                            } else {
                                FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                                        songInfo, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                        callbackHandler);
                            }
                            return true;
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };

    /**
     * Display the songs
     *
     * @param cursor Cursor with the data
     */
    private void displaySongsList(Cursor cursor) {
        if (cursor.moveToFirst()) {
            songInfoList = new ArrayList<FileDownloadHelper.SongInfo>(cursor.getCount());
            do {
                View songView = LayoutInflater.from(getActivity())
                                              .inflate(R.layout.list_item_song, songListView, false);
                TextView songTitle = (TextView)songView.findViewById(R.id.song_title);
                TextView trackNumber = (TextView)songView.findViewById(R.id.track_number);
                TextView duration = (TextView)songView.findViewById(R.id.duration);
                ImageView contextMenu = (ImageView)songView.findViewById(R.id.list_context_menu);

                // Add this song to the list
                FileDownloadHelper.SongInfo songInfo = new FileDownloadHelper.SongInfo(
                        albumDisplayArtist,
                        albumTitle,
                        cursor.getInt(AlbumSongsListQuery.SONGID),
                        cursor.getInt(AlbumSongsListQuery.TRACK),
                        cursor.getString(AlbumSongsListQuery.TITLE),
                        cursor.getString(AlbumSongsListQuery.FILE));
                songInfoList.add(songInfo);

                songTitle.setText(songInfo.title);
                trackNumber.setText(String.valueOf(songInfo.track));
                duration.setText(UIUtils.formatTime(cursor.getInt(AlbumSongsListQuery.DURATION)));

                contextMenu.setTag(songInfo);
                contextMenu.setOnClickListener(songItemMenuClickListener);

                songView.setTag(songInfo);
                songView.setOnClickListener(songClickListener);
                songListView.addView(songView);
            } while (cursor.moveToNext());

            if (songInfoList.size() > 0) {
                downloadButton.setVisibility(View.VISIBLE);
                // Check if download dir exists
                FileDownloadHelper.SongInfo songInfo = new FileDownloadHelper.SongInfo
                        (albumDisplayArtist, albumTitle, 0, 0, null, null);
                if (songInfo.downloadDirectoryExists()) {
                    Resources.Theme theme = getActivity().getTheme();
                    TypedArray styledAttributes = theme.obtainStyledAttributes(new int[]{
                            R.attr.colorAccent});
                    downloadButton.setColorFilter(
                            styledAttributes.getColor(0, R.color.accent_default));
                    styledAttributes.recycle();
                } else {
                    downloadButton.clearColorFilter();
                }
            }
        }
    }

    /**
     * Album details query parameters.
     */
    private interface AlbumDetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Albums.TITLE,
                MediaContract.Albums.DISPLAYARTIST,
                MediaContract.Albums.THUMBNAIL,
                MediaContract.Albums.FANART,
                MediaContract.Albums.YEAR,
                MediaContract.Albums.GENRE,
                MediaContract.Albums.ALBUMLABEL,
                MediaContract.Albums.DESCRIPTION,
                MediaContract.Albums.RATING,
        };

        final int ID = 0;
        final int TITLE = 1;
        final int DISPLAYARTIST = 2;
        final int THUMBNAIL = 3;
        final int FANART = 4;
        final int YEAR = 5;
        final int GENRE = 6;
        final int ALBUMLABEL = 7;
        final int DESCRIPTION = 8;
        final int RATING = 9;
    }

    /**
     * Movie cast list query parameters.
     */
    private interface AlbumSongsListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Songs.TITLE,
                MediaContract.Songs.TRACK,
                MediaContract.Songs.DURATION,
                MediaContract.Songs.FILE,
                MediaContract.Songs.SONGID,
        };

        String SORT = MediaContract.Songs.TRACK + " ASC";

        final int ID = 0;
        final int TITLE = 1;
        final int TRACK = 2;
        final int DURATION = 3;
        final int FILE = 4;
        final int SONGID = 5;
    }
}
