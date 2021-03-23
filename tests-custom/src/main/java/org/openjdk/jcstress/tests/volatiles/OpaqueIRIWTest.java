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
package org.openjdk.jcstress.tests.volatiles;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IIII_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@JCStressTest
@Description("Tests the IRIW sequential consistency: machine ordering case.")
@Outcome(id = "0, 1, 0, 1", expect = Expect.ACCEPTABLE, desc = "This is a rare event, because it requires precise juxtaposition of threads to observe.")
@Outcome(id = "1, 0, 1, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Threads see the updates in the inconsistent order")
@Outcome(                   expect = Expect.ACCEPTABLE, desc = "All other cases are acceptable.")
@Ref("http://cs.oswego.edu/pipermail/concurrency-interest/2013-January/010608.html")
@State
public class OpaqueIRIWTest {

    static final VarHandle VH_X, VH_Y;

    static {
        try {
            VH_X = MethodHandles.lookup().findVarHandle(OpaqueIRIWTest.class, "x", int.class);
            VH_Y = MethodHandles.lookup().findVarHandle(OpaqueIRIWTest.class, "y", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public int x;
    public int y;

    @Actor
    public void actor1() {
        VH_X.setOpaque(this, 1);
    }

    @Actor
    public void actor2() {
        VH_Y.setOpaque(this, 1);
    }

    @Actor
    public void actor3(IIII_Result r) {
        r.r1 = (int) VH_X.getOpaque(this);
        r.r2 = (int) VH_Y.getOpaque(this);
    }

    @Actor
    public void actor4(IIII_Result r) {
        r.r3 = (int) VH_Y.getOpaque(this);
        r.r4 = (int) VH_X.getOpaque(this);
    }

}
