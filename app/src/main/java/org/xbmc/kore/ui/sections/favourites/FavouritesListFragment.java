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
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiList;
import org.xbmc.kore.jsonrpc.method.Favourites;
import org.xbmc.kore.jsonrpc.method.GUI;
import org.xbmc.kore.jsonrpc.type.FavouriteType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.ui.AbstractListFragment;
import org.xbmc.kore.ui.viewgroups.RecyclerViewEmptyViewSupport;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.MediaPlayerUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class FavouritesListFragment extends AbstractListFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FavouritesListFragment";

    private final Handler callbackHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getFavourites();
    }

    @Override
    protected RecyclerViewEmptyViewSupport.OnItemClickListener createOnItemClickListener() {
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
        return (view, position) -> {
            final FavouritesAdapter favouritesAdapter = (FavouritesAdapter) getAdapter();
            final HostManager hostManager = HostManager.getInstance(requireContext());

            final FavouriteType.DetailsFavourite detailsFavourite =
                    favouritesAdapter.getItem(position);
            if (detailsFavourite == null) {
                return;
            }
            if (detailsFavourite.type.equals(FavouriteType.FavouriteTypeEnum.WINDOW) &&
                !TextUtils.isEmpty(detailsFavourite.window)) {
                GUI.ActivateWindow activateWindow =
                        new GUI.ActivateWindow(detailsFavourite.window, detailsFavourite.windowParameter);
                activateWindow.execute(hostManager.getConnection(), genericApiCallback, callbackHandler);
            } else if (detailsFavourite.type.equals(FavouriteType.FavouriteTypeEnum.MEDIA) &&
                       !TextUtils.isEmpty(detailsFavourite.path)) {
                final PlaylistType.Item playlistItem = new PlaylistType.Item();
                playlistItem.file = detailsFavourite.path;
                MediaPlayerUtils.play(FavouritesListFragment.this, playlistItem);
            } else {
                Toast.makeText(getActivity(), R.string.unable_to_play_favourite_item, Toast.LENGTH_SHORT)
                     .show();
            }
        };
    }

    @Override
    protected RecyclerView.Adapter<ViewHolder> createAdapter() {
        return new FavouritesAdapter(requireContext(), HostManager.getInstance(requireContext()));
    }

    @Override
    public void onRefresh() {
        getFavourites();
    }

    private void getFavourites() {
        final HostManager hostManager = HostManager.getInstance(requireContext());
        final Favourites.GetFavourites action = new Favourites.GetFavourites();

        action.execute(hostManager.getConnection(), new ApiCallback<ApiList<FavouriteType.DetailsFavourite>>() {
            @Override
            public void onSuccess(ApiList<FavouriteType.DetailsFavourite> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got Favourites");

                // To prevent the empty text from appearing on the first load, set it now
                getEmptyView().setText(getString(R.string.no_favourites_found_refresh));
                ((FavouritesAdapter) getAdapter()).setFavouriteItems(result.items);
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

    private static class FavouritesAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final HostManager hostManager;
        private final int artWidth, artHeight;
        private final Context context;
        private final ArrayList<FavouriteType.DetailsFavourite> favouriteItems = new ArrayList<>();

        FavouritesAdapter(@NonNull Context context, HostManager hostManager) {
            this.context = context;
            this.hostManager = hostManager;
            Resources resources = context.getResources();
            artWidth = (int) (resources.getDimension(R.dimen.channellist_art_width) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
            artHeight = (int) (resources.getDimension(R.dimen.channellist_art_heigth) /
                    UIUtils.IMAGE_RESIZE_FACTOR);
        }

        public void setFavouriteItems(List<FavouriteType.DetailsFavourite> favouriteItems) {
            this.favouriteItems.clear();
            this.favouriteItems.addAll(favouriteItems);
            notifyDataSetChanged();
        }

        public FavouriteType.DetailsFavourite getItem(int position) {
            return favouriteItems.get(position);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.grid_item_channel,
                                                                    parent, false);
            return new ViewHolder(view, context, hostManager, artWidth, artHeight);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bindView(favouriteItems.get(position));
        }

        @Override
        public int getItemCount() {
            return favouriteItems.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView artView;
        final TextView titleView;
        final TextView detailView;
        HostManager hostManager;
        int artWidth;
        int artHeight;
        Context context;

        ViewHolder(View itemView, Context context, HostManager hostManager, int artWidth, int artHeight) {
            super(itemView);
            this.context = context;
            this.hostManager = hostManager;
            this.artWidth = artWidth;
            this.artHeight = artHeight;
            artView = itemView.findViewById(R.id.art);
            titleView = itemView.findViewById(R.id.title);
            detailView = itemView.findViewById(R.id.details);

            View contextMenu = itemView.findViewById(R.id.list_context_menu);
            contextMenu.setVisibility(View.GONE);
        }

        void bindView(FavouriteType.DetailsFavourite favouriteDetail) {
            titleView.setText(UIUtils.applyMarkup(context, favouriteDetail.title));

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
            detailView.setText(typeRes);

            UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                                                 favouriteDetail.thumbnail, favouriteDetail.title,
                                                 artView, artWidth, artHeight);
        }
    }
}
