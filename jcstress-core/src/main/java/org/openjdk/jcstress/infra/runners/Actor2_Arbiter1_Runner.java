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
import org.openjdk.jcstress.infra.Result;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.tests.Actor2_Arbiter1_Test;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.Counters;
import org.openjdk.jcstress.util.VMSupport;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Actor2_Arbiter1_Runner<S, R extends Result> extends Runner {
    private final Actor2_Arbiter1_Test<S, R> test;
    private final String testName;

    public Actor2_Arbiter1_Runner(Options opts, Actor2_Arbiter1_Test<S, R> test, TestResultCollector collector, ExecutorService pool) throws FileNotFoundException, JAXBException {
        super(opts, collector, pool);
        this.test = test;
        this.testName = test.getClass().getName();
    }

    public void run() {
        testLog.println("Running " + testName);

        try {
            R res = test.newResult();
            S state = test.newState();
            test.actor1(state, res);
            test.actor2(state, res);
            test.arbiter1(state, res);
        } catch (NoClassDefFoundError e) {
            testLog.println("Test sanity check failed, skipping");
            testLog.println();
            dumpFailure(testName, Status.API_MISMATCH);
            return;
        } catch (NoSuchFieldError e) {
            testLog.println("Test sanity check failed, skipping");
            testLog.println();
            dumpFailure(testName, Status.API_MISMATCH);
            return;
        } catch (NoSuchMethodError e) {
            testLog.println("Test sanity check failed, skipping");
            testLog.println();
            dumpFailure(testName, Status.API_MISMATCH);
            return;
        } catch (Throwable e) {
            testLog.println("Check test failed");
            testLog.println();
            dumpFailure(testName, Status.CHECK_TEST_ERROR);
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
            Counter<R> runResult = internalRun();

            dump(testName, runResult);
        }

        testLog.println();
    }

    @Override
    public int requiredThreads() {
        return 3;
    }

    public Counter<R> internalRun() {
        @SuppressWarnings("unchecked")
        final S[] poison = (S[]) new Object[0];

        Collection<Future<?>> tasks = new ArrayList<Future<?>>();

        final AtomicReference<StateHolder<S, R>> version = new AtomicReference<StateHolder<S, R>>();

        @SuppressWarnings("unchecked")
        final Counter<R> counter = Counters.newCounter((Class<R>) test.newResult().getClass());

        @SuppressWarnings("unchecked")
        S[] newStride = (S[]) new Object[control.minStride];
        for (int c = 0; c < control.minStride; c++) {
            newStride[c] = test.newState();
        }

        @SuppressWarnings("unchecked")
        R[] newResult = (R[]) new Result[control.minStride];
        for (int c = 0; c < control.minStride; c++) {
            newResult[c] = test.newResult();
        }

        StateHolder<S, R> holder = new StateHolder<S, R>(newStride, newResult, 2);
        version.set(holder);

        AtomicInteger epoch = new AtomicInteger();

        Future<?> a1 = pool.submit(
                new ActorBase<Actor2_Arbiter1_Test<S, R>, S, R>(1, test, version, epoch, counter, control, poison) {
                    @Override
                    protected void work1(Actor2_Arbiter1_Test<S, R> test, S state, R result) {
                        test.actor1(state, result);
                    }

                    @Override
                    protected void arbitrate(Actor2_Arbiter1_Test<S, R> test, S state, R result) {
                        test.arbiter1(state, result);
                    }
                }
        );
        tasks.add(a1);

        Future<?> a2 = pool.submit(
                new ActorBase<Actor2_Arbiter1_Test<S, R>, S, R>(2, test, version, epoch, counter, control, poison) {
                    @Override
                    protected void work2(Actor2_Arbiter1_Test<S, R> test, S state, R result) {
                        test.actor2(state, result);
                    }

                    @Override
                    protected void arbitrate(Actor2_Arbiter1_Test<S, R> test, S state, R result) {
                        test.arbiter1(state, result);
                    }
                }
        );
        tasks.add(a2);

        try {
            TimeUnit.MILLISECONDS.sleep(control.time);
        } catch (InterruptedException e) {
            // do nothing
        }

        control.isStopped = true;

        waitFor(testName, tasks);

        return counter;
    }

}
