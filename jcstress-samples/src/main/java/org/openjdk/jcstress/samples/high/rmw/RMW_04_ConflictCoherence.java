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
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Trivial")
@Outcome(id = "false, false",                 expect = ACCEPTABLE, desc = "Not even once")
@Outcome(id = "true, true",                   expect = FORBIDDEN,  desc = "More than once")
@State
public class RMW_04_ConflictCoherence {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_04_ConflictCoherence[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Another example elaborates on conflict behavior.

        This example shows that even with the weakest form of RMW (CAS), without any memory ordering
        whatsoever, we are still covered by coherence (see BasicJMM_05_Coherence example). That is,
        a conflicting RMW operation cannot see the "stale" value and succeed to update the location
        the second time.

        In this example, even though the store of "1" happens concurrently with both CASes,
        only one CAS is allowed to succeed. This test specifically uses the weakest non-barrier
        form of CAS to point that it is the property of the accesses themselves, not their
        implied memory ordering.

        Indeed, on all platforms (x86_64, AArch64, PPC64), this would happen:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          false, false    672,734,373   15.43%  Acceptable  Not even once
           false, true  1,757,321,910   40.31%  Acceptable  Trivial
           true, false  1,929,492,645   44.26%  Acceptable  Trivial
            true, true              0    0.00%   Forbidden  More than once

        Both CASes can fail (they are weak, and the store to "1" might not happen yet), but
        they never succeed at the same time.
     */

    private int v;
    public static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(RMW_04_ConflictCoherence.class, "v", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor
    public void actor1() {
        VH.set(this, 1);
    }

    @Actor
    public void actor2(ZZ_Result r) {
        r.r1 = VH.weakCompareAndSetPlain(this, 1, 2);
    }

    @Actor
    public void actor3(ZZ_Result r) {
        r.r2 = VH.weakCompareAndSetPlain(this, 1, 3);
    }

}
