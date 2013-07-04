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
package org.openjdk.jcstress.tests.atomicity.crosscache;

import org.openjdk.jcstress.infra.results.ByteResult4;
import org.openjdk.jcstress.tests.Actor2_Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class ByteBufferIntAtomicityTest implements Actor2_Test<ByteBufferIntAtomicityTest.State, ByteResult4> {

    @Override
    public State newState() {
        return new State();
    }

    @Override
    public void actor1(State s, ByteResult4 r) {
        s.bytes.putInt(s.offset, 0xFFFFFFFF);
    }

    @Override
    public void actor2(State s, ByteResult4 r) {
        int t = s.bytes.getInt(s.offset);
        r.r1 = (byte) ((t >> 0) & 0xFF);
        r.r2 = (byte) ((t >> 8) & 0xFF);
        r.r3 = (byte) ((t >> 16) & 0xFF);
        r.r4 = (byte) ((t >> 24) & 0xFF);
    }

    @Override
    public ByteResult4 newResult() {
        return new ByteResult4();
    }

    /**
     * We don't have the alignment information, so we would try to read/write to the
     * random offset within the byte array.
     */
    public static class State {
        /** Array size: 256 bytes inevitably crosses the cache line on most implementations */
        public static final int SIZE = 256;

        public static final Random RANDOM = new Random();
        public static final int COMPONENT_SIZE = 4;

        /** Alignment constraint: 4-bytes is default, for integers */
        public static final int ALIGN = Integer.getInteger("align", COMPONENT_SIZE);

        public final ByteBuffer bytes;
        public final int offset;

        public State() {
            bytes = ByteBuffer.allocate(SIZE);
            bytes.order(ByteOrder.nativeOrder());
            offset = RANDOM.nextInt((SIZE - COMPONENT_SIZE)/ALIGN)*ALIGN;
        }
    }

}
