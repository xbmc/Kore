/*
 * Copyright 2015 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.ui;

import android.content.Intent;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.service.SyncUtils;
import org.xbmc.kore.utils.UIUtils;

public abstract class AbstractMusicListFragment extends AbstractListFragment {
	@Override
	protected void onSwipeRefresh() {
		// Start the syncing process
		Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);
		syncIntent.putExtra(LibrarySyncService.SYNC_ALL_MUSIC, true);
		getActivity().startService(syncIntent);
	}

	@Override
	protected void onSyncProcessEnded(MediaSyncEvent event) {
		if (event.syncType.equals(LibrarySyncService.SYNC_ALL_MUSIC)) {
			swipeRefreshLayout.setRefreshing(false);
			if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
				refreshList();
				Toast.makeText(getActivity(), R.string.sync_successful, Toast.LENGTH_SHORT)
						.show();
			} else {
				String msg = (event.errorCode == ApiException.API_ERROR) ?
						String.format(getString(R.string.error_while_syncing), event.errorMessage) :
						getString(R.string.unable_to_connect_to_xbmc);
				Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onServiceConnected(LibrarySyncService librarySyncService) {
		if(SyncUtils.isLibrarySyncing(librarySyncService, HostManager.getInstance(getActivity()).getHostInfo(),
				LibrarySyncService.SYNC_ALL_MUSIC, LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS)) {
			showRefreshAnimation();
		}
	}
}
