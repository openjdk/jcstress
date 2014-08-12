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
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.util.UnsafeHolder;
import sun.misc.Contended;

/**
 * Tests if read-after-write using fences preserves SC
 *
 *  @author Doug Lea (dl@cs.oswego.edu)
 */
@JCStressTest
@Description("Tests the sequential consistency on Dekker-like construction using explicit fences, not volatiles")
@Outcome(id = {"[0, 1]", "[1, 0]", "[1, 1]"}, expect = Expect.ACCEPTABLE, desc = "Acceptable under sequential consistency")
@Outcome(id = {"[0, 0]"},                     expect = Expect.FORBIDDEN,  desc = "Un-acceptable under sequential consistency")
@State
public class FencedDekkerTest {

    @Contended
    int a;

    @Contended
    int b;

    @Actor
    public void actor1(IntResult2 r) {
        a = 1;
        UnsafeHolder.U.fullFence();
        r.r1 = b;
    }

    @Actor
    public void actor2(IntResult2 r) {
        b = 1;
        UnsafeHolder.U.fullFence();
        r.r2 = a;
    }

}
