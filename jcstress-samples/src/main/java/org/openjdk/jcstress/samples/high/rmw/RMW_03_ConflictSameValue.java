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
import org.openjdk.jcstress.infra.results.Z_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class RMW_03_ConflictSameValue {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_03_ConflictSameValue[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This test explores the behaviors of atomic RMW instructions.

        This shows the important caveat about the notion of conflict. Even if there is an intervening
        write to the same variable _that keeps the value the same_, the CAS is still guaranteed
        to succeed.
     */

    @State
    public static class S {
        private int v;
        public static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(S.class, "v", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The strong CAS should always succeed, even though there is a write to the same
        variable. Since that write brings the value still expected by the CAS, it would
        succeed.

        Indeed, on both x86_64 and AArch64 this is the result:

          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
           false            0    0.00%   Forbidden  Cannot happen
            true  515,528,704  100.00%  Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = "true",  expect = ACCEPTABLE, desc = "Trivial")
    @Outcome(id = "false", expect = FORBIDDEN,  desc = "Cannot happen")
    public static class Strong {
        @Actor
        public void actor1(S s, Z_Result r) {
            r.r1 = S.VH.compareAndSet(s, 0, 1);
        }

        @Actor
        public void actor2(S s) {
            S.VH.setVolatile(s, 0);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The weak CAS can spuriously fail, even without any conflict. But with the conflict like
        this, the chances that a weak CAS would fail are much greater. Compare with the failure
        frequency in RMW_02_ContendedSuccess.

        It would not manifest on x86_64:

          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
           false            0    0.00%  Interesting  Spurious failures are allowed
            true  430,516,224  100.00%   Acceptable  Trivial

        But it would manifest on AArch64 -XX:-UseLSE:

          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
           false    1,468,383    0.38%  Interesting  Spurious failures are allowed
            true  386,877,473   99.62%   Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = "true",  expect = ACCEPTABLE,             desc = "Trivial")
    @Outcome(id = "false", expect = ACCEPTABLE_INTERESTING, desc = "Spurious failures are allowed")
    public static class Weak {
        @Actor
        public void actor1(S s, Z_Result r) {
            r.r1 = S.VH.weakCompareAndSet(s, 0, 1);
        }

        @Actor
        public void actor2(S s) {
            S.VH.setVolatile(s, 0);
        }
    }

}
