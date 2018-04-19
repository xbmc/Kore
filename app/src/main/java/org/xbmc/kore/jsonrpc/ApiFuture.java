package org.xbmc.kore.jsonrpc;

import android.support.annotation.NonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Java future implementation, with explicit methods to complete the Future
 * <p>
 * Don't forget that a call to {@link ApiFuture#get()} blocks the current
 * thread until it's unblocked by {@link ApiFuture#cancel(boolean)},
 * {@link ApiFuture#complete(Object)}  or {@link ApiFuture#completeExceptionally(Throwable)}
 *
 * @param <T> The type of the result returned by {@link ApiFuture#get()}
 */
class ApiFuture<T> implements Future<T> {
    private enum Status { WAITING, OK, ERROR, CANCELLED }
    private final Object lock = new Object();
    private Status status = Status.WAITING;
    private T ok;
    private Throwable error;

    ApiFuture() {}

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return get(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Request timed out. This should not happen when time out is disabled!");
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

    private boolean setResultAndNotify(Status status, T ok, Throwable error) {
        synchronized (lock) {
            if (this.status != Status.WAITING) {
                return false;
            }

            this.status = status;
            if (status == Status.OK) this.ok = ok;
            if (status == Status.ERROR) this.error = error;

            this.lock.notifyAll();
            return true;
        }
    }

    @Override
    public boolean cancel(boolean b) {
        return setResultAndNotify(Status.CANCELLED, null, null);
    }

    @Override
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return status != Status.WAITING;
    }

    /**
     * If not already completed, sets the value returned by get() to the given value.
     * @param value - the result value
     * @return true if this invocation caused this CompletableFuture to transition to a completed state, else false
     */
    public boolean complete(T value) {
        return setResultAndNotify(Status.OK, value, null);
    }

    /**
     * If not already completed, causes invocations of get() to throw the given exception.
     * @param ex = the exception
     * @return true if this invocation caused this CompletableFuture to transition to a completed state, else false
     */
    public boolean completeExceptionally(Throwable ex) {
        return setResultAndNotify(Status.ERROR, null, ex);
    }
}
