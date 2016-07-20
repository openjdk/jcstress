/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jcstress.infra.results.IntResult2;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Description("Tests Dekker-lock-style idioms")
@Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
@Outcome(id = "0, 0",                   expect = ACCEPTABLE_INTERESTING,  desc = "Apparently violates sequential consistency")
@State
public class DekkerRelaxation2Test {

    static final VarHandle VH_A, VH_B;

    static {
        try {
            VH_A = MethodHandles.lookup().findVarHandle(DekkerRelaxation2Test.class, "a", int.class);
            VH_B = MethodHandles.lookup().findVarHandle(DekkerRelaxation2Test.class, "b", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    volatile int a;
    volatile int b;

    @Actor
    public void actor1(IntResult2 r) {
        VH_A.setVolatile(this, 1);
        r.r1 = (int) VH_B.getAcquire(this);  // relax to acquire
    }

    @Actor
    public void actor2(IntResult2 r) {
        VH_B.setVolatile(this, 1);
        r.r2 = (int) VH_A.getVolatile(this);
    }
}
