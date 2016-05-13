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

import org.openjdk.jcstress.infra.Scheduler;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.*;
import org.openjdk.jcstress.infra.grading.ConsoleReportPrinter;
import org.openjdk.jcstress.infra.grading.ExceptionReportPrinter;
import org.openjdk.jcstress.infra.grading.HTMLReportPrinter;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.link.BinaryLinkServer;
import org.openjdk.jcstress.util.InputStreamDrainer;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * JCStress main entry point.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class JCStress {
    final ExecutorService pool;
    final PrintStream out;
    final Options opts;

    public JCStress(Options opts) {
        this.opts = opts;
        this.pool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("worker" + id.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
        out = System.out;
    }

    class TestCfgTask implements Scheduler.ScheduledTask {
        private final TestConfig cfg;
        private final BinaryLinkServer server;
        private final TestResultCollector sink;

        public TestCfgTask(TestConfig cfg, BinaryLinkServer server, TestResultCollector sink) {
            this.cfg = cfg;
            this.server = server;
            this.sink = sink;
        }

        @Override
        public int getTokens() {
            return cfg.threads;
        }

        @Override
        public void run() {
            switch (cfg.runMode) {
                case EMBEDDED:
                    runEmbedded(cfg, sink);
                    break;
                case FORKED:
                    runForked(cfg, server, sink);
                    break;
            }
        }
    }

    public void run() throws Exception {
        VMSupport.initSupport();
        VMSupport.detectAvailableVMModes();

        opts.printSettingsOn(out);

        SortedSet<String> tests = getTests();
        List<TestConfig> configs = prepareRunProgram(tests);

        ConsoleReportPrinter printer = new ConsoleReportPrinter(opts, new PrintWriter(out, true), tests.size(), configs.size());
        DiskWriteCollector diskCollector = new DiskWriteCollector(opts.getResultFile());
        TestResultCollector sink = MuxCollector.of(printer, diskCollector);

        BinaryLinkServer server = new BinaryLinkServer(sink);

        Scheduler scheduler = new Scheduler(opts.getUserCPUs());
        for (TestConfig cfg : configs) {
            server.addTask(cfg);
            scheduler.schedule(new TestCfgTask(cfg, server, sink));
        }
        scheduler.waitFinish();

        server.terminate();

        diskCollector.close();

        parseResults();
    }

    public void parseResults() throws Exception {
        out.println();
        out.println("Reading the results back... ");

        InProcessCollector collector = new InProcessCollector();
        new DiskReadCollector(opts.getResultFile(), collector).dump();

        out.println("Generating the report... ");

        HTMLReportPrinter p = new HTMLReportPrinter(opts, collector);
        p.parse();

        out.println();
        out.println();
        out.println("Look at " + opts.getResultDest() + "index.html for the complete run results.");
        out.println();

        out.println("Will throw any pending exceptions at this point.");
        ExceptionReportPrinter e = new ExceptionReportPrinter(collector);
        e.parse();

        out.println("Done.");
    }

    private List<TestConfig> prepareRunProgram(Set<String> tests) {
        List<TestConfig> configs = new ArrayList<>();
        if (opts.shouldFork()) {
            List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String test : tests) {
                for (List<String> jvmArgs : VMSupport.getAvailableVMModes()) {
                    List<String> fullArgs = new ArrayList<>();
                    fullArgs.addAll(inputArgs);
                    fullArgs.addAll(jvmArgs);
                    for (int f = 0; f < opts.getForks(); f++) {
                        configs.add(new TestConfig(opts, TestList.getInfo(test), TestConfig.RunMode.FORKED, f, fullArgs));
                    }
                }
            }
        } else {
            for (String test : tests) {
                TestInfo info = TestList.getInfo(test);
                TestConfig.RunMode mode = info.requiresFork() ? TestConfig.RunMode.FORKED : TestConfig.RunMode.EMBEDDED;
                configs.add(new TestConfig(opts, info, mode, -1, Collections.emptyList()));
            }
        }
        return configs;
    }

    void runForked(TestConfig config, BinaryLinkServer server, TestResultCollector collector) {
        try {
            List<String> command = new ArrayList<>();

            // basic Java line
            command.addAll(VMSupport.getJavaInvokeLine());

            // jvm args
            command.addAll(config.jvmArgs);

            command.add(ForkedMain.class.getName());

            command.add(server.getHost());
            command.add(String.valueOf(server.getPort()));

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
                collector.add(result);
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void runEmbedded(TestConfig config, TestResultCollector collector) {
        try {
            Class<?> aClass = Class.forName(config.generatedRunnerName);
            Constructor<?> cnstr = aClass.getConstructor(TestConfig.class, TestResultCollector.class, ExecutorService.class);
            Runner<?> o = (Runner<?>) cnstr.newInstance(config, collector, pool);
            o.run();
        } catch (Exception ex) {
            throw new IllegalStateException("Should have been handled within the Runner");
        }
    }

    public SortedSet<String> getTests() {
        String filter = opts.getTestFilter();
        SortedSet<String> s = new TreeSet<>();

        Pattern pattern = Pattern.compile(filter);
        for (String testName : TestList.tests()) {
            if (pattern.matcher(testName).find()) {
                s.add(testName);
            }
        }
        return s;
   }

}
