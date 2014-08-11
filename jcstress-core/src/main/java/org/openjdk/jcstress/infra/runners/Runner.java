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
package org.openjdk.jcstress.infra.runners;

import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.NullOutputStream;
import org.openjdk.jcstress.util.VMSupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic runner for concurrency tests.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public abstract class Runner<R> {
    protected final Control control;
    protected final TestResultCollector collector;
    protected final ExecutorService pool;
    protected final PrintWriter testLog;
    protected final String testName;

    public Runner(Options opts, TestResultCollector collector, ExecutorService pool, String testName) {
        this.collector = collector;
        this.pool = pool;
        this.testName = testName;
        this.control = new Control(opts);

        if (control.verbose) {
            testLog = new PrintWriter(System.out, true);
        } else {
            testLog = new PrintWriter(new NullOutputStream(), true);
        }
    }

    /**
     * Run the test.
     * This method blocks until test is complete
     */
    public void run() {
        testLog.println("Running " + testName);

        try {
            sanityCheck();
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            testLog.println("Test sanity check failed, skipping");
            testLog.println();
            dumpFailure(testName, Status.API_MISMATCH, e);
            return;
        } catch (Throwable e) {
            testLog.println("Check test failed");
            testLog.println();
            dumpFailure(testName, Status.CHECK_TEST_ERROR, e);
            return;
        }

        testLog.print("Iterations ");
        for (int c = 0; c < control.iters; c++) {
            try {
                VMSupport.tryDeoptimizeAllInfra(control.deoptRatio);
            } catch (NoClassDefFoundError err) {
                // gracefully "handle"
            }

            testLog.print(".");
            testLog.flush();
            dump(testName, internalRun());
        }
        testLog.println();
    }


    protected void dumpFailure(String testName, Status status) {
        TestResult result = new TestResult(testName, status);
        collector.add(result);
    }

    protected void dumpFailure(String testName, Status status, Throwable aux) {
        TestResult result = new TestResult(testName, status);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        aux.printStackTrace(pw);
        pw.close();
        result.addAuxData(sw.toString());
        collector.add(result);
    }

    protected void dump(String testName, Counter<R> results) {
        TestResult result = new TestResult(testName, Status.NORMAL);

        for (R e : results.elementSet()) {
            result.addState(e, results.count(e));
        }

        collector.add(result);
    }

    public abstract void sanityCheck() throws Throwable;

    public abstract Counter<R> internalRun();

    protected void waitFor(Collection<Future<?>> tasks) {
        long startTime = System.nanoTime();
        boolean allStopped = false;
        while (!allStopped) {
            allStopped = true;
            for (Future<?> t : tasks) {
                try {
                    t.get(1, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    allStopped = false;
                } catch (ExecutionException e) {
                    dumpFailure(testName, Status.TEST_ERROR, e.getCause());
                    return;
                } catch (InterruptedException e) {
                    return;
                }
            }

            if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) > Math.max(control.time, 60*1000)) {
                dumpFailure(testName, Status.TIMEOUT_ERROR);
                return;
            }
        }
    }
}
