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
package org.openjdk.jcstress.infra.grading;


import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.InProcessCollector;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Prints HTML reports.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class TextReportPrinter {

    private final InProcessCollector collector;
    private final boolean verbose;
    private final PrintWriter pw;
    private final Set<TestResult> emittedTests;

    public TextReportPrinter(Options opts, InProcessCollector collector) throws FileNotFoundException {
        this.collector = collector;
        this.pw = new PrintWriter(System.out, true);
        this.verbose = opts.isVerbose();
        this.emittedTests = new HashSet<>();
    }

    public void work() throws FileNotFoundException {
        emittedTests.clear();

        List<TestResult> byConfig = ReportUtils.mergedByConfig(collector.getTestResults());

        pw.println("RUN RESULTS:");
        pw.println("------------------------------------------------------------------------------------------------------------------------");
        pw.println();

        printXTests(byConfig,
                "INTERESTING tests",
                "Some interesting behaviors observed. This is for the plain curiosity.",
                r -> r.status() == Status.NORMAL && r.grading().hasInteresting);

        printXTests(byConfig,
                "SPEC tests",
                "Formally acceptable, but surprising results are observed. Implementations going beyond the minimal requirements should have none.",
                r -> r.status() == Status.NORMAL && r.grading().hasSpec);

        printXTests(byConfig,
                "FAILED tests",
                "Strong asserts were violated. Correct implementations should have no assert failures here.",
                r -> r.status() == Status.NORMAL && !r.grading().isPassed);

        printXTests(byConfig,
                "ERROR tests",
                "Tests break for some reason, other than failing the assert. Correct implementations should have none.",
                r -> r.status() != Status.NORMAL && r.status() != Status.API_MISMATCH);

        if (verbose) {
            printXTests(byConfig,
                    "All remaining tests",
                    "Tests that do not fall into any of the previous categories.",
                    r -> !emittedTests.contains(r));
        }

        pw.println("------------------------------------------------------------------------------------------------------------------------");
    }

    private void printXTests(List<TestResult> byName,
                             String header,
                             String subHeader,
                             Predicate<TestResult> predicate) {
        boolean hadHeader = false;

        for (TestResult result : byName) {

            if (predicate.test(result)) {
                if (!hadHeader) {
                    pw.println("*** " + header);
                    pw.println("  " + subHeader);
                    pw.println();
                    hadHeader = true;
                }
                emitTest(result);
            }
        }

        if (hadHeader) {
            pw.println();
        }
    }

    public void emitTest(TestResult result) {
        emittedTests.add(result);
        pw.printf("%10s %s\n", "[" + ReportUtils.statusToLabel(result) + "]", StringUtils.chunkName(result.getName()));
        ReportUtils.printDetails(pw, result, false);
    }

}
