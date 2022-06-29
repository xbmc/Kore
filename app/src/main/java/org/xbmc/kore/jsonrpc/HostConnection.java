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

import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.notification.Application;
import org.xbmc.kore.jsonrpc.notification.Input;
import org.xbmc.kore.jsonrpc.notification.Player;
import org.xbmc.kore.jsonrpc.notification.Playlist;
import org.xbmc.kore.jsonrpc.notification.System;
import org.xbmc.kore.utils.LogUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Class responsible for communicating with the host.
 */
public class HostConnection {
	public static final String TAG = LogUtils.makeLogTag(HostConnection.class);

	/**
	 * Communicate via TCP
	 */
	public static final int PROTOCOL_TCP = 0;
	/**
	 * Communicate via HTTP
	 */
	public static final int PROTOCOL_HTTP = 1;

    /**
     * Interface that an observer must implement to be notified of player notifications
     */
    public interface PlayerNotificationsObserver {
        void onPropertyChanged(Player.OnPropertyChanged notification);
        void onPlay(Player.OnPlay notification);
        void onResume(Player.OnResume notification);
        void onPause(Player.OnPause notification);
        void onSpeedChanged(Player.OnSpeedChanged notification);
        void onSeek(Player.OnSeek notification);
        void onStop(Player.OnStop notification);
        void onAVStart(Player.OnAVStart notification);
        void onAVChange(Player.OnAVChange notification);
    }

    /**
     * Interface that an observer must implement to be notified of System notifications
     */
    public interface SystemNotificationsObserver {
        void onQuit(System.OnQuit notification);
        void onRestart(System.OnRestart notification);
        void onSleep(System.OnSleep notification);
    }

    /**
     * Interface that an observer must implement to be notified of Input notifications
     */
    public interface InputNotificationsObserver {
        void onInputRequested(Input.OnInputRequested notification);
    }

    public interface ApplicationNotificationsObserver {
        void onVolumeChanged(Application.OnVolumeChanged notification);
    }

    public interface PlaylistNotificationsObserver {
        void onPlaylistCleared(Playlist.OnClear notification);
        void onPlaylistItemAdded(Playlist.OnAdd notification);
        void onPlaylistItemRemoved(Playlist.OnRemove notification);
    }

    /**
	 * Host to connect too
	 */
	private final HostInfo hostInfo;

    /**
     * The protocol to use: {@link #PROTOCOL_HTTP} or {@link #PROTOCOL_TCP}
     * This is initially obtained from the {@link HostInfo}, but can be later changed through
     * {@link #setProtocol(int)}
     */
    private int protocol;

    private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Socket used to communicate through TCP
	 */
	private Socket socket = null;

    /**
     * Listener thread that will be listening on the TCP socket
     */
    private Thread tcpListenerThread = null;

	/**
	 * {@link HashMap} that will hold the {@link MethodCallInfo} with the information
	 * necessary to respond to clients (TCP only)
	 */
	private final HashMap<String, MethodCallInfo<?>> clientCallbacks = new HashMap<>();

    /**
     * The observers that will be notified of player notifications
     */
    private final HashMap<PlayerNotificationsObserver, Handler> playerNotificationsObservers =
            new HashMap<>();

    /**
     * The observers that will be notified of system notifications
     */
    private final HashMap<SystemNotificationsObserver, Handler> systemNotificationsObservers =
            new HashMap<>();

    /**
     * The observers that will be notified of input notifications
     */
    private final HashMap<InputNotificationsObserver, Handler> inputNotificationsObservers =
            new HashMap<>();

    /**
     * The observers that will be notified of application notifications
     */
    private final HashMap<ApplicationNotificationsObserver, Handler> applicationNotificationsObservers =
            new HashMap<>();

    /**
     * The observers that will be notified of playlist notifications
     */
    private final HashMap<PlaylistNotificationsObserver, Handler> playlistNotificationsObservers =
            new HashMap<>();

    private final ExecutorService executorService;

    private final int connectTimeout;

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms

    public static final int TCP_READ_TIMEOUT = 30000; // ms

    private static final int CALLABLE_TIMEOUT = 30000; // ms

    /**
     * OkHttpClient. Make sure it is initialized, by calling {@link #getOkHttpClient()}
     */
    private OkHttpClient httpClient = null;
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * Flag to indicate that the tcp response have to be ignored.
     */
    private boolean ignoreTcpResponse = false;

    public void setIgnoreTcpResponse(boolean ignoreTcpResponse) {
        this.ignoreTcpResponse = ignoreTcpResponse;
    }

    /**
     * Creates a new host connection
     * @param hostInfo Host info object
     */
    public HostConnection(final HostInfo hostInfo) {
        this(hostInfo, DEFAULT_CONNECT_TIMEOUT);
	}

    /**
     * Creates a new host connection
     * @param hostInfo Host info object
     * @param connectTimeout Connection timeout in ms
     */
    public HostConnection(final HostInfo hostInfo, int connectTimeout) {
        this.hostInfo = hostInfo;
        // Start with the default host protocol
        this.protocol = hostInfo.getProtocol();
        // Create a single threaded executor
        this.executorService = Executors.newFixedThreadPool(5);
        // Set timeout
        this.connectTimeout = connectTimeout;
    }

    /**
     * Returns this connection protocol
     * @return {@link #PROTOCOL_HTTP} or {@link #PROTOCOL_TCP}
     */
    public int getProtocol() {
        return protocol;
    }

    /**
     * Overrides the protocol for this connection
     * @param protocol {@link #PROTOCOL_HTTP} or {@link #PROTOCOL_TCP}
     */
    public synchronized void setProtocol(int protocol) {
        if (!isValidProtocol(protocol)) {
            throw new IllegalArgumentException("Invalid protocol specified.");
        }
        this.protocol = protocol;
    }

    public static boolean isValidProtocol(int protocol) {
        return ((protocol == PROTOCOL_TCP) || (protocol == PROTOCOL_HTTP));
    }

    /**
     * Returns this connection {@link HostInfo}
     * @return This connection {@link HostInfo}
     */
    public HostInfo getHostInfo() {
        return hostInfo;
    }

    /**
     * Returns this default connection timeout
     * @return Connection timeout
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Registers an observer for player notifications
     * @param observer The {@link PlayerNotificationsObserver}
     */
    public void registerPlayerNotificationsObserver(PlayerNotificationsObserver observer,
                                                    Handler handler) {
        playerNotificationsObservers.put(observer, handler);
    }

    /**
     * Unregisters and observer from the player notifications
     * @param observer The {@link PlayerNotificationsObserver} to unregister
     */
    public void unregisterPlayerNotificationsObserver(PlayerNotificationsObserver observer) {
        playerNotificationsObservers.remove(observer);
    }

    /**
     * Registers an observer for system notifications
     * @param observer The {@link SystemNotificationsObserver}
     */
    public void registerSystemNotificationsObserver(SystemNotificationsObserver observer,
                                                    Handler handler) {
        systemNotificationsObservers.put(observer, handler);
    }

    /**
     * Unregisters and observer from the system notifications
     * @param observer The {@link SystemNotificationsObserver}
     */
    public void unregisterSystemNotificationsObserver(SystemNotificationsObserver observer) {
        systemNotificationsObservers.remove(observer);
    }

    /**
     * Registers an observer for input notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void registerInputNotificationsObserver(InputNotificationsObserver observer,
                                                   Handler handler) {
        inputNotificationsObservers.put(observer, handler);
    }

    /**
     * Unregisters and observer from the input notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void unregisterInputNotificationsObserver(InputNotificationsObserver observer) {
        inputNotificationsObservers.remove(observer);
    }

    /**
     * Registers an observer for application notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void registerApplicationNotificationsObserver(ApplicationNotificationsObserver observer,
                                                   Handler handler) {
        applicationNotificationsObservers.put(observer, handler);
    }

    /**
     * Unregisters and observer from the application notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void unregisterApplicationNotificationsObserver(ApplicationNotificationsObserver observer) {
        applicationNotificationsObservers.remove(observer);
    }

    /**
     * Registers an observer for playlist notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void registerPlaylistNotificationsObserver(PlaylistNotificationsObserver observer,
                                                         Handler handler) {
        playlistNotificationsObservers.put(observer, handler);
    }

    /**
     * Unregisters and observer from the playlist notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void unregisterPlaylistNotificationsObserver(PlaylistNotificationsObserver observer) {
        playlistNotificationsObservers.remove(observer);
    }

    /**
	 * Calls the given method on the server
	 * This call is always asynchronous. The results will be posted, through the
	 * {@link ApiCallback callback} parameter, on the specified {@link android.os.Handler}.
     * <BR/>
     * If you need to update the callback and handler (e.g. due to a device configuration change)
     * use {@link #updateClientCallback(int, ApiCallback, Handler)}
     * @param method Method object that represents the methood too call
	 * @param callback {@link ApiCallback} to post the response to
	 * @param handler {@link Handler} to invoke callbacks on. When null, the
	 *                callbacks are invoked on the same thread as the request.
	 *                You cannot do UI manipulation in the callbacks when this is null.
	 * @param <T> Method return type
	 */
	public <T> void execute(final ApiMethod<T> method, final ApiCallback<T> callback,
							final Handler handler) {
		LogUtils.LOGD(TAG, "Starting method execute. Method: " + method.getMethodName() +
			" on host: " + hostInfo.getJsonRpcHttpEndpoint());

        if (protocol == PROTOCOL_TCP) {
            // Do not call this from the runnable below as it may cause a race condition
            // with {@link #updateClientCallback(int, ApiCallback, Handler)}
            //
            // Save this method/callback for any later response
            addClientCallback(method, callback, handler);
        }

		// Launch background thread
        Runnable command = () -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            if (protocol == PROTOCOL_HTTP) {
                executeThroughOkHttp(method, callback, handler);
            } else {
                executeThroughTcp(method);
            }
        };

        executorService.execute(command);
	}

    /**
     * Executes the remote method in the background and returns a future that may be
     * awaited on any thread.
     * <p>
     * Not a good idea to await on the UI thread although you can. The app will become
     * unresponsive until the request is completed if you do. Android can't know that
     * you're effectively doing a network request on the main thread so it won't stop you.
     * <p>
     * This is meant to be used in a background thread for doing requests that depend
     * on the result of another. Nested callbacks make it hard to follow the logic,
     * tend to make you repeat error handling at every level, and also slower because
     * of constant switching between the worker and UI threads.
     * <p>
     * If you don't care about the result and just want to fire a request, you could
     * call this but not call {@link Future#get()} on the result. It's safe to do
     * in the UI thread but you wouldn't know if an error happened or not.
     *
     * @param method The remote method to invoke
     * @param <T> The type of the return value of the method
     * @return the future result of the method call. API errors will be wrapped in
     * an {@link ExecutionException} like regular futures.
     */
    public <T> Future<T> execute(ApiMethod<T> method) {
        final ApiFuture<T> future = new ApiFuture<>();
        execute(method, new ApiCallback<T>() {
            @Override
            public void onSuccess(T result) {
                future.complete(result);
            }

            @Override
            public void onError(int errorCode, String description) {
                future.completeExceptionally(new ApiException(errorCode, description));
            }
        }, null);
        return future;
    }
    
    /**
     * Executes the {@link Callable} and waits for it to finish on a background thread. The
     * result is returned using the {@link ApiCallback} and handler
     * @param callable executed using an {@link ExecutorService}
     * @param apiCallback used to return the result of the callable
     * @param handler used to execute the {@link ApiCallback} methods
     * @param <T> The callable return type
     */
    public <T> void execute(final Callable<T> callable, final ApiCallback<T> apiCallback, final Handler handler) {
        final Future<T> future = executorService.submit(callable);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    T result = future.get(CALLABLE_TIMEOUT, TimeUnit.MILLISECONDS);
                    handleSuccess(result);
                } catch (ExecutionException e) {
                    handleError(ApiException.API_ERROR, e.getMessage());
                } catch (InterruptedException e) {
                    handleError(ApiException.API_WAITING_ON_RESULT_INTERRUPTED, e.getMessage());
                } catch (TimeoutException e) {
                    handleError(ApiException.API_WAITING_ON_RESULT_TIMEDOUT, e.getMessage());
                }
            }

            private void handleSuccess(final T result) {
                handler.post(() -> apiCallback.onSuccess(result));
            }

            private void handleError(final int errorCode, final String message) {
                handler.post(() -> apiCallback.onError(errorCode, message));
            }
        });
    }

    /**
     * Updates the client callback for the given {@link ApiMethod} if it is still pending.
     * This can be used when the activity or fragment has been destroyed and recreated and
     * you are still interested in the result of any pending {@link ApiMethod}
     * @param methodId for which a new callback needs to be attached
     * @param callback new callback that needs to be called for the new activity or fragment
     * @param handler used to execute the callback on the UI thread
     * @param <T> result type
     * @return true if the {@link ApiMethod} was still pending, false otherwise.
     */
    @SuppressWarnings("unchecked")
    public <T> boolean updateClientCallback(final int methodId, final ApiCallback<T> callback,
                                            final Handler handler) {

        if (getProtocol() == PROTOCOL_HTTP)
            return false;

        synchronized (clientCallbacks) {
            String id = String.valueOf(methodId);
            MethodCallInfo<?> methodCallInfo = clientCallbacks.get(id);
            if (methodCallInfo != null) {
                clientCallbacks.put(id, new MethodCallInfo<>((ApiMethod<T>) methodCallInfo.method, callback, handler));
                return true;
            }
            return  false;
        }
    }

    /**
     * Stores the method and callback to handle asynchronous responses.
     * Note this is only needed for requests over TCP.
     * @param method Method
     * @param callback Callback
     * @param handler Handler
     * @param <T> Method/Callback type
     */
    private <T> void addClientCallback(final ApiMethod<T> method, final ApiCallback<T> callback,
                                       final Handler handler) {

        if (getProtocol() == PROTOCOL_HTTP)
            return;

        String methodId = String.valueOf(method.getId());

        synchronized (clientCallbacks) {
            if (clientCallbacks.containsKey(methodId)) {
                if ((handler != null) && (callback != null)) {
                    handler.post(() -> callback.onError(ApiException.API_METHOD_WITH_SAME_ID_ALREADY_EXECUTING,
                                                "A method with the same Id is already executing"));
                }
                return;
            }
            clientCallbacks.put(methodId, new MethodCallInfo<>(method, callback, handler));
        }
    }

    /**
     * Sends the JSON RPC request through HTTP (using OkHttp library)
     */
    private <T> void executeThroughOkHttp(final ApiMethod<T> method, final ApiCallback<T> callback,
                                          final Handler handler) {
        OkHttpClient client = getOkHttpClient();
        String jsonRequest = method.toJsonString();
        LogUtils.LOGD(TAG, "Sending request via HTTP: " + jsonRequest);

        try {
            Request request = new Request.Builder()
                    .url(hostInfo.getJsonRpcHttpEndpoint())
                    .post(RequestBody.create(jsonRequest, MEDIA_TYPE_JSON))
                    .build();
            Response response = sendOkHttpRequest(client, request);
            final T result = method.resultFromJson(parseJsonResponse(handleOkHttpResponse(response)));

            if (callback != null) {
                postOrRunNow(handler, () -> callback.onSuccess(result));
            }
        } catch (final ApiException e) {
            // Got an error, call error handler
            if (callback != null) {
                postOrRunNow(handler, () -> callback.onError(e.getCode(), e.getMessage()));
            }
        }
    }

    /**
     * Initializes this class OkHttpClient
     */
    public synchronized OkHttpClient getOkHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .authenticator(getOkHttpAuthenticator())
                    .build();
        }
        return httpClient;
    }

    public Authenticator getOkHttpAuthenticator() {
        return (route, response) -> {
            if (TextUtils.isEmpty(hostInfo.getUsername()) ||
                (response.request().header("Authorization") != null)) {
                return null; // Give up, we've already attempted to authenticate.
            }

            String credential = Credentials.basic(hostInfo.getUsername(), hostInfo.getPassword());
            return response.request().newBuilder()
                           .header("Authorization", credential)
                           .build();
        };
    }

    // Hack to circumvent a Protocol Exception that occurs when the server returns bogus Status Line
    // http://forum.kodi.tv/showthread.php?tid=224288
    private OkHttpClient getNewOkHttpClientNoKeepAlive() {
        java.lang.System.setProperty("http.keepAlive", "false");
        httpClient = null;
        return getOkHttpClient();
    }

    /**
     * Send an OkHttp POST request
     * @param request Request to send
     * @throws ApiException {@link ApiException} if request can't be sent
     */
    private Response sendOkHttpRequest(final OkHttpClient client, final Request request) throws ApiException {
        try {
            return client.newCall(request).execute();
        } catch (ProtocolException e) {
            LogUtils.LOGW(TAG, "Got a Protocol Exception when trying to send OkHttp request. " +
                            "Trying again without connection pooling to try to circunvent this", e);
            // Hack to circumvent a Protocol Exception that occurs when the server returns bogus Status Line
            // http://forum.kodi.tv/showthread.php?tid=224288
            httpClient = getNewOkHttpClientNoKeepAlive();
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
        } catch (IOException e) {
            LogUtils.LOGW(TAG, "Failed to send OkHttp request.", e);
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
        } catch (RuntimeException e) {
            // Seems like OkHttp throws a RuntimeException when it gets a malformed URL
            LogUtils.LOGW(TAG, "Got a Runtime exception when sending OkHttp request. Probably a malformed URL.", e);
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
        }
    }

    /**
     * Reads the response from the server
     * @param response Response from OkHttp
     * @return Response body string
     * @throws ApiException {@link ApiException} if response can't be read/processed
     */
    private String handleOkHttpResponse(Response response) throws ApiException {
        try {
//			LogUtils.LOGD(TAG, "Reading HTTP response.");
            int responseCode = response.code();

            switch (responseCode) {
                case 200:
                    ResponseBody body = response.body();
                    if (body != null) {
                        // All ok, read response
                        String res = body.string();
                        body.close();
                        LogUtils.LOGD(TAG, "OkHTTP response: " + res);
                        return res;
                    } else {
                        LogUtils.LOGD(TAG, "OkHTTP response body is null: " + response);
                        throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNKNOWN,
                                               "Server returned response code: " + response);
                    }
                case 401:
                    LogUtils.LOGD(TAG, "OkHTTP response read error. Got a 401: " + response);
                    throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNAUTHORIZED,
                            "Server returned response code: " + response);
                case 404:
                    LogUtils.LOGD(TAG, "OkHTTP response read error. Got a 404: " + response);
                    throw new ApiException(ApiException.HTTP_RESPONSE_CODE_NOT_FOUND,
                            "Server returned response code: " + response);
                default:
                    LogUtils.LOGD(TAG, "OkHTTP response read error. Got: " + response);
                    throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNKNOWN,
                            "Server returned response code: " + response);
            }
        } catch (IOException e) {
            LogUtils.LOGW(TAG, "Failed to read OkHTTP response.", e);
            throw new ApiException(ApiException.IO_EXCEPTION_WHILE_READING_RESPONSE, e);
        }
    }

    /**
	 * Parses the JSON response from the server.
	 * If it is a valid result returns the JSON {@link com.fasterxml.jackson.databind.node.ObjectNode} that represents it.
	 * If it is an error (contains the error tag), returns an {@link ApiException} with the info.
	 * @param response JSON response
	 * @return {@link com.fasterxml.jackson.databind.node.ObjectNode} constructed
	 * @throws ApiException Exception trown if we can't parse the response
	 */
	private ObjectNode parseJsonResponse(String response) throws ApiException {
//		LogUtils.LOGD(TAG, "Parsing JSON response");
		try {
			ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(response);

			if (jsonResponse.has(ApiMethod.ERROR_NODE)) {
				throw new ApiException(ApiException.API_ERROR, jsonResponse);
			}

			if (!jsonResponse.has(ApiMethod.RESULT_NODE)) {
				// Something strange is going on
				throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST,
					"Result doesn't contain a result node.");
			}

			return jsonResponse;
		} catch (IOException e) {
			LogUtils.LOGW(TAG, "Got an exception while parsing JSON response.", e);
			throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
		}
    }

	/**
	 * Sends the JSON RPC request through TCP
	 * Keeps a background thread running, listening on a socket
	 */
	private <T> void executeThroughTcp(final ApiMethod<T> method) {
        String methodId = String.valueOf(method.getId());
        try {
            synchronized (this) {
                if (socket == null) {
                    // Open connection to the server and setup reader thread
                    socket = openTcpConnection(hostInfo);
                    startListenerThread(socket);
                }

                // Write request
                sendTcpRequest(socket, method.toJsonString());
            }
		} catch (final ApiException e) {
			callErrorCallback(methodId, e);
		}
	}

	/**
	 * Auxiliary method to open the TCP {@link Socket}.
	 * This method calls connect() so that any errors are cathced
	 * @param hostInfo Host info
	 * @return Connection set up
	 * @throws ApiException Exception if open is unsucessful
	 */
	private Socket openTcpConnection(HostInfo hostInfo) throws ApiException {
		try {
			LogUtils.LOGD(TAG, "Opening TCP connection on host: " + hostInfo.getAddress());

			Socket socket = new Socket();
			final InetSocketAddress address = new InetSocketAddress(hostInfo.getAddress(), hostInfo.getTcpPort());
            // We're setting a read timeout on the socket, so no need to explicitly close it
			socket.setSoTimeout(TCP_READ_TIMEOUT);
			socket.connect(address, connectTimeout);

			return socket;
		} catch (IOException e) {
			LogUtils.LOGW(TAG, "Failed to open TCP connection to host: " + hostInfo.getAddress());
			throw new ApiException(ApiException.IO_EXCEPTION_WHILE_CONNECTING, e);
		}
	}

	/**
	 * Send a TCP request
	 * @param socket Socket to write to
	 * @param request Request to send
	 * @throws ApiException Exception if can't send
	 */
	private void sendTcpRequest(Socket socket, String request) throws ApiException {
		try {
			LogUtils.LOGD(TAG, "Sending request via TCP: " + request);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			writer.write(request);
			writer.flush();
		} catch (Exception e) {
			LogUtils.LOGW(TAG, "Failed to send TCP request.", e);
            disconnect();
			throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
		}
	}

	private void startListenerThread(final Socket socket) {
        tcpListenerThread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                LogUtils.LOGD(TAG, "Starting TCP socket listener thread from thread: " + Thread.currentThread().getName());
                // We're going to read from the socket. This will be a blocking call and
                // it will keep on going until disconnect() is called on this object.
                // Note: Mind the objects used here: we use createParser because it doesn't
                // close the socket after ObjectMapper.readTree.
                JsonParser jsonParser = objectMapper.getFactory().createParser(socket.getInputStream());
                ObjectNode jsonResponse;
                while ((jsonResponse = objectMapper.readTree(jsonParser)) != null) {
                    LogUtils.LOGD(TAG, "Read from socket: " + jsonResponse);
//                        LogUtils.LOGD_FULL(TAG, "Read from socket: " + jsonResponse.toString());
                    handleTcpResponse(jsonResponse);
                }
            } catch (JsonProcessingException e) {
                LogUtils.LOGW(TAG, "Got an exception while parsing JSON response.", e);
                callErrorCallback(null, new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e));
            } catch (IOException e) {
                LogUtils.LOGW(TAG, "Error reading from socket.", e);
                callErrorCallback(null, new ApiException(ApiException.IO_EXCEPTION_WHILE_READING_RESPONSE, e));
            } finally {
                disconnect();
            }
        });
	    tcpListenerThread.start();
	}

    private boolean shouldIgnoreTcpResponse(ObjectNode jsonResponse) {
        boolean ignore = false;
        if (jsonResponse.has(ApiMethod.ID_NODE) && ignoreTcpResponse) {
            ignoreTcpResponse = false;
            ignore = true;
        }
        //LogUtils.LOGD(TAG, "ignore tcp response - " + ignore);
        return ignore;
    }

	private <T> void handleTcpResponse(ObjectNode jsonResponse) {
        if (shouldIgnoreTcpResponse(jsonResponse))
            return;

		if (!jsonResponse.has(ApiMethod.ID_NODE)) {
            // It's a notification, notify observers
            String notificationName = jsonResponse.get(ApiNotification.METHOD_NODE).asText();
            ObjectNode params = (ObjectNode)jsonResponse.get(ApiNotification.PARAMS_NODE);

            switch (notificationName) {
                case Player.OnPause.NOTIFICATION_NAME: {
                    final Player.OnPause apiNotification = new Player.OnPause(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onPause(apiNotification));
                    }
                    break;
                }
                case Player.OnPlay.NOTIFICATION_NAME: {
                    final Player.OnPlay apiNotification = new Player.OnPlay(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onPlay(apiNotification));
                    }
                    break;
                }
                case Player.OnResume.NOTIFICATION_NAME: {
                    final Player.OnResume apiNotification = new Player.OnResume(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onResume(apiNotification));
                    }
                    break;
                }
                case Player.OnSeek.NOTIFICATION_NAME: {
                    final Player.OnSeek apiNotification = new Player.OnSeek(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onSeek(apiNotification));
                    }
                    break;
                }
                case Player.OnSpeedChanged.NOTIFICATION_NAME: {
                    final Player.OnSpeedChanged apiNotification = new Player.OnSpeedChanged(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onSpeedChanged(apiNotification));
                    }
                    break;
                }
                case Player.OnStop.NOTIFICATION_NAME: {
                    final Player.OnStop apiNotification = new Player.OnStop(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onStop(apiNotification));
                    }
                    break;
                }
                case Player.OnAVStart.NOTIFICATION_NAME: {
                    final Player.OnAVStart apiNotification = new Player.OnAVStart(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onAVStart(apiNotification));
                    }
                    break;
                }
                case Player.OnAVChange.NOTIFICATION_NAME: {
                    final Player.OnAVChange apiNotification = new Player.OnAVChange(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onAVChange(apiNotification));
                    }
                    break;
                }
                case Player.OnPropertyChanged.NOTIFICATION_NAME: {
                    final Player.OnPropertyChanged apiNotification = new Player.OnPropertyChanged(params);
                    for (final PlayerNotificationsObserver observer : playerNotificationsObservers.keySet()) {
                        Handler handler = playerNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onPropertyChanged(apiNotification));
                    }
                    break;
                }
                case System.OnQuit.NOTIFICATION_NAME: {
                    final System.OnQuit apiNotification = new System.OnQuit(params);
                    for (final SystemNotificationsObserver observer : systemNotificationsObservers.keySet()) {
                        Handler handler = systemNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onQuit(apiNotification));
                    }
                    break;
                }
                case System.OnRestart.NOTIFICATION_NAME: {
                    final System.OnRestart apiNotification = new System.OnRestart(params);
                    for (final SystemNotificationsObserver observer : systemNotificationsObservers.keySet()) {
                        Handler handler = systemNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onRestart(apiNotification));
                    }
                    break;
                }
                case System.OnSleep.NOTIFICATION_NAME: {
                    final System.OnSleep apiNotification = new System.OnSleep(params);
                    for (final SystemNotificationsObserver observer : systemNotificationsObservers.keySet()) {
                        Handler handler = systemNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onSleep(apiNotification));
                    }
                    break;
                }
                case Input.OnInputRequested.NOTIFICATION_NAME: {
                    final Input.OnInputRequested apiNotification = new Input.OnInputRequested(params);
                    for (final InputNotificationsObserver observer : inputNotificationsObservers.keySet()) {
                        Handler handler = inputNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onInputRequested(apiNotification));
                    }
                    break;
                }
                case Application.OnVolumeChanged.NOTIFICATION_NAME: {
                    final Application.OnVolumeChanged apiNotification = new Application.OnVolumeChanged(params);
                    for (final ApplicationNotificationsObserver observer : applicationNotificationsObservers.keySet()) {
                        Handler handler = applicationNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onVolumeChanged(apiNotification));
                    }
                    break;
                }
                case Playlist.OnClear.NOTIFICATION_NAME: {
                    final Playlist.OnClear apiNotification =
                            new Playlist.OnClear(params);
                    for (final PlaylistNotificationsObserver observer :
                            playlistNotificationsObservers.keySet()) {
                        Handler handler = playlistNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onPlaylistCleared(apiNotification));
                    }
                    break;
                }
                case Playlist.OnAdd.NOTIFICATION_NAME: {
                    final Playlist.OnAdd apiNotification =
                            new Playlist.OnAdd(params);
                    for (final PlaylistNotificationsObserver observer :
                            playlistNotificationsObservers.keySet()) {
                        Handler handler = playlistNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onPlaylistItemAdded(apiNotification));
                    }
                    break;
                }
                case Playlist.OnRemove.NOTIFICATION_NAME: {
                    final Playlist.OnRemove apiNotification =
                            new Playlist.OnRemove(params);
                    for (final PlaylistNotificationsObserver observer :
                            playlistNotificationsObservers.keySet()) {
                        Handler handler = playlistNotificationsObservers.get(observer);
                        postOrRunNow(handler, () -> observer.onPlaylistItemRemoved(apiNotification));
                    }
                    break;
                }
            }
            LogUtils.LOGD(TAG, "Got a notification: " + jsonResponse.get("method").textValue());
		} else {
			String methodId = jsonResponse.get(ApiMethod.ID_NODE).asText();

			if (jsonResponse.has(ApiMethod.ERROR_NODE)) {
				// Error response
				callErrorCallback(methodId, new ApiException(ApiException.API_ERROR, jsonResponse));
			} else {
				// Success response
				final MethodCallInfo<?> methodCallInfo = clientCallbacks.get(methodId);

                if (methodCallInfo != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        final T result = (T) methodCallInfo.method.resultFromJson(jsonResponse);
                        @SuppressWarnings("unchecked")
                        final ApiCallback<T> callback = (ApiCallback<T>) methodCallInfo.callback;

                        if (callback != null) {
                            postOrRunNow(methodCallInfo.handler, () -> callback.onSuccess(result));
                        }

                        // We've replied, remove the client from the list
                        synchronized (clientCallbacks) {
                            clientCallbacks.remove(methodId);
                        }
                    } catch (ApiException e) {
                        callErrorCallback(methodId, e);
                    }
                }
			}
		}
	}

    private <T> void callErrorCallback(String methodId, final ApiException error) {
        synchronized (clientCallbacks) {
            if (methodId != null) {
                // Send error back to client
                final MethodCallInfo<?> methodCallInfo = clientCallbacks.get(methodId);
                if (methodCallInfo != null) {
                    @SuppressWarnings("unchecked")
                    final ApiCallback<T> callback = (ApiCallback<T>) methodCallInfo.callback;

                    if (callback != null) {
                        postOrRunNow(methodCallInfo.handler, () -> callback.onError(error.getCode(), error.getMessage()));
                    }
                }
                clientCallbacks.remove(methodId);
            } else {
                // Notify all pending clients, it might be an error for them
                for (String id : clientCallbacks.keySet()) {
                    final MethodCallInfo<?> methodCallInfo = clientCallbacks.get(id);
                    if (methodCallInfo == null) continue;
                    @SuppressWarnings("unchecked")
                    final ApiCallback<T> callback = (ApiCallback<T>)methodCallInfo.callback;

                    if (callback != null) {
                        postOrRunNow(methodCallInfo.handler, () -> callback.onError(error.getCode(), error.getMessage()));
                    }
                }

                clientCallbacks.clear();
            }
        }
    }

	/**
	 * Cleans up used resources.
	 * This method should always be called if the protocol used is TCP, so we can shutdown gracefully
	 */
    public synchronized void disconnect() {
		if (protocol == PROTOCOL_HTTP)
			return;

		try {
			if (socket != null) {
				// Remove pending calls
                clientCallbacks.clear();
				if (!socket.isClosed()) {
					socket.close();
				}
			}
		} catch (IOException e) {
			LogUtils.LOGE(TAG, "Error while closing socket", e);
		} finally {
			socket = null;
		}
	}

    private static void postOrRunNow(Handler handler, Runnable r) {
        if (handler != null) {
            handler.post(r);
        } else {
            r.run();
        }
    }

	/**
	 * Helper class to aggregate a method, callback and handler
	 * @param <T>
	 */
	private static class MethodCallInfo<T> {
		public final ApiMethod<T> method;
		public final ApiCallback<T> callback;
		public final Handler handler;

		public MethodCallInfo(ApiMethod<T> method, ApiCallback<T> callback, Handler handler) {
			this.method = method;
			this.callback = callback;
			this.handler = handler;
		}
	}
}
