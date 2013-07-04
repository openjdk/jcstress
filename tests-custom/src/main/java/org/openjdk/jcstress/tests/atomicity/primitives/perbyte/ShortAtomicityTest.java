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
package org.openjdk.jcstress.tests.atomicity.primitives.perbyte;

import org.openjdk.jcstress.infra.results.ByteResult2;
import org.openjdk.jcstress.tests.Actor2_Test;
import org.openjdk.jcstress.tests.atomicity.primitives.Constants;

/**
 * Tests if primitive shorts experience non-atomic reads/writes.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ShortAtomicityTest implements Actor2_Test<ShortAtomicityTest.State, ByteResult2> {

    public static class State {
        short x;
    }

    @Override
    public State newState() {
        return new State();
    }

    @Override
    public void actor1(State s, ByteResult2 r) {
        s.x = Constants.SHORT_SAMPLE;
    }

    @Override
    public void actor2(State s, ByteResult2 r) {
        short t = s.x;
        r.r1 = (byte) ((t >> 0) & 0xFF);
        r.r2 = (byte) ((t >> 8) & 0xFF);
    }

    @Override
    public ByteResult2 newResult() {
        return new ByteResult2();
    }

}
