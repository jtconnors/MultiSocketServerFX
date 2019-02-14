/*
 * Copyright (c) 2019, Jim Connors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of this project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jtconnors.socketfx;

import com.jtconnors.socket.Constants;
import com.jtconnors.socket.DebugFlags;
import com.jtconnors.socket.SocketBase;
import com.jtconnors.socket.SocketListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Logger;

public class FxMultipleSocketServer extends SocketBase implements Runnable {

    private final static Logger LOGGER
            = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private int listenerPort;
    private ServerSocket serverSocket;
    private SocketListener fxListener;
    private List<MultipleSocketListener> updateListeners;

    class MultipleSocketListener implements SocketListener {

        private PrintWriter writer;
        private BufferedReader reader;
        private Socket socket;

        /*
         * Called whenever a message is read from the socket. In JavaFX, this
         * method must be run on the main thread and is accomplished by the
         * Platform.runLater() call. Failure to do so *will* result in strange
         * errors and exceptions.
         *
         * @param line Line of text read from the socket.
         */
        @Override
        public void onMessage(final String line) {
            javafx.application.Platform.runLater(() -> {
                fxListener.onMessage(line);
            });
        }

        /*
         * Called whenever the open/closed status of the Socket changes. In
         * JavaFX, this method must be run on the main thread and is
         * accomplished by the Platform.runLater() call. Failure to do so will
         * result in strange errors and exceptions.
         *
         * @param isClosed true if the socket is closed
         */
        @Override
        public void onClosedStatus(final boolean isClosed) {
            javafx.application.Platform.runLater(() -> {
                fxListener.onClosedStatus(isClosed);
            });
        }

        /*
         * Close down the Socket infrastructure.  As per the Java Socket
         * API, once a Socket has been closed, it is not available for
         * further networking use (i.e. can't be reconnected or rebound).
         * A new Socket needs to be created.
         */
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                if (debugFlagIsSet(DebugFlags.instance().DEBUG_STATUS)) {
                    LOGGER.info("socket closed");
                }
                removeListener(this);
                onClosedStatus(true);
            } catch (IOException e) {
                if (debugFlagIsSet(DebugFlags.instance().DEBUG_EXCEPTIONS)) {
                    LOGGER.info(ExceptionStackTraceAsString(e));
                }
            }
        }

        /*
         * Even if we don't read anything from the socket, set up a
         * ReaderThread because it will unable us to detect when a
         * socket connection has been closed.
         */
        class ReaderThread extends Thread {

            @Override
            public void run() {
                /*
                 * Read from input stream one line at a time.  The read
                 * loop will terminate when the socket connection is closed.
                 */
                try {
                    if (reader != null) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (debugFlagIsSet(
                                    DebugFlags.instance().DEBUG_RECV)) {
                                String logMsg = "recv> " + line;
                                LOGGER.info(logMsg);
                            }
                            onMessage(line);
                        }
                    }
                } catch (IOException e) {
                    if (debugFlagIsSet(DebugFlags.instance().DEBUG_EXCEPTIONS)) {
                        LOGGER.info("ReaderThread Exception");
                        LOGGER.info(e.getMessage());
                    }
                } finally {
                    close();
                }
            }
        }

        public void sendMessage(String line) {
            try {
                if (debugFlagIsSet(DebugFlags.instance().DEBUG_SEND)) {
                    System.out.println("send> " + line);
                }
                writer.println(line);
                if (writer.checkError()) {
                    removeListener(this);
                }
            } catch (Exception ex) {
                removeListener(this);
            }
        }

        private void setup(Socket socket) throws IOException {
            this.socket = socket;
            /* 
             * Leave check for null here.  At startup, we create a null
             * listener just so that we can call onClosedStatus(true) to
             * print out the connection status line.
             */
            if (socket != null) {
                writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                new ReaderThread().start();
            }
        }

        public MultipleSocketListener(Socket socket) throws IOException {
            setup(socket);
        }
    }

    public boolean isServerSocketClosed() {
        return serverSocket.isClosed();
    }

    public int getListenerCount() {
        return updateListeners.size();
    }

    private void addListener(MultipleSocketListener listener) {
        updateListeners.add(listener);
        listener.onClosedStatus(false);
    }

    private void removeListener(MultipleSocketListener listener) {
        updateListeners.remove(listener);
        listener.onClosedStatus(true);
    }

    /**
     * Bring the FxMultipleSocketServer instance down in an orderly fashion.
     */
    public void shutdown() {
        /* 
         * To avoid a ConcurrentModificationException, which happens if the
         * updateListener is modified while iterating over it, copy
         * it to an array and iterate over that. 
         */
        final MultipleSocketListener[] listeners = updateListeners.toArray(
                new MultipleSocketListener[this.updateListeners.size()]);
        for (final MultipleSocketListener listener : listeners) {
            listener.close();
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            fxListener.onClosedStatus(true);
        } catch (IOException e) {
            LOGGER.info(ExceptionStackTraceAsString(e));
        }
    }

    @Override
    public void run() {
        try {
//            new MultipleSocketListener(null).onClosedStatus(true);
            serverSocket = new ServerSocket(listenerPort);
            while (true) {
                Socket acceptSocket = serverSocket.accept();
                addListener(new MultipleSocketListener(acceptSocket));
            }
        } catch (IOException e) {
            /*
             * When the serverSocket is closed, for example when the the
             * shutdown() method is invoked, a SocketException (child of
             * IOException) is thrown.  We need to at minumum catch this.
             */
            if (debugFlagIsSet(DebugFlags.instance().DEBUG_EXCEPTIONS)) {
                LOGGER.info(e.getMessage());
            }
        }
    }

    /*
     * Use ExecutorService to post updates.  Each individual socket will
     * get its own thread.  This construct seems to avoid a
     * ConcurrentModificationException which happens if the updateListener
     * list is modified while iterating over it.
     */
    private final ExecutorService executor
            = new ScheduledThreadPoolExecutor(10);

    /**
     * Send a message to all listening sockets.
     *
     * @param line message to send to all listening sockets
     */
    public void postUpdate(final String line) {

        final MultipleSocketListener[] listeners = updateListeners.toArray(
                new MultipleSocketListener[this.updateListeners.size()]);

        for (final MultipleSocketListener listener : listeners) {
            executor.submit(() -> {
                listener.sendMessage(line);
            });
        }
    }

    public FxMultipleSocketServer(SocketListener listener) {
        this(listener, Constants.instance().DEFAULT_PORT,
                DebugFlags.instance().DEBUG_NONE);
    }

    public FxMultipleSocketServer(SocketListener listener,
            int listenerPort) {
        this(listener, listenerPort, DebugFlags.instance().DEBUG_NONE);
    }

    public FxMultipleSocketServer(SocketListener listener,
            int listenerPort, int debugFlags) {
        fxListener = listener;
        this.listenerPort = listenerPort;
        updateListeners = new ArrayList<>();
    }
}
