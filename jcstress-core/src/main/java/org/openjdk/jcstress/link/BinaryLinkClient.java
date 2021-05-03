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

    private final String hostName;
    private final int hostPort;

    public BinaryLinkClient(String hostName, int hostPort) {
        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    private Object requestResponse(Object frame) throws IOException {
        long time1 = System.nanoTime();
        try (Socket socket = new Socket(hostName, hostPort)) {
            socket.setTcpNoDelay(true);
            long time2 = System.nanoTime();

            try (BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(frame);
                oos.flush();

                long time3 = System.nanoTime();

                try (BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                     ObjectInputStream ois = new ObjectInputStream(bis)) {
                    final Object o = ois.readObject();

                    long time4 = System.nanoTime();

                    System.out.println("Connect time: " + (time2 - time1)/1000 + " us");
                    System.out.println("Write time: " + (time3 - time2)/1000 + " us");
                    System.out.println("Read time: " + (time4 - time3)/1000 + " us");

                    return o;
                } catch (ClassNotFoundException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    public TestConfig jobRequest(String token) throws IOException {
        Object reply = requestResponse(new JobRequestFrame(token));
        if (reply instanceof TestConfig) {
            return (TestConfig) reply;
        } else {
            throw new IllegalStateException("Got the erroneous reply: " + reply);
        }
    }

    public void doneResult(String token, TestResult result) throws IOException {
        requestResponse(new ResultsFrame(token, result));
    }

}
