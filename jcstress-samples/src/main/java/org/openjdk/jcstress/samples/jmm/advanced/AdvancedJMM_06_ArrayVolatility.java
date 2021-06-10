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
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class AdvancedJMM_06_ArrayVolatility {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_06_ArrayVolatility[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        It is easy to misplace volatiles when arrays are used. Notably, declaring the array itself "volatile"
        does not make the accesses to its elements "volatile". This is similar to declaring the field that store
        a reference to class "volatile": it would not translate to those fields being "volatile".

        Therefore, this test yields:

          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0  266,067,332   69.40%   Acceptable  Boring
            0, 1       31,968   <0.01%   Acceptable  Okay
            1, 0       67,654    0.02%  Interesting  Whoa
            1, 1  117,191,510   30.57%   Acceptable  Boring

        That is, the write to a[1] does not cause updates of a[0] to be visible.
     */

    @JCStressTest
    @State
    @Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = "0, 1",           expect = ACCEPTABLE,             desc = "Okay")
    @Outcome(id = "1, 0",           expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class DeclarationSite {
        volatile int[] arr = new int[2];

        @Actor
        void actor() {
            int[] a = arr;
            a[0] = 1;
            a[1] = 1;
        }

        @Actor
        void observer(II_Result r) {
            int[] a = arr;
            r.r1 = a[1];
            r.r2 = a[0];
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        VarHandles provide the possibility to perform volatile accesses by using the "volatile" access mode.
        In this example, a[1] element works as volatile guard see in BasicJMM_06_Causality primers.

        Indeed, this would be the result on all platforms:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0  308,853,172   78.55%  Acceptable  Boring
            0, 1       39,632    0.01%  Acceptable  Okay
            1, 0            0    0.00%   Forbidden  Whoa
            1, 1   84,275,580   21.43%  Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0, 1",           expect = ACCEPTABLE, desc = "Okay")
    @Outcome(id = "1, 0",           expect = FORBIDDEN,  desc = "Whoa")
    public static class UseSite {
        static final VarHandle VH = MethodHandles.arrayElementVarHandle(int[].class);

        int[] arr = new int[2];

        @Actor
        void actor() {
            int[] a = arr;
            VH.set(a, 0, 1);
            VH.setVolatile(a, 1, 1);
        }

        @Actor
        void observer(II_Result r) {
            int[] a = arr;
            r.r1 = (int) VH.getVolatile(a, 1);
            r.r2 = (int) VH.get(a, 0);
        }
    }
}