/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
package org.openjdk.jcstress;

import org.openjdk.jcstress.infra.grading.ReportUtils;
import org.openjdk.jcstress.util.TimeValue;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeBudget {

    final long endTime;
    final boolean zeroBudget;
    final int expectedTests;

    final AtomicInteger inflightTests;
    final AtomicInteger maxInflightTests;
    final AtomicInteger leftoverTests;

    public TimeBudget(int expectedTests, TimeValue timeBudget) {
        this.expectedTests = expectedTests;
        this.endTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + timeBudget.milliseconds();
        this.zeroBudget = timeBudget.isZero();
        this.inflightTests = new AtomicInteger();
        this.maxInflightTests = new AtomicInteger();
        this.leftoverTests = new AtomicInteger(expectedTests);
    }

    public void finishTest() {
        inflightTests.decrementAndGet();
        leftoverTests.decrementAndGet();
    }

    public void startTest() {
        int inflight = inflightTests.incrementAndGet();
        maxInflightTests.updateAndGet(x -> Math.max(x, inflight));
    }

    public int targetTestTimeMs() {
        if (isZero()) {
            return 0;
        }

        int parMult;

        int testsLeft = leftoverTests.get();
        int maxInflight = maxInflightTests.get();

        if (testsLeft >= maxInflight) {
            // Parallel multiplier is at least the number of currently
            // running parallel tests.
            parMult = inflightTests();
            if (parMult <= 0) {
                parMult = 1;
            }
        } else {
            // If the last tests are running, we should not cut down
            // the test time unnecessarily.
            parMult = maxInflight;
        }

        long timeLeft = timeLeftMs();

        long msPerTest = (timeLeft > 0 && testsLeft > 0) ?
                timeLeft / testsLeft * parMult :
                0;

        // Enforce reasonable target brackets and leave some time
        // for test infrastructure to run.
        final int MIN_TIME_MS = 30;
        final int MAX_TIME_MS = 60_000;
        if (msPerTest > MIN_TIME_MS * 2) {
            msPerTest -= MIN_TIME_MS;
        }
        msPerTest = Math.max(MIN_TIME_MS, msPerTest);
        msPerTest = Math.min(MAX_TIME_MS, msPerTest);

        return (int)msPerTest;
    }

    public long timeLeftMs() {
        return endTime - TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    public int inflightTests() {
        return inflightTests.get();
    }

    public void printOn(PrintStream out) {
        out.println("  Time budget:");
        if (isZero()) {
            out.println("    Zero budget, sanity test mode");
        } else {
            out.println("    Target completion: in " + ReportUtils.msToDate(timeLeftMs(), true));
            out.println("    Initial test time: " + targetTestTimeMs() + " ms");
        }
        out.println();
    }

    public boolean isZero() {
        return zeroBudget;
    }
}
