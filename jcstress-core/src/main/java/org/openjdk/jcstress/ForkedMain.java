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

import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.ForkedTestConfig;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.link.BinaryLinkClient;
import org.openjdk.jcstress.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point for the forked VM run.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ForkedMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalStateException("Expected three arguments");
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String token = args[2];

        BinaryLinkClient link = new BinaryLinkClient(host, port);

        ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("jcstress-worker-" + id.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });

        ForkedTestConfig config = link.nextJob(token);

        TestResult result;

        try {
            Class<?> aClass = Class.forName(config.generatedRunnerName);
            Constructor<?> cnstr = aClass.getConstructor(ForkedTestConfig.class, ExecutorService.class);
            Runner<?> o = (Runner<?>) cnstr.newInstance(config, pool);
            result = o.run();
        } catch (ClassFormatError | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            result = new TestResult(Status.API_MISMATCH);
            result.addMessage(StringUtils.getStacktrace(e));
        } catch (Throwable ex) {
            result = new TestResult(Status.TEST_ERROR);
            result.addMessage(StringUtils.getStacktrace(ex));
        }

        link.doneResult(token, result);
    }

}
