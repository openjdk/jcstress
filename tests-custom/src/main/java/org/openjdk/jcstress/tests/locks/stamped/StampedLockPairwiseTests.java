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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

public class StampedLockPairwiseTests {

    public static class State {
        public final StampedLock lock = new StampedLock();
        public int x;
        public int y;
    }

    public static abstract class AbstractStampedLockTest implements Actor2_Test<State, IntResult2> {

        @Override
        public State newState() {
            return new State();
        }

        @Override
        public IntResult2 newResult() {
            return new IntResult2();
        }

        /* ----------------- READ PATTERNS ----------------- */

        public void aRL_U(State s, IntResult2 r) {
            Lock lock = s.lock.asReadLock();
            lock.lock();
            r.r1 = s.x;
            r.r2 = s.y;
            lock.unlock();
        }

        public void aRWLr_U(State s, IntResult2 r) {
            Lock lock = s.lock.asReadWriteLock().readLock();
            lock.lock();
            r.r1 = s.x;
            r.r2 = s.y;
            lock.unlock();
        }

        public void RL_tUR(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            lock.readLock();
            r.r1 = s.x;
            r.r2 = s.y;
            lock.tryUnlockRead();
        }

        public void RL_Us(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.readLock();
            r.r1 = s.x;
            r.r2 = s.y;
            lock.unlock(stamp);
        }

        public void RL_URs(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.readLock();
            r.r1 = s.x;
            r.r2 = s.y;
            lock.unlockRead(stamp);
        }

        public void RLI_tUR(State s, IntResult2 r) {
            try {
            StampedLock lock = s.lock;
            lock.readLockInterruptibly();
            r.r1 = s.x;
            r.r2 = s.y;
            lock.tryUnlockRead();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void RLI_Us(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.readLockInterruptibly();
                r.r1 = s.x;
                r.r2 = s.y;
                lock.unlock(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void RLI_URs(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.readLockInterruptibly();
                r.r1 = s.x;
                r.r2 = s.y;
                lock.unlockRead(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tOR_V(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            int x = 0, y = 0;
            long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                x = s.x;
                y = s.y;
                if (!lock.validate(stamp)) {
                    x = 0;
                    y = 0;
                }
            }
            r.r1 = x;
            r.r2 = y;
        }

        public void tRL_tUR(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                r.r1 = s.x;
                r.r2 = s.y;
                lock.tryUnlockRead();
            }
        }

        public void tRL_Us(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                r.r1 = s.x;
                r.r2 = s.y;
                lock.unlock(stamp);
            }
        }

        public void tRL_URs(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                r.r1 = s.x;
                r.r2 = s.y;
                lock.unlockRead(stamp);
            }
        }

        public void tRLt_tUR(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.tryReadLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    r.r1 = s.x;
                    r.r2 = s.y;
                    lock.tryUnlockRead();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tRLt_Us(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.tryReadLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    r.r1 = s.x;
                    r.r2 = s.y;
                    lock.unlock(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tRLt_URs(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.tryReadLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    r.r1 = s.x;
                    r.r2 = s.y;
                    lock.unlockRead(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /* ----------------- WRITE PATTERNS ----------------- */

        public void aWL_U(State s, IntResult2 r) {
            Lock lock = s.lock.asWriteLock();
            lock.lock();
            s.x = 1;
            s.y = 2;
            lock.unlock();
        }

        public void aRWLw_U(State s, IntResult2 r) {
            Lock lock = s.lock.asReadWriteLock().writeLock();
            lock.lock();
            s.x = 1;
            s.y = 2;
            lock.unlock();
        }

        public void WL_tUW(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            lock.writeLock();
            s.x = 1;
            s.y = 2;
            lock.tryUnlockWrite();
        }

        public void orWL_V(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.readLock();
            try {
                while (s.x == 0 && s.y == 0) {
                    long ws = lock.tryConvertToWriteLock(stamp);
                    if (ws != 0L) {
                        stamp = ws;
                        s.x = 1;
                        s.y = 2;
                        break;
                    } else {
                        lock.unlockRead(stamp);
                        stamp = lock.writeLock();
                    }
                }
            } finally {
               lock.unlock(stamp);
            }
        }

        public void WL_Us(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.writeLock();
            s.x = 1;
            s.y = 2;
            lock.unlock(stamp);
        }

        public void WL_UWs(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.writeLock();
            s.x = 1;
            s.y = 2;
            lock.unlockWrite(stamp);
        }

        public void WLI_tUW(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                lock.writeLockInterruptibly();
                s.x = 1;
                s.y = 2;
                lock.tryUnlockWrite();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void WLI_Us(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.writeLockInterruptibly();
                s.x = 1;
                s.y = 2;
                lock.unlock(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void WLI_UWs(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.writeLockInterruptibly();
                s.x = 1;
                s.y = 2;
                lock.unlockWrite(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tWL_tUW(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                s.x = 1;
                s.y = 2;
                lock.tryUnlockWrite();
            }
        }

        public void tWL_Us(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                s.x = 1;
                s.y = 2;
                lock.unlock(stamp);
            }
        }

        public void tWL_UWs(State s, IntResult2 r) {
            StampedLock lock = s.lock;
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                s.x = 1;
                s.y = 2;
                lock.unlockWrite(stamp);
            }
        }

        public void tWLt_tUW(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    s.x = 1;
                    s.y = 2;
                    lock.tryUnlockWrite();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tWLt_Us(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    s.x = 1;
                    s.y = 2;
                    lock.unlock(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tWLt_UWs(State s, IntResult2 r) {
            try {
                StampedLock lock = s.lock;
                long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    s.x = 1;
                    s.y = 2;
                    lock.unlockWrite(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /* --------------- CARTESIAN PRODUCT OF READ/WRITE PATTERNS ------------  */

    public abstract static class aRL_U extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { aRL_U(s, r); }

        public abstract static class Base extends aRL_U {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class aRWLr_U extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { aRWLr_U(s, r); }

        public abstract static class Base extends aRWLr_U {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class RL_tUR extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { RL_tUR(s, r); }

        public abstract static class Base extends RL_tUR {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class RL_Us extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { RL_Us(s, r); }

        public abstract static class Base extends RL_Us {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class RL_URs extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { RL_URs(s, r); }

        public abstract static class Base extends RL_URs {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class RLI_tUR extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { RLI_tUR(s, r); }

        public abstract static class Base extends RLI_tUR {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class RLI_Us extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { RLI_Us(s, r); }

        public abstract static class Base extends RLI_Us {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class RLI_URs extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { RLI_URs(s, r); }

        public abstract static class Base extends RLI_URs {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tOR_V extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tOR_V(s, r); }

        public abstract static class Base extends tOR_V {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tRL_tUR extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tRL_tUR(s, r); }

        public abstract static class Base extends tRL_tUR {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tRL_Us extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tRL_Us(s, r); }

        public abstract static class Base extends tRL_Us {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tRL_URs extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tRL_URs(s, r); }

        public abstract static class Base extends tRL_URs {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tRLt_tUR extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tRLt_tUR(s, r); }

        public abstract static class Base extends tRLt_tUR {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tRLt_Us extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tRLt_Us(s, r); }

        public abstract static class Base extends tRLt_Us {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

    public abstract static class tRLt_URs extends AbstractStampedLockTest {
        @Override
        public void actor1(State s, IntResult2 r) { tRLt_URs(s, r); }

        public abstract static class Base extends tRLt_URs {}

        public static class aWL_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aWL_U(s, r); }
        }
        public static class aRWLw_U extends Base {
            @Override public void actor2(State s, IntResult2 r) { aRWLw_U(s, r); }
        }
        public static class WL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_tUW(s, r); }
        }
        public static class WL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_Us(s, r); }
        }
        public static class WL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WL_UWs(s, r); }
        }
        public static class WLI_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_tUW(s, r); }
        }
        public static class WLI_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_Us(s, r); }
        }
        public static class WLI_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { WLI_UWs(s, r); }
        }
        public static class tWL_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_tUW(s, r); }
        }
        public static class tWL_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_Us(s, r); }
        }
        public static class tWL_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWL_UWs(s, r); }
        }
        public static class tWLt_tUW extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_tUW(s, r); }
        }
        public static class tWLt_Us extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_Us(s, r); }
        }
        public static class tWLt_UWs extends Base {
            @Override public void actor2(State s, IntResult2 r) { tWLt_UWs(s, r); }
        }
        public static class orWL_V extends Base {
            @Override public void actor2(State s, IntResult2 r) { orWL_V(s, r); }
        }
    }

}
