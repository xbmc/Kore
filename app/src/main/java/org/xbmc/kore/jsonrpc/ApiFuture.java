package org.xbmc.kore.jsonrpc;

import android.support.annotation.NonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Java future wrapping the result of a Kodi remote method call.
 * <p>
 * Instantiable only through {@link HostConnection#execute(ApiMethod)}.
 *
 * @param <T> The type of the result of the remote method call.
 */
class ApiFuture<T> implements Future<T> {
    private enum Status { WAITING, OK, ERROR, CANCELLED }
    private final Object lock = new Object();
    private Status status = Status.WAITING;
    private T ok;
    private Throwable error;

    static <T> Future<T> from(HostConnection host, ApiMethod<T> method) {
        final ApiFuture<T> future = new ApiFuture<>();
        host.execute(method, new ApiCallback<T>() {
            @Override
            public void onSuccess(T result) {
                synchronized (future.lock) {
                    future.ok = result;
                    future.status = Status.OK;
                    future.lock.notifyAll();
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                synchronized (future.lock) {
                    future.error = new ApiException(errorCode, description);
                    future.status = Status.ERROR;
                    future.lock.notifyAll();
                }
            }
        }, null);
        return future;
    }

    private ApiFuture() {}

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return get(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("impossible");
        }
    }

    @Override
    public T get(long timeout, @NonNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException
    {
        boolean timed = timeout > 0;
        long remaining = unit.toNanos(timeout);
        while (true) synchronized (lock) {
            switch (status) {
                case OK: return ok;
                case ERROR: throw new ExecutionException(error);
                case CANCELLED: throw new CancellationException();
                case WAITING:
                    if (timed && remaining <= 0) {
                        throw new TimeoutException();
                    }
                    if (!timed) {
                        lock.wait();
                    } else {
                        long start = System.nanoTime();
                        TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                        remaining -= System.nanoTime() - start;
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
            return true;
        }
    }

    @Override
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return status != Status.WAITING;
    }

}
