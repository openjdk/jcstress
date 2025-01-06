/*
 * Copyright (c) 2005, 2014, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jcstress.TestExecutor;
import org.openjdk.jcstress.infra.collectors.TestResult;

import java.io.PrintWriter;


public class FailFastKiller extends CountingResultCollector {

    private final PrintWriter output;
    private final long expectedResults;

    private TestExecutor executor;
    private int numericDeadline;


    public FailFastKiller(Options opts, PrintWriter pw, long expectedResults) {
        this.output = pw;
        this.expectedResults = expectedResults;
        output.println("  FailFast attached as:");
        if (opts.isFailFastAllVariants()) {
            output.println("    all variants, " + opts.getFailFast());
        } else {
            output.println("    whole tests, " + opts.getFailFast());
        }
        this.numericDeadline = Integer.valueOf(opts.getFailFast());
        output.println();
    }

    @Override
    public synchronized void add(TestResult r) {
        addResult(r);
    }

    private void addResult(TestResult r) {
        TestGrading grading = r.grading();
        countResult(r);
        verifyState();
    }

    private void verifyState() {
        String l4 = String.format("(Results: %d planned; %d passed, %d failed, %d soft errs, %d hard errs)",
                expectedResults, passed, failed, softErrors, hardErrors);
        //output.println(l4);
        //output.flush();
        if (passed+failed+softErrors+hardErrors > numericDeadline) {
            executor.setDiedFast();
        }
    }

    public void setExecutor(TestExecutor executor) {
        this.executor = executor;
    }
}
