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

import android.support.test.rule.ActivityTestRule;
import android.widget.TextView;

import org.junit.Rule;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.tests.ui.BaseMediaActivityTests;
import org.xbmc.kore.ui.sections.audio.MusicActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickAlbumsTab;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickGenresTab;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickMusicVideosTab;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemAndCheckActionbarTitle;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemPressBackAndCheckActionbarTitle;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.selectListItemRotateDeviceAndCheckActionbarTitle;

public class MusicActivityTests extends BaseMediaActivityTests<MusicActivity> {

    @Rule
    public ActivityTestRule<MusicActivity> musicActivityActivityTestRule =
            new ActivityTestRule<>(MusicActivity.class);

    @Override
    protected ActivityTestRule<MusicActivity> getActivityTestRule() {
        return musicActivityActivityTestRule;
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
        selectListItemAndCheckActionbarTitle(0, R.id.list, "ABC Orch");
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
        selectListItemAndCheckActionbarTitle(0, R.id.list, "1958 - The Fabulous Johnny Cash");
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
        selectListItemAndCheckActionbarTitle(0, R.id.list, "Ambient");
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
        selectListItemAndCheckActionbarTitle(0, R.id.list, "(You Drive Me) Crazy");
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
        selectListItemRotateDeviceAndCheckActionbarTitle(0, R.id.list,
                                                         "ABC Orch", getActivity());
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
        selectListItemRotateDeviceAndCheckActionbarTitle(0, R.id.list,
                                                         "1958 - The Fabulous Johnny Cash",
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
        selectListItemRotateDeviceAndCheckActionbarTitle(0, R.id.list,
                                                         "Ambient", getActivity());
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
        selectListItemRotateDeviceAndCheckActionbarTitle(0, R.id.list,
                                                         "(You Drive Me) Crazy",
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
        selectListItemPressBackAndCheckActionbarTitle(0, R.id.list,
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
        EspressoTestUtils.clickAdapterViewItem(0, R.id.list);
        clickAlbumsTab();
        selectListItemPressBackAndCheckActionbarTitle(0, R.id.list, "ABC Orch");
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
        selectListItemPressBackAndCheckActionbarTitle(0, R.id.list,
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
        selectListItemPressBackAndCheckActionbarTitle(0, R.id.list,
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
        selectListItemPressBackAndCheckActionbarTitle(0, R.id.list,
                                                      getActivity().getString(R.string.music));
    }
}
