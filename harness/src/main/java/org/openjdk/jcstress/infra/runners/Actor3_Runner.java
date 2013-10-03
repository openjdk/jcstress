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
import org.openjdk.jcstress.tests.Actor3_Test;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.Counters;
import org.openjdk.jcstress.util.VMSupport;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Actor3_Runner<S, R extends Result> extends Runner {
    final Actor3_Test<S, R> test;

    public Actor3_Runner(Options opts, Actor3_Test<S, R> test, TestResultCollector collector, ExecutorService pool) throws FileNotFoundException, JAXBException {
        super(opts, collector, pool);
        this.test = test;
    }

    /**
     * Run the test.
     * This method blocks until test is complete
     *
     * @throws InterruptedException
     * @throws java.util.concurrent.ExecutionException
     *
     */
    public void run() throws ExecutionException, InterruptedException {
        testLog.println("Running " + test.getClass().getName());

        Status status = checkRun();
        if (status != Status.NORMAL) {
            testLog.println("Test sanity check failed, skipping");
            testLog.println();
            dumpFailure(test, status);
            return;
        }

        testLog.print("Iterations ");
        for (int c = 0; c < iters; c++) {
            try {
                VMSupport.tryDeoptimizeAllInfra(deoptRatio);
            } catch (NoClassDefFoundError err) {
                // gracefully "handle"
            }

            testLog.print(".");
            testLog.flush();
            Counter<R> runResult = run(time);

            dump(test, runResult);
        }
        testLog.println();
    }

    @Override
    public int requiredThreads() {
        return 3;
    }

    private Status checkRun() {
        try {
            R res1 = test.newResult();
            R res2 = test.newResult();
            R res3 = test.newResult();
            S state = test.newState();
            test.actor1(state, res1);
            test.actor2(state, res2);
            test.actor3(state, res3);
            return Status.NORMAL;
        } catch (NoClassDefFoundError e) {
            return Status.API_MISMATCH;
        } catch (NoSuchFieldError e) {
            return Status.API_MISMATCH;
        } catch (NoSuchMethodError e) {
            return Status.API_MISMATCH;
        } catch (Throwable e) {
            e.printStackTrace();
            return Status.CHECK_TEST_ERROR;
        }
    }

    private Counter<R> run(int time) throws InterruptedException, ExecutionException {

        @SuppressWarnings("unchecked")
        final S[] poison = (S[]) new Object[0];

        ControlHolder controlHolder = new ControlHolder(minStride, maxStride, shouldYield);

        Collection<Future<?>> tasks = new ArrayList<Future<?>>();

        final AtomicReference<StateHolder<S, R>> version = new AtomicReference<StateHolder<S, R>>();

        @SuppressWarnings("unchecked")
        final Counter<R> counter = Counters.newCounter((Class<R>) test.newResult().getClass());

        @SuppressWarnings("unchecked")
        S[] newStride = (S[]) new Object[minStride];
        for (int c = 0; c < minStride; c++) {
            newStride[c] = test.newState();
        }

        @SuppressWarnings("unchecked")
        R[] newResult = (R[]) new Result[minStride];
        for (int c = 0; c < minStride; c++) {
            newResult[c] = test.newResult();
        }

        StateHolder<S, R> holder = new StateHolder<S, R>(newStride, newResult, 3);
        version.set(holder);

        AtomicInteger epoch = new AtomicInteger();

        tasks.add(pool.submit(
                new ActorBase<Actor3_Test<S, R>, S, R>(1, test, version, epoch, counter, controlHolder, poison) {
                    @Override
                    protected void work1(Actor3_Test<S, R> test, S state, R result) {
                        test.actor1(state, result);
                    }
                }
        ));

        tasks.add(pool.submit(
                new ActorBase<Actor3_Test<S, R>, S, R>(2, test, version, epoch, counter, controlHolder, poison) {
                    @Override
                    protected void work2(Actor3_Test<S, R> test, S state, R result) {
                        test.actor2(state, result);
                    }
                }
        ));

        tasks.add(pool.submit(
                new ActorBase<Actor3_Test<S, R>, S, R>(3, test, version, epoch, counter, controlHolder, poison) {
                    @Override
                    protected void work3(Actor3_Test<S, R> test, S state, R result) {
                        test.actor3(state, result);
                    }
                }
        ));

        TimeUnit.MILLISECONDS.sleep(time);

        controlHolder.isStopped = true;

        if (!waitFor(tasks)) {
            dumpFailure(test, Status.TIMEOUT_ERROR);
        }

        return counter;
    }

}
