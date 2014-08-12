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
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Ref;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult4;

@JCStressTest
@Description("Tests the IRIW sequential consistency.")
@Outcome(id = "[0, 1, 1, 0]", expect = Expect.ACCEPTABLE, desc = "This is a rare event, because it requires precise juxtaposition of threads to observe.")
@Outcome(id = "[1, 0, 0, 1]", expect = Expect.ACCEPTABLE, desc = "Threads see the updates in the inconsistent order")
@Outcome(                     expect = Expect.ACCEPTABLE, desc =  "All other cases are acceptable.")
@Ref("http://cs.oswego.edu/pipermail/concurrency-interest/2013-January/010608.html")
@State
public class IRIWTest {

    public int x;
    public int y;

    @Actor
    public void actor1() {
        x = 1;
    }

    @Actor
    public void actor2() {
        y = 1;
    }

    @Actor
    public void actor3(IntResult4 r) {
        r.r1 = x;
        r.r2 = y;
    }

    @Actor
    public void actor4(IntResult4 r) {
        r.r4 = y;
        r.r3 = x;
    }

}
