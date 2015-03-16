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
package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.type.FilesType;
import org.xbmc.kore.jsonrpc.type.SortType;

import java.util.ArrayList;
import java.util.List;

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
    /**
     * DanhDroid
     */
    /**
     * Files.GetSources command
     */
    public static final class GetSources extends ApiMethod<List<FilesType.FileLocation>> {
        public final static String METHOD_NAME = "Files.GetSources";
        public final static String LIST_NODE = "sources";

        /**
         * Provides a way to download a given file (e.g. providing an URL to the real file location)
         */
        public GetSources(String mediaType) {
            super();
            addParameterToRequest("media", mediaType);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public List<FilesType.FileLocation> resultFromJson(ObjectNode jsonObject) throws ApiException {
            /*
            response from the File.GetSources command
            {"id":"1",
            "jsonrpc":"2.0",
            "result":{
                "limits":{
                    "end":2,
                    "start":0,
                    "total":2
                },
                "sources":[
                    {"file":"addons://sources/audio/","label":"Music Add-ons"},
                    {"file":"/home/osmc/ftp/files/Media/Musics/","label":"Musics"}
                ]
            }
            }
            */
            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                    (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<FilesType.FileLocation>(0);
            }
            ArrayList<FilesType.FileLocation> result = new ArrayList<FilesType.FileLocation>(items.size());

            for (JsonNode item : items) {
                result.add(new FilesType.FileLocation(item));
            }
            return result;
        }
    }

    /**
     * Files.GetDirectory command
     */
    public static final class GetDirectory extends ApiMethod<List<FilesType.FileLocation>> {
        public final static String METHOD_NAME = "Files.GetDirectory";
        public final static String SORT_NODE = "sort";
        public final static String LIST_NODE = "files";

        /**
         * Provides a way to download a given file (e.g. providing an URL to the real file location)
         */
        public GetDirectory(String path, SortType.Sort sort_params) {
            super();
            addParameterToRequest("media", LIST_NODE);
            addParameterToRequest("directory", path);
            addParameterToRequest(SORT_NODE, sort_params.toJsonNode());
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public List<FilesType.FileLocation> resultFromJson(ObjectNode jsonObject) throws ApiException {

            JsonNode resultNode = jsonObject.get(RESULT_NODE);
            ArrayNode items = resultNode.has(LIST_NODE) ?
                    (ArrayNode)resultNode.get(LIST_NODE) : null;
            if (items == null) {
                return new ArrayList<FilesType.FileLocation>(0);
            }
            ArrayList<FilesType.FileLocation> result = new ArrayList<FilesType.FileLocation>(items.size());

            for (JsonNode item : items) {
                result.add(new FilesType.FileLocation(item));
            }
            return result;
        }
    }
}
