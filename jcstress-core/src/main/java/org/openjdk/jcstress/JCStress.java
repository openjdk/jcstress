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

import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.*;
import org.openjdk.jcstress.infra.grading.ConsoleReportPrinter;
import org.openjdk.jcstress.infra.grading.ExceptionReportPrinter;
import org.openjdk.jcstress.infra.grading.TextReportPrinter;
import org.openjdk.jcstress.infra.grading.HTMLReportPrinter;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.regex.Pattern;

/**
 * JCStress main entry point.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class JCStress {
    final PrintStream out;
    final Options opts;

    public JCStress(Options opts) {
        this.opts = opts;
        this.out = System.out;
    }

    public void run() throws Exception {
        VMSupport.detectAvailableVMModes(opts.getJvmArgs(), opts.getJvmArgsPrepend());
        if (VMSupport.getAvailableVMModes().isEmpty()) {
            out.println("FATAL: No JVM modes to run with.");
            return;
        }

        VMSupport.initFlags();

        opts.printSettingsOn(out);

        SortedSet<String> tests = getTests();
        List<TestConfig> configs = prepareRunProgram(tests);

        ConsoleReportPrinter printer = new ConsoleReportPrinter(opts, new PrintWriter(out, true), tests.size(), configs.size());
        DiskWriteCollector diskCollector = new DiskWriteCollector(opts.getResultFile());
        TestResultCollector mux = MuxCollector.of(printer, diskCollector);
        SerializedBufferCollector sink = new SerializedBufferCollector(mux);

        TestExecutor executor = new TestExecutor(opts.getUserCPUs(), opts.getBatchSize(), sink, true);
        executor.runAll(configs);

        sink.close();
        diskCollector.close();

        out.println();
        out.println();
        out.println("RUN COMPLETE.");
        out.println();

        parseResults();
    }

    public void parseResults() throws Exception {
        InProcessCollector collector = new InProcessCollector();
        new DiskReadCollector(opts.getResultFile(), collector).dump();

        new TextReportPrinter(opts, collector).work();
        new HTMLReportPrinter(opts, collector).work();

        out.println();
        out.println("HTML report was generated. Look at " + opts.getResultDest() + "index.html for the complete run results.");
        out.println();

        out.println("Will throw any pending exceptions at this point.");
        new ExceptionReportPrinter(collector).work();

        out.println("Done.");
    }

    private List<TestConfig> prepareRunProgram(Set<String> tests) {
        List<TestConfig> configs = new ArrayList<>();
        if (opts.shouldFork()) {
            List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String test : tests) {
                for (Collection<String> jvmArgs : VMSupport.getAvailableVMModes()) {
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

        // Randomize the testing order
        Collections.shuffle(configs);

        return configs;
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
