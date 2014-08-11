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
import org.openjdk.jcstress.infra.collectors.DiskReadCollector;
import org.openjdk.jcstress.infra.collectors.DiskWriteCollector;
import org.openjdk.jcstress.infra.collectors.InProcessCollector;
import org.openjdk.jcstress.infra.collectors.MuxCollector;
import org.openjdk.jcstress.infra.collectors.NetworkInputCollector;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.grading.ConsoleReportPrinter;
import org.openjdk.jcstress.infra.grading.ExceptionReportPrinter;
import org.openjdk.jcstress.infra.grading.HTMLReportPrinter;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.InputStreamDrainer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
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
    volatile NetworkInputCollector networkCollector;
    volatile Scheduler scheduler;

    public JCStress() {
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

    public void run(Options opts) throws Exception {
        SortedSet<String> tests = getTests(opts.getTestFilter());

        if (!opts.shouldParse()) {
            opts.printSettingsOn(out);

            ConsoleReportPrinter printer = new ConsoleReportPrinter(opts, new PrintWriter(out, true), tests.size());
            DiskWriteCollector diskCollector = new DiskWriteCollector(opts.getResultFile());
            TestResultCollector sink = MuxCollector.of(printer, diskCollector);

            networkCollector = new NetworkInputCollector(sink);

            scheduler = new Scheduler(opts.getUserCPUs());

            if (opts.shouldFork()) {
                for (String test : tests) {
                    for (int f = 0; f < opts.getForks(); f++) {
                        runForked(opts, test, sink);
                    }
                }
            } else {
                run(opts, tests, false, sink);
            }

            scheduler.waitFinish();
            networkCollector.terminate();

            diskCollector.close();
        }

        out.println("Reading the results back... ");

        InProcessCollector collector = new InProcessCollector();
        new DiskReadCollector(opts.getResultFile(), collector).dump();

        out.println("Generating the report... ");

        HTMLReportPrinter p = new HTMLReportPrinter(opts, collector);
        p.parse();

        out.println("Look at " + opts.getResultDest() + "index.html for the complete run results.");
        out.println();

        out.println("Will throw any pending exceptions at this point.");
        ExceptionReportPrinter e = new ExceptionReportPrinter(opts, collector);
        e.parse();

        out.println("Done.");
    }

    void runForked(final Options opts, final String test, final TestResultCollector collector) {
        try {
            scheduler.schedule(new Scheduler.ScheduledTask() {
                @Override
                public int getTokens() {
                    return TestList.getInfo(test).threads();
                }

                @Override
                public void run() {
                    runForked0(opts, test, collector);
                }
            });
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    void runForked0(Options opts, String test, TestResultCollector collector) {
        try {
            Collection<String> commandString = getSeparateExecutionCommand(opts, test);
            Process p = Runtime.getRuntime().exec(commandString.toArray(new String[commandString.size()]));

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
                TestResult result = new TestResult(test, Status.VM_ERROR);
                String s = new String(baos.toByteArray()).trim();
                result.addAuxData(s);
                collector.add(result);
            }

        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void run(Options opts, boolean alreadyForked, TestResultCollector collector) throws Exception {
        run(opts, getTests(opts.getTestFilter()), alreadyForked, collector);
    }

    public void async(final Runner runner, final int threads) throws ExecutionException, InterruptedException {
        if (scheduler == null) {
            runner.run();
            return;
        }

        scheduler.schedule(new Scheduler.ScheduledTask() {
            @Override
            public int getTokens() {
                return threads;
            }

            @Override
            public void run() {
                runner.run();
            }
        });
    }

    private void run(Options opts, Collection<String> tests, boolean alreadyForked, TestResultCollector collector) throws Exception {
        for (String test : tests) {
            TestInfo info = TestList.getInfo(test);
            if (info.requiresFork() && !alreadyForked && !opts.shouldNeverFork()) {
                runForked(opts, test, collector);
            } else {
                Class<?> aClass = Class.forName(info.generatedRunner());
                Constructor<?> cnstr = aClass.getConstructor(Options.class, TestResultCollector.class, ExecutorService.class);
                Runner<?> o = (Runner<?>) cnstr.newInstance(opts, collector, pool);
                async(o, info.threads());
            }
        }
    }

    public Collection<String> getSeparateExecutionCommand(Options opts, String test) {
        List<String> command = new ArrayList<>();

        // jvm path
        command.add(getDefaultJvm());

        // jvm classpath
        command.add("-cp");
        if (isWindows()) {
            command.add('"' + System.getProperty("java.class.path") + '"');
        } else {
            command.add(System.getProperty("java.class.path"));
        }

        // jvm args
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());

        String appendJvmArgs = opts.getAppendJvmArgs();
        if (appendJvmArgs.length() > 0) {
            command.addAll(Arrays.asList(appendJvmArgs.split("\\s")));
        }

        command.add(ForkedMain.class.getName());

        // add jcstress options
        command.addAll(opts.buildForkedCmdLine());

        command.add("-t");
        command.add(test);

        command.add("--hostName");
        command.add(networkCollector.getHost());

        command.add("--hostPort");
        command.add(String.valueOf(networkCollector.getPort()));

        return command;
    }

    private String getDefaultJvm() {
        StringBuilder javaExecutable = new StringBuilder();
        javaExecutable.append(System.getProperty("java.home"));
        javaExecutable.append(File.separator);
        javaExecutable.append("bin");
        javaExecutable.append(File.separator);
        javaExecutable.append("java");
        javaExecutable.append(isWindows() ? ".exe" : "");
        return javaExecutable.toString();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").contains("indows");
    }

    static SortedSet<String> getTests(final String filter) {
        SortedSet<String> s = new TreeSet<>();

        Pattern pattern = Pattern.compile(filter);
        for (String testName : TestList.tests()) {
            if (pattern.matcher(testName).matches()) {
                s.add(testName);
            }
        }
        return s;
   }

}
