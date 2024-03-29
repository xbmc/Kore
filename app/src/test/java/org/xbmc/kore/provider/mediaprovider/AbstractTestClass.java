/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.provider.mediaprovider;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.pm.ProviderInfo;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.testutils.Database;

@SuppressLint("IgnoreWithoutReason")
@RunWith(AndroidJUnit4.class)
@Ignore
@Config(sdk = 28)
public class AbstractTestClass {
    protected static HostInfo hostInfo;
    private static ContentResolver contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
    private static final String AUTHORITY = "org.xbmc.kore.provider";
    ContentProviderClient client;

    @Before
    public void setUp() throws Exception {
        MediaProvider provider = new MediaProvider();
        provider.onCreate();

        ProviderInfo info = new ProviderInfo();
        info.authority = AUTHORITY;
        Robolectric.buildContentProvider(MediaProvider.class).create(info);

        client = contentResolver.acquireContentProviderClient(AUTHORITY);

        hostInfo = Database.addHost(ApplicationProvider.getApplicationContext());

        Database.fill(hostInfo, ApplicationProvider.getApplicationContext(), contentResolver);
    }
}
