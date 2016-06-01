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
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Prints the test results to the console.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ConsoleReportPrinter implements TestResultCollector {

    private final boolean verbose;
    private final Options opts;
    private final PrintWriter output;
    private final long expectedTests;
    private final long expectedIterations;
    private final long expectedForks;

    private long observedIterations;
    private long observedCount;

    private final Set<String> observedTests = Collections.newSetFromMap(new HashMap<>());
    private final Set<ConfigFork> observedForks = Collections.newSetFromMap(new HashMap<>());

    private long firstTest;
    private int progressLen;

    private long passed;
    private long failed;
    private long softErrors;
    private long hardErrors;

    public ConsoleReportPrinter(Options opts, PrintWriter pw, int expectedTests, int expectedForks) throws FileNotFoundException {
        this.opts = opts;
        this.output = pw;
        this.expectedTests = expectedTests;
        this.expectedForks = expectedForks;
        this.expectedIterations = expectedForks * opts.getIterations();
        verbose = opts.isVerbose();
        progressLen = 1;
    }

    @Override
    public synchronized void add(TestResult r) {
        if (firstTest == 0) {
            firstTest = System.nanoTime();
        }

        observedTests.add(r.getName());
        observedForks.add(new ConfigFork(r.getConfig()));
        observedIterations++;
        observedCount += r.getTotalCount();

        printResult(r, verbose);
    }

    private void printResult(TestResult r, boolean isVerbose) {
        switch (r.status()) {
            case TIMEOUT_ERROR:
                printLine("TIMEOUT", r);
                hardErrors++;
                return;
            case CHECK_TEST_ERROR:
            case TEST_ERROR:
                output.println();
                printLine("ERROR", r);
                for (String data : r.getAuxData()) {
                    output.println(data);
                }
                output.println();
                hardErrors++;
                return;
            case VM_ERROR:
                output.println();
                printLine("VM ERROR", r);
                for (String data : r.getAuxData()) {
                    output.println(data);
                }
                output.println();
                hardErrors++;
                return;
            case API_MISMATCH:
                printLine("SKIPPED", r);
                softErrors++;
                return;
            case NORMAL:
                TestInfo test = TestList.getInfo(r.getName());
                if (test == null) {
                    output.println();
                    printLine("UNKNOWN", r);
                    isVerbose = true;
                } else {
                    TestGrading grading = new TestGrading(r, test);
                    if (grading.isPassed) {
                        printLine("OK", r);
                        passed++;
                    } else {
                        output.println();
                        printLine("FAILED", r);
                        isVerbose = true;
                        failed++;
                    }
                }
                break;
            default:
                throw new IllegalStateException("Illegal status: " + r.status());
        }

        if (isVerbose) {
            output.format("    (fork: #%d, iteration #%d, JVM args: %s)%n",
                    r.getConfig().forkId + 1,
                    r.getIteration() + 1,
                    r.getConfig().jvmArgs
            );

            int idLen = "Observed state".length();
            int occLen = "Occurrences".length();
            int expectLen = "Expectation".length();
            int descLen = 60;

            for (String s : r.getStateKeys()) {
                idLen = Math.max(idLen, s.length());
                occLen = Math.max(occLen, String.format("%,d", r.getCount(s)).length());
                expectLen = Math.max(expectLen, Expect.UNKNOWN.toString().length());
            }

            TestInfo test = TestList.getInfo(r.getName());
            if (test == null) {
                output.printf("%" + idLen + "s %" + occLen +"s %" + expectLen + "s  %-" + descLen + "s%n", "Observed state", "Occurrences", "Expectation", "Interpretation");
                for (String s : r.getStateKeys()) {
                    output.printf("%" + idLen + "s %," + occLen + "d %" + expectLen + "s  %-" + descLen + "s%n",
                            StringUtils.cutoff(s, idLen),
                            r.getCount(s),
                            Expect.UNKNOWN,
                            "N/A");
                }
                return;
            }

            for (StateCase c : test.cases()) {
                idLen = Math.max(idLen, c.matchPattern().length());
                expectLen = Math.max(expectLen, c.expect().toString().length());
            }
            expectLen = Math.max(expectLen, test.unmatched().expect().toString().length());

            idLen += 2;
            occLen += 2;
            expectLen += 2;

            output.printf("%" + idLen + "s %" + occLen +"s %" + expectLen + "s  %-" + descLen + "s%n", "Observed state", "Occurrences", "Expectation", "Interpretation");

            List<String> unmatchedStates = new ArrayList<>();
            unmatchedStates.addAll(r.getStateKeys());

            for (StateCase c : test.cases()) {
                boolean matched = false;

                for (String s : r.getStateKeys()) {
                    if (c.matches(s)) {
                        // match!
                        output.printf("%" + idLen + "s %," + occLen + "d %" + expectLen + "s  %-" + descLen + "s%n",
                                StringUtils.cutoff(s, idLen),
                                r.getCount(s),
                                c.expect(),
                                StringUtils.cutoff(c.description(), descLen));
                        matched = true;
                        unmatchedStates.remove(s);
                    }
                }

                if (!matched) {
                    output.printf("%" + idLen + "s %," + occLen + "d %" + expectLen + "s  %-" + descLen + "s%n",
                                StringUtils.cutoff(c.matchPattern(), idLen),
                                0,
                                c.expect(),
                                StringUtils.cutoff(c.description(), descLen));
                }
            }

            for (String s : unmatchedStates) {
                output.printf("%" + idLen + "s %," + occLen + "d %" + expectLen + "s  %-" + descLen + "s%n",
                        StringUtils.cutoff(s, idLen),
                        r.getCount(s),
                        test.unmatched().expect(),
                        StringUtils.cutoff(test.unmatched().description(), descLen));
            }

            output.println();
        }

        printProgress();
    }

    private void printLine(String label, TestResult r) {
        output.printf("\r%" + progressLen + "s\r", "");
        output.printf("%10s %s\n", "[" + label + "]", StringUtils.chunkName(r.getName()));
    }

    private void printProgress() {
        String line = String.format("(ETA: %10s) (Rate: %s samples/sec) (Tests: %d of %d) (Forks: %2d of %d) (Iterations: %2d of %d; %d passed, %d failed, %d soft errs, %d hard errs) ",
                computeETA(),
                computeSpeed(),
                observedTests.size(), expectedTests,
                observedForks.size(), expectedForks,
                observedIterations, expectedIterations, passed, failed, softErrors, hardErrors
        );
        progressLen = line.length();
        output.print(line);
        output.flush();
    }

    private String computeSpeed() {
        long timeSpent = System.nanoTime() - firstTest;
        return String.format("%3.2E", 1.0 * TimeUnit.SECONDS.toNanos(1) * observedCount / timeSpent);
    }

    private String computeETA() {
        long timeSpent = System.nanoTime() - firstTest;
        long resultsGot = observedIterations;
        if (resultsGot == 0) {
            return "n/a";
        }

        long nsToGo = (long)(timeSpent * (1.0 * (expectedIterations - 1) / resultsGot - 1));
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
