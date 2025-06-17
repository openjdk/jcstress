/*
 * Copyright (c) 2014, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.tests.varhandles;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@JCStressTest
@Description("Tests if Unsafe.putOrderedInt is in-order")
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "T2 observes TOP early")
@State
public class SetReleaseTwice {

    static final VarHandle VH_LOCK, VH_TOP;

    static {
        try {
            VH_LOCK = MethodHandles.lookup().findVarHandle(SetReleaseTwice.class, "lock", int.class);
            VH_TOP  = MethodHandles.lookup().findVarHandle(SetReleaseTwice.class, "top", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    volatile int lock;
    int top;

    @Actor
    public void actor1() {
        VH_TOP.setRelease(this, 1);
        VH_LOCK.setRelease(this, 1);
    }

    @Actor
    public void actor2(II_Result r) {
        r.r1 = (int)VH_LOCK.getAcquire(this);
        r.r2 = (int)VH_TOP.get(this);
    }

}
