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
package org.openjdk.jcstress.tests.unsafe;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

/**
 * Test if volatile write-read induces happens-before if in between two non-volatile reads.
 *
 * @author Aleksey Shipilev (shade@redhat.com)
 */
@JCStressTest
@Outcome(id = "0, 0, 0", expect = Expect.ACCEPTABLE, desc = "Default value for the fields. Observers are allowed to see the default value for the field, because there is the data race between reader and writer.")
@Outcome(id = "0, 1, 0", expect = Expect.FORBIDDEN,  desc = "Volatile write to $y had happened, and update to $x had been lost.")
@Outcome(id = "1, 1, 0", expect = Expect.FORBIDDEN,  desc = "Volatile write to $y had happened, and update to $x had been lost.")
@Outcome(id = "1, 0, 0", expect = Expect.ACCEPTABLE, desc = "Write to $y is still in flight, can see inconsistent $x.")
@Outcome(id = "0, 0, 1", expect = Expect.ACCEPTABLE, desc = "Write to $y is still in flight, $x is arriving late.")
@Outcome(id = "1, 0, 1", expect = Expect.ACCEPTABLE, desc = "Write to $y is still in flight, $x has arrived.")
@Outcome(id = "0, 1, 1", expect = Expect.ACCEPTABLE, desc = "The writes appear the the writers' order.")
@Outcome(id = "1, 1, 1", expect = Expect.ACCEPTABLE, desc = "Both updates are visible.")
@Ref("https://bugs.openjdk.java.net/browse/JDK-8175887")
@State
public class UnsafeReadTwiceOverVolatileReadTest {

    public static final long OFFSET;

    static {
        try {
            OFFSET = UNSAFE.objectFieldOffset(UnsafeReadTwiceOverVolatileReadTest.class.getDeclaredField("y"));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    int x;
    volatile int y;

    @Actor
    public void actor1() {
        x = 1;
        UNSAFE.putIntVolatile(this, OFFSET, 1);
    }

    @Actor
    public void actor2(III_Result r) {
        r.r1 = x;
        r.r2 = UNSAFE.getIntVolatile(this, OFFSET);
        r.r3 = x;
    }

}
