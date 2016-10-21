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
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.link.BinaryLinkServer;
import org.openjdk.jcstress.util.HashMultimap;
import org.openjdk.jcstress.util.Multimap;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages test execution for the entire run.
 *
 * This executor is deliberately single-threaded for two reasons:
 *   a) Tests are heavily multithreaded and spawning new threads here may
 *      deplete the thread budget sooner rather than later;
 *   b) Dead-locks in scheduling logic are more visible without threads;
 */
public class TestExecutor {

    private static final int SPIN_WAIT_DELAY_MS = 100;

    static final AtomicInteger ID = new AtomicInteger();

    private final Semaphore semaphore;
    private final BinaryLinkServer server;
    private final int maxThreads;
    private final int batchSize;
    private final TestResultCollector sink;
    private final Multimap<BatchKey, TestConfig> tasks;
    private final Set<VM> vms;
    private final EmbeddedExecutor embeddedExecutor;

    public TestExecutor(int maxThreads, int batchSize, TestResultCollector sink, boolean possiblyForked) throws IOException {
        this.maxThreads = maxThreads;
        this.batchSize = batchSize;
        this.sink = sink;

        this.tasks = new HashMultimap<>();
        this.vms = Collections.newSetFromMap(new ConcurrentHashMap<>());

        semaphore = new Semaphore(maxThreads);
        server = possiblyForked ? new BinaryLinkServer(maxThreads, sink) : null;
        embeddedExecutor = new EmbeddedExecutor(sink, (cfg) -> semaphore.release(cfg.threads));
    }

    public void runAll(List<TestConfig> configs) throws InterruptedException {
        for (TestConfig cfg : configs) {
            switch (cfg.runMode) {
                case EMBEDDED:
                    waitForMoreThreads(cfg.threads);
                    embeddedExecutor.submit(cfg);
                    break;
                case FORKED:
                    BatchKey batchKey = BatchKey.getFrom(cfg);
                    tasks.put(batchKey, cfg);

                    Collection<TestConfig> curBatch = tasks.get(batchKey);
                    if (curBatch.size() >= batchSize) {
                        tasks.remove(batchKey);
                        doSchedule(batchKey, curBatch);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown mode: " + cfg.runMode);
            }
        }

        // Run down the remaining tasks
        for (BatchKey key : tasks.keys()) {
            Collection<TestConfig> curBatch = tasks.get(key);
            if (!curBatch.isEmpty()) {
                doSchedule(key, curBatch);
            }
        }

        // Wait until all threads are done, which means everything got processed
        waitForMoreThreads(maxThreads);

        server.terminate();
    }

    private void doSchedule(BatchKey batchKey, Collection<TestConfig> configs)  {
        // Make fat tasks bypass in exclusive mode:
        final int threads = Math.min(batchKey.threads, maxThreads);
        waitForMoreThreads(threads);
        startVM(batchKey, configs);
    }

    private void waitForMoreThreads(int threads) {
        while (!semaphore.tryAcquire(threads)) {
            processReadyVMs();
            try {
                Thread.sleep(SPIN_WAIT_DELAY_MS);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    private void startVM(BatchKey batchKey, Collection<TestConfig> configs) {
        String token = "fork-token-" + ID.incrementAndGet();
        server.addTask(token, configs);

        VM vm = new VM(server.getHost(), server.getPort(), batchKey, token);
        vms.add(vm);
        vm.start();
    }

    private void stopVM(VM vm) {
        vms.remove(vm);
        semaphore.release(vm.key.threads);
    }

    private void processReadyVMs() {
        for (VM vm : vms) {
            try {
                if (vm.checkTermination()) {
                    stopVM(vm);
                }
            } catch (ForkFailedException e) {
                // Record the failure for the actual test
                TestConfig failed = server.getCurrentTask(vm.token);
                if (failed != null) {
                    // TODO: Handle the VM bootup failure better, when failed == null
                    TestResult result = new TestResult(failed, Status.VM_ERROR, -1);
                    for (String i : e.getInfo()) {
                        result.addAuxData(i);
                    }
                    sink.add(result);
                }

                stopVM(vm);

                // Remaining tasks from the fork need to get back on queue
                doSchedule(vm.key, server.removePendingTasks(vm.token));
            }
        }
    }

    private static class VM {
        private final String host;
        private final int port;
        private final BatchKey key;
        private final String token;
        private final File stdout;
        private final File stderr;
        private Process process;

        public VM(String host, int port, BatchKey key, String token) {
            this.host = host;
            this.port = port;
            this.key = key;
            this.token = token;
            try {
                this.stdout = File.createTempFile("jcstress", "stdout");
                this.stderr = File.createTempFile("jcstress", "stderr");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        void start() throws ForkFailedException {
            try {
                List<String> command = new ArrayList<>();

                // basic Java line
                command.addAll(VMSupport.getJavaInvokeLine());

                // jvm args
                command.addAll(key.jvmArgs);

                command.add(ForkedMain.class.getName());

                command.add(host);
                command.add(String.valueOf(port));

                // which config should the forked VM pull?
                command.add(token);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectOutput(stdout);
                pb.redirectError(stderr);
                process = pb.start();
            } catch (IOException ex) {
                throw new ForkFailedException(ex.getMessage());
            }
        }

        boolean checkTermination() {
            if (process.isAlive()) {
                return false;
            } else {
                // Try to poll the exit code, and fail if it's not zero.
                try {
                    int ecode = process.waitFor();
                    if (ecode != 0) {
                        List<String> output = new ArrayList<>();
                        try {
                            output.addAll(Files.readAllLines(stdout.toPath()));
                        } catch (IOException e) {
                            output.add("Failed to read stdout: " + e.getMessage());
                        }
                        try {
                            output.addAll(Files.readAllLines(stderr.toPath()));
                        } catch (IOException e) {
                            output.add("Failed to read stderr: " + e.getMessage());
                        }
                        throw new ForkFailedException(output);
                    }
                } catch (InterruptedException ex) {
                    throw new ForkFailedException(ex.getMessage());
                }
                return true;
            }
        }
    }

    static class BatchKey {
        private int threads;
        private List<String> jvmArgs;

        BatchKey(int threads, List<String> jvmArgs) {
            this.threads = threads;
            this.jvmArgs = jvmArgs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BatchKey batchKey = (BatchKey) o;

            if (threads != batchKey.threads) return false;
            return jvmArgs.equals(batchKey.jvmArgs);

        }

        @Override
        public int hashCode() {
            int result = threads;
            result = 31 * result + jvmArgs.hashCode();
            return result;
        }

        static BatchKey getFrom(TestConfig cfg) {
            return new BatchKey(cfg.threads, cfg.jvmArgs);
        }
    }

}
