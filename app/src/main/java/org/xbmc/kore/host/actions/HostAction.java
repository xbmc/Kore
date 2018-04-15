package org.xbmc.kore.host.actions;


import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.HostConnection;

public abstract class HostAction<T> {
    /**
     * @return the integer that uniquely identifies this action
     */
    public int getId() {
        return this.getClass().hashCode();
    }

    /**
     * Implement this to hold a sequence of ApiMethods that need to be executed on
     * the given hostConnection.
     * @see org.xbmc.kore.host.HostManager#withCurrentHost(HostAction, HostManager.OnActionListener)
     * @param hostConnection connection on which to execute the ApiMethod
     * @return result of the action
     * @throws ApiException holds any errors that occured while executing
     */
    abstract public T using(HostConnection hostConnection) throws ApiException;
}
