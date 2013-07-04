/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jcstress.infra.State;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.schema.descr.Case;
import org.openjdk.jcstress.schema.descr.ExpectType;
import org.openjdk.jcstress.schema.descr.Test;
import org.openjdk.jcstress.util.NonNullArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class TestGrading {
    public boolean isPassed;
    public boolean isSpecial;
    public final List<String> failureMessages;

    public TestGrading(TestResult r, Test test) {
        failureMessages = new NonNullArrayList<String>();

        if (test == null) {
            isPassed = false;
            isSpecial = false;
            failureMessages.add("No test.");
            return;
        }

        isPassed = true;
        isSpecial = false;

        List<State> unmatchedStates = new ArrayList<State>();
        unmatchedStates.addAll(r.getStates());
        for (Case c : test.getCase()) {
            boolean matched = false;

            for (State s : r.getStates()) {
                if (c.getMatch().contains(s.getId())) {
                    isPassed &= passed(c.getExpect(), s.getCount());
                    isSpecial |= special(c.getExpect(), s.getCount());
                    failureMessages.add(failureMessage(s.getId(), c.getExpect(), s.getCount()));
                    matched = true;
                    unmatchedStates.remove(s);
                }
            }

            if (!matched) {
                isPassed &= passed(c.getExpect(), 0);
                isSpecial |= special(c.getExpect(), 0);
                failureMessages.add(failureMessage("N/A", c.getExpect(), 0));
            }
        }

        for (State s : unmatchedStates) {
            isPassed &= passed(test.getUnmatched().getExpect(), s.getCount());
            isSpecial |= special(test.getUnmatched().getExpect(), s.getCount());
            failureMessages.add(failureMessage(s.getId(), test.getUnmatched().getExpect(), s.getCount()));
        }
    }

    public static String failureMessage(String id, ExpectType expect, long count) {
        if (passed(expect, count)) {
            return null;
        } else {
            switch (expect) {
                case ACCEPTABLE:
                case KNOWN_ACCEPTABLE:
                    return null;
                case FORBIDDEN:
                case KNOWN_FORBIDDEN:
                    return "Observed forbidden state: " + id;
                case REQUIRED:
                    return "Have not observed required state" + id;
                case UNKNOWN:
                    return "Missing description";
                default:
                    return "Missing grading";
            }
        }
    }

    public static boolean passed(ExpectType expect, long count) {
        switch (expect) {
            case ACCEPTABLE:
            case KNOWN_ACCEPTABLE:
                return true;
            case FORBIDDEN:
            case KNOWN_FORBIDDEN:
                return count == 0;
            case REQUIRED:
                return count != 0;
            case UNKNOWN:
                return false;
            default:
                throw new IllegalStateException("No grading for expect type = " + expect);
        }
    }

    public static boolean special(ExpectType expect, long count) {
        switch (expect) {
            case ACCEPTABLE:
            case REQUIRED:
            case FORBIDDEN:
                return false;
            case KNOWN_ACCEPTABLE:
                return count != 0;
            case KNOWN_FORBIDDEN:
                return count == 0;
            case UNKNOWN:
                return false;
            default:
                throw new IllegalStateException("No grading for expect type = " + expect);
        }
    }

}
