/*
 * Copyright 2018 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.tests.ui.remote.playlistfragment.TCP;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.testhelpers.action.ViewActions;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Playlist;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.xbmc.kore.testutils.TestUtils.createMusicItem;
import static org.xbmc.kore.testutils.TestUtils.createPictureItem;
import static org.xbmc.kore.testutils.TestUtils.createVideoItem;
import static org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.notifications.Playlist.OnClear;

public class PlaylistTests extends AbstractTestClass<RemoteActivity> {

    private static final int PLAYLIST_SIZE = 10;

    @Rule
    public ActivityTestRule<RemoteActivity> remoteActivityActivityTestRule =
            new ActivityTestRule<>(RemoteActivity.class);

    @Override
    protected ActivityTestRule<RemoteActivity> getActivityTestRule() {
        return remoteActivityActivityTestRule;
    }

    @Override
    protected void setSharedPreferences(Context context) {

    }

    @Override
    public void setUp() throws Throwable {
        int itemId = 0;

        getPlaylistHandler().reset();
        for (int i = 0; i < PLAYLIST_SIZE; i++) {
            getPlaylistHandler().addItemToPlaylist(Playlist.playlistID.AUDIO, createMusicItem(i, itemId++), false);
            getPlaylistHandler().addItemToPlaylist(Playlist.playlistID.VIDEO, createVideoItem(i, itemId++), false);
            getPlaylistHandler().addItemToPlaylist(Playlist.playlistID.PICTURE, createPictureItem(i, itemId++), false);
        }

        getPlayerHandler().reset();
        getPlayerHandler().setPlaylists(getPlaylistHandler().getPlaylists());
        getPlayerHandler().startPlay(Playlist.playlistID.AUDIO, 0);

        // Checking for available playlists is done in PlaylistFragment on startup
        // and every 10 seconds. To make sure PlaylistFragment can get the available
        // playlists at startup, the activity needs to be created after the backend
        // has been fully setup.
        super.setUp();

        onView(isRoot()).perform(swipeLeft());
        waitForAudioPlaylistToShow();
    }

    /**
     * Test if playlist is not cleared when playback is stopped
     *
     * UI interaction flow tested:
     *   1. Start playing multiple music items
     *   2. Stop playback
     *   3. Result: playlist should still be visible
     */
    @Test
    public void keepPlaylistOnStop() {
        onView(isRoot()).perform(swipeRight());
        EspressoTestUtils.clickButton(R.id.stop);
        onView(isRoot()).perform(swipeLeft());

        assertEquals(getPlaylistHandler().getPlaylist(Playlist.playlistID.AUDIO).size(), PLAYLIST_SIZE);
        EspressoTestUtils.checkListViewSize(PLAYLIST_SIZE, R.id.playlist);
    }

    /**
     * Test if playlist is not cleared when playback is paused
     *
     * UI interaction flow tested:
     *   1. Start playing multiple music items
     *   2. Pause playback
     *   3. Result: playlist should still be visible
     */
    @Test
    public void keepPlaylistOnPause() {
        onView(isRoot()).perform(swipeRight());
        EspressoTestUtils.clickButton(R.id.play);
        onView(isRoot()).perform(swipeLeft());

        assertEquals(getPlaylistHandler().getPlaylist(Playlist.playlistID.AUDIO).size(), PLAYLIST_SIZE);
        EspressoTestUtils.checkListViewSize(PLAYLIST_SIZE, R.id.playlist);
    }

    /**
     * Test if playlist is cleared when cleared on Kodi
     *
     * UI interaction flow tested:
     *   1. Start playing multiple music items
     *   2. Clear playlist on server (Kodi)
     *   3. Result: playlist should be empty
     */
    @Test
    public void clearPlaylistWhenClearedOnKodi() throws Exception {
        getPlaylistHandler().clearPlaylist(Playlist.playlistID.AUDIO);
        getConnectionHandlerManager().waitForNotification(OnClear.METHOD_NAME, 10000);

        assertEquals(0, getPlaylistHandler().getPlaylist(Playlist.playlistID.AUDIO).size());
        onView(allOf(withId(R.id.info_title), withText(R.string.playlist_empty)))
                .check(matches(isDisplayed()));
    }

    /**
     * Test if playback of a playlist is resumed after stopping playback
     *
     * UI interaction flow tested:
     *   1. Start playing multiple music items
     *   2. Stop playback
     *   3. Click on playlist item
     *   4. Result: playback should resume from clicked playlist item
     */
    @Test
    public void stopPlayingAndResumeNextItem() throws TimeoutException {
        int positionClicked = 3;
        onView(isRoot()).perform(swipeRight());
        EspressoTestUtils.clickButton(R.id.stop);
        onView(isRoot()).perform(swipeLeft());
        getConnectionHandlerManager().clearMethodsHandled();
        EspressoTestUtils.clickAdapterViewItem(positionClicked, R.id.playlist);
        getConnectionHandlerManager().waitForMethodHandled(Player.Open.METHOD_NAME, 10000);

        List<Player.GetItem> playlistOnServer = getPlaylistHandler().getPlaylist(Playlist.playlistID.AUDIO);
        assertSame(getPlayerHandler().getPlayState(), PlayerHandler.PLAY_STATE.PLAYING);
        assertEquals("Playlist on server has size " + playlistOnServer.size() +
                     " but should be " + PLAYLIST_SIZE, playlistOnServer.size(), PLAYLIST_SIZE);
        assertEquals("Current playing item ID is " + getPlayerHandler().getMediaItem().getLibraryId() +
                   ", but this should be " + playlistOnServer.get(positionClicked).getLibraryId(),
                   getPlayerHandler().getMediaItem().getLibraryId(), playlistOnServer.get(positionClicked).getLibraryId());
    }

    /**
     * Test if playlist is correctly restored after playback has stopped
     * and device configuration changed
     * UI interaction flow tested:
     *   1. Start playing multiple music items
     *   2. Rotate device
     *   3. Result: playlist should be the same as before rotation
     */
    @Test
    public void restorePlaylistAfterConfigurationChange() {
        getConnectionHandlerManager().clearMethodsHandled();
        EspressoTestUtils.rotateDevice(getActivity());
        waitForAudioPlaylistToShow();

        assertEquals(getPlaylistHandler().getPlaylist(Playlist.playlistID.AUDIO).size(), PLAYLIST_SIZE);
        EspressoTestUtils.checkListViewSize(PLAYLIST_SIZE, R.id.playlist);
    }

    /**
     * Test if playlist is correctly restored after playback has stopped
     * and device configuration changed
     * UI interaction flow tested:
     *   1. Start playing multiple music items
     *   2. Stop playback
     *   3. Rotate device
     *   4. Result: playlist should be the same as before rotation
     */
    @Test
    public void restorePlaylistAfterStopAndConfigurationChange() {
        onView(isRoot()).perform(swipeRight());
        EspressoTestUtils.clickButton(R.id.stop);
        onView(isRoot()).perform(swipeLeft());

        getConnectionHandlerManager().clearMethodsHandled();
        EspressoTestUtils.rotateDevice(getActivity());
        waitForAudioPlaylistToShow();

        assertEquals(getPlaylistHandler().getPlaylist(Playlist.playlistID.AUDIO).size(), PLAYLIST_SIZE);
        EspressoTestUtils.checkListViewSize(PLAYLIST_SIZE, R.id.playlist);
    }

    /**
     * Test if playlist for currently playing item is shown even if other
     * playlists are available on server
     * UI interaction flow tested:
     *   1. Add audio and video playlists on server
     *   2. Start playing video item
     *   3. Result: playlist for video items should be shown
     */
    @Test
    public void showCurrentlyPlayingPlaylist() {
        getPlayerHandler().startPlay(Playlist.playlistID.VIDEO, 0);
        onView(isRoot()).perform(ViewActions.waitForView(R.id.playlist_item_title, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((TextView) v).getText().toString().contains("Video");
            }
        }, 10000));

        assertEquals("Playlist on server has size "
                   + getPlaylistHandler().getPlaylist(Playlist.playlistID.VIDEO).size() +
                   " but should be " + PLAYLIST_SIZE,
                   getPlaylistHandler().getPlaylist(Playlist.playlistID.VIDEO).size(), PLAYLIST_SIZE);
        assertEquals("Got media type "
                   + getPlayerHandler().getMediaItem().getType() +
                   ", this should be " + Player.GetItem.TYPE.movie.name(),
                   getPlayerHandler().getMediaItem().getType(), Player.GetItem.TYPE.movie.name());

        onView(allOf(withText(getPlayerHandler().getMediaItem().getTitle()), isDisplayed())).check(matches(isDisplayed()));
    }

    /**
     * Test if playlist for last played item is shown when playback has stopped
     * and other playlists are available on server
     * UI interaction flow tested:
     *   1. Add audio, picture, and video playlists on server
     *   2. Start playing video item
     *   3. Stop playback
     *   4. Result: playlist for video items should be shown
     */
    @Test
    public void showLastActivePlaylist() {
        getPlayerHandler().startPlay(Playlist.playlistID.VIDEO, 0);
        onView(isRoot()).perform(swipeRight());
        EspressoTestUtils.clickButton(R.id.stop);

        onView(isRoot()).perform(swipeLeft());
        onView(isRoot()).perform(ViewActions.waitForView(R.id.playlist_item_title, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((TextView) v).getText().toString().contains("Video");
            }
        }, 10000));

        assertEquals("Playlist on server has size "
                     + getPlaylistHandler().getPlaylist(Playlist.playlistID.VIDEO).size() +
                     " but should be " + PLAYLIST_SIZE,
                     getPlaylistHandler().getPlaylist(Playlist.playlistID.VIDEO).size(), PLAYLIST_SIZE);
        assertEquals("Got media type "
                     + getPlayerHandler().getMediaItem().getType() +
                     ", this should be " + Player.GetItem.TYPE.movie.name(),
                     getPlayerHandler().getMediaItem().getType(), Player.GetItem.TYPE.movie.name());

        onView(allOf(withText(getPlayerHandler().getMediaItem().getTitle()), isDisplayed())).check(matches(isDisplayed()));
    }

    private void waitForAudioPlaylistToShow() {
        onView(isRoot()).perform(ViewActions.waitForView(R.id.playlist_item_title, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return "Music 1".contentEquals(((TextView) v).getText());
            }
        }, 10000));
    }
}
