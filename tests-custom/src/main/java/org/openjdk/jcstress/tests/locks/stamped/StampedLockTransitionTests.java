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

import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.tests.Actor2_Test;

import java.util.concurrent.locks.StampedLock;

public class StampedLockTransitionTests {

    public static class State {
        public final StampedLock lock = new StampedLock();
    }

    public abstract static class AbstractBase implements Actor2_Test<StampedLockTransitionTests.State, IntResult2> {
        @Override
        public State newState() {
            return new State();
        }

        @Override
        public IntResult2 newResult() {
            return new IntResult2();
        }


        public int optimistic_optimistic(StampedLock lock) {
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                long sw = lock.tryConvertToOptimisticRead(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int optimistic_read(StampedLock lock) {
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                long sw = lock.tryConvertToReadLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int optimistic_write(StampedLock lock) {
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                long sw = lock.tryConvertToWriteLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int read_optimistic(StampedLock lock) {
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToOptimisticRead(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int read_read(StampedLock lock) {
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToReadLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int read_write(StampedLock lock) {
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToWriteLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int write_optimistic(StampedLock lock) {
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToOptimisticRead(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int write_read(StampedLock lock) {
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToReadLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }

        public int write_write(StampedLock lock) {
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                long sw = lock.tryConvertToWriteLock(stamp);
                return (sw == 0) ? 0 : 1;
            } else {
                return -1;
            }
        }
    }

    public static class OO_OO extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = optimistic_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = optimistic_optimistic(s.lock); }
    }

    public static class OO_OR extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = optimistic_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = optimistic_read(s.lock); }
    }

    public static class OO_OW extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = optimistic_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = optimistic_write(s.lock); }
    }

    public static class OR_OW extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = optimistic_read(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = optimistic_write(s.lock); }
    }

    public static class RO_RO extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = read_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = read_optimistic(s.lock); }
    }

    public static class RO_RR extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = read_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = read_read(s.lock); }
    }

    public static class RO_RW extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = read_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = read_write(s.lock); }
    }

    public static class RR_RW extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = read_read(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = read_write(s.lock); }
    }

    public static class WO_WO extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = write_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = write_optimistic(s.lock); }
    }

    public static class WO_WR extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = write_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = write_read(s.lock); }
    }

    public static class WO_WW extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = write_optimistic(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = write_write(s.lock); }
    }

    public static class WR_WW extends AbstractBase {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = write_read(s.lock); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = write_write(s.lock); }
    }

}
