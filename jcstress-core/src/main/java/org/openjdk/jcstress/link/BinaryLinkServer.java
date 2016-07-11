/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jcstress.link;

import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.TestConfig;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Accepts the binary data from the forked VM and pushes it to parent VM
 * as appropriate. This server assumes there is only the one and only
 * client at any given point of time.
 */
public final class BinaryLinkServer {

    private static final int BUFFER_SIZE = Integer.getInteger("jcstress.link.bufferSize", 64*1024);

    private final Acceptor acceptor;
    private final Collection<Handler> handlers;
    private final TestResultCollector out;
    private final ConcurrentMap<Integer, TestConfig> configs;

    public BinaryLinkServer(TestResultCollector out) throws IOException {
        this.out = out;
        this.configs = new ConcurrentHashMap<>();
        acceptor = new Acceptor();
        acceptor.start();

        handlers = Collections.synchronizedCollection(new ArrayList<>());
    }

    public void terminate() {
        acceptor.close();

        synchronized (handlers) {
            for (Handler h : handlers) {
                h.close();
            }
        }

        try {
            acceptor.join();
            synchronized (handlers) {
                for (Handler h : handlers) {
                    h.join();
                }
            }
        } catch (InterruptedException e) {
            // ignore
        }

        handlers.clear();
    }

    private InetAddress getListenAddress() {
        // Try to use user-provided override first.
        String addr = System.getProperty("jcstress.link.address");
        if (addr != null) {
            try {
                return InetAddress.getByName(addr);
            } catch (UnknownHostException e) {
                // override failed, notify user
                throw new IllegalStateException("Can not initialize binary link.", e);
            }
        }

        return InetAddress.getLoopbackAddress();
    }

    private int getListenPort() {
        return Integer.getInteger("jmh.link.port", 0);
    }

    public void addTask(TestConfig cfg) {
        configs.put(cfg.uniqueToken, cfg);
    }

    private final class Acceptor extends Thread {

        private final ServerSocket server;
        private final InetAddress listenAddress;

        public Acceptor() throws IOException {
            listenAddress = getListenAddress();
            server = new ServerSocket(getListenPort(), 50, listenAddress);
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Socket clientSocket = server.accept();
                    Handler r = new Handler(clientSocket);
                    handlers.add(r);
                    r.start();
                }
            } catch (SocketException e) {
                // assume this is "Socket closed", return
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                close();
            }
        }

        public String getHost() {
            return listenAddress.getHostAddress();
        }

        public int getPort() {
            // Poll the actual listen port, in case it is ephemeral
            return server.getLocalPort();
        }

        public void close() {
            try {
                server.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public String getHost() {
        return acceptor.getHost();
    }

    public int getPort() {
        return acceptor.getPort();
    }

    private final class Handler extends Thread {
        private final InputStream is;
        private final Socket socket;
        private ObjectInputStream ois;
        private final OutputStream os;
        private ObjectOutputStream oos;

        public Handler(Socket socket) throws IOException {
            this.socket = socket;
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();

            // eager OOS initialization, let the other party read the stream header
            oos = new ObjectOutputStream(new BufferedOutputStream(os, BUFFER_SIZE));
            oos.flush();
        }

        @Override
        public void run() {
            try {
                // late OIS initialization, otherwise we'll block reading the header
                ois = new ObjectInputStream(new BufferedInputStream(is, BUFFER_SIZE));

                Object obj;
                while ((obj = ois.readObject()) != null) {

                    if (obj instanceof JobRequestFrame) {
                        handleHandshake((JobRequestFrame) obj);
                    }
                    if (obj instanceof ResultsFrame) {
                        handleResults((ResultsFrame) obj);
                    }
                    if (obj instanceof FinishingFrame) {
                        // close the streams
                        break;
                    }
                }
            } catch (EOFException e) {
                // ignore
            } catch (Exception e) {
                System.out.println("<binary link had failed, forked VM corrupted the stream?");
                e.printStackTrace(System.out);
            } finally {
                close();
                handlers.remove(this);
            }
        }

        private void handleResults(ResultsFrame obj) {
            out.add(obj.getRes());
        }

        private void handleHandshake(JobRequestFrame obj) throws IOException {
            TestConfig poll = configs.remove(obj.getToken());
            if (poll == null) {
                throw new IllegalStateException("No jobs left, this should not happen");
            }
            oos.writeObject(new JobResponseFrame(poll));
            oos.flush();
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

    }

}
