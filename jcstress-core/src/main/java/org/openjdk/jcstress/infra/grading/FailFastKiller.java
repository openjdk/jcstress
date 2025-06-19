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
import org.openjdk.jcstress.infra.runners.TestConfig;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;


public class FailFastKiller extends CountingResultCollector {

    private static final String INCLUDE_SOFT_ERRORS = "jcstress.foe.countsoft";
    private static final String LIMIT = "jcstress.foe.limit";

    private TestExecutor executor;
    private final double absoluteThreshold;
    private final double relativeThreshold;

    private final boolean includeSoftErrors;


    public FailFastKiller(Options opts, PrintWriter output, List<TestConfig> finalVariants) {
        output.println("  Fail-on-error enabled as:");
        String originalValue = System.getProperty(LIMIT, "1");
        includeSoftErrors = Boolean.parseBoolean(System.getProperty(INCLUDE_SOFT_ERRORS, "false"));
        double userValue = Double.parseDouble(originalValue.replaceAll("%*", ""));
        boolean relative = originalValue.endsWith("%");
        List<TestConfig> variants = Collections.unmodifiableList(finalVariants);
        output.println("    all tests, " + originalValue);
        output.println("    include soft errors: " + includeSoftErrors);
        if (relative) {
            relativeThreshold = userValue;
            absoluteThreshold = (relativeThreshold * (double) variants.size()) / 100d;
        } else {
            absoluteThreshold = (long) userValue;
            relativeThreshold = (absoluteThreshold * 100d) / (double) variants.size();
        }
        output.println("    The suite will terminate once failure rate reaches " + getRelativeThresholdNice() + "% (" + getAbsoluteThresholdNice() + ") of total tests (" + variants.size() + ")");
        output.println();
    }

    @Override
    public synchronized void add(TestResult r) {
        addResult(r);
    }

    private void addResult(TestResult r) {
        countResult(r);
        verifyState(r);
    }

    private void verifyState(TestResult r) {
        long totalFailed = failed + hardErrors;
        if (includeSoftErrors) {
            totalFailed += softErrors;
        }
       if (totalFailed >= absoluteThreshold) {
            executor.setDiedFast();
        }
    }

    public void setExecutor(TestExecutor executor) {
        this.executor = executor;
    }

    private String getAbsoluteThresholdNice() {
        return String.format("%.0f", absoluteThreshold);
    }

    private String getRelativeThresholdNice() {
        return String.format("%.2f", relativeThreshold);
    }

}
