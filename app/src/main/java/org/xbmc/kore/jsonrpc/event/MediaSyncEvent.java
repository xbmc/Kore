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
package org.xbmc.kore.jsonrpc.event;

import android.os.Bundle;

import org.xbmc.kore.service.library.LibrarySyncService;

/**
 * Event to post on {@link org.greenrobot.eventbus.EventBus} that notifies of a sync
 */
public class MediaSyncEvent {
    public static final int STATUS_FAIL = 0;
    public static final int STATUS_SUCCESS = 1;

    public final String syncType;
    public final int status;
    public final int errorCode;
    public final String errorMessage;
    public final Bundle syncExtras;

    /**
     * Creates a new sync event
     *
     * @param syncType One of the constants in {@link LibrarySyncService}
     */
    public MediaSyncEvent(String syncType, Bundle syncExtras, int status) {
        this(syncType, syncExtras, status, -1, null);
        // Assert that status is success
        if (status != STATUS_SUCCESS)
            throw new IllegalArgumentException("This MediaSyncEvent constructor should only be " +
                                               "called with a successful status.");
    }

    public MediaSyncEvent(String syncType, Bundle syncExtras,
                          int status, int errorCode, String errorMessage) {
        this.syncType = syncType;
        this.syncExtras = syncExtras;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
