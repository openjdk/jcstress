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
package org.openjdk.jcstress.tests.fences;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.util.UnsafeHolder;

/**
 * Tests if release fence before publication ensures non-default read
 *
 *  @author Doug Lea (dl@cs.oswego.edu)
 */
@JCStressTest
@Outcome(id = "[0, 0]", expect = Expect.ACCEPTABLE, desc = "Data not yet published")
@Outcome(id = "[0, 1]", expect = Expect.ACCEPTABLE, desc = "Data not yet published")
@Outcome(id = "[1, 0]", expect = Expect.FORBIDDEN,  desc = "Reads the default value for field $x after publication.")
@Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "Must read the written value for $x after publication.")
@State
public class FencedPublicationTest {

    Data data;

    static class Data {
        int x;
    }

    @Actor
    public void actor1() {
        Data d = new Data();
        d.x = 1;
        UnsafeHolder.U.storeFence();
        data = d;
    }

    @Actor
    public void actor2(IntResult2 r) {
        int sy, sx;
        Data d = data;
        if (d == null) {
            sy = 0;
            sx = 0;
        }
        else {
            sy = 1;
            sx = d.x;
        }
        r.r1 = sy;
        r.r2 = sx;
    }

}
