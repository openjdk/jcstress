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
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class ByteBufferViewsAtomicityTests {

    public static ByteBuffer order(ByteBuffer b) { b.order(ByteOrder.nativeOrder()); return b; }

    public static class IntViewTest implements Actor2_Test<IntBuffer, LongResult1> {
        @Override public IntBuffer newState()                        { return order(ByteBuffer.allocate(16)).asIntBuffer(); }
        @Override public LongResult1 newResult()                     { return new LongResult1();                         }
        @Override public void actor1(IntBuffer b, LongResult1 r)     { b.put(0, -1);                                     }
        @Override public void actor2(IntBuffer b, LongResult1 r)     { r.r1 = b.get(0);                                  }
    }

    public static class CharViewTest implements Actor2_Test<CharBuffer, LongResult1> {
        @Override public CharBuffer newState()                       { return order(ByteBuffer.allocate(16)).asCharBuffer();    }
        @Override public LongResult1 newResult()                     { return new LongResult1();                         }
        @Override public void actor1(CharBuffer b, LongResult1 r)    { b.put(0, 'a');                                    }
        @Override public void actor2(CharBuffer b, LongResult1 r)    { r.r1 = b.get(0);                                  }
    }

    public static class DoubleViewTest implements Actor2_Test<DoubleBuffer, LongResult1> {
        @Override public DoubleBuffer newState()                     { return order(ByteBuffer.allocate(16)).asDoubleBuffer();  }
        @Override public LongResult1 newResult()                     { return new LongResult1();                         }
        @Override public void actor1(DoubleBuffer b, LongResult1 r)  { b.put(0, -1D);                                    }
        @Override public void actor2(DoubleBuffer b, LongResult1 r)  { r.r1 = Double.doubleToRawLongBits(b.get(0));      }
    }

    public static class FloatViewTest implements Actor2_Test<FloatBuffer, LongResult1> {
        @Override public FloatBuffer newState()                      { return order(ByteBuffer.allocate(16)).asFloatBuffer();   }
        @Override public LongResult1 newResult()                     { return new LongResult1();                         }
        @Override public void actor1(FloatBuffer b, LongResult1 r)   { b.put(0, -1F);                                    }
        @Override public void actor2(FloatBuffer b, LongResult1 r)   { r.r1 = Float.floatToRawIntBits(b.get(0));         }
    }

    public static class LongViewTest implements Actor2_Test<LongBuffer, LongResult1> {
        @Override public LongBuffer newState()                       { return order(ByteBuffer.allocate(16)).asLongBuffer();    }
        @Override public LongResult1 newResult()                     { return new LongResult1();                         }
        @Override public void actor1(LongBuffer b, LongResult1 r)    { b.put(0, -1);                                     }
        @Override public void actor2(LongBuffer b, LongResult1 r)    { r.r1 = b.get(0);                                  }
    }

    public static class ShortViewTest implements Actor2_Test<ShortBuffer, LongResult1> {
        @Override public ShortBuffer newState()                      { return order(ByteBuffer.allocate(16)).asShortBuffer();   }
        @Override public LongResult1 newResult()                     { return new LongResult1();                         }
        @Override public void actor1(ShortBuffer b, LongResult1 r)   { b.put(0, (short) -1);                             }
        @Override public void actor2(ShortBuffer b, LongResult1 r)   { r.r1 = b.get(0);                                  }
    }

}
