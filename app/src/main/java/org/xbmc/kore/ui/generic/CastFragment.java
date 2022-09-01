/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.ui.generic;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.ui.AbstractFragment;
import org.xbmc.kore.ui.sections.video.AllCastActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

public class CastFragment
        extends AbstractFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(CastFragment.class);

    private static final String BUNDLE_ITEMID = "itemid";
    private static final String BUNDLE_TITLE = "title";
    private static final String BUNDLE_LOADER_TYPE = "loadertype";

    public enum TYPE {
        TVSHOW,
        MOVIE
    }

    public void setArgs(int itemId, String title, TYPE type) {
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_ITEMID, itemId);
        bundle.putString(BUNDLE_TITLE, title);
        bundle.putInt(BUNDLE_LOADER_TYPE, type.ordinal());
        setArguments(bundle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(BUNDLE_LOADER_TYPE))
            LoaderManager.getInstance(this).initLoader(bundle.getInt(BUNDLE_LOADER_TYPE), null, this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        int hostId = HostManager.getInstance(requireContext()).getHostInfo().getId();

        Uri uri;
        assert getArguments() != null;
        int itemId = getArguments().getInt(BUNDLE_ITEMID);

        if (id == TYPE.MOVIE.ordinal()) {
            uri = MediaContract.MovieCast.buildMovieCastListUri(hostId, itemId);
            return new CursorLoader(requireContext(), uri,
                                    MovieCastListQuery.PROJECTION, null, null, MovieCastListQuery.SORT);
        } else {
            uri = MediaContract.TVShowCast.buildTVShowCastListUri(hostId, itemId);
            return new CursorLoader(requireContext(), uri,
                                    TVShowCastListQuery.PROJECTION, null, null, TVShowCastListQuery.SORT);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }

        ArrayList<VideoType.Cast> castArrayList;

        assert getArguments() != null;
        int id = getArguments().getInt(BUNDLE_LOADER_TYPE);
        if (id == TYPE.MOVIE.ordinal()) {
            castArrayList = createMovieCastList(cursor);
        } else {
            castArrayList = createTVShowCastList(cursor);
        }

        View rootView = getView();
        if (rootView == null || rootView.findViewById(R.id.cast_list) == null) return;

        UIUtils.setupCastInfo(getActivity(), castArrayList,
                              getView().findViewById(R.id.cast_list),
                              AllCastActivity.buildLaunchIntent(getActivity(),
                                                                getArguments().getString(BUNDLE_TITLE),
                                                                castArrayList));
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    private ArrayList<VideoType.Cast> createMovieCastList(Cursor cursor) {
        ArrayList<VideoType.Cast> castArrayList = new ArrayList<>(cursor.getCount());
        do {
            castArrayList.add(new VideoType.Cast(cursor.getString(MovieCastListQuery.NAME),
                                                 cursor.getInt(MovieCastListQuery.ORDER),
                                                 cursor.getString(MovieCastListQuery.ROLE),
                                                 cursor.getString(MovieCastListQuery.THUMBNAIL)));
        } while (cursor.moveToNext());

        return castArrayList;
    }

    private ArrayList<VideoType.Cast> createTVShowCastList(Cursor cursor) {
        ArrayList<VideoType.Cast> castArrayList = new ArrayList<>(cursor.getCount());
        do {
            castArrayList.add(new VideoType.Cast(cursor.getString(TVShowCastListQuery.NAME),
                                                 cursor.getInt(TVShowCastListQuery.ORDER),
                                                 cursor.getString(TVShowCastListQuery.ROLE),
                                                 cursor.getString(TVShowCastListQuery.THUMBNAIL)));
        } while (cursor.moveToNext());

        return castArrayList;
    }

    public void refresh() {
        if (getArguments() == null) return;
        LoaderManager.getInstance(this)
                     .restartLoader(getArguments().getInt(BUNDLE_LOADER_TYPE), null, this);
    }

    /**
     * Movie cast list query parameters.
     */
    public interface MovieCastListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.MovieCast.NAME,
                MediaContract.MovieCast.ORDER,
                MediaContract.MovieCast.ROLE,
                MediaContract.MovieCast.THUMBNAIL,
                };

        String SORT = MediaContract.MovieCast.ORDER + " ASC";

        int ID = 0;
        int NAME = 1;
        int ORDER = 2;
        int ROLE = 3;
        int THUMBNAIL = 4;
    }

    /**
     * Movie cast list query parameters.
     */
    public interface TVShowCastListQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MediaContract.MovieCast.NAME,
                MediaContract.MovieCast.ORDER,
                MediaContract.MovieCast.ROLE,
                MediaContract.MovieCast.THUMBNAIL,
                };

        String SORT = MediaContract.TVShowCast.ORDER + " ASC";

        int ID = 0;
        int NAME = 1;
        int ORDER = 2;
        int ROLE = 3;
        int THUMBNAIL = 4;
    }
}
