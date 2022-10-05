package org.xbmc.kore.host;

import android.os.Handler;

import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Superclass that facilitates the execution of composite actions, ie sequence of calls to
 * {@link org.xbmc.kore.jsonrpc.ApiMethod}, on Kodi, tp be done in a synchronous way, without using callbacks on each
 * call and globally handling errors.
 * The goal is to be able to call methods on Kodi using {@link HostConnection#execute(ApiMethod)}, getting back the
 * future and immediatelly await the result of its completion by calling its {@link Future#get()} method, handling any
 * errors in a global try/catch block.
 * This is not a major abstraction, just a helper class that allows for client code to be written similarly to a single
 * call to {@link HostConnection#execute(ApiMethod, ApiCallback, Handler)} but where the called method is composite.
 *
 * Subclasses should implement the abstract method {@link HostCompositeAction#execInBackground()} with the specific
 * logic that is meant to be executed, knowing that it will be executed in a background thread, thereby allowing
 * the use of {@link HostConnection#execute(ApiMethod)} and awaiting on the resulting {@link Future#get()}.
 *
 * Clients should call {@link HostCompositeAction#execute(HostConnection, ApiCallback, Handler)}, which creates a
 * background thread, calls runInBackground and sends the result to the given callback.
 */
public abstract class HostCompositeAction<T> {

    protected HostConnection hostConnection;

    /**
     * Composite action to be executed synchronously
     * @return result
     */
    public abstract T execInBackground()  throws ExecutionException, InterruptedException;

    /**
     * Calls {@link HostCompositeAction#execInBackground()} in a background thread, and posts the result through the
     * given callback on the specified handler
     *
     * @param hostConnection Host connection on which to call the method
     * @param callback Callbacks to post the response to
     * @param handler Handler to invoke callbacks on
     */
    public void execute(HostConnection hostConnection, ApiCallback<T> callback, Handler handler) {
        this.hostConnection = hostConnection;
        // Just a protection
        if (hostConnection == null) return;

        hostConnection.getExecutorService().execute(() -> {
            try {
                T result = execInBackground();
                handler.post(() -> callback.onSuccess(result));
            } catch (ExecutionException e) {
                handler.post(() -> callback.onError(ApiException.API_ERROR, e.getMessage()));
            } catch (InterruptedException e) {
                handler.post(() -> callback.onError(ApiException.API_WAITING_ON_RESULT_INTERRUPTED, e.getMessage()));
            }
        });

    }
}
