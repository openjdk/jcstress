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
import org.openjdk.jcstress.annotations.ConcurrencyStressTest;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;
import org.openjdk.jcstress.infra.results.IntResult2;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class AtomicIntegerArrayPairwiseTests {

    @State
    public static class S {
        public final int SIZE = 256; // double the maximum cache line
        public final AtomicIntegerArray a = new AtomicIntegerArray(SIZE);
        public final int idx = ThreadLocalRandom.current().nextInt(SIZE);
    }

    // ------------------- first is addAndGet

    @ConcurrencyStressTest
    public static class AddAndGet_AddAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.addAndGet(s.idx, 5); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_DecAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.decrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_GetAndAdd {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndAdd(s.idx, 5); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndDecrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndIncrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndSet(s.idx, 10); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class AddAndGet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.addAndGet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is decAndGet

    @ConcurrencyStressTest
    public static class DecAndGet_DecAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.decrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_GetAndAdd {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndAdd(s.idx, 5); }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndDecrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndIncrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndSet(s.idx, 10); }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class DecAndGet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.decrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndAdd

    @ConcurrencyStressTest
    public static class GetAndAdd_GetAndAdd {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndAdd(s.idx, 5); }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndDecrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndIncrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndSet(s.idx, 10); }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndAdd_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndAdd(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndDec

    @ConcurrencyStressTest
    public static class GetAndDec_GetAndDec {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndDecrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndDec_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndIncrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndDec_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndSet(s.idx, 10); }
    }

    @ConcurrencyStressTest
    public static class GetAndDec_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndDec_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndDec_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndDec_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndDecrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndInc

    @ConcurrencyStressTest
    public static class GetAndInc_GetAndInc {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndIncrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndIncrement(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndInc_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndIncrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndSet(s.idx, 10); }
    }

    @ConcurrencyStressTest
    public static class GetAndInc_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndIncrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndInc_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndIncrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndInc_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndIncrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndInc_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndIncrement(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is getAndSet

    @ConcurrencyStressTest
    public static class GetAndSet_GetAndSet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndSet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.getAndSet(s.idx, 10); }
    }

    @ConcurrencyStressTest
    public static class GetAndSet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndSet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class GetAndSet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndSet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndSet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndSet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class GetAndSet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.getAndSet(s.idx, 5); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is incAndGet

    @ConcurrencyStressTest
    public static class IncAndGet_IncAndGet {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.incrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.incrementAndGet(s.idx); }
    }

    @ConcurrencyStressTest
    public static class IncAndGet_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.incrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class IncAndGet_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.incrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class IncAndGet_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.incrementAndGet(s.idx); }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is CAS

    @ConcurrencyStressTest
    public static class CAS_CAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.compareAndSet(s.idx, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.compareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class CAS_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.compareAndSet(s.idx, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class CAS_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.compareAndSet(s.idx, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is WCAS

    @ConcurrencyStressTest
    public static class WCAS_WCAS {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.weakCompareAndSet(s.idx, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { r.r2 = s.a.weakCompareAndSet(s.idx, 0, 20) ? 20 : 10; }
    }

    @ConcurrencyStressTest
    public static class WCAS_Set {
        @Actor public void actor1(S s, IntResult2 r) { r.r1 = s.a.weakCompareAndSet(s.idx, 0, 5) ? 5 : 1; }
        @Actor public void actor2(S s, IntResult2 r) { s.a.set(s.idx, 10); r.r2 = 0; }
    }

    // ------------------- first is set

    @ConcurrencyStressTest
    public static class Set_Set {
        @Actor public void actor1(S s, IntResult1 r) { s.a.set(s.idx, 5); }
        @Actor public void actor2(S s, IntResult1 r) { s.a.set(s.idx, 10); }
        @Arbiter public void arbiter1(S s, IntResult1 r) { r.r1 = s.a.get(s.idx); }
    }

}
