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
        String pluginUrl = PluginUrlUtils.toArtePluginUrl(playUri);
        assertNotNull(pluginUrl);
        assertEquals("plugin://plugin.video.arteplussept/play/SHOW/084692-000-A", pluginUrl);
    }

    @Test
    public void toPluginUrlVimeo() throws Exception {
        Uri playUriDefault = Uri.parse("https://vimeo.com/12345");
        String pluginUrlDefault = PluginUrlUtils.toVimeoPluginUrl(playUriDefault);
        assertEquals("plugin://plugin.video.vimeo/play/?video_id=12345", pluginUrlDefault);

        Uri playUriChannel = Uri.parse("https://vimeo.com/channels/staffpicks/654321");
        String pluginUrlChannel = PluginUrlUtils.toVimeoPluginUrl(playUriChannel);
        assertEquals("plugin://plugin.video.vimeo/play/?video_id=654321", pluginUrlChannel);

        Uri playUriShowcase = Uri.parse("https://vimeo.com/showcase/123/video/1234567");
        String pluginUrlShowcase = PluginUrlUtils.toVimeoPluginUrl(playUriShowcase);
        assertEquals("plugin://plugin.video.vimeo/play/?video_id=1234567", pluginUrlShowcase);

        Uri playUriUnlisted = Uri.parse("https://vimeo.com/1234/hash");
        String pluginUrlUnlisted = PluginUrlUtils.toVimeoPluginUrl(playUriUnlisted);
        assertEquals("plugin://plugin.video.vimeo/play/?video_id=1234:hash", pluginUrlUnlisted);
    }
}
