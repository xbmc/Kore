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
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.notification.Application;
import org.xbmc.kore.jsonrpc.notification.Input;
import org.xbmc.kore.jsonrpc.notification.Player;
import org.xbmc.kore.jsonrpc.notification.System;
import org.xbmc.kore.utils.LogUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        public void onPlay(Player.OnPlay notification);
        public void onPause(Player.OnPause notification);
        public void onSpeedChanged(Player.OnSpeedChanged notification);
        public void onSeek(Player.OnSeek notification);
        public void onStop(Player.OnStop notification);
    }

    /**
     * Interface that an observer must implement to be notified of System notifications
     */
    public interface SystemNotificationsObserver {
        public void onQuit(System.OnQuit notification);
        public void onRestart(System.OnRestart notification);
        public void onSleep(System.OnSleep notification);
    }

    /**
     * Interface that an observer must implement to be notified of Input notifications
     */
    public interface InputNotificationsObserver {
        public void onInputRequested(Input.OnInputRequested notification);
    }

    public interface ApplicationNotificationsObserver {
        public void onVolumeChanged(Application.OnVolumeChanged notification);
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
	 * Listener {@link Thread} that will be listening on the TCP socket
	 */
	private Thread listenerThread = null;

	/**
	 * {@link java.util.HashMap} that will hold the {@link MethodCallInfo} with the information
	 * necessary to respond to clients (TCP only)
	 */
	private final HashMap<String, MethodCallInfo<?>> clientCallbacks = new HashMap<String, MethodCallInfo<?>>();

    /**
     * The observers that will be notified of player notifications
     */
    private final HashMap<PlayerNotificationsObserver, Handler> playerNotificationsObservers =
            new HashMap<PlayerNotificationsObserver, Handler>();

    /**
     * The observers that will be notified of system notifications
     */
    private final HashMap<SystemNotificationsObserver, Handler> systemNotificationsObservers =
            new HashMap<SystemNotificationsObserver, Handler>();

    /**
     * The observers that will be notified of input notifications
     */
    private final HashMap<InputNotificationsObserver, Handler> inputNotificationsObservers =
            new HashMap<InputNotificationsObserver, Handler>();

    /**
     * The observers that will be notified of application notifications
     */
    private final HashMap<ApplicationNotificationsObserver, Handler> applicationNotificationsObservers =
            new HashMap<>();

    private ExecutorService executorService;

    private final int connectTimeout;

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms

    private static final int TCP_READ_TIMEOUT = 30000; // ms

    /**
     * OkHttpClient. Make sure it is initialized, by calling {@link #getOkHttpClient()}
     */
    private OkHttpClient httpClient = null;
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

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
        this.executorService = Executors.newSingleThreadExecutor();
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
    public void setProtocol(int protocol) {
        if (!isValidProtocol(protocol)) {
            throw new IllegalArgumentException("Invalid protocol specified.");
        }
        this.protocol = protocol;
    }

    public static boolean isValidProtocol(int protocol) {
        return ((protocol == PROTOCOL_TCP) || (protocol == PROTOCOL_HTTP));
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
     * Registers an observer for input notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void registerApplicationNotificationsObserver(ApplicationNotificationsObserver observer,
                                                   Handler handler) {
        applicationNotificationsObservers.put(observer, handler);
    }

    /**
     * Unregisters and observer from the input notifications
     * @param observer The {@link InputNotificationsObserver}
     */
    public void unregisterApplicationotificationsObserver(ApplicationNotificationsObserver observer) {
        applicationNotificationsObservers.remove(observer);
    }

    /**
	 * Calls the a method on the server
	 * This call is always asynchronous. The results will be posted, through the
	 * {@link ApiCallback callback} parameter, on the specified {@link android.os.Handler}.
	 *
	 * @param method Method object that represents the methood too call
	 * @param callback {@link ApiCallback} to post the response to
	 * @param handler {@link Handler} to invoke callbacks on
	 * @param <T> Method return type
	 */
	public <T> void execute(final ApiMethod<T> method, final ApiCallback<T> callback,
							final Handler handler) {
		LogUtils.LOGD(TAG, "Starting method execute. Method: " + method.getMethodName() +
			" on host: " + hostInfo.getJsonRpcHttpEndpoint());

		// Launch background thread
        Runnable command = new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                if (protocol == PROTOCOL_HTTP) {
//                    executeThroughHttp(method, callback, handler);
                    executeThroughOkHttp(method, callback, handler);
                } else {
                    executeThroughTcp(method, callback, handler);
                }
            }
        };

        executorService.execute(command);
	}

    /**
     * Sends the JSON RPC request through HTTP (using OkHttp library)
     */
    private <T> void executeThroughOkHttp(final ApiMethod<T> method, final ApiCallback<T> callback,
                                          final Handler handler) {
        OkHttpClient client = getOkHttpClient();
        String jsonRequest = method.toJsonString();

        try {
            Request request = new Request.Builder()
                    .url(hostInfo.getJsonRpcHttpEndpoint())
                    .post(RequestBody.create(MEDIA_TYPE_JSON, jsonRequest))
                    .build();
            LogUtils.LOGD(TAG, "Sending request via OkHttp: " + jsonRequest);
            Response response = sendOkHttpRequest(client, request);
            final T result = method.resultFromJson(parseJsonResponse(handleOkHttpResponse(response)));

            if ((handler != null) && (callback != null)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(result);
                    }
                });
            }
        } catch (final ApiException e) {
            // Got an error, call error handler
            if ((handler != null) && (callback != null)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e.getCode(), e.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Initializes this class OkHttpClient
     */
    public OkHttpClient getOkHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient();
            httpClient.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);

            httpClient.setAuthenticator(new Authenticator() {
                @Override
                public Request authenticate(Proxy proxy, Response response) throws IOException {
                    if (TextUtils.isEmpty(hostInfo.getUsername()))
                        return null;

                    String credential = Credentials.basic(hostInfo.getUsername(), hostInfo.getPassword());
                    return response.request().newBuilder().header("Authorization", credential).build();
                }

                @Override
                public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                    return null;
                }
            });
        }
        return httpClient;
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
     * @throws ApiException
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
     * @throws ApiException
     */
    private String handleOkHttpResponse(Response response) throws ApiException {
        try {
//			LogUtils.LOGD(TAG, "Reading HTTP response.");
            int responseCode = response.code();

            switch (responseCode) {
                case 200:
                    // All ok, read response
                    String res = response.body().string();
                    response.body().close();
					LogUtils.LOGD(TAG, "OkHTTP response: " + res);
                    return res;
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
	 * @throws ApiException
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
		} catch (JsonProcessingException e) {
			LogUtils.LOGW(TAG, "Got an exception while parsing JSON response.", e);
			throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
		} catch (IOException e) {
			LogUtils.LOGW(TAG, "Got an exception while parsing JSON response.", e);
			throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
		}
	}

	/**
	 * Sends the JSON RPC request through TCP
	 * Keeps a background thread running, listening on a socket
	 */
	private <T> void executeThroughTcp(final ApiMethod<T> method, final ApiCallback<T> callback,
									   final Handler handler) {
        String methodId = String.valueOf(method.getId());
		try {
			// Save this method/callback for later response
            // Check if a method with this id is already running and raise an error if so
            synchronized (clientCallbacks) {
                if (clientCallbacks.containsKey(methodId)) {
                    if ((handler != null) && (callback != null)) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(ApiException.API_METHOD_WITH_SAME_ID_ALREADY_EXECUTING,
                                        "A method with the same Id is already executing");
                            }
                        });
                    }
                    return;
                }
                clientCallbacks.put(methodId, new MethodCallInfo<T>(method, callback, handler));
            }

            // TODO: Validate if this shouldn't be enclosed by a synchronized.
			if (socket == null) {
				// Open connection to the server and setup reader thread
				socket = openTcpConnection(hostInfo);
				listenerThread = newListenerThread(socket);
				listenerThread.start();
			}

			// Write request
			sendTcpRequest(socket, method.toJsonString());
		} catch (final ApiException e) {
			callErrorCallback(methodId, e);
		}
	}

	/**
	 * Auxiliary method to open the TCP {@link Socket}.
	 * This method calls connect() so that any errors are cathced
	 * @param hostInfo Host info
	 * @return Connection set up
	 * @throws ApiException
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
	 * @throws ApiException
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

	private Thread newListenerThread(final Socket socket) {
		// Launch a new thread to read from the socket
		return new Thread(new Runnable() {
			@Override
			public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
					LogUtils.LOGD(TAG, "Starting socket listener thread...");
					// We're going to read from the socket. This will be a blocking call and
					// it will keep on going until disconnect() is called on this object.
					// Note: Mind the objects used here: we use createParser because it doesn't
					// close the socket after ObjectMapper.readTree.
					JsonParser jsonParser = objectMapper.getFactory().createParser(socket.getInputStream());
					ObjectNode jsonResponse;
					while ((jsonResponse = objectMapper.readTree(jsonParser)) != null) {
                        LogUtils.LOGD(TAG, "Read from socket: " + jsonResponse.toString());
//                        LogUtils.LOGD_FULL(TAG, "Read from socket: " + jsonResponse.toString());
						handleTcpResponse(jsonResponse);
					}
				} catch (JsonProcessingException e) {
					LogUtils.LOGW(TAG, "Got an exception while parsing JSON response.", e);
					callErrorCallback(null, new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e));
				} catch (IOException e) {
					LogUtils.LOGW(TAG, "Error reading from socket.", e);
                    disconnect();
					callErrorCallback(null, new ApiException(ApiException.IO_EXCEPTION_WHILE_READING_RESPONSE, e));
				}
			}
		});
	}

	private <T> void handleTcpResponse(ObjectNode jsonResponse) {

		if (!jsonResponse.has(ApiMethod.ID_NODE)) {
            // It's a notification, notify observers
            String notificationName = jsonResponse.get(ApiNotification.METHOD_NODE).asText();
            ObjectNode params = (ObjectNode)jsonResponse.get(ApiNotification.PARAMS_NODE);

            if (notificationName.equals(Player.OnPause.NOTIFICATION_NAME)) {
                final Player.OnPause apiNotification = new Player.OnPause(params);
                for (final PlayerNotificationsObserver observer :
                        playerNotificationsObservers.keySet()) {
                    Handler handler = playerNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onPause(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(Player.OnPlay.NOTIFICATION_NAME)) {
                final Player.OnPlay apiNotification = new Player.OnPlay(params);
                for (final PlayerNotificationsObserver observer :
                        playerNotificationsObservers.keySet()) {
                    Handler handler = playerNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onPlay(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(Player.OnSeek.NOTIFICATION_NAME)) {
                final Player.OnSeek apiNotification = new Player.OnSeek(params);
                for (final PlayerNotificationsObserver observer :
                        playerNotificationsObservers.keySet()) {
                    Handler handler = playerNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onSeek(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(Player.OnSpeedChanged.NOTIFICATION_NAME)) {
                final Player.OnSpeedChanged apiNotification = new Player.OnSpeedChanged(params);
                for (final PlayerNotificationsObserver observer :
                        playerNotificationsObservers.keySet()) {
                    Handler handler = playerNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onSpeedChanged(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(Player.OnStop.NOTIFICATION_NAME)) {
                final Player.OnStop apiNotification = new Player.OnStop(params);
                for (final PlayerNotificationsObserver observer :
                        playerNotificationsObservers.keySet()) {
                    Handler handler = playerNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onStop(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(System.OnQuit.NOTIFICATION_NAME)) {
                final System.OnQuit apiNotification = new System.OnQuit(params);
                for (final SystemNotificationsObserver observer :
                        systemNotificationsObservers.keySet()) {
                    Handler handler = systemNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onQuit(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(System.OnRestart.NOTIFICATION_NAME)) {
                final System.OnRestart apiNotification = new System.OnRestart(params);
                for (final SystemNotificationsObserver observer :
                        systemNotificationsObservers.keySet()) {
                    Handler handler = systemNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onRestart(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(System.OnSleep.NOTIFICATION_NAME)) {
                final System.OnSleep apiNotification = new System.OnSleep(params);
                for (final SystemNotificationsObserver observer :
                        systemNotificationsObservers.keySet()) {
                    Handler handler = systemNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onSleep(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(Input.OnInputRequested.NOTIFICATION_NAME)) {
                final Input.OnInputRequested apiNotification = new Input.OnInputRequested(params);
                for (final InputNotificationsObserver observer :
                        inputNotificationsObservers.keySet()) {
                    Handler handler = inputNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onInputRequested(apiNotification);
                        }
                    });
                }
            } else if (notificationName.equals(Application.OnVolumeChanged.NOTIFICATION_NAME)) {
                final Application.OnVolumeChanged apiNotification =
                        new Application.OnVolumeChanged(params);
                for (final ApplicationNotificationsObserver observer :
                        applicationNotificationsObservers.keySet()) {
                    Handler handler = applicationNotificationsObservers.get(observer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onVolumeChanged(apiNotification);
                        }
                    });
                }
            }

			LogUtils.LOGD(TAG, "Got a notification: " + jsonResponse.get("method").textValue());
		} else {
			String methodId = jsonResponse.get(ApiMethod.ID_NODE).asText();

			if (jsonResponse.has(ApiMethod.ERROR_NODE)) {
				// Error response
				callErrorCallback(methodId, new ApiException(ApiException.API_ERROR, jsonResponse));
			} else {
				// Sucess response
				final MethodCallInfo<?> methodCallInfo = clientCallbacks.get(methodId);
//				LogUtils.LOGD(TAG, "Sending response to method: " + methodCallInfo.method.getMethodName());

                if (methodCallInfo != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        final T result = (T) methodCallInfo.method.resultFromJson(jsonResponse);
                        @SuppressWarnings("unchecked")
                        final ApiCallback<T> callback = (ApiCallback<T>) methodCallInfo.callback;

                        if ((methodCallInfo.handler != null) && (callback != null)) {
                            methodCallInfo.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(result);
                                }
                            });
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

                    if ((methodCallInfo.handler != null) && (callback != null)) {
                        methodCallInfo.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error.getCode(), error.getMessage());
                            }
                        });
                    }
                }
                clientCallbacks.remove(methodId);
            } else {
                // Notify all pending clients, it might be an error for them
                for (String id : clientCallbacks.keySet()) {
                    final MethodCallInfo<?> methodCallInfo = clientCallbacks.get(id);
                    @SuppressWarnings("unchecked")
                    final ApiCallback<T> callback = (ApiCallback<T>)methodCallInfo.callback;

                    if ((methodCallInfo.handler != null) && (callback != null)) {
                        methodCallInfo.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error.getCode(), error.getMessage());
                            }
                        });
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
    public void disconnect() {
		if (protocol == PROTOCOL_HTTP)
			return;

		try {
			if (socket != null) {
				// Remove pending calls
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
