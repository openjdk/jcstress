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

import org.openjdk.jcstress.infra.runners.TestConfig;

import java.io.*;
import java.net.*;

/**
 * Accepts the binary data from the forked VM and pushes it to parent VM
 * as appropriate. This server assumes there is only the one and only
 * client at any given point of time.
 */
public final class BinaryLinkServer {

    private static final String LINK_ADDRESS = System.getProperty("jcstress.link.address");
    private static final int LINK_PORT = Integer.getInteger("jcstress.link.port", 0);
    private static final int LINK_TIMEOUT_MS = Integer.getInteger("jcstress.link.timeoutMs", 30 * 1000);

    private final ServerSocket server;
    private final InetAddress listenAddress;
    private final Handler handler;
    private final ServerListener listener;

    public BinaryLinkServer(ServerListener listener) throws IOException {
        this.listener = listener;

        listenAddress = getListenAddress();
        server = new ServerSocket(LINK_PORT, 50, listenAddress);
        server.setSoTimeout(LINK_TIMEOUT_MS);

        handler = new Handler(server);
        handler.start();
    }

    private InetAddress getListenAddress() {
        // Try to use user-provided override first.
        if (LINK_ADDRESS != null) {
            try {
                return InetAddress.getByName(LINK_ADDRESS);
            } catch (UnknownHostException e) {
                // override failed, notify user
                throw new IllegalStateException("Can not initialize binary link.", e);
            }
        }

        return InetAddress.getLoopbackAddress();
    }

    public void terminate() {
        // set interrupt flag
        handler.interrupt();

        // all existing Handlers blocked on accept() should exit now
        try {
            server.close();
        } catch (IOException e) {
            // do nothing
        }

        // wait for handler to join
        try {
            handler.join();
        } catch (InterruptedException e) {
            // do nothing
        }

    }

    public String getHost() {
        return listenAddress.getHostAddress();
    }

    public int getPort() {
        // Poll the actual listen port, in case it is ephemeral
        return server.getLocalPort();
    }

    private final class Handler extends Thread {
        private final ServerSocket server;

        public Handler(ServerSocket server) {
            this.server = server;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                acceptAndProcess();
            }
        }

        private void acceptAndProcess() {
            try (Socket socket = server.accept()) {
                try (BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                     ObjectInputStream ois = new ObjectInputStream(bis)) {
                    Object obj = ois.readObject();
                    try (BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        if (obj instanceof JobRequestFrame) {
                            String tkn = ((JobRequestFrame) obj).getToken();
                            TestConfig cfg = listener.onJobRequest(tkn);
                            oos.writeObject(new JobResponseFrame(cfg));
                        } else if (obj instanceof ResultsFrame) {
                            ResultsFrame rf = (ResultsFrame) obj;
                            listener.onResult(rf.getToken(), rf.getRes());
                            oos.writeObject(new OkResponseFrame());
                        } else {
                            // should always reply something
                            oos.writeObject(new WTFWasThatFrame());
                        }
                    }
                }
            } catch (EOFException e) {
                // ignore
            } catch (Exception e) {
                // ignore, the exit code would be non-zero, and TestExecutor would handle it.
            }
        }

    }

}
