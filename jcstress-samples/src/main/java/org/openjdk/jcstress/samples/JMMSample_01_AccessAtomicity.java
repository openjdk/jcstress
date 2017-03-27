/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jcstress.samples;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.J_Result;
import org.openjdk.jcstress.util.UnsafeHolder;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class JMMSample_01_AccessAtomicity {

    /*
      ----------------------------------------------------------------------------------------------------------

        This is our first case: access atomicity. Most basic types come with an
        intuitive property: the reads and the writes of these basic types happen
        in full, even under races:

              [OK] org.openjdk.jcstress.samples.JMMSample_01_AccessAtomicity.Integers
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                      -1   221,268,498    ACCEPTABLE  Seeing the full value.
                       0    17,764,332    ACCEPTABLE  Seeing the default value: writer had not acted yet.

    */

    @JCStressTest
    @Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = Expect.FORBIDDEN, desc = "Other cases are forbidden.")
    @State
    public static class Integers {
        int v;

        @Actor
        public void writer() {
            v = 0xFFFFFFFF;
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        There are a few interesting exceptions codified in Java Language Specification,
        under 17.7 "Non-Atomic Treatment of double and long". It says that longs and
        doubles could be treated non-atomically.

        NOTE: This test would yield interesting results on 32-bit VMs.

               [OK] org.openjdk.jcstress.samples.JMMSample_01_AccessAtomicity.Longs
            (JVM args: [-server])
          Observed state   Occurrences              Expectation  Interpretation
                      -1   181,716,629               ACCEPTABLE  Seeing the full value.
             -4294967296        40,481   ACCEPTABLE_INTERESTING  Other cases are violating access atomicity, but allowed u...
                       0    10,439,305               ACCEPTABLE  Seeing the default value: writer had not acted yet.
              4294967295         2,545   ACCEPTABLE_INTERESTING  Other cases are violating access atomicity, but allowed u...
     */

    @JCStressTest
    @Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Other cases are violating access atomicity, but allowed under JLS.")
    @Ref("https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.7")
    @State
    public static class Longs {
        long v;

        @Actor
        public void writer() {
            v = 0xFFFFFFFF_FFFFFFFFL;
        }

        @Actor
        public void reader(J_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Recovering the access atomicity is possible with making the field "volatile":

               [OK] org.openjdk.jcstress.samples.JMMSample_01_AccessAtomicity.VolatileLongs
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                      -1    25,920,268    ACCEPTABLE  Seeing the full value.
                       0   101,853,902    ACCEPTABLE  Seeing the default value: writer had not acted yet.
     */

    @JCStressTest
    @Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = Expect.FORBIDDEN, desc = "Other cases are forbidden.")
    @State
    public static class VolatileLongs {
        volatile long v;

        @Actor
        public void writer() {
            v = 0xFFFFFFFF_FFFFFFFFL;
        }

        @Actor
        public void reader(J_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Since Java 9, VarHandles in "opaque" access mode also require access atomicity.
        (The same applies for "acquire/release", and "volatile" access modes too!)

              [OK] org.openjdk.jcstress.samples.JMMSample_01_AccessAtomicity.OpaqueLongs
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                      -1   161,358,765    ACCEPTABLE  Seeing the full value.
                       0     5,778,795    ACCEPTABLE  Seeing the default value: writer had not acted yet.
     */

    @JCStressTest
    @Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = Expect.FORBIDDEN, desc = "Other cases are forbidden.")
    @State
    public static class OpaqueLongs {

        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(OpaqueLongs.class, "v", long.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        long v;

        @Actor
        public void writer() {
            VH.setOpaque(this, 0xFFFFFFFF_FFFFFFFFL);
        }

        @Actor
        public void reader(J_Result r) {
            r.r1 = (long) VH.getOpaque(this);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        While the spec requirements for field and array element accesses are
        strict, the implementations of concrete classes may have a relaxed
        semantics. Take ByteBuffer where we can read the 4-byte integer from
        an arbitrary offset.

        Older ByteBuffer implementations accessed one byte at a time, and that
        required merging/splitting anything larger than a byte into the individual
        operations. Of course, there is no access atomicity there by construction.
        In newer ByteBuffer implementations, the _aligned_ accesses are done with
        larger instructions that gives back atomicity. Misaligned accesses would
        still have to do several narrower accesses on machines that don't support
        misalignments.

              [OK] org.openjdk.jcstress.samples.JMMSample_01_AccessAtomicity.ByteBuffers
            (JVM args: [-server])
          Observed state   Occurrences              Expectation  Interpretation
                      -1    32,580,349               ACCEPTABLE  Seeing the full value.
               -16777216         2,791   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                    -256         2,739   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                  -65536         2,848   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                       0     5,961,926               ACCEPTABLE  Seeing the default value: writer had not acted yet.
                16777215         1,467   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                     255         1,502   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                   65535         1,498   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
    */

    @JCStressTest
    @Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Other cases are allowed, because reads/writes are not atomic.")
    @State
    public static class ByteBuffers {
        public static final int SIZE = 256;

        ByteBuffer bb = ByteBuffer.allocate(SIZE);
        int idx = ThreadLocalRandom.current().nextInt(SIZE - 4);

        @Actor
        public void writer() {
            bb.putInt(idx, 0xFFFFFFFF);
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = bb.getInt(idx);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        However, even if the misaligned accesses is supported by hardware, it would never
        be guaranteed atomic. For example, reading the value that spans two cache-lines would
        not be atomic, even if we manage to issue a single instruction for access.

              [OK] org.openjdk.jcstress.samples.JMMSample_01_AccessAtomicity.UnsafeCrossCacheLine
            (JVM args: [-server])
          Observed state   Occurrences              Expectation  Interpretation
                      -1    40,495,875               ACCEPTABLE  Seeing the full value.
               -16777216           760   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                    -256           726   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                  -65536           789   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                       0     2,136,183               ACCEPTABLE  Seeing the default value: writer had not acted yet.
                16777215           539   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                     255           554   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
                   65535           574   ACCEPTABLE_INTERESTING  Other cases are allowed, because reads/writes are not ato...
     */

    @JCStressTest
    @Outcome(id = "0",  expect = Expect.ACCEPTABLE, desc = "Seeing the default value: writer had not acted yet.")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Seeing the full value.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Other cases are allowed, because reads/writes are not atomic.")
    @State
    public static class UnsafeCrossCacheLine {

        public static final int SIZE = 256;
        public static final long ARRAY_BASE_OFFSET = UnsafeHolder.U.arrayBaseOffset(byte[].class);
        public static final long ARRAY_BASE_SCALE = UnsafeHolder.U.arrayIndexScale(byte[].class);

        byte[] ss = new byte[SIZE];
        long off = ARRAY_BASE_OFFSET + ARRAY_BASE_SCALE * ThreadLocalRandom.current().nextInt(SIZE - 4);

        @Actor
        public void writer() {
            UnsafeHolder.U.putInt(ss, off, 0xFFFFFFFF);
        }

        @Actor
        public void reader(I_Result r) {
            r.r1 = UnsafeHolder.U.getInt(ss, off);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Conclusion: for fields and array elements access atomicity is guaranteed, except
        for non-volatile longs and doubles. Regaining the atomicity is possible with anything
        stronger than a plain read/write.

        Are reads/writes atomic?

                        boolean,    byte,   char,   short,     int,    float,  double,  long,   Object

          plain:            yes      yes     yes      yes      yes       yes       NO     NO      yes
          volatile:         yes      yes     yes      yes      yes       yes      YES    YES      yes

          VH (plain):       yes      yes     yes      yes      yes       yes       NO     NO      yes
          VH (opaque):      yes      yes     yes      yes      yes       yes      YES    YES      yes
          VH (acq/rel):     yes      yes     yes      yes      yes       yes      YES    YES      yes
          VH (volatile):    yes      yes     yes      yes      yes       yes      YES    YES      yes

        Access atomicity for unnatural accesses is not guaranteed. Alignment issues,
        implementation quirks, etc. may deconstruct the access atomicity. The case of single
        aligned reads/writes is similar to the usual language guarantees.

        Are reads/writes atomic?

                                                   access type:
                                    multiple,   single misaligned,  single aligned

          plain:                           no                  no,          YES/NO (yes, except for long/double)
          volatile:                        no                  no,             YES
          VH (plain):                      no,                 no,          YES/NO (yes, except for long/double)
          VH (opaque):                     no,                 no,             YES
          VH (acq/rel):                    no,                 no,             YES
          VH (volatile):                   no,                 no,             YES

     */

}
