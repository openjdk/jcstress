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
import org.openjdk.jcstress.infra.results.JJ_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@JCStressTest
@Description("Tests if VarHandle.getAndAddLong is racy")
@Outcome(id = "1, 4398046511104", expect = Expect.ACCEPTABLE, desc = "T1 -> T2 execution")
@Outcome(id = "0, 0",             expect = Expect.ACCEPTABLE, desc = "T2 -> T1 execution")
@Outcome(id = "0, 4398046511104", expect = Expect.ACCEPTABLE, desc = "T2 reads the result early")
@State
public class AddLong42 {

    static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(AddLong42.class, "x", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    long x;
    volatile int written;

    @Actor
    public void actor1() {
        VH.getAndAdd(this, 1L << 42);
        written = 1;
    }

    @Actor
    public void actor2(JJ_Result r) {
        r.r1 = written;
        r.r2 = (long)VH.get(this);
    }

}
