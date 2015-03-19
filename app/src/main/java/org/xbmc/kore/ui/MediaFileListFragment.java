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
package org.xbmc.kore.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.ItemType;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by danhdroid on 3/14/15.
 */
public class MediaFileListFragment extends Fragment {

    public static final String MEDIA_TYPE = "mediaType";
    private static final int MUSIC_PLAYLISTID = 0;
    private static final int VIDEO_PLAYLISTID = 1;

    private HostManager hostManager;
    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();
    String mediaType = Files.Media.MUSIC;
    String parentDirectory = null;
    int playlistId = MUSIC_PLAYLISTID;             // this is the ID of the music player
    private MediaFileListAdapter adapter = null;
    boolean browseRootAlready = false;
    ArrayList<FileLocation> rootFileLocation = new ArrayList<FileLocation>();
    Queue<FileLocation> mediaQueueFileLocation = new LinkedList<>();

    @InjectView(R.id.info_panel) RelativeLayout infoPanel;
    @InjectView(R.id.playlist) GridView folderGridView;
    @InjectView(R.id.info_title) TextView infoTitle;

    public static MediaFileListFragment newInstance(final String media) {
        MediaFileListFragment fragment = new MediaFileListFragment();
        Bundle args = new Bundle();
        args.putString(MEDIA_TYPE, media);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle args = getArguments();
        if (args != null) {
            mediaType = getArguments().getString(MEDIA_TYPE, Files.Media.MUSIC);
            if (mediaType.equalsIgnoreCase(Files.Media.VIDEO)) {
                playlistId = VIDEO_PLAYLISTID;
            }
        }
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, root);

        hostManager = HostManager.getInstance(getActivity());
        folderGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileLocation f = adapter.getItem(position);
                // if selection is a directory, browse the the level below
                if (f.isDirectory) {
                    // a directory - store the path of this directory so that we can reverse travel if
                    // we want to
                    if (f.isRootDir()) {
                        if (browseRootAlready)
                            browseFolderForFiles(f);
                        else {
                            browsingSourceForFolders();
                        }
                    }
                    else {
                        browseFolderForFiles(f);
                    }
                }
            }
        });

        adapter = new MediaFileListAdapter(getActivity(), R.layout.grid_item_playlist);
        folderGridView.setAdapter(adapter);
        return root;
    }


    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (adapter.getCount() == 0)
            browsingSourceForFolders();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void browsingSourceForFolders() {
        Files.GetSources action = new Files.GetSources(mediaType);
        action.execute(hostManager.getConnection(), new ApiCallback<List<ItemType.Source>>() {
            @Override
            public void onSuccess(List<ItemType.Source> result) {
                if (!isAdded())
                    return;
                // save this to compare when the user select a node
                rootFileLocation.clear();
                FileLocation fl;
                for (ItemType.Source item : result) {
                    fl = new FileLocation(item.label, item.file, true);
                    fl.setRootDir(true);
                    rootFileLocation.add(fl);
                }
                browseRootAlready = true;
                adapter.setFilelistItems(rootFileLocation);
                if (rootFileLocation.size() == 0) {
                    displayEmptyListMessage();
                }
                else
                    switchToPanel(R.id.playlist);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_source_info), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    private void browseFolderForFiles(final FileLocation item) {
        if (item.isRootDir()) {
            // this is a root directory
            parentDirectory = item.file;
        }
        else {
            // check to make sure that this is not our root path
            String rootPath = null;
            String path;
            for (FileLocation fl : rootFileLocation) {
                path = fl.file;
                if (item.file.contentEquals(path))    {
                    rootPath = fl.file;
                    break;
                }
            }
            if (rootPath != null) {
                parentDirectory = rootPath;
                item.setRootDir(true);
            }
            else
                parentDirectory = getParentDirectory(item.file);
        }
        Files.GetDirectory action = new Files.GetDirectory(item.file, new ListType.Sort(ListType.Sort.SORT_METHOD_LABEL, true, true));
        action.execute(hostManager.getConnection(), new ApiCallback<List<ListType.ItemFile>>() {
            @Override
            public void onSuccess(List<ListType.ItemFile> result) {
                if (!isAdded())
                    return;
                // insert the parent directory as the first item in the list
                FileLocation fl = new FileLocation("..", parentDirectory, true);
                fl.setRootDir(item.isRootDir());
                ArrayList<FileLocation> flList = new ArrayList<FileLocation>();
                flList.add(0, fl);
                for (ListType.ItemFile i : result) {
                    flList.add(new FileLocation(i.label, i.file, i.filetype.equalsIgnoreCase(ListType.ItemFile.DIRECTORY)));
                }
                adapter.setFilelistItems(flList);
                browseRootAlready = false;
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_source_info), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void playMediaFile(final String filename) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = filename;
        Player.Open action = new Player.Open(item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                if (!isAdded())
                    return;
                while (mediaQueueFileLocation.size() > 0) {
                    queueMediaFile(mediaQueueFileLocation.poll());
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_play_media_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void queueMediaFile(final FileLocation loc) {
        PlaylistType.Item item = new PlaylistType.Item();
        item.file = loc.file;
        Playlist.Add action = new Playlist.Add(playlistId, item);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                if (!isAdded())
                    return;
                startPlayingIfNoActivePlayers();
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_queue_media_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void startPlayingIfNoActivePlayers() {
        Player.GetActivePlayers action = new Player.GetActivePlayers();
        action.execute(hostManager.getConnection(), new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result ) {
                if (!isAdded())
                    return;
                // find out if any player is running. If it is not, start one
                if (result.size() == 0) {
                    startPlaying(playlistId);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_get_active_player), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void startPlaying(int playlistID) {
        Player.Open action = new Player.Open(playlistID);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                if (!isAdded())
                    return;
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_play_media_file), description),
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
        p = p.substring(0, p.lastIndexOf(pathSymbol));
        p = p + pathSymbol;            // add it back to make it look like path
        return p;
    }

    /**
     * Switches the info panel shown (they are exclusive)
     * @param panelResId The panel to show
     */
    private void switchToPanel(int panelResId) {
        switch (panelResId) {
            case R.id.info_panel:
                infoPanel.setVisibility(View.VISIBLE);
                folderGridView.setVisibility(View.GONE);
                break;
            case R.id.playlist:
                infoPanel.setVisibility(View.GONE);
                folderGridView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Displays empty list
     */
    private void displayEmptyListMessage() {
        switchToPanel(R.id.info_panel);
        infoTitle.setText(R.string.source_empty);
    }

    private class MediaFileListAdapter extends BaseAdapter implements ListAdapter {

        Context ctx;
        int resource;
        List<FileLocation> fileLocationItems = null;

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
                                        queueMediaFile(loc);
                                        return true;
                                    case R.id.action_play_item:
                                        playMediaFile(loc.file);
                                        return true;
                                    case R.id.action_play_from_this_item:
                                    {
                                        mediaQueueFileLocation.clear();
                                        FileLocation fl;
                                        for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                            fl = fileLocationItems.get(i);
                                            if (!fl.isDirectory) {
                                                mediaQueueFileLocation.add(fl);
                                            }
                                        }
                                        // start playing the selected one, then queue the rest make sure to queue
                                        // the selected on last so the it does not lose its place in the queue
                                        mediaQueueFileLocation.add(loc);
                                        playMediaFile(loc.file);
                                    }
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

        public MediaFileListAdapter(Context context, int resource) {
            super();
            this.ctx = context;
            this.resource = resource;
            this.fileLocationItems = null;
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
        @Override
        public int getCount() {
            if (fileLocationItems == null) {
                return 0;
            }
            else {
                return fileLocationItems.size();
            }
        }

        @Override
        public FileLocation getItem(int position) {
            if (fileLocationItems == null) {
                return null;
            }
            else {
                return fileLocationItems.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount () {
            return 1;
        }

        /** {@inheritDoc} */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(ctx)
                        .inflate(resource, parent, false);

                // Setup View holder pattern
                viewHolder = new ViewHolder();
                viewHolder.art = (ImageView) convertView.findViewById(R.id.art);
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.detail = (TextView) convertView.findViewById(R.id.details);
                viewHolder.contextMenu = (ImageView) convertView.findViewById(R.id.list_context_menu);
                convertView.setTag(viewHolder);
            }

            viewHolder = (ViewHolder) convertView.getTag();
            FileLocation fileLocation = this.getItem(position);
            viewHolder.label = fileLocation.label;
            viewHolder.path = fileLocation.file;
            viewHolder.isDirectory = fileLocation.isDirectory;
            if (fileLocation.isDirectory) {
                viewHolder.title.setText(fileLocation.label);
                viewHolder.detail.setText("");
            }
            else {
                viewHolder.title.setText("");
                viewHolder.detail.setText(fileLocation.label);
            }
            viewHolder.position = position;

            int artWidth = getResources().getDimensionPixelSize(R.dimen.playlist_art_width);
            int artHeight = getResources().getDimensionPixelSize(R.dimen.playlist_art_heigth);
            ViewGroup.LayoutParams layoutParams = viewHolder.art.getLayoutParams();
            layoutParams.width = layoutParams.height;
            viewHolder.art.setLayoutParams(layoutParams);
            artWidth = artHeight;

            UIUtils.loadImageWithCharacterAvatar(getActivity(), hostManager,
                    null, fileLocation.label,
                    viewHolder.art, artWidth, artHeight);
            // For the popup menu
            if (fileLocation.isDirectory) {
                viewHolder.contextMenu.setVisibility(View.GONE);
            }
            else {
                viewHolder.contextMenu.setVisibility(View.VISIBLE);
                viewHolder.contextMenu.setTag(position);
                viewHolder.contextMenu.setOnClickListener(itemMenuClickListener);
            }

            return convertView;
        }
    }

    /**
     * View holder pattern
     */
    private class ViewHolder {
        ImageView art;
        TextView title;
        TextView detail;
        ImageView contextMenu;
        String label;
        String path;
        boolean isDirectory;
        int position;
    }

    public class FileLocation {
        public final String label;
        public final String file;
        public final boolean isDirectory;
        private boolean isRoot;


        public boolean isRootDir() { return this.isRoot; }
        public void setRootDir(boolean root) { this.isRoot = root; }

        public FileLocation(String label, String path, boolean isDir) {
            this.label = label;
            this.file = path;
            this.isDirectory = isDir;
            this.isRoot = false;
        }
    }
}

