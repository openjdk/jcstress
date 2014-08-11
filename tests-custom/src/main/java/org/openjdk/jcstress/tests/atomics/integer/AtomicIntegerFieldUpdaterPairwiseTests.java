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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;
import org.openjdk.jcstress.infra.results.IntResult2;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicIntegerFieldUpdaterPairwiseTests {

    @State
    public static class S {
        public volatile int v;
        public final AtomicIntegerFieldUpdater<S> u = AtomicIntegerFieldUpdater.newUpdater(S.class, "v");
    }

    @Description("Tests pairwise operations on AtomicIntegerArray")
    public static class G {

    }

    // ------------------- first is addAndGet

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 10]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 5]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_AddAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.addAndGet(s, 5); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 4]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[4, -1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_DecAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.decrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 5]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_GetAndAdd {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndAdd(s, 5); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 5]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[4, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 5]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[6, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 5]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[15, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 6]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[6, 1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[25, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[25, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[15, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class AddAndGet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.addAndGet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is decAndGet

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, -2]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[-2, -1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_DecAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.decrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[4, 0]",   expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_GetAndAdd {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndAdd(s, 5); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[-2, 0]",  expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[0, 0]",   expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[9, 0]",   expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, 0]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[0, 1]",  expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, 10]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[19, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, 10]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[19, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[-1, 0]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[9, 0]",  expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class DecAndGet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.decrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndAdd

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 5]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[5, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_GetAndAdd {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndAdd(s, 5); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 5]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[-1, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 5]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 5]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 6]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndAdd_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndAdd(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndDec

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[-1, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndDecrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 0]",  expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, -1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 0]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndDec_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndDecrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndInc

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndInc_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndIncrement(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndInc_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 2]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndInc_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndInc_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndInc_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndInc_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndIncrement(s); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndSet

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 5]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndSet_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.getAndSet(s, 10); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 6]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndSet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndSet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[20, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndSet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[0, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class GetAndSet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.getAndSet(s, 5); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is incAndGet

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[1, 2]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[2, 1]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class IncAndGet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.incrementAndGet(s); }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[1, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[21, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class IncAndGet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[1, 10]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[21, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class IncAndGet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[1, 0]",  expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[11, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class IncAndGet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.incrementAndGet(s); }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is CAS

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 10]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class CAS_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.compareAndSet(s, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.compareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 10]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class CAS_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.compareAndSet(s, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 0]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class CAS_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.compareAndSet(s, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is WCAS

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 10]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 20]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class WCAS_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.weakCompareAndSet(s, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.u.weakCompareAndSet(s, 0, 20) ? 20 : 10; }
    }

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5, 0]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[1, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class WCAS_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.u.weakCompareAndSet(s, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { s.u.set(s, 10); r.r2 = 0; }
    }

    // ------------------- first is set

    @JCStressTest
    @JCStressMeta(G.class)
    @Outcome(id = "[5]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
    @Outcome(id = "[10]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
    public static class Set_Set {
        @Actor public void actor1(S s, IntResult1 r) { s.u.set(s, 5); }
        @Actor public void actor2(S s, IntResult1 r) { s.u.set(s, 10); }
        @Arbiter public void arbiter1(S s, IntResult1 r) { r.r1 = s.u.get(s); }
    }

}
