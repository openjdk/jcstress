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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Sends the test results over the network.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class NetworkOutputCollector implements TestResultCollector {

    private final ObjectOutputStream oos;
    private final Socket clientSocket;

    public NetworkOutputCollector(String hostName, int hostPort) throws IOException {
        this.clientSocket = new Socket(hostName, hostPort);
        this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void add(TestResult result) {
        try {
            oos.writeObject(result);
            oos.flush();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void close() {
        try {
            oos.writeObject(new EndResult());
            oos.flush();
        } catch (IOException e) {
            // do nothing
        }

        try {
            oos.close();
        } catch (IOException e) {
            // do nothing
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            // do nothing
        }
    }
}
