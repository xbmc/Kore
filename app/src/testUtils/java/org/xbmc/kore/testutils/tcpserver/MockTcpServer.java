/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

package org.xbmc.kore.testutils.tcpserver;

import com.squareup.okhttp.internal.Util;

import org.xbmc.kore.utils.LogUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;



public class MockTcpServer {
    public static final String TAG = LogUtils.makeLogTag(MockTcpServer.class);

    private ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
    private ServerSocket serverSocket;
    private boolean started;
    private ExecutorService executor;
    private int port = -1;
    private StringBuffer request;
    private InetSocketAddress inetSocketAddress;

    private final Set<Socket> openClientSockets =
            Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());

    private final TcpServerConnectionHandler connectionHandler;

    public interface TcpServerConnectionHandler {
        /**
         * Processes received input
         * @param c character received
         * @return id of associated response if any, -1 if more input is needed.
         */
        void processInput(char c);

        /**
         * Gets the answer for this handler that should be returned to the server after input has been
         * processed successfully
         * @return answer or null if no answer is available
         */
        String getResponse();
    }

    public MockTcpServer(TcpServerConnectionHandler handler) {
        connectionHandler = handler;
    }

    /**
     * Starts the server on localhost on a random free port
     * @throws IOException
     */
    public void start() throws IOException {
        start(new InetSocketAddress(InetAddress.getByName("localhost"), 0));
    }

    /**
     *
     * @param inetSocketAddress set portnumber to 0 to select a random free port
     * @throws IOException
     */
    public void start(InetSocketAddress inetSocketAddress) throws IOException {
        if (started) throw new IllegalStateException("start() already called");
        started = true;
        this.inetSocketAddress = inetSocketAddress;

        serverSocket = serverSocketFactory.createServerSocket();
        // Reuse port if not using a random port
        serverSocket.setReuseAddress(inetSocketAddress.getPort() != 0);
        serverSocket.bind(inetSocketAddress, 50);

        executor = Executors.newCachedThreadPool(Util.threadFactory("MockTcpServer", false));

        port = serverSocket.getLocalPort();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    acceptConnection();
                } catch (Throwable e) {
                    LogUtils.LOGE(TAG, " failed unexpectedly: " + e);
                }

                // Release all sockets and all threads, even if any close fails.
                Util.closeQuietly(serverSocket);
                for (Iterator<Socket> s = openClientSockets.iterator(); s.hasNext(); ) {
                    Util.closeQuietly(s.next());
                    s.remove();
                }

                executor.shutdown();
            }

            private void acceptConnection() throws Exception {
                while (true) {
                    Socket socket;
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketException e) {
                        //Socket closed
                        return;
                    }

                    openClientSockets.add(socket);
                    serveConnection(socket);
                }
            }
        });
    }

    public synchronized void shutdown() throws IOException {
        if (!started) return;
        if (serverSocket == null) throw new IllegalStateException("shutdown() before start()");

        serverSocket.close();

        // Await shutdown.
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IOException("Gave up waiting for executor to shut down");
            }
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Gets the local port of this server socket or -1 if it is not bound
     * @return the local port this server is listening on.
     */
    public int getPort() {
        return port;
    }

    public String getHostName() {
        if ( inetSocketAddress == null )
            throw new RuntimeException("Must start server before getting hostname");

        return inetSocketAddress.getHostName();
    }

    private void serveConnection(final Socket socket) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    handleInput();
                } catch (IOException e) {
                    LogUtils.LOGW(TAG, "processing input from " + socket.getInetAddress() + " failed: " + e);
                }
            }

            private void handleInput() throws IOException {
                InputStreamReader in = new InputStreamReader(socket.getInputStream());

                request = new StringBuffer();
                int i;
                while ((i = in.read()) != -1) {
                    request.append((char) i);

                    synchronized (connectionHandler) {
                        connectionHandler.processInput((char) i);
                    }
                }

                socket.close();
                openClientSockets.remove(socket);
            }
        });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sendResponse();
                        Thread.sleep(1000);
                        if ( serverSocket.isClosed() )
                            return;
                    }
                } catch (IOException e) {
                    LogUtils.LOGW(TAG, " sending response from " + socket.getInetAddress() + " failed: " + e);
                } catch (InterruptedException e) {
                    LogUtils.LOGW(TAG, " wait interrupted" + e);
                }
            }

            private void sendResponse() throws IOException {
                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);
                String answer = connectionHandler.getResponse();
                if (answer != null) {
                    out.print(answer);
                    out.flush();
                }
            }
        });
    }

    @Override
    public String toString() {
        return "MockTcpServer[" + port + "]";
    }
}
