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
    private boolean running;
    private ExecutorService executor;
    private int port = -1;
    private InetSocketAddress inetSocketAddress;

    private final Set<Socket> openClientSockets =
            Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());

    private final TcpServerConnectionHandler connectionHandler;

    // TODO
    // Enhance handler to handle multiple connections simultaneously. It can now handle one
    // connection at a time, which makes the current setup of the MockTcpServer (with threading)
    // overkill.
    public interface TcpServerConnectionHandler {
        /**
         * Processes received input
         * @param socket
         * @return id of associated response if any, -1 if more input is needed.
         */
        void processInput(Socket socket);

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
        if (running) throw new IllegalStateException("start() already called");
        running = true;
        this.inetSocketAddress = inetSocketAddress;

        serverSocket = serverSocketFactory.createServerSocket();
        // Reuse port if not using a random port
        serverSocket.setReuseAddress(inetSocketAddress.getPort() != 0);
        serverSocket.bind(inetSocketAddress, 50);

        executor = Executors.newCachedThreadPool(Util.threadFactory("MockTcpServer", false));

        port = serverSocket.getLocalPort();

        LogUtils.LOGD(TAG, "start: server started on " + serverSocket.getInetAddress() + ":" + port);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        Socket socket = acceptConnection();
                        serveConnection(socket);
                    } catch(IOException e){
                        //Socket closed
                        LogUtils.LOGD(TAG, "acceptConnection: " + e.getMessage());
                    }
                }
            }

            private Socket acceptConnection() throws IOException {
                Socket socket = serverSocket.accept();

                synchronized (openClientSockets) {
                    openClientSockets.add(socket);
                }

                return socket;
            }
        });
    }

    public synchronized void shutdown() throws IOException {
        if (!running) return;

        if (serverSocket == null) throw new IllegalStateException("shutdown() before start()");

        running = false;

        // Release all sockets and all threads, even if any close fails.
        for (Iterator<Socket> s = openClientSockets.iterator(); s.hasNext(); ) {
            Socket socket = s.next();
            Util.closeQuietly(socket);
            s.remove();
        }
        Util.closeQuietly(serverSocket);

        executor.shutdown();

        // Await shutdown.
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IOException("Gave up waiting for executor to shut down");
            }
        } catch (InterruptedException e) {
            LogUtils.LOGD(TAG, "shutdown: " + e.getMessage());
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
                    LogUtils.LOGD(TAG, "serveConnection: handling client " + socket.getInetAddress()
                                       + ":" + socket.getLocalPort());

                    connectionHandler.processInput(socket);
                    socket.close();

                    synchronized (openClientSockets) {
                        openClientSockets.remove(socket);
                    }
                } catch (IOException e) {
                    LogUtils.LOGW(TAG, "processing input from " + socket.getInetAddress() + " failed: " + e);
                }
            }
        });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while ( ! (serverSocket.isClosed() || socket.isClosed()) ) {
                        sendResponse();
                        Thread.sleep(100);
                    }
                } catch (IOException e) {
                    LogUtils.LOGW(TAG, " sending response from " + socket.getInetAddress() + " failed: " + e);
                } catch (InterruptedException e) {
                    LogUtils.LOGW(TAG, " wait interrupted" + e);
                }
            }

            private void sendResponse() throws IOException {
                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), false);
                String answer = connectionHandler.getResponse();
                if (answer != null) {
                    LogUtils.LOGD(TAG, "serveConnection: sendResponse: " +answer);
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
