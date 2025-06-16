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
import org.openjdk.jcstress.infra.results.III_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@Description("Tests the word-tearing guarantees for byte[] via VarHandles.")
@Outcome(id = "0, 128, 128", expect = Expect.ACCEPTABLE, desc = "Seeing all updates intact.")
@State
public class VarHandleArrayInterleaveTest {

    private static final VarHandle VH = MethodHandles.arrayElementVarHandle(byte[].class);

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    public static final int SIZE = 256;

    byte[] ss = new byte[SIZE];

    public void summary(III_Result r) {
        for (byte s : ss) {
            switch (s) {
                case 0:
                    r.r1++;
                    break;
                case 1:
                    r.r2++;
                    break;
                case 2:
                    r.r3++;
                    break;
                default:
                    throw new IllegalStateException(String.valueOf(s));
            }
        }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Plain_Plain extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.set(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.set(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Plain_Opaque extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.set(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setOpaque(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Plain_Release extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.set(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setRelease(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Plain_Volatile extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.set(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setVolatile(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Opaque_Opaque extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.setOpaque(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setOpaque(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Opaque_Release extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.setOpaque(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setRelease(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Opaque_Volatile extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.setOpaque(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setVolatile(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Release_Release extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.setRelease(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setRelease(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Release_Volatile extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.setRelease(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setVolatile(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }

    @JCStressTest
    @JCStressMeta(VarHandleArrayInterleaveTest.class)
    @State
    public static class Volatile_Volatile extends VarHandleArrayInterleaveTest {
        @Actor public void actor1() { for (int i = 0; i < ss.length; i += 2) VH.setVolatile(ss, i, (byte) 1); }
        @Actor public void actor2() { for (int i = 1; i < ss.length; i += 2) VH.setVolatile(ss, i, (byte) 2); }
        @Arbiter public void arbiter(III_Result r) { summary(r); }
    }
}
