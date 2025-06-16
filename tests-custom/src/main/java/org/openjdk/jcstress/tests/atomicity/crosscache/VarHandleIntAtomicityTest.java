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
package org.openjdk.jcstress.tests.atomicity.crosscache;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.BBBB_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

@JCStressTest
@Description("Tests if VarHandle breaks the atomicity while doing cross cache-line reads/writes.")
@Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value, this is a legal race.")
@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value, this is a legal behavior.")
@State
public class VarHandleIntAtomicityTest {

    private static final VarHandle VH = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    public static final int SIZE = 256;

    public final byte[] bytes;
    public final int offset;

    public VarHandleIntAtomicityTest() {
        bytes = new byte[SIZE];
        offset = ThreadLocalRandom.current().nextInt(SIZE - Integer.BYTES) / Integer.BYTES * Integer.BYTES;
    }

    @Actor
    public void actor1() {
        VH.set(bytes, offset, 0xFFFFFFFF);
    }

    @Actor
    public void actor2(I_Result r) {
        r.r1 = (int)VH.get(bytes, offset);
    }

}
