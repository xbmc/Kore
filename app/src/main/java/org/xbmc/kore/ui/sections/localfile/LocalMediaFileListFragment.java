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
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.AbstractListFragment;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.CharacterDrawable;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Presents a list of files of different types (Video/Music)
 */
public class LocalMediaFileListFragment extends AbstractListFragment {
    private static final String TAG = LogUtils.makeLogTag(LocalMediaFileListFragment.class);

    public static final String MEDIA_TYPE = "mediaType";
    public static final String SORT_METHOD = "sortMethod";
    public static final String PATH_CONTENTS = "pathContents";
    public static final String ROOT_PATH_CONTENTS = "rootPathContents";
    public static final String ROOT_VISITED = "rootVisited";
    public static final String ROOT_PATH_LOCATION = "rootPath";

    private String rootPathLocation = null;

    private HostManager hostManager;
    /**
     * Handler on which to post RPC callbacks
     */
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    private LocalFileLocation currentDirectory = null;

    String mediaType = Files.Media.FILES;
    ListType.Sort sortMethod = null;
    String parentDirectory = null;
    boolean browseRootAlready = false;

    ArrayList<LocalFileLocation> rootFileLocation = new ArrayList<>();

    // Permission check callback
    private final ActivityResultLauncher<String> filesPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    browseSources();
                } else {
                    Toast.makeText(getActivity(), R.string.read_storage_permission_denied, Toast.LENGTH_SHORT)
                         .show();
                }
            });

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MEDIA_TYPE, mediaType);
        outState.putParcelable(SORT_METHOD, sortMethod);
        try {
            outState.putParcelableArrayList(PATH_CONTENTS, (ArrayList<LocalFileLocation>) ((MediaPictureListAdapter) getAdapter()).getFileItemList());
        } catch (NullPointerException npe) {
            // adapter is null probably nothing was save in bundle because the directory is empty
            // ignore this so that the empty message would display later on
        }
        outState.putParcelableArrayList(ROOT_PATH_CONTENTS, rootFileLocation);
        outState.putBoolean(ROOT_VISITED, browseRootAlready);
    }

    @Override
    protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener() {
        return (view, position) -> {
            LocalFileLocation selection = ((MediaPictureListAdapter) getAdapter()).getItem(position);
            if (selection != null) handleFileSelect(selection);
        };
    }

    @Override
    protected MediaPictureListAdapter createAdapter() {
        return new MediaPictureListAdapter(requireContext(), R.layout.item_file);
    }

    @Override
    public void onRefresh() {
        if (currentDirectory == null) {
            browseSources();
        } else {
            browseDirectory(currentDirectory);
        }
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Override parent Connection Status callbacks, so that they don't disable the SwipeRefreshLayout and the list.
     * This fragment doesn't need a Kodi connection to show results
     */
    @Override
    public void onConnectionStatusError(int errorCode, String description) {}

    @Override
    public void onConnectionStatusSuccess() {}

    @Override
    public void onConnectionStatusNoResultsYet() {}

    private void checkReadStoragePermission() {
        boolean hasStoragePermission =
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (!hasStoragePermission) {
            filesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();

        checkReadStoragePermission();

        try {
            if (http_app == null) {
                http_app = HttpApp.getInstance(getContext(), 8080);
            }
        } catch (IOException ioe) {
            Toast.makeText(getContext(),
                    getString(R.string.error_starting_http_server),
                    Toast.LENGTH_LONG).show();
        }

        if (args != null) {
            this.rootPathLocation = args.getString(ROOT_PATH_LOCATION);
            mediaType = args.getString(MEDIA_TYPE, Files.Media.FILES);
            sortMethod = args.getParcelable(SORT_METHOD);
        }

        hostManager = HostManager.getInstance(requireContext());

        getEmptyView().setOnClickListener(v -> {
            if (!atRootDirectory())
                browseSources();
        });

        if (savedInstanceState != null) {
            mediaType = savedInstanceState.getString(MEDIA_TYPE, Files.Media.FILES);
            //currentPath = savedInstanceState.getString(CURRENT_PATH);
            sortMethod = savedInstanceState.getParcelable(SORT_METHOD);
            ArrayList<LocalFileLocation> list = savedInstanceState.getParcelableArrayList(PATH_CONTENTS);
            rootFileLocation = savedInstanceState.getParcelableArrayList(ROOT_PATH_CONTENTS);
            browseRootAlready = savedInstanceState.getBoolean(ROOT_VISITED);
            ((MediaPictureListAdapter) getAdapter()).setFilelistItems(list);
        } else {
            browseSources();
        }
        return root;
    }

    void handleFileSelect(LocalFileLocation f) {
        // if selection is a directory, browse the the level below
        if (f.isDirectory) {
            if (f.isRootDir() && !browseRootAlready) {
                browseSources();
            } else {
                browseDirectory(f);
            }
        } else {
            playMediaFile(f, null);
        }
    }

    public void onBackPressed() {
        // Emulate a click on ..
        LocalFileLocation selection = ((MediaPictureListAdapter) getAdapter()).getItem(0);
        if (selection != null) handleFileSelect(selection);
    }

    public boolean atRootDirectory() {
        return currentDirectory.fullPath.equals(rootPathLocation);
    }

    /**
     * Gets and presents the list of media sources
     */
    private void browseSources() {
        File directory;
        if (rootFileLocation != null) {
            directory = new File(rootPathLocation);
        } else {
            return;
        }
        File[] files = directory.listFiles();

        if (files == null) {
            Toast.makeText(getActivity(),
                    getString(R.string.error_reading_local_storage),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Arrays.sort(files);

        rootFileLocation.clear();
        browseRootAlready = true;
        for (File file : files) {
            boolean isDir = file.isDirectory();
            rootFileLocation.add(
                    new LocalFileLocation(file.getName(), file.getAbsolutePath(), isDir)
            );
        }
        ((MediaPictureListAdapter) getAdapter()).setFilelistItems(rootFileLocation);
    }


    /**
     * Gets and presents the files of the specified directory
     * @param dir Directory to browse
     */
    private void browseDirectory(final LocalFileLocation dir) {
        if (dir.isRootDir()) {
            // this is a root directory
            parentDirectory = dir.fullPath;
        } else {
            // check to make sure that this is not our root path
            String rootPath = null;
            String path;
            for (LocalFileLocation fl : rootFileLocation) {
                path = fl.fullPath;
                if ((path != null) && (dir.fullPath != null) &&
                    (dir.fullPath.contentEquals(path))) {
                    rootPath = fl.fullPath;
                    break;
                }
            }
            if (rootPath != null) {
                parentDirectory = rootPath;
                dir.setRootDir(true);
            } else if (dir.fullPath != null) {
                parentDirectory = getParentDirectory(dir.fullPath);
            }
        }

        currentDirectory = dir;

        File[] files = (dir.fullPath == null) ? null : (new File(dir.fullPath)).listFiles();
        if (files == null) {
            Toast.makeText(
                getContext(),
                String.format(getString(R.string.error_getting_source_info), "listFiles() failed"),
                Toast.LENGTH_LONG).show();
            return;
        }
        Arrays.sort(files);

        ArrayList<LocalFileLocation> dir_list = new ArrayList<>();
        ArrayList<LocalFileLocation> file_list = new ArrayList<>();

        if (dir.hasParent) {
            // insert the parent directory as the first item in the list
            parentDirectory = getParentDirectory(dir.fullPath);
            LocalFileLocation fl = new LocalFileLocation("..", parentDirectory, true);
            fl.setRootDir(dir.isRootDir());
            dir_list.add(0, fl);
        }

        for (File file : files) {
            LocalFileLocation fl = new LocalFileLocation(file.getName(), file.getAbsolutePath(), file.isDirectory());
            if (fl.isDirectory) {
                dir_list.add(fl);
            } else {
                file_list.add(fl);
            }
        }

        // TODO: use sortMethod here
        ArrayList<LocalFileLocation> full_list = new ArrayList<>();
        full_list.addAll(dir_list);
        full_list.addAll(file_list);

        ((MediaPictureListAdapter) getAdapter()).setFilelistItems(full_list);
        browseRootAlready = false;
    }

    /**
     * Starts playing the given media file
     * @param localFileLocation LocalFileLocation to start playing
     */
    private void playMediaFile(final LocalFileLocation localFileLocation, ArrayList<LocalFileLocation> queuedFiles) {
        http_app.addLocalFilePath(localFileLocation);
        String url = http_app.getLinkToFile();

        PlaylistType.Item item = new PlaylistType.Item();
        item.file = url;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (queuedFiles == null) return;
                // Queue the rest
                for (LocalFileLocation fl : queuedFiles) {
                    queueMediaFile(fl);
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
     * @param localFileLocation LocalFileLocation to queue
     */
    private void queueMediaFile(final LocalFileLocation localFileLocation) {
        http_app.addLocalFilePath(localFileLocation);
        String url = http_app.getLinkToFile();

        final HostConnection connection = hostManager.getConnection();
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = url;
        Playlist.Add action = new Playlist.Add(localFileLocation.getPlaylistTypeId(), item);
        action.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                startPlaylistIfNoActivePlayers(connection, localFileLocation.getPlaylistTypeId(), callbackHandler);
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

    private class MediaPictureListAdapter extends RecyclerView.Adapter<ViewHolder> {

        Context ctx;
        int resource;
        List<LocalFileLocation> fileLocationItems;

        int artWidth;
        int artHeight;

        private final View.OnClickListener itemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                if (fileLocationItems != null) {
                    final LocalFileLocation loc = fileLocationItems.get(position);
                    if (!loc.isDirectory) {
                        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.getMenuInflater().inflate(R.menu.local_file_list_item, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(item -> {
                            int itemId = item.getItemId();
                            if (itemId == R.id.action_queue_item) {
                                queueMediaFile(loc);
                                return true;
                            } else if (itemId == R.id.action_play_item) {
                                playMediaFile(loc, null);
                                return true;
                            } else if (itemId == R.id.action_play_from_this_item) {
                                ArrayList<LocalFileLocation> queuedFiles = new ArrayList<>(fileLocationItems.size());
                                LocalFileLocation fl;
                                for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                    fl = fileLocationItems.get(i);
                                    if (!fl.isDirectory) queuedFiles.add(fl);
                                }
                                // start playing the selected one, then queue the rest
                                playMediaFile(loc, queuedFiles);
                                return true;
                            }
                            return false;
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
            artWidth = (int)(resources.getDimension(R.dimen.filelist_art_width) / UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.filelist_art_heigth) / UIUtils.IMAGE_RESIZE_FACTOR);
        }

        /**
         * Manually set the items on the adapter
         * Calls notifyDataSetChanged()
         *
         * @param items list of files/directories
         */
        public void setFilelistItems(List<LocalFileLocation> items) {
            this.fileLocationItems = items;
            notifyDataSetChanged();
        }

        public List<LocalFileLocation> getFileItemList() {
            if (fileLocationItems == null)
                return new ArrayList<>();
            return new ArrayList<>(fileLocationItems);
        }

        public LocalFileLocation getItem(int position) {
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
            LocalFileLocation fileLocation = this.getItem(position);
            if (fileLocation != null) holder.bindView(fileLocation, position);
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

        public void bindView(LocalFileLocation fileLocation, int position) {
            title.setText(fileLocation.fileName);
            setViewText(details, fileLocation.details);
            setViewText(sizeDuration, fileLocation.sizeDuration);

            CharacterDrawable avatarDrawable = UIUtils.getCharacterAvatar(context, fileLocation.fileName);
            File file_path = new File(fileLocation.fullPath);

            Picasso.get()
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

        private void setViewText(TextView v, String text) {
            v.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
            if (v.getVisibility() == View.VISIBLE)
                v.setText(UIUtils.applyMarkup(context, text));
        }
    }

    HttpApp http_app = null;
}
