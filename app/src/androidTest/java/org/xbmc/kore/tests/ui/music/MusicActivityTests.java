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
import android.os.SystemClock;
import android.widget.TextView;

import androidx.test.espresso.Espresso;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.tests.ui.AbstractTestClass;
import org.xbmc.kore.ui.sections.audio.MusicActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickAlbumsTab;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickGenresTab;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickMusicVideosTab;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.rotateDevice;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemAndCheckActionbarTitle;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemPressBackAndCheckActionbarTitle;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemRotateDeviceAndCheckActionbarTitle;

public class MusicActivityTests extends AbstractTestClass<MusicActivity> {
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

    /**
     * Test if action bar title initially displays Music
     */
    @Test
    public void setActionBarTitleMain() {
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.default_toolbar))))
                .check(matches(withText(R.string.music)));
    }

    /**
     * Test if action bar title is correctly set after selecting an artist
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Result: action bar title should show list item title
     */
    @Test
    public void setActionBarTitleArtist() {
        selectListItemAndCheckActionbarTitle(ArtistTestData.title, R.id.list, ArtistTestData.title);
    }

    /**
     * Test if action bar title is correctly set after selecting an album
     *
     * UI interaction flow tested:
     *   1. Click on albums tab
     *   2. Click on list item
     *   3. Result: action bar title should show list item title
     */
    @Test
    public void setActionBarTitleAlbum() {
        clickAlbumsTab();
        selectListItemAndCheckActionbarTitle(AlbumTestData.title, R.id.list, AlbumTestData.title);
    }

    /**
     * Test if action bar title is correctly set after selecting a genre
     *
     * UI interaction flow tested:
     *   1. Click on genres tab
     *   2. Click on list item
     *   3. Result: action bar title should show list item title
     */
    @Test
    public void setActionBarTitleGenre() {
        clickGenresTab();
        selectListItemAndCheckActionbarTitle(GenreTestData.title, R.id.list, GenreTestData.title);
    }

    /**
     * Test if action bar title is correctly set after selecting a video
     *
     * UI interaction flow tested:
     *   1. Click on videos tab
     *   2. Click on list item
     *   3. Result: action bar title should show list item title
     */
    @Test
    public void setActionBarTitleVideo() {
        clickMusicVideosTab();
        selectListItemAndCheckActionbarTitle(MusicVideoTestData.title, R.id.list, MusicVideoTestData.title);
    }

    /**
     * Test if action bar title is correctly restored after a configuration change when artist
     * is selected
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Rotate device
     *   3. Result: action bar title should show list item title
     */
    @Test
    public void restoreActionBarTitleArtistOnConfigurationStateChanged() {
        SystemClock.sleep(10000);
        selectListItemRotateDeviceAndCheckActionbarTitle(ArtistTestData.title, R.id.list,
                                                         ArtistTestData.title, getActivity());
    }

    /**
     * Test if action bar title is correctly restored after a configuration change when album
     * is selected
     *
     * UI interaction flow tested:
     *   1. Select albums tab
     *   2. Click on list item
     *   3. Rotate device
     *   4. Result: action bar title should show list item title
     */
    @Test
    public void restoreActionBarTitleAlbumOnConfigurationStateChanged() {
        clickAlbumsTab();
        selectListItemRotateDeviceAndCheckActionbarTitle(AlbumTestData.title, R.id.list,
                                                         AlbumTestData.title,
                                                         getActivity());
    }

    /**
     * Test if action bar title is correctly restored after a configuration change when genre
     * is selected
     *
     * UI interaction flow tested:
     *   1. Select genres tab
     *   2. Click on list item
     *   3. Rotate device
     *   4. Result: action bar title should show list item title
     */
    @Test
    public void restoreActionBarTitleGenreOnConfigurationStateChanged() {
        clickGenresTab();
        selectListItemRotateDeviceAndCheckActionbarTitle(GenreTestData.title, R.id.list,
                                                         GenreTestData.title, getActivity());
    }

    /**
     * Test if action bar title is correctly restored after a configuration change when music video
     * is selected
     *
     * UI interaction flow tested:
     *   1. Select music videos tab
     *   2. Click on list item
     *   3. Rotate device
     *   4. Result: action bar title should show list item title
     */
    @Test
    public void restoreActionBarTitleMusicVideoOnConfigurationStateChanged() {
        clickMusicVideosTab();
        selectListItemRotateDeviceAndCheckActionbarTitle(MusicVideoTestData.title, R.id.list,
                                                         MusicVideoTestData.title,
                                                         getActivity());
    }

    /**
     * Test if action bar title is correctly restored after returning from artist selection
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: action bar title should show main title
     */
    @Test
    public void restoreActionBarTitleOnReturningFromArtist() {
        selectListItemPressBackAndCheckActionbarTitle(ArtistTestData.title, R.id.list,
                                                      getActivity().getString(R.string.music));
    }

    /**
     * Test if action bar title is correctly restored after returning from an album under
     * artist
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Select albums tab
     *   3. Press back
     *   4. Result: action bar title should show artist title
     */
    @Test
    public void restoreActionBarTitleOnArtistOnReturningFromAlbum() {
        EspressoTestUtils.clickRecyclerViewItem(ArtistTestData.title, R.id.list);
        clickAlbumsTab();
        selectListItemPressBackAndCheckActionbarTitle(ArtistTestData.album, R.id.list, ArtistTestData.title);
    }

    /**
     * Test if action bar title is correctly restored after returning from music video selection
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: action bar title should show main title
     */
    @Test
    public void restoreActionBarTitleOnReturningFromMusicVideo() {
        clickMusicVideosTab();
        selectListItemPressBackAndCheckActionbarTitle(MusicVideoTestData.title, R.id.list,
                                                      getActivity().getString(R.string.music));
    }

    /**
     * Test if action bar title is correctly restored after returning from genre selection
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: action bar title should show main title
     */
    @Test
    public void restoreActionBarTitleOnReturningFromGenre() {
        clickGenresTab();
        selectListItemPressBackAndCheckActionbarTitle(GenreTestData.title, R.id.list,
                                                      getActivity().getString(R.string.music));
    }

    /**
     * Test if action bar title is correctly restored after returning from album selection
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: action bar title should show main title
     */
    @Test
    public void restoreActionBarTitleOnReturningFromAlbum() {
        clickAlbumsTab();
        selectListItemPressBackAndCheckActionbarTitle(AlbumTestData.title, R.id.list,
                                                      getActivity().getString(R.string.music));
    }

    /**
     * Test if the initial state shows the hamburger icon
     */
    @Test
    public void showHamburgerInInitialState() {
        assertFalse(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is changed to an arrow when selecting a list item
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Result: navigation icon should be an arrow
     */
    @Test
    public void showArrowWhenSelectingListItem() {
        EspressoTestUtils.clickRecyclerViewItem(ArtistTestData.title, R.id.list);

        assertTrue(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is changed to an arrow when selecting a list item
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Press back
     *   3. Result: navigation icon should be a hamburger
     */
    @Test
    public void showHamburgerWhenSelectingListItemAndReturn() {
        EspressoTestUtils.clickRecyclerViewItem(ArtistTestData.title, R.id.list);

        Espresso.pressBack();

        assertFalse(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is restored to an arrow when selecting a list item
     * and rotating the device
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Rotate device
     *   3. Result: navigation icon should be an arrow
     */
    @Test
    public void restoreArrowOnConfigurationChange() {
        EspressoTestUtils.clickRecyclerViewItem(ArtistTestData.title, R.id.list);

        rotateDevice(getActivity());

        assertTrue(getActivity().getDrawerIndicatorIsArrow());
    }

    /**
     * Test if navigation icon is restored to an hamburger when selecting a list item
     * and rotating the device and returning to the list
     *
     * UI interaction flow tested:
     *   1. Click on list item
     *   2. Rotate device
     *   3. Press back
     *   4. Result: navigation icon should be a hamburger
     */
    @Test
    public void restoreHamburgerOnConfigurationChangeOnReturn() {
        EspressoTestUtils.clickRecyclerViewItem(ArtistTestData.title, R.id.list);
        rotateDevice(getActivity());
        Espresso.pressBack();

        assertTrue(EspressoTestUtils.getActivity() instanceof MusicActivity);
        assertFalse(((MusicActivity) EspressoTestUtils.getActivity()).getDrawerIndicatorIsArrow());
    }

    private static class ArtistTestData {
        static String title = "ABC Orch Conducted by Herschel Burke Gilbert";
        static String album = "Songs Of The West";
    }

    private static class AlbumTestData {
        static String title = "1958 - The Fabulous Johnny Cash";
    }

    private static class GenreTestData {
        static String title = "Ambient";
    }

    private static class MusicVideoTestData {
        static String title = "(You Drive Me) Crazy";
    }
}
