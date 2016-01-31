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
package org.xbmc.kore.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.method.JSONRPC;
import org.xbmc.kore.jsonrpc.type.FilesType;

import java.io.File;
import java.util.List;

/**
 * Various methods to help with file downloading
 */
public class FileDownloadHelper {
    private static final String TAG = LogUtils.makeLogTag(FileDownloadHelper.class);

    public static final int OVERWRITE_FILES = 0,
            DOWNLOAD_WITH_NEW_NAME = 1;

    public static abstract class MediaInfo {
        public final String fileName;

        public MediaInfo(final String fileName) {
            this.fileName = fileName;
        }

        /**
         * Check whether the directory on which to load the file exists
         * @return Whether the directory exists
         */
        public boolean downloadDirectoryExists() {
            File file = new File(getAbsoluteDirectoryPath());
//            LogUtils.LOGD(TAG, "Checking directory: " + file.getPath());
//            LogUtils.LOGD(TAG, "Exists: " + file.exists());
            return file.exists();
        }

        /**
         * Check whether the file to download already exists
         * @return Whether the file exists
         */
        public boolean downloadFileExists() {
            if (!downloadDirectoryExists())
                return false;
            File file = new File(getAbsoluteFilePath());
            return file.exists();
        }

        public String getAbsoluteDirectoryPath() {
            File externalFilesDir = Environment.getExternalStoragePublicDirectory(getExternalPublicDirType());
            return externalFilesDir.getPath() + "/" + getRelativeDirectoryPath();

        }

        public String getAbsoluteFilePath() {
            return getAbsoluteDirectoryPath() + "/" + getDownloadFileName();
        }

        public String getRelativeFilePath() {
            return getRelativeDirectoryPath() + "/" + getDownloadFileName();
        }

        public abstract String getExternalPublicDirType();
        public abstract String getRelativeDirectoryPath();
        public abstract String getDownloadFileName();

        public abstract String getDownloadTitle(Context context);
        public String getDownloadDescrition(Context context) {
            return context.getString(R.string.download_file_description);
        }
    }

    /**
     * Info for downloading songs
     */
    public static class SongInfo extends MediaInfo {
        public final String artist;
        public final String album;
        public final int songId;
        public final int track;
        public final String title;

        public SongInfo(final String artist, final String album,
                        final int songId, final int track, final String title,
                        final String fileName) {
            super(fileName);
            this.artist = artist;
            this.album = album;
            this.songId = songId;
            this.track = track;
            this.title = title;
        }

        public String getRelativeDirectoryPath() {
            return (TextUtils.isEmpty(album) || TextUtils.isEmpty(artist)) ?
                    null : artist + "/" + album;
        }

        public String getDownloadFileName() {
            String ext = getFilenameExtension(fileName);
            return (ext != null) ?
                   String.valueOf(track) + " - " + title + ext :
                   null;
        }

        public String getExternalPublicDirType() {
            return Environment.DIRECTORY_MUSIC;
        }

        public String getDownloadTitle(Context context) {
            return title;
        }
    }

    /**
     * Info for downloading movies
     */
    public static class MovieInfo extends MediaInfo {
        public final String title;

        public MovieInfo(final String title, final String fileName) {
            super(fileName);
            this.title = title;
        }

        public String getRelativeDirectoryPath() {
            return (TextUtils.isEmpty(title)) ?
                   null : title;
        }

        public String getDownloadFileName() {
            String ext = getFilenameExtension(fileName);
            return (ext != null) ?
                   title + ext : null;
        }

        public String getExternalPublicDirType() {
            return Environment.DIRECTORY_MOVIES;
        }

        public String getDownloadTitle(Context context) {
            return title;
        }
    }

    /**
     * Info for downloading TVShows
     */
    public static class TVShowInfo extends MediaInfo {
        public final String tvshowTitle;
        public final int season;
        public final int episodeNumber;
        public final String title;

        public TVShowInfo(final String tvshowTitle, final int season,
                          final int episodeNumber, final String title,
                          final String fileName) {
            super(fileName);
            this.tvshowTitle = tvshowTitle;
            this.season = season;
            this.episodeNumber = episodeNumber;
            this.title = title;
        }

        public String getRelativeDirectoryPath() {
            if (season > 0) {
                return (TextUtils.isEmpty(tvshowTitle)) ?
                       null : tvshowTitle + "/Season" + String.valueOf(season);
            } else {
                return (TextUtils.isEmpty(tvshowTitle)) ?
                       null : tvshowTitle;
            }
        }

        public String getDownloadFileName() {
            String ext = getFilenameExtension(fileName);
            return (ext != null) ?
                   String.valueOf(episodeNumber) + " - " + title + ext : null;
        }

        public String getExternalPublicDirType() {
            return Environment.DIRECTORY_MOVIES;
        }

        public String getDownloadTitle(Context context) {
            return title;
        }
    }

    /**
     * Info for downloading music videos
     */
    public static class MusicVideoInfo extends MediaInfo {
        public final String title;

        private static final String SUBDIRECTORY = "Music Videos";

        public MusicVideoInfo(final String title, final String fileName) {
            super(fileName);
            this.title = title;
        }

        public String getRelativeDirectoryPath() {
            return (TextUtils.isEmpty(title)) ?
                   null : SUBDIRECTORY + "/" + title;
        }

        public String getDownloadFileName() {
            String ext = getFilenameExtension(fileName);
            return (ext != null) ?
                   title + ext : null;
        }

        public String getExternalPublicDirType() {
            return Environment.DIRECTORY_MUSIC;
        }

        public String getDownloadTitle(Context context) {
            return title;
        }
    }

    /**
     * Auxiliary method to get a filename extension, assuming the filename ends with .ext
     * @param filename File name
     * @return Extension if present, or null
     */
    public static String getFilenameExtension(String filename) {
        int idx = filename.lastIndexOf(".");
        return (idx > 0) ? filename.substring(idx) : null;
    }

    public static void downloadFiles(final Context context, final HostInfo hostInfo,
                                     final MediaInfo mediaInfo,
                                     final int fileHandlingMode,
                                     final Handler callbackHandler) {
        if (mediaInfo == null)
            return;

        if (!checkDownloadDir(context, mediaInfo.getAbsoluteDirectoryPath()))
            return;

        // Check if we are connected to the host
        final HostConnection httpHostConnection = new HostConnection(hostInfo);
        httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        JSONRPC.Ping action = new JSONRPC.Ping();
        action.execute(httpHostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Ok, continue, iterate through the song list and launch a download for each
                final DownloadManager downloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);

                downloadSingleFile(context, httpHostConnection, hostInfo,
                        mediaInfo, fileHandlingMode, downloadManager, callbackHandler);
            }

            @Override
            public void onError(int errorCode, String description) {
                Toast.makeText(context, R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }

    public static void downloadFiles(final Context context, final HostInfo hostInfo,
                                    final List<? extends MediaInfo> mediaInfoList,
                                    final int fileHandlingMode,
                                    final Handler callbackHandler) {
        if ((mediaInfoList == null) || (mediaInfoList.size() == 0))
            return;

        if (!checkDownloadDir(context, mediaInfoList.get(0).getAbsoluteDirectoryPath()))
            return;

        // Check if we are connected to the host
        final HostConnection httpHostConnection = new HostConnection(hostInfo);
        httpHostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);

        JSONRPC.Ping action = new JSONRPC.Ping();
        action.execute(httpHostConnection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Ok, continue, iterate through the song list and launch a download for each
                final DownloadManager downloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);

                for (final MediaInfo mediaInfo : mediaInfoList) {
                    downloadSingleFile(context, httpHostConnection, hostInfo,
                            mediaInfo, fileHandlingMode, downloadManager, callbackHandler);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                Toast.makeText(context, R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                        .show();
            }
        }, callbackHandler);
    }

    private static boolean checkDownloadDir(Context context, String downloadDirPath) {
        File downloadDir = new File(downloadDirPath);
        if ((downloadDir.exists() && !downloadDir.isDirectory())) {
            Toast.makeText(context,
                    "Download directory already exists and is not a directory.",
                    Toast.LENGTH_SHORT)
                 .show();
            return false;
        }
        if (!downloadDir.isDirectory() && !downloadDir.mkdirs()) {
            Toast.makeText(context,
                    "Couldn't create download directory: " + downloadDir.getPath(),
                    Toast.LENGTH_SHORT)
                 .show();
            return false;
        }
        return true;
    }

    private static void downloadSingleFile(final Context context,
                                           final HostConnection httpHostConnection,
                                           final HostInfo hostInfo,
                                           final MediaInfo mediaInfo,
                                           final int fileHandlingMode,
                                           final DownloadManager downloadManager,
                                           final Handler callbackHandler) {
        Files.PrepareDownload action = new Files.PrepareDownload(mediaInfo.fileName);
        action.execute(httpHostConnection, new ApiCallback<FilesType.PrepareDownloadReturnType>() {
            @Override
            public void onSuccess(FilesType.PrepareDownloadReturnType result) {
                // If the file exists and it's to be overwritten, delete it,
                // as the DownloadManager always creates a new name
                if (fileHandlingMode == OVERWRITE_FILES) {
                    File file = new File(mediaInfo.getAbsoluteFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }

                // Ok, we got the path, invoke downloader
                Uri uri = Uri.parse(hostInfo.getHttpURL() + "/" + result.path);
                DownloadManager.Request request = new DownloadManager.Request(uri);
                // http basic authorization
                if ((hostInfo.getUsername() != null) && !hostInfo.getUsername().isEmpty() &&
                    (hostInfo.getPassword() != null) && !hostInfo.getPassword().isEmpty()) {
                    final String token = Base64.encodeToString((hostInfo.getUsername() + ":" +
                                                                hostInfo.getPassword()).getBytes(), Base64.DEFAULT);
                    request.addRequestHeader("Authorization", "Basic " + token);
                }
                request.allowScanningByMediaScanner();
                request.setAllowedNetworkTypes(Settings.allowedDownloadNetworkTypes(context));
                request.setTitle(mediaInfo.getDownloadTitle(context));
                request.setDescription(mediaInfo.getDownloadDescrition(context));

                request.setDestinationInExternalPublicDir(mediaInfo.getExternalPublicDirType(),
                        mediaInfo.getRelativeFilePath());
                downloadManager.enqueue(request);
            }

            @Override
            public void onError(int errorCode, String description) {
                Toast.makeText(context,
                        String.format(context.getString(R.string.error_getting_file_information),
                                mediaInfo.getDownloadTitle(context)),
                        Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }
}
