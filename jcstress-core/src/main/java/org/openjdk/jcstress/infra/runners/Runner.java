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

import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.vm.WhiteBoxSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    protected static final int MIN_TIMEOUT_MS = 30*1000;

    protected final Control control;
    protected final TestResultCollector collector;
    protected final ExecutorService pool;
    protected final String testName;
    protected final TestConfig config;
    protected final List<String> messages;

    public Runner(TestConfig config, TestResultCollector collector, ExecutorService pool, String testName) {
        this.collector = collector;
        this.pool = pool;
        this.testName = testName;
        this.control = new Control();
        this.config = config;
        this.messages = new ArrayList<>();
    }

    /**
     * Run the test.
     * This method blocks until test is complete
     */
    public void run() {
        @SuppressWarnings("unchecked")
        Counter<R>[] results = (Counter<R>[]) new Counter[config.iters + 1];

        try {
            results[0] = sanityCheck();
        } catch (ClassFormatError | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            dumpFailure(Status.API_MISMATCH, "Test sanity check failed, skipping", e);
            return;
        } catch (Throwable e) {
            dumpFailure(Status.CHECK_TEST_ERROR, "Check test failed", e);
            return;
        }

        try {
            WhiteBoxSupport.tryDeopt(config.deoptMode);
        } catch (NoClassDefFoundError err) {
            // gracefully "handle"
        }

        for (int c = 1; c <= config.iters; c++) {
            results[c] = internalRun();
        }
        dump(results);
    }

    private TestResult prepareResult(Status status) {
        TestResult result = new TestResult(config, status);
        for (String msg : messages) {
            result.addMessage(msg);
        }
        messages.clear();
        return result;
    }

    protected void dumpFailure(Status status, String message) {
        messages.add(message);
        TestResult result = prepareResult(status);
        collector.add(result);
    }

    protected void dumpFailure(Status status, String message, Throwable aux) {
        messages.add(message);
        TestResult result = prepareResult(status);
        result.addMessage(StringUtils.getStacktrace(aux));
        collector.add(result);
    }

    protected void dump(Counter<R> cnt) {
        TestResult result = prepareResult(Status.NORMAL);
        for (R e : cnt.elementSet()) {
             result.addState(String.valueOf(e), cnt.count(e));
        }
        collector.add(result);
    }

    protected void dump(Counter<R>[] results) {
        TestResult result = prepareResult(Status.NORMAL);
        for (Counter<R> cnt : results) {
            for (R e : cnt.elementSet()) {
                result.addState(String.valueOf(e), cnt.count(e));
            }
        }
        collector.add(result);
    }

    public abstract Counter<R> sanityCheck() throws Throwable;

    public abstract Counter<R> internalRun();

    protected <T> void waitFor(Collection<Future<T>> tasks) {
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
                    dumpFailure(Status.TEST_ERROR, "Unrecoverable error while running", e.getCause());
                    return;
                } catch (InterruptedException e) {
                    return;
                }
            }

            long timeSpent = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (timeSpent > Math.max(10*config.time, MIN_TIMEOUT_MS)) {
                dumpFailure(Status.TIMEOUT_ERROR, "Timeout waiting for tasks to complete: " + timeSpent + " ms");
                return;
            }
        }
    }
}
