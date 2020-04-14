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
package org.xbmc.kore.jsonrpc;

/**
 * Callback from a JSON RPC method execution.
 * When executing a method in JSON RPC, through
 * {@link HostConnection#execute(ApiMethod, ApiCallback, android.os.Handler)},
 * an object implementing this interface should be provided, to call after receiving the response
 * from XBMC. Depending on the response {@link ApiCallback#onSuccess(Object)} or {@link
 * ApiCallback#onError(int, String)} will be called.
 * * @param <T> Result type
 */
public interface ApiCallback<T> {

    /**
     * Callback that will be called after a sucessfull reponse from the XBMC JSON RPC method
     * @param result The result that was obtained and sucessfully parsed from XBMC
     */
	void onSuccess(T result);

    /**
     * Calllback that will be called when an error occurs executing the method on XBMC.
     * This can be a general error (like a connection error), or an error reported by XBMC (like
     * an incorrect call)
     * @param errorCode Error code. Check {@link ApiException} for detailed error codes
     * @param description Error description
     */
	void onError(int errorCode, String description);
}
