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

import org.openjdk.jcstress.tests.ActorConcurrencyTest;
import org.openjdk.jcstress.util.Counter;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Actor base.
 *
 * This serves as the base for the worker threads. This class is optimized to extreme,
 * because the speed of the infrastructure is essential to get enough samples for the tests,
 * effectively meaning "faster infra => more reliability for probabilistic tests".
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public abstract class ActorBase<T extends ActorConcurrencyTest<S, R>, S, R> implements Callable<Void> {
    private final int index;
    private final T test;
    private final AtomicReference<StateHolder<S, R>> version;
    private final AtomicInteger epoch;
    private final Counter<R> counter;
    private final ControlHolder control;
    private final S[] poison;

    public ActorBase(int index, T test, AtomicReference<StateHolder<S,R>> version, AtomicInteger epoch, Counter<R> counter, ControlHolder control, S[] poison) {
        this.index = index;
        this.test = test;
        this.version = version;
        this.epoch = epoch;
        this.counter = counter;
        this.control = control;
        this.poison = poison;
    }

    // fired concurrently for each worker
    protected void work1(T test, S state, R result) {
        // no default implementation
    }

    // fired concurrently for each worker
    protected void work2(T test, S state, R result) {
        // no default implementation
    }

    // fired concurrently for each worker
    protected void work3(T test, S state, R result) {
        // no default implementation
    }

    // fired concurrently for each worker
    protected void work4(T test, S state, R result) {
        // no default implementation
    }

    // fired by one of the workers before consuming the results
    protected void arbitrate(T test, S state, R result) {
        // no default implementation
    }

    public Void call() {
        int lastLoops = 0;

        int[] indices = null;
        int curEpoch = 0;

        boolean shouldYield = control.shouldYield;
        int maxStride = control.maxStride;

        Counter<R> lCounter = counter;
        T lt = test;
        int lIndex = index;
        S[] lPoison = poison;

        while (true) {
            // poll the holder and my relevant state
            StateHolder<S, R> holder = version.get();
            S[] cur = holder.s;
            R[] res = holder.r;

            // got the poison pill, break out
            if (cur == lPoison) {
                return null;
            }

            int loops = holder.loops;

            // check if we need to refit our collections
            if (loops != lastLoops) {
                lastLoops = loops;
                indices = Runner.generatePermutation(loops);
            }

            holder.announceReady();
            while (holder.notAllReady) {
                if (shouldYield) Thread.yield();
            }

            holder.announceStarted();

            // specialize to avoid megamorphic call here.
            switch (lIndex) {
                case 1:
                    for (int l = 0; l < loops; l++) {
                        int index = indices[l];
                        work1(lt, cur[index], res[index]);
                    }
                    break;

                case 2:
                    for (int l = 0; l < loops; l++) {
                        int index = indices[l];
                        work2(lt, cur[index], res[index]);
                    }
                    break;

                case 3:
                    for (int l = 0; l < loops; l++) {
                        int index = indices[l];
                        work3(lt, cur[index], res[index]);
                    }
                    break;

                case 4:
                    for (int l = 0; l < loops; l++) {
                        int index = indices[l];
                        work4(lt, cur[index], res[index]);
                    }
                    break;

                default:
                    throw new IllegalStateException("Unhandled index: " + lIndex);
            }

            // notify we had consumed the stride and pushed the result
            // Important: This acts as the release edge for $res updates
            holder.announceFinished();

            // check if anyone is lagging behind:
            // this is a naive feedback control to increase stride size on demand
            holder.hasLaggedWorkers |= holder.notAllStarted;

            // wait for everyone else to finish
            // Important: This acts as the acquire edge for $res updates
            while (holder.notAllFinished) {
                if (shouldYield) Thread.yield();
            }

            // segregate the runner roles:
            //   - first race winner will consume the results
            //   - second race winner will prepare the next state
            //   - others will wait until first two are completed

            if (epoch.compareAndSet(curEpoch, curEpoch + 1)) {

                // see if we need arbitrage
                for (int l = 0; l < loops; l++) {
                    int index = indices[l];
                    arbitrate(lt, cur[index], res[index]);
                }

                // consume!
                for (R r1 : res) {
                    lCounter.record(r1);
                }

            }

            if (epoch.compareAndSet(curEpoch + 1, curEpoch + 2)) {
                // prepare the new chunk of work
                StateHolder<S, R> newHolder;
                if (control.isStopped) {
                    newHolder = new StateHolder<S, R>(lPoison, null, holder.countWorkers);
                } else {
                    // feedback: should bump the stride size?
                    int newLoops = holder.hasLaggedWorkers ? Math.min(loops * 2, maxStride) : loops;

                    S[] newStride = Arrays.copyOf(cur, newLoops);
                    for (int c = 0; c < newLoops; c++) {
                        newStride[c] = lt.newState();
                    }

                    R[] newRes = Arrays.copyOf(res, newLoops);
                    for (int c = 0; c < newLoops; c++) {
                        newRes[c] = lt.newResult();
                    }
                    newHolder = new StateHolder<S, R>(newStride, newRes, holder.countWorkers);
                }

                version.set(newHolder);
            }

            // defensive cross-check against global
            curEpoch += 2;
            while (curEpoch != epoch.get()) {
                if (shouldYield) Thread.yield();
            }

            // wait for the entire group
            holder.announceConsumed();
            while (holder.notAllConsumed) {
                if (shouldYield) Thread.yield();
            }

        }

    }

}
