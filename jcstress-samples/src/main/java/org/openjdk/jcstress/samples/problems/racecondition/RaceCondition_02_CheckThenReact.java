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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.openjdk.jcstress.annotations.Expect.*;

public class RaceCondition_02_CheckThenReact {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RaceCondition_02_CheckThenReact[.SubTestName]
    */

    /*
      ----------------------------------------------------------------------------------------------------------

         This sample demonstrates how a check-the-react code is broken under concurrent updates.
         Indeed, both threads can enter the branch. Either would try to set the flag too late.

         On x86_64:
                        RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          false, true  431,678,316   43.12%   Acceptable  Only one actors entered the section
          true, false  445,639,900   44.52%   Acceptable  Only one actors entered the section
           true, true  123,760,568   12.36%  Interesting  Conflict: both actors entered the section
     */

    @JCStressTest
    @Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Only one actors entered the section")
    @Outcome(id = {"true, true"}, expect = ACCEPTABLE_INTERESTING, desc = "Conflict: both actors entered the section")
    @State
    public static class Racy {
        private volatile boolean flag = true;

        boolean checkThenReact() {
            if (flag) {
                flag = false;
                return true;
            } else {
                return false;
            }
        }

        @Actor
        public void actor1(ZZ_Result r) {
            r.r1 = checkThenReact();
        }

        @Actor
        public void actor2(ZZ_Result r) {
            r.r2 = checkThenReact();
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

         The simple way out of this race condition is synchronization. Indeed, wrapping the
         code in "synchronized (this)" precludes the race, and only one actor can enter at once.

         Note that with a synchronization like that, we don't need "volatile".

         On x86_64:
                        RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          false, true  385,791,982   48.86%  Acceptable  Only one actors entered the section
          true, false  403,738,642   51.14%  Acceptable  Only one actors entered the section
           true, true            0    0.00%   Forbidden  Conflict: both actors entered the section
     */

    @JCStressTest
    @Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Only one actors entered the section")
    @Outcome(id = {"true, true"},                 expect = FORBIDDEN, desc = "Conflict: both actors entered the section")
    @State
    public static class Sync {
        private boolean flag = true;

        boolean checkThenReact() {
            synchronized (this) {
                if (flag) {
                    flag = false;
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Actor
        public void actor1(ZZ_Result r) {
            r.r1 = checkThenReact();
        }

        @Actor
        public void actor2(ZZ_Result r) {
            r.r2 = checkThenReact();
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

         Similarly, we can replace check-and-set sequence with a single _atomic_ operation.
         This would resolve the race condition as well.

         On x86_64:
               RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          false, true  420,625,392   49.72%  Acceptable  Only one actors entered the section
          true, false  425,378,832   50.28%  Acceptable  Only one actors entered the section
           true, true            0    0.00%   Forbidden  Conflict: both actors entered the section
     */

    @JCStressTest
    @Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Only one actors entered the section")
    @Outcome(id = {"true, true"},                 expect = FORBIDDEN, desc = "Conflict: both actors entered the section")
    @State
    public static class Atomic {
        private final AtomicBoolean flag = new AtomicBoolean(true);

        boolean checkThenReact() {
            if (flag.compareAndSet(true, false)) {
                return true;
            } else {
                return false;
            }
        }

        @Actor
        public void actor1(ZZ_Result r) {
            r.r1 = checkThenReact();
        }

        @Actor
        public void actor2(ZZ_Result r) {
            r.r2 = checkThenReact();
        }
    }
}
