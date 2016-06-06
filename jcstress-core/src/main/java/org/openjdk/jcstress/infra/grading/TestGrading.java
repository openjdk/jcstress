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

import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.NonNullArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class TestGrading {
    public boolean isPassed;
    public boolean hasInteresting;
    public boolean hasSpec;
    public final List<GradingResult> gradingResults;
    public final List<String> failureMessages;

    public static TestGrading grade(TestResult r) {
        return new TestGrading(r);
    }

    private TestGrading(TestResult r) {
        TestInfo test = TestList.getInfo(r.getName());
        gradingResults = new ArrayList<>();
        failureMessages = new NonNullArrayList<>();

        isPassed = true;
        hasInteresting = false;
        hasSpec = false;

        List<String> unmatchedStates = new ArrayList<>();
        unmatchedStates.addAll(r.getStateKeys());

        for (StateCase c : test.cases()) {
            boolean matched = false;

            Expect ex = c.expect();
            for (String s : r.getStateKeys()) {
                if (c.matches(s)) {
                    long count = r.getCount(s);
                    isPassed &= passed(ex, count);
                    hasInteresting |= hasInteresting(ex, count);
                    hasSpec |= hasSpec(ex, count);
                    failureMessages.add(failureMessage(s, ex, count));
                    matched = true;
                    unmatchedStates.remove(s);

                    gradingResults.add(new GradingResult(
                            c.matchPattern(),
                            c.expect(),
                            r.getCount(s),
                            c.description()
                    ));
                }
            }

            if (!matched) {
                isPassed &= passed(ex, 0);
                hasInteresting |= hasInteresting(ex, 0);
                hasSpec |= hasSpec(ex, 0);
                failureMessages.add(failureMessage("N/A", ex, 0));

                gradingResults.add(new GradingResult(
                        c.matchPattern(),
                        c.expect(),
                        0,
                        c.description()
                ));
            }
        }

        for (String s : unmatchedStates) {
            Expect expect = test.unmatched().expect();
            long count = r.getCount(s);
            isPassed &= passed(expect, count);
            hasInteresting |= hasInteresting(expect, count);
            hasSpec |= hasSpec(expect, count);
            failureMessages.add(failureMessage(s, expect, count));

            gradingResults.add(new GradingResult(
                    s,
                    test.unmatched().expect(),
                    r.getCount(s),
                    test.unmatched().description()
            ));
        }
    }

    public static String failureMessage(String id, Expect expect, long count) {
        if (passed(expect, count)) {
            return null;
        } else {
            switch (expect) {
                case ACCEPTABLE:
                case ACCEPTABLE_INTERESTING:
                case ACCEPTABLE_SPEC:
                    return null;
                case FORBIDDEN:
                    return "Observed forbidden state: " + id;
                case UNKNOWN:
                    return "Missing description";
                default:
                    return "Missing grading";
            }
        }
    }

    public static boolean passed(Expect expect, long count) {
        switch (expect) {
            case ACCEPTABLE:
            case ACCEPTABLE_INTERESTING:
            case ACCEPTABLE_SPEC:
                return true;
            case FORBIDDEN:
                return count == 0;
            case UNKNOWN:
                return false;
            default:
                throw new IllegalStateException("No grading for expect type = " + expect);
        }
    }

    private static boolean hasInteresting(Expect expect, long count) {
        switch (expect) {
            case ACCEPTABLE:
            case ACCEPTABLE_SPEC:
            case FORBIDDEN:
                return false;
            case ACCEPTABLE_INTERESTING:
                return count != 0;
            case UNKNOWN:
                return false;
            default:
                throw new IllegalStateException("No grading for expect type = " + expect);
        }
    }

    private static boolean hasSpec(Expect expect, long count) {
        switch (expect) {
            case ACCEPTABLE:
            case ACCEPTABLE_INTERESTING:
            case FORBIDDEN:
                return false;
            case ACCEPTABLE_SPEC:
                return count != 0;
            case UNKNOWN:
                return false;
            default:
                throw new IllegalStateException("No grading for expect type = " + expect);
        }
    }

}
