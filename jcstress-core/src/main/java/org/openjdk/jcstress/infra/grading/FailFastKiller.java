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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class FailFastKiller extends CountingResultCollector {

    private final PrintWriter output;
    private final String originalValue;
    private final Map<String, Integer> tests;
    private final Set<String> failures = new HashSet<>();
    private final List<TestConfig> variants;
    private final boolean isFailFastAllVariants;
    private final double userValue;
    private final boolean relative;
    private final boolean superRelative;

    private TestExecutor executor;
    private double absoluteThreshold = 1;
    private double relativeThreshold = 1;
    private int usedTotal = -1;


    public FailFastKiller(Options opts, PrintWriter pw, List<TestConfig> finalVariants) {
        this.output = pw;
        output.println("  FailFast attached as:");
        this.originalValue = opts.getFailFast();
        this.userValue = Double.valueOf(originalValue.replaceAll("%*", ""));
        this.relative = originalValue.endsWith("%");
        this.superRelative = originalValue.endsWith("%%");
        this.isFailFastAllVariants = opts.isFailFastAllVariants();
        this.variants = Collections.unmodifiableList(finalVariants);
        this.tests = groupVariants(finalVariants);
        if (isFailFastAllVariants) {
            output.println("    all variants, " + originalValue);
            usedTotal = variants.size();
        } else {
            output.println("    whole tests, " + originalValue);
            usedTotal = tests.size();
        }
        if (superRelative) {
            relativeThreshold = userValue;
            absoluteThreshold = -1;
            output.println("    The suite will terminate once failure rate reaches " + getRelativeThresholdNice()
                    + "% of *currently* finished number of tests/variants");
        } else {
            if (relative) {
                relativeThreshold = userValue;
                if (isFailFastAllVariants) {
                    absoluteThreshold = (relativeThreshold * (double) variants.size()) / 100d;
                } else {
                    absoluteThreshold = (relativeThreshold * (double) tests.size()) / 100d;
                }
            } else {
                absoluteThreshold = (long) userValue;
                if (isFailFastAllVariants) {
                    relativeThreshold = (absoluteThreshold * 100d) / (double) variants.size();
                } else {
                    relativeThreshold = (absoluteThreshold * 100d) / (double) tests.size();
                }
            }
            output.println("    The suite will terminate once failure rate reaches " + getRelativeThresholdNice() + "% ("
                    + getAbsoluteThresholdNice() + ") of total tests/variants (" + usedTotal + ")");
        }
        output.println();
    }

    private static Map<String, Integer> groupVariants(List<TestConfig> finalVariants) {
        Map<String, Integer> tests = new HashMap<>();
        for (TestConfig testVariant : finalVariants) {
            int counter = tests.getOrDefault(testVariant.name, 0);
            counter++;
            tests.put(testVariant.name, counter);
        }
        return tests;
    }

    @Override
    public synchronized void add(TestResult r) {
        addResult(r);
    }

    private void addResult(TestResult r) {
        int groupCounter = tests.get(r.getName());
        //we are counting each group down, so we can assure that the group is finished
        tests.put(r.getName(), groupCounter-1);
        long wasFailed = hardErrors + failed;
        countResult(r);
        long isFailed = hardErrors + failed;
        if (isFailed > wasFailed) {
            failures.add(r.getName());
        }
        verifyState(r);
    }

    private void verifyState(TestResult r) {
        long totalFailed = failed + hardErrors;
        if (superRelative) {
            double totalFinishedUpToNow;
            if (isFailFastAllVariants) {
                totalFinishedUpToNow = passed + failed + softErrors + hardErrors;
            } else {
                totalFinishedUpToNow = tests.values().stream().filter(a -> a <= 0).collect(Collectors.counting());
            }
            double currentAbsoluteThreshold = (relativeThreshold * totalFinishedUpToNow) / 100d;
            //there must be enough finished to get to some reasonable numbers
            if (currentAbsoluteThreshold > 1 && totalFailed > currentAbsoluteThreshold) {
                executor.setDiedFast();
            }
        } else {
            if (isFailFastAllVariants) {
                if (totalFailed > absoluteThreshold) {
                    executor.setDiedFast();
                }
            } else {
                //we have to ensure, that all tests in current group finished
                if (failures.size() > absoluteThreshold && tests.get(r.getName()) <= 0) {
                    executor.setDiedFast();
                }
            }
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
