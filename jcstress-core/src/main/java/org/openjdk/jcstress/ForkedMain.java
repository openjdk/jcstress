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
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.link.BinaryLinkClient;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.vm.WhiteBoxSupport;

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
        try {
            WhiteBoxSupport.initSafely();
        } catch (NoClassDefFoundError e) {
            // expected on JDK 7 and lower, parent should have printed the message for user
        }

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

        TestResultCollector sink = result -> link.addResult(token, result);
        TestConfig config = link.nextJob(token);

        try {
            Class<?> aClass = Class.forName(config.generatedRunnerName);
            Constructor<?> cnstr = aClass.getConstructor(TestConfig.class, TestResultCollector.class, ExecutorService.class);
            Runner<?> o = (Runner<?>) cnstr.newInstance(config, sink, pool);
            o.run();
        } catch (ClassFormatError | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            TestResult result = new TestResult(config, Status.API_MISMATCH);
            result.addMessage(StringUtils.getStacktrace(e));
            link.addResult(token, result);
        } catch (Throwable ex) {
            TestResult result = new TestResult(config, Status.TEST_ERROR);
            result.addMessage(StringUtils.getStacktrace(ex));
            link.addResult(token, result);
        }

        link.done(token);
    }

}
