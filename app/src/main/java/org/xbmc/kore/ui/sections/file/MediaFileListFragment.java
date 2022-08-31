/*
 * Copyright 2015 DanhDroid. All rights reserved.
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
package org.xbmc.kore.ui.sections.file;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ItemType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.AbstractListFragment;
import org.xbmc.kore.ui.viewgroups.GridRecyclerView;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Presents a list of files of different types (Video/Music)
 */
public class MediaFileListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(MediaFileListFragment.class);

    public static final String MEDIA_TYPE = "mediaType";
    public static final String SORT_METHOD = "sortMethod";
    public static final String CURRENT_LOCATION = "currentLocation";
    public static final String DELAY_LOAD = "delayLoad";

    private static final String ADDON_SOURCE = "addons:";

    private HostManager hostManager;
    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    String mediaType = Files.Media.FILES;
    ListType.Sort sortMethod = null;
    /**
     * The current location we're browsing. Can be null, if we're at the main Sources level
     */
    FileLocation currentLocation = null;
    /**
     * Whether to delay loading until onResume, instead of when we get a valid connection
     */
    boolean delayLoad = false;

    Queue<FileLocation> mediaQueueFileLocation = new LinkedList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostManager = HostManager.getInstance(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mediaType = args.getString(MEDIA_TYPE, Files.Media.FILES);
            sortMethod = args.getParcelable(SORT_METHOD);
            currentLocation = args.getParcelable(CURRENT_LOCATION);
            delayLoad = args.getBoolean(DELAY_LOAD, false);
        }

        if (savedInstanceState != null) {
            mediaType = savedInstanceState.getString(MEDIA_TYPE, Files.Media.FILES);
            sortMethod = savedInstanceState.getParcelable(SORT_METHOD);
            currentLocation = savedInstanceState.getParcelable(CURRENT_LOCATION);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If we were asked to delay the load, only load now, as we're only visible now
        if (delayLoad) {
            delayLoad = false;
            if (currentLocation != null && lastConnectionStatusResult == CONNECTION_SUCCESS)
                browseDirectory(currentLocation);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MEDIA_TYPE, mediaType);
        outState.putParcelable(SORT_METHOD, sortMethod);
        outState.putParcelable(CURRENT_LOCATION, currentLocation);
    }

    @Override
    public void onConnectionStatusSuccess() {
        boolean refresh = lastConnectionStatusResult != CONNECTION_SUCCESS;
        super.onConnectionStatusSuccess();

        if (delayLoad) showStatusMessage(getString(R.string.loading), null);

        // Refresh contents if we're not asked to delay and if we're transioning to a successful connection
        if (delayLoad || !refresh) return;
        onRefresh();
    }

    @Override
    public void onRefresh() {
        if (currentLocation == null) {
            browseSources();
        } else {
            browseDirectory(currentLocation);
        }
    }

    @Override
    protected GridRecyclerView.OnItemClickListener createOnItemClickListener() {
        return (view, position) -> handleFileSelect(((MediaFileListAdapter) getAdapter()).getItem(position));
    }

    @Override
    protected RecyclerView.Adapter<ViewHolder> createAdapter() {
        return new MediaFileListAdapter(requireContext(), R.layout.item_file);
    }

    @Override
    protected String getEmptyResultsTitle() { return getString(R.string.source_empty); }

    void handleFileSelect(FileLocation f) {
        if (f == null || f.file == null) {
            // If at first level, browse Sources
            browseSources();
        } else if (f.isDirectory) {
            browseDirectory(f);
        } else {
            playMediaFile(f.file);
        }
    }

    public boolean navigateToParentDir() {
        if (currentLocation != null && !currentLocation.isRoot) {
            handleFileSelect(currentLocation.parentDirectory);
            return true;
        }
        return false;
    }

    public boolean atRootDirectory() {
        return (currentLocation == null || currentLocation.isRoot);
    }

    private int getPlaylistId(String mediaType) {
        if (mediaType.equalsIgnoreCase(Files.Media.VIDEO)) {
            return PlaylistType.VIDEO_PLAYLISTID;
        } else if (mediaType.equalsIgnoreCase(Files.Media.PICTURES)) {
            return PlaylistType.PICTURE_PLAYLISTID;
        } else {
            return PlaylistType.MUSIC_PLAYLISTID;
        }
    }

    /**
     * Gets and presents the list of media sources
     */
    private void browseSources() {
        currentLocation = null;
        Files.GetSources action = new Files.GetSources(mediaType);
        action.execute(hostManager.getConnection(), new ApiCallback<List<ItemType.Source>>() {
            @Override
            public void onSuccess(List<ItemType.Source> result) {
                if (!isAdded()) return;
                hideRefreshAnimation();
                // Add the sources, setting no parent dir as it doesn't exist but not setting them as root
                List<FileLocation> rootContents = new ArrayList<>();
                for (ItemType.Source item : result) {
                    if ((item.file != null) && (!item.file.contains(ADDON_SOURCE))) {
                        rootContents.add(new FileLocation(item.label, item.file, true, null, false));
                    }
                }
                ((MediaFileListAdapter) getAdapter()).setFilelistItems(rootContents);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isResumed()) return;
                hideRefreshAnimation();
                showStatusMessage(null, String.format(getString(R.string.error_getting_source_info), description));
            }
        }, callbackHandler);
    }

    /**
     * Gets and presents the files of the specified directory
     * @param dir Directory to browse
     */
    private void browseDirectory(final FileLocation dir) {
        String[] properties = new String[] {
                ListType.FieldsFiles.TITLE, ListType.FieldsFiles.ARTIST,
                //ListType.FieldsFiles.ALBUMARTIST, ListType.FieldsFiles.GENRE,
                //ListType.FieldsFiles.YEAR, ListType.FieldsFiles.RATING,
                ListType.FieldsFiles.ALBUM, ListType.FieldsFiles.TRACK, ListType.FieldsFiles.DURATION,
                //ListType.FieldsFiles.COMMENT,
                //ListType.FieldsFiles.LYRICS, ListType.FieldsFiles.MUSICBRAINZTRACKID,
                //ListType.FieldsFiles.MUSICBRAINZARTISTID, ListType.FieldsFiles.MUSICBRAINZALBUMID,
                //ListType.FieldsFiles.MUSICBRAINZALBUMARTISTID, ListType.FieldsFiles.PLAYCOUNT,
                //ListType.FieldsFiles.FANART,
                //ListType.FieldsFiles.DIRECTOR, ListType.FieldsFiles.TRAILER,
                ListType.FieldsFiles.TAGLINE,
                //ListType.FieldsFiles.PLOT, ListType.FieldsFiles.PLOTOUTLINE, ListType.FieldsFiles.ORIGINALTITLE,
                //ListType.FieldsFiles.LASTPLAYED, ListType.FieldsFiles.WRITER, ListType.FieldsFiles.STUDIO,
                //ListType.FieldsFiles.MPAA, ListType.FieldsFiles.CAST, ListType.FieldsFiles.COUNTRY,
                //ListType.FieldsFiles.IMDBNUMBER, ListType.FieldsFiles.PREMIERED,
                //ListType.FieldsFiles.PRODUCTIONCODE,
                ListType.FieldsFiles.RUNTIME,
                //ListType.FieldsFiles.SET,
                //ListType.FieldsFiles.SHOWLINK, ListType.FieldsFiles.STREAMDETAILS,
                //ListType.FieldsFiles.TOP250, ListType.FieldsFiles.VOTES,
                //ListType.FieldsFiles.FIRSTAIRED,
                ListType.FieldsFiles.SEASON, ListType.FieldsFiles.EPISODE,
                ListType.FieldsFiles.SHOWTITLE, ListType.FieldsFiles.THUMBNAIL, ListType.FieldsFiles.FILE,
                //ListType.FieldsFiles.RESUME, ListType.FieldsFiles.ARTISTID, ListType.FieldsFiles.ALBUMID,
                //ListType.FieldsFiles.TVSHOWID, ListType.FieldsFiles.SETID, ListType.FieldsFiles.WATCHEDEPISODES,
                //ListType.FieldsFiles.DISC, ListType.FieldsFiles.TAG, ListType.FieldsFiles.ART,
                //ListType.FieldsFiles.GENREID,
                ListType.FieldsFiles.DISPLAYARTIST,
                //ListType.FieldsFiles.ALBUMARTISTID, ListType.FieldsFiles.DESCRIPTION, ListType.FieldsFiles.THEME,
                //ListType.FieldsFiles.MOOD, ListType.FieldsFiles.STYLE, ListType.FieldsFiles.ALBUMLABEL,
                //ListType.FieldsFiles.SORTTITLE, ListType.FieldsFiles.EPISODEGUIDE,
                //ListType.FieldsFiles.UNIQUEID, ListType.FieldsFiles.DATEADDED,
                ListType.FieldsFiles.SIZE, ListType.FieldsFiles.LASTMODIFIED, ListType.FieldsFiles.MIMETYPE
        };
        Files.GetDirectory action = new Files.GetDirectory(dir.file,
                                                           mediaType,
                                                           sortMethod,
                                                           properties);
        action.execute(hostManager.getConnection(), new ApiCallback<List<ListType.ItemFile>>() {
            @Override
            public void onSuccess(List<ListType.ItemFile> result) {
                if (!isAdded()) return;
                currentLocation = dir;
                hideRefreshAnimation();

                ArrayList<FileLocation> flList = new ArrayList<>();
                if (!dir.isRoot) {
                    // Insert the parent directory as the first item in the list
                    // If it doesn't exist, create a fake one just to show, and set it as root, will browse sources instead
                    FileLocation fl = dir.parentDirectory;
                    if (fl == null) {
                        fl = new FileLocation("..", null, true, null, true);
                    } else {
                        fl.title = "..";
                    }
                    flList.add(0, fl);
                }
                for (ListType.ItemFile item : result) {
                    flList.add(FileLocation.newInstanceFromItemFile(getActivity(), item, currentLocation));
                }
                ((MediaFileListAdapter) getAdapter()).setFilelistItems(flList);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                hideRefreshAnimation();
                showStatusMessage(null, String.format(getString(R.string.error_getting_source_info), description));
            }
        }, callbackHandler);

    }

    /**
     * Starts playing the given media file
     * @param filename Filename to start playing
     */
    private void playMediaFile(final String filename) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = filename;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                HostConnection connection = hostManager.getConnection();
                startPlaylistIfNoActivePlayers(connection, getPlaylistId(mediaType), callbackHandler);
                callbackHandler.post(queueMediaQueueFileLocations);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isResumed()) return;
                UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_play_media_file), description));
            }
        }, callbackHandler);
    }

    private final Runnable queueMediaQueueFileLocations = new Runnable() {
        @Override
        public void run() {
            if (!mediaQueueFileLocation.isEmpty()) {
                final HostConnection connection = hostManager.getConnection();
                PlaylistType.Item item = new PlaylistType.Item();
                FileLocation fl = mediaQueueFileLocation.poll();
                if (fl == null) return;
                item.file = fl.file;
                Playlist.Add action = new Playlist.Add(getPlaylistId(mediaType), item);
                action.execute(connection, new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result ) {
                        callbackHandler.post(queueMediaQueueFileLocations);
                    }

                    @Override
                    public void onError(int errorCode, String description) {
                        if (!isResumed()) return;
                        UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_queue_media_file), description));
                        callbackHandler.post(queueMediaQueueFileLocations);
                    }
                }, callbackHandler);
            }
        }
    };

    /**
     * Starts playing the given media file on the local device
     * @param filename Filename to start playing
     */
    private void playMediaFileLocally(final String filename) {
        FileDownloadHelper.MovieInfo movieDownloadInfo = new FileDownloadHelper.MovieInfo(null, filename);
        Uri uri = Uri.parse(movieDownloadInfo.getMediaUrl(hostManager.getHostInfo()));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (mediaType.equalsIgnoreCase(Files.Media.VIDEO)) {
            intent.setDataAndType(uri, "video/*");
        } else if (mediaType.equalsIgnoreCase(Files.Media.MUSIC)) {
            intent.setDataAndType(uri, "audio/*");
        } else if (mediaType.equalsIgnoreCase(Files.Media.PICTURES)) {
            intent.setDataAndType(uri, "image/*");
        } else {
            intent.setDataAndType(uri, "application/*");
        }
        startActivity(intent);
    }

    /**
     * Queues the given media file on the active playlist, and starts it if nothing is playing
     * @param filename File to queue
     */
    private void queueMediaFile(final String filename) {
        final HostConnection connection = hostManager.getConnection();
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = filename;
        Playlist.Add action = new Playlist.Add(getPlaylistId(mediaType), item);
        action.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                startPlaylistIfNoActivePlayers(connection, getPlaylistId(mediaType), callbackHandler);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isResumed()) return;
                UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_queue_media_file), description));
            }
        }, callbackHandler);
    }

    /**
     * Starts a playlist if no active players are playing
     * @param connection Host connection
     * @param playlistId PlaylistId to start
     * @param callbackHandler Handler on which to post method callbacks
     */
    private void startPlaylistIfNoActivePlayers(final HostConnection connection,
                                                final int playlistId,
                                                final Handler callbackHandler) {
        Player.GetActivePlayers action = new Player.GetActivePlayers();
        action.execute(connection, new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result ) {
                // find out if any player is running. If it is not, start one
                if (result.isEmpty()) {
                    Player.Open action = new Player.Open(Player.Open.TYPE_PLAYLIST, playlistId);
                    action.execute(connection, new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) { }

                        @Override
                        public void onError(int errorCode, String description) {
                            if (!isResumed()) return;
                            UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_play_media_file), description));
                        }
                    }, callbackHandler);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isResumed()) return;
                UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_get_active_player), description));
            }
        }, callbackHandler);

    }

    /**
     * return the filename of a given path, if path is a directory, return directory name
     * @param path of the current file
     * @return filename or directory name
     */
    public static String getFilenameFromPath(final String path) {
        String p = path;
        String pathSymbol = "/";        // unix style
        if (path.contains("\\")) {
            pathSymbol = "\\";          // windows style
        }
        // if path ends with /, remove it
        if (path.endsWith(pathSymbol)) {
            p = path.substring(0, path.length() - 1);
        }
        if (p.lastIndexOf(pathSymbol) != -1) {
            p = p.substring(p.lastIndexOf(pathSymbol)+1);
        }
        return p;
    }

    private class MediaFileListAdapter extends RecyclerView.Adapter<ViewHolder> {
        Context ctx;
        int resource;
        List<FileLocation> fileLocationItems;

        int artWidth;
        int artHeight;

        private final View.OnClickListener itemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                if (fileLocationItems != null) {
                    final FileLocation loc = fileLocationItems.get(position);
                    if (!loc.isDirectory) {
                        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.getMenuInflater().inflate(R.menu.file_list_item, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(item -> {
                            int itemId = item.getItemId();
                            if (itemId == R.id.action_queue_item) {
                                queueMediaFile(loc.file);
                                return true;
                            } else if (itemId == R.id.action_play_item) {
                                playMediaFile(loc.file);
                                return true;
                            } else if (itemId == R.id.action_play_local_item) {
                                playMediaFileLocally(loc.file);
                                return true;
                            } else if (itemId == R.id.action_play_from_this_item) {
                                mediaQueueFileLocation.clear();
                                FileLocation fl;
                                // start playing the selected one, then queue the rest
                                for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                    fl = fileLocationItems.get(i);
                                    if (!fl.isDirectory) {
                                        mediaQueueFileLocation.add(fl);
                                    }
                                }
                                playMediaFile(loc.file);
                                return true;
                            }
                            return false;
                        });
                        popupMenu.show();
                    }
                }
            }
        };

        MediaFileListAdapter(Context context, int resource) {
            super();
            this.ctx = context;
            this.resource = resource;
            this.fileLocationItems = null;

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.filelist_art_width) /
                             UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.filelist_art_heigth) /
                              UIUtils.IMAGE_RESIZE_FACTOR);

        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param items list of files/directories
         */
        public void setFilelistItems(List<FileLocation> items) {
            this.fileLocationItems = items;
            notifyDataSetChanged();
        }

        public List<FileLocation> getFileItemList() {
            if (fileLocationItems == null)
                return new ArrayList<>();
            return new ArrayList<>(fileLocationItems);
        }

        public FileLocation getItem(int position) {
            if (fileLocationItems == null) {
                return null;
            } else {
                return fileLocationItems.get(position);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(ctx)
                                      .inflate(resource, parent, false);
            return new ViewHolder(view, getContext(), hostManager, artWidth, artHeight, itemMenuClickListener);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FileLocation fileLocation = this.getItem(position);
            if (fileLocation != null)
                holder.bindView(fileLocation, position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if (fileLocationItems == null) {
                return 0;
            } else {
                return fileLocationItems.size();
            }
        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView art;
        TextView title;
        TextView details;
        TextView sizeDuration;
        ImageView contextMenu;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight,
                   View.OnClickListener itemMenuClickListener) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            art = itemView.findViewById(R.id.art);
            title = itemView.findViewById(R.id.title);
            details = itemView.findViewById(R.id.details);
            contextMenu = itemView.findViewById(R.id.list_context_menu);
            sizeDuration = itemView.findViewById(R.id.other_info);
            contextMenu.setOnClickListener(itemMenuClickListener);
        }

        public void bindView(FileLocation fileLocation, int position) {
            title.setText(UIUtils.applyMarkup(context, fileLocation.title));
            if (TextUtils.isEmpty(fileLocation.details) && TextUtils.isEmpty(fileLocation.sizeDuration)) {
                title.setSingleLine(false);
                title.setMaxLines(3);
            }
            setViewText(details, fileLocation.details);
            setViewText(sizeDuration, fileLocation.sizeDuration);

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 fileLocation.artUrl, fileLocation.title,
                                                 art, artWidth, artHeight);
            // For the popup menu
            if (fileLocation.isDirectory) {
                contextMenu.setVisibility(View.GONE);
            } else {
                contextMenu.setVisibility(View.VISIBLE);
                contextMenu.setTag(position);

            }
        }

        private void setViewText(TextView v, String text) {
            v.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
            if (v.getVisibility() == View.VISIBLE)
                v.setText(UIUtils.applyMarkup(context, text));
        }
    }

    public static class FileLocation implements Parcelable {
        // Text to show on the list
        public String title;
        public final String details;
        public final String sizeDuration;
        public final String artUrl;

        public final String file;
        public final boolean isDirectory;
        // This location parent, can be null
        public final FileLocation parentDirectory;
        // Whether this location should be considered root, in which case its not possible to go up
        public final boolean isRoot;

        public FileLocation(String title, String path, boolean isDir, FileLocation parentDirectory, boolean isRoot) {
            this(title, path, isDir, parentDirectory, isRoot, null, null, null);
        }

        public FileLocation(String title, String path, boolean isDir, FileLocation parentDirectory, boolean isRoot,
                            String details, String sizeDuration, String artUrl) {
            this.title = title;
            this.file = path;
            this.isDirectory = isDir;
            this.parentDirectory = parentDirectory;
            this.isRoot = isRoot;

            this.details = details;
            this.sizeDuration = sizeDuration;
            this.artUrl = artUrl;
        }


        public static FileLocation newInstanceFromItemFile(Context context, ListType.ItemFile itemFile, FileLocation parentDirectory) {
            String DELIMITER = "  |  ";
            String title, details;
            int duration;

            switch (itemFile.type) {
                case ListType.ItemBase.TYPE_MOVIE:
                    title = itemFile.title;
                    details = itemFile.tagline;
                    duration = itemFile.runtime;
                    break;
                case ListType.ItemBase.TYPE_EPISODE:
                    title = itemFile.title;
                    details = String.format(context.getString(R.string.season_episode), itemFile.season, itemFile.episode);
                    duration = itemFile.runtime;
                    break;
                case ListType.ItemBase.TYPE_MUSIC_VIDEO:
                    title = itemFile.title;
                    details = Utils.listStringConcat(itemFile.artist, ", ") + DELIMITER + itemFile.album;
                    duration = itemFile.runtime;
                    break;
                case ListType.ItemBase.TYPE_ALBUM:
                    title = itemFile.displayartist + DELIMITER + itemFile.album;
                    details = getFilenameFromPath(itemFile.file);
                    duration = itemFile.duration;
                    break;
                case ListType.ItemBase.TYPE_SONG:
                    title = itemFile.label;
                    details = getFilenameFromPath(itemFile.file);
                    duration = itemFile.duration;
                    break;
                case ListType.ItemBase.TYPE_PICTURE:
                default:
                    title = itemFile.label;
                    details = null;
                    duration = 0;
                    break;
            }

            String artUrl = itemFile.thumbnail;
            String sizeDuration = (itemFile.size > 0) && (duration > 0) ?
                                  UIUtils.formatTime(duration) + DELIMITER + UIUtils.formatFileSize(itemFile.size) :
                                  (itemFile.size > 0) ? UIUtils.formatFileSize(itemFile.size) :
                                  (duration > 0)? UIUtils.formatTime(duration) : null;
            return new FileLocation(title, itemFile.file,
                                    itemFile.filetype.equalsIgnoreCase(ListType.ItemFile.FILETYPE_DIRECTORY),
                                    parentDirectory, false,
                                    details, sizeDuration, artUrl);
        }

        private FileLocation(Parcel in) {
            this.title = in.readString();
            this.file = in.readString();
            this.isDirectory = (in.readInt() != 0);

            this.details = in.readString();
            this.sizeDuration = in.readString();
            this.artUrl = in.readString();

            this.isRoot = (in.readInt() != 0);
            this.parentDirectory = in.readParcelable(getClass().getClassLoader());
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(title);
            out.writeString(file);
            out.writeInt(isDirectory ? 1 : 0);

            out.writeString(details);
            out.writeString(sizeDuration);
            out.writeString(artUrl);

            out.writeInt(isRoot ? 1 : 0);
            out.writeParcelable(parentDirectory, flags);
        }

        public static final Parcelable.Creator<FileLocation> CREATOR = new Parcelable.Creator<FileLocation>() {
            public FileLocation createFromParcel(Parcel in) {
                return new FileLocation(in);
            }

            public FileLocation[] newArray(int size) {
                return new FileLocation[size];
            }
        };
    }
}

