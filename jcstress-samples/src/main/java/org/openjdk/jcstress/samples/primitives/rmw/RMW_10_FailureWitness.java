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
import org.openjdk.jcstress.infra.results.ZI_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class RMW_10_FailureWitness {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_10_FailureWitness[.SubTestName]
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

        int v;
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        This example shows one of the cases where having a "failure witness" for the failing CAS
        operation is useful. Failure witness is the memory value that CAS had failed on -- the one
        that was not expected by the CAS.

        Re-reading the memory value "right after" the CAS update can show oddities: it could
        show the interfering updated that now matches the expected value from the CAS! This would
        look like a spurious failure in CAS, except that it is allowed by a simple interleaving.

        In this example, a CAS that expected "0" failed, and yet there is "0" in memory.

        On x86_64:
            RESULT         SAMPLES     FREQ       EXPECT  DESCRIPTION
          false, 0      65,410,888    0.13%  Interesting  Whoa
          false, 1  24,486,524,897   47.97%   Acceptable  Trivial
           true, 0      25,634,745    0.05%   Acceptable  Trivial
           true, 1  26,473,074,974   51.86%   Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = "false, 0",                         expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    @Outcome(id = {"false, 1", "true, 0", "true, 1"}, expect = ACCEPTABLE, desc = "Trivial")
    @State
    public static class BooleanCAS extends Base {
        @Actor
        public void actor1(ZI_Result r) {
            r.r1 = VH.compareAndSet(this, 0, 1);
            r.r2 = (int) VH.get(this);
        }

        @Actor
        public void actor2() {
            VH.set(this, 0);
        }

        @Actor
        public void actor3() {
            VH.set(this, 1);
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        With witness, we clearly know what was the memory value CAS had failed on, so the
        interesting outcome is now forbidden.

        On x86_64:
            RESULT         SAMPLES     FREQ      EXPECT  DESCRIPTION
          false, 0               0    0.00%   Forbidden  Cannot happen
          false, 1  26,892,302,582   49.43%  Acceptable  Trivial
           true, 0  27,516,182,282   50.57%  Acceptable  Trivial
           true, 1               0    0.00%  Acceptable  Trivial
     */

    @JCStressTest
    @Outcome(id = "false, 0",                         expect = FORBIDDEN,  desc = "Cannot happen")
    @Outcome(id = {"false, 1", "true, 0", "true, 1"}, expect = ACCEPTABLE, desc = "Trivial")
    @State
    public static class WitnessCAS extends Base {
        @Actor
        public void actor1(ZI_Result r) {
            int witness = (int) VH.compareAndExchange(this, 0, 1);
            r.r1 = (witness == 0);
            r.r2 = witness;
        }

        @Actor
        public void actor2() {
            VH.set(this, 0);
        }

        @Actor
        public void actor3() {
            VH.set(this, 1);
        }
    }

}
