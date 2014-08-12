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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.util.UnsafeHolder;

import java.util.Random;

@JCStressTest
@Description("Tests the word-tearing guarantees for byte[] via Unsafe.")
@Outcome(id = "[-1431655766, 1431655765]", expect = Expect.ACCEPTABLE, desc = "Seeing all updates intact.")
@State
public class UnsafeIntTearingTest {

    /**
     * We don't have the alignment information, so we would try to read/write to the
     * random offset within the byte array.
     */

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    public static final int SIZE = 256;

    public static final Random RANDOM = new Random();
    public static final int ARRAY_BASE_OFFSET = UnsafeHolder.U.arrayBaseOffset(byte[].class);
    public static final int ARRAY_BASE_SCALE = UnsafeHolder.U.arrayIndexScale(byte[].class);
    public static final int COMPONENT_SIZE = 4;

    /** Alignment constraint: 4-bytes is default, for integers */
    public static final int ALIGN = Integer.getInteger("align", COMPONENT_SIZE);

    public final byte[] bytes;
    public final long offset1;
    public final long offset2;

    public UnsafeIntTearingTest() {
        bytes = new byte[SIZE];
        int index = RANDOM.nextInt((SIZE - COMPONENT_SIZE*2)/ALIGN)*ALIGN;
        offset1 = ARRAY_BASE_OFFSET + ARRAY_BASE_SCALE*index;
        offset2 = offset1 + COMPONENT_SIZE;
    }

    @Actor
    public void actor1() {
        UnsafeHolder.U.putInt(bytes, offset1, 0xAAAAAAAA);
    }

    @Actor
    public void actor2() {
        UnsafeHolder.U.putInt(bytes, offset2, 0x55555555);
    }

    @Arbiter
    public void arbiter1(IntResult2 r) {
        r.r1 = UnsafeHolder.U.getInt(bytes, offset1);
        r.r2 = UnsafeHolder.U.getInt(bytes, offset2);
    }

}
