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

package org.xbmc.kore.testutils;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static String readFile(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);

        int size = is.available();

        byte[] buffer = new byte[size];

        is.read(buffer);

        is.close();

        return new String(buffer, "UTF-8");
    }

    @SuppressLint("NewApi")
    static String readFile(String filename) throws IOException {
        String pathToFile = getPathToThisFile() + "assets" + File.separator + filename;
        String fileContent;

        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToFile))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();

            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
            fileContent = stringBuilder.toString();
        }

        return fileContent;
    }

    /**
     * Returns the path of this file, there are some heavy assumptions here (tested on Windows)
     *
     * @return path to this file
     */
    private static String getPathToThisFile() {
        String className = FileUtils.class.getName();
        int i = className.lastIndexOf(".");
        if (i > -1) {
            className = className.substring(i + 1);
        }
        className = className + ".class";
        Object url = FileUtils.class.getResource(className);
        String path = url.toString();
        path = path.replace("file:/", "");
        path = path.replaceAll("build/.*", "build/");
        return path;
    }
}
