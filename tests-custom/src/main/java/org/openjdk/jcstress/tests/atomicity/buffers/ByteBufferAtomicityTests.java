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

public class ByteBufferAtomicityTests {

    @State
    public static class MyState {
        private final ByteBuffer b;

        public MyState() {
            b = ByteBuffer.allocate(16);
            b.order(ByteOrder.nativeOrder());
        }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class IntTest {
        @Actor public void actor1(MyState s)                 { s.b.putInt(0, -1);                                    }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = s.b.getInt(0);                                 }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class ShortTest {
        @Actor public void actor1(MyState s, LongResult1 r)  { s.b.putShort(0, (short) -1);                          }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = s.b.getShort(0);                               }
    }

    @JCStressTest
    @JCStressMeta(GradeChar.class)
    public static class CharTest {
        @Actor public void actor1(MyState s, LongResult1 r)  { s.b.putChar(0, 'a');                                  }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = s.b.getChar(0);                                }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class LongTest {
        @Actor public void actor1(MyState s, LongResult1 r)  { s.b.putLong(0, -1L);                                  }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = s.b.getLong(0);                                }
    }

    @JCStressTest
    @JCStressMeta(GradeDouble.class)
    public static class DoubleTest {
        @Actor public void actor1(MyState s, LongResult1 r)  { s.b.putDouble(0, -1D);                                }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = Double.doubleToRawLongBits(s.b.getDouble(0));  }
    }

    @JCStressTest
    @JCStressMeta(GradeFloat.class)
    public static class FloatTest {
        @Actor public void actor1(MyState s, LongResult1 r)  { s.b.putFloat(0, -1F);                                 }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = Float.floatToRawIntBits(s.b.getFloat(0));      }
    }

    @JCStressTest
    @JCStressMeta(GradeInt.class)
    public static class ByteTest {
        @Actor public void actor1(MyState s, LongResult1 r)  { s.b.put(0, (byte) -1);                                }
        @Actor public void actor2(MyState s, LongResult1 r)  { r.r1 = s.b.get();                                     }
    }

}
