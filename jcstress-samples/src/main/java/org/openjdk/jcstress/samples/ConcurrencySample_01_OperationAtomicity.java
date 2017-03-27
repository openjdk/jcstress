/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jcstress.samples;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.*;

public class ConcurrencySample_01_OperationAtomicity {

    /*
      ----------------------------------------------------------------------------------------------------------

        This test demonstrates the operation atomicity tests. First, the naive
        test that tests if plain increment is indivisible or not. It is not, as
        jcstress would tell on just about any platform.

              [OK] org.openjdk.jcstress.samples.ConcurrencySample_01_OperationAtomicity.PlainIncrement
            (JVM args: [-server])
          Observed state   Occurrences              Expectation  Interpretation
                       1     4,090,172   ACCEPTABLE_INTERESTING  One update lost.
                       2   200,723,108               ACCEPTABLE  Both updates.
    */

    @JCStressTest
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost.")
    @Outcome(id = "2", expect = ACCEPTABLE,  desc = "Both updates.")
    @State
    public static class PlainIncrement {
        int v;

        @Actor
        public void actor1() {
            v++;
        }

        @Actor
        public void actor2() {
            v++;
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

         Volatile increment is not atomic either. The mere modifier cannot resolve
         the problem with having distinct read and write operations, that are not
         atomic together.

              [OK] org.openjdk.jcstress.samples.ConcurrencySample_01_OperationAtomicity.VolatileIncrement
            (JVM args: [-server])
          Observed state   Occurrences              Expectation  Interpretation
                       1        25,641   ACCEPTABLE_INTERESTING  One update lost.
                       2   116,446,539               ACCEPTABLE  Both updates.
     */

    @JCStressTest
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Both updates.")
    @State
    public static class VolatileIncrement {
        volatile int v;

        @Actor
        public void actor1() {
            v++;
        }

        @Actor
        public void actor2() {
            v++;
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = v;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        AtomicInteger.incrementAndGet() is atomic.

              [OK] org.openjdk.jcstress.samples.ConcurrencySample_01_OperationAtomicity.AtomicIncrement
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                       1             0     FORBIDDEN  One update lost.
                       2   168,640,200    ACCEPTABLE  Both updates.
     */

    @JCStressTest
    @Outcome(id = "1", expect = FORBIDDEN,  desc = "One update lost.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Both updates.")
    @State
    public static class AtomicIncrement {
        AtomicInteger ai = new AtomicInteger();

        @Actor
        public void actor1() {
            ai.incrementAndGet();
        }

        @Actor
        public void actor2() {
            ai.incrementAndGet();
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = ai.get();
        }
    }

}
