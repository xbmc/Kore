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
package org.xbmc.kore.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlaylistType;

import java.util.ArrayList;
import java.util.List;

/**
 * Because every project needs one of these
 * */
public class Utils {

    public static final int PERMISSION_REQUEST_WRITE_STORAGE = 0,
            PERMISSION_REQUEST_READ_PHONE_STATE = 1;

    /**
     * Returns whether the SDK is the Jellybean release or later.
     */
    public static boolean isJellybeanOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isJellybeanMR1OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean isJellybeanMR2OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean isKitKatOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean isLollipopOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isLollipopAndPreOreo() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) &&
               (Build.VERSION.SDK_INT < 27);
    }

    /**
     * Concats a list of strings...
     * @param list
     * @param delimiter
     * @return
     */
    public static String listStringConcat(List<String> list, String delimiter) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String item : list) {
            if (TextUtils.isEmpty(item)) continue;
            if (!first) builder.append(delimiter);
            builder.append(item);
            first = false;
        }
        return builder.toString();
    }

    /**
     * Concats a list of integers...
     * @param list
     * @param delimiter
     * @return
     */
    public static String listIntegerConcat(List<Integer> list, String delimiter) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Integer item : list) {
            if (!first) builder.append(delimiter);
            builder.append(item);
            first = false;
        }
        return builder.toString();
    }

    /**
     * Calls {@link Context#startActivity(Intent)} with the given <b>implicit</b> {@link Intent}
     * after making sure there is an Activity to handle it.
     * <br> <br> This may happen if e.g. the web browser has been disabled through restricted
     * profiles.
     *
     * @return Whether there was an Activity to handle the given {@link Intent}.
     */
    public static boolean tryStartActivity(Context context, Intent intent) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static final String IMDB_APP_PERSON_SEARCH_URI = "imdb:///find?q=%s&s=nm";
    public static final String IMDB_PERSON_SEARCH_URL = "http://m.imdb.com/find?q=%s&s=nm";

    public static final String IMDB_APP_MOVIE_URI = "imdb:///title/%s/";
    public static final String IMDB_MOVIE_URL = "http://m.imdb.com/title/%s/";

    /**
     * Open the IMDb app or web page for the given person name.
     */
    public static void openImdbForPerson(Context context, String name) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(IMDB_APP_PERSON_SEARCH_URI, name)));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // try launching IMDb app
        if (!Utils.tryStartActivity(context, intent)) {
            // on failure, try launching the web page
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(IMDB_PERSON_SEARCH_URL, name)));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            context.startActivity(intent);
        }
    }

    /**
     * Open the IMDb app or web page for the given person name.
     */
    public static void openImdbForMovie(Context context, String imdbNumber) {
        if (context == null || TextUtils.isEmpty(imdbNumber)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(IMDB_APP_MOVIE_URI, imdbNumber)));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // try launching IMDb app
        if (!Utils.tryStartActivity(context, intent)) {
            // on failure, try launching the web page
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(IMDB_MOVIE_URL, imdbNumber)));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            context.startActivity(intent);
        }
    }


    /**
     * Converts a drawable to a bitmap
     * @param drawable Drawable to convert
     * @return Bitmap
     */
    public static Bitmap drawableToBitmap (Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static void addToPlaylist(final Fragment fragment, final int itemId, final String playlistId) {
        Playlist.GetPlaylists getPlaylists = new Playlist.GetPlaylists();

        final Context context = fragment.getContext();
        final HostConnection hostConnection = HostManager.getInstance(context).getConnection();
        final Handler callbackHandler = new Handler();

        getPlaylists.execute(hostConnection, new ApiCallback<ArrayList<PlaylistType.GetPlaylistsReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlaylistType.GetPlaylistsReturnType> result) {
                if (!fragment.isAdded()) return;
                // Ok, loop through the playlists, looking for the video one
                int videoPlaylistId = -1;
                for (PlaylistType.GetPlaylistsReturnType playlist : result) {
                    if (playlist.type.equals(playlistId)) {
                        videoPlaylistId = playlist.playlistid;
                        break;
                    }
                }
                // If found, add to playlist
                if (videoPlaylistId != -1) {
                    PlaylistType.Item item = new PlaylistType.Item();
                    item.episodeid = itemId;
                    Playlist.Add action = new Playlist.Add(videoPlaylistId, item);
                    action.execute(hostConnection, new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            if (!fragment.isAdded()) return;
                            // Got an error, show toast
                            Toast.makeText(context, R.string.item_added_to_playlist, Toast.LENGTH_SHORT)
                                 .show();
                        }

                        @Override
                        public void onError(int errorCode, String description) {
                            if (!fragment.isAdded()) return;
                            // Got an error, show toast
                            Toast.makeText(context, R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                                 .show();
                        }
                    }, callbackHandler);
                } else {
                    Toast.makeText(context, R.string.no_suitable_playlist, Toast.LENGTH_SHORT)
                         .show();
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!fragment.isAdded()) return;
                // Got an error, show toast
                Toast.makeText(context, R.string.unable_to_connect_to_xbmc, Toast.LENGTH_SHORT)
                     .show();
            }
        }, callbackHandler);
    }
}
