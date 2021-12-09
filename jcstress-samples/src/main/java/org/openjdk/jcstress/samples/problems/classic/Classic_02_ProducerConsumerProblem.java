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
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class Classic_02_ProducerConsumerProblem {
    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Classic_02_ProducerConsumerProblem
    */

    /*
      ----------------------------------------------------------------------------------------------------------

        This sample shows you how JCStress can help you to test solutions for the famous producer-consumer problem.
        See https://en.wikipedia.org/wiki/Producer%E2%80%93consumer_problem for more information
        about the problem and solutions.

        The producer-consumer problem is about transferring items from the producer(s) to the consumer(s)
        in a thread-safe way. Some solutions support only one producer and one consumer while other solutions
        don't mind many producers and many consumers.
     */

    private final static int BUFFER_SIZE = 2;

    /*
      ----------------------------------------------------------------------------------------------------------

        One solution uses semaphores to solve the producer-consumer problem.
     */
    static class SemaphoresBase {
        protected final Semaphore fillCount = new Semaphore(BUFFER_SIZE);
        protected final Semaphore emptyCount = new Semaphore(BUFFER_SIZE);

        public SemaphoresBase() {
            try {
                fillCount.acquire(BUFFER_SIZE);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        public int produce() {
            try {
                emptyCount.acquire();
                int index = putItemIntoBuffer();
                fillCount.release();
                return index;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        public int consume() {
            try {
                int count = count();
                fillCount.acquire();
                takeItemFromBuffer();
                emptyCount.release();
                return count;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        protected int putItemIntoBuffer() {
            return count();
        }

        protected void takeItemFromBuffer() { }

        protected int count() {
            return fillCount.availablePermits();
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        This solution shows how semaphores can be used to solve the producer-consumer problem
        for only one producer and only one consumer.
     */
    @JCStressTest
    @Outcome(id = "true", expect = ACCEPTABLE, desc = "Trivial")
    @State
    public static class OneProducerOneConsumer extends SemaphoresBase {
        @Actor
        void producer() {
            produce();
            produce();
        }

        @Actor
        void consumer() {
            consume();
            consume();
        }

        @Arbiter
        public void fake(Z_Result r) {
            r.r1 = true;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        This solution with semaphores only works with one producer and one consumer.
        If two producers are used, then this leads to a race condition:
        Both producers might use the same index at the same time
        to put their elements into the buffer so that they overwrite each other's item.

          RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
          0, 0, 0      61.158    0,37%   Acceptable  Producers didn't overwrite each other's item.
          0, 0, 1  15.066.987   91,84%   Acceptable  Producers didn't overwrite each other's item.
          0, 0, 2       9.078    0,06%  Interesting  Producers overwrote each other's item.
          0, 1, 0      23.658    0,14%   Acceptable  Producers didn't overwrite each other's item.
          0, 1, 1      80.552    0,49%   Acceptable  Producers didn't overwrite each other's item.
          0, 1, 2     799.311    4,87%   Acceptable  Producers didn't overwrite each other's item.
          1, 0, 0      48.668    0,30%   Acceptable  Producers didn't overwrite each other's item.
          1, 0, 1      66.056    0,40%   Acceptable  Producers didn't overwrite each other's item.
          1, 0, 2     251.060    1,53%   Acceptable  Producers didn't overwrite each other's item.
     */
    @JCStressTest
    @Outcome(id = "0, 0, 2", expect = ACCEPTABLE_INTERESTING, desc = "Producers overwrote each other's item.")
    @Outcome(expect = ACCEPTABLE, desc = "Producers didn't overwrite each other's item.")
    @State
    public static class FlawedTwoProducersOneConsumer extends SemaphoresBase {
        @Actor
        void producer1(III_Result r) {
            r.r1 = produce();
        }

        @Actor
        void producer2(III_Result r) {
            r.r2 = produce();
        }

        @Actor
        void consumer(III_Result r) {
            r.r3 = consume();
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The solution with semaphores can be extended so that more than one producer and consumer are supported.
        It uses a separate lock to synchronize the access to index counter.
        This makes it impossible for producers to accidentally use the same index.
     */
    @JCStressTest
    @Outcome(id = "0, 0, 2", expect = ACCEPTABLE_INTERESTING, desc = "Producers overwrote each other's item.")
    @Outcome(expect = ACCEPTABLE, desc = "Producers didn't overwrite each other's item.")
    @State
    public static class FixedTwoProducersOneConsumer extends SemaphoresBase {
        private final Object indexLock = new Object();
        private int index = 0;

        @Actor
        void producer1(III_Result r) {
            r.r1 = produce();
        }

        @Actor
        void producer2(III_Result r) {
            r.r2 = produce();
        }

        @Actor
        void consume(III_Result r) {
            r.r3 = consume();
        }

        @Override
        protected int putItemIntoBuffer() {
            synchronized (indexLock) {
                return index++;
            }
        }

        @Override
        protected void takeItemFromBuffer() {
            synchronized (indexLock) {
                index--;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        This solution with a ReentrantLock and two conditions works with many producers and many consumers.
        While the condition full wakes up producers when
     */
    @JCStressTest
    @Outcome(id = "0, 0, 2", expect = ACCEPTABLE_INTERESTING, desc = "Producers overwrote each other's item.")
    @Outcome(expect = ACCEPTABLE, desc = "Producers didn't overwrite each other's item.")
    @State
    public static class Lock {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition full = lock.newCondition();
        private final Condition empty = lock.newCondition();
        private int count = 0;

        @Actor
        void producer1(III_Result r) {
            r.r1 = produce();
        }

        @Actor
        void producer2(III_Result r) {
            r.r2 = produce();
        }

        @Actor
        void consumer(III_Result r) {
            r.r3 = consume();
        }

        public int produce() {
            lock.lock();
            try {
                while (count == BUFFER_SIZE) {
                    full.await();
                }
                int index = count++;
                if (count == 1) {
                    empty.signalAll();
                }
                return index;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } finally {
                lock.unlock();
            }
        }

        public int consume() {
            int result;
            lock.lock();
            try {
                while (count == 0) {
                    empty.await();
                }
                result = count;
                count--;
                if (count == BUFFER_SIZE - 1) {
                    full.signalAll();
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } finally {
                lock.unlock();
            }
            return result;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        This solution with AtomicIntegers only works with one producer and one consumer.
        It fails if an int overflow happens.
     */
    @JCStressTest
    @Outcome(id = "true", expect = ACCEPTABLE, desc = "One producer produced 2 items which were consumed.")
    @State
    public static class AtomicIntegers {
        private final AtomicInteger produced = new AtomicInteger();
        private final AtomicInteger consumed = new AtomicInteger();

        @Actor
        void producer() {
            produce();
            produce();
        }

        @Actor
        void consumer() {
            consume();
            consume();
        }

        public void produce() {
            while (produced.get() - consumed.get() == BUFFER_SIZE); // spin
            produced.getAndIncrement();
        }

        public void consume() {
            while (produced.get() - consumed.get() == 0); // spin
            consumed.getAndIncrement();
        }

        @Arbiter
        public void fake(Z_Result r) {
            r.r1 = true;
        }
    }
}
