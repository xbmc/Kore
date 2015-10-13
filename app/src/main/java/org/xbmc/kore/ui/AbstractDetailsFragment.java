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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;
import org.xbmc.kore.service.LibrarySyncService;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.UIUtils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

abstract public class AbstractDetailsFragment extends Fragment {
	private static final String TAG = LogUtils.makeLogTag(AbstractDetailsFragment.class);

	public static final String ITEMID = "item_id";
	public static final String POSTERID = "poster_id";

	private HostManager hostManager;
	private HostInfo hostInfo;
	private EventBus bus;

	private ImageView posterPlaceHolder;

	// Displayed movie id
	private int itemId = -1;

	abstract protected View createView(LayoutInflater inflater, ViewGroup container);

	/**
	 * Should return one or more {@link org.xbmc.kore.service.LibrarySyncService} SyncTypes that
	 * this fragment initiates
	 * @return List of {@link org.xbmc.kore.service.LibrarySyncService} SyncType
	 */
	abstract protected ArrayList<String> getSyncTypes();

	protected void setupArguments(int itemId, ImageView poster) {
		Bundle args = new Bundle();
		args.putInt(ITEMID, itemId);

		if( poster != null ) {
			Bitmap bitmap = null;
			Drawable drawable = poster.getDrawable();
			if( drawable instanceof BitmapDrawable ) {
				bitmap = ((BitmapDrawable) poster.getDrawable()).getBitmap();
			}
			args.putParcelable(POSTERID, bitmap);
		}

		setArguments(args);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bus = EventBus.getDefault();
		hostManager = HostManager.getInstance(getActivity());
		hostInfo = hostManager.getHostInfo();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		itemId = getArguments().getInt(ITEMID, -1);

		if ((container == null) || (itemId == -1)) {
			// We're not being shown or there's nothing to show
			return null;
		}

		Bitmap posterBitMap = getArguments().getParcelable(POSTERID);
		if( posterBitMap != null ) {
			posterPlaceHolder = new ImageView(getActivity());
			posterPlaceHolder.setImageBitmap(posterBitMap);
		}

		return createView(inflater, container);
	}

	@Override
	public void onResume() {
		bus.register(this);
		super.onResume();
	}

	@Override
	public void onPause() {
		bus.unregister(this);
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
//        outState.putInt(ITEMID, itemId);
	}

	/**
	 * Event bus post. Called when the syncing process ended
	 *
	 * @param event Refreshes data
	 */
	public void onEventMainThread(MediaSyncEvent event) {
		boolean silentSync = false;
		if (event.syncExtras != null) {
			silentSync = event.syncExtras.getBoolean(LibrarySyncService.SILENT_SYNC, false);
		}

		ArrayList<String> syncTypes = getSyncTypes();

		if( ( syncTypes != null ) && (syncTypes.contains(event.syncType) ) ) {
			onSyncProcessEnded(event);
			if (event.status == MediaSyncEvent.STATUS_SUCCESS) {
				if (!silentSync) {
					Toast.makeText(getActivity(),
							R.string.sync_successful, Toast.LENGTH_SHORT)
							.show();
				}
			} else if (!silentSync) {
				String msg = (event.errorCode == ApiException.API_ERROR) ?
						String.format(getString(R.string.error_while_syncing), event.errorMessage) :
						getString(R.string.unable_to_connect_to_xbmc);
				Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Starts synchronisation process
	 * @param silentRefresh to indicate UI should or should not show message when refresh ended
	 * @param syncType {@link org.xbmc.kore.service.LibrarySyncService} SyncType to sync. If null syncType will not be synced.
	 * @param syncId {@link org.xbmc.kore.service.LibrarySyncService} Sync ID for item ID set through {@link #setupArguments(int, ImageView)}. If null syncing item ID will be skipped
	 */
	protected void startSyncProcess(boolean silentRefresh, String syncType, String syncId) {
		if(( syncType == null ) && ( syncId == null ))
			return;

		// Start the syncing process
		Intent syncIntent = new Intent(this.getActivity(), LibrarySyncService.class);

		if( syncType != null ) {
			syncIntent.putExtra(syncType, true);
		}

		if( syncId != null ) {
			syncIntent.putExtra(syncId, itemId);
		}

		Bundle syncExtras = new Bundle();
		syncExtras.putBoolean(LibrarySyncService.SILENT_SYNC, silentRefresh);
		syncIntent.putExtra(LibrarySyncService.SYNC_EXTRAS, syncExtras);

		getActivity().startService(syncIntent);
	}

	/**
	 * Called when sync process for types set through {@link #getSyncTypes()} ends
	 * @param event
	 */
	abstract protected void onSyncProcessEnded(MediaSyncEvent event);

	public HostManager getHostManager() {
		return hostManager;
	}

	public HostInfo getHostInfo() {
		return hostInfo;
	}

	public int getItemId() {
		return itemId;
	}

	public ImageView getPosterPlaceHolder() {
		return posterPlaceHolder;
	}

	/**
	 * Loads the image at posterUrl into poster and uses the image provided through {@link #setupArguments(int, ImageView)}
	 * as placeholder (if any). If placeholder is not available it will use a CharacterDrawable as placeholder image
	 *
	 * @param poster          ImageView that will hold the images (either the placeholder or the loaded image)
	 * @param posterUrl       URL to download poster image from
	 * @param title           Title of poster image
	 * @param posterWidth     width of poster image
	 * @param posterHeight    height of poster image
	 */
	public void displayPoster(ImageView poster, String posterUrl, String title, int posterWidth, int posterHeight) {
		if( posterPlaceHolder == null ) {
			UIUtils.loadImageWithCharacterAvatar(getActivity(), getHostManager(),
					posterUrl, title,
					poster, posterWidth, posterHeight);
		} else {
			UIUtils.loadImageIntoImageview(getHostManager(),
					posterUrl,
					poster, posterWidth, posterHeight, posterPlaceHolder);
		}
	}
}
