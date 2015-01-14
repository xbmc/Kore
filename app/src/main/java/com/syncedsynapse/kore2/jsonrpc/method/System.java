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

/**
 * All JSON RPC methods in System.*
 */
public class System {

    /**
     * Shuts the system running XBMC down
     */
    public static final class Shutdown extends ApiMethod<String> {
        public final static String METHOD_NAME = "System.Shutdown";

        /**
         * Shuts the system running XBMC down
         */
        public Shutdown() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

    /**
     * Suspends the system running XBMC
     */
    public static final class Suspend extends ApiMethod<String> {
        public final static String METHOD_NAME = "System.Suspend";

        /**
         * Suspends the system running XBMC
         */
        public Suspend() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }

}
