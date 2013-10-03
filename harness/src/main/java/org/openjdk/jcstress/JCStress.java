/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jcstress.infra.Result;
import org.openjdk.jcstress.infra.Scheduler;
import org.openjdk.jcstress.infra.Status;
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
import org.openjdk.jcstress.infra.runners.Actor1_Runner;
import org.openjdk.jcstress.infra.runners.Actor2_Arbiter1_Runner;
import org.openjdk.jcstress.infra.runners.Actor2_Runner;
import org.openjdk.jcstress.infra.runners.Actor3_Runner;
import org.openjdk.jcstress.infra.runners.Actor4_Runner;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TerminationRunner;
import org.openjdk.jcstress.tests.Actor1_Test;
import org.openjdk.jcstress.tests.Actor2_Arbiter1_Test;
import org.openjdk.jcstress.tests.Actor2_Test;
import org.openjdk.jcstress.tests.Actor3_Test;
import org.openjdk.jcstress.tests.Actor4_Test;
import org.openjdk.jcstress.tests.ConcurrencyTest;
import org.openjdk.jcstress.tests.TerminationTest;
import org.openjdk.jcstress.util.InputStreamDrainer;
import org.openjdk.jcstress.util.Reflections;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
    private final PrintStream out;
    NetworkInputCollector networkCollector;
    Scheduler scheduler;

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
        SortedSet<Class<? extends ConcurrencyTest>> tests = filterTests(opts.getTestFilter(), ConcurrencyTest.class);

        if (!opts.shouldParse()) {
            opts.printSettingsOn(out);

            ConsoleReportPrinter printer = new ConsoleReportPrinter(opts, new PrintWriter(out, true), tests.size());
            DiskWriteCollector diskCollector = new DiskWriteCollector(opts.getResultFile());
            TestResultCollector sink = MuxCollector.of(printer, diskCollector);

            networkCollector = new NetworkInputCollector(sink);

            // FIXME: Scheduler will stuck itself if there is a test requiring more than $userCPUs.
            scheduler = new Scheduler(opts.getUserCPUs());

            if (opts.shouldFork()) {
                for (Class<? extends ConcurrencyTest> test : tests) {
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

    void runForked(final Options opts, final Class<? extends ConcurrencyTest> test, final TestResultCollector collector) {
        try {
            scheduler.schedule(new Scheduler.ScheduledTask() {
                @Override
                public int getTokens() {
                    if (Actor1_Test.class.isAssignableFrom(test)) return 1;
                    if (Actor2_Test.class.isAssignableFrom(test)) return 2;
                    if (Actor3_Test.class.isAssignableFrom(test)) return 3;
                    if (Actor4_Test.class.isAssignableFrom(test)) return 4;
                    if (Actor2_Arbiter1_Test.class.isAssignableFrom(test)) return 3;
                    if (TerminationTest.class.isAssignableFrom(test)) return 2;
                    return 1;
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

    void runForked0(Options opts, Class<? extends ConcurrencyTest> test, TestResultCollector collector) {
        try {
            Collection<String> commandString = getSeparateExecutionCommand(opts, test.getName());
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
                TestResult result = new TestResult(test.getName(), Status.VM_ERROR);
                String s = new String(baos.toByteArray()).trim();
                result.addAuxData(s);
                collector.add(result);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void run(Options opts, boolean alreadyForked, TestResultCollector collector) throws Exception {
        run(opts, filterTests(opts.getTestFilter(), ConcurrencyTest.class), alreadyForked, collector);
    }

    public void async(final Runner runner) throws ExecutionException, InterruptedException {
        if (scheduler == null) {
            runner.run();
            return;
        }

        scheduler.schedule(new Scheduler.ScheduledTask() {
            @Override
            public int getTokens() {
                return runner.requiredThreads();
            }

            @Override
            public void run() {
                try {
                    runner.run();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                } catch (ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void run(Options opts, Set<Class<? extends ConcurrencyTest>> tests, boolean alreadyForked, TestResultCollector collector) throws Exception {

        for (Class<? extends ConcurrencyTest> test : tests) {
            if (Actor2_Arbiter1_Test.class.isAssignableFrom(test)) {
                @SuppressWarnings("unchecked")
                Actor2_Arbiter1_Test<Object, ? extends Result> obj = (Actor2_Arbiter1_Test<Object, ? extends Result>) test.newInstance();
                async(new Actor2_Arbiter1_Runner(opts, obj, collector, pool));
            }

            if (Actor1_Test.class.isAssignableFrom(test)) {
                @SuppressWarnings("unchecked")
                Actor1_Test<Object, ? extends Result> obj = (Actor1_Test<Object, ? extends Result>) test.newInstance();
                async(new Actor1_Runner(opts, obj, collector, pool));
            }

            if (Actor2_Test.class.isAssignableFrom(test)) {
                @SuppressWarnings("unchecked")
                Actor2_Test<Object, ? extends Result> obj = (Actor2_Test<Object, ? extends Result>) test.newInstance();
                async(new Actor2_Runner(opts, obj, collector, pool));
            }

            if (Actor3_Test.class.isAssignableFrom(test)) {
                @SuppressWarnings("unchecked")
                Actor3_Test<Object, ? extends Result> obj = (Actor3_Test<Object, ? extends Result>) test.newInstance();
                async(new Actor3_Runner(opts, obj, collector, pool));
            }

            if (Actor4_Test.class.isAssignableFrom(test)) {
                @SuppressWarnings("unchecked")
                Actor4_Test<Object, ? extends Result> obj = (Actor4_Test<Object, ? extends Result>) test.newInstance();
                async(new Actor4_Runner(opts, obj, collector, pool));
            }

            if (TerminationTest.class.isAssignableFrom(test)) {
                if (!alreadyForked && !opts.shouldNeverFork()) {
                    for (int f = 0; f < opts.getForks(); f++) {
                        runForked(opts, test, collector);
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    TerminationTest<Object> obj = (TerminationTest<Object>) test.newInstance();
                    async(new TerminationRunner<Object>(opts, obj, collector, pool));
                }
            }
        }

    }

    public Collection<String> getSeparateExecutionCommand(Options opts, String test) {
        Properties props = System.getProperties();
        String javaHome = (String) props.get("java.home");
        String separator = File.separator;
        String osName = props.getProperty("os.name");
        boolean isOnWindows = osName.contains("indows");
        String platformSpecificBinaryPostfix = isOnWindows ? ".exe" : "";

        String classPath = (String) props.get("java.class.path");

        if (isOnWindows) {
            classPath = '"' + classPath + '"';
        }

        // else find out which one parent is and use that
        StringBuilder javaExecutable = new StringBuilder();
        javaExecutable.append(javaHome);
        javaExecutable.append(separator);
        javaExecutable.append("bin");
        javaExecutable.append(separator);
        javaExecutable.append("java");
        javaExecutable.append(platformSpecificBinaryPostfix);
        String javaExecutableString = javaExecutable.toString();


        // else use same jvm args given to this runner
        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> args = RuntimemxBean.getInputArguments();

        // assemble final process command

        List<String> command = new ArrayList<String>();
        command.add(javaExecutableString);

        command.addAll(args);

        command.add("-cp");
        command.add(classPath);
        String appendJvmArgs = opts.getAppendJvmArgs();
        if (appendJvmArgs.length() > 0) {
            command.addAll(Arrays.asList(appendJvmArgs.split("\\s")));
        }
        command.add(ForkedMain.class.getName());
        command.addAll(opts.buildForkedCmdLine());
        command.add("-t");
        command.add(test);

        command.add("--hostName");
        command.add(networkCollector.getHost());

        command.add("--hostPort");
        command.add(String.valueOf(networkCollector.getPort()));

        return command;
    }

    static <T> SortedSet<Class<? extends T>> filterTests(final String filter, Class<T> klass) {
        SortedSet<Class<? extends T>> s = new TreeSet<Class<? extends T>>(new Comparator<Class<? extends T>>() {
            @Override
            public int compare(Class<? extends T> o1, Class<? extends T> o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        // speculatively handle the case when there is a direct hit
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> k = (Class<? extends T>) Class.forName(filter);
            if (klass.isAssignableFrom(k)) {
                s.add(k);
            }

            return s;
        } catch (ClassNotFoundException e) {
            // continue
        }

        // God I miss both diamonds and lambdas here.

        Pattern pattern = Pattern.compile(filter);

        for (Class k : Reflections.findAllClassesImplementing(klass, "org.openjdk.jcstress")) {
            if (!pattern.matcher(k.getName()).matches()) {
                continue;
            }
            if (Modifier.isAbstract(k.getModifiers())) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends T> k1 = k;

            s.add(k1);
        }

        return s;
    }
}