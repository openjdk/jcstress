/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.problems.classic;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

public class Classic_01_DiningPhilosophers {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Classic_01_DiningPhilosophers[.SubTestName]
    */

    /*
      ----------------------------------------------------------------------------------------------------------

        This sample shows the solutions for the classic Dining philosophers problem.
            See: https://en.wikipedia.org/wiki/Dining_philosophers_problem

        There is a round table where philosophers sit. Every philosopher has two modes:
        thinking and eating. A philosopher needs two adjacent forks to eat. The problem
        is to write the algorithm that lets philosophers eat without deadlocks, starvation,
        and with fairness. For the purposes of this example, we don't model thinking.

        The trivial deadlock in this problem is when every philosopher holds one fork,
        and waits for other fork to drop. If all philosophers take the fork on one side,
        no philosophers would be able to complete. // TODO: Demo with multi-actor termination tests.

        The first solution is "Resource Hierarchy": it avoids the deadlock by asking the
        last philosopher to take the forks in the _different_ order.

        Indeed, no deadlock occurs:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            true  6,325,295,104  100.00%  Acceptable  Trivial.
     */

    @JCStressTest
    @Outcome(expect = ACCEPTABLE, desc = "Trivial.")
    @State
    public static class ResourceHierarchy {
        private final Object[] forks = new Object[] { new Object(), new Object(), new Object() };

        @Actor
        public void p1() {
            eat(0, 1);
        }

        @Actor
        public void p2() {
            eat(1, 2);
        }

        @Actor
        public void p3() {
            eat(0, 2); // in different order
        }

        @Arbiter
        public void fake(Z_Result r) {
            // Fake the result. The actual failure is deadlock.
            r.r1 = true;
        }

        final protected void eat(int fork1, int fork2) {
            synchronized (forks[fork1]) {
                synchronized (forks[fork2]) {
                    // Forks acquired. Do nothing. Release forks on exit.
                }
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Another solution is to introduce Arbitrator. Indeed, having a waiter to serve the forks
        solves the deadlock by not letting circular resource waits. For the purposes of this test,
        philosopher busy-wait (sic!) on a waiter.

        Indeed, no deadlock occurs:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            true  6,270,081,024  100.00%  Acceptable  Trivial.
     */

    @JCStressTest
    @Outcome(id = "true", expect = ACCEPTABLE, desc = "Trivial.")
    @State
    public static class Arbitrator  {
        private final boolean[] forks = new boolean[3];
        private final Object waiter = new Object();

        @Actor
        public void p1() {
            eat(0, 1);
        }

        @Actor
        public void p2() {
            eat(1, 2);
        }

        @Actor
        public void p3() {
            eat(2, 0);
        }

        @Arbiter
        public void fake(Z_Result r) {
            // Fake the result. The actual failure is deadlock.
            r.r1 = true;
        }

        void eat(int f1, int f2) {
            // Acquire forks
            while (true) {
                synchronized (waiter) {
                    if (!forks[f1] && !forks[f1]) {
                        // Success!
                        forks[f1] = true;
                        forks[f2] = true;
                        break;
                    }
                }
            }

            // Release forks
            forks[f1] = false;
            forks[f2] = false;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Yet another solution is to make sure that no more than N-1 philosopher eat at a time.
        By construction, this guarantees that last philosopher would wait for adjacent eater
        to complete before trying to acquire forks.

        Indeed, no deadlock occurs:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            true  5,377,787,904  100.00%  Acceptable  Trivial.
     */

    @JCStressTest
    @Outcome(id = "true", expect = ACCEPTABLE, desc = "Trivial.")
    @State
    public static class OneDinerFewer {
        private final AtomicIntegerArray forks = new AtomicIntegerArray(3);
        private final Semaphore diners = new Semaphore(2);

        @Actor
        public void p1() {
            eat(0, 1);
        }

        @Actor
        public void p2() {
            eat(1, 2);
        }

        @Actor
        public void p3() {
            eat(2, 0);
        }

        @Arbiter
        public void fake(Z_Result r) {
            // Fake the result. The actual failure is deadlock.
            r.r1 = true;
        }

        void eat(int f1, int f2) {
            // Acquire forks
            while (true) {
                try {
                    diners.acquire();
                    if (forks.compareAndSet(f1, 0, 1)) {
                        if (forks.compareAndSet(f2, 0, 1)) {
                            // Success!
                            break;
                        } else {
                            forks.set(f1, 0);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                } finally {
                    diners.release();
                }
            }

            // Release forks
            forks.set(f1, 0);
            forks.set(f2, 0);
        }
    }
}
