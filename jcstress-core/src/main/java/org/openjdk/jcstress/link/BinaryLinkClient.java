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
import org.openjdk.jcstress.infra.runners.TestConfig;

import java.io.*;
import java.net.Socket;

public final class BinaryLinkClient {

    private static final int LINK_TIMEOUT_MS = Integer.getInteger("jcstress.link.timeoutMs", 30 * 1000);

    private final Object lock;
    private final String hostName;
    private final int hostPort;

    public BinaryLinkClient(String hostName, int hostPort) {
        this.hostName = hostName;
        this.hostPort = hostPort;
        this.lock = new Object();
    }

    private Object requestResponse(Object frame) throws IOException {
        synchronized (lock) {
            try (Socket socket = new Socket(hostName, hostPort)) {
                socket.setKeepAlive(true);
                socket.setSoTimeout(LINK_TIMEOUT_MS);

                try (BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(frame);
                    oos.flush();

                    try (BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                         ObjectInputStream ois = new ObjectInputStream(bis)) {
                        return ois.readObject();
                    } catch (ClassNotFoundException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
    }

    public TestConfig nextJob(String token) throws IOException {
        Object reply = requestResponse(new JobRequestFrame(token));
        if (reply instanceof JobResponseFrame) {
            return ((JobResponseFrame) reply).getConfig();
        } else {
            throw new IllegalStateException("Got the erroneous reply: " + reply);
        }
    }

    public void addResult(String token, TestResult result) {
        try {
            requestResponse(new ResultsFrame(token, result));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
