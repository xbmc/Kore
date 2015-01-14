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
package com.syncedsynapse.kore2.jsonrpc.method;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syncedsynapse.kore2.jsonrpc.ApiException;
import com.syncedsynapse.kore2.jsonrpc.ApiMethod;
import com.syncedsynapse.kore2.jsonrpc.type.FilesType;

/**
 * All JSON RPC methods in Files.*
 */
public class Files {

    /**
     * Prepare Download
     * Provides a way to download a given file (e.g. providing an URL to the real file location)
     */
    public static final class PrepareDownload extends ApiMethod<FilesType.PrepareDownloadReturnType> {
        public final static String METHOD_NAME = "Files.PrepareDownload";

        /**
         * Provides a way to download a given file (e.g. providing an URL to the real file location)
         */
        public PrepareDownload(String path) {
            super();
            addParameterToRequest("path", path);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public FilesType.PrepareDownloadReturnType resultFromJson(ObjectNode jsonObject) throws ApiException {
            return  new FilesType.PrepareDownloadReturnType(jsonObject.get(RESULT_NODE));
        }
    }
}
