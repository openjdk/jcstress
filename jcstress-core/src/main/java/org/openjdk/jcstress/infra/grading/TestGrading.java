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
import org.openjdk.jcstress.infra.State;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.TestResult;
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
    public final List<String> failureMessages;

    public TestGrading(TestResult r, TestInfo test) {
        failureMessages = new NonNullArrayList<>();

        if (test == null) {
            isPassed = false;
            hasInteresting = false;
            hasSpec = false;
            failureMessages.add("No test.");
            return;
        }

        isPassed = true;
        hasInteresting = false;
        hasSpec = false;

        List<State> unmatchedStates = new ArrayList<>();
        unmatchedStates.addAll(r.getStates());
        for (StateCase c : test.cases()) {
            boolean matched = false;

            for (State s : r.getStates()) {
                if (c.state().equals(s.getId())) {
                    isPassed &= passed(c.expect(), s.getCount());
                    hasInteresting |= hasInteresting(c.expect(), s.getCount());
                    hasSpec |= hasSpec(c.expect(), s.getCount());
                    failureMessages.add(failureMessage(s.getId(), c.expect(), s.getCount()));
                    matched = true;
                    unmatchedStates.remove(s);
                }
            }

            if (!matched) {
                isPassed &= passed(c.expect(), 0);
                hasInteresting |= hasInteresting(c.expect(), 0);
                hasSpec |= hasSpec(c.expect(), 0);
                failureMessages.add(failureMessage("N/A", c.expect(), 0));
            }
        }

        for (State s : unmatchedStates) {
            isPassed &= passed(test.unmatched().expect(), s.getCount());
            hasInteresting |= hasInteresting(test.unmatched().expect(), s.getCount());
            hasSpec |= hasSpec(test.unmatched().expect(), s.getCount());
            failureMessages.add(failureMessage(s.getId(), test.unmatched().expect(), s.getCount()));
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
