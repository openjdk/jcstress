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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult3;

@JCStressTest
@Outcome(id = "[0, 0, 0]", expect = Expect.ACCEPTABLE, desc = "Legal: No updates had occured.")
@Outcome(id = "[1, 0, 0]", expect = Expect.FORBIDDEN,  desc = "Illegal to see the stale $data once $guard1 is visible")
@Outcome(id = "[1, 0, 1]", expect = Expect.FORBIDDEN,  desc = "Illegal to see the stale $data once $guard1 is visible")
@Outcome(id = "[0, 0, 1]", expect = Expect.ACCEPTABLE, desc = "Legal: Seeing the update for $guard2, but no other updates.")
@Outcome(id = "[0, 1, 0]", expect = Expect.ACCEPTABLE, desc = "Legal: Seeing the early update for $data, while no updates for guards are visible.")
@Outcome(id = "[0, 1, 1]", expect = Expect.ACCEPTABLE, desc = "Legal: Seeing the update for $data and $guard2.")
@Outcome(id = "[1, 1, 1]", expect = Expect.ACCEPTABLE, desc = "Legal: Seeing all updates.")
@State
public class DoubleVolatileTest {

    volatile int guard1 = 0;
    int data = 0;
    volatile int guard2 = 0;

    @Actor
    public void actor1() {
        guard2 = 1;
        data = 1;
        guard1 = 1;
    }

    @Actor
    public void actor2(IntResult3 r) {
        r.r1 = guard1;
        r.r2 = data;
        r.r3 = guard2;
    }

}
