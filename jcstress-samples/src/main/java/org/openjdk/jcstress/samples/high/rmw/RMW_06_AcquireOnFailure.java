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
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = {"0, 0", "1, 1", "0, 1"}, expect = ACCEPTABLE, desc = "Trivial")
@Outcome(id = "1, 0",                   expect = FORBIDDEN,  desc = "Cannot happen")
@State
public class RMW_06_AcquireOnFailure {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_06_AcquireOnFailure[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This test shows that even a failing CAS provides the "acquire" semantics:
        it still observes the value regardless of the subsequent CAS result.

        x86_64, AArch64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0  146,825,939   44.46%  Acceptable  Trivial
            0, 1    4,112,904    1.25%  Acceptable  Trivial
            1, 0            0    0.00%   Forbidden  Cannot happen
            1, 1  179,276,581   54.29%  Acceptable  Trivial
     */

    private int x, g;
    public static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(RMW_06_AcquireOnFailure.class, "g", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor
    public void actor1(II_Result r) {
        x = 1;
        VH.setVolatile(this, 1);
    }

    @Actor
    public void actor2(II_Result r) {
        // This CAS fails when it observes "1".
        // Ternary operator converts that failure to "1" explicitly.
        r.r1 = VH.compareAndSet(this, 0, 1) ? 0 : 1;
        r.r2 = x;
    }

}
