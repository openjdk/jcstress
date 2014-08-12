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
package org.openjdk.jcstress.tests.locks.stamped;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;

import java.util.concurrent.locks.StampedLock;

public class StampedLockTransitionTests {

    @State
    public static class S {
        public final StampedLock lock = new StampedLock();

        public int optimistic_optimistic() {
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                long sw = lock.tryConvertToOptimisticRead(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int optimistic_read() {
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                long sw = lock.tryConvertToReadLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int optimistic_write() {
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                long sw = lock.tryConvertToWriteLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int read_optimistic() {
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToOptimisticRead(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int read_read() {
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToReadLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int read_write() {
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToWriteLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int write_optimistic() {
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToOptimisticRead(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int write_read() {
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToReadLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int write_write() {
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToWriteLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }
    }

    @JCStressTest
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "Both optimistic reads have successfully validated.")
    public static class OO_OO {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_optimistic(); }
    }

    @JCStressTest
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "T1 had successfully validated the optimistic read. T2 had acquired the read lock.")
    @Outcome(id = "[0, 1]", expect = Expect.ACCEPTABLE, desc = "T1 had failed to convert to the optimistic read. T2 had acquired the read lock.")
    public static class OO_OR {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_read(); }
    }

    @JCStressTest
    @Outcome(id = "[1, 1]",              expect = Expect.ACCEPTABLE, desc = "T1 had successfully validated the optimistic read. T2 had acquired the write lock.")
    @Outcome(id = {"[0, 1]", "[-1, 1]"}, expect = Expect.ACCEPTABLE, desc = "T1 had failed to acquire/convert to the optimistic read. T2 had acquired the write lock.")
    public static class OO_OW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_write(); }
    }

    @JCStressTest
    @Outcome(id = "[0, 1]",  expect = Expect.ACCEPTABLE, desc = "T1 had failed to convert optimistic to read, because T2 had acquired the write lock.")
    @Outcome(id = "[-1, 1]", expect = Expect.ACCEPTABLE, desc = "T1 had failed to acquire optimistic, because T2 had acquired the write lock.")
    @Outcome(id = "[1, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 had converted optimistic to read, and T2 had failed to acquire the write lock.")
    public static class OR_OW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_read(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_write(); }
    }

    @JCStressTest
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "Both threads have acquired read locks; both have successfully converted to optimistic.")
    public static class RO_RO {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_optimistic(); }
    }

    @JCStressTest
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "Both threads have acquired read locks; both have successfully converted to optimistic/read.")
    public static class RO_RR {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_read(); }
    }

    @JCStressTest
    @Outcome(id = "[1, 1]",  expect = Expect.ACCEPTABLE, desc = "Both threads have acquired read locks; T1 had acquired optimistic, then T2 acquire write lock.")
    @Outcome(id = "[-1, 1]", expect = Expect.ACCEPTABLE, desc = "T2 had successfully acquired read, then write lock; T1 had failed to acquire read lock.")
    @Outcome(id = "[1, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 had successfully acquired read lock, had successfully converted to optimistic. T2 had failed to acquire write lock, while T1 was holding the read.")
    public static class RO_RW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_write(); }
    }

    @JCStressTest
    @Outcome(id = "[-1, 1]", expect = Expect.ACCEPTABLE, desc = "T2 had successfully acquired read, then write lock; T1 had failed to acquire read lock.")
    @Outcome(id = "[1, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 had successfully acquired read lock, had successfully converted to read lock. T2 had failed to acquire write lock, while T1 was holding the read.")
    public static class RR_RW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_read(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_write(); }
    }

    @JCStressTest
    @Outcome(id = {"[-1, 1]", "[1, -1]"}, expect = Expect.ACCEPTABLE, desc = "One of the threads had successfully acquired the write lock, and converted it to optimistic. Another thread had failed to acquire write lock, letting first thread to transit to optimistic.")
    @Outcome(id = "[1, 1]",               expect = Expect.ACCEPTABLE, desc = "Both threads have acquired the write lock, and were quick enough to convert them to the optimistic.")
    public static class WO_WO {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_optimistic(); }
    }

    @JCStressTest
    @Outcome(id = {"[-1, 1]", "[1, -1]"}, expect = Expect.ACCEPTABLE, desc = "One of the threads had successfully acquired the write lock, and converted it to optimistic. Another thread had failed to acquire write lock, letting first thread to transit to read.")
    @Outcome(id = "[1, 1]",               expect = Expect.ACCEPTABLE, desc = "Both threads have acquired the write lock, and were quick enough to convert them to the read locks.")
    public static class WO_WR {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_read(); }
    }

    @JCStressTest
    @Outcome(id = {"[-1, 1]", "[1, -1]"}, expect = Expect.ACCEPTABLE, desc = "One of the thread had succeeded in acquiring the write lock, breaking another from acquiring another write lock.")
    @Outcome(id = "[1, 1]",               expect = Expect.ACCEPTABLE, desc = "T1 had succeeded in acquiring the write lock, and converted it to optimistic; which allowed T2 to acquire and hold the write lock.")
    public static class WO_WW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_write(); }
    }

    @JCStressTest
    @Outcome(id = {"[-1, 1]", "[1, -1]"}, expect = Expect.ACCEPTABLE, desc = "One of the thread had succeeded in acquiring the write lock, breaking another from acquiring another write lock. T1 conversion to read lock still prohibits T2 from acquiring the write lock.")
    public static class WR_WW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_read(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_write(); }
    }

}
