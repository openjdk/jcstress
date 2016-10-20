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
package org.openjdk.jcstress;

import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.link.BinaryLinkClient;
import org.openjdk.jcstress.vm.WhiteBoxSupport;

import java.io.IOException;

/**
 * Entry point for the forked VM run.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ForkedMain {

    public static void main(String[] args) throws Exception {
        try {
            WhiteBoxSupport.initSafely();
        } catch (NoClassDefFoundError e) {
            // expected on JDK 7 and lower, parent should have printed the message for user
        }

        if (args.length < 3) {
            throw new IllegalStateException("Expected three arguments");
        }

        String host = args[0];
        int port = Integer.valueOf(args[1]);
        int token = Integer.valueOf(args[2]);

        BinaryLinkClient link = new BinaryLinkClient(host, port);
        Runtime.getRuntime().addShutdownHook(new CloseBinaryLinkHook(link));

        TestConfig config = link.nextJob(token);
        new TestExecutor(0, link, false).runEmbedded(config);
    }

    /**
     * Shutdown hook dedicated to properly closing the binary link.
     * Can be used for regular or exceptional shutdown.
     */
    private static class CloseBinaryLinkHook extends Thread {
        private final BinaryLinkClient link;

        CloseBinaryLinkHook(BinaryLinkClient link) {
            this.link = link;
        }

        @Override
        public void run() {
            try {
                link.close();
            } catch (IOException e) {
                // IOException on closing the link means we can only communicate the
                // failure via the exit code.
                Runtime.getRuntime().halt(1);
            }
        }
    }

}
