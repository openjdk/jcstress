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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Outcome(id = {"0, 0", "1, 1", "0, 1"}, expect = ACCEPTABLE, desc = "Trivial")
@Outcome(id = {"1, 0", "1, 1"},         expect = FORBIDDEN,  desc = "Cannot happen by construction")
@State
public class RMW_08_ReleaseOnFailure {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_08_ReleaseOnFailure[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        This test naively tries to show that a failing CAS does not provide "release" semantics.
        But there are no observable results, because failing CAS does not write anything.
        As far as reader side is concerned, no writes of "g" had been published.

        x86_64, AArch64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0  161,650,705   52.25%  Acceptable  Trivial
            0, 1  147,757,039   47.75%  Acceptable  Trivial
            1, 0            0    0.00%   Forbidden  Cannot happen by construction
            1, 1            0    0.00%   Forbidden  Cannot happen by construction
     */

    private int x, g;
    public static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(RMW_08_ReleaseOnFailure.class, "g", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor
    public void actor1(II_Result r) {
        x = 1;
        // This CAS always fails: no release semantics.
        VH.compareAndSet(this, 1, 0);
    }

    @Actor
    public void actor2(II_Result r) {
        r.r1 = (int)VH.getVolatile(this);
        r.r2 = x;
    }

}
