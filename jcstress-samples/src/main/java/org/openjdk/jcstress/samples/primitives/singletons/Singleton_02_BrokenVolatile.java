/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
package org.openjdk.jcstress.samples.primitives.singletons;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;
import org.openjdk.jcstress.samples.primitives.singletons.shared.*;

import java.util.function.Supplier;

public class Singleton_02_BrokenVolatile {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_02
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        The "obvious" solution for synchronization is to add volatile to instance field.
     */

    public static class VolatileS<T> implements Factory<T> {
        private volatile T instance;

        @Override
        public T get(Supplier<T> supplier) {
            if (instance == null) {
                instance = supplier.get();
            }
            return instance;
        }
    }

    /*
        Sadly, volatile still does not solve the interleaving problem: the separate reads and writes of
        volatile variables are not atomic in combination.

        AArch64, x86_64:
                    RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              data1, data1  1,513,077,834   53.05%   Acceptable  Trivial.
              data1, data2    305,328,400   10.71%  Interesting  Race condition.
              data2, data2  1,033,548,910   36.24%   Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition.")
    public static class Final {
        VolatileS<Singleton> factory = new VolatileS<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        What *does* volatile solve, however, is the safe publication of the singleton instance.
        Even though there is a race condition that can install different versions of singletons,
        every thread is guaranteed to see the singleton contents, even if its contents are not final.
        This is going to be a building block for so called double-checked locking implementation later.

        This is similar to BasicJMM_06_Causality example.

        AArch64:
                   RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
             data1, data1  1,240,790,356   54.77%   Acceptable  Trivial.
             data1, data2    234,875,383   10.37%  Interesting  Race condition.
             data2, data2    789,721,725   34.86%   Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition.")
    public static class NonFinal {
        VolatileS<Singleton> factory = new VolatileS<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }


}
