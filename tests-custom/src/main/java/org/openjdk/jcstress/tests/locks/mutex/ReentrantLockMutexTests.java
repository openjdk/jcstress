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

import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.tests.Actor2_Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockMutexTests {

    public static class BaseState {
        public static final Lock N_LOCK = new ReentrantLock(false);
        public static final Lock F_LOCK = new ReentrantLock(true);
        public final Lock lock;

        public BaseState(Lock lock) {
            this.lock = lock;
        }

        public int value;
    }

    public static class I_N_State extends BaseState {
        public I_N_State() {
            super(new ReentrantLock(false));
        }
    }

    public static class I_F_State extends BaseState {
        public I_F_State() {
            super(new ReentrantLock(true));
        }
    }

    public static class S_N_State extends BaseState {
        public S_N_State() {
            super(BaseState.N_LOCK);
        }
    }

    public static class S_F_State extends BaseState {
        public S_F_State() {
            super(BaseState.F_LOCK);
        }
    }

    public static abstract class Base implements Actor2_Test<BaseState, IntResult2> {
        @Override public IntResult2 newResult() { return new IntResult2(); }

        public int L(BaseState s) {
            Lock lock = s.lock;
            lock.lock();
            try {
                int r = (s.value == 0) ? 1 : 0;
                s.value = 1;
                return r;
            } finally {
                lock.unlock();
            }
        }

        public int LI(BaseState s) {
            Lock lock = s.lock;
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                return -1;
            }
            try {
                int r = (s.value == 0) ? 1 : 0;
                s.value = 1;
                return r;
            } finally {
                lock.unlock();
            }
        }

        public int TL(BaseState s) {
            Lock lock = s.lock;
            if (lock.tryLock()) {
                try {
                    int r = (s.value == 0) ? 1 : 0;
                    s.value = 1;
                    return r;
                } finally {
                    lock.unlock();
                }
            }
            return -1;
        }

        public int TLt(BaseState s) {
            Lock lock = s.lock;
            try {
                if (lock.tryLock(1, TimeUnit.MINUTES)) {
                    try {
                        int r = (s.value == 0) ? 1 : 0;
                        s.value = 1;
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

    public abstract static class I_F extends Base {
        @Override public BaseState newState() { return new I_F_State(); }

        public abstract static class A extends I_F {}

        public static class LI_LI extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class LI_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class L_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = L(s); }
        }

        public static class L_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class L_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TL_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class TL_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TLt_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TLt(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }
    }

    public abstract static class I_N extends Base {
        @Override public BaseState newState() { return new I_N_State(); }

        public abstract static class A extends I_N {}

        public static class LI_LI extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class LI_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class L_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = L(s); }
        }

        public static class L_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class L_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TL_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class TL_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TLt_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TLt(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }
    }

    public abstract static class S_N extends Base {
        @Override public BaseState newState() { return new S_N_State(); }

        public abstract static class A extends S_N {}

        public static class LI_LI extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class LI_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class L_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = L(s); }
        }

        public static class L_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class L_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TL_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class TL_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TLt_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TLt(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }
    }

    public abstract static class S_F extends Base {
        @Override public BaseState newState() { return new S_F_State(); }

        public abstract static class A extends S_F {}

        public static class LI_LI extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = LI(s); }
        }

        public static class LI_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class LI_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = LI(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class L_L extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = L(s); }
        }

        public static class L_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class L_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = L(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TL_TL extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TL(s); }
        }

        public static class TL_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TL(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }

        public static class TLt_TLt extends A {
            @Override public void actor1(BaseState s, IntResult2 r) { r.r1 = TLt(s); }
            @Override public void actor2(BaseState s, IntResult2 r) { r.r2 = TLt(s); }
        }
    }

}
