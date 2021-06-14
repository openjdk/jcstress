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
package org.openjdk.jcstress.samples.jmm.basic;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class BasicJMM_05_Coherence {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_05_Coherence[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Yet another subtle and intuitive property comes from the naive understanding of how programs work.
        Under Java Memory Model, in absence of synchronization, the order of independent reads is undefined.
        That includes reads of the *same* variable!

          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0   14,577,607    6.96%   Acceptable  Doing both reads early.
            0, 1       24,942    0.01%   Acceptable  Doing first read early, not surprising.
            1, 0        6,376   <0.01%  Interesting  First read seen racy value early, and the second one did ...
            1, 1  194,792,419   93.02%   Acceptable  Doing both reads late.
    */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Doing first read early, not surprising.")
    @Outcome(id = "1, 0", expect = ACCEPTABLE_INTERESTING, desc = "First read seen racy value early, and the second one did not.")
    @State
    public static class SameRead {

        private final Holder h1 = new Holder();
        private final Holder h2 = h1;

        private static class Holder {
            int a;
            int trap;
        }

        @Actor
        public void actor1() {
            h1.a = 1;
        }

        @Actor
        public void actor2(II_Result r) {
            Holder h1 = this.h1;
            Holder h2 = this.h2;

            // Spam null-pointer check folding: try to step on NPEs early.
            // Doing this early frees compiler from moving h1.a and h2.a loads
            // around, because it would not have to maintain exception order anymore.
            h1.trap = 0;
            h2.trap = 0;

            // Spam alias analysis: the code effectively reads the same field twice,
            // but compiler does not know (h1 == h2) (i.e. does not check it, as
            // this is not a profitable opt for real code), so it issues two independent
            // loads.
            r.r1 = h1.a;
            r.r2 = h2.a;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The stronger property -- coherence -- mandates that the writes to the same variable to be observed in
        a total order (that implies that _observers_ are also ordered). Java "volatile" assumes this property.

          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0  114,696,597   30.95%  Acceptable  Doing both reads early.
            0, 1    2,126,717    0.57%  Acceptable  Doing first read early, not surprising.
            1, 0            0    0.00%   Forbidden  Violates coherence.
            1, 1  253,704,430   68.47%  Acceptable  Doing both reads late.
     */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Doing first read early, not surprising.")
    @Outcome(id = "1, 0", expect = FORBIDDEN, desc = "Violates coherence.")
    @State
    public static class SameVolatileRead {

        private final Holder h1 = new Holder();
        private final Holder h2 = h1;

        private static class Holder {
            volatile int a;
            int trap;
        }

        @Actor
        public void actor1() {
            h1.a = 1;
        }

        @Actor
        public void actor2(II_Result r) {
            Holder h1 = this.h1;
            Holder h2 = this.h2;

            h1.trap = 0;
            h2.trap = 0;

            r.r1 = h1.a;
            r.r2 = h2.a;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        VarHandles "opaque" mode also provide coherency.

          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0   22,265,880    6.07%  Acceptable  Doing both reads early.
            0, 1      147,500    0.04%  Acceptable  Doing first read early, not surprising.
            1, 0            0    0.00%   Forbidden  Violates coherence.
            1, 1  344,427,964   93.89%  Acceptable  Doing both reads late.
     */

    @JCStressTest
    @Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
    @Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Doing first read early, not surprising.")
    @Outcome(id = "1, 0", expect = FORBIDDEN, desc = "Violates coherence.")
    @State
    public static class SameOpaqueRead {

        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(Holder.class, "a", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private final Holder h1 = new Holder();
        private final Holder h2 = h1;

        private static class Holder {
            int a;
            int trap;
        }

        @Actor
        public void actor1() {
            VH.setOpaque(h1, 1);
        }

        @Actor
        public void actor2(II_Result r) {
            Holder h1 = this.h1;
            Holder h2 = this.h2;

            h1.trap = 0;
            h2.trap = 0;

            r.r1 = (int) VH.getOpaque(h1);
            r.r2 = (int) VH.getOpaque(h2);
        }
    }

}
