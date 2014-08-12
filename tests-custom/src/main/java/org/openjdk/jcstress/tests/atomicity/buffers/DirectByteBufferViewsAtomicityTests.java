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
package org.openjdk.jcstress.tests.atomicity.buffers;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LongResult1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class DirectByteBufferViewsAtomicityTests {

    @State
    public static class MyState {
        private final ByteBuffer b;
        private final IntBuffer ib;
        private final CharBuffer cb;
        private final DoubleBuffer db;
        private final FloatBuffer fb;
        private final LongBuffer lb;
        private final ShortBuffer sb;

        public MyState() {
            b = ByteBuffer.allocate(16);
            b.order(ByteOrder.nativeOrder());
            ib = b.asIntBuffer();
            cb = b.asCharBuffer();
            db = b.asDoubleBuffer();
            fb = b.asFloatBuffer();
            lb = b.asLongBuffer();
            sb = b.asShortBuffer();
        }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class IntViewTest {
        @Actor public void actor1(MyState s)                { s.ib.put(0, -1);                                  }
        @Actor public void actor2(MyState s, LongResult1 r) { r.r1 = s.ib.get(0);                               }
    }

    @JCStressTest
    @JCStressMeta(GradeChar.class)
    public static class CharViewTest {
        @Actor public void actor1(MyState s)                { s.cb.put(0, 'a');                                 }
        @Actor public void actor2(MyState s, LongResult1 r) { r.r1 = s.cb.get(0);                               }
    }

    @JCStressTest
    @JCStressMeta(GradeDouble.class)
    public static class DoubleViewTest {
        @Actor public void actor1(MyState s)                { s.db.put(0, -1D);                                 }
        @Actor public void actor2(MyState s, LongResult1 r) { r.r1 = Double.doubleToRawLongBits(s.db.get(0));   }
    }

    @JCStressTest
    @JCStressMeta(GradeFloat.class)
    public static class FloatViewTest {
        @Actor public void actor1(MyState s)                { s.fb.put(0, -1F);                                 }
        @Actor public void actor2(MyState s, LongResult1 r) { r.r1 = Float.floatToRawIntBits(s.fb.get(0));      }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class LongViewTest {
        @Actor public void actor1(MyState s)                { s.lb.put(0, -1);                                  }
        @Actor public void actor2(MyState s, LongResult1 r) { r.r1 = s.lb.get(0);                               }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class ShortViewTest {
        @Actor public void actor1(MyState s)                { s.sb.put(0, (short) -1);                          }
        @Actor public void actor2(MyState s, LongResult1 r) { r.r1 = s.sb.get(0);                               }
    }

}
