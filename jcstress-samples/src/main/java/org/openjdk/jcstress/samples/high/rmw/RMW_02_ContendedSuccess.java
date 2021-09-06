/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.high.rmw;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class RMW_02_ContendedSuccess {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_02_ContendedSuccess[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This tests explores a more advanced property of CASes:
          - for strong CASes, exactly one operation should succeed
          - for weak CASes, at most one operation should succeed (accept spurious failures)

        The weak CASes are normally only seen on the platforms that implement LL/SC-based atomics,
        so these tests are good to run on AArch64 without LSE atomics (in Hotspot, this is achieved
        with -XX:-UseLSE).
     */

    @State
    public static class S {
        private int v;
        public static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(S.class, "v", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The strong CAS should always succeed, as long as there is no conflicts.

        Indeed, on both x86_64 and AArch64 this is the result:

                RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          false, false            0    0.00%   Forbidden  Not even once
           false, true  232,042,522   52.44%  Acceptable  Trivial
           true, false  210,454,502   47.56%  Acceptable  Trivial
            true, true            0    0.00%   Forbidden  More than once

     */

    @JCStressTest
    @Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Trivial")
    @Outcome(id = "false, false",                 expect = FORBIDDEN,  desc = "Not even once")
    @Outcome(id = "true, true",                   expect = FORBIDDEN,  desc = "More than once")
    public static class Strong {
        @Actor
        public void actor1(S s, ZZ_Result r) {
            r.r1 = S.VH.compareAndSet(s, 0, 1);
        }

        @Actor
        public void actor2(S s, ZZ_Result r) {
            r.r2 = S.VH.compareAndSet(s, 0, 1);
        }
    }


    /*
      ----------------------------------------------------------------------------------------------------------

        The weak CAS can spuriously fail, even without any conflict, but otherwise they
        cannot succeed both.

        It would not manifest on x86_64:

                RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          false, false            0    0.00%  Interesting  Not even once
           false, true  240,890,038   51.04%   Acceptable  Trivial
           true, false  231,108,426   48.96%   Acceptable  Trivial
            true, true            0    0.00%    Forbidden  More than once

        But it would manifest on AArch64 -XX:-UseLSE:

                RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          false, false          385   <0.01%  Interesting  Not even once
           false, true  152,343,703   48.79%   Acceptable  Trivial
           true, false  159,898,088   51.21%   Acceptable  Trivial
            true, true            0    0.00%    Forbidden  More than once
     */

    @JCStressTest
    @Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE,             desc = "Trivial")
    @Outcome(id = "false, false",                 expect = ACCEPTABLE_INTERESTING, desc = "Not even once")
    @Outcome(id = "true, true",                   expect = FORBIDDEN,              desc = "More than once")
    public static class Weak {
        @Actor
        public void actor1(S s, ZZ_Result r) {
            r.r1 = S.VH.weakCompareAndSet(s, 0, 1);
        }
        @Actor
        public void actor2(S s, ZZ_Result r) {
            r.r2 = S.VH.weakCompareAndSet(s, 0, 1);
        }
    }

}
