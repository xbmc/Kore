/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.method.Application;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.ui.sections.hosts.AddHostActivity;
import org.xbmc.kore.utils.UIUtils;

/**
 * Base activity, where common behaviour is implemented
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Host manager singleton
     */
    protected HostManager hostManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(UIUtils.getThemeResourceId(
                prefs.getString(Settings.KEY_PREF_THEME, Settings.DEFAULT_PREF_THEME)));
        super.onCreate(savedInstanceState);

        hostManager = HostManager.getInstance(this);

        // Check if we have any hosts setup
        if (hostManager.getHostInfo() == null && !(this instanceof AddHostActivity)) {
            final Intent intent = new Intent(this, AddHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Override hardware volume keys and send to Kodi
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Check whether we should intercept this
        boolean useVolumeKeys = android.support.v7.preference.PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Settings.KEY_PREF_USE_HARDWARE_VOLUME_KEYS,
                        Settings.DEFAULT_PREF_USE_HARDWARE_VOLUME_KEYS);
        if (useVolumeKeys) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (action == KeyEvent.ACTION_DOWN) {
                        interactWithNowPlayingPanel();
                        new Application
                                .SetVolume(GlobalType.IncrementDecrement.INCREMENT)
                                .execute(hostManager.getConnection(), null, null);
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (action == KeyEvent.ACTION_DOWN) {
                        interactWithNowPlayingPanel();
                        new Application
                                .SetVolume(GlobalType.IncrementDecrement.DECREMENT)
                                .execute(hostManager.getConnection(), null, null);
                    }
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void interactWithNowPlayingPanel() {
        if (this instanceof BaseMediaActivity) {
            ((BaseMediaActivity) this).expandNowPlayingPanel();
        }
    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.global, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_settings:
//                return true;
//            default:
//                break;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
