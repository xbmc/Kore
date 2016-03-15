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

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.IBinder;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;

import org.xbmc.kore.R;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class Utils {
    private static final String TAG = LogUtils.makeLogTag(Utils.class);

    private static final String ANIMATION_PERMISSION = "android.permission.SET_ANIMATION_SCALE";
    private static final float DISABLED = 0.0f;
    private static final float DEFAULT = 1.0f;

    private static boolean isInitialized;

    private static HostInfo hostInfo;
    private static Context context;

    public static String readFile(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);

        int size = is.available();

        byte[] buffer = new byte[size];

        is.read(buffer);

        is.close();

        return new String(buffer, "UTF-8");
    }

    public static void closeDrawer(final ActivityTestRule<?> activityTestRule) throws Throwable {
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DrawerLayout drawerLayout = (DrawerLayout) activityTestRule.getActivity().findViewById(R.id.drawer_layout);
                drawerLayout.closeDrawers();
            }
        });
    }

    public static void initialize(ActivityTestRule<?> activityTestRule) throws Throwable {
        if (isInitialized)
            return;

        context = activityTestRule.getActivity();

        disableAnimations();

        hostInfo = Database.fill(context);

        HostManager.getInstance(context).switchHost(hostInfo);
        Utils.closeDrawer(activityTestRule);

        isInitialized = true;
    }

    public static void cleanup() {
        Database.flush(context, hostInfo);

        enableAnimations();

        isInitialized = false;
    }

    public static String cursorToString(Cursor cursor) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String name : cursor.getColumnNames()) {
            int index = cursor.getColumnIndex(name);
            stringBuffer.append(name + "=" + cursor.getString(index) + "\n");
        }
        return stringBuffer.toString();
    }

    private static void disableAnimations() {
        int permStatus = context.checkCallingOrSelfPermission(ANIMATION_PERMISSION);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            setSystemAnimationsScale(DISABLED);
        }
    }

    private static void enableAnimations() {
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

    public static boolean moveCursorTo(Cursor cursor, int index, int item) {
        if (( cursor == null ) || ( ! cursor.moveToFirst() ))
            return false;

        do {
            if ( cursor.getInt(index) == item )
                return true;
        } while (cursor.moveToNext());

        return false;
    }
}
