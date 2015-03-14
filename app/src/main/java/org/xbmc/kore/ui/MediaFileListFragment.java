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
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.FilesType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.jsonrpc.type.SortType;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;
import org.xbmc.kore.utils.Utils;

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
    private static final String TAG = LogUtils.makeLogTag(MediaFileListFragment.class);

    public static final String MEDIA_TYPE = "mediaType";
    public static final String MUSIC_MEDIA_TYPE = "music";
    public static final String VIDEO_MEDIA_TYPE = "video";
    private static final int MUSIC_PLAYLISTID = 0;
    private static final int VIDEO_PLAYLISTID = 1;

    private HostManager hostManager;
    /**
     * Handler on which to post RPC callbacks
     */
    private Handler callbackHandler = new Handler();
    String mediaType = MUSIC_MEDIA_TYPE;
    String parentDirectory = null;
    int playlistId = MUSIC_PLAYLISTID;             // this is the ID of the music player
    boolean hasError = false;
    private MediaFileListAdapter adapter = null;
    boolean browseRootAlready = false;
    ArrayList<FilesType.FileLocation> rootFileLocation = new ArrayList<FilesType.FileLocation>();
    Queue<FilesType.FileLocation> mediaQueueFileLocation = new LinkedList<>();
    @InjectView(R.id.playlist)
    GridView folderGridView;

    /**
     * Create a new instance of this, initialized to show albums of genres
     */
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
            mediaType = getArguments().getString(MEDIA_TYPE, "");
            if (mediaType.equalsIgnoreCase(VIDEO_MEDIA_TYPE)) {
                playlistId = VIDEO_PLAYLISTID;
            }
        }
        LogUtils.LOGD(TAG, "MediaFileListFragment.onCreateView(): mediaType = " + mediaType);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, root);

        hostManager = HostManager.getInstance(getActivity());
        folderGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FilesType.FileLocation f = adapter.getItem(position);
                LogUtils.LOGD(TAG, "setOnItemClickListener() label = " + f.label + ", path = " + f.path + ", Directory = " + f.isDirectory);
                // if selection is a directory, browse the the level below
                if (f.isDirectory) {
                    // a directory - store the path of this directory so that we can reverse travel if
                    // we want to
                    if (f.isRootDir()) {
                        if (browseRootAlready)
                            callBrowseFolderForFile(f);
                        else {
                            callBrowsingFolderAndSetup();
                        }
                    }
                    else {
                        callBrowseFolderForFile(f);
                    }
                }
            }
        });

        // Configure the adapter and start the loader

        adapter = new MediaFileListAdapter(getActivity(), R.layout.grid_item_playlist);
        folderGridView.setAdapter(adapter);
        //registerForContextMenu(folderGridView);
        return root;
    }


    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (adapter.getCount() == 0)
            callBrowsingFolderAndSetup();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the addons list and setup the gridview
     */
    private void callBrowsingFolderAndSetup() {
        LogUtils.LOGD(TAG, "MediaFileListFragment.callBrowsingFolderAndSetup");

        Files.GetSources action = new Files.GetSources(mediaType);
        action.execute(hostManager.getConnection(), new ApiCallback<List<FilesType.FileLocation>>() {
            @Override
            public void onSuccess(List<FilesType.FileLocation> result) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.callBrowsingFolderAndSetup.onSuccess()");
                if (!isAdded())
                    return;
                // save this to compare when the user select a node
                rootFileLocation.clear();
                for (FilesType.FileLocation item : result) {
                    //LogUtils.LOGD(TAG, "FilesType.FileLocation label = " + item.label + ", path = " + item.path);
                    item.setRootDir(true);
                    rootFileLocation.add(item);
                }
                browseRootAlready = true;
                adapter.setFilelistItems(result);
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.callBrowsingFolderAndSetup.onError()");
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_source_info), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    private void callBrowseFolderForFile(final FilesType.FileLocation item) {
        LogUtils.LOGD(TAG, "MediaFileListFragment.callBrowseFolderForFile: path = " + item.path + " root = " + item.isRootDir());
        if (item.isRootDir()) {
            // this is a root directory
            parentDirectory = item.path;
        }
        else {
            // check to make sure that this is not our root path
            String rootPath = null;
            String path;
            for (FilesType.FileLocation fl : rootFileLocation) {
                path = fl.path;
                if (item.path.contentEquals(path))    {
                    rootPath = fl.path;
                    break;
                }
            }
            if (rootPath != null) {
                parentDirectory = rootPath;
                item.setRootDir(true);
            }
            else
                parentDirectory = Utils.getParentDirectory(item.path);
        }
        Files.GetDirectory action = new Files.GetDirectory(item.path, new SortType.Sort("label", true, true));
        action.execute(hostManager.getConnection(), new ApiCallback<List<FilesType.FileLocation>>() {
            @Override
            public void onSuccess(List<FilesType.FileLocation> result) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.callBrowseFolderForFile.onSuccess()");
                if (!isAdded())
                    return;
                //for (FilesType.FileLocation item : result) {
                //LogUtils.LOGD(TAG, "FilesType.FileLocation label = " + item.label + ", path = " + item.path);
                //    adapter.add(item);
                //}
                // insert the parent directory as the first item in the list
                FilesType.FileLocation fl = new FilesType.FileLocation("..", parentDirectory, true);
                fl.setRootDir(item.isRootDir());
                result.add(0, fl);
                adapter.setFilelistItems(result);
                browseRootAlready = false;
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.callBrowseFolderForFile.onError()");
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_getting_source_info), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void playMediaFile(final String filename) {
        LogUtils.LOGD(TAG, "MediaFileListFragment.playMediaFile: filename = " + filename);
        Player.Open action = new Player.Open(filename);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.playMediaFile.onSuccess()");
                if (!isAdded())
                    return;
                while (mediaQueueFileLocation.size() > 0) {
                    queueMediaFile(mediaQueueFileLocation.poll());
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.playMediaFile.onError()");
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_play_media_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void queueMediaFile(final FilesType.FileLocation loc) {
        LogUtils.LOGD(TAG, "MediaFileListFragment.queueMediaFile: filename = " + loc.path);

        Playlist.Add action = new Playlist.Add(playlistId, loc);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.queueMediaFile.onSuccess()");
                if (!isAdded())
                    return;
                startPlayingIfNoActivePlayers();
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.queueMediaFile.onError()");
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_queue_media_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void startPlayingIfNoActivePlayers() {
        LogUtils.LOGD(TAG, "MediaFileListFragment.getActivePlayers()");
        Player.GetActivePlayers action = new Player.GetActivePlayers();
        action.execute(hostManager.getConnection(), new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result ) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.getActivePlayers.onSuccess()");
                if (!isAdded())
                    return;
                // find out if any player is running. If it is not, start one
                if (result.size() == 0) {
                    startPlaying(playlistId);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.getActivePlayers.onError()");
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_get_active_player), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

    }

    private void startPlaying(int playlistID) {
        LogUtils.LOGD(TAG, "MediaFileListFragment.startPlaying: playlistID = " + playlistID);
        Player.Open action = new Player.Open(playlistID);
        action.execute(hostManager.getConnection(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String result ) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.startPlaying.onSuccess()");
                if (!isAdded())
                    return;
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "MediaFileListFragment.startPlaying.onError()");
                if (!isAdded())
                    return;
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.error_play_media_file), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);
    }

    private class MediaFileListAdapter extends BaseAdapter implements ListAdapter {

        Context ctx;
        int resource;
        List<FilesType.FileLocation> fileLocationItems = null;

        private View.OnClickListener playlistItemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                if (fileLocationItems != null) {
                    final FilesType.FileLocation loc = fileLocationItems.get(position);
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
                                        playMediaFile(loc.path);
                                        return true;
                                    case R.id.action_play_from_this_item:
                                    {
                                        mediaQueueFileLocation.clear();
                                        FilesType.FileLocation fl;
                                        LogUtils.LOGD(TAG, "onMenuItemClick(): position = " + position + ", Queue size = " + (fileLocationItems.size() - position));
                                        for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                            fl = fileLocationItems.get(i);
                                            if (!fl.isDirectory) {
                                                LogUtils.LOGD(TAG, "Adding " + fl.path + " to play queue");
                                                mediaQueueFileLocation.add(fl);
                                            }
                                        }
                                        // start playing the selected one, then queue the rest make sure to queue
                                        // the selected on last so the it does not lose its place in the queue
                                        mediaQueueFileLocation.add(loc);
                                        playMediaFile(loc.path);
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
         * @param items
         */
        public void setFilelistItems(List<FilesType.FileLocation> items) {
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
        public FilesType.FileLocation getItem(int position) {
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
            FilesType.FileLocation fileLocation = this.getItem(position);
            viewHolder.label = fileLocation.label;
            viewHolder.path = fileLocation.path;
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
            //Drawable myDrawable = getResources().getDrawable(fileLocation.isDirectory ? R.drawable.icon_folder : R.drawable.icon_song);
            //viewHolder.art.setImageDrawable(myDrawable);
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
                viewHolder.contextMenu.setOnClickListener(playlistItemMenuClickListener);
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
}

