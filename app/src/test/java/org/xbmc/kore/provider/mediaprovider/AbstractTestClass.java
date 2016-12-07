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


import android.content.ContentResolver;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.xbmc.kore.BuildConfig;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.testutils.Database;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@Ignore
public class AbstractTestClass {
    protected static HostInfo hostInfo;
    protected static ShadowContentResolver shadowContentResolver;

    @Before
    public void setUp() throws Exception {
        MediaProvider provider = new MediaProvider();
        ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
        provider.onCreate();
        shadowContentResolver = Shadows.shadowOf(contentResolver);
        ShadowContentResolver.registerProvider("org.xbmc.kore.provider", provider);
        provider.onCreate();

        hostInfo = Database.fill(RuntimeEnvironment.application, contentResolver);
    }
}
