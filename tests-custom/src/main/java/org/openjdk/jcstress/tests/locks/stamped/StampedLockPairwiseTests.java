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
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

public class StampedLockPairwiseTests {

    @Description("Tests if StampedLock successfully guards against non-atomic read/writes.")
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Seeing the default values.")
    @Outcome(id = "1, 2", expect = Expect.ACCEPTABLE, desc = "Seeing the complete update.")
    public static class G {

    }

    @State
    public static class S {
        public final StampedLock lock = new StampedLock();
        public int x;
        public int y;

        /* ----------------- READ PATTERNS ----------------- */

        public void aRL_U(II_Result r) {
            Lock lock = this.lock.asReadLock();
            lock.lock();
            r.r1 = x;
            r.r2 = y;
            lock.unlock();
        }

        public void aRWLr_U(II_Result r) {
            Lock lock = this.lock.asReadWriteLock().readLock();
            lock.lock();
            r.r1 = x;
            r.r2 = y;
            lock.unlock();
        }

        public void RL_tUR(II_Result r) {
            StampedLock lock = this.lock;
            lock.readLock();
            r.r1 = x;
            r.r2 = y;
            lock.tryUnlockRead();
        }

        public void RL_Us(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.readLock();
            r.r1 = x;
            r.r2 = y;
            lock.unlock(stamp);
        }

        public void RL_URs(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.readLock();
            r.r1 = x;
            r.r2 = y;
            lock.unlockRead(stamp);
        }

        public void RLI_tUR(II_Result r) {
            try {
            StampedLock lock = this.lock;
            lock.readLockInterruptibly();
            r.r1 = x;
            r.r2 = y;
            lock.tryUnlockRead();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void RLI_Us(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.readLockInterruptibly();
                r.r1 = x;
                r.r2 = y;
                lock.unlock(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void RLI_URs(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.readLockInterruptibly();
                r.r1 = x;
                r.r2 = y;
                lock.unlockRead(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tOR_V(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryOptimisticRead();
            int lx = x;
            int ly = y;
            if (lock.validate(stamp)) {
                r.r1 = lx;
                r.r2 = ly;
            }
        }

        public void tRL_tUR(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                r.r1 = x;
                r.r2 = y;
                lock.tryUnlockRead();
            }
        }

        public void tRL_Us(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                r.r1 = x;
                r.r2 = y;
                lock.unlock(stamp);
            }
        }

        public void tRL_URs(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryReadLock();
            if (stamp != 0) {
                r.r1 = x;
                r.r2 = y;
                lock.unlockRead(stamp);
            }
        }

        public void tRLt_tUR(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.tryReadLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    r.r1 = x;
                    r.r2 = y;
                    lock.tryUnlockRead();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tRLt_Us(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.tryReadLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    r.r1 = x;
                    r.r2 = y;
                    lock.unlock(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tRLt_URs(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.tryReadLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    r.r1 = x;
                    r.r2 = y;
                    lock.unlockRead(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /* ----------------- WRITE PATTERNS ----------------- */

        public void aWL_U(II_Result r) {
            Lock lock = this.lock.asWriteLock();
            lock.lock();
            x = 1;
            y = 2;
            lock.unlock();
        }

        public void aRWLw_U(II_Result r) {
            Lock lock = this.lock.asReadWriteLock().writeLock();
            lock.lock();
            x = 1;
            y = 2;
            lock.unlock();
        }

        public void WL_tUW(II_Result r) {
            StampedLock lock = this.lock;
            lock.writeLock();
            x = 1;
            y = 2;
            lock.tryUnlockWrite();
        }

        public void orWL_V(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.readLock();
            try {
                while (x == 0 && y == 0) {
                    long ws = lock.tryConvertToWriteLock(stamp);
                    if (ws != 0L) {
                        stamp = ws;
                        x = 1;
                        y = 2;
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

        public void WL_Us(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.writeLock();
            x = 1;
            y = 2;
            lock.unlock(stamp);
        }

        public void WL_UWs(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.writeLock();
            x = 1;
            y = 2;
            lock.unlockWrite(stamp);
        }

        public void WLI_tUW(II_Result r) {
            try {
                StampedLock lock = this.lock;
                lock.writeLockInterruptibly();
                x = 1;
                y = 2;
                lock.tryUnlockWrite();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void WLI_Us(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.writeLockInterruptibly();
                x = 1;
                y = 2;
                lock.unlock(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void WLI_UWs(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.writeLockInterruptibly();
                x = 1;
                y = 2;
                lock.unlockWrite(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tWL_tUW(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                x = 1;
                y = 2;
                lock.tryUnlockWrite();
            }
        }

        public void tWL_Us(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                x = 1;
                y = 2;
                lock.unlock(stamp);
            }
        }

        public void tWL_UWs(II_Result r) {
            StampedLock lock = this.lock;
            long stamp = lock.tryWriteLock();
            if (stamp != 0) {
                x = 1;
                y = 2;
                lock.unlockWrite(stamp);
            }
        }

        public void tWLt_tUW(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    x = 1;
                    y = 2;
                    lock.tryUnlockWrite();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tWLt_Us(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    x = 1;
                    y = 2;
                    lock.unlock(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void tWLt_UWs(II_Result r) {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.tryWriteLock(1, TimeUnit.SECONDS);
                if (stamp != 0) {
                    x = 1;
                    y = 2;
                    lock.unlockWrite(stamp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /* --------------- CARTESIAN PRODUCT OF READ/WRITE PATTERNS ------------  */

    public abstract static class aRL_U {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.aRL_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class aRWLr_U {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.aRWLr_U(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class RL_tUR {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.RL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class RL_Us {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.RL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class RL_URs {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.RL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public abstract static class RLI_tUR  {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.RLI_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class RLI_Us {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.RLI_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class RLI_URs {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.RLI_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class tOR_V {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tOR_V(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class tRL_tUR {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tRL_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class tRL_Us {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tRL_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public static class tRL_URs {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tRL_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public abstract static class tRLt_tUR {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_tUR(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public abstract static class tRLt_Us {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_Us(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

    public abstract static class tRLt_URs {

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aWL_U {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aWL_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class aRWLw_U {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.aRWLw_U(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class WLI_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WLI_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWL_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.WL_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_tUW {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_tUW(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_Us {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_Us(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class tWLt_UWs {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.tWLt_UWs(r); }
        }

        @JCStressTest
        @JCStressMeta(G.class)
        public static class orWL_V {
            @Actor public void actor1(S s, II_Result r) { s.tRLt_URs(r); }
            @Actor public void actor2(S s, II_Result r) { s.orWL_V(r); }
        }
    }

}
