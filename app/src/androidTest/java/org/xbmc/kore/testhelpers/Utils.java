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

package org.xbmc.kore.testhelpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.provider.MediaProvider;
import org.xbmc.kore.testutils.Database;
import org.xbmc.kore.utils.LogUtils;

import java.lang.reflect.Method;

public class Utils {
    private static final String TAG = LogUtils.makeLogTag(Utils.class);

    private static final String ANIMATION_PERMISSION = "android.permission.SET_ANIMATION_SCALE";
    private static final float DISABLED = 0.0f;
    private static final float DEFAULT = 1.0f;

    public static void closeDrawer(final Activity activity) throws Throwable {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
                drawerLayout.closeDrawers();
            }
        });
    }

    public static void openDrawer(final ActivityTestRule<?> activityTestRule) throws Throwable {
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DrawerLayout drawerLayout = (DrawerLayout) activityTestRule.getActivity().findViewById(R.id.drawer_layout);
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });
        DrawerLayout drawerLayout = (DrawerLayout) activityTestRule.getActivity().findViewById(R.id.drawer_layout);
        while(true) {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                return;
        }
    }

    public static void switchHost(final Context context, Activity activity, final HostInfo hostInfo) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                HostManager.getInstance(context).switchHost(hostInfo);
            }
        });
    }

    public static void clearSharedPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    public static void setupMediaProvider(Context context) {
        MediaProvider mediaProvider = new MediaProvider();
        mediaProvider.setContext(context);
        mediaProvider.onCreate();
    }

    public static void disableAnimations(Context context) {
        int permStatus = context.checkCallingOrSelfPermission(ANIMATION_PERMISSION);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            setSystemAnimationsScale(DISABLED);
        }
    }

    public static void enableAnimations(Context context) {
        int permStatus = context.checkCallingOrSelfPermission(ANIMATION_PERMISSION);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            setSystemAnimationsScale(DEFAULT);
        }
    }

    private static void setSystemAnimationsScale(float animationScale) {
        try {
            Class<?> windowManagerStubClazz = Class.forName("android.view.IWindowManager$Stub");
            Method asInterface = windowManagerStubClazz.getDeclaredMethod("asInterface", IBinder.class);
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClazz.getDeclaredMethod("getService", String.class);
            Class<?> windowManagerClazz = Class.forName("android.view.IWindowManager");
            Method setAnimationScales = windowManagerClazz.getDeclaredMethod("setAnimationScales", float[].class);
            Method getAnimationScales = windowManagerClazz.getDeclaredMethod("getAnimationScales");

            IBinder windowManagerBinder = (IBinder) getService.invoke(null, "window");
            Object windowManagerObj = asInterface.invoke(null, windowManagerBinder);
            float[] currentScales = (float[]) getAnimationScales.invoke(windowManagerObj);
            for (int i = 0; i < currentScales.length; i++) {
                currentScales[i] = animationScale;
            }
            setAnimationScales.invoke(windowManagerObj, new Object[]{currentScales});
        } catch (Exception e) {
            Log.e("SystemAnimations", "Could not change animation scale to " + animationScale + " :'(");
        }
    }
}
