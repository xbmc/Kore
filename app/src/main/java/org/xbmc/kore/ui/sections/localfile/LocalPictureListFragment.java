/*
 * Copyright 2015 DanhDroid. All rights reserved.
 * Copyright 2019 Upabjojr. All rights reserved.
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
package org.xbmc.kore.ui.sections.localfile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.AbstractFileListFragment;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.CharacterDrawable;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;


/**
 * Presents a list of files of different types (Video/Music)
 */
public class LocalPictureListFragment extends AbstractFileListFragment {
    private static final String TAG = LogUtils.makeLogTag(LocalPictureListFragment.class);

    public static final String MEDIA_TYPE = "mediaType";
    public static final String SORT_METHOD = "sortMethod";
    public static final String PATH_CONTENTS = "pathContents";
    public static final String ROOT_PATH_CONTENTS = "rootPathContents";
    public static final String ROOT_VISITED = "rootVisited";
    public static final String ROOT_PATH = "rootPath";
    public static final String ROOT_PATH_LOCATION = "rootPath";
    public static final String DELAY_LOAD = "delayLoad";
    private static final String ADDON_SOURCE = "addons:";

    private String rootPathLocation = null;

    private HostManager hostManager;
    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();

    String mediaType = Files.Media.FILES;
    ListType.Sort sortMethod = null;
    String parentDirectory = null;
    int playlistId = PlaylistType.MUSIC_PLAYLISTID;             // this is the ID of the music player
//    private MediaPictureListAdapter adapter = null;
    boolean browseRootAlready = false;
    LocalPictureListFragment.FileLocation loadOnVisible = null;

    ArrayList<FileLocation> rootFileLocation = new ArrayList<>();
    Queue<FileLocation> mediaQueueFileLocation = new LinkedList<>();

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MEDIA_TYPE, mediaType);
        outState.putParcelable(SORT_METHOD, sortMethod);
        try {
            outState.putParcelableArrayList(PATH_CONTENTS, (ArrayList<FileLocation>) ((MediaPictureListAdapter) getAdapter()).getFileItemList());
        } catch (NullPointerException npe) {
            // adapter is null probably nothing was save in bundle because the directory is empty
            // ignore this so that the empty message would display later on
        }
        outState.putParcelableArrayList(ROOT_PATH_CONTENTS, rootFileLocation);
        outState.putBoolean(ROOT_VISITED, browseRootAlready);
    }

    @Override
    protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener() {
        return new RecyclerViewEmptyViewSupport.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                handleFileSelect(((MediaPictureListAdapter) getAdapter()).getItem(position));
            }
        };
    }

    @Override
    protected RecyclerView.Adapter createAdapter() {
        return new MediaPictureListAdapter(getActivity(), R.layout.grid_item_picture);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();
        FileLocation rootPath = null;

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to read files was not granted,
            // prompt the user for storage access permissions:
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Utils.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
        }

        try {
            if (http_app == null) {
                http_app = HttpApp.getInstance(getContext(), 8080);
            }
        } catch (IOException ioe) {
            Toast.makeText(getContext(),
                    getString(R.string.error_starting_http_server),
                    Toast.LENGTH_LONG);
        }

        if (args != null) {
            rootPath = args.getParcelable(ROOT_PATH);
            this.rootPathLocation = args.getString(ROOT_PATH_LOCATION);
            mediaType = args.getString(MEDIA_TYPE, Files.Media.FILES);
            if (mediaType.equalsIgnoreCase(Files.Media.VIDEO)) {
                playlistId = PlaylistType.VIDEO_PLAYLISTID;
            } else if (mediaType.equalsIgnoreCase(Files.Media.PICTURES)) {
                playlistId = PlaylistType.PICTURE_PLAYLISTID;
            }
            sortMethod = args.getParcelable(SORT_METHOD);
        }

        hostManager = HostManager.getInstance(getActivity());

        getEmptyView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!atRootDirectory())
                    browseSources();
            }
        });

        if (savedInstanceState != null) {
            mediaType = savedInstanceState.getString(MEDIA_TYPE, Files.Media.FILES);
            //currentPath = savedInstanceState.getString(CURRENT_PATH);
            if (mediaType.equalsIgnoreCase(Files.Media.VIDEO)) {
                playlistId = PlaylistType.VIDEO_PLAYLISTID;
            } else if (mediaType.equalsIgnoreCase(Files.Media.PICTURES)) {
                playlistId = PlaylistType.PICTURE_PLAYLISTID;
            }
            sortMethod = savedInstanceState.getParcelable(SORT_METHOD);
            ArrayList<FileLocation> list = savedInstanceState.getParcelableArrayList(PATH_CONTENTS);
            rootFileLocation = savedInstanceState.getParcelableArrayList(ROOT_PATH_CONTENTS);
            browseRootAlready = savedInstanceState.getBoolean(ROOT_VISITED);
            ((MediaPictureListAdapter) getAdapter()).setFilelistItems(list);
        }
        else if (rootPath != null) {
            loadOnVisible = rootPath;
            // setUserVisibleHint may have already fired
            setUserVisibleHint(getUserVisibleHint() || !args.getBoolean(DELAY_LOAD, false));
        }
        else {
            browseSources();
        }
        return root;
    }

    @Override
    public void setUserVisibleHint (boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && loadOnVisible != null) {
            FileLocation rootPath = loadOnVisible;
            loadOnVisible = null;
            browseRootAlready = true;
            browseDirectory(rootPath);
        }
    }

    void handleFileSelect(FileLocation f) {
        // if selection is a directory, browse the the level below
        if (f.isDirectory) {
            // a directory - store the path of this directory so that we can reverse travel if
            // we want to
            if (f.isRootDir()) {
                if (browseRootAlready)
                    browseDirectory(f);
                else {
                    browseSources();
                }
            } else {
                browseDirectory(f);
            }
        } else {
            playMediaFile(f.title, f.file);
        }
    }

    public void onBackPressed() {
        // Emulate a click on ..
        handleFileSelect(((MediaPictureListAdapter) getAdapter()).getItem(0));
    }

    public boolean atRootDirectory() {
        if (getAdapter().getItemCount() == 0)
            return true;
        FileLocation fl = ((MediaPictureListAdapter) getAdapter()).getItem(0);
        if (fl == null)
            return true;
        else
            // if we still see "..", it is not the real root directory
            return fl.isRootDir() &&
                   (fl.title != null) && !fl.title.contentEquals("..");
    }

    /**
     * Gets and presents the list of media sources
     */
    @Override
    protected void browseSources() {
        File directory = null;
        if (rootFileLocation != null) {
            directory = new File(rootPathLocation);
        } else {
            return;
        }
        File[] files = directory.listFiles();

        if (files == null) {
            Toast.makeText(getActivity(),
                    getString(R.string.error_reading_local_storage),
                    Toast.LENGTH_LONG);
            return;
        }
        Arrays.sort(files);

        rootFileLocation.clear();
        browseRootAlready = true;
        for (File file : files) {
            boolean isDir = file.isDirectory();
            rootFileLocation.add(
                    new FileLocation(file.getName(), file.getAbsolutePath(), isDir)
            );
        }
        ((MediaPictureListAdapter) getAdapter()).setFilelistItems(rootFileLocation);
    }


    /**
     * Gets and presents the files of the specified directory
     * @param dir Directory to browse
     */
    private void browseDirectory(final FileLocation dir) {
        if (dir.isRootDir()) {
            // this is a root directory
            parentDirectory = dir.file;
        } else {
            // check to make sure that this is not our root path
            String rootPath = null;
            String path;
            for (FileLocation fl : rootFileLocation) {
                path = fl.file;
                if ((path != null) && (dir.file != null) &&
                    (dir.file.contentEquals(path))) {
                    rootPath = fl.file;
                    break;
                }
            }
            if (rootPath != null) {
                parentDirectory = rootPath;
                dir.setRootDir(true);
            } else if (dir.file != null) {
                parentDirectory = getParentDirectory(dir.file);
            }
        }

        // TODO: maybe async task here?
        File[] files = (new File(dir.file)).listFiles();
        Arrays.sort(files);

        ArrayList<FileLocation> dir_list = new ArrayList<>();
        ArrayList<FileLocation> file_list = new ArrayList<>();

        if (dir.hasParent) {
            // insert the parent directory as the first item in the list
            parentDirectory = getParentDirectory(dir.file);
            FileLocation fl = new FileLocation("..", parentDirectory, true);
            fl.setRootDir(dir.isRootDir());
            dir_list.add(0, fl);
        }

        for (File file : files) {
            FileLocation fl = new FileLocation(file.getName(), file.getAbsolutePath(), file.isDirectory());
            if (fl.isDirectory) {
                dir_list.add(fl);
            } else {
                file_list.add(fl);
            }
        }

        // TODO: use sortMethod here
        ArrayList<FileLocation> full_list = new ArrayList<>();
        full_list.addAll(dir_list);
        full_list.addAll(file_list);

        ((MediaPictureListAdapter) getAdapter()).setFilelistItems(full_list);
        browseRootAlready = false;
    }

    /**
     * Starts playing the given media file
     * @param filename Filename to start playing
     */
    private void playMediaFile(final String filename, final String path) {
        http_app.addLocalFilePath(filename, path);
        String url = http_app.getLinkToFile();

        PlaylistType.Item item = new PlaylistType.Item();
        item.file = url;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                while (!mediaQueueFileLocation.isEmpty()) {
                    queueMediaFile(mediaQueueFileLocation.poll().file);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_play_local_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    /**
     * Queues the given media file on the active playlist, and starts it if nothing is playing
     * @param filename File to queue
     */
    private void queueMediaFile(final String filename) {
        Toast.makeText(getContext(), "Media not really implemented for local device files.", Toast.LENGTH_LONG);
        final HostConnection connection = hostManager.getConnection();
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = filename;
        Playlist.Add action = new Playlist.Add(playlistId, item);
        action.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                startPlaylistIfNoActivePlayers(connection, playlistId, callbackHandler);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_queue_media_file), description),
                        Toast.LENGTH_SHORT).show();
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
                            if (!isAdded()) return;
                            Toast.makeText(getActivity(),
                                    String.format(getString(R.string.error_play_media_file), description),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, callbackHandler);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_get_active_player), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    /**
     * return the path of the parent based on path
     * @param path of the current media file
     * @return path of the parent
     */
    public static String getParentDirectory(final String path) {
        String p = path;
        String pathSymbol = "/";        // unix style
        if (path.contains("\\")) {
            pathSymbol = "\\";          // windows style
        }

        // if path ends with /, remove it before removing the directory name
        if (path.endsWith(pathSymbol)) {
            p = path.substring(0, path.length() - 1);
        }

        if (p.lastIndexOf(pathSymbol) != -1) {
            p = p.substring(0, p.lastIndexOf(pathSymbol));
        }
        p = p + pathSymbol;            // add it back to make it look like path
        return p;
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

    private class MediaPictureListAdapter extends RecyclerView.Adapter {

        Context ctx;
        int resource;
        List<FileLocation> fileLocationItems;

        int artWidth;
        int artHeight;

        private View.OnClickListener itemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                if (fileLocationItems != null) {
                    final FileLocation loc = fileLocationItems.get(position);
                    if (!loc.isDirectory) {
                        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.getMenuInflater().inflate(R.menu.filelist_item, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                switch (item.getItemId()) {
                                    case R.id.action_queue_item:
                                        queueMediaFile(loc.file);
                                        return true;
                                    case R.id.action_play_item:
                                        playMediaFile(loc.title, loc.file);
                                        return true;
                                    case R.id.action_play_from_this_item:
                                        mediaQueueFileLocation.clear();
                                        FileLocation fl;
                                        // start playing the selected one, then queue the rest
                                        mediaQueueFileLocation.add(loc);
                                        for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                            fl = fileLocationItems.get(i);
                                            if (!fl.isDirectory) {
                                                mediaQueueFileLocation.add(fl);
                                            }
                                        }
                                        playMediaFile(loc.title, loc.file);
                                        return true;
                                }
                                return false;
                            }
                        });
                        popupMenu.show();
                    }
                }
            }
        };

        MediaPictureListAdapter(Context context, int resource) {
            super();
            this.ctx = context;
            this.resource = resource;
            this.fileLocationItems = null;

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.picturelist_art_width) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.picturelist_art_heigth) /
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

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(ctx)
                                        .inflate(resource, parent, false);
            return new ViewHolder(view, getContext(), hostManager, artWidth, artHeight, itemMenuClickListener);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            FileLocation fileLocation = this.getItem(position);
            ((ViewHolder) holder).bindView(fileLocation, position);
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

    static boolean checkFileIsPicture(File file_path) {
        if ((file_path.getAbsolutePath().toLowerCase().endsWith(".jpg")) ||
                (file_path.getAbsolutePath().toLowerCase().endsWith(".jpeg"))) {
            return true;
        }
        return false;
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
            sizeDuration = itemView.findViewById(R.id.size_duration);
            contextMenu.setOnClickListener(itemMenuClickListener);
        }

        public void bindView(FileLocation fileLocation, int position) {
            title.setText(fileLocation.title);
            details.setText(fileLocation.details);
            sizeDuration.setText(fileLocation.sizeDuration);

            CharacterDrawable avatarDrawable = UIUtils.getCharacterAvatar(context, fileLocation.title);
            File file_path = new File(fileLocation.file);

            Picasso.with(context)
                    .load(file_path)
                    .placeholder(avatarDrawable)
                    .resize(artWidth, artHeight)
                    .centerCrop()
                    .into(art);

            // For the popup menu
            if (fileLocation.isDirectory) {
                contextMenu.setVisibility(View.GONE);
            } else {
                contextMenu.setVisibility(View.VISIBLE);
                contextMenu.setTag(position);
            }
        }
    }

    HttpApp http_app = null;

    public static class FileLocation implements Parcelable {
        public final String title;
        public final String details;
        public final String sizeDuration;
        public final String artUrl;

        public final String file;
        public final boolean isDirectory;
        public final boolean hasParent;
        private boolean isRoot;


        public boolean isRootDir() { return this.isRoot; }
        public void setRootDir(boolean root) { this.isRoot = root; }

        public FileLocation(String title, String path, boolean isDir) {
            this(title, path, isDir, null, null, null);
        }

        static final Pattern noParent = Pattern.compile("plugin://[^/]*/?");
        public FileLocation(String title, String path, boolean isDir, String details, String sizeDuration, String artUrl) {
            this.title = title;
            this.file = path;
            this.isDirectory = isDir;
            this.hasParent = !noParent.matcher(path).matches();

            this.isRoot = false;

            this.details = details;
            this.sizeDuration = sizeDuration;
            this.artUrl = artUrl;
        }

        private FileLocation(Parcel in) {
            this.title = in.readString();
            this.file = in.readString();
            this.isDirectory = (in.readInt() != 0);
            this.hasParent = (in.readInt() != 0);
            this.isRoot = (in.readInt() != 0);

            this.details = in.readString();
            this.sizeDuration = in.readString();
            this.artUrl = in.readString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(title);
            out.writeString(file);
            out.writeInt(isDirectory ? 1 : 0);
            out.writeInt(hasParent ? 1 : 0);
            out.writeInt(isRoot ? 1 : 0);

            out.writeString(details);
            out.writeString(sizeDuration);
            out.writeString(artUrl);
        }

        public static final Creator<FileLocation> CREATOR = new Creator<FileLocation>() {
            public FileLocation createFromParcel(Parcel in) {
                return new FileLocation(in);
            }

            public FileLocation[] newArray(int size) {
                return new FileLocation[size];
            }
        };
    }
}
