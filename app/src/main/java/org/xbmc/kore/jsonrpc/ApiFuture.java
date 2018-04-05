/**
 * Copyright 2017 XBMC Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.jsonrpc;

import android.support.annotation.NonNull;

import org.xbmc.kore.MainApp;
import org.xbmc.kore.R;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Java future wrapping the result of a Kodi remote method call.
 *
 * @param <T> The type of the result of the remote method call.
 */
public class ApiFuture<T> implements Future<T> {
    public enum Status { WAITING, OK, ERROR, CANCELLED }
    private Status status = Status.WAITING;
    private final Object lock = new Object();
    private T result;
    private ApiException error;
    private ApiMethod<T> apiMethod;

    private ApiFuture() {}

    public ApiFuture(ApiMethod<T> method) {
        apiMethod = method;
    }

    @Override
    public T get() throws InterruptedException {
        try {
            return get(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Request timed out. This should not happen when time out is disabled!");
        }
    }

    @Override
    public T get(long timeout, @NonNull TimeUnit unit)
            throws TimeoutException, InterruptedException
    {
        boolean timed = timeout > 0;
        long remaining = unit.toNanos(timeout);
        while (true) synchronized (lock) {
            switch (status) {
                case ERROR:
                case OK:
                case CANCELLED:
                    return this.result;
                case WAITING:
                    if (timed && remaining <= 0) {
                        throw new TimeoutException(MainApp.getContext().getString(R.string.api_method_timedout,
                                                                                   getApiMethod().getMethodName()));
                    }
                    try {
                        if (!timed) {
                            lock.wait();
                        } else {
                            long start = System.nanoTime();
                            TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                            remaining -= System.nanoTime() - start;
                        }
                    } catch (InterruptedException e) {
                        throw new InterruptedException(MainApp.getContext().getString(R.string.api_method_wait_interrupted,
                                                                                      getApiMethod().getMethodName()));
                    }
            }
        }
    }

    @Override
    public boolean cancel(boolean b) {
        if (status != Status.WAITING) {
            return false;
        }

        synchronized (lock) {
            status = Status.CANCELLED;
            lock.notifyAll();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return status != Status.WAITING;
    }

    public ApiMethod<T> getApiMethod() {
        return apiMethod;
    }

    /**
     * Finishes the Future after which {{@link #get()}} or {@link #get(long, TimeUnit)} can be used
     * to retrieve the result.
     * @param result the result that this future should hold when finished
     * @param error any error that occured. If null status of this Future will be set to Status.OK,
     *              otherwise it will be set to Status.ERROR
     */
    public void setResult(T result, ApiException error) {
        synchronized (lock) {
            this.result = result;
            this.status = error == null ? Status.OK : Status.ERROR;
            this.error = error;
            lock.notifyAll();
        }
    }

    public T getResult() {
        return result;
    }

    public Status getStatus() {
        return status;
    }

    public ApiException getError() {
        return error;
    }
}
