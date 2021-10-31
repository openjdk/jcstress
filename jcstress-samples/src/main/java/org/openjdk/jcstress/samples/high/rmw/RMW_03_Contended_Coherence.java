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
public class RMW_03_Contended_Coherence {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_03_Contended_Coherence[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        ....
     */

    private int v;
    public static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(RMW_03_Contended_Coherence.class, "v", int.class);
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
