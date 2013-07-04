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
package org.openjdk.jcstress.tests.tearing.buffers;

import org.openjdk.jcstress.infra.results.IntResult3;
import org.openjdk.jcstress.tests.Actor2_Arbiter1_Test;

import java.nio.IntBuffer;

public class IntBufferInterleaveTest implements Actor2_Arbiter1_Test<IntBuffer, IntResult3> {

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    public static final int SIZE = 256;

    @Override
    public IntBuffer newState() {
        return IntBuffer.allocate(SIZE);
    }

    @Override
    public void actor1(IntBuffer s, IntResult3 r) {
        for (int i = 0; i < SIZE; i += 2) {
            s.put(i, 1);
        }
    }

    @Override
    public void actor2(IntBuffer s, IntResult3 r) {
        for (int i = 1; i < SIZE; i += 2) {
            s.put(i, 2);
        }
    }

    @Override
    public void arbiter1(IntBuffer state, IntResult3 r) {
        r.r1 = r.r2 = r.r3 = 0;
        for (int i = 0; i < SIZE; i++) {
            int s = state.get(i);
            switch (s) {
                case 0:
                    r.r1++;
                    break;
                case 1:
                    r.r2++;
                    break;
                case 2:
                    r.r3++;
                    break;
                default:
                    throw new IllegalStateException(String.valueOf(s));
            }
        }
    }

    @Override
    public IntResult3 newResult() {
        return new IntResult3();
    }

}
