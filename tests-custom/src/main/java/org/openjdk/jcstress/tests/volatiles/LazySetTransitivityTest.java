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


import org.openjdk.jcstress.infra.results.IntResult3;
import org.openjdk.jcstress.tests.Actor3_Test;

import java.util.concurrent.atomic.AtomicInteger;

public class LazySetTransitivityTest implements Actor3_Test<LazySetTransitivityTest.State, IntResult3> {

    @Override
    public State newState() {
        return new State();
    }

    @Override
    public void actor1(State s, IntResult3 r) {
        s.a.lazySet(1);
    }

    @Override
    public void actor2(State s, IntResult3 r) {
        int aValue = s.a.get();
        s.b.set(aValue);
        r.r1 = aValue;
    }

    @Override
    public void actor3(State s, IntResult3 r) {
        r.r2 = s.b.get();
        r.r3 = s.a.get();
    }

    @Override
    public IntResult3 newResult() {
        return new IntResult3();
    }

    // TODO: Should have used volatiles, but lazySet is conveniently exposed by Atomics.
    public static class State {
        public final AtomicInteger a = new AtomicInteger();
        public final AtomicInteger b = new AtomicInteger();
    }
}
