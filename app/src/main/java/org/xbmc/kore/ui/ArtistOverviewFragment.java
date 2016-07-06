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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ArtistOverviewFragment extends AbstractDetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(ArtistOverviewFragment.class);

    public static final String BUNDLE_KEY_ARTISTID = "id";
    public static final String POSTER_TRANS_NAME = "POSTER_TRANS_NAME";
    public static final String BUNDLE_KEY_TITLE = "title";
    public static final String BUNDLE_KEY_GENRE = "genre";
    public static final String BUNDLE_KEY_DESCRIPTION = "description";
    public static final String BUNDLE_KEY_FANART = "fanart";
    public static final String BUNDLE_KEY_POSTER = "poster";

    // Loader IDs
    private static final int LOADER_ARTIST = 0,
            LOADER_ALBUMS = 1,
            LOADER_SONGS = 2;

    private HostManager hostManager;
    private HostInfo hostInfo;

    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    private int artistId = -1;

    private String artistTitle;

    private HashMap<Integer, String> albumTitles = new HashMap<>();

    @InjectView(R.id.exit_transition_view) View exitTransitionView;
    // Buttons
    @InjectView(R.id.fab) ImageButton fabButton;

    // Detail views
    @InjectView(R.id.media_panel) ScrollView mediaPanel;
    @InjectView(R.id.art) ImageView mediaArt;
    @InjectView(R.id.poster) ImageView mediaPoster;
    @InjectView(R.id.media_title) TextView mediaTitle;
    @InjectView(R.id.media_undertitle) TextView mediaUndertitle;
    @InjectView(R.id.media_description) TextView mediaDescription;

    @TargetApi(21)
    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        Bundle bundle = getArguments();
        artistId = bundle.getInt(BUNDLE_KEY_ARTISTID, -1);

        if ((container == null) || (artistId == -1)) {
            // We're not being shown or there's nothing to show
            return null;
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_artist_details, container, false);
        ButterKnife.inject(this, root);

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

        ((FloatingActionButton) fabButton).attachToScrollView((ObservableScrollView) mediaPanel);

        if(Utils.isLollipopOrLater()) {
            mediaPoster.setTransitionName(bundle.getString(POSTER_TRANS_NAME));
        }

        artistTitle = bundle.getString(BUNDLE_KEY_TITLE);

        mediaTitle.setText(artistTitle);

        mediaUndertitle.setText(bundle.getString(BUNDLE_KEY_GENRE));
        mediaDescription.setText(bundle.getString(BUNDLE_KEY_DESCRIPTION));
        setArt(bundle.getString(BUNDLE_KEY_POSTER), bundle.getString(BUNDLE_KEY_FANART));

        return root;
    }

    @Override
    protected String getSyncType() {
        return null;
    }

    @Override
    protected String getSyncID() {
        return null;
    }

    @Override
    protected int getSyncItemID() {
        return 0;
    }

    @Override
    protected SwipeRefreshLayout getSwipeRefreshLayout() {
        return null;
    }

    @Override
    protected void onDownload() {
        getLoaderManager().initLoader(LOADER_ALBUMS, null, this);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        //Make sure loader is not reloaded for albums and songs when we return
        //These loaders should only be activated by the user pressing the download button
        getLoaderManager().destroyLoader(LOADER_ALBUMS);
        getLoaderManager().destroyLoader(LOADER_SONGS);
        super.onPause();
    }

    @Override
    protected void onSyncProcessEnded(MediaSyncEvent event) {

    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        switch (i) {
            case LOADER_ARTIST:
                uri = MediaContract.Artists.buildArtistUri(hostInfo.getId(), artistId);
                return new CursorLoader(getActivity(), uri,
                                        DetailsQuery.PROJECTION, null, null, null);
            case LOADER_ALBUMS:
                uri = MediaContract.AlbumArtists.buildAlbumsForArtistListUri(hostInfo.getId(), artistId);
                return new CursorLoader(getActivity(), uri,
                                        AlbumListQuery.PROJECTION, null, null, null);
            case LOADER_SONGS:
                uri = MediaContract.Songs.buildArtistSongsListUri(hostInfo.getId(), artistId);
                return new CursorLoader(getActivity(), uri,
                                        SongsListQuery.PROJECTION, null, null, SongsListQuery.SORT);
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_ARTIST:
                    displayArtistDetails(cursor);
                    break;
                case LOADER_ALBUMS:
                    createAlbumList(cursor);
                    getLoaderManager().initLoader(LOADER_SONGS, null, this);
                    break;
                case LOADER_SONGS:
                    downloadSongs(cursor);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Release loader's data
    }

    @OnClick(R.id.fab)
    public void onFabClicked(View v) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.artistid = artistId;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
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
        final PlaylistType.Item playListItem = new PlaylistType.Item();
        playListItem.artistid = artistId;
        MediaPlayerUtils.queue(this, playListItem, PlaylistType.GetPlaylistsReturnType.AUDIO);
    }

    private FileDownloadHelper.SongInfo createSongInfo(Cursor cursor) {
        FileDownloadHelper.SongInfo songInfo = null;
        String albumTitle = albumTitles.get(cursor.getInt(SongsListQuery.ALBUMID));
        if (albumTitle != null) {
            // Add this song to the list
            songInfo = new FileDownloadHelper.SongInfo(
                    artistTitle,
                    albumTitle,
                    cursor.getInt(SongsListQuery.SONGID),
                    cursor.getInt(SongsListQuery.TRACK),
                    cursor.getString(SongsListQuery.TITLE),
                    cursor.getString(SongsListQuery.FILE));
        }
        return songInfo;
    }

    private void createAlbumList(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                albumTitles.put(cursor.getInt(AlbumListQuery.ALBUMID), cursor.getString(AlbumListQuery.TITLE));
            } while(cursor.moveToNext());
        }
    }

    private void downloadSongs(Cursor cursor) {
        DialogInterface.OnClickListener noopClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }
                };

        final ArrayList<FileDownloadHelper.SongInfo> songInfoList = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                FileDownloadHelper.SongInfo songInfo = createSongInfo(cursor);
                if (songInfo != null) {
                    songInfoList.add(songInfo);
                }
            } while (cursor.moveToNext());
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
                   .setNegativeButton(android.R.string.cancel, noopClickListener)
                   .show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download)
                   .setMessage(R.string.confirm_artist_download)
                   .setPositiveButton(android.R.string.ok,
                                      new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                              FileDownloadHelper.downloadFiles(getActivity(), hostInfo,
                                                                               songInfoList, FileDownloadHelper.OVERWRITE_FILES,
                                                                               callbackHandler);
                                          }
                                      })
                   .setNegativeButton(android.R.string.cancel, noopClickListener)
                   .show();
        }
    }

    private void displayArtistDetails(Cursor cursor) {
        cursor.moveToFirst();
        artistTitle = cursor.getString(DetailsQuery.ARTIST);
        mediaTitle.setText(artistTitle);
        mediaUndertitle.setText(cursor.getString(DetailsQuery.GENRE));
        mediaDescription.setText(cursor.getString(DetailsQuery.DESCRIPTION));
        setArt(cursor.getString(DetailsQuery.THUMBNAIL), cursor.getString(DetailsQuery.FANART));
    }

    private void setArt(String poster, String fanart) {
        final Resources resources = getActivity().getResources();

        // Images
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int artHeight = resources.getDimensionPixelOffset(R.dimen.now_playing_art_height),
                artWidth = displayMetrics.widthPixels;
        int posterWidth = resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_width);
        int posterHeight = resources.getDimensionPixelOffset(R.dimen.albumdetail_poster_heigth);
        UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                                             poster, artistTitle,
                                             mediaPoster, posterWidth, posterHeight);
        UIUtils.loadImageIntoImageview(hostManager,
                                       TextUtils.isEmpty(fanart) ? poster : fanart,
                                       mediaArt, artWidth, artHeight);
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

    private interface DetailsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Artists.ARTISTID,
                MediaContract.Artists.ARTIST,
                MediaContract.Artists.GENRE,
                MediaContract.Artists.THUMBNAIL,
                MediaContract.Artists.DESCRIPTION,
                MediaContract.Artists.FANART
        };

        int ID = 0;
        int ARTISTID = 1;
        int ARTIST = 2;
        int GENRE = 3;
        int THUMBNAIL = 4;
        int DESCRIPTION = 5;
        int FANART = 6;
    }

    private interface AlbumListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Albums.ALBUMID,
                MediaContract.Albums.TITLE,
        };

        int ID = 0;
        int ALBUMID = 1;
        int TITLE = 2;
    }

    /**
     * Song list query parameters.
     */
    private interface SongsListQuery {
        String[] PROJECTION = {
                MediaDatabase.Tables.SONGS + "." + BaseColumns._ID,
                MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.TITLE,
                MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.TRACK,
                MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.DURATION,
                MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.FILE,
                MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.SONGID,
                MediaDatabase.Tables.SONGS + "." + MediaContract.Songs.ALBUMID,
        };

        String SORT = MediaContract.Songs.TRACK + " ASC";

        int ID = 0;
        int TITLE = 1;
        int TRACK = 2;
        int DURATION = 3;
        int FILE = 4;
        int SONGID = 5;
        int ALBUMID = 6;
    }
}
