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
import org.openjdk.jcstress.os.*;
import org.openjdk.jcstress.util.*;
import org.openjdk.jcstress.vm.CompileMode;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.*;
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

    static final AtomicInteger ID = new AtomicInteger();

    private final BinaryLinkServer server;
    private final Verbosity verbosity;
    private final TestResultCollector sink;
    private final Scheduler scheduler;

    private final Map<String, VM> vmByToken;
    private final Object notifyLock;

    public TestExecutor(Verbosity verbosity, TestResultCollector sink, Scheduler scheduler) throws IOException {
        this.verbosity = verbosity;
        this.sink = sink;
        this.vmByToken = new ConcurrentHashMap<>();
        this.scheduler = scheduler;
        this.notifyLock = new Object();

        server = new BinaryLinkServer(new ServerListener() {
            @Override
            public TestConfig onJobRequest(String token) {
                return vmByToken.get(token).jobRequest();
            }

            @Override
            public void onResult(String token, TestResult result) {
                vmByToken.get(token).recordResult(result);
                notifyChanged();
            }
        });
    }

    private void awaitNotification() {
        synchronized (notifyLock) {
            try {
                // Wait one second and then unblock for extra safety
                notifyLock.wait(1000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    private void notifyChanged() {
        synchronized (notifyLock) {
            notifyLock.notify();
        }
    }

    public void runAll(List<TestConfig> configs) {
        // Build the scheduling classes maps
        Multimap<SchedulingClass, TestConfig> byScl = new HashMultimap<>();
        List<SchedulingClass> scls = new ArrayList<>();

        {
            Set<SchedulingClass> uniqueScls = new HashSet<>();

            for (TestConfig cfg : configs) {
                byScl.put(cfg.getSchedulingClass(), cfg);
                uniqueScls.add(cfg.getSchedulingClass());
            }

            // Try the largest scheduling classes first
            scls.addAll(uniqueScls);
            Collections.sort(scls, Comparator.comparing(SchedulingClass::numActors).reversed());
        }

        while (!byScl.isEmpty()) {

            // Roll over the scheduling classes and try to greedily cram most
            // of the tasks for it. This exits when no scheduling classes can fit
            // the current state of the machine.
            for (SchedulingClass scl : scls) {
                while (byScl.containsKey(scl)) {
                    CPUMap cpuMap = scheduler.tryAcquire(scl);
                    if (cpuMap == null) {
                        // No more scheduling for this class
                        break;
                    }

                    TestConfig cfg = byScl.removeLast(scl);
                    cfg.setCPUMap(cpuMap);
                    String token = "fork-token-" + ID.incrementAndGet();
                    VM vm = new VM(server.getHost(), server.getPort(), token, cfg, cpuMap);
                    vmByToken.put(token, vm);
                    vm.start();
                }
            }

            // Wait until any VM finishes before rescheduling
            while (!processReadyVMs()) {
                awaitNotification();
            }
        }

        // Wait until all threads are done, which means everything got processed
        while (!vmByToken.isEmpty()) {
            while (!processReadyVMs()) {
                awaitNotification();
            }
        }

        server.terminate();
    }

    private boolean processReadyVMs() {
        boolean reclaimed = false;
        for (VM vm : vmByToken.values()) {
            if (vm.checkCompleted(sink)) {
                vmByToken.remove(vm.token, vm);
                scheduler.release(vm.cpuMap);
                reclaimed = true;
            }
        }
        return reclaimed;
    }

    private class VM {
        private final String host;
        private final int port;
        private final String token;
        private File compilerDirectives;
        private final TestConfig task;
        private final CPUMap cpuMap;
        private Process process;
        private boolean processed;
        private IOException pendingException;
        private TestResult result;
        private InputStreamCollector errCollector;
        private InputStreamCollector outCollector;

        public VM(String host, int port, String token, TestConfig task, CPUMap cpuMap) {
            this.host = host;
            this.port = port;
            this.token = token;
            this.cpuMap = cpuMap;
            this.task = task;
            if (VMSupport.compilerDirectivesAvailable()) {
                try {
                    generateDirectives();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        void generateDirectives() throws IOException {
            compilerDirectives = File.createTempFile("jcstress", "directives");

            // Register these files for removal in case we terminate through the uncommon path
            compilerDirectives.deleteOnExit();

            PrintWriter pw = new PrintWriter(compilerDirectives);
            pw.println("[");

            // The task loop:
            pw.println("  {");
            pw.println("    match: \"" + task.generatedRunnerName + "::" + JCStressTestProcessor.TASK_LOOP_PREFIX + "*\",");

            // Avoid inlining the run loop, it should be compiled as separate hot code
            pw.println("    inline: \"-" + task.generatedRunnerName + "::" + JCStressTestProcessor.RUN_LOOP_PREFIX + "*\",");

            // Force inline the auxiliary methods and classes in the run loop
            pw.println("    inline: \"+" + task.generatedRunnerName + "::" + JCStressTestProcessor.AUX_PREFIX + "*\",");
            pw.println("    inline: \"+" + WorkerSync.class.getName() + "::*\",");
            pw.println("    inline: \"+java.util.concurrent.atomic.*::*\",");

            // The test is running in resource-constrained JVM. Block the task loop execution until
            // compiled code is available. This would allow compilers to work in relative peace.
            pw.println("    BackgroundCompilation: false,");

            pw.println("  },");

            // Force inline everything from WorkerSync. WorkerSync does not use anything
            // too deeply, so inlining everything is fine.
            pw.println("  {");
            pw.println("    match: \"" + WorkerSync.class.getName() + "::*" + "\",");
            pw.println("    inline: \"+*::*\",");

            // The test is running in resource-constrained JVM. Block the WorkerSync execution until
            // compiled code is available. This would allow compilers to work in relative peace.
            pw.println("    BackgroundCompilation: false,");

            pw.println("  },");

            // The run loops:
            int cm = task.getCompileMode();
            for (int a = 0; a < task.threads; a++) {
                String an = task.actorNames.get(a);

                pw.println("  {");
                pw.println("    match: \"" + task.generatedRunnerName + "::" + JCStressTestProcessor.RUN_LOOP_PREFIX + an + "\",");
                pw.println("    inline: \"+" + task.generatedRunnerName + "::" + JCStressTestProcessor.AUX_PREFIX + "*\",");

                // Force inline of actor methods if run in compiled mode: this would inherit
                // compiler for them. Forbid inlining of actor methods in interpreted mode:
                // this would make sure that while actor methods are running in interpreter,
                // the run loop still runs in compiled mode, running faster. The call to interpreted
                // method would happen anyway, even though through c2i transition.
                if (CompileMode.isInt(cm, a)) {
                    pw.println("    inline: \"-" + task.name + "::" + an + "\",");
                } else {
                    pw.println("    inline: \"+" + task.name + "::" + an + "\",");
                }

                // Run loop should be compiled with C2? Forbid C1 compilation then.
                if (CompileMode.isC2(cm, a)) {
                    pw.println("    c1: {");
                    pw.println("      Exclude: true,");
                    pw.println("    },");
                }

                // Run loop should be compiled with C1? Forbid C2 compilation then.
                if (CompileMode.isC1(cm, a)) {
                    pw.println("    c2: {");
                    pw.println("      Exclude: true,");
                    pw.println("    },");
                }

                if (VMSupport.printAssemblyAvailable() && verbosity.printAssembly() && !CompileMode.isInt(cm, a)) {
                    pw.println("    PrintAssembly: true,");
                }

                // The test is running in resource-constrained JVM. Block the run loop execution until
                // compiled code is available. This would allow compilers to work in relative peace.
                pw.println("    BackgroundCompilation: false,");

                pw.println("  },");
            }

            for (int a = 0; a < task.threads; a++) {
                String an = task.actorNames.get(a);

                // If this actor runs in interpreted mode, then actor method should not be compiled.
                // Allow run loop to be compiled with the best compiler available.
                if (CompileMode.isInt(cm, a)) {
                    pw.println("  {");
                    pw.println("    match: \"+" + task.name + "::" + an + "\",");
                    pw.println("    c1: {");
                    pw.println("      Exclude: true,");
                    pw.println("    },");
                    pw.println("    c2: {");
                    pw.println("      Exclude: true,");
                    pw.println("    },");
                    pw.println("  },");
                }
            }
            pw.println("]");
            pw.flush();
            pw.close();
        }

        void start() {
            try {
                List<String> command = new ArrayList<>();

                if (OSSupport.taskSetAvailable() && (task.shClass.mode() != AffinityMode.NONE)) {
                    command.add("taskset");
                    command.add("-c");
                    command.add(cpuMap.globalAffinityMap());
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
                process = pb.start();

                // start the stream drainers and read the streams into memory;
                // makes little sense to write them to files, since we would be
                // reading them back soon anyway
                errCollector = new InputStreamCollector(process.getErrorStream());
                outCollector = new InputStreamCollector(process.getInputStream());

                errCollector.start();
                outCollector.start();

            } catch (IOException ex) {
                pendingException = ex;
            }
        }

        public synchronized TestConfig jobRequest() {
            if (processed) {
                return null;
            }
            processed = true;
            return getTask();
        }

        public synchronized TestConfig getTask() {
            return task;
        }

        public synchronized boolean checkCompleted(TestResultCollector sink) {
            // There is a pending exception that terminated the target VM.
            if (pendingException != null) {
                result = new TestResult(task, Status.VM_ERROR);
                result.addMessage(pendingException.getMessage());
                sink.add(result);
                return true;
            }

            // Process is still alive, no need to ask about the status.
            if (result == null && process.isAlive()) {
                return false;
            }

            // Try to poll the exit code, and fail if it's not zero.
            try {
                int ecode = process.waitFor();

                outCollector.join();
                errCollector.join();

                if (ecode != 0) {
                    result = new TestResult(task, Status.VM_ERROR);
                    result.addMessage("Failed with error code " + ecode);
                }
                result.addVMOuts(outCollector.getOutput());
                result.addVMErrs(errCollector.getOutput());
                sink.add(result);
            } catch (InterruptedException ex) {
                result = new TestResult(task, Status.VM_ERROR);
                result.addMessage(ex.getMessage());
                sink.add(result);
            } finally {
                // The process is definitely dead, remove the temporary files.
                if (compilerDirectives != null) {
                    compilerDirectives.delete();
                }
            }
            return true;
        }

        public synchronized void recordResult(TestResult r) {
            if (result != null) {
                throw new IllegalStateException("VM had already published a result.");
            }
            result = r;
        }
    }

}
