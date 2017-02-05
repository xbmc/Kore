package org.xbmc.kore.ui.sections.remote;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 23)
public class SharedUrlConverterTest {
    private static final String YT_PLUGIN_PREFIX = "plugin://plugin.video.youtube/play/?video_id=";
    private static final String VM_PLUGIN_PREFIX = "plugin://plugin.video.vimeo/play/?video_id=";
    private static final String YT_ID = "vOHNyS8KPYY";
    private static final String VM_ID = "12345678";
    private static final String TWITCH_ID = "arteezy";
    private static final String GARBAGE_BEFORE = "asdf sdf sdf ";
    private static final String GARBAGE_AFTER = " asdf sdf sdf";

    private String twitchUrl(String id) {
        return "plugin://plugin.video.twitch/playLive/" + id + "/";
    }

    @Test
    public void twitch_actual_shared_text() {
        assertEquals(twitchUrl(TWITCH_ID), ShareHandlingFragment
                .urlFrom("Watch blabla with me on Twitch! http://www.twitch.tv/" + TWITCH_ID + "?sr=a"));
    }
    @Test
    public void youtube_short_url() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://youtu.be/" + YT_ID));
    }

    @Test
    public void youtube_short_url_with_garbage() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom(GARBAGE_BEFORE + "https://youtu.be/" + YT_ID));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://youtu.be/" + YT_ID + GARBAGE_AFTER));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom(GARBAGE_BEFORE + "https://youtu.be/" + YT_ID + GARBAGE_AFTER));
    }

    @Test
    public void youtube_short_with_query() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://youtu.be/" + YT_ID + "?foo=bar&baz=1&quux"));
    }

    @Test
    public void youtube_long_url() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://www.youtube.com/watch?v=" + YT_ID));
    }

    @Test
    public void youtube_long_no_www() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://youtube.com/watch?v=" + YT_ID));
    }

    @Test
    public void youtube_long_with_garbage() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom(GARBAGE_BEFORE + "https://www.youtube.com/watch?v=" + YT_ID));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://www.youtube.com/watch?v=" + YT_ID + GARBAGE_AFTER));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom(GARBAGE_BEFORE + "https://www.youtube.com/watch?v=" + YT_ID + GARBAGE_AFTER));
    }

    @Test
    public void youtube_long_extra_query_vars_before() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://www.youtube.com/watch?foo=bar&baz=1&quux&v=" + YT_ID));
    }

    @Test
    public void youtube_long_extra_query_vars_after() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://www.youtube.com/watch?v=" + YT_ID + "&foo=bar&baz=1&quux"));
    }
    @Test
    public void youtube_long_extra_query_vars_sandwiched() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://www.youtube.com/watch?foo=bar&v=" + YT_ID + "&baz=1&quux"));
    }

    @Test
    public void youtube_long_slash_before_query_part() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://www.youtube.com/watch/?v=" + YT_ID));
    }

    @Test
    public void youtube_short_slash_before_query_part() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment
                .urlFrom("https://youtu.be/" + YT_ID + "/?foo=bar"));
    }

    @Test
    public void youtube_bad_long_urls() {
        for (String s : new String[] {
                "https://www.youtube.com/watch/?u=" + YT_ID,
                "https://wwe.youtube.com/watch/?v=" + YT_ID,
                "https://youtube.com/?v=" + YT_ID,
        }) {
            assertNull(ShareHandlingFragment.urlFrom(s));
        }
    }

    @Test
    public void vimeo_url() {
        String expected = VM_PLUGIN_PREFIX + VM_ID;
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("http://www.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("http://player.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("http://vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("https://www.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("https://player.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("https://vimeo.com/" + VM_ID));
    }

    @Test
    public void vimeo_id_is_the_first_numeric_segment() {
        String expected = VM_PLUGIN_PREFIX + VM_ID;
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("http://vimeo.com/" + VM_ID + "/a/b/c/987654321"));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("http://vimeo.com/a/b/" + VM_ID + "/c/987654321"));
        assertEquals(expected, ShareHandlingFragment
                .urlFrom("http://vimeo.com/a/b/c/" + VM_ID));
        assertNull(ShareHandlingFragment.urlFrom("http://vimeo.com/a/b/c?path=/" + VM_ID));
    }

    private static void weAreElectric() {
        try {
            ShareHandlingFragment.urlFrom("http://svtplay.se/video/0/");
        } catch (Throwable e) {
            assumeNoException(e);
        }
    }

    @Test
    public void svtplay_url_format() throws UnsupportedEncodingException {
        weAreElectric();
        String path = "/1234567890/lorem/1psum?dolor=sit&amet#foo-bar-baz";
        assertEquals(svtplayPluginUrl(path), ShareHandlingFragment
                .urlFrom("http://www.svtplay.se/video" + path));
        assertEquals(svtplayPluginUrl(path), ShareHandlingFragment
                .urlFrom("https://www.svtplay.se/video" + path));
    }

    @Test
    public void svtplay_path_should_have_a_trailing_slash() {
        assertNull(ShareHandlingFragment.urlFrom("http://www.svtplay.se/video/12345"));
    }

    @Test
    public void svtplay_path_should_start_with_a_numeric_segment() {
        for (String path : new String[] {"/a1/", "/1a/", "//1/", "/a/1/"}) {
            assertNull(ShareHandlingFragment.urlFrom("http://www.svtplay.se/video" + path));
        }
    }

    private String svtplayPluginUrl(String path) throws UnsupportedEncodingException {
        return "plugin://plugin.video.svtplay/"
                + "?url=%2Fvideo"
                + URLEncoder.encode(path, "UTF-8")
                + "&mode=video";
    }

    @Test
    public void case_insensitive() {
        weAreElectric();
        for (String url : new String[] {
                "HtTp://yOUtu.Be/" + YT_ID,
                "htTP://M.YOutUbE.Com/waTCH?V=" + YT_ID,
                "HTTPs://WWw.TwiTch.tv/" + TWITCH_ID,
                "hTTPs://PLayeR.vimeO.coM/" + VM_ID,
                "hTtp://SVTPlay.Se/video/12345/",
        }) {
            assertNotNull(ShareHandlingFragment.urlFrom(url));
        }
    }
}
