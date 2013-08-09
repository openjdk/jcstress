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
package org.openjdk.jcstress.tests.scratch;

import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.tests.Actor3_Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

public class StampedLockConcReaderTest implements Actor3_Test<StampedLockConcReaderTest.State, IntResult2> {

    @Override
    public void actor1(State s, IntResult2 intResult2) {
        long stamp = s.lock.writeLock();
        s.lock.unlockWrite(stamp);
    }

    @Override
    public void actor2(State s, IntResult2 r) {
        if (!s.lock.isWriteLocked()) {
            r.r1 = -1;
            return;
        }
        long stamp = s.lock.readLock();
        try {
            int v = s.ai.get();
            r.r1 = s.ai.compareAndSet(v, v + 1) ? 1 : 0;
        } finally {
            s.lock.unlockRead(stamp);
        }
    }

    @Override
    public void actor3(State s, IntResult2 r) {
        if (!s.lock.isWriteLocked()) {
            r.r2 = -1;
            return;
        }

        long stamp = s.lock.readLock();
        try {
            int v = s.ai.get();
            r.r2 = s.ai.compareAndSet(v, v + 1) ? 1 : 0;
        } finally {
            s.lock.unlockRead(stamp);
        }
    }

    @Override
    public State newState() {
        return new State();
    }

    @Override
    public IntResult2 newResult() {
        return new IntResult2();
    }

    public static class State {
        public final StampedLock lock = new StampedLock();
        public final AtomicInteger ai = new AtomicInteger();
    }

}
