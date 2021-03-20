/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.*;

import java.io.PrintWriter;
import java.util.*;

public class ReportUtils {

    public static List<TestResult> mergedByConfig(Collection<TestResult> src) {
        Multimap<TestConfig, TestResult> multiResults = new HashMultimap<>();
        for (TestResult r : src) {
            multiResults.put(r.getConfig(), r);
        }

        List<TestResult> results = new ArrayList<>();
        for (TestConfig config : multiResults.keys()) {
            Collection<TestResult> mergeable = multiResults.get(config);
            TestResult root = merged(config, mergeable);
            results.add(root);
        }

        return results;
    }

    public static List<TestResult> mergedByName(Collection<TestResult> src) {
        Multimap<String, TestResult> multiResults = new HashMultimap<>();
        for (TestResult r : src) {
            multiResults.put(r.getConfig().name, r);
        }

        List<TestResult> results = new ArrayList<>();
        for (String name : multiResults.keys()) {
            Collection<TestResult> mergeable = multiResults.get(name);
            TestResult root = merged(mergeable.iterator().next().getConfig(), mergeable);
            results.add(root);
        }

        return results;
    }

    public static Multimap<String, TestResult> byName(Collection<TestResult> src) {
        Multimap<String, TestResult> result = new HashMultimap<>();
        for (TestResult r : mergedByConfig(src)) {
            result.put(r.getName(), r);
        }
        return result;
    }

    private static TestResult merged(TestConfig config, Collection<TestResult> mergeable) {
        Multiset<String> stateCounts = new HashMultiset<>();

        List<String> auxData = new ArrayList<>();

        Status status = Status.NORMAL;
        Environment env = null;
        for (TestResult r : mergeable) {
            status = status.combine(r.status());
            for (String s : r.getStateKeys()) {
                stateCounts.add(s, r.getCount(s));
            }
            env = r.getEnv();
            auxData.addAll(r.getMessages());
        }

        TestResult root = new TestResult(config, status);

        for (String s : stateCounts.keys()) {
            root.addState(s, stateCounts.count(s));
        }

        root.setEnv(env);

        for (String data : auxData) {
            root.addMessage(data);
        }
        return root;
    }

    public static void printDetails(PrintWriter pw, TestResult r, boolean inProgress) {
        if (inProgress) {
            pw.format("    (fork: #%d, JVM args: %s)%n",
                    r.getConfig().forkId + 1,
                    r.getConfig().jvmArgs
            );
        } else {
            pw.format("    (JVM args: %s)%n",
                    r.getConfig().jvmArgs
            );
        }

        if (!r.hasSamples()) {
            return;
        }

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
        for (StateCase c : test.cases()) {
            idLen = Math.max(idLen, c.matchPattern().length());
            expectLen = Math.max(expectLen, c.expect().toString().length());
        }
        expectLen = Math.max(expectLen, test.unmatched().expect().toString().length());

        idLen += 2;
        occLen += 2;
        expectLen += 2;

        pw.printf("%" + idLen + "s %" + occLen +"s %" + expectLen + "s  %-" + descLen + "s%n", "Observed state", "Occurrences", "Expectation", "Interpretation");

        for (GradingResult gradeRes : r.grading().gradingResults) {
            pw.printf("%" + idLen + "s %," + occLen + "d %" + expectLen + "s  %-" + descLen + "s%n",
                    StringUtils.cutoff(gradeRes.id, idLen),
                    gradeRes.count,
                    gradeRes.expect,
                    StringUtils.cutoff(gradeRes.description, descLen));
        }

        pw.println();
    }

    public static void printMessages(PrintWriter pw, TestResult r) {
        boolean errMsgsPrinted = false;
        for (String data : r.getMessages()) {
            if (skipMessage(data)) continue;
            if (!errMsgsPrinted) {
                pw.println("    Messages: ");
                errMsgsPrinted = true;
            }
            pw.println("        " + data);
        }
        if (errMsgsPrinted) {
            pw.println();
        }

        boolean vmOutPrinted = false;
        for (String data : r.getVmOut()) {
            if (skipMessage(data)) continue;
            if (!vmOutPrinted) {
                pw.println("    VM output stream: ");
                vmOutPrinted = true;
            }
            pw.println("        " + data);
        }
        if (vmOutPrinted) {
            pw.println();
        }

        boolean vmErrPrinted = false;
        for (String data : r.getVmErr()) {
            if (skipMessage(data)) continue;
            if (!vmErrPrinted) {
                pw.println("    VM error stream: ");
                vmErrPrinted = true;
            }
            pw.println("        " + data);
        }
        if (vmErrPrinted) {
            pw.println();
        }
    }

    private static boolean skipMessage(String data) {
        if (data == null) {
            return true;
        }

        if (data.startsWith("Warning: 'NoSuchMethodError' on register of sun.hotspot.WhiteBox")) {
            return true;
        }

        if (data.contains("compiler directives added")) {
            return true;
        }

        return false;
    }

    public static String statusToLabel(TestResult result) {
        switch (result.status()) {
            case TIMEOUT_ERROR:
                return "TIMEOUT";
            case CHECK_TEST_ERROR:
            case TEST_ERROR:
                return "ERROR";
            case VM_ERROR:
                return "VM ERROR";
            case API_MISMATCH:
                return "SKIPPED";
            case NORMAL:
                if (result.grading().isPassed) {
                    return "OK";
                } else {
                    return "FAILED";
                }
            default:
                throw new IllegalStateException("Illegal status: " + result.status());
        }
    }

    public static boolean statusToPassed(TestResult result) {
        switch (result.status()) {
            case TIMEOUT_ERROR:
            case CHECK_TEST_ERROR:
            case TEST_ERROR:
            case VM_ERROR:
            case API_MISMATCH:
                return false;
            case NORMAL:
                return result.grading().isPassed;
            default:
                throw new IllegalStateException("Illegal status: " + result.status());
        }
    }
}
