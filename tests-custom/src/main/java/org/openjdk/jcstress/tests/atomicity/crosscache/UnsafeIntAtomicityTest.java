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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.BBBB_Result;

import java.util.Random;

import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

@JCStressTest
@Description("Tests if Unsafe breaks the atomicity while doing cross cache-line reads/writes.")
@Outcome(id = "0, 0, 0, 0",     expect = Expect.ACCEPTABLE, desc = "Seeing the default value, this is a legal race.")
@Outcome(id = "-1, -1, -1, -1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value, this is a legal behavior.")
@State
public class UnsafeIntAtomicityTest {

    /**
     * We don't have the alignment information, so we would try to read/write to the
     * random offset within the byte array.
     */

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    public static final int SIZE = 256;

    public static final Random RANDOM = new Random();
    public static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    public static final long ARRAY_BASE_SCALE = UNSAFE.arrayIndexScale(byte[].class);
    public static final int COMPONENT_SIZE = 4;

    /** Alignment constraint: 4-bytes is default, for integers */
    public static final int ALIGN = Integer.getInteger("align", COMPONENT_SIZE);

    public final byte[] bytes;
    public final long offset;

    public UnsafeIntAtomicityTest() {
        bytes = new byte[SIZE];
        int index = RANDOM.nextInt((SIZE - COMPONENT_SIZE)/ALIGN)*ALIGN;
        offset = ARRAY_BASE_OFFSET + ARRAY_BASE_SCALE*index;
    }

    @Actor
    public void actor1() {
        UNSAFE.putInt(bytes, offset, 0xFFFFFFFF);
    }

    @Actor
    public void actor2(BBBB_Result r) {
        int t = UNSAFE.getInt(bytes, offset);
        r.r1 = (byte) ((t >> 0) & 0xFF);
        r.r2 = (byte) ((t >> 8) & 0xFF);
        r.r3 = (byte) ((t >> 16) & 0xFF);
        r.r4 = (byte) ((t >> 24) & 0xFF);
    }

}
