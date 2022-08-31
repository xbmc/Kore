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
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;

import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.host.HostConnection;
import org.xbmc.kore.jsonrpc.method.VideoLibrary;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.provider.MediaContract;
import org.xbmc.kore.utils.LogUtils;

import java.util.List;

public class SyncMusicVideos extends SyncItem {
    public static final String TAG = LogUtils.makeLogTag(SyncMusicVideos.class);

    private final int hostId;
    private final Bundle syncParams;

    /**
     * Syncs all the music videos on XBMC, to the local database
     * @param hostId XBMC host id
     */
    public SyncMusicVideos(final int hostId, Bundle syncParams) {
        this.hostId = hostId;
        this.syncParams = syncParams;
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "Sync music videos for host: " + hostId;
    }

    /** {@inheritDoc} */
    public String getSyncType() {
        return LibrarySyncService.SYNC_ALL_MUSIC_VIDEOS;
    }

    /** {@inheritDoc} */
    public Bundle getSyncParams() {
        return syncParams;
    }

    /** {@inheritDoc} */
    public void sync(final SyncOrchestrator orchestrator,
                     final HostConnection hostConnection,
                     final Handler callbackHandler,
                     final ContentResolver contentResolver) {
        String[] properties = {
                VideoType.FieldsMusicVideo.TITLE, VideoType.FieldsMusicVideo.PLAYCOUNT,
                VideoType.FieldsMusicVideo.RUNTIME, VideoType.FieldsMusicVideo.DIRECTOR,
                VideoType.FieldsMusicVideo.STUDIO, VideoType.FieldsMusicVideo.YEAR,
                VideoType.FieldsMusicVideo.PLOT, VideoType.FieldsMusicVideo.ALBUM,
                VideoType.FieldsMusicVideo.ARTIST, VideoType.FieldsMusicVideo.GENRE,
                VideoType.FieldsMusicVideo.TRACK, VideoType.FieldsMusicVideo.STREAMDETAILS,
                //VideoType.FieldsMusicVideo.LASTPLAYED,
                VideoType.FieldsMusicVideo.FANART,
                VideoType.FieldsMusicVideo.THUMBNAIL, VideoType.FieldsMusicVideo.FILE,
                // VideoType.FieldsMusicVideo.RESUME, VideoType.FieldsMusicVideo.DATEADDED,
                VideoType.FieldsMusicVideo.TAG,
                //VideoType.FieldsMusicVideo.ART
        };

        // Delete and sync all music videos
        VideoLibrary.GetMusicVideos action = new VideoLibrary.GetMusicVideos(properties);
        action.execute(hostConnection, new ApiCallback<List<VideoType.DetailsMusicVideo>>() {
            @Override
            public void onSuccess(List<VideoType.DetailsMusicVideo> result) {
                deleteMusicVideos(contentResolver, hostId);
                insertMusicVideos(result, contentResolver);
                orchestrator.syncItemFinished();
            }

            @Override
            public void onError(int errorCode, String description) {
                // Ok, something bad happend, just quit
                orchestrator.syncItemFailed(errorCode, description);
            }
        }, callbackHandler);
    }

    private void deleteMusicVideos(final ContentResolver contentResolver, int hostId) {
        // Delete all music videos
        String where = MediaContract.MusicVideosColumns.HOST_ID + "=?";
        contentResolver.delete(MediaContract.MusicVideos.CONTENT_URI,
                               where, new String[]{String.valueOf(hostId)});
    }

    public void insertMusicVideos(List<VideoType.DetailsMusicVideo> musicVideos, ContentResolver contentResolver) {
        ContentValues[] musicVideosValuesBatch = new ContentValues[musicVideos.size()];

        // Iterate on each music video
        for (int i = 0; i < musicVideos.size(); i++) {
            VideoType.DetailsMusicVideo musicVideo = musicVideos.get(i);
            musicVideosValuesBatch[i] = SyncUtils.contentValuesFromMusicVideo(hostId, musicVideo);
        }

        // Insert the movies
        contentResolver.bulkInsert(MediaContract.MusicVideos.CONTENT_URI, musicVideosValuesBatch);
    }
}
