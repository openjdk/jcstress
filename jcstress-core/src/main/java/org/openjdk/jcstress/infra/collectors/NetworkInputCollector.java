/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.infra.collectors;

import org.openjdk.jcstress.infra.EndResult;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Receives the test results over the network.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class NetworkInputCollector {

    private final Acceptor acceptor;
    private final List<Reader> registeredReaders;
    private final TestResultCollector out;

    public NetworkInputCollector(TestResultCollector out) throws IOException {
        this.out = out;

        registeredReaders = Collections.synchronizedList(new ArrayList<>());

        acceptor = new Acceptor();
        acceptor.start();
    }

    public void terminate() {
        acceptor.close();

        for (Reader r : registeredReaders) {
            r.close();
        }

        try {
            acceptor.join();
            for (Reader r : registeredReaders) {
                r.join();
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void waitFinish() {
        for (Iterator<Reader> iterator = registeredReaders.iterator(); iterator.hasNext(); ) {
            Reader r = iterator.next();
            try {
                r.join();
                iterator.remove();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private final class Acceptor extends Thread {

        private final ServerSocket server;

        public Acceptor() throws IOException {
            server = new ServerSocket(0);
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Socket clientSocket = server.accept();
                    Reader r = new Reader(clientSocket);
                    registeredReaders.add(r);
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
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to resolve local host", e);
            }
        }

        public int getPort() {
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

    private final class Reader extends Thread {
        private final InputStream is;
        private final Socket socket;
        private ObjectInputStream ois;

        public Reader(Socket socket) throws IOException {
            this.socket = socket;
            this.is = socket.getInputStream();
        }

        @Override
        public void run() {
            try {
                // late OIS initialization, otherwise we'll block reading the header
                ois = new ObjectInputStream(is);

                Object obj;
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof EndResult) return;

                    if (obj instanceof TestResult) {
                        out.add((TestResult) obj);
                    }
                }
            } catch (EOFException e) {
                // expect
            } catch (ClassNotFoundException | IOException e) {
                throw new IllegalStateException(e);
            } finally {
                close();
            }
        }

        public void close() {
            try {
                ois.close();
            } catch (IOException e) {
                // ignore
            }

            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }

            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

    }

}
