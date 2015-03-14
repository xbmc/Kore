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
package org.xbmc.kore.jsonrpc.type;

import com.fasterxml.jackson.databind.JsonNode;
import org.xbmc.kore.utils.JsonUtils;
import org.xbmc.kore.utils.LogUtils;

/**
 * Return types for methods in Files.*
 */
public class FilesType {
    private static final String TAG = LogUtils.makeLogTag(FilesType.class);
    /**
     * GetActivePlayers return type
     */
    public static final class PrepareDownloadReturnType {
        public final static String DETAILS = "details";
        public final static String MODE = "mode";
        public final static String PROTOCOL = "protocol";
        public final static String PATH = "path";

        // Returned info
//        public final String details;
        public final String mode;
        public final String protocol;
        public final String path;

        public PrepareDownloadReturnType(JsonNode node) {
            mode = JsonUtils.stringFromJsonNode(node, MODE);
            protocol = JsonUtils.stringFromJsonNode(node, PROTOCOL);

            JsonNode details =  node.get(DETAILS);
            path = JsonUtils.stringFromJsonNode(details, PATH);
        }
    }

    /**
     * DanhDroid
     */
    public static final class FileLocation {
        public final static String LABEL = "label";
        public final static String FILE_PATH = "file";
        public final static String FILE_TYPE = "file_type";
        public final static String DIRECTORY = "directory";

        public final String label;
        public final String path;
        public final boolean isDirectory;
        private boolean isRoot;

        public FileLocation(JsonNode node) {
            label = JsonUtils.stringFromJsonNode(node,LABEL);
            path = JsonUtils.stringFromJsonNode(node,FILE_PATH);
            if (node.has(FILE_TYPE)) {
                isDirectory = JsonUtils.stringFromJsonNode(node,FILE_TYPE).equalsIgnoreCase(DIRECTORY);
            }
            else {
                isDirectory = path.endsWith("/") || path.endsWith("\\");
            }
            LogUtils.LOGD(TAG, "FilesType.FilesType: label = " + label + ", path = " + path + ", directory = " + isDirectory);
        }

        public boolean isRootDir() { return this.isRoot; }
        public void setRootDir(boolean root) { this.isRoot = root; }

        public FileLocation(String label, String path, boolean isDir) {
            this.label = label;
            this.path = path;
            this.isDirectory = isDir;
            this.isRoot = false;
        }
    }
}
