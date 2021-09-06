/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.high.rmw;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.ZZ_Result;
import org.openjdk.jcstress.util.UnsafeHolder;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;


public class RMW_08_GAS_Effects {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_08_AtomicityEffects[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This test construct a rather complicated example when the failing CAS semantics
        matters a bit, and why a stronger primitives might be needed. Since failing RMW
        operations do not produce observable writes, the tests are complicated, and have
        to test the memory semantics in a round-about way.

        We shall build up the test case gradually. First, a very basic test.

        This test produces (0, 0), and the justifying execution is:

             w(x, 1) --po/hb--> r(y):0
                                  |
                                  |  so (not sw)
                                  v
                                r(y):0 --po/hb--> w(y, 1) --po/hb --> r(x):0

        In other words, this reads "x" through a race, and "y" has not been set yet.

        Indeed, this is clearly visible on AArch64:
          RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0       4,930    0.02%  Interesting  Interesting
            0, 1  12,429,506   58.06%   Acceptable  Trivial
            1, 0   7,416,292   34.64%   Acceptable  Trivial
            1, 1   1,559,064    7.28%   Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Trivial")
    @Outcome(id = "0, 0",                   expect = ACCEPTABLE_INTERESTING, desc = "Interesting")
    @State
    public static class CTS_CTS {
        private int x;
        private volatile int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            int t = y;
            if (t == 1) {
                y = 0;
            }
            r.r1 = t;
        }

        @Actor
        public void actor2(II_Result r) {
            int t = y;
            if (t == 0) {
                y = 1;
            }
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Replacing both non-atomic checks with CAS yields the example that still produces
        (0, 0). The justifying execution for that outcome is:

             w(x, 1) --po/hb--> [ r(y):0; nothing happens ]
                                         |
                                         |  so (not sw)
                                         v
                                [ r(y):0; w(y, 1) ] --po/hb --> r(x):0

        ...where CAS actions are "indivisible" in "[ ]".

        The fact these are atomic CASes changes nothing (yet): there is no store,
        and therefore no memory semantics can be assumed.

        Indeed, this still happens on AArch64:
          RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0         868   <0.01%  Interesting  Interesting
            0, 1  13,122,195   63.29%   Acceptable  Trivial
            1, 0   6,171,450   29.76%   Acceptable  Trivial
            1, 1   1,439,439    6.94%   Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Trivial")
    @Outcome(id = "0, 0",                   expect = ACCEPTABLE_INTERESTING, desc = "Interesting")
    @State
    public static class CAS_CAS {
        public static final VarHandle VH_Y;

        static {
            try {
                VH_Y = MethodHandles.lookup().findVarHandle(CAS_CAS.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private int x;
        private volatile int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = VH_Y.compareAndSet(this, 1, 0) ? 1 : 0;
        }

        @Actor
        public void actor2(II_Result r) {
            VH_Y.compareAndSet(this, 0, 1);
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Doing the store to provide the release on one side still produces (0, 0),
        and the justifying execution is:

        w(x,1) --po/hb--> r(y):0 --po/hb--> w(y,0)
                            |                 ^
                            | so              | so
                            v                 |
                        [ r(y):0     ;     w(y,1) ] --po/hb--> r(x):0

        It is similar as before, and the fact there is an unconditional volatile write
        changes nothing (yet).

        Note that the order over "y" is still linearizable, as required for synchronization
        actions: r(y):0 --> r(y):0 --> w(y,1) --> w(y, 0).

        Indeed, this is still possible on AArch64:
          RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0       2,087   <0.01%  Interesting  Interesting
            0, 1  11,400,418   52.20%   Acceptable  Trivial
            1, 0   9,820,853   44.97%   Acceptable  Trivial
            1, 1     616,514    2.82%   Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Trivial")
    @Outcome(id = "0, 0",                   expect = ACCEPTABLE_INTERESTING, desc = "Interesting")
    @State
    public static class GTS_CAS {
        public static final VarHandle VH_Y;

        static {
            try {
                VH_Y = MethodHandles.lookup().findVarHandle(GTS_CAS.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private int x;
        private volatile int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            int t = y;
            y = 0;
            r.r1 = t;
        }

        @Actor
        public void actor2(II_Result r) {
            VH_Y.compareAndSet(this, 0, 1);
            r.r2 = x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Now to the final test. This test cannot produce (0, 0), because it uses a much stronger
        primitive: Get-And-Set (GAS).

        To reason whether we can produce (0, 0), we basically need to fill in the blanks
        in between the actions in GAS and CAS:

        w(x,1) --po/hb--> [ r(y):0 ; w(y,0) ]

                               ????????

                          [ r(y):0 ; w(y,1) ] --po/hb--> r(x):0

        The important thing is that both are *atomic*, which means we cannot split the
        first read-write pair like in the example before. Which means this execution
        is no longer valid:

        w(x,1) --po/hb--> [ r(y):0 ; w(y,0) ]
                              |         ^
                              | so      | so
                              v         |
                          [ r(y):0 ; w(y,1) ] --po/hb--> r(x):0

        The two valid executions are where both atomic groups are sequenced one after another.
        There are two such executions, and both executions are invalid.

        This execution is invalid, because r(y):0 should have observed w(y,1), which
        fails synchronization order consistency.

        w(x,1) --po/hb--> [ r(y):0 ; w(y,0) ]
                              ^
                              \---------\
                                        |
                          [ r(y):0 ; w(y,1) ] --po/hb--> r(x):0

        This execution is invalid, because r(y) observes w(y), which means there
        is a synchronizes-with between them, which hooks w(x) and r(x), which
        fails happens-before consistency: r(x) should see 1.

        w(x,1) --po/hb--> [ r(y):0 ; w(y,0) ]
                                        |
                              /--sw/hb--/
                              v
                          [ r(y):0 ; w(y,1) ] --po/hb--> r(x):0

        In the end, there is no execution that justifies (0, 0).

        Note that it is an effect of all three:
          - GAS is being atomic;
          - GAS is carrying "release" semantics;
          - GAS is performing the unconditional store is detectable by CAS;

        Previous examples show how failing any of these prerequisites exposes (0, 0).

        Indeed, this does not happen on AArch64 anymore:
          RESULT    SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0          0    0.00%   Forbidden  Nope
            0, 1  9,899,632   53.12%  Acceptable  Trivial
            1, 0  7,369,069   39.54%  Acceptable  Trivial
            1, 1  1,366,051    7.33%  Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial")
    @Outcome(id = "0, 0",                   expect = FORBIDDEN,  desc = "Nope")
    @State
    public static class GAS_CAS {
        public static final VarHandle VH_Y;

        static {
            try {
                VH_Y = MethodHandles.lookup().findVarHandle(GAS_CAS.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private int x;
        private volatile int y;

        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = (int) VH_Y.getAndSet(this, 0);
        }

        @Actor
        public void actor2(II_Result r) {
            VH_Y.compareAndSet(this, 0, 1);
            r.r2 = x;
        }
    }

}
