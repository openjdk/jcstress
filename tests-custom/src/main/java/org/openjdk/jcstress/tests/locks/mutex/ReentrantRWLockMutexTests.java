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
package org.openjdk.jcstress.tests.locks.mutex;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.tests.locks.BothSucceed;
import org.openjdk.jcstress.tests.locks.NoOneSucceeds;
import org.openjdk.jcstress.tests.locks.OneSucceeds;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantRWLockMutexTests {

    public abstract static class BaseState {
        public static final Lock N_LOCK = new ReentrantReadWriteLock(false).writeLock();
        public static final Lock F_LOCK = new ReentrantReadWriteLock(true).writeLock();
        public final Lock lock;
        public int value;

        public BaseState(Lock lock) {
            this.lock = lock;
        }

        public int L() {
            Lock lock = this.lock;
            lock.lock();
            try {
                int r = (value == 0) ? 1 : 0;
                value = 1;
                return r;
            } finally {
                lock.unlock();
            }
        }

        public int LI() {
            Lock lock = this.lock;
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                return -1;
            }
            try {
                int r = (value == 0) ? 1 : 0;
                value = 1;
                return r;
            } finally {
                lock.unlock();
            }
        }

        public int TL() {
            Lock lock = this.lock;
            if (lock.tryLock()) {
                try {
                    int r = (value == 0) ? 1 : 0;
                    value = 1;
                    return r;
                } finally {
                    lock.unlock();
                }
            }
            return -1;
        }

        public int TLt() {
            Lock lock = this.lock;
            try {
                if (lock.tryLock(1, TimeUnit.MINUTES)) {
                    try {
                        int r = (value == 0) ? 1 : 0;
                        value = 1;
                        return r;
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                return -2;
            }
            return -1;
        }


    }

    @State
    public static class I_N_State extends BaseState {
        public I_N_State() {
            super(new ReentrantReadWriteLock(false).writeLock());
        }
    }

    @State
    public static class I_F_State extends BaseState {
        public I_F_State() {
            super(new ReentrantReadWriteLock(true).writeLock());
        }
    }

    @State
    public static class S_N_State extends BaseState {
        public S_N_State() {
            super(BaseState.N_LOCK);
        }
    }

    @State
    public static class S_F_State extends BaseState {
        public S_F_State() {
            super(BaseState.F_LOCK);
        }
    }

    public static class I_F  {

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_LI {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.LI(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_L {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TL {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TLt {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class L_L {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TL {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TLt {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TL {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TLt {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TLt_TLt {
            @Actor public void actor1(I_F_State s, IntResult2 r) { r.r1 = s.TLt(); }
            @Actor public void actor2(I_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }
    }

    public static class I_N {

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_LI {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.LI(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_L {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TL {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TLt {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class L_L {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TL {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TLt {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TL {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TLt {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TLt_TLt {
            @Actor public void actor1(I_N_State s, IntResult2 r) { r.r1 = s.TLt(); }
            @Actor public void actor2(I_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }
    }

    public abstract static class S_N {

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_LI {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.LI(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_L {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TL {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TLt {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class L_L {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TL {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TLt {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TL {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TLt {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TLt_TLt {
            @Actor public void actor1(S_N_State s, IntResult2 r) { r.r1 = s.TLt(); }
            @Actor public void actor2(S_N_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }
    }

    public abstract static class S_F {

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_LI {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.LI(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class LI_L {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TL {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class LI_TLt {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.LI(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(BothSucceed.class)
        public static class L_L {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.L(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TL {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(OneSucceeds.class)
        public static class L_TLt {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.L(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TL {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TL(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TL_TLt {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.TL(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }

        @JCStressTest
        @JCStressMeta(NoOneSucceeds.class)
        public static class TLt_TLt {
            @Actor public void actor1(S_F_State s, IntResult2 r) { r.r1 = s.TLt(); }
            @Actor public void actor2(S_F_State s, IntResult2 r) { r.r2 = s.TLt(); }
        }
    }

}
