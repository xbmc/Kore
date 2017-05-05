/*
 * Copyright 2017 XBMC Foundation. All rights reserved.
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
package org.xbmc.kore.ui.sections.favourites;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiList;
import org.xbmc.kore.jsonrpc.method.Favourites;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.FavouriteType;
import org.xbmc.kore.ui.AbstractListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.List;

import butterknife.ButterKnife;

public class FavouritesListFragment extends AbstractListFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FavouritesListFragment";

    private Handler callbackHandler = new Handler();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getFavourites();
    }

    @Override
    protected AdapterView.OnItemClickListener createOnItemClickListener() {
        final ApiCallback<String> genericApiCallback = new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Do Nothing
            }

            @Override
            public void onError(int errorCode, String description) {
                Toast.makeText(getActivity(), description, Toast.LENGTH_SHORT).show();
            }
        };
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final FavouritesAdapter favouritesAdapter = (FavouritesAdapter) getAdapter();
                final HostManager hostManager = HostManager.getInstance(getActivity());

                final FavouriteType.DetailsFavourite detailsFavourite =
                        favouritesAdapter.getItem(position);
                if (detailsFavourite == null) {
                    return;
                }
                if (detailsFavourite.type.equals(FavouriteType.FavouriteTypeEnum.WINDOW)
                        && !TextUtils.isEmpty(detailsFavourite.window)) {
                    GUI.ActivateWindow activateWindow = new GUI.ActivateWindow(detailsFavourite.window,
                            detailsFavourite.windowParameter);
                    hostManager.getConnection().execute(activateWindow, genericApiCallback, callbackHandler);
                } else if (detailsFavourite.type.equals(FavouriteType.FavouriteTypeEnum.MEDIA)
                        && !TextUtils.isEmpty(detailsFavourite.path)) {
                    Player.Open openPlayer = new Player.Open(detailsFavourite.path);
                    hostManager.getConnection().execute(openPlayer, genericApiCallback, callbackHandler);
                } else {
                    Toast.makeText(getActivity(), R.string.unable_to_play_favourite_item,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    protected BaseAdapter createAdapter() {
        return new FavouritesAdapter(getActivity(), HostManager.getInstance(getActivity()));
    }

    @Override
    public void onRefresh() {
        getFavourites();
    }

    private void getFavourites() {
        final HostManager hostManager = HostManager.getInstance(getActivity());
        final Favourites.GetFavourites action = new Favourites.GetFavourites();

        hostManager.getConnection().execute(action, new ApiCallback<ApiList<FavouriteType.DetailsFavourite>>() {
            @Override
            public void onSuccess(ApiList<FavouriteType.DetailsFavourite> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got Favourites");

                // To prevent the empty text from appearing on the first load, set it now
                getEmptyView().setText(getString(R.string.no_channels_found_refresh));
                setupFavouritesList(result.items);
                hideRefreshAnimation();
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting favourites: " + description);

                getEmptyView().setText(getString(R.string.error_favourites, description));
                Toast.makeText(getActivity(), getString(R.string.error_favourites, description),
                        Toast.LENGTH_SHORT).show();
                hideRefreshAnimation();
            }
        }, callbackHandler);
    }

    /**
     * Called to set the GridView with the favourites that are coming from the host.
     *
     * @param favourites the favourites list that is supplied to the GridView.
     */
    private void setupFavouritesList(List<FavouriteType.DetailsFavourite> favourites) {
        final FavouritesAdapter favouritesAdapter = (FavouritesAdapter) getAdapter();
        favouritesAdapter.clear();
        favouritesAdapter.addAll(favourites);
        favouritesAdapter.notifyDataSetChanged();
    }

    private static class FavouritesAdapter extends ArrayAdapter<FavouriteType.DetailsFavourite> {

        private final HostManager hostManager;
        private final int artWidth, artHeight;

        FavouritesAdapter(@NonNull Context context, HostManager hostManager) {
            super(context, R.layout.grid_item_channel);
            this.hostManager = hostManager;
            Resources resources = context.getResources();
            artWidth = (int) (resources.getDimension(R.dimen.channellist_art_width) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int) (resources.getDimension(R.dimen.channellist_art_heigth) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.grid_item_channel,
                        parent, false);
                final FavouriteItemViewHolder vh = new FavouriteItemViewHolder(convertView);
                convertView.setTag(vh);
            }

            final FavouriteItemViewHolder vh = (FavouriteItemViewHolder) convertView.getTag();
            final FavouriteType.DetailsFavourite favouriteDetail = getItem(position);

            // We don't need the context menu here.
            vh.contextMenu.setVisibility(View.GONE);

            vh.titleView.setText(favouriteDetail.title);

            @StringRes final int typeRes;
            switch (favouriteDetail.type) {
                case FavouriteType.FavouriteTypeEnum.MEDIA:
                    typeRes = R.string.media;
                    break;
                case FavouriteType.FavouriteTypeEnum.SCRIPT:
                    typeRes = R.string.script;
                    break;
                case FavouriteType.FavouriteTypeEnum.WINDOW:
                    typeRes = R.string.window;
                    break;
                default:
                    typeRes = R.string.unknown;
            }
            vh.detailView.setText(typeRes);

            UIUtils.loadImageWithCharacterAvatar(getContext(), hostManager,
                    favouriteDetail.thumbnail, favouriteDetail.title,
                    vh.artView, artWidth, artHeight);

            return convertView;
        }
    }

    private static class FavouriteItemViewHolder {
        final ImageView artView;
        final ImageView contextMenu;
        final TextView titleView;
        final TextView detailView;

        FavouriteItemViewHolder(View v) {
            artView = ButterKnife.findById(v, R.id.art);
            contextMenu = ButterKnife.findById(v, R.id.list_context_menu);
            titleView = ButterKnife.findById(v, R.id.title);
            detailView = ButterKnife.findById(v, R.id.details);
        }
    }
}
