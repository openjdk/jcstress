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

import org.openjdk.jcstress.infra.annotations.Actor;
import org.openjdk.jcstress.infra.annotations.ConcurrencyStressTest;
import org.openjdk.jcstress.infra.annotations.State;
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

    @ConcurrencyStressTest
    public static class OO_OO {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_optimistic(); }
    }

    @ConcurrencyStressTest
    public static class OO_OR {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_read(); }
    }

    @ConcurrencyStressTest
    public static class OO_OW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_write(); }
    }

    @ConcurrencyStressTest
    public static class OR_OW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.optimistic_read(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.optimistic_write(); }
    }

    @ConcurrencyStressTest
    public static class RO_RO {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_optimistic(); }
    }

    @ConcurrencyStressTest
    public static class RO_RR {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_read(); }
    }

    @ConcurrencyStressTest
    public static class RO_RW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_write(); }
    }

    @ConcurrencyStressTest
    public static class RR_RW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.read_read(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.read_write(); }
    }

    @ConcurrencyStressTest
    public static class WO_WO {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_optimistic(); }
    }

    @ConcurrencyStressTest
    public static class WO_WR {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_read(); }
    }

    @ConcurrencyStressTest
    public static class WO_WW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_optimistic(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_write(); }
    }

    @ConcurrencyStressTest
    public static class WR_WW {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.write_read(); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.write_write(); }
    }

}
