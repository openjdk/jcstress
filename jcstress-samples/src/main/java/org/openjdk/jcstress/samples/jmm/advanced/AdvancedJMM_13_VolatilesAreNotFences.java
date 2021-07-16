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
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;
import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

public class AdvancedJMM_13_VolatilesAreNotFences {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_13_VolatilesAreNotFences[.SubTestName]
     */

    /*
       ----------------------------------------------------------------------------------------------------------

        Similarly to AdvancedJMM_12_SynchronizedAreNotFences example, the volatile accesses cannot be reliably
        used for their auxiliary memory effects. In this example, if we do not observe the write of the "b", then
        we can see the old "x", even though volatile accesses _might_ be implemented with barriers.

        This reproduces on AArch64:
            RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
           0, 0, 0  401,242,129   51.33%   Acceptable  Boring
           0, 0, 1   12,608,887    1.61%   Acceptable  Irrelevant
           0, 1, 1    6,231,104    0.80%   Acceptable  Irrelevant
           1, 0, 0       91,935    0.01%  Interesting  Whoa
           1, 0, 1    4,941,677    0.63%   Acceptable  Irrelevant
           1, 1, 1  356,621,484   45.62%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"0, 0, 0", "1, 1, 1"},   expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = {"0, .*, 1", "1, .*, 1"}, expect = ACCEPTABLE,             desc = "Irrelevant")
    @Outcome(id = "1, 0, 0",                expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class Volatiles {
        int x, y;
        volatile int b;

        @Actor
        void thread1() {
            x = 1;
            b = 1; // fake "release"
            y = 1;
        }

        @Actor
        void thread2(III_Result r) {
            r.r1 = y;
            r.r2 = b; // fake "acquire"
            r.r3 = x;
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        Once again, using the fences directly helps to get the effect that we want.

        Same AArch64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0   97,306,826   10.81%  Acceptable  Boring
            0, 1    9,990,750    1.11%  Acceptable  Plausible
            1, 0            0    0.00%   Forbidden  Now forbidden
            1, 1  793,182,680   88.08%  Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0, 1",           expect = ACCEPTABLE, desc = "Plausible")
    @Outcome(id = "1, 0",           expect = FORBIDDEN,  desc = "Now forbidden")
    public static class Fences {
        int x, y;

        @Actor
        void thread1() {
            x = 1;
            UNSAFE.storeFence();
            y = 1;
        }

        @Actor
        void thread2(II_Result r) {
            r.r1 = y;
            UNSAFE.loadFence();
            r.r2 = x;
        }
    }

}