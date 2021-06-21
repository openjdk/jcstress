/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.jmm.basic;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.BitSet;

import static org.openjdk.jcstress.annotations.Expect.*;

public class BasicJMM_03_WordTearing {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_03_WordTearing[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Java Memory Model prohibits word tearing. That is, it requires that every field and array element
        as distinct, and the operations for one element should not disturb others.

        Note this is a bit different from access atomicity. Access atomicity says that the accesses to
        a _wide_ logical field should be atomic, even if it requires several _narrower_ physical accesses.
        Prohibited word tearing means the accesses to a _narrow_ logical field should not disturb the adjacent
        fields, even if done with a _wider_ physical access.

        Indeed, the test on plain boolean arrays shows this rule holds:

              RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          true, true  489,444,864  100.00%  Acceptable  Seeing both updates intact.
      */

    @JCStressTest
    @Outcome(id = "true, true", expect = ACCEPTABLE, desc = "Seeing both updates intact.")
    @Outcome(                   expect = FORBIDDEN,  desc = "Other cases are forbidden.")
    @State
    public static class JavaArrays {
        boolean[] bs = new boolean[2];

        @Actor
        public void writer1() {
            bs[0] = true;
        }

        @Actor
        public void writer2() {
            bs[1] = true;
        }

        @Arbiter
        public void arbiter(ZZ_Result r) {
            r.r1 = bs[0];
            r.r2 = bs[1];
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        However, while that requirement is enforced for fields and array elements, the Java classes
        implementations may still violate this requirement, if, say, they pack elements densely, and
        read/write adjacent elements routinely. The usual example of this is java.util.BitSet.

        Indeed, this is simple to reproduce:

               RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          false, true   75,809,316   16.27%  Interesting  Destroyed one update.
          true, false   84,291,298   18.09%  Interesting  Destroyed one update.
           true, true  305,945,850   65.65%   Acceptable  Seeing both updates intact.
     */

    @JCStressTest
    @Outcome(id = "true, true",  expect = ACCEPTABLE, desc = "Seeing both updates intact.")
    @Outcome(id = "false, true", expect = ACCEPTABLE_INTERESTING, desc = "Destroyed one update.")
    @Outcome(id = "true, false", expect = ACCEPTABLE_INTERESTING, desc = "Destroyed one update.")
    @State
    public static class BitSets {

        BitSet bs = new BitSet();

        @Actor
        public void writer1() {
            bs.set(0);
        }

        @Actor
        public void writer2() {
            bs.set(1);
        }

        @Arbiter
        public void arbiter(ZZ_Result r) {
            r.r1 = bs.get(0);
            r.r2 = bs.get(1);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        When no Java code explicitly accepts the word tearing, the JVM has to work with hardware
        to prohibit it. On the majority of hardware platforms, all accesses are possible with
        their natural widths: bytes can be accessed with 1-byte accesses, shorts with 2-byte accesses,
        etc. The exception from that rule is boolean, which can be technically represented with
        a single bit, but most hardware has only 1-byte reads/writes. Because of this, JVM normally
        allocates 1 byte per boolean.

        There are peculiarities with atomic instructions. When the sub-word atomic accesses are done
        on platforms that do not have direct sub-word accesses, CASes should still work _as if_
        the boolean fields are distinct.

        For example, this test passes on ARM32, that does not have byte-wide CASes. The test verifies
        that CAS over each of the fields do not conflict, and both are able to succeed.

              RESULT     SAMPLES     FREQ      EXPECT  DESCRIPTION
          true, true  62,199,552  100.00%  Acceptable  All CASes succeed
     */

    @JCStressTest
    @Outcome(id = "true, true", expect = ACCEPTABLE, desc = "All CASes succeed")
    @Outcome(                   expect = FORBIDDEN,  desc = "CAS word tearing")
    @State
    public static class ByteCAS {
        static final VarHandle VH_A, VH_B;

        static {
            try {
                VH_A = MethodHandles.lookup().findVarHandle(ByteCAS.class, "a", boolean.class);
                VH_B = MethodHandles.lookup().findVarHandle(ByteCAS.class, "b", boolean.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        boolean a;
        boolean b;

        @Actor
        public void cas1(ZZ_Result r) {
            r.r1 = VH_A.compareAndSet(this, false, true);
        }

        @Actor
        public void cas2(ZZ_Result r) {
            r.r2 = VH_B.compareAndSet(this, false, true);
        }
    }

}
