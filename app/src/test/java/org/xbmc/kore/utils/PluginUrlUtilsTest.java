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

package org.xbmc.kore.utils;

import android.net.Uri;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.annotation.Config;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class PluginUrlUtilsTest {
	
    @Test
    public void isHostArte() throws Exception {
        assertTrue(PluginUrlUtils.isHostArte("www.arte.tv"));
    }
	
    @Test
    public void toPluginUrlArte() throws Exception {
    Uri playUri = Uri.parse("https://www.arte.tv/fr/videos/084692-000-A/mongolie-le-reve-d-une-jeune-nomade/");
	String pluginUrl = PluginUrlUtils.toPluginUrlArte(playUri);
    assertNotNull(pluginUrl);
	assertEquals("plugin://plugin.video.arteplussept/play/SHOW/084692-000-A", pluginUrl);
    }	
}
