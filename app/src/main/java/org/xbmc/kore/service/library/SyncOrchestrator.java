/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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

package org.xbmc.kore.service.library;

import android.app.Service;
import android.content.ContentResolver;
import android.os.Handler;

import org.greenrobot.eventbus.EventBus;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayDeque;
import java.util.Iterator;

public class SyncOrchestrator {
    public static final String TAG = LogUtils.makeLogTag(SyncOrchestrator.class);

    private final ArrayDeque<SyncItem> syncItems;
    private final Service syncService;
    private final int serviceStartId;
    private HostConnection hostConnection;
    private final HostInfo hostInfo;
    private final Handler callbackHandler;
    private final ContentResolver contentResolver;

    private SyncItem currentSyncItem;

    private Iterator<SyncItem> syncItemIterator;

    public interface OnSyncListener {
        void onSyncFinished(SyncOrchestrator syncOrchestrator);
    }

    private OnSyncListener listener;

    /**
     * Constructor
     * @param syncService Service on which to call {@link LibrarySyncService#stopSelf()} when finished
     * @param startId Service startid to use when calling {@link LibrarySyncService#stopSelf()}
     * @param hostInfo Host from which to sync
     * @param callbackHandler Handler on which to post callbacks
     * @param contentResolver Content resolver
     */
    public SyncOrchestrator(Service syncService, final int startId,
                            final HostInfo hostInfo,
                            final Handler callbackHandler,
                            final ContentResolver contentResolver) {
        this.syncService = syncService;
        this.syncItems = new ArrayDeque<>();
        this.serviceStartId = startId;
        this.hostInfo = hostInfo;
        this.callbackHandler = callbackHandler;
        this.contentResolver = contentResolver;
    }

    public void setListener(OnSyncListener listener) {
        this.listener = listener;
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    /**
     * Add this item to the sync list
     * @param syncItem Sync item
     */
    public void addSyncItem(SyncItem syncItem) {
        syncItems.add(syncItem);
    }

    public ArrayDeque<SyncItem> getSyncItems() {
        return syncItems;
    }

    private long startTime = -1;
    private long partialStartTime;

    /**
     * Starts the syncing process
     */
    public void startSync() {
        startTime = System.currentTimeMillis();
        hostConnection = new HostConnection(hostInfo);
        hostConnection.setProtocol(HostConnection.PROTOCOL_HTTP);
        syncItemIterator = syncItems.iterator();
        nextSync();
    }

    /**
     * Processes the next item on the sync list, or cleans up if it is finished.
     */
    private void nextSync() {
        if (syncItemIterator.hasNext()) {
            partialStartTime = System.currentTimeMillis();
            currentSyncItem = syncItemIterator.next();
            currentSyncItem.sync(this, hostConnection, callbackHandler, contentResolver);
        } else {
            LogUtils.LOGD(TAG, "Sync finished for all items. Total time: " +
                               (System.currentTimeMillis() - startTime));
            // No more syncs, cleanup.
            // No need to disconnect, as this is HTTP
            //hostConnection.disconnect();
            if (listener != null) {
                listener.onSyncFinished(this);
            }
            syncService.stopSelf(serviceStartId);
        }
    }

    /**
     * One of the syync items finish syncing
     */
    public void syncItemFinished() {
        LogUtils.LOGD(TAG, "Sync finished for item: " + currentSyncItem.getDescription() +
                           ". Total time: " + (System.currentTimeMillis() - partialStartTime));

        EventBus.getDefault()
                .post(new MediaSyncEvent(currentSyncItem.getSyncType(),
                                         currentSyncItem.getSyncParams(),
                                         MediaSyncEvent.STATUS_SUCCESS));

        syncItems.remove(currentSyncItem);

        nextSync();
    }

    /**
     * One of the sync items failed, stop and clean up
     * @param errorCode Error code
     * @param description Description
     */
    public void syncItemFailed(int errorCode, String description) {
        LogUtils.LOGD(TAG, "A Sync item has got an error. Sync item: " +
                           currentSyncItem.getDescription() +
                           ". Error description: " + description);
        // No need to disconnect, as this is HTTP
        //hostConnection.disconnect();
        EventBus.getDefault()
                .post(new MediaSyncEvent(currentSyncItem.getSyncType(),
                                         currentSyncItem.getSyncParams(),
                                         MediaSyncEvent.STATUS_FAIL, errorCode, description));
        // Keep syncing till the end
        nextSync();
        //syncService.stopSelf(serviceStartId);
    }
}
