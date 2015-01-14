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
package com.syncedsynapse.kore2.utils;

import android.text.TextUtils;
import android.util.Base64;

import com.squareup.picasso.UrlConnectionDownloader;

import java.net.HttpURLConnection;

/**
 * Picasso Downloader that sets basic authentication in the headers
 */
public class BasicAuthPicassoDownloader extends UrlConnectionDownloader {

    protected final String username;
    protected final String password;

    public BasicAuthPicassoDownloader(android.content.Context context) {
        super(context);
        this.username = null;
        this.password = null;
    }

    public BasicAuthPicassoDownloader(android.content.Context context, String username, String password) {
        super(context);
        this.username = username;
        this.password = password;
    }

    @Override
    protected HttpURLConnection openConnection(android.net.Uri uri)
            throws java.io.IOException {
        HttpURLConnection urlConnection = super.openConnection(uri);

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            String creds = username + ":" + password;
            urlConnection.setRequestProperty("Authorization", "Basic " +
                    Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP));
        }
        return urlConnection;
    }
}
