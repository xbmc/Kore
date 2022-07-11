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

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Handler;

import org.xbmc.kore.host.HostConnection;

/**
 * Represent an item that can be synced
 */
public abstract class SyncItem {
    /**
     * Syncs an item from the XBMC host to the local database
     * @param orchestrator Orchestrator to call when finished
     * @param hostConnection Host connection to use
     * @param callbackHandler Handler on which to post callbacks
     * @param contentResolver Content resolver
     */
    abstract public void sync(final SyncOrchestrator orchestrator,
                              final HostConnection hostConnection,
                              final Handler callbackHandler,
                              final ContentResolver contentResolver);

    /**
     * Friendly description of this sync item
     * @return Description
     */
    abstract public String getDescription();

    /**
     * Returns the sync event that should be posted after completion
     * @return Sync type, one of the constants in {@link LibrarySyncService}
     */
    abstract public String getSyncType();

    /**
     * Returns the extras that were passed during creation.
     * Allows the caller to pass parameters that will be sent back to him
     * @return Sync extras passed during construction
     */
    abstract public Bundle getSyncExtras();
}
