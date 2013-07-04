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
package org.openjdk.jcstress.tests.atomics.integer;

import org.openjdk.jcstress.infra.results.IntResult1;
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.tests.Actor2_Arbiter1_Test;
import org.openjdk.jcstress.tests.Actor2_Test;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicIntegerFieldUpdaterPairwiseTests {

    public static class State {
        public volatile int v;
        public final AtomicIntegerFieldUpdater<State> u = AtomicIntegerFieldUpdater.newUpdater(State.class, "v");
    }

    public abstract static class AbstractTest implements Actor2_Test<State, IntResult2> {
        @Override public State newState() { return new State(); }
        @Override public IntResult2 newResult() { return new IntResult2(); }
    }

    // ------------------- first is addAndGet

    public static class AddAndGet_AddAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.addAndGet(s, 5); }
    }

    public static class AddAndGet_DecAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.decrementAndGet(s); }
    }

    public static class AddAndGet_GetAndAdd extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndAdd(s, 5); }
    }

    public static class AddAndGet_GetAndDec extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    public static class AddAndGet_GetAndInc extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    public static class AddAndGet_GetAndSet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    public static class AddAndGet_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class AddAndGet_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class AddAndGet_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class AddAndGet_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is decAndGet

    public static class DecAndGet_DecAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.decrementAndGet(s); }
    }

    public static class DecAndGet_GetAndAdd extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.decrementAndGet(s); }
    }

    public static class DecAndGet_GetAndDec extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    public static class DecAndGet_GetAndInc extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    public static class DecAndGet_GetAndSet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    public static class DecAndGet_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class DecAndGet_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class DecAndGet_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class DecAndGet_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndAdd

    public static class GetAndAdd_GetAndAdd extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndAdd(s, 5); }
    }

    public static class GetAndAdd_GetAndDec extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    public static class GetAndAdd_GetAndInc extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    public static class GetAndAdd_GetAndSet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    public static class GetAndAdd_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class GetAndAdd_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndAdd_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndAdd_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndDec

    public static class GetAndDec_GetAndDec extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    public static class GetAndDec_GetAndInc extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    public static class GetAndDec_GetAndSet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    public static class GetAndDec_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class GetAndDec_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndDec_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndDec_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndInc

    public static class GetAndInc_GetAndInc extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    public static class GetAndInc_GetAndSet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    public static class GetAndInc_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class GetAndInc_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndInc_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndInc_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndSet

    public static class GetAndSet_GetAndSet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    public static class GetAndSet_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class GetAndSet_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndSet_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class GetAndSet_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is incAndGet

    public static class IncAndGet_IncAndGet extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    public static class IncAndGet_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class IncAndGet_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class IncAndGet_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is CAS

    public static class CAS_CAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.compareAndSet(s, 0, 5) ? 5 : 1; }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class CAS_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.compareAndSet(s, 0, 5) ? 5 : 1; }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class CAS_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.compareAndSet(s, 0, 5) ? 5 : 1; }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is WCAS

    public static class WCAS_WCAS extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.weakCompareAndSet(s, 0, 5) ? 5 : 1; }
        @Override public void actor2(State s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    public static class WCAS_Set extends AbstractTest {
        @Override public void actor1(State s, IntResult2 r) { r.r1 = s.u.weakCompareAndSet(s, 0, 5) ? 5 : 1; }
        @Override public void actor2(State s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is set

    public static class Set_Set implements Actor2_Arbiter1_Test<State, IntResult1> {
        @Override public void actor1(State s, IntResult1 r) { s.u.set(s, 5); }
        @Override public void actor2(State s, IntResult1 r) { s.u.set(s, 10); }
        @Override public void arbiter1(State s, IntResult1 r) { r.r1 = s.u.get(s); }

        @Override public State newState() { return new State();  }
        @Override public IntResult1 newResult() { return new IntResult1(); }
    }

}
