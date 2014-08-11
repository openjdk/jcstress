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
import org.openjdk.jcstress.infra.State;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.TestList;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prints the test results to the console.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ConsoleReportPrinter implements TestResultCollector {

    private final boolean verbose;
    private final Options opts;
    private final PrintWriter output;
    private final int expectedTests;
    private final int expectedIterations;
    private final int expectedForks;

    private AtomicLong observedResults = new AtomicLong();
    private AtomicLong observedCount = new AtomicLong();

    private final ConcurrentMap<String, TestProgress> testsProgress = new ConcurrentHashMap<>();
    private final int totalExpectedResults;

    private long firstTest;

    public ConsoleReportPrinter(Options opts, PrintWriter pw, int expectedTests) throws JAXBException, FileNotFoundException {
        this.opts = opts;
        this.output = pw;
        this.expectedTests = expectedTests;
        this.expectedForks = opts.getForks();
        this.expectedIterations = opts.getIterations();
        this.totalExpectedResults = expectedTests * opts.getIterations() * (opts.getForks() > 0 ? opts.getForks() : 1);
        verbose = opts.isVerbose();
    }

    @Override
    public void add(TestResult r) {
        TestProgress e = testsProgress.get(r.getName());
        if (e == null) {
            e = new TestProgress(r);
            TestProgress exist = testsProgress.putIfAbsent(r.getName(), e);
            e = (exist != null) ? exist : e;
        }

        if (opts.getForks() > 0) {
            e.enregisterVM(r.getVmID());
        } else {
            e.enregisterVM(null);
        }

        if (firstTest == 0) {
            firstTest = System.nanoTime();
        } else {
            observedResults.incrementAndGet();

            int totalCount = 0;
            for (State s : r.getStates()) {
                totalCount += s.getCount();
            }
            observedCount.addAndGet(totalCount);
        }

        printResult(r, verbose);
    }

    public void printResult(TestResult r, boolean isVerbose) {
        switch (r.status()) {
            case TIMEOUT_ERROR:
                printLine(output, "TIMEOUT", r);
                return;
            case CHECK_TEST_ERROR:
            case TEST_ERROR:
                output.println();
                printLine(output, "ERROR", r);
                for (String data : r.getAuxData()) {
                    output.println(data);
                }
                output.println();
                return;
            case VM_ERROR:
                output.println();
                printLine(output, "VM ERROR", r);
                for (String data : r.getAuxData()) {
                    output.println(data);
                }
                output.println();
                return;
            case API_MISMATCH:
                printLine(output, "SKIPPED", r);
                return;
            case NORMAL:
                TestInfo test = TestList.getInfo(r.getName());
                if (test == null) {
                    output.println();
                    printLine(output, "UNKNOWN", r);
                    isVerbose = true;
                } else {
                    TestGrading grading = new TestGrading(r, test);
                    if (grading.isPassed) {
                        printLine(output, "OK", r);
                    } else {
                        output.println();
                        printLine(output, "FAILED", r);
                        isVerbose = true;
                    }
                }
                break;
            default:
                throw new IllegalStateException("Illegal status: " + r.status());
        }

        if (isVerbose) {
            int len = 35;

            TestInfo test = TestList.getInfo(r.getName());
            if (test == null) {
                output.printf("%" + len + "s %15s %18s %-20s\n", "Observed state", "Occurrences", "Expectation", "Interpretation");
                for (State s : r.getStates()) {
                    output.printf("%" + len + "s (%,13d) %18s %-40s\n",
                            cutoff(s.getId(), len),
                            s.getCount(),
                            Expect.UNKNOWN,
                            "N/A");
                }

                return;
            }

            output.printf("%" + len + "s %15s %18s %-20s\n", "Observed state", "Occurrences", "Expectation", "Interpretation");

            List<State> unmatchedStates = new ArrayList<>();
            unmatchedStates.addAll(r.getStates());
            for (StateCase c : test.cases()) {
                boolean matched = false;

                for (State s : r.getStates()) {
                    if (c.state().equals(s.getId())) {
                        // match!
                        output.printf("%" + len + "s (%,13d) %18s %-60s\n",
                                cutoff(s.getId(), len),
                                s.getCount(),
                                c.expect(),
                                cutoff(c.description(), 60));
                        matched = true;
                        unmatchedStates.remove(s);
                    }
                }

                if (!matched) {
                    output.printf("%" + len + "s (%,13d) %18s %-60s\n",
                                cutoff(c.state(), len),
                                0,
                                c.expect(),
                                cutoff(c.description(), 60));
                }
            }

            for (State s : unmatchedStates) {
                output.printf("%" + len + "s (%,13d) %18s %-60s\n",
                        cutoff(s.getId(), len),
                        s.getCount(),
                        test.unmatched().expect(),
                        cutoff(test.unmatched().description(), 60));
            }

            output.println();
        }
    }

    private static String cutoff(String src, int len) {
        while (src.contains("  ")) {
            src = src.replaceAll("  ", " ");
        }
        String trim = src.replaceAll("\n", "").trim();
        String substring = trim.substring(0, Math.min(len - 3, trim.length()));
        if (!substring.equals(trim)) {
            return substring + "...";
        } else {
            return substring;
        }
    }

    private PrintWriter printLine(PrintWriter output, String label, TestResult r) {
        return output.printf(" (ETA: %10s) (R: %s) (T:%4d/%d) (F:%2d/%d) (I:%2d/%d) %10s %s\n",
                computeETA(),
                computeSpeed(),
                testsProgress.size(), expectedTests, testsProgress.get(r.getName()).getVMindex(r.getVmID()), expectedForks, testsProgress.get(r.getName()).getIteration(r.getVmID()), expectedIterations,
                "[" + label + "]", chunkName(r.getName()));
    }

    private String computeSpeed() {
        long timeSpent = System.nanoTime() - firstTest;
        return String.format("%3.2E", 1.0 * TimeUnit.SECONDS.toNanos(1) * observedCount.get() / timeSpent);
    }

    private String computeETA() {
        long timeSpent = System.nanoTime() - firstTest;
        long resultsGot = observedResults.get();
        if (resultsGot == 0) {
            return "n/a";
        }

        long nsToGo = (long)(timeSpent * (1.0 * (totalExpectedResults - 1) / resultsGot - 1));
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

    private String chunkName(String name) {
        return name.replace("org.openjdk.jcstress.tests", "o.o.j.t");
    }

    private static class TestProgress {
        private final String name;

        private int currentVM;
        private final Map<String, Integer> vmIDs = new HashMap<>();
        private final Map<String, Integer> iterations = new HashMap<>();

        public TestProgress(TestResult result){
            this.name = result.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestProgress that = (TestProgress) o;

            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        public void enregisterVM(String vmID) {
            if (vmID == null) return;
            synchronized (this) {
                Integer id = vmIDs.get(vmID);
                if (id == null) {
                    vmIDs.put(vmID, ++currentVM);
                }
                Integer iters = iterations.get(vmID);
                if (iters == null) {
                    iters = 0;
                }
                iterations.put(vmID, ++iters);
            }
        }

        public int getVMindex(String vmID) {
            synchronized (this) {
                Integer id = vmIDs.get(vmID);
                return id != null ? id : 0;
            }
        }

        public int getIteration(String vmID) {
            synchronized (this) {
                Integer iters = iterations.get(vmID);
                return iters != null ? iters : 0;
            }
        }
    }

}
