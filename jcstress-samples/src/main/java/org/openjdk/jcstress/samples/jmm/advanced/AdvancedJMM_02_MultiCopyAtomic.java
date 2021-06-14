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

public class AdvancedJMM_02_MultiCopyAtomic {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_02_MultiCopyAtomic[.SubTestName]
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

              RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
          0, 0, 0, 0     37,176,296    0.64%   Acceptable  Boring
          0, 0, 0, 1      9,400,698    0.16%   Acceptable  Boring
          0, 0, 1, 0      7,820,972    0.13%   Acceptable  Boring
          0, 0, 1, 1    264,268,403    4.54%   Acceptable  Boring
          0, 1, 0, 0      6,000,722    0.10%   Acceptable  Boring
          0, 1, 0, 1        285,037   <0.01%   Acceptable  Boring
          0, 1, 1, 0     33,201,729    0.57%   Acceptable  Boring
          0, 1, 1, 1    119,718,218    2.05%   Acceptable  Boring
          1, 0, 0, 0      5,952,891    0.10%   Acceptable  Boring
          1, 0, 0, 1     42,960,279    0.74%   Acceptable  Boring
          1, 0, 1, 0        144,597   <0.01%  Interesting  Whoa
          1, 0, 1, 1    111,705,898    1.92%   Acceptable  Boring
          1, 1, 0, 0    149,136,163    2.56%   Acceptable  Boring
          1, 1, 0, 1    108,356,855    1.86%   Acceptable  Boring
          1, 1, 1, 0    100,533,303    1.73%   Acceptable  Boring
          1, 1, 1, 1  4,829,074,643   82.89%   Acceptable  Boring
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
        If we follow the "usual" fencing around the accesses, it would not help on non-multi-copy-atomic
        platforms. Even though we stick all accesses in their places, and prevent reads going one over the
        other, the asynchronous nature of memory updates would manifest on some platforms.

        PPC64:
              RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          0, 0, 0, 0  626,124,064   22.51%   Acceptable  Boring
          0, 0, 0, 1  100,592,056    3.62%   Acceptable  Boring
          0, 0, 1, 0   86,443,974    3.11%   Acceptable  Boring
          0, 0, 1, 1  117,149,197    4.21%   Acceptable  Boring
          0, 1, 0, 0  102,075,205    3.67%   Acceptable  Boring
          0, 1, 0, 1      270,587   <0.01%   Acceptable  Boring
          0, 1, 1, 0  278,452,698   10.01%   Acceptable  Boring
          0, 1, 1, 1   18,548,574    0.67%   Acceptable  Boring
          1, 0, 0, 0   96,280,543    3.46%   Acceptable  Boring
          1, 0, 0, 1  311,153,395   11.19%   Acceptable  Boring
          1, 0, 1, 0        7,599   <0.01%  Interesting  Whoa
          1, 0, 1, 1   14,614,329    0.53%   Acceptable  Boring
          1, 1, 0, 0  195,921,897    7.04%   Acceptable  Boring
          1, 1, 0, 1   52,391,418    1.88%   Acceptable  Boring
          1, 1, 1, 0   42,992,478    1.55%   Acceptable  Boring
          1, 1, 1, 1  738,325,730   26.55%   Acceptable  Boring
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

        Depending on the hardware, a more thorough fencing might be required. For example, on PPC64,
        we would need to emit a full fence before the sequentially consistent read, if we want to stop
        seeing non-MCA behaviors. Indeed, in this test, the interesting cases are gone.

        PPC64:
              RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
          0, 0, 0, 0     16,428,729    0.62%   Acceptable  Boring
          0, 0, 0, 1      1,054,058    0.04%   Acceptable  Boring
          0, 0, 1, 0        219,026   <0.01%   Acceptable  Boring
          0, 0, 1, 1     82,801,314    3.11%   Acceptable  Boring
          0, 1, 0, 0      2,250,558    0.08%   Acceptable  Boring
          0, 1, 0, 1        248,656   <0.01%   Acceptable  Boring
          0, 1, 1, 0      3,318,302    0.12%   Acceptable  Boring
          0, 1, 1, 1     89,272,976    3.36%   Acceptable  Boring
          1, 0, 0, 0        375,410    0.01%   Acceptable  Boring
          1, 0, 0, 1      3,598,655    0.14%   Acceptable  Boring
          1, 0, 1, 0              0    0.00%  Interesting  Whoa
          1, 0, 1, 1     81,560,207    3.07%   Acceptable  Boring
          1, 1, 0, 0     94,117,189    3.54%   Acceptable  Boring
          1, 1, 0, 1    124,773,490    4.69%   Acceptable  Boring
          1, 1, 1, 0    107,590,486    4.05%   Acceptable  Boring
          1, 1, 1, 1  2,051,591,968   77.15%   Acceptable  Boring
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