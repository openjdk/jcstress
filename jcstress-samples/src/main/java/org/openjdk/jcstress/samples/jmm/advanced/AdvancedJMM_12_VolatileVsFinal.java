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
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.*;

public class AdvancedJMM_12_VolatileVsFinal {
    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_12_VolatileVsFinal[.SubTestName]
     */

    /*
       ----------------------------------------------------------------------------------------------------------

        Perhaps, one of the most surprising JMM behaviors is that volatile fields do not include
        the final field semantics. That is, if we publish the reference to the object racily,
        then we can see the default value for the "volatile" field! This is mostly because the
        volatile itself is in the wrong place. This is similar to previous AdvancedJMM_11_WrongAcquireReleaseOrder
        example.

        It can be seen on some platforms with this synthetic test.

        For example, AArch64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  1,428,517,070   91.74%   Acceptable  Boring
               0          7,105   <0.01%  Interesting  Whoa
              42    128,534,641    8.25%   Acceptable  Boring

        Notably, some platforms go beyond the formal JMM requirement, and forbid this result.

        For example, PPC64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  2,085,381,011   75.77%   Acceptable  Boring
               0              0    0.00%  Interesting  Whoa
              42    666,849,645   24.23%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = "0",          expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class Synthetic {
        static class Holder {
            volatile int x;
            Holder() {
                x = 42;
            }
        }

        Holder h;

        @Actor
        void thread1() {
            h = new Holder();
        }

        @Actor
        void thread2(I_Result r) {
            Holder lh = h;
            if (lh != null) {
                r.r1 = lh.x;
            } else {
                r.r1 = -1;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The real life consequence of this rule is racy publication of otherwise thread-safe classes,
        like AtomicInteger.

        Again, on AArch64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  1,358,737,999   90.09%   Acceptable  Boring
               0          8,322   <0.01%  Interesting  Whoa
              42    149,426,735    9.91%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = "0",          expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class RealLife {
        AtomicInteger ai;

        @Actor
        void thread1() {
            ai = new AtomicInteger(42);
        }

        @Actor
        void thread2(I_Result r) {
            AtomicInteger lai = ai;
            if (lai != null) {
                r.r1 = lai.get();
            } else {
                r.r1 = -1;
            }
        }
    }
}