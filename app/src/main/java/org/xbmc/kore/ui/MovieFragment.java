package org.xbmc.kore.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.provider.MediaDatabase;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * Created by danhdroid on 3/14/15.
 */
public class MovieFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        SearchView.OnQueryTextListener {
    private static final String TAG = LogUtils.makeLogTag(MovieFragment.class);

    public interface OnMovieSelectedListener {
        public void onMovieSelected(int movieId, String movieTitle);
    }

    // Loader IDs
    private static final int LOADER_MOVIES = 0;

    // The search filter to use in the loader
    private String searchFilter = null;

    // Movies adapter
    private CursorAdapter adapter;

    // Activity listener
    private OnMovieSelectedListener listenerActivity;

    private HostManager hostManager;
    private HostInfo hostInfo;
    private EventBus bus;

    @InjectView(R.id.list)
    GridView moviesGridView;
    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(android.R.id.empty)
    TextView emptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_generic_media_list, container, false);
        ButterKnife.inject(this, root);

        bus = EventBus.getDefault();
        hostManager = HostManager.getInstance(getActivity());
        hostInfo = hostManager.getHostInfo();

        swipeRefreshLayout.setOnRefreshListener(this);
        //UIUtils.setSwipeRefreshLayoutColorScheme(swipeRefreshLayout);

        // Pad main content view to overlap with bottom system bar
//        UIUtils.setPaddingForSystemBars(getActivity(), moviesGridView, false, false, true);
//        moviesGridView.setClipToPadding(false);

        return root;
    }


    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        moviesGridView.setEmptyView(emptyView);
        moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the movie id from the tag
                ViewHolder tag = (ViewHolder) view.getTag();
                // Notify the activity
                listenerActivity.onMovieSelected(tag.movieId, tag.movieTitle);
            }
        });

        // Configure the adapter and start the loader
        adapter = new MoviesAdapter(getActivity());
        moviesGridView.setAdapter(adapter);
        getLoaderManager().initLoader(LOADER_MOVIES, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerActivity = (OnMovieSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMovieSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void onResume() {
        bus.register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        bus.unregister(this);
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.movie_list, menu);

        // Setup search view
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.action_search_movies));

        // Setup filters
        MenuItem hideWatched = menu.findItem(R.id.action_hide_watched),
                ignoreArticles = menu.findItem(R.id.action_ignore_prefixes),
                sortByName = menu.findItem(R.id.action_sort_by_name),
                sortByDateAdded = menu.findItem(R.id.action_sort_by_date_added);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hideWatched.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED));
        ignoreArticles.setChecked(preferences.getBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, Settings.DEFAULT_PREF_MOVIES_IGNORE_PREFIXES));

        int sortOrder = preferences.getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
        switch (sortOrder) {
            case Settings.SORT_BY_DATE_ADDED:
                sortByDateAdded.setChecked(true);
                break;
            default:
                sortByName.setChecked(true);
                break;
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        switch (item.getItemId()) {
            case R.id.action_hide_watched:
                item.setChecked(!item.isChecked());
                preferences.edit()
                        .putBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, item.isChecked())
                        .apply();
                getLoaderManager().restartLoader(LOADER_MOVIES, null, this);
                break;
            case R.id.action_ignore_prefixes:
                item.setChecked(!item.isChecked());
                preferences.edit()
                        .putBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, item.isChecked())
                        .apply();
                getLoaderManager().restartLoader(LOADER_MOVIES, null, this);
                break;
            case R.id.action_sort_by_name:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_NAME)
                        .apply();
                getLoaderManager().restartLoader(LOADER_MOVIES, null, this);
                break;
            case R.id.action_sort_by_date_added:
                item.setChecked(true);
                preferences.edit()
                        .putInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.SORT_BY_DATE_ADDED)
                        .apply();
                getLoaderManager().restartLoader(LOADER_MOVIES, null, this);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Search view callbacks
     */
    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextChange(String newText) {
        searchFilter = newText;
        getLoaderManager().restartLoader(LOADER_MOVIES, null, this);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextSubmit(String newText) {
        // All is handled in onQueryTextChange
        return true;
    }

    /**
     * Swipe refresh layout callback
     */
    /** {@inheritDoc} */
    @Override
    public void onRefresh () {
        if (hostInfo != null) {
            // Make sure we're showing the refresh
            swipeRefreshLayout.setRefreshing(true);
            // Start the syncing process
            Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
            syncIntent.putExtra(LibrarySyncService.SYNC_ALL_MOVIES, true);
            getActivity().startService(syncIntent);

//            Toast.makeText(getActivity(),
//                    String.format(getString(R.string.sync_movies_for_host), hostInfo.getName()),
//                    Toast.LENGTH_SHORT)
//                 .show();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Event bus post. Called when the syncing process ended
     *
     * @param event Refreshes data
     */
    public void onEventMainThread(MediaSyncEvent event) {
        boolean silentSync = false;
        if (event.syncExtras != null) {
            silentSync = event.syncExtras.getBoolean(LibrarySyncService.SILENT_SYNC, false);
        }

        if (event.syncType.equals(LibrarySyncService.SYNC_SINGLE_MOVIE) ||
                event.syncType.equals(LibrarySyncService.SYNC_ALL_MOVIES)) {
            swipeRefreshLayout.setRefreshing(false);
            if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
                getLoaderManager().restartLoader(LOADER_MOVIES, null, this);
                if (!silentSync) {
                    Toast.makeText(getActivity(), R.string.sync_successful, Toast.LENGTH_SHORT)
                            .show();
                }
            } else if (!silentSync) {
                String msg = (event.errorCode == ApiException.API_ERROR) ?
                        String.format(getString(R.string.error_while_syncing), event.errorMessage) :
                        getString(R.string.unable_to_connect_to_xbmc);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Loader callbacks
     */
    /** {@inheritDoc} */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = MediaContract.Movies.buildMoviesListUri(hostInfo != null? hostInfo.getId() : -1);

        StringBuilder selection = new StringBuilder();
        String selectionArgs[] = null;
        if (!TextUtils.isEmpty(searchFilter)) {
            selection.append(MediaContract.MoviesColumns.TITLE + " LIKE ?");
            selectionArgs = new String[] {"%" + searchFilter + "%"};
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.getBoolean(Settings.KEY_PREF_MOVIES_FILTER_HIDE_WATCHED, Settings.DEFAULT_PREF_MOVIES_FILTER_HIDE_WATCHED)) {
            if (selection.length() != 0)
                selection.append(" AND ");
            selection.append(MediaContract.MoviesColumns.PLAYCOUNT)
                    .append("=0");
        }

        String sortOrderStr;
        int sortOrder = preferences.getInt(Settings.KEY_PREF_MOVIES_SORT_ORDER, Settings.DEFAULT_PREF_MOVIES_SORT_ORDER);
        if (sortOrder == Settings.SORT_BY_DATE_ADDED) {
            sortOrderStr = MovieListQuery.SORT_BY_DATE_ADDED;
        } else {
            // Sort by name
            if (preferences.getBoolean(Settings.KEY_PREF_MOVIES_IGNORE_PREFIXES, Settings.DEFAULT_PREF_MOVIES_IGNORE_PREFIXES)) {
                sortOrderStr = MovieListQuery.SORT_BY_NAME_IGNORE_ARTICLES;
            } else {
                sortOrderStr = MovieListQuery.SORT_BY_NAME;
            }
        }

        return new CursorLoader(getActivity(), uri,
                MovieListQuery.PROJECTION, selection.toString(), selectionArgs, sortOrderStr);
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        adapter.swapCursor(cursor);
        // To prevent the empty text from appearing on the first load, set it now
        emptyView.setText(getString(R.string.no_movies_found_refresh));
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.swapCursor(null);
    }

    /**
     * Movie list query parameters.
     */
    private interface MovieListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.Movies.MOVIEID,
                MediaContract.Movies.TITLE,
                MediaContract.Movies.THUMBNAIL,
                MediaContract.Movies.YEAR,
                MediaContract.Movies.GENRES,
                MediaContract.Movies.RUNTIME,
                MediaContract.Movies.RATING,
                MediaContract.Movies.TAGLINE,
        };

        String SORT_BY_NAME = MediaContract.Movies.TITLE + " ASC";
        String SORT_BY_DATE_ADDED = MediaContract.Movies.DATEADDED + " DESC";
        String SORT_BY_NAME_IGNORE_ARTICLES = MediaDatabase.sortCommonTokens(MediaContract.Movies.TITLE) + " ASC";

        final int ID = 0;
        final int MOVIEID = 1;
        final int TITLE = 2;
        final int THUMBNAIL = 3;
        final int YEAR = 4;
        final int GENRES = 5;
        final int RUNTIME = 6;
        final int RATING = 7;
        final int TAGLINE = 8;
    }

    private static class MoviesAdapter extends CursorAdapter {

        private HostManager hostManager;
        private int artWidth, artHeight;

        public MoviesAdapter(Context context) {
            super(context, null, false);
            this.hostManager = HostManager.getInstance(context);

            // Get the art dimensions
            Resources resources = context.getResources();
            artWidth = (int)(resources.getDimension(R.dimen.movielist_art_width) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int)(resources.getDimension(R.dimen.movielist_art_heigth) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, final Cursor cursor, ViewGroup parent) {
            final View view = LayoutInflater.from(context)
                    .inflate(R.layout.grid_item_movie, parent, false);

            // Setup View holder pattern
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.titleView = (TextView)view.findViewById(R.id.title);
            viewHolder.detailsView = (TextView)view.findViewById(R.id.details);
//            viewHolder.yearView = (TextView)view.findViewById(R.id.year);
            viewHolder.durationView = (TextView)view.findViewById(R.id.duration);
            viewHolder.artView = (ImageView)view.findViewById(R.id.art);

            view.setTag(viewHolder);
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder viewHolder = (ViewHolder)view.getTag();

            // Save the movie id
            viewHolder.movieId = cursor.getInt(MovieListQuery.MOVIEID);
            viewHolder.movieTitle = cursor.getString(MovieListQuery.TITLE);

            viewHolder.titleView.setText(viewHolder.movieTitle);
            String details = TextUtils.isEmpty(cursor.getString(MovieListQuery.TAGLINE)) ?
                    cursor.getString(MovieListQuery.GENRES) :
                    cursor.getString(MovieListQuery.TAGLINE);
            viewHolder.detailsView.setText(details);
//            viewHolder.yearView.setText(String.valueOf(cursor.getInt(MovieListQuery.YEAR)));
            int runtime = cursor.getInt(MovieListQuery.RUNTIME) / 60;
            String duration =  runtime > 0 ?
                    String.format(context.getString(R.string.minutes_abbrev), String.valueOf(runtime)) +
                            "  |  " + String.valueOf(cursor.getInt(MovieListQuery.YEAR)) :
                    String.valueOf(cursor.getInt(MovieListQuery.YEAR));
            viewHolder.durationView.setText(duration);
            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                    cursor.getString(MovieListQuery.THUMBNAIL), viewHolder.movieTitle,
                    viewHolder.artView, artWidth, artHeight);
        }
    }

    /**
     * View holder pattern
     */
    private static class ViewHolder {
        TextView titleView;
        TextView detailsView;
        //        TextView yearView;
        TextView durationView;
        ImageView artView;

        int movieId;
        String movieTitle;
    }
}

