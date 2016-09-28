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

import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.TestConfig;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Accepts the binary data from the forked VM and pushes it to parent VM
 * as appropriate. This server assumes there is only the one and only
 * client at any given point of time.
 */
public final class BinaryLinkServer {

    private static final int BUFFER_SIZE = Integer.getInteger("jcstress.link.bufferSize", 64*1024);
    private static final String LINK_ADDRESS = System.getProperty("jcstress.link.address");
    private static final int LINK_PORT = Integer.getInteger("jcstress.link.port", 0);
    private static final int LINK_TIMEOUT_MS = Integer.getInteger("jcstress.link.timeoutMs", 30*1000);

    private final ServerSocket server;
    private final InetAddress listenAddress;
    private final TestResultCollector out;
    private final ConcurrentMap<Integer, TestConfig> configs;
    private final ExecutorService executor;

    public BinaryLinkServer(int workers, TestResultCollector out) throws IOException {
        this.out = out;
        this.configs = new ConcurrentHashMap<>();

        listenAddress = getListenAddress();
        server = new ServerSocket(LINK_PORT, 50, listenAddress);
        server.setSoTimeout(LINK_TIMEOUT_MS);
        executor = Executors.newFixedThreadPool(workers);
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
        try {
            server.close();
        } catch (IOException e) {
            // do nothing
        }

        List<Runnable> outstanding = executor.shutdownNow();
        for (Runnable r : outstanding) {
            Handler h = (Handler) r;
            h.close();
        }
    }

    public void addTask(TestConfig cfg) {
        configs.put(cfg.uniqueToken, cfg);
        executor.submit(new Handler(server));
    }

    public String getHost() {
        return listenAddress.getHostAddress();
    }

    public int getPort() {
        // Poll the actual listen port, in case it is ephemeral
        return server.getLocalPort();
    }

    private final class Handler implements Runnable {
        private final ServerSocket server;
        private Socket socket;

        public Handler(ServerSocket server) {
            this.server = server;
        }

        @Override
        public void run() {
            TestConfig config = null;
            try {
                socket = server.accept();

                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                // eager OOS initialization, let the other party read the stream header
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os, BUFFER_SIZE));
                oos.flush();

                // late OIS initialization, otherwise we'll block reading the header
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is, BUFFER_SIZE));

                Object obj;
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof JobRequestFrame) {
                        config = configs.remove(((JobRequestFrame) obj).getToken());
                        if (config == null) {
                            throw new IllegalStateException("No jobs left, this should not happen");
                        }
                        oos.writeObject(new JobResponseFrame(config));
                        oos.flush();
                    }
                    if (obj instanceof ResultsFrame) {
                        out.add(((ResultsFrame) obj).getRes());
                    }
                    if (obj instanceof FinishingFrame) {
                        // close the streams
                        break;
                    }
                }
            } catch (EOFException e) {
                // ignore
            } catch (Exception e) {
                TestResult tr = new TestResult(config, Status.VM_ERROR, -1);
                tr.addAuxData("<binary link had failed, forked VM corrupted the stream?");
                tr.addAuxData(e.getMessage());
                out.add(tr);
            } finally {
                close();
            }
        }

        public void close() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }

}
