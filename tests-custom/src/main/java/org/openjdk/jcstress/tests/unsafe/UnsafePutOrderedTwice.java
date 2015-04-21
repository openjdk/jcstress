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
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.util.UnsafeHolder;
import sun.misc.Contended;

@JCStressTest
@Description("Tests if Unsafe.putOrderedInt is in-order")
@Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
@Outcome(id = "[0, 0]", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
@Outcome(id = "[0, 1]", expect = Expect.ACCEPTABLE, desc = "T2 observes TOP early")
@State
public class UnsafePutOrderedTwice {

    public static final long OFFSET_LOCK, OFFSET_TOP;

    static {
        try {
            OFFSET_LOCK = UnsafeHolder.U.objectFieldOffset(UnsafePutOrderedTwice.class.getDeclaredField("lock"));
            OFFSET_TOP = UnsafeHolder.U.objectFieldOffset(UnsafePutOrderedTwice.class.getDeclaredField("top"));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    @Contended
    volatile int lock;

    @Contended
    int top;

    @Actor
    public void actor1() {
        UnsafeHolder.U.putOrderedInt(this, OFFSET_TOP, 1);
        UnsafeHolder.U.putOrderedInt(this, OFFSET_LOCK, 1);
    }

    @Actor
    public void actor2(IntResult2 r) {
        r.r1 = lock;
        r.r2 = top;
    }

}
