/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.concurrency.classic;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/*
    How to run this test:
        $ java -jar jcstress-samples/target/jcstress.jar -t ClassicProblem_01_DiningPhilosophers

    If you run this sample with Java 15+, check the -XX:+UseBiasedLocking flag for monitors
    because biased locking is disabled by default which can harm the performance.
 */

/**
 * This sample shows you how JCStress can help you to test solutions for the famous Dining philosophers problem.
 * It solves this problem by monitors, reentrant locks and semaphores.
 * See https://en.wikipedia.org/wiki/Dining_philosophers_problem for more information about the problem.
 */
public abstract class ClassicProblem_01_DiningPhilosophers {

    static abstract class Base {
        final protected void eat(int fork1, int fork2) {
            try {
                pickFork(fork1);
                pickFork(fork2);
                // eating
                dropFork(fork1);
                dropFork(fork2);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        protected abstract void pickFork(int fork) throws InterruptedException;

        protected abstract void dropFork(int fork);
    }

    @JCStressTest
    @Outcome(id = {"true"}, expect = ACCEPTABLE, desc = "All philosophers could eat with their 2 neighboured forks.")
    @Outcome(expect = FORBIDDEN, desc = "At least one philosopher couldn't eat.")
    @State
    public static class Monitors extends Base {
        private final Object lock = new Object();
        private final boolean[] forksInUse = new boolean[4];

        @Actor
        public void p1() {
            // think
            eat(0, 1);
        }

        @Actor
        public void p2() {
            // think
            eat(1, 2);
        }

        @Actor
        public void p3() {
            // think
            eat(2, 3);
        }

        @Actor
        public void p4() {
            // think
            eat(0, 3); // and not eat(3, 0) because we must acquire all locks in the same order to avoid deadlocks
        }

        @Arbiter
        public void fake(Z_Result r) {
            r.r1 = true;
        }

        @Override
        protected void pickFork(int fork) {
            synchronized (lock) {
                while (forksInUse[fork]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                    forksInUse[fork] = true;
                }
            }
        }

        @Override
        protected void dropFork(int fork) {
            synchronized (lock) {
                forksInUse[fork] = false;
                lock.notifyAll();
            }
        }
    }


    @JCStressTest
    @Outcome(id = {"true"}, expect = ACCEPTABLE, desc = "All philosophers could eat with their 2 neighboured forks.")
    @Outcome(expect = FORBIDDEN, desc = "At least one philosopher couldn't eat.")
    @State
    public static class ReentrantLocks extends Base {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition forkReleased = lock.newCondition();
        private final boolean[] forksInUse = new boolean[4];

        @Actor
        public void p1() {
            // think
            eat(0, 1);
        }

        @Actor
        public void p2() {
            // think
            eat(1, 2);
        }

        @Actor
        public void p3() {
            // think
            eat(2, 3);
        }

        @Actor
        public void p4() {
            // think
            eat(0, 3); // and not eat(3, 0) because we must acquire all locks in the same order to avoid deadlocks
        }

        @Arbiter
        public void fake(Z_Result r) {
            r.r1 = true;
        }

        @Override
        protected void pickFork(int fork) throws InterruptedException {
            lock.lock();
            try {
                while(forksInUse[fork]) {
                    forkReleased.await();
                }
                forksInUse[fork] = true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        protected void dropFork(int fork) {
            lock.lock();
            try {
                forksInUse[fork] = false;
                forkReleased.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    @JCStressTest
    @Outcome(id = {"true"}, expect = ACCEPTABLE, desc = "All philosophers could eat with their 2 neighboured forks.")
    @Outcome(expect = FORBIDDEN, desc = "At least one philosopher couldn't eat.")
    @State
    public static class Semaphores extends Base {
        private final Semaphore[] semaphores =
                new Semaphore[] {
                        new Semaphore(1), new Semaphore(1),
                        new Semaphore(1), new Semaphore(1)};
        @Actor
        public void p1() {
            // think
            eat(0, 1);
        }

        @Actor
        public void p2() {
            // think
            eat(1, 2);
        }

        @Actor
        public void p3() {
            // think
            eat(2, 3);
        }

        @Actor
        public void p4() {
            // think
            eat(0, 3); // and not eat(3, 0) because we must acquire all locks in the same order to avoid deadlocks
        }

        @Arbiter
        public void fake(Z_Result r) {
            r.r1 = true;
        }

        @Override
        protected void pickFork(int fork) throws InterruptedException {
            semaphores[fork].acquire();
        }

        @Override
        protected void dropFork(int fork) {
            semaphores[fork].release();
        }
    }
}
