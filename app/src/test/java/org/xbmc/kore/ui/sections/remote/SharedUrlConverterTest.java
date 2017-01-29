package org.xbmc.kore.ui.sections.remote;

import org.junit.Test;
import static org.junit.Assert.*;

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
    public void twitch_app_handler() {
        assertEquals(twitchUrl(TWITCH_ID), ShareHandlingFragment.TWITCH_APP
                .urlFrom("Watch blabla with me on Twitch! http://www.twitch.tv/" + TWITCH_ID + "?sr=a"));
    }

    @Test
    public void youtube_app_handler() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.YOUTUBE_APP
                .urlFrom("https://youtu.be/" + YT_ID));
    }

    @Test
    public void youtube_short_url() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://youtu.be/" + YT_ID));
    }

    @Test
    public void youtube_short_url_with_garbage() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom(GARBAGE_BEFORE + "https://youtu.be/" + YT_ID));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://youtu.be/" + YT_ID + GARBAGE_AFTER));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom(GARBAGE_BEFORE + "https://youtu.be/" + YT_ID + GARBAGE_AFTER));
    }

    @Test
    public void youtube_short_with_query() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://youtu.be/" + YT_ID + "?foo=bar&baz=1&quux"));
    }

    @Test
    public void youtube_long_url() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.youtube.com/watch?v=" + YT_ID));
    }

    @Test
    public void youtube_long_no_www() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://youtube.com/watch?v=" + YT_ID));
    }

    @Test
    public void youtube_long_with_garbage() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom(GARBAGE_BEFORE + "https://www.youtube.com/watch?v=" + YT_ID));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.youtube.com/watch?v=" + YT_ID + GARBAGE_AFTER));
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom(GARBAGE_BEFORE + "https://www.youtube.com/watch?v=" + YT_ID + GARBAGE_AFTER));
    }

    @Test
    public void youtube_long_extra_query_vars_before() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.youtube.com/watch?foo=bar&baz=1&quux&v=" + YT_ID));
    }

    @Test
    public void youtube_long_extra_query_vars_after() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.youtube.com/watch?v=" + YT_ID + "&foo=bar&baz=1&quux"));
    }
    @Test
    public void youtube_long_extra_query_vars_sandwiched() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.youtube.com/watch?foo=bar&v=" + YT_ID + "&baz=1&quux"));
    }

    @Test
    public void youtube_long_slash_before_query_segment() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.youtube.com/watch/?v=" + YT_ID));
    }

    @Test
    public void youtube_short_slash_before_query_segment() {
        assertEquals(YT_PLUGIN_PREFIX + YT_ID, ShareHandlingFragment.GENERIC
                .urlFrom("https://youtu.be/" + YT_ID + "/?foo=bar"));
    }

    @Test
    public void vimeo_url() {
        String expected = VM_PLUGIN_PREFIX + VM_ID;
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("http://www.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("http://player.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("http://vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("https://www.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("https://player.vimeo.com/" + VM_ID));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("https://vimeo.com/" + VM_ID));
    }

    @Test
    public void vimeo_id_is_the_first_numeric_segment() {
        String expected = VM_PLUGIN_PREFIX + VM_ID;
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("http://vimeo.com/" + VM_ID + "/a/b/c/987654321"));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("http://vimeo.com/a/b/" + VM_ID + "/c/987654321"));
        assertEquals(expected, ShareHandlingFragment.GENERIC
                .urlFrom("http://vimeo.com/a/b/c/" + VM_ID));
        assertNull(ShareHandlingFragment.GENERIC.urlFrom("http://vimeo.com/a/b/c?path=/" + VM_ID));
    }
}