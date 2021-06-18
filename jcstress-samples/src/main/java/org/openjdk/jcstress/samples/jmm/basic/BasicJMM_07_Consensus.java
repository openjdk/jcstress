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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class BasicJMM_07_Consensus {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_07_Consensus[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Another property comes for the inter-thread semantics deals not with partial, but total order.
        In JMM, synchronization order mandates that special "synchronization" actions always form a total
        order, consistent with program order.

        The most famous example that needs total order of operation is Dekker idiom, the building block
        of Dekker lock.

        x86_64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0    349,652,433    6.29%  Interesting  Violates sequential consistency
            0, 1  2,566,329,748   46.14%   Acceptable  Trivial under sequential consistency
            1, 0  2,640,017,118   47.47%   Acceptable  Trivial under sequential consistency
            1, 1      5,522,365    0.10%   Acceptable  Trivial under sequential consistency
    */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0",                   expect = ACCEPTABLE_INTERESTING, desc = "Violates sequential consistency")
    @State
    public static class PlainDekker {
        int x;
        int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = y;
        }

        @Actor
        public void actor2(II_Result r) {
            y = 1;
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Adding volatile to both $x and $y bring them together into synchronization order, and thus require
        the results to be consistent with the case when reads/writes form a total order.

          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0              0    0.00%   Forbidden  Violates sequential consistency
            0, 1  1,016,018,128   44.40%  Acceptable  Trivial under sequential consistency
            1, 0  1,068,127,239   46.68%  Acceptable  Trivial under sequential consistency
            1, 1    204,027,177    8.92%  Acceptable  Trivial under sequential consistency
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0",                   expect = FORBIDDEN,  desc = "Violates sequential consistency")
    @State
    public static class VolatileDekker {
        volatile int x;
        volatile int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = y;
        }

        @Actor
        public void actor2(II_Result r) {
            y = 1;
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        VarHandles acquire and release modes are too weak to achieve the required effect.
        VarHandles opaque mode is also too weak.

          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0    256,068,354    6.18%  Interesting  Violates sequential consistency
            0, 1  1,907,567,721   46.01%   Acceptable  Trivial under sequential consistency
            1, 0  1,975,159,576   47.64%   Acceptable  Trivial under sequential consistency
            1, 1      7,025,533    0.17%   Acceptable  Trivial under sequential consistency
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0",                   expect = ACCEPTABLE_INTERESTING,  desc = "Violates sequential consistency")
    @State
    public static class AcqRelDekker {
        static final VarHandle VH_X;
        static final VarHandle VH_Y;

        static {
            try {
                VH_X = MethodHandles.lookup().findVarHandle(AcqRelDekker.class, "x", int.class);
                VH_Y = MethodHandles.lookup().findVarHandle(AcqRelDekker.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        int x;
        int y;

        @Actor
        public void actor1(II_Result r) {
            VH_X.setRelease(this, 1);
            r.r1 = (int) VH_Y.getAcquire(this);
        }

        @Actor
        public void actor2(II_Result r) {
            VH_Y.setRelease(this, 1);
            r.r2 = (int) VH_X.getAcquire(this);
        }
    }

}
