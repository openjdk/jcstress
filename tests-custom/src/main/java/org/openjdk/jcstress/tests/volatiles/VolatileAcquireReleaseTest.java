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
import org.openjdk.jcstress.infra.results.IntResult2;

/**
 * Tests if acquire-release use of volatiles induce proper happens-before.
 *
 *  @author Doug Lea (dl@cs.oswego.edu)
 */
@JCStressTest
@Outcome(id = "[0, 0]", expect = Expect.ACCEPTABLE, desc = "Before observing releasing write to, any value is OK for $x.")
@Outcome(id = "[0, 1]", expect = Expect.ACCEPTABLE, desc = "Before observing releasing write to, any value is OK for $x.")
@Outcome(id = "[0, 2]", expect = Expect.ACCEPTABLE, desc = "Before observing releasing write to, any value is OK for $x.")
@Outcome(id = "[0, 3]", expect = Expect.ACCEPTABLE, desc = "Before observing releasing write to, any value is OK for $x.")
@Outcome(id = "[1, 0]", expect = Expect.FORBIDDEN,  desc = "Can't read the default or old value for $x after $y is observed.")
@Outcome(id = "[1, 1]", expect = Expect.FORBIDDEN,  desc = "Can't read the default or old value for $x after $y is observed.")
@Outcome(id = "[1, 2]", expect = Expect.ACCEPTABLE, desc = "Can see a released value of $x if $y is observed.")
@Outcome(id = "[1, 3]", expect = Expect.ACCEPTABLE, desc = "Can see a released value of $x if $y is observed.")
@State
public class VolatileAcquireReleaseTest {

    int x;
    volatile int y; // acq/rel var

    @Actor
    public void actor1() {
        x = 1;
        x = 2;
        y = 1;
        x = 3;
    }

    @Actor
    public void actor2(IntResult2 r) {
        r.r1 = y;
        r.r2 = x;
    }

}
