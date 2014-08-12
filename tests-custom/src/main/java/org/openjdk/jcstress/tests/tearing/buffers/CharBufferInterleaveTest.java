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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult3;

import java.nio.CharBuffer;

@JCStressTest
@Description("Tests the word-tearing guarantees for CharBuffer.")
@Outcome(id = "[0, 128, 128]", expect = Expect.ACCEPTABLE, desc = "Seeing all updates intact.")
@State
public class CharBufferInterleaveTest {

    /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
    public static final int SIZE = 256;

    private final CharBuffer buffer = CharBuffer.allocate(SIZE);

    @Actor
    public void actor1() {
        for (int i = 0; i < SIZE; i += 2) {
            buffer.put(i, 'a');
        }
    }

    @Actor
    public void actor2() {
        for (int i = 1; i < SIZE; i += 2) {
            buffer.put(i, 'b');
        }
    }

    @Arbiter
    public void arbiter1(IntResult3 r) {
        r.r1 = r.r2 = r.r3 = 0;
        for (int i = 0; i < SIZE; i++) {
            switch (buffer.get(i)) {
                case 'a': r.r2++; break;
                case 'b': r.r3++; break;
                default: r.r1++; break;
            }
        }
    }

}
