/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.tests.ui.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.test.rule.ActivityTestRule;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.testhelpers.action.ViewActions;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.testutils.tcpserver.handlers.PlayerHandler;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Application;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;
import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Playlist;
import org.xbmc.kore.ui.sections.audio.MusicActivity;
import org.xbmc.kore.ui.widgets.HighlightButton;
import org.xbmc.kore.ui.widgets.RepeatModeButton;

import java.util.concurrent.TimeoutException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickAdapterViewItem;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.rotateDevice;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.waitForPanelState;
import static org.xbmc.kore.testhelpers.Matchers.withHighlightState;
import static org.xbmc.kore.testhelpers.Matchers.withProgress;
import static org.xbmc.kore.testutils.TestUtils.createMusicItem;
import static org.xbmc.kore.testutils.TestUtils.createMusicVideoItem;
import static org.xbmc.kore.testutils.TestUtils.createVideoItem;

public class SlideUpPanelTests extends AbstractTestClass<MusicActivity> {

    @Rule
    public ActivityTestRule<MusicActivity> musicActivityActivityTestRule =
            new ActivityTestRule<>(MusicActivity.class);

    @Override
    protected ActivityTestRule<MusicActivity> getActivityTestRule() {
        return musicActivityActivityTestRule;
    }

    @Override
    protected void setSharedPreferences(Context context) {

    }

    @Override
    public void setUp() throws Throwable {
        super.setUp();

        getPlaylistHandler().reset();
        getPlaylistHandler().addItemToPlaylist(Playlist.playlistID.AUDIO, createMusicItem(0, 0), true);
        getPlaylistHandler().addItemToPlaylist(Playlist.playlistID.VIDEO, createVideoItem(0, 1), false);
        getPlaylistHandler().addItemToPlaylist(Playlist.playlistID.VIDEO, createMusicVideoItem(0, 2), false);

        getPlayerHandler().reset();
        getPlayerHandler().setPlaylists(getPlaylistHandler().getPlaylists());
        getPlayerHandler().startPlay(Playlist.playlistID.AUDIO, 0);

        waitForPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    /**
     * Test if panel title is correctly set
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Result: panel title should show current playing media item
     */
    @Test
    public void panelTitleTest() {
        Player.GetItem item = getPlayerHandler().getMediaItem();
        onView(withId(R.id.npp_title)).check(matches(withText(item.getTitle())));
    }

    /**
     * Test if panel buttons are correctly set for music items
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Result: panel should show next, play, and previous buttons
     */
    @Test
    public void panelButtonsMusicTest() {
        onView(withId(R.id.npp_next)).check(matches(isDisplayed()));
        onView(withId(R.id.npp_previous)).check(matches(isDisplayed()));
        onView(withId(R.id.npp_play)).check(matches(isDisplayed()));
    }

    /**
     * Test if panel buttons are correctly set for movie items
     *
     * UI interaction flow tested:
     *   1. Start playing a movie item
     *   2. Result: panel should show play button
     */
    @Test
    public void panelButtonsMoviesTest() {
        getPlayerHandler().startPlay(Playlist.playlistID.VIDEO, 0);
        Player.GetItem item = getPlayerHandler().getMediaItem();
        final String title = item.getTitle();
        onView(isRoot()).perform(ViewActions.waitForView(
                R.id.npp_title, new ViewActions.CheckStatus() {
                    @Override
                    public boolean check(View v) {
                        return title.contentEquals(((TextView) v).getText());
                    }
                }, 10000));

        onView(withId(R.id.npp_next)).check(matches(not(isDisplayed())));
        onView(withId(R.id.npp_previous)).check(matches(not(isDisplayed())));
        onView(withId(R.id.npp_play)).check(matches(isDisplayed()));
    }

    /**
     * Test if panel buttons are correctly set for music video items
     *
     * UI interaction flow tested:
     *   1. Start playing a music video item
     *   2. Result: panel should show next, play, and previous buttons
     */
    @Test
    public void panelButtonsMusicVideoTest() {
        getPlayerHandler().startPlay(Playlist.playlistID.VIDEO, 1);
        Player.GetItem item = getPlayerHandler().getMediaItem();
        final String title = item.getTitle();
        onView(isRoot()).perform(ViewActions.waitForView(
                R.id.npp_title, new ViewActions.CheckStatus() {
                    @Override
                    public boolean check(View v) {
                        return title.contentEquals(((TextView) v).getText());
                    }
                }, 10000));

        onView(withId(R.id.npp_next)).check(matches(isDisplayed()));
        onView(withId(R.id.npp_previous)).check(matches(isDisplayed()));
        onView(withId(R.id.npp_play)).check(matches(isDisplayed()));
    }

    /**
     * Test if shuffle button state is correctly set
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Click on shuffle button
     *   4. Result: shuffle button should be highlighted
     */
    @Test
    public void panelButtonsShuffleTest() {
        expandPanel();

        onView(withId(R.id.npp_shuffle)).perform(click());

        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_shuffle, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((HighlightButton) v).isHighlighted();
            }
        }, 10000));
    }

    /**
     * Test if repeat button state is correctly set
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Click on repeat button
     *   4. Result: repeat button should be highlighted and show single item repeat mode
     *   5. Click on repeat button
     *   6. Result: repeat button should be highlighted and show repeat playlist mode
     *   7. Click on repeat button
     *   8. Result: repeat button should not be highlighted
     */
    @Test
    public void panelButtonsRepeatModes() {
        expandPanel();

        //Initial state should be OFF
        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_repeat, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((RepeatModeButton) v).getMode() == RepeatModeButton.MODE.OFF;
            }
        }, 10000));

        // Test if repeat mode is set to ONE after first click
        onView(withId(R.id.npp_repeat)).perform(click());
        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_repeat, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((RepeatModeButton) v).getMode() == RepeatModeButton.MODE.ONE;
            }
        }, 10000));

        // Test if repeat mode is set to ALL after second click
        onView(withId(R.id.npp_repeat)).perform(click());
        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_repeat, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((RepeatModeButton) v).getMode() == RepeatModeButton.MODE.ALL;
            }
        }, 10000));


        // Test if repeat mode is set to OFF after third click
        onView(withId(R.id.npp_repeat)).perform(click());
        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_repeat, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((RepeatModeButton) v).getMode() == RepeatModeButton.MODE.OFF;
            }
        }, 10000));
    }

    /**
     * Test if panel collapsed state is restored on configuration changes
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Rotate device
     *   3. Result: panel state should be collapsed
     */
    @Test
    public void keepCollapsedOnRotate() {
        rotateDevice(getActivity());

        waitForPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    /**
     * Test if panel expanded state is restored on configuration changes
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Rotate device
     *   4. Result: panel state should be expanded
     */
    @Test
    public void keepExpandedOnRotate() {
        expandPanel();

        rotateDevice(getActivity());

        waitForPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }

    /**
     * Test if repeat button state is restored on configuration changes
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Click on repeat button
     *   4. Rotate device
     *   5. Result: repeat button state should be restored to state in step 2
     */
    @Test
    public void restoreRepeatButtonStateOnRotate() {
        expandPanel();
        onView(withId(R.id.npp_repeat)).perform(click());

        rotateDevice(getActivity());

        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_repeat, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((RepeatModeButton) v).getMode() == RepeatModeButton.MODE.ONE;
            }
        }, 10000));
    }

    /**
     * Test if shuffle button state is correctly set
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Click on shuffle button
     *   4. Result: shuffle button state should be set to shuffle
     */
    @Test
    public void setShuffleButtonState() {
        expandPanel();

        onView(withId(R.id.npp_shuffle)).perform(click()); //Set state to shuffled

        onView(withId(R.id.npp_shuffle)).check(matches(withHighlightState(true)));
    }

    /**
     * Test if shuffle button state is restored on configuration changes
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Click on shuffle button
     *   4. Rotate device
     *   5. Result: shuffle button state should be restored to state in step 2
     */
    @Test
    public void restoreShuffleButtonStateOnRotate() {
        expandPanel();
        onView(withId(R.id.npp_shuffle)).perform(click()); //Set state to shuffled

        rotateDevice(getActivityTestRule().getActivity());

        //Using waitForView as we need to wait for the rotate to finish
        onView(isRoot()).perform(ViewActions.waitForView(R.id.npp_shuffle, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((HighlightButton) v).isHighlighted();
            }
        }, 10000));
    }

    /**
     * Test if volume is correctly set at start
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Set volume at server
     *   3. Expand panel
     *   4. Result: Volume indicator should show the same volume level as set at the server
     */
    @Test
    public void setVolume() {
        final int volume = 16;

        getApplicationHandler().setVolume(volume, true);

        assertTrue(getApplicationHandler().getVolume() == volume);
        expandPanel();
        onView(withId(R.id.vli_seek_bar)).check(matches(withProgress(volume)));
        onView(withId(R.id.vli_volume_text)).check(matches(withText(String.valueOf(volume))));
    }

    /**
     * Test if changing volume through the volume slider, updates the volume indicator correctly
     * and sends the volume change to the server
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Set volume using slider
     *   4. Result: Volume indicator should show volume level and server should be set to new volume level
     */
    @Test
    public void changeVolume() throws TimeoutException {
        final int volume = 16;
        expandPanel();

        onView(withId(R.id.vli_seek_bar)).perform(ViewActions.slideSeekBar(volume));

        onView(withId(R.id.vli_seek_bar)).check(matches(withProgress(volume)));
        onView(withId(R.id.vli_volume_text)).check(matches(withText(String.valueOf(volume))));

        getConnectionHandlerManager().waitForMethodHandled(Application.SetVolume.METHOD_NAME, 10000);
        assertTrue("applicationHandler volume: "+ getApplicationHandler().getVolume()
                   + " != " + volume, getApplicationHandler().getVolume() == volume);
    }

    /**
     * Test if changing volume through the volume slider, updates the volume indicator correctly
     * and sends the volume change to the server
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Set volume using slider
     *   4. Result: Volume indicator should show volume level and server should be set to new volume level
     */
    @Test
    public void restoreVolumeIndicatorOnRotate() throws TimeoutException {
        final int volume = 16;
        expandPanel();
        onView(withId(R.id.vli_seek_bar)).perform(ViewActions.slideSeekBar(volume));

        rotateDevice(getActivity());

        assertTrue("applicationHandler volume: "+ getApplicationHandler().getVolume()
                   + " != " + volume, getApplicationHandler().getVolume() == volume);
        onView(isRoot()).perform(ViewActions.waitForView(R.id.vli_seek_bar, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                return ((SeekBar) v).getProgress() == volume;
            }
        }, 10000));
        onView(withId(R.id.vli_volume_text)).check(matches(withText(String.valueOf(volume))));
    }

    /**
     * Test if setting progression correctly updates the media progress indicator
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Pause playback
     *   3. Expand panel
     *   4. Set progression
     *   5. Result: Media progression indicator should be correctly updated and progression change
     *              should be sent to the server.
     */
    @Test
    public void setProgression() {
        final int progress = 16;
        final String progressText = "0:16";
        expandPanel();
        onView(withId(R.id.npp_play)).perform(click()); //Pause playback

        onView(withId(R.id.mpi_seek_bar)).perform(ViewActions.slideSeekBar(progress));

        onView(withId(R.id.mpi_progress)).check(matches(withText(progressText)));
        assertTrue(getPlayerHandler().getTimeElapsed() == progress);
    }

    /**
     * Test if progression is correctly restored after device configuration change
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Pause playback
     *   3. Expand panel
     *   4. Set progression
     *   5. Rotate device
     *   6. Result: Progression should be correctly same as before rotating the device.
     */
    @Test
    public void restoreProgressOnRotate() {
        final int progress = 16;
        final String progressText = "0:16";
        expandPanel();
        onView(withId(R.id.npp_play)).perform(click()); //Pause playback

        onView(withId(R.id.mpi_seek_bar)).perform(ViewActions.slideSeekBar(progress));
        rotateDevice(getActivity());

        assertEquals(getPlayerHandler().getTimeElapsed(), progress);
        onView(withId(R.id.mpi_progress)).check(matches(withProgress(progressText)));
        onView(withId(R.id.mpi_seek_bar)).check(matches(withProgress(progress)));
    }

    /**
     * Kodi resumes playback when progression changes.
     * Test if changing progression when player is paused caused
     * progression to start updating again
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Pause playback
     *   4. Set progression
     *   5. Start playback at server (that's what Kodi does)
     *   6. Result: Playback should start at paused position
     */
    @Test
    public void pauseSetProgressionPlay() {
        expandPanel();

        onView(withId(R.id.npp_play)).perform(click()); //Pause playback
        onView(withId(R.id.mpi_seek_bar)).perform(ViewActions.slideSeekBar(16));
        getPlayerHandler().startPlay();

        SeekBar seekBar = (SeekBar) getActivity().findViewById(R.id.mpi_seek_bar);
        final int progress = seekBar.getProgress();
        onView(isRoot()).perform(ViewActions.waitForView(
                R.id.mpi_seek_bar, new ViewActions.CheckStatus() {
                    @Override
                    public boolean check(View v) {
                        return ((SeekBar) v).getProgress() > progress;
                    }
                }, 10000));
    }

    /**
     * Test if panel's progressionbar progresses when playing media
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Result: Progression should be progressing
     */
    @Test
    public void progressionUpdaterStartedAfterPlay() {
        expandPanel();
        SeekBar seekBar = (SeekBar) getActivity().findViewById(R.id.mpi_seek_bar);
        final int progress = seekBar.getProgress();

        onView(isRoot()).perform(ViewActions.waitForView(
                R.id.mpi_seek_bar, new ViewActions.CheckStatus() {
                    @Override
                    public boolean check(View v) {
                        return ((SeekBar) v).getProgress() > progress;
                    }
                }, 10000));
    }

    /**
     * Test if panel's progression is maintained when starting a new activity
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Expand panel
     *   3. Set progression
     *   4. Switch to movies (new activity)
     *   5. Result: Progression should continue from step 3
     */
    @Test
    public void continueProgressionAfterSwitchingActivity() throws Throwable {
        final int progress = 24;
        expandPanel();
        onView(withId(R.id.mpi_seek_bar)).perform(ViewActions.slideSeekBar(progress));

        Utils.openDrawer(getActivityTestRule());
        clickAdapterViewItem(2, R.id.navigation_drawer); //select movie activity

        waitForPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        expandPanel();

        onView(isRoot()).perform(ViewActions.waitForView(R.id.mpi_seek_bar, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                int seekBarProgress = ((SeekBar) v).getProgress();
                return  (seekBarProgress > progress) && (seekBarProgress < (progress + 4));
            }
        }, 10000));
    }

    /**
     * Test if pause button pauses playback
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Pause playback
     *   3. Result: Server should stop playing and progressbar should pause
     */
    @Test
    public void pausePlayback() {
        onView(withId(R.id.npp_play)).perform(click());

        assertSame(getPlayerHandler().getPlayState(), PlayerHandler.PLAY_STATE.PAUSED);

        expandPanel();
        final int progress = ((SeekBar) getActivity().findViewById(R.id.mpi_seek_bar)).getProgress();
        SystemClock.sleep(1000); //wait one second to check if progression has indeed paused
        onView(isRoot()).perform(ViewActions.waitForView(R.id.mpi_seek_bar, new ViewActions.CheckStatus() {
            @Override
            public boolean check(View v) {
                int seekBarProgress = ((SeekBar) v).getProgress();
                return  seekBarProgress == progress;
            }
        }, 10000));
    }

    /**
     * Test if panel is not displayed when user disables the panel
     * through the preference screen
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Disable showing panel in settings
     *   3. Result: Panel should not show
     */
    @Test
    public void disableShowingPanelInPreferences() throws Throwable {
        Utils.openDrawer(getActivityTestRule());
        clickAdapterViewItem(10, R.id.navigation_drawer); //Show preference screen

        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        edit.putBoolean(Settings.KEY_PREF_SHOW_NOW_PLAYING_PANEL, false);
        edit.apply();
        pressBack();

        waitForPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    /**
     * Test if panel is displayed when user enables the panel
     * through the preference screen
     *
     * UI interaction flow tested:
     *   1. Start playing a music item
     *   2. Disable showing panel in settings
     *   3. Show Music screen
     *   4. Enable showing panel in settings
     *   4. Return to Music screen
     *   5. Result: Panel should show
     */
    @Test
    public void showPanelWhenUserEnablesPanel() throws Throwable {
        Utils.openDrawer(getActivityTestRule());
        clickAdapterViewItem(10, R.id.navigation_drawer); //Show preference screen
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        edit.putBoolean(Settings.KEY_PREF_SHOW_NOW_PLAYING_PANEL, false);
        edit.apply();
        pressBack();

        Utils.openDrawer(getActivityTestRule());
        clickAdapterViewItem(10, R.id.navigation_drawer); //Show preference screen
        edit.putBoolean(Settings.KEY_PREF_SHOW_NOW_PLAYING_PANEL, true);
        edit.apply();
        pressBack();

        waitForPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private void expandPanel() {
        int tries = 10;
        while (tries-- > 0) {
            try {
                onView(withId(R.id.npp_title)).perform(click());

                onView(isRoot()).perform(ViewActions.waitForView(R.id.now_playing_panel, new ViewActions.CheckStatus() {
                    @Override
                    public boolean check(View v) {
                        return ((SlidingUpPanelLayout) v).getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
                    }
                }, 1000));

                return;
            } catch (Exception e) {
                //Either the click event did not work or the panel did not expand.
                //Let's try again.
            }
        }
    }
}
