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

public class BasicJMM_06_Causality {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_06_Causality[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        The next property helps some inter-thread semantics. In JMM, happens-before mandates what results are
        plausible and what are not, when non-synchronized reads are involved. That order is partial, so there
        are pairs of reads/writes we can tell nothing about order-wise.

        For example, in the case of two non-volatile variables, JMM allows observing
        "1, 0".

          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0    593,615,020    9.50%   Acceptable  Doing both reads early.
            0, 1     66,963,791    1.07%   Acceptable  Caught in the middle: $x is visible, $y is not.
            1, 0      5,559,858    0.09%  Interesting  Seeing $y, but not $x!
            1, 1  5,580,479,955   89.34%   Acceptable  Doing both reads late.
    */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Caught in the middle: $x is visible, $y is not.")
    @Outcome(id = "1, 0", expect = ACCEPTABLE_INTERESTING, desc = "Seeing $y, but not $x!")
    @State
    public static class PlainReads {
        int x;
        int y;

        @Actor
        public void actor1() {
            x = 1;
            y = 1;
        }

        @Actor
        public void actor2(II_Result r) {
            r.r1 = y;
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

      We can bisect the result above a little by rewriting the test in "opaque" accesses. This way,
      we instruct optimizing compilers to keep the accesses in order, and let the hardware decide what
      semantics those accesses have.

      On x86_64, which has the Total Store Order guarantee, we would not see the interesting case anymore:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0  1,136,906,006   19.41%   Acceptable  Doing both reads early.
            0, 1     78,452,276    1.34%   Acceptable  Caught in the middle: $x is visible, $y is not.
            1, 0              0    0.00%  Interesting  Seeing $y, but not $x!
            1, 1  4,642,078,902   79.25%   Acceptable  Doing both reads late.

      On AArch64, which is weaker, we would see the interesting case:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0    494,407,410   24.00%   Acceptable  Doing both reads early.
            0, 1     26,332,570    1.28%   Acceptable  Caught in the middle: $x is visible, $y is not.
            1, 0     14,042,443    0.68%  Interesting  Seeing $y, but not $x!
            1, 1  1,525,203,753   74.04%   Acceptable  Doing both reads late.
     */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Caught in the middle: $x is visible, $y is not.")
    @Outcome(id = "1, 0", expect = ACCEPTABLE_INTERESTING, desc = "Seeing $y, but not $x!")
    @State
    public static class OpaqueReads {
        static final VarHandle VH_X;
        static final VarHandle VH_Y;

        static {
            try {
                VH_X = MethodHandles.lookup().findVarHandle(OpaqueReads.class, "x", int.class);
                VH_Y = MethodHandles.lookup().findVarHandle(OpaqueReads.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        int x;
        int y;

        @Actor
        public void actor1() {
            VH_X.setOpaque(this, 1);
            VH_Y.setOpaque(this, 1);
        }

        @Actor
        public void actor2(II_Result r) {
            r.r1 = (int) VH_Y.getOpaque(this);
            r.r2 = (int) VH_X.getOpaque(this);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

         The easiest way to solve this is to mark $y as "volatile". In this case, JMM would disallow seeing
         (1, 0). Volatile write would now be "release"-ing write and volatile read would now be "acquiring"
         read. That means all writes that precede releasing store would be visible to readers of acquiring
         read. Note this effect is only guaranteed if the acquiring read sees the value written by releasing
         write.

         Indeed, in all configurations, we shall see zero samples for the now forbidden
         test case.

         AArch64:
            RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
              0, 0  615,329,595   53.92%  Acceptable  Doing both reads early.
              0, 1   38,517,623    3.37%  Acceptable  Caught in the middle: $x is visible, $y is not.
              1, 0            0    0.00%   Forbidden  Seeing $y, but not $x!
              1, 1  487,416,398   42.71%  Acceptable  Doing both reads late.
     */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Caught in the middle: $x is visible, $y is not.")
    @Outcome(id = "1, 0", expect = FORBIDDEN, desc = "Seeing $y, but not $x!")
    @State
    public static class VolatileGuard {

        int x;
        volatile int y;

        @Actor
        public void actor1() {
            x = 1;
            y = 1;
        }

        @Actor
        public void actor2(II_Result r) {
            r.r1 = y;
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        VarHandles acquire and release modes can be used to achieve the same effect, but
        anything weaker is not guaranteed to work.

        AArch64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0  782,933,737   60.67%  Acceptable  Doing both reads early.
            0, 1   12,754,346    0.99%  Acceptable  Caught in the middle: $x is visible, $y is not.
            1, 0            0    0.00%   Forbidden  Seeing $y, but not $x!
            1, 1  494,741,613   38.34%  Acceptable  Doing both reads late.
     */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Caught in the middle: $x is visible, $y is not.")
    @Outcome(id = "1, 0", expect = FORBIDDEN, desc = "Seeing $y, but not $x!")
    @State
    public static class AcquireReleaseGuard {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(AcquireReleaseGuard.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        int x;
        int y;

        @Actor
        public void actor1() {
            x = 1;
            VH.setRelease(this, 1);
        }

        @Actor
        public void actor2(II_Result r) {
            r.r1 = (int) VH.getAcquire(this);
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Of course, the same thing is achievable with locks, except that (0, 1) is forbidden due to
        mutual exclusion of the entire locked section.

        AArch64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0  341,919,270   46.04%  Acceptable  Doing both reads early.
            0, 1            0    0.00%   Forbidden  Caught in the middle: $x is visible, $y is not.
            1, 0            0    0.00%   Forbidden  Seeing $y, but not $x!
            1, 1  400,772,826   53.96%  Acceptable  Doing both reads late.
     */
    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = FORBIDDEN, desc = "Caught in the middle: $x is visible, $y is not.")
    @Outcome(id = "1, 0", expect = FORBIDDEN, desc = "Seeing $y, but not $x!")
    @State
    public static class LockGuard {

        int x;
        int y;

        @Actor
        public void actor1() {
            synchronized (this) {
                x = 1;
                y = 1;
            }
        }

        @Actor
        public void actor2(II_Result r) {
            synchronized (this) {
                r.r1 = y;
                r.r2 = x;
            }
        }
    }

}
