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
package org.openjdk.jcstress.infra.runners;

import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.tests.ConcurrencyTest;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.NullOutputStream;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Random;
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
public abstract class Runner {
    protected final int time;
    protected final int iters;
    protected final int minStride, maxStride;
    protected final int deoptRatio;
    protected final boolean verbose;
    protected final TestResultCollector collector;
    protected final ExecutorService pool;
    protected volatile boolean shouldYield;

    protected volatile boolean isStopped;

    protected final PrintWriter testLog;

    public Runner(Options opts, TestResultCollector collector, ExecutorService pool) throws FileNotFoundException, JAXBException {
        this.collector = collector;
        this.pool = pool;

        time = opts.getTime();
        minStride = opts.getMinStride();
        maxStride = opts.getMaxStride();
        iters = opts.getIterations();
        shouldYield = opts.shouldYield();
        verbose = opts.isVerbose();
        deoptRatio = opts.deoptRatio();

        if (verbose) {
            testLog = new PrintWriter(System.out, true);
        } else {
            testLog = new PrintWriter(new NullOutputStream(), true);
        }

        int totalThreads = requiredThreads();
        testLog.printf("Executing with %d threads\n", totalThreads);
    }

    public static int[] generatePermutation(int len) {
        int[] res = new int[len];
        for (int i = 0; i < len; i++) {
            res[i] = i;
        }
        return shuffle(res);
    }

    public static int[] shuffle(int[] arr) {
        Random r = new Random();
        int[] res = arr.clone();
        for (int i = arr.length; i > 1; i--) {
            int i1 = i - 1;
            int i2 = r.nextInt(i);
            int t = res[i1];
            res[i1] = res[i2];
            res[i2] = t;
        }
        return res;
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

    protected <R> void dump(String testName, Counter<R> results) {
        TestResult result = new TestResult(testName, Status.NORMAL);

        for (R e : results.elementSet()) {
            result.addState(e, results.count(e));
        }

        collector.add(result);
    }

    public abstract void run();

    public abstract int requiredThreads();

    protected void hardExit() {
        testLog.flush();
        testLog.close();
        System.exit(0);
    }

    public void warn(String s) {
        testLog.println(s);
    }

    protected void waitFor(String testName, Collection<Future<?>> tasks) {
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

            if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) > Math.max(time, 60*1000)) {
                dumpFailure(testName, Status.TIMEOUT_ERROR);
                return;
            }
        }
    }
}
