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
package org.openjdk.jcstress.samples.primitives.rmw;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class RMW_12_FailureWitnessLoops {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_12_FailureWitnessLoops[.SubTestName]
     */

    abstract static class Base {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(Base.class, "v", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        int v = 1;
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Failure witness can also be used to get the fresh value in update loops. First, consider
        the "normal" loop: read the latest value, prepare the update, CAS it, check if successful.
        This is the usual way to do the CAS-update-loop.

        On x86_64:
           RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          2, 4, 4  2,602,060,422   50.34%  Acceptable  Trivial
          4, 2, 4  2,566,882,682   49.66%  Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = {"2, 4, 4", "4, 2, 4"}, expect = ACCEPTABLE, desc = "Trivial")
    @Outcome(                             expect = FORBIDDEN,  desc = "Cannot happen")
    @State
    public static class NormalLoop extends Base {
        private int update() {
            while (true) {
                int val = (int) VH.get(this);
                int newVal = val * 2;
                if (VH.compareAndSet(this, val, newVal)) {
                    return newVal;
                }
            }
        }

        @Actor
        public void actor1(III_Result r) {
            r.r1 = update();
        }

        @Actor
        public void actor2(III_Result r) {
            r.r2 = update();
        }

        @Arbiter
        public void check(III_Result r) {
            r.r3 = (int) VH.get(this);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        With failure witness, we can use it as the "latest" value for retry, thus avoiding the
        reload in the hot loop.

        Note that the actual performance really depends on the hardware implementation,
        the contention in the loop, scheduling lags, etc. Do not assume that sparing a read
        would have an universally positive impact. See for example:
            https://bugs.openjdk.java.net/browse/JDK-8141640

        On x86_64:
           RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          2, 4, 4  2,484,075,481   47.92%  Acceptable  Trivial
          4, 2, 4  2,699,879,463   52.08%  Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = {"2, 4, 4", "4, 2, 4"}, expect = ACCEPTABLE, desc = "Trivial")
    @Outcome(                             expect = FORBIDDEN,  desc = "Cannot happen")
    @State
    public static class WitnessLoop extends Base {
        private int update() {
            int val = (int) VH.get(this);
            while (true) {
                int newVal = val*2;
                int witness = (int) VH.compareAndExchange(this, val, newVal);
                if (val == witness) {
                    return newVal;
                }
                val = witness;
            }
        }

        @Actor
        public void actor1(III_Result r) {
            r.r1 = update();
        }

        @Actor
        public void actor2(III_Result r) {
            r.r2 = update();
        }

        @Arbiter
        public void check(III_Result r) {
            r.r3 = (int) VH.get(this);
        }
    }

}
