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

import org.openjdk.jcstress.infra.results.LongResult1;
import org.openjdk.jcstress.tests.Actor2_Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteBufferAtomicityTests {

    public static ByteBuffer order(ByteBuffer b) { b.order(ByteOrder.nativeOrder()); return b; }

    public abstract static class ByteBufferTest implements Actor2_Test<ByteBuffer, LongResult1> {
        @Override public ByteBuffer newState()                { return order(ByteBuffer.allocate(16));              }
        @Override public LongResult1 newResult()              { return new LongResult1();                           }
    }

    public static class IntTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.putInt(0, -1);                                    }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = b.getInt(0);                                 }
    }

    public static class ShortTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.putShort(0, (short) -1);                          }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = b.getShort(0);                               }
    }

    public static class CharTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.putChar(0, 'a');                                  }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = b.getChar(0);                                }
    }

    public static class LongTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.putLong(0, -1L);                                  }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = b.getLong(0);                                }
    }

    public static class DoubleTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.putDouble(0, -1D);                                }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = Double.doubleToRawLongBits(b.getDouble(0));  }
    }

    public static class FloatTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.putFloat(0, -1F);                                 }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = Float.floatToRawIntBits(b.getFloat(0));      }
    }

    public static class ByteTest extends ByteBufferTest {
        @Override public void actor1(ByteBuffer b, LongResult1 r)  { b.put(0, (byte) -1);                                }
        @Override public void actor2(ByteBuffer b, LongResult1 r)  { r.r1 = b.get();                                     }
    }

}
