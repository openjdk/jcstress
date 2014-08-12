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
import org.openjdk.jcstress.annotations.Ref;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult3;

import java.util.concurrent.atomic.AtomicInteger;

@JCStressTest
@Outcome(id = "[1, 0, 1]", expect = Expect.ACCEPTABLE, desc = "Legal: T3 reads B=0, and there are no guarantees about the value of A.")
@Outcome(id = "[1, 0, 0]", expect = Expect.ACCEPTABLE, desc = "Legal: T3 reads B=0, and there are no guarantees about the value of A.")
@Outcome(id = "[0, 0, 0]", expect = Expect.ACCEPTABLE, desc = "Legal: T3 reads B=0, and there are no guarantees about the value of A.")
@Outcome(id = "[0, 0, 1]", expect = Expect.ACCEPTABLE, desc = "Legal: T3 reads B=0, and there are no guarantees about the value of A.")
@Outcome(id = "[1, 1, 1]", expect = Expect.ACCEPTABLE, desc = "Legal: T3 observes (B=1, A=1). This looks like the evidence for transitivity.")
@Outcome(id = "[1, 1, 0]", expect = Expect.FORBIDDEN,  desc = "Illegal: T3 observes stale value for A.")
@Ref("http://cs.oswego.edu/pipermail/concurrency-interest/2013-January/010669.html")
@State
public class LazySetTransitivityTest {

    public final AtomicInteger a = new AtomicInteger();
    public final AtomicInteger b = new AtomicInteger();

    @Actor
    public void actor1() {
        a.lazySet(1);
    }

    @Actor
    public void actor2(IntResult3 r) {
        int aValue = a.get();
        b.set(aValue);
        r.r1 = aValue;
    }

    @Actor
    public void actor3(IntResult3 r) {
        r.r2 = b.get();
        r.r3 = a.get();
    }

}
