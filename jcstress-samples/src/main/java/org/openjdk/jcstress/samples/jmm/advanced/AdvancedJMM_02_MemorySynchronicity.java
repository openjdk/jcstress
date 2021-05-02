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
package org.openjdk.jcstress.samples.jmm.advanced;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;
import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

public class AdvancedJMM_02_MemorySynchronicity {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_02_MemorySynchronicity[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Another interesting quirk that comes from wrong intuition is the belief that once values hit the
        memory/barriers they would be globally exposed to the rest of the system. That is not necessarily
        true, because not all hardware provides the interesting property: multi-copy atomicity.

        Multi-copy atomicity is "either the new value is visible to all processors, or no processor sees it".
        That is, multi-copy atomicity means the update to the single location is "instantaneously" visible
        to all processors in the system. To test this property, we can perform the "Independent Reads of
        Independent Writes" test. Doing this test with "opaque" accesses targets the underlying hardware,
        rather than optimizing compilers.

        IRIW detects whether the independent writes to "x" and "y" are seen in different orders by two
        other workers.

        On x86_64 -- that is multi-copy atomic architecture -- this test yields:

              RESULT         SAMPLES     FREQ       EXPECT  DESCRIPTION
          0, 0, 0, 0   1,018,009,462    3.39%   Acceptable  Boring
          0, 0, 0, 1     192,218,036    0.64%   Acceptable  Boring
          0, 0, 1, 0     188,848,400    0.63%   Acceptable  Boring
          0, 0, 1, 1     726,013,966    2.42%   Acceptable  Boring
          0, 1, 0, 0     229,095,909    0.76%   Acceptable  Boring
          0, 1, 0, 1         610,683   <0.01%   Acceptable  Boring
          0, 1, 1, 0   2,968,164,366    9.88%   Acceptable  Boring
          0, 1, 1, 1   1,280,404,610    4.26%   Acceptable  Boring
          1, 0, 0, 0     164,642,284    0.55%   Acceptable  Boring
          1, 0, 0, 1   2,762,782,477    9.19%   Acceptable  Boring
          1, 0, 1, 0               0    0.00%  Interesting  Whoa
          1, 0, 1, 1   1,080,270,592    3.59%   Acceptable  Boring
          1, 1, 0, 0     793,200,776    2.64%   Acceptable  Boring
          1, 1, 0, 1   1,282,983,565    4.27%   Acceptable  Boring
          1, 1, 1, 0   1,248,633,288    4.15%   Acceptable  Boring
          1, 1, 1, 1  16,117,960,946   53.63%   Acceptable  Boring

        But on PPC64 -- that is not a multi-copy atomic architecture -- this test yields:

              RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          0, 0, 0, 0    8,000,004    0.78%   Acceptable  Boring
          0, 0, 0, 1    1,464,110    0.14%   Acceptable  Boring
          0, 0, 1, 0    1,179,814    0.12%   Acceptable  Boring
          0, 0, 1, 1   41,275,652    4.03%   Acceptable  Boring
          0, 1, 0, 0    1,038,437    0.10%   Acceptable  Boring
          0, 1, 0, 1       60,198   <0.01%   Acceptable  Boring
          0, 1, 1, 0    5,957,811    0.58%   Acceptable  Boring
          0, 1, 1, 1   19,326,879    1.88%   Acceptable  Boring
          1, 0, 0, 0      999,321    0.10%   Acceptable  Boring
          1, 0, 0, 1    6,711,610    0.65%   Acceptable  Boring
          1, 0, 1, 0       28,752   <0.01%  Interesting  Whoa
          1, 0, 1, 1   19,428,477    1.89%   Acceptable  Boring
          1, 1, 0, 0   21,080,890    2.06%   Acceptable  Boring
          1, 1, 0, 1   17,442,987    1.70%   Acceptable  Boring
          1, 1, 1, 0   15,403,205    1.50%   Acceptable  Boring
          1, 1, 1, 1  865,916,157   84.45%   Acceptable  Boring
     */

    @JCStressTest
    @Outcome(id = "1, 0, 1, 0", expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    @Outcome(                   expect = ACCEPTABLE,             desc = "Boring")
    @State
    public static class OpaqueIRIW {

        static final VarHandle VH_X, VH_Y;

        static {
            try {
                VH_X = MethodHandles.lookup().findVarHandle(OpaqueIRIW.class, "x", int.class);
                VH_Y = MethodHandles.lookup().findVarHandle(OpaqueIRIW.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public int x;
        public int y;

        @Actor
        public void actor1() {
            VH_X.setOpaque(this, 1);
        }

        @Actor
        public void actor2() {
            VH_Y.setOpaque(this, 1);
        }

        @Actor
        public void actor3(IIII_Result r) {
            r.r1 = (int) VH_X.getOpaque(this);
            r.r2 = (int) VH_Y.getOpaque(this);
        }

        @Actor
        public void actor4(IIII_Result r) {
            r.r3 = (int) VH_Y.getOpaque(this);
            r.r4 = (int) VH_X.getOpaque(this);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        To see why this is not a vanilla memory reordering, we can put fences around the critical accesses.
        If we follow the "usual" fencing around the seqc


PPC64:
      RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
  0, 0, 0, 0  116,646,109   22.21%   Acceptable  Boring
  0, 0, 0, 1   19,589,207    3.73%   Acceptable  Boring
  0, 0, 1, 0   13,989,162    2.66%   Acceptable  Boring
  0, 0, 1, 1   22,118,188    4.21%   Acceptable  Boring
  0, 1, 0, 0   16,780,917    3.20%   Acceptable  Boring
  0, 1, 0, 1       44,960   <0.01%   Acceptable  Boring
  0, 1, 1, 0   54,578,089   10.39%   Acceptable  Boring
  0, 1, 1, 1    3,448,588    0.66%   Acceptable  Boring
  1, 0, 0, 0   15,474,140    2.95%   Acceptable  Boring
  1, 0, 0, 1   63,265,053   12.05%   Acceptable  Boring
  1, 0, 1, 0        2,151   <0.01%  Interesting  Whoa
  1, 0, 1, 1    2,820,992    0.54%   Acceptable  Boring
  1, 1, 0, 0   33,421,547    6.36%   Acceptable  Boring
  1, 1, 0, 1   10,101,731    1.92%   Acceptable  Boring
  1, 1, 1, 0    9,358,080    1.78%   Acceptable  Boring
  1, 1, 1, 1  143,471,870   27.32%   Acceptable  Boring
     */

    @JCStressTest
    @Outcome(id = "1, 0, 1, 0", expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    @Outcome(                   expect = ACCEPTABLE,             desc = "Boring")
    @State
    public static class FencedIRIWTest {

        public int x;
        public int y;

        @Actor
        public void actor1() {
            UNSAFE.fullFence(); // "SeqCst" store
            x = 1;
        }

        @Actor
        public void actor2() {
            UNSAFE.fullFence(); // "SeqCst" store
            y = 1;
        }

        @Actor
        public void actor3(IIII_Result r) {
            r.r1 = x;
            UNSAFE.loadFence(); // "SeqCst" load x
            r.r2 = y;
            UNSAFE.loadFence(); // "SeqCst" load y
        }

        @Actor
        public void actor4(IIII_Result r) {
            r.r3 = y;
            UNSAFE.loadFence(); // "SeqCst" load y
            r.r4 = x;
            UNSAFE.loadFence(); // "SeqCst" load x
        }

    }

    /*
      ----------------------------------------------------------------------------------------------------------

        To see why this is not a vanilla memory reordering, we can put fences around the critical accesses.
        If we follow the "usual" fencing around the seqc

PPC64:
      RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
  0, 0, 0, 0    3,162,682    0.63%   Acceptable  Boring
  0, 0, 0, 1      420,692    0.08%   Acceptable  Boring
  0, 0, 1, 0       49,183   <0.01%   Acceptable  Boring
  0, 0, 1, 1   17,923,289    3.55%   Acceptable  Boring
  0, 1, 0, 0      344,895    0.07%   Acceptable  Boring
  0, 1, 0, 1       58,153    0.01%   Acceptable  Boring
  0, 1, 1, 0      703,765    0.14%   Acceptable  Boring
  0, 1, 1, 1   17,055,833    3.38%   Acceptable  Boring
  1, 0, 0, 0       55,722    0.01%   Acceptable  Boring
  1, 0, 0, 1      926,450    0.18%   Acceptable  Boring
  1, 0, 1, 0            0    0.00%  Interesting  Whoa
  1, 0, 1, 1   15,951,198    3.16%   Acceptable  Boring
  1, 1, 0, 0   15,055,385    2.98%   Acceptable  Boring
  1, 1, 0, 1   23,580,682    4.67%   Acceptable  Boring
  1, 1, 1, 0   20,358,942    4.03%   Acceptable  Boring
  1, 1, 1, 1  389,383,273   77.10%   Acceptable  Boring
     */

    @JCStressTest
    @Outcome(id = "1, 0, 1, 0", expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    @Outcome(                   expect = ACCEPTABLE,             desc = "Boring")
    @State
    public static class FullyFencedIRIWTest {

        public int x;
        public int y;

        @Actor
        public void actor1() {
            UNSAFE.fullFence(); // "SeqCst" store
            x = 1;
        }

        @Actor
        public void actor2() {
            UNSAFE.fullFence(); // "SeqCst" store
            y = 1;
        }

        @Actor
        public void actor3(IIII_Result r) {
            UNSAFE.fullFence(); // "SeqCst" load x, part 1
            r.r1 = x;
            UNSAFE.fullFence(); // "SeqCst" load x, part 2 (subsumed); "SeqCst" load y, part 1
            r.r2 = y;
            UNSAFE.loadFence(); // "SeqCst" load y, part 2
        }

        @Actor
        public void actor4(IIII_Result r) {
            UNSAFE.fullFence();
            r.r3 = y;
            UNSAFE.fullFence(); // subsumes loadFence
            r.r4 = x;
            UNSAFE.loadFence();
        }

    }

}