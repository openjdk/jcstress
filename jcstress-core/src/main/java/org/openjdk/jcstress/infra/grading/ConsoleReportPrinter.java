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
import org.openjdk.jcstress.Verbosity;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.util.StringUtils;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Prints the test results to the console.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ConsoleReportPrinter implements TestResultCollector {

    private static final Integer PRINT_INTERVAL_MS = Integer.getInteger("jcstress.console.printIntervalMs");

    private final Verbosity verbosity;
    private final PrintWriter output;
    private final long expectedTests;
    private final long expectedForks;
    private final long expectedResults;

    private long observedResults;
    private long observedCount;

    private final Set<String> observedTests = Collections.newSetFromMap(new HashMap<>());
    private final Set<ConfigFork> observedForks = Collections.newSetFromMap(new HashMap<>());

    private long firstTest;

    private final long printIntervalMs;
    private long lastPrint;

    private final boolean progressInteractive;
    private int progressLen;

    private long passed;
    private long failed;
    private long softErrors;
    private long hardErrors;

    public ConsoleReportPrinter(Options opts, PrintWriter pw, long expectedTests, long expectedForks) {
        this.output = pw;
        this.expectedTests = expectedTests;
        this.expectedForks = expectedForks;
        this.expectedResults = expectedForks;
        verbosity = opts.verbosity();
        progressLen = 1;

        progressInteractive = (System.console() != null);
        output.println("  Attached the " + (progressInteractive ? "interactive console" : "non-interactive output stream") + ".");

        printIntervalMs = (PRINT_INTERVAL_MS != null) ?
                PRINT_INTERVAL_MS :
                progressInteractive ? 1_000 : 15_000;

        output.println("  Printing the progress line at least every " + printIntervalMs + " milliseconds.");
        output.println();
    }

    @Override
    public synchronized void add(TestResult r) {
        if (firstTest == 0) {
            firstTest = System.nanoTime();
        }

        observedTests.add(r.getName());
        observedForks.add(new ConfigFork(r.getConfig()));
        observedResults++;
        observedCount += r.getTotalCount();

        printResult(r);
    }

    private void printResult(TestResult r) {
        TestGrading grading = r.grading();

        boolean inHardError = false;
        switch (r.status()) {
            case TIMEOUT_ERROR:
            case CHECK_TEST_ERROR:
            case TEST_ERROR:
            case VM_ERROR:
                hardErrors++;
                inHardError = true;
                break;
            case API_MISMATCH:
                softErrors++;
                break;
            case NORMAL:
                if (grading.isPassed) {
                    passed++;
                } else {
                    failed++;
                }
                break;
            default:
                throw new IllegalStateException("Illegal status: " + r.status());
        }

        boolean shouldPrintResults =
                inHardError ||
                !grading.isPassed ||
                grading.hasInteresting ||
                verbosity.printAllTests();

        long currentTime = System.nanoTime();

        boolean shouldPrintStatusLine =
                shouldPrintResults ||
                TimeUnit.NANOSECONDS.toMillis(currentTime - lastPrint) >= printIntervalMs;

        if (shouldPrintStatusLine && progressInteractive) {
            output.printf("\r%" + progressLen + "s\r", "");
        }

        if (shouldPrintResults) {
            if (!progressInteractive) {
                output.println();
            }
            output.printf("%10s %s%n", "[" + ReportUtils.statusToLabel(r) + "]", StringUtils.chunkName(r.getName()));
            ReportUtils.printDetails(output, r, true);
            ReportUtils.printMessages(output, r);
        }

        if (shouldPrintStatusLine) {
            String line = String.format("(ETA: %10s) (Sample Rate: %s) (Tests: %d of %d) (Forks: %2d of %d) (Results: %2d of %d; %d passed, %d failed, %d soft errs, %d hard errs) ",
                    computeETA(),
                    computeSpeed(),
                    observedTests.size(), expectedTests,
                    observedForks.size(), expectedForks,
                    observedResults, expectedResults, passed, failed, softErrors, hardErrors
            );
            progressLen = line.length();
            output.print(line);
            if (!progressInteractive) {
                output.println();
            }
            output.flush();
            lastPrint = currentTime;
        }
    }

    private String computeSpeed() {
        long timeSpent = System.nanoTime() - firstTest;
        double v = 1.0 * TimeUnit.SECONDS.toNanos(1) * observedCount / timeSpent;

        final long K = 1000;
        final long M = 1000*K;
        final long G = 1000*M;
        final long T = 1000*G;

        if (v > 10*T) {
            return String.format("%3.2f T/sec", v / T);
        }

        if (v > 10*G) {
            return String.format("%3.2f G/sec", v / G);
        }

        if (v > 10*M) {
            return String.format("%3.2f M/sec", v / M);
        }

        if (v > 10*K) {
            return String.format("%3.2f K/sec", v / K);
        }

        return String.format("%3.2f #/sec", v);
    }

    private String computeETA() {
        long timeSpent = System.nanoTime() - firstTest;
        long resultsGot = observedResults;
        if (resultsGot == 0) {
            return "n/a";
        }

        long nsToGo = (long)(timeSpent * (1.0 * (expectedResults - 1) / resultsGot - 1));
        if (nsToGo > 0) {
            String result = "";
            long days = TimeUnit.NANOSECONDS.toDays(nsToGo);
            if (days > 0) {
                result += days + "d+";
                nsToGo -= TimeUnit.DAYS.toNanos(days);
            }
            long hours = TimeUnit.NANOSECONDS.toHours(nsToGo);
            nsToGo -= TimeUnit.HOURS.toNanos(hours);

            long minutes = TimeUnit.NANOSECONDS.toMinutes(nsToGo);
            nsToGo -= TimeUnit.MINUTES.toNanos(minutes);

            long seconds = TimeUnit.NANOSECONDS.toSeconds(nsToGo);

            result += String.format("%02d:%02d:%02d", hours, minutes, seconds);
            return result;
        } else {
            return "now";
        }
    }

    static class ConfigFork {
        private TestConfig config;

        public ConfigFork(TestConfig config) {
            this.config = config;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ConfigFork that = (ConfigFork) o;

            if (config.forkId != that.config.forkId) return false;
            return config.equals(that.config);
        }

        @Override
        public int hashCode() {
            int result = config.hashCode();
            result = 31 * result + config.forkId;
            return result;
        }
    }

}
