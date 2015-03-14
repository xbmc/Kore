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
import android.util.Base64;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.jsonrpc.notification.Input;
import org.xbmc.kore.jsonrpc.notification.Player;
import org.xbmc.kore.jsonrpc.notification.System;
import org.xbmc.kore.utils.LogUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ExecutorService executorService;

    private final int connectTimeout;

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms

    private static final int TCP_READ_TIMEOUT = 30000; // ms

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
                    executeThroughHTTP(method, callback, handler);
                } else {
                    executeThroughTcp(method, callback, handler);
                }
            }
        };

        executorService.execute(command);
		//new Thread(command).start();
	}

	/**
	 * Sends the JSON RPC request through HTTP
	 */
	private <T> void executeThroughHTTP(final ApiMethod<T> method, final ApiCallback<T> callback,
										final Handler handler) {
		String jsonRequest = method.toJsonString();
        LogUtils.LOGD(TAG, "send jsonRequest = " + jsonRequest);
		try {
			HttpURLConnection connection = openHttpConnection(hostInfo);
		    sendHttpRequest(connection, jsonRequest);
			// Read response and convert it
			final T result = method.resultFromJson(parseJsonResponse(readHttpResponse(connection)));

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
     * Sends the JSON RPC request through HTTP, and calls the callback with the raw response,
     * not parsed into the internal representation.
     * Useful for sync methods that don't want to incur the overhead of constructing the
     * internal objects.
     *
     * @param method Method object that represents the method too call
     * @param callback {@link ApiCallback} to post the response to. This will be the raw
     * {@link ObjectNode} received
     * @param handler {@link Handler} to invoke callbacks on
     * @param <T> Method return type
     */
    public <T> void executeRaw(final ApiMethod<T> method, final ApiCallback<ObjectNode> callback,
                                     final Handler handler) {
        String jsonRequest = method.toJsonString();
        try {
            HttpURLConnection connection = openHttpConnection(hostInfo);
            sendHttpRequest(connection, jsonRequest);
            // Read response and convert it
            final ObjectNode result = parseJsonResponse(readHttpResponse(connection));

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
	 * Auxiliary method to open a HTTP connection.
	 * This method calls connect() so that any errors are cathced
	 * @param hostInfo Host info
	 * @return Connection set up
	 * @throws ApiException
	 */
	private HttpURLConnection openHttpConnection(HostInfo hostInfo) throws ApiException {
		try {
//			LogUtils.LOGD(TAG, "Opening HTTP connection.");
			HttpURLConnection connection = (HttpURLConnection) new URL(hostInfo.getJsonRpcHttpEndpoint()).openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(connectTimeout);
			//connection.setReadTimeout(connectTimeout);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			// http basic authorization
			if ((hostInfo.getUsername() != null) && !hostInfo.getUsername().isEmpty() &&
				(hostInfo.getPassword() != null) && !hostInfo.getPassword().isEmpty()) {
				final String token = Base64.encodeToString((hostInfo.getUsername() + ":" +
					hostInfo.getPassword()).getBytes(), Base64.DEFAULT);
				connection.setRequestProperty("Authorization", "Basic " + token);
			}

			// Check the connection
			connection.connect();
			return connection;
		} catch (ProtocolException e) {
			// Won't try to catch this
			LogUtils.LOGE(TAG, "Got protocol exception while opening HTTP connection.", e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.LOGW(TAG, "Failed to open HTTP connection.", e);
			throw new ApiException(ApiException.IO_EXCEPTION_WHILE_CONNECTING, e);
		}
	}

	/**
	 * Send an HTTP POST request
	 * @param connection Open connection
	 * @param request Request to send
	 * @throws ApiException
	 */
	private void sendHttpRequest(HttpURLConnection connection, String request) throws ApiException {
        LogUtils.LOGD(TAG, "Sending request via HTTP: " + request);
		try {
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(request);
			out.flush();
			out.close();
		} catch (IOException e) {
			LogUtils.LOGW(TAG, "Failed to send HTTP request.", e);
			throw new ApiException(ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST, e);
		}

	}

	/**
	 * Reads the response from the server
	 * @param connection Connection
	 * @return Response read
	 * @throws ApiException
	 */
	private String readHttpResponse(HttpURLConnection connection) throws ApiException {
		try {
//			LogUtils.LOGD(TAG, "Reading HTTP response.");
			int responseCode = connection.getResponseCode();

			switch (responseCode) {
				case 200:
					// All ok, read response
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					StringBuilder response = new StringBuilder();
					String inputLine;
					while ((inputLine = in.readLine()) != null)
						response.append(inputLine);
					in.close();
					LogUtils.LOGD(TAG, "HTTP response: " + response.toString());
					return response.toString();
				case 401:
					LogUtils.LOGD(TAG, "HTTP response read error. Got a 401.");
					throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNAUTHORIZED,
						"Server returned response code: " + responseCode);
				case 404:
					LogUtils.LOGD(TAG, "HTTP response read error. Got a 404.");
					throw new ApiException(ApiException.HTTP_RESPONSE_CODE_NOT_FOUND,
						"Server returned response code: " + responseCode);
				default:
					LogUtils.LOGD(TAG, "HTTP response read error. Got: " + responseCode);
					throw new ApiException(ApiException.HTTP_RESPONSE_CODE_UNKNOWN,
						"Server returned response code: " + responseCode);
			}
		} catch (IOException e) {
			LogUtils.LOGW(TAG, "Failed to read HTTP response.", e);
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
		LogUtils.LOGD(TAG, "parseJsonResponse: response = " + response);
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
        LogUtils.LOGD(TAG, "executeThroughTcp() methodId = " + methodId);
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
        LogUtils.LOGD(TAG, "Opening TCP connection on host: " + hostInfo.getAddress());
		try {


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
        LogUtils.LOGD(TAG, "Sending request via TCP: " + request);
		try {
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
                        //LogUtils.LOGD(TAG, "Read from socket: " + jsonResponse.toString());
                        LogUtils.LOGD_FULL(TAG, "Read from socket: " + jsonResponse.toString());
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
            }

			LogUtils.LOGD(TAG, "Got a notification: " + jsonResponse.get("method").textValue());
		} else {
			String methodId = jsonResponse.get(ApiMethod.ID_NODE).asText();
                        LogUtils.LOGD(TAG, "handleTcpResponse(): methodId = " + methodId);
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
	 * This method should always be called if the protocoll used is TCP, so we can shutdown gracefully
	 */
    public void disconnect() {
        LogUtils.LOGD(TAG, "disconnect(): ");
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
