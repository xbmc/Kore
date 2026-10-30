package org.xbmc.kore.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class BaseActivityTest {

    public static class TestActivity extends BaseActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat);
            super.onCreate(savedInstanceState);
        }
    }

    @Test
    public void appIsOpenOnlyWhileAnActivityExists() {
        assertFalse(BaseActivity.isAppOpen());

        ActivityController<TestActivity> first = Robolectric.buildActivity(TestActivity.class).create();
        assertTrue(BaseActivity.isAppOpen());

        ActivityController<TestActivity> second = Robolectric.buildActivity(TestActivity.class).create();
        first.destroy();
        assertTrue(BaseActivity.isAppOpen());

        second.destroy();
        assertFalse(BaseActivity.isAppOpen());
    }

    @Test
    public void appStaysOpenAcrossConfigurationChange() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class).create();
        assertTrue(BaseActivity.isAppOpen());

        controller.configurationChange();
        assertTrue(BaseActivity.isAppOpen());

        controller.destroy();
        assertFalse(BaseActivity.isAppOpen());
    }
}
