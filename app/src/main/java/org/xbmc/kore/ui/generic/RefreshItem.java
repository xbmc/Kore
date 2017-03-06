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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.service.library.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import de.greenrobot.event.EventBus;

public class RefreshItem {
    private final String TAG = LogUtils.makeLogTag(RefreshItem.class);

    public interface RefreshItemListener {
        void onSyncProcessEnded(MediaSyncEvent event);
    }

    private RefreshItemListener listener;

    private String syncType;
    private String syncID;
    private int itemId;
    private Context context;
    private SwipeRefreshLayout swipeRefreshLayout;

    private RefreshItem() {}

    /**
     * RefreshItem can be used to refresh the information for one or more items.
     * If you want to sync a single item you will need to use {@link #setSyncItem(String, int)}
     * to set the item that needs to be refreshed.
     * @param context
     * @param syncType {@link LibrarySyncService} SyncType
     */
    public RefreshItem(Context context, String syncType) {
        if (syncType == null) {
            throw new IllegalArgumentException("Argument syncType can not be null");
        }
        this.syncType = syncType;
        this.context = context;
    }

    /**
     * Sets the item that needs to be refreshed. Only required if you want to refresh a single item.
     * @param syncID {@link LibrarySyncService} syncID if you want to refresh a single item.
     * @param itemId the item ID of the single item (set with syncID) you want to refresh.
     */
    public void setSyncItem(String syncID, int itemId) {
        this.syncID = syncID;
        this.itemId = itemId;
    }

    /**
     * @return {@link LibrarySyncService} SyncType
     */
    public String getSyncType() {
        return syncType;
    }

    /**
     * Specifiy a listener if you want to be notified when the synchronization has finished.
     * @param listener
     */
    public void setListener(RefreshItemListener listener) {
        this.listener = listener;
    }

    /**
     * If you use a SwipeRefreshLayout you can let RefreshItem manage the refresh animation
     * by passing in the reference to SwipeRefreshLayout in your View
     * @param swipeRefreshLayout
     */
    public void setSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    public void startSync(boolean silentRefresh) {
        LogUtils.LOGD(TAG, "Starting sync. Silent? " + silentRefresh);

        HostInfo hostInfo = HostManager.getInstance(context).getHostInfo();

        if (hostInfo != null) {
            register();

            if ((swipeRefreshLayout != null) && (!silentRefresh)) {
                UIUtils.showRefreshAnimation(swipeRefreshLayout);
            }
            // Start the syncing process
            Intent syncIntent = new Intent(context, LibrarySyncService.class);

            syncIntent.putExtra(syncType, true);

            if ((syncID != null) && (itemId != -1)) {
                syncIntent.putExtra(syncID, itemId);
            }

            Bundle syncExtras = new Bundle();
            syncExtras.putBoolean(LibrarySyncService.SILENT_SYNC, silentRefresh);
            syncIntent.putExtra(LibrarySyncService.SYNC_EXTRAS, syncExtras);

            context.startService(syncIntent);
        } else {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(context, R.string.no_xbmc_configured, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    /**
     * Event bus post. Called when the syncing process ended
     *
     * @param event Refreshes data
     */
    public void onEventMainThread(MediaSyncEvent event) {
        unregister();

        if (! event.syncType.equals(syncType))
            return;

        boolean silentRefresh = false;
        if (event.syncExtras != null) {
            silentRefresh = event.syncExtras.getBoolean(LibrarySyncService.SILENT_SYNC, false);
        }

        if( swipeRefreshLayout != null ) {
            swipeRefreshLayout.setRefreshing(false);
        }

        if (listener != null) {
            listener.onSyncProcessEnded(event);
        }

        if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
            if (!silentRefresh) {
                Toast.makeText(context,
                               R.string.sync_successful, Toast.LENGTH_SHORT)
                     .show();
            }
        } else if (!silentRefresh) {
            String msg = (event.errorCode == ApiException.API_ERROR) ?
                         String.format(context.getString(R.string.error_while_syncing), event.errorMessage) :
                         context.getString(R.string.unable_to_connect_to_xbmc);
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public void unregister() {
        EventBus eventBus = EventBus.getDefault();
        if ( eventBus.isRegistered(this) ) {
            eventBus.unregister(this);
        }
    }

    public void register() {
        EventBus eventBus = EventBus.getDefault();
        if ( ! eventBus.isRegistered(this) ) {
            eventBus.register(this);
        }
    }
}
