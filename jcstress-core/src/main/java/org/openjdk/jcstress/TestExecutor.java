/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jcstress.link.BinaryLinkServer;
import org.openjdk.jcstress.util.InputStreamDrainer;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestExecutor {

    private final ExecutorService pool;
    private final Semaphore semaphore;
    private final BinaryLinkServer server;
    private final int maxThreads;
    private final TestResultCollector sink;

    public TestExecutor(int maxThreads, TestResultCollector sink, boolean possiblyForked) throws IOException {
        this.maxThreads = maxThreads;
        this.sink = sink;
        this.pool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("jcstress-worker-" + id.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });

        if (possiblyForked) {
            semaphore = new Semaphore(maxThreads);
            server = new BinaryLinkServer(maxThreads, sink);
        } else {
            semaphore = null;
            server = null;
        }
    }

    void runForked(TestConfig config) {
        try {
            List<String> command = new ArrayList<>();

            // basic Java line
            command.addAll(VMSupport.getJavaInvokeLine());

            // jvm args
            command.addAll(config.jvmArgs);

            command.add(ForkedMain.class.getName());

            command.add(server.getHost());
            command.add(String.valueOf(server.getPort()));

            // which config should the forked VM pull?
            command.add(String.valueOf(config.uniqueToken));

            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = pb.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), baos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), baos);

            errDrainer.start();
            outDrainer.start();

            int ecode = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            if (ecode != 0) {
                // Test had failed, record this.
                TestResult result = new TestResult(config, Status.VM_ERROR, -1);
                result.addAuxData(new String(baos.toByteArray()).trim());
                sink.add(result);
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void submit(TestConfig cfg) throws InterruptedException {
        if (server == null || semaphore == null) {
            throw new IllegalStateException("Embedded runner cannot accept tasks");
        }

        // Make fat tasks bypass in exclusive mode:
        final int threads = Math.min(cfg.threads, maxThreads);
        semaphore.acquire(threads);

        pool.submit(() -> {
            try {
                switch (cfg.runMode) {
                    case EMBEDDED:
                        runEmbedded(cfg);
                        break;
                    case FORKED:
                        server.addTask(cfg);
                        runForked(cfg);
                        break;
                }
            } finally {
                semaphore.release(threads);
            }
        });
    }

    public void waitFinish() throws InterruptedException {
        if (server == null || semaphore == null) {
            throw new IllegalStateException("Embedded runner cannot accept tasks");
        }
        semaphore.acquire(maxThreads);
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.DAYS);
        server.terminate();
    }

    public void runEmbedded(TestConfig config) {
        try {
            Class<?> aClass = Class.forName(config.generatedRunnerName);
            Constructor<?> cnstr = aClass.getConstructor(TestConfig.class, TestResultCollector.class, ExecutorService.class);
            Runner<?> o = (Runner<?>) cnstr.newInstance(config, sink, pool);
            o.run();
        } catch (ClassFormatError e) {
            TestResult result = new TestResult(config, Status.API_MISMATCH, 0);
            result.addAuxData(e.getMessage());
            sink.add(result);
        } catch (Exception ex) {
            TestResult result = new TestResult(config, Status.TEST_ERROR, 0);
            result.addAuxData(ex.getMessage());
            sink.add(result);
        }
    }

}
