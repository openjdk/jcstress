/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.problems.racecondition;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

public class RaceCondition_01_RMW {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RaceCondition_01_RMW[.SubTestName]
    */

    /*
      ----------------------------------------------------------------------------------------------------------

         This sample demonstrates how a read-modify-write sequence can lead to surprising results.
         While operations are over "volatile" field, they still are not atomic, and can conflict
         with each other, producing the permanently broken result.

         On x86_64:
                 RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          150, 100, 150  494,050,015   45.48%   Acceptable  actor2 completed first, then actor1 completed
          250, 100, 100   61,924,095    5.70%  Interesting  actors conflicted, actor2 won the race
          250, 100, 250   59,970,298    5.52%  Interesting  actors conflicted, actor1 won the race
          250, 150, 150  470,474,536   43.31%   Acceptable  actor1 completed first, then actor2 completed
     */

    @JCStressTest
    @Outcome(id = {"150, 100, 150"}, expect = ACCEPTABLE, desc = "actor2 completed first, then actor1 completed")
    @Outcome(id = {"250, 150, 150"}, expect = ACCEPTABLE, desc = "actor1 completed first, then actor2 completed")
    @Outcome(id = {"250, 100, 250"}, expect = ACCEPTABLE_INTERESTING, desc = "actors conflicted, actor1 won the race")
    @Outcome(id = {"250, 100, 100"}, expect = ACCEPTABLE_INTERESTING, desc = "actors conflicted, actor2 won the race")
    @State
    public static class Racy {
        private volatile int v = 200;

        @Actor
        public void actor1(III_Result r) {
            int t1 = v;
            t1 += 50;
            v = t1;

            r.r1 = t1;
        }

        @Actor
        public void actor2(III_Result r) {
            int t2 = v;
            t2 -= 100;
            v = t2;

            r.r2 = t2;
        }

        @Arbiter
        public void arbiter(III_Result r) {
            r.r3 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

         One way out of this race condition is synchronization. Indeed, wrapping critical segments
         in synchronized(this) precludes their concurrent execution, and the previously interesting
         results are now forbidden.

         Note that with a synchronization like that, we don't need "volatile".

         On x86_64:
                 RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          150, 100, 150  361,728,228   47.45%  Acceptable  actor2 completed first, then actor1 completed
          250, 100, 100            0    0.00%   Forbidden  Cannot happen
          250, 100, 250            0    0.00%   Forbidden  Cannot happen
          250, 150, 150  400,656,156   52.55%  Acceptable  actor1 completed first, then actor2 completed
     */

    @JCStressTest
    @Outcome(id = {"150, 100, 150"}, expect = ACCEPTABLE, desc = "actor2 completed first, then actor1 completed")
    @Outcome(id = {"250, 150, 150"}, expect = ACCEPTABLE, desc = "actor1 completed first, then actor2 completed")
    @Outcome(id = {"250, 100, 250"}, expect = FORBIDDEN, desc = "Cannot happen")
    @Outcome(id = {"250, 100, 100"}, expect = FORBIDDEN, desc = "Cannot happen")
    @State
    public static class Sync {
        private int v = 200;

        @Actor
        public void actor1(III_Result r) {
            int t1;
            synchronized (this) {
                t1 = v;
                t1 += 50;
                v = t1;
            }

            r.r1 = t1;
        }

        @Actor
        public void actor2(III_Result r) {
            int t2;
            synchronized (this) {
                t2 = v;
                t2 -= 100;
                v = t2;
            }
            r.r2 = t2;
        }

        @Arbiter
        public void arbiter(III_Result r) {
            r.r3 = v;
        }
    }

}
