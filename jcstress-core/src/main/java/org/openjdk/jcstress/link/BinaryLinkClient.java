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

import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.util.FileUtils;

import java.io.*;
import java.net.Socket;

public final class BinaryLinkClient implements TestResultCollector {

    private static final int RESET_EACH = Integer.getInteger("jcstress.link.resetEach", 100);
    private static final int BUFFER_SIZE = Integer.getInteger("jcstress.link.bufferSize", 64*1024);

    private final Object lock;

    private final Socket clientSocket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private volatile boolean failed;
    private int resetToGo;

    public BinaryLinkClient(String hostName, int hostPort) throws IOException {
        this.lock = new Object();
        this.clientSocket = new Socket(hostName, hostPort);

        // Initialize the OOS first, and flush, letting the other party read the stream header.
        this.oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream(), BUFFER_SIZE));
        this.oos.flush();

        this.ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream(), BUFFER_SIZE));
    }

    private void pushFrame(Serializable frame) throws IOException {
        if (failed) {
            throw new IOException("Link had failed already");
        }

        // It is important to reset the OOS to avoid garbage buildup in internal identity
        // tables. However, we cannot do that after each frame since the huge referenced
        // objects like benchmark and iteration parameters will be duplicated on the receiver
        // side. This is why we reset only each RESET_EACH frames.
        //
        // It is as much as important to flush the stream to let the other party know we
        // pushed something out.

        synchronized (lock) {
            try {
                if (resetToGo-- < 0) {
                    oos.reset();
                    resetToGo = RESET_EACH;
                }

                oos.writeObject(frame);
                oos.flush();
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }
    }

    private Object readFrame() throws IOException, ClassNotFoundException {
        try {
            return ois.readObject();
        } catch (ClassNotFoundException ex) {
            failed = true;
            throw ex;
        } catch (IOException ex) {
            failed = true;
            throw ex;
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            oos.writeObject(new FinishingFrame());
            FileUtils.safelyClose(ois);
            FileUtils.safelyClose(oos);
            clientSocket.close();
        }
    }

    public TestConfig nextJob(int token) throws IOException, ClassNotFoundException {
        synchronized (lock) {
            pushFrame(new JobRequestFrame(token));

            Object reply = readFrame();
            if (reply instanceof JobResponseFrame) {
                return ((JobResponseFrame) reply).getConfig();
            } else {
                throw new IllegalStateException("Got the erroneous reply: " + reply);
            }
        }
    }

    @Override
    public void add(TestResult result) {
        try {
            pushFrame(new ResultsFrame(result));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
