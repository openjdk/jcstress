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
import org.openjdk.jcstress.infra.processors.JCStressTestProcessor;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.WorkerSync;
import org.openjdk.jcstress.link.BinaryLinkServer;
import org.openjdk.jcstress.link.ServerListener;
import org.openjdk.jcstress.vm.CPULayout;
import org.openjdk.jcstress.vm.OSSupport;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages test execution for the entire run.
 * <p>
 * This executor is deliberately single-threaded for two reasons:
 * a) Tests are heavily multithreaded and spawning new threads here may
 * deplete the thread budget sooner rather than later;
 * b) Dead-locks in scheduling logic are more visible without threads;
 */
public class TestExecutor {

    private static final int SPIN_WAIT_DELAY_MS = 100;

    static final AtomicInteger ID = new AtomicInteger();

    private final BinaryLinkServer server;
    private final int maxThreads;
    private final TestResultCollector sink;
    private final EmbeddedExecutor embeddedExecutor;
    private final CPULayout cpuLayout;

    private final Map<String, VM> vmByToken;

    public TestExecutor(int maxThreads, TestResultCollector sink, boolean possiblyForked) throws IOException {
        this.maxThreads = maxThreads;
        this.sink = sink;
        this.vmByToken = new ConcurrentHashMap<>();

        cpuLayout = new CPULayout(maxThreads);

        server = possiblyForked ? new BinaryLinkServer(new ServerListener() {
            @Override
            public TestConfig onJobRequest(String token) {
                return vmByToken.get(token).jobRequest();
            }

            @Override
            public void onResult(String token, TestResult result) {
                sink.add(result);
            }
        }) : null;
        embeddedExecutor = new EmbeddedExecutor(sink, cpuLayout);
    }

    public void runAll(List<TestConfig> configs) {
        for (TestConfig cfg : configs) {
            List<Integer> acquiredCPUs = acquireCPUs(cfg.threads);

            switch (cfg.runMode) {
                case EMBEDDED:
                    embeddedExecutor.submit(cfg, acquiredCPUs);
                    break;
                case FORKED:
                    String token = "fork-token-" + ID.incrementAndGet();
                    VM vm = new VM(server.getHost(), server.getPort(), token, cfg, acquiredCPUs);
                    vmByToken.put(token, vm);
                    vm.start();
                    break;
                default:
                    throw new IllegalStateException("Unknown mode: " + cfg.runMode);
            }
        }

        // Wait until all threads are done, which means everything got processed
        acquireCPUs(maxThreads);

        server.terminate();
    }

    private List<Integer> acquireCPUs(int cpus) {
        List<Integer> acquired;
        while ((acquired = cpuLayout.tryAcquire(cpus)) == null) {
            processReadyVMs();
            try {
                Thread.sleep(SPIN_WAIT_DELAY_MS);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        return acquired;
    }

    private void processReadyVMs() {
        for (VM vm : vmByToken.values()) {
            try {
                if (!vm.checkTermination()) continue;
            } catch (ForkFailedException e) {
                TestConfig task = vm.getTask();
                TestResult result = new TestResult(task, Status.VM_ERROR, -1);
                for (String i : e.getInfo()) {
                    result.addAuxData(i);
                }
                sink.add(result);
            }

            vmByToken.remove(vm.token, vm);
            cpuLayout.release(vm.claimedCPUs);
        }
    }

    private static class VM {
        private final String host;
        private final int port;
        private final String token;
        private final File stdout;
        private final File stderr;
        private final File compilerDirectives;
        private final TestConfig task;
        private final List<Integer> claimedCPUs;
        private Process process;
        private boolean processed;
        private IOException pendingException;

        public VM(String host, int port, String token, TestConfig task, List<Integer> claimedCPUs) {
            this.host = host;
            this.port = port;
            this.token = token;
            this.claimedCPUs = claimedCPUs;
            this.task = task;
            try {
                this.stdout = File.createTempFile("jcstress", "stdout");
                this.stderr = File.createTempFile("jcstress", "stderr");
                this.compilerDirectives = File.createTempFile("jcstress", "directives");

                if (VMSupport.compilerDirectivesAvailable()) {
                    generateDirectives();
                }

                // Register these files for removal in case we terminate through the uncommon path
                this.stdout.deleteOnExit();
                this.stderr.deleteOnExit();
                this.compilerDirectives.deleteOnExit();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        void generateDirectives() {
            try {
                PrintWriter pw = new PrintWriter(compilerDirectives);
                pw.println("[");

                // The task loop:
                //   - avoid inlining the run loop, it should be compiled as hot code
                //   - force inline the auxiliary methods and classes in the run loop
                pw.println("  {");
                pw.println("    match: \"" + task.generatedRunnerName + "::" + JCStressTestProcessor.TASK_LOOP_PREFIX + "*\",");
                pw.println("    inline: \"-" + task.generatedRunnerName + "::" + JCStressTestProcessor.RUN_LOOP_PREFIX + "*\",");
                pw.println("    inline: \"+" + task.generatedRunnerName + "::" + JCStressTestProcessor.AUX_PREFIX + "*\",");
                pw.println("    inline: \"+" + WorkerSync.class.getName() + "::*\",");
                pw.println("    inline: \"+java.util.concurrent.atomic.*::*\",");
                pw.println("  },");

                // Force inline everything from WorkerSync. WorkerSync does not use anything
                // too deeply, so inlining everything is fine.
                pw.println("  {");
                pw.println("    match: \"" + WorkerSync.class.getName() + "::*" + "\",");
                pw.println("    inline: \"+*::*\",");
                pw.println("  },");

                // The run loop:
                //   - force inline of the workload methods
                //   - force inline of sink methods
                for (String an : task.actorNames) {
                    pw.println("  {");
                    pw.println("    match: \"" + task.generatedRunnerName + "::" + JCStressTestProcessor.RUN_LOOP_PREFIX + an + "\",");
                    pw.println("    inline: \"+" + task.name + "::" + an + "\",");
                    pw.println("    inline: \"+" + task.generatedRunnerName + "::" + JCStressTestProcessor.AUX_PREFIX + "*\",");
                    pw.println("  },");
                }
                pw.println("]");
                pw.flush();
                pw.close();
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        void start() {
            try {
                List<String> command = new ArrayList<>();

                if (OSSupport.taskSetAvailable()) {
                    command.add("taskset");
                    command.add("-c");
                    command.add(StringUtils.join(claimedCPUs, ","));
                }

                // basic Java line
                command.addAll(VMSupport.getJavaInvokeLine());

                // jvm args
                command.addAll(task.jvmArgs);

                if (VMSupport.compilerDirectivesAvailable()) {
                    command.add("-XX:CompilerDirectivesFile=" + compilerDirectives.getAbsolutePath());
                }

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
                pendingException = ex;
            }
        }

        boolean checkTermination() {
            if (pendingException != null) {
                throw new ForkFailedException(pendingException.getMessage());
            }

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
                } finally {
                    // The process is definitely dead, remove the temporary files.
                    stdout.delete();
                    stderr.delete();
                }
                return true;
            }
        }

        public synchronized TestConfig jobRequest() {
            if (processed) {
                return null;
            }
            processed = true;
            return getTask();
        }

        public TestConfig getTask() {
            return task;
        }
    }

}
