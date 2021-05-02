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

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@State
@Outcome(id = "10",                      expect = ACCEPTABLE,             desc = "Boring")
@Outcome(id = {"0", "1"},                expect = FORBIDDEN,              desc = "Boring")
@Outcome(id = {"9", "8", "7", "6", "5"}, expect = ACCEPTABLE,             desc = "Okay")
@Outcome(                                expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
public class AdvancedJMM_03_LosingUpdates {

     /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_03_LosingUpdates[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This is a simple example, but it nevertheless important to see how dangerous race conditions are.
        We have already established that v++ over volatile field is not atomic. But here is a more interesting
        question: how many updates we can actually lose? Perhaps naively, many would answer that we could lose
        one update per iteration. After all, the actors always see the latest value in the "v", so we might see
        at most the old-just-before-update value. So, if we do 5 updates in every thread, we could expect that
        we would see at least 5 as the final result?

        This intuition is contradicted by this simple test:

          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              10  4,314,297,607   25.10%   Acceptable  Boring
               2     19,121,833    0.11%  Interesting  Whoa
               3     65,889,475    0.38%  Interesting  Whoa
               4    167,420,625    0.97%  Interesting  Whoa
               5  2,682,284,766   15.61%   Acceptable  Okay
               6  2,033,772,928   11.83%   Acceptable  Okay
               7  2,523,123,422   14.68%   Acceptable  Okay
               8  2,847,721,682   16.57%   Acceptable  Okay
               9  2,533,299,886   14.74%   Acceptable  Okay

        The most interesting result, "2" can be explaned by this interleaving:
            Thread 1: (0 ------ stalled -------> 1)     (1->2)(2->3)(3->4)(4->5)
            Thread 2:   (0->1)(1->2)(2->3)(3->4)    (1 -------- stalled ---------> 2)

        This example shows that non-synchronized counter can lose the arbitrary number of
        updates, and even revert the history!

        Exercise for the reader: prove that both "0" and "1" are impossible results.
     */

    volatile int x;

    @Actor
    void actor1() {
        for (int i = 0; i < 5; i++) {
            x++;
        }
    }

    @Actor
    void actor2() {
        for (int i = 0; i < 5; i++) {
            x++;
        }
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}