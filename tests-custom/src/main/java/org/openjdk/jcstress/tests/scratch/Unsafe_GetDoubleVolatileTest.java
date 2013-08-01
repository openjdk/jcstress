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
package org.openjdk.jcstress.tests.scratch;

import org.openjdk.jcstress.infra.results.DoubleResult1;
import org.openjdk.jcstress.tests.Actor2_Test;
import org.openjdk.jcstress.util.UnsafeHolder;

public class Unsafe_GetDoubleVolatileTest implements Actor2_Test<Unsafe_GetDoubleVolatileTest.State, DoubleResult1> {

    @Override
    public void actor1(State s, DoubleResult1 r) {
        UnsafeHolder.U.putDoubleVolatile(s, State.OFFSET, -1D);
    }

    @Override
    public void actor2(State s, DoubleResult1 r) {
        r.r1 = UnsafeHolder.U.getDoubleVolatile(s, State.OFFSET);
    }

    @Override
    public State newState() {
        return new State();
    }

    @Override
    public DoubleResult1 newResult() {
        return new DoubleResult1();
    }

    public static class State {
        private static long OFFSET;

        static {
            try {
                OFFSET = UnsafeHolder.U.objectFieldOffset(State.class.getDeclaredField("x"));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        public volatile double x;
    }

}
