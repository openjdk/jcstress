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
import org.openjdk.jcstress.infra.results.IIII_Result;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = {"1, 2, 2, 1", "2, 1, 1, 2"}, expect = FORBIDDEN,  desc = "Violates coherence.")
@Outcome(                                   expect = ACCEPTABLE, desc = "Every other result is ignored.")
@State
public class AdvancedJMM_03_NonMCA_Coherence {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_03_NonMCA_Coherence[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

         The AdvancedJMM_02_MultiCopyAtomic example shows that writes that from several processors
         can be seen by different processors in different orders, on the machines that exhibit no multi-copy
         atomicity (non-MCA platforms). However, this only manifests on *different* memory locations.

         With a single memory location, coherence (see BasicJMM_05_Coherence) still holds: there is
         a total order of writes to a single location. To demonstrate this, we can rewrite AdvancedJMM_02_MultiCopyAtomic
         for a single variable, and then even the non-MCA platforms would not show incoherent results.

         PPC64 (some less likely Acceptable results pruned):
              RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          0, 0, 0, 0  144,731,042    8.70%  Acceptable  Every other result is ignored.
          0, 0, 1, 1  110,292,857    6.63%  Acceptable  Every other result is ignored.
          0, 0, 2, 2  109,134,556    6.56%  Acceptable  Every other result is ignored.
          1, 1, 0, 0  107,110,080    6.44%  Acceptable  Every other result is ignored.
          1, 1, 1, 1  360,030,567   21.65%  Acceptable  Every other result is ignored.
          1, 1, 2, 2  171,924,770   10.34%  Acceptable  Every other result is ignored.
          1, 2, 2, 1            0    0.00%   Forbidden  Violates coherence.
          2, 1, 1, 2            0    0.00%   Forbidden  Violates coherence.
          2, 2, 0, 0  107,366,602    6.46%  Acceptable  Every other result is ignored.
          2, 2, 1, 1  172,793,130   10.39%  Acceptable  Every other result is ignored.
          2, 2, 2, 2  370,602,534   22.29%  Acceptable  Every other result is ignored.
     */

    static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(AdvancedJMM_03_NonMCA_Coherence.class, "x", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    int x;

    @Actor
    public void actor1() {
        VH.setOpaque(this, 1);
    }

    @Actor
    public void actor2() {
        VH.setOpaque(this, 2);
    }

    @Actor
    public void actor3(IIII_Result r) {
        r.r1 = (int) VH.getOpaque(this);
        r.r2 = (int) VH.getOpaque(this);
    }

    @Actor
    public void actor4(IIII_Result r) {
        r.r3 = (int) VH.getOpaque(this);
        r.r4 = (int) VH.getOpaque(this);
    }

}
