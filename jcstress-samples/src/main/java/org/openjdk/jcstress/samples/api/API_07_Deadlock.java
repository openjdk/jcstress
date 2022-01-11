/*
 * Copyright (c) 2022, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.api;

import org.openjdk.jcstress.annotations.*;

import static org.openjdk.jcstress.annotations.Expect.*;

public class API_07_Deadlock {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t API_07_Deadlock[.SubTestName]
     */

    /*
        ----------------------------------------------------------------------------------------------------------

        Some concurrency tests are naturally testing for deadlocks. This is supported by
        the special Mode.Deadlock.

        Here, we have two @Actor-s that acquire locks in the same order. JCStress would
        run both methods continuously over the same @State object, and see if these methods
        deadlock. If the test exits in reasonable time, it will record "FINISHED" result,
        otherwise it will record "STALE".

        This one, correctly synchronized one, yields no deadlocks:
            RESULT  SAMPLES     FREQ      EXPECT  DESCRIPTION
          FINISHED      280  100.00%  Acceptable  Gracefully finished.
             STALE        0    0.00%   Forbidden  Deadlock?
     */

    @JCStressTest(Mode.Deadlock)
    @Outcome(id = "FINISHED", expect = ACCEPTABLE, desc = "Gracefully finished.")
    @Outcome(id = "STALE",    expect = FORBIDDEN,  desc = "Deadlock?")
    @State
    public static class Correct {
        private final Object o1 = new Object();
        private final Object o2 = new Object();

        @Actor
        public void actor1() {
            synchronized (o1) {
                synchronized (o2) {
                    // Deliberately empty
                }
            }
        }

        @Actor
        public void actor2() {
            synchronized (o1) {
                synchronized (o2) {
                    // Deliberately empty
                }
            }
        }
    }

    /*
        ----------------------------------------------------------------------------------------------------------

        This variant, however, contains a deadlock.

        Indeed, this would be the test result:
            RESULT  SAMPLES     FREQ       EXPECT  DESCRIPTION
          FINISHED        0    0.00%   Acceptable  Gracefully finished.
             STALE       28  100.00%  Interesting  Deadlock?

          Messages:
            Have stale threads, forcing VM to exit for proper cleanup.
     */

    @JCStressTest(Mode.Deadlock)
    @Outcome(id = "FINISHED", expect = ACCEPTABLE,             desc = "Gracefully finished.")
    @Outcome(id = "STALE",    expect = ACCEPTABLE_INTERESTING, desc = "Deadlock?")
    @State
    public static class Incorrect {
        private final Object o1 = new Object();
        private final Object o2 = new Object();

        @Actor
        public void actor1() {
            synchronized (o1) {
                synchronized (o2) {
                    // Deliberately empty
                }
            }
        }

        @Actor
        public void actor2() {
            synchronized (o2) {      // Acquiring in reverse, potential deadlock
                synchronized (o1) {
                    // Deliberately empty
                }
            }
        }
    }

}
