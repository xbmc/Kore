package org.xbmc.kore.ui.sections.favourites;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
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
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class FavouritesListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FavouritesListFragment";

    private Handler callbackHandler = new Handler();

    private HostManager hostManager;
    private FavouritesAdapter favouritesAdapter;

    @InjectView(android.R.id.empty)
    TextView emptyView;
    @InjectView(R.id.list)
    GridView gridView;
    @InjectView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_generic_media_list, container, false);
        ButterKnife.inject(this, root);

        hostManager = HostManager.getInstance(getActivity());

        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        gridView.setEmptyView(emptyView);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        swipeRefreshLayout.setOnRefreshListener(this);
        getFavourites();

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

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (favouritesAdapter == null) {
                    return;
                }

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
        });
    }

    @Override
    public void onRefresh() {
        getFavourites();
    }

    private void getFavourites() {
        final Favourites.GetFavourites action = new Favourites.GetFavourites();
        hostManager.getConnection().execute(action, new ApiCallback<ApiList<FavouriteType.DetailsFavourite>>() {
            @Override
            public void onSuccess(ApiList<FavouriteType.DetailsFavourite> result) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Got Favourites");

                // To prevent the empty text from appearing on the first load, set it now
                emptyView.setText(getString(R.string.no_channels_found_refresh));
                setupFavouritesList(result.items);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isAdded()) return;
                LogUtils.LOGD(TAG, "Error getting favourites: " + description);

                emptyView.setText(getString(R.string.error_favourites, description));
                Toast.makeText(getActivity(), getString(R.string.error_favourites, description),
                        Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        }, callbackHandler);
    }

    /**
     * Called to set the GridView with the favourites that are coming from the host.
     *
     * @param favourites the favourites list that is supplied to the GridView.
     */
    private void setupFavouritesList(List<FavouriteType.DetailsFavourite> favourites) {
        if (favouritesAdapter == null) {
            favouritesAdapter = new FavouritesAdapter(getActivity(), R.layout.grid_item_channel,
                    hostManager);
        }
        gridView.setAdapter(favouritesAdapter);

        favouritesAdapter.clear();
        favouritesAdapter.addAll(favourites);
        favouritesAdapter.notifyDataSetChanged();
    }

    private static class FavouritesAdapter extends ArrayAdapter<FavouriteType.DetailsFavourite> {

        private final HostManager hostManager;
        private final int artWidth, artHeight;

        FavouritesAdapter(@NonNull Context context, @LayoutRes int resource, HostManager hostManager) {
            super(context, resource);
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
            vh.detailView.setText(favouriteDetail.type);

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
