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
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;
import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

public class AdvancedJMM_12_SynchronizedAreNotFences {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_12_SynchronizedAreNotFences
     */

    /*
       ----------------------------------------------------------------------------------------------------------

        This example is superficially similar to AdvancedJMM_01_SynchronizedBarriers, but this time it shows
        that relying on the "synchronized" just for the memory effects is not reliable. Notably, the constructions
        that use no-op synchronized blocks are routinely elided by optimizers. This test produces the interesting
        result more or less reliably, by using "new Object()" as synchronization target. Choosing a different
        target may mask the interesting result, but it can reappear in real programs after aggressive optimizations.

        On x86_64 this test yields:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0    704,537,467   18.23%   Acceptable  Boring
            0, 1     51,261,212    1.33%   Acceptable  Plausible
            1, 0      2,583,316    0.07%  Interesting  Whoa
            1, 1  3,106,218,069   80.38%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = "0, 1",           expect = ACCEPTABLE,             desc = "Plausible")
    @Outcome(id = "1, 0",           expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class Synchronized {
        int x, y;

        @Actor
        public void actor1() {
            x = 1;
            synchronized (new Object()) {}
            y = 1;
        }

        @Actor
        public void actor2(II_Result r) {
            r.r1 = y;
            synchronized (new Object()) {}
            r.r2 = x;
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        If fence-like effects are required in low-level concurrency code, then Unsafe.*Fence should be used instead.

        Indeed, this provides the effect we are after, on all platforms:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0    943,174,523   40.81%  Acceptable  Boring
            0, 1     58,099,523    2.51%  Acceptable  Plausible
            1, 0              0    0.00%   Forbidden  Now forbidden
            1, 1  1,309,989,698   56.68%  Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0, 1",           expect = ACCEPTABLE, desc = "Plausible")
    @Outcome(id = "1, 0",           expect = FORBIDDEN,  desc = "Now forbidden")
    public static class Fenced {
        int x, y;

        @Actor
        public void actor1() {
            x = 1;
            UNSAFE.storeFence();
            y = 1;
        }

        @Actor
        public void actor2(II_Result r) {
            r.r1 = y;
            UNSAFE.loadFence();
            r.r2 = x;
        }
    }
}