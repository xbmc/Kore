/*
 * Copyright 2012 Google Inc.
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

package org.xbmc.kore.utils;

import android.util.Log;

import org.xbmc.kore.BuildConfig;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.jsonrpc.HostConnection;

import java.util.Arrays;
import java.util.List;

/**
 * Log utils shamelessly ripped from Google's iosched app...
 */
public class LogUtils {
    private static final int MAX_LOG_TAG_LENGTH = 23;

    // TODO: Remove this later
    private static final List<String> doNotLogTags = Arrays.asList(
//            HostConnection.TAG,
//            HostConnectionObserver.TAG
    );

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH) {
			return str.substring(0, MAX_LOG_TAG_LENGTH - 1);
        }
        return str;
    }

    /**
     * Don't use this when obfuscating class names!
     */
    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }

    public static void LOGD(final String tag, String message) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((BuildConfig.DEBUG && !doNotLogTags.contains(tag)) ||
                Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
        //Log.d(tag, message);
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message, cause);
        }
    }

    public static void LOGD_FULL(final String tag, String message) {
        if (BuildConfig.DEBUG || Log.isLoggable(tag, Log.DEBUG)) {
            for (int i = 0; i < message.length(); i += 1024) {
                if (i + 1024 < message.length())
                    LOGD(tag, message.substring(i, i + 1024));
                else
                    LOGD(tag, message.substring(i, message.length()));
            }
        }
    }

    public static void LOGV(final String tag, String message) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, message);
        }
    }

    public static void LOGV(final String tag, String message, Throwable cause) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, message, cause);
        }
    }

    public static void LOGI(final String tag, String message) {
        Log.i(tag, message);
    }

    public static void LOGI(final String tag, String message, Throwable cause) {
        Log.i(tag, message, cause);
    }

    public static void LOGW(final String tag, String message) {
        Log.w(tag, message);
    }

    public static void LOGW(final String tag, String message, Throwable cause) {
        Log.w(tag, message, cause);
    }

    public static void LOGE(final String tag, String message) {
        Log.e(tag, message);
    }

    public static void LOGE(final String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
    }

    private LogUtils() {
    }
}
