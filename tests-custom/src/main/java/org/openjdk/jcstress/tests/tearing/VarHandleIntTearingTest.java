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
package org.openjdk.jcstress.tests.tearing;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

@Description("Tests the word-tearing guarantees for byte[] via VarHandle.")
@Outcome(id = "-1431655766, 1431655765", expect = Expect.ACCEPTABLE, desc = "Seeing all updates intact.")
public class VarHandleIntTearingTest {

    private static final VarHandle VH = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    private static final int SIZE = 256;

    int offset1;
    int offset2;

    byte[] bytes;

    public VarHandleIntTearingTest() {
        bytes = new byte[SIZE];
        offset1 = ThreadLocalRandom.current().nextInt(SIZE - Integer.BYTES*2);
        offset1 = offset1 / Integer.BYTES * Integer.BYTES;
        offset2 = offset1 + Integer.BYTES;
    }

    public void summary(II_Result r) {
        r.r1 = (int)VH.get(bytes, offset1);
        r.r2 = (int)VH.get(bytes, offset2);
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Plain_Plain extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.set(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.set(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Plain_Opaque extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.set(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setOpaque(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Plain_Release extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.set(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setRelease(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Plain_Volatile extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.set(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setVolatile(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Opaque_Opaque extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.setOpaque(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setOpaque(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Opaque_Release extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.setOpaque(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setRelease(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Opaque_Volatile extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.setOpaque(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setVolatile(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Release_Release extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.setRelease(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setRelease(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Release_Volatile extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.setRelease(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setVolatile(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleIntTearingTest.class)
    @State
    public static class Volatile_Volatile extends VarHandleIntTearingTest {
        @Actor public void actor1() { VH.setVolatile(bytes, offset1, 0xAAAAAAAA); }
        @Actor public void actor2() { VH.setVolatile(bytes, offset2, 0x55555555); }
        @Arbiter public void arbiter1(II_Result r) { summary(r); }
    }

}
