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

import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.tests.Actor2_Test;

/**
 * Tests if acquire-release use of volatiles induce proper happens-before.
 *
 *  @author Doug Lea (dl@cs.oswego.edu)
 */
public class VolatileAcquireReleaseTest implements Actor2_Test<VolatileAcquireReleaseTest.State, IntResult2> {

    public static class State {
        int x;
        volatile int y; // acq/rel var
    }

    @Override
    public void actor1(State s, IntResult2 r) {
        s.x = 1;
        s.x = 2;
        s.y = 1;
        s.x = 3;
    }

    @Override
    public void actor2(State s, IntResult2 r) {
        r.r1 = s.y;
        r.r2 = s.x;
    }

    @Override
    public State newState() {
        return new State();
    }

    @Override
    public IntResult2 newResult() {
        return new IntResult2();
    }

}
