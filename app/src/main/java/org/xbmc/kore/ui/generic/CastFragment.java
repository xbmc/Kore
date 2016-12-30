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
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.ui.AbstractAdditionalInfoFragment;
import org.xbmc.kore.ui.sections.video.AllCastActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

public class CastFragment extends AbstractAdditionalInfoFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(CastFragment.class);

    private static final String BUNDLE_ITEMID = "itemid";
    private static final String BUNDLE_TITLE = "title";
    private static final String BUNDLE_LOADER_TYPE = "loadertype";

    public static enum TYPE {
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = getArguments();
        getLoaderManager().initLoader(bundle.getInt(BUNDLE_LOADER_TYPE), null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        int hostId = HostManager.getInstance(getActivity()).getHostInfo().getId();

        Uri uri;
        int itemId = getArguments().getInt(BUNDLE_ITEMID);

        if (id == TYPE.MOVIE.ordinal()) {
            uri = MediaContract.MovieCast.buildMovieCastListUri(hostId, itemId);
            return new CursorLoader(getActivity(), uri,
                                    MovieCastListQuery.PROJECTION, null, null, MovieCastListQuery.SORT);
        } else {
            uri = MediaContract.TVShowCast.buildTVShowCastListUri(hostId, itemId);
            return new CursorLoader(getActivity(), uri,
                                    TVShowCastListQuery.PROJECTION, null, null, TVShowCastListQuery.SORT);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (! cursor.moveToFirst()) {
            return;
        }

        ArrayList<VideoType.Cast> castArrayList;

        int id = getArguments().getInt(BUNDLE_LOADER_TYPE);
        if (id == TYPE.MOVIE.ordinal()) {
            castArrayList = createMovieCastList(cursor);
        } else {
            castArrayList = createTVShowCastList(cursor);

        }

        UIUtils.setupCastInfo(getActivity(), castArrayList,
                              (GridLayout) getView().findViewById(R.id.cast_list),
                              AllCastActivity.buildLaunchIntent(getActivity(),
                                                                getArguments().getString(BUNDLE_TITLE),
                                                                castArrayList));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

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

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(getArguments().getInt(BUNDLE_LOADER_TYPE),
                                         null, this);
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
