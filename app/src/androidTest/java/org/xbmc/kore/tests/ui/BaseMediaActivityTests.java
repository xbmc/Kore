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

package org.xbmc.kore.tests.ui;

import android.content.Context;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;

import org.junit.Ignore;
import org.junit.Test;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.testhelpers.EspressoTestUtils;
import org.xbmc.kore.testhelpers.Utils;
import org.xbmc.kore.ui.BaseMediaActivity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.clickAdapterViewItem;
import static org.xbmc.kore.testhelpers.EspressoTestUtils.rotateDevice;

/**
 * Contains generic tests for all activities extending BaseMediaActivity
 * @param <T>
 */
@Ignore
abstract public class BaseMediaActivityTests<T extends BaseMediaActivity> extends AbstractTestClass<T> {

    @Override
    protected void setSharedPreferences(Context context) {

    }

    @Override
    protected void configureHostInfo(HostInfo hostInfo) {

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
        clickAdapterViewItem(0, R.id.list);

        assertTrue(((T) EspressoTestUtils.getActivity()).getDrawerIndicatorIsArrow());
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
        clickAdapterViewItem(0, R.id.list);
        Espresso.pressBack();

        assertFalse(((T) EspressoTestUtils.getActivity()).getDrawerIndicatorIsArrow());
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
        clickAdapterViewItem(0, R.id.list);
        rotateDevice(getActivity());

        assertTrue(((T) EspressoTestUtils.getActivity()).getDrawerIndicatorIsArrow());
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
        clickAdapterViewItem(0, R.id.list);
        rotateDevice(getActivity());
        Espresso.pressBack();

        assertFalse(((T) EspressoTestUtils.getActivity()).getDrawerIndicatorIsArrow());

    }
}
