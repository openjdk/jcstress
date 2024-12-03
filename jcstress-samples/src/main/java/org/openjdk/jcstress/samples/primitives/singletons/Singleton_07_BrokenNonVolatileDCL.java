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

public class Singleton_07_BrokenNonVolatileDCL {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_07
     */

    /*
        ----------------------------------------------------------------------------------------------------------

        A very common antipattern is omitting `volatile` from the instance field. The reason why it is broken
        should be obvious after looking at previous examples, this breaks safe publication guarantees, as the
        read of instance on fast path has no memory ordering at all.
     */

    public static class NonVolatileDCL<T> implements Factory<T> {
        private T instance; // specifically non-volatile

        @Override
        public T get(Supplier<T> supplier) {
            T res = instance;
            if (res != null) {
                return res;
            }

            synchronized (this) {
                if (instance == null) {
                    instance = supplier.get();
                }
                return instance;
            }
        }
    }

    /*
        Now, here is a peculiarity: if the object we are constructing is able to survive the races
        on its own, even this code would work well on all architectures. There would still be a race
        on `instance` field, but it would be benign. (See also BasicJMM_09_BenignRaces example).

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,251,305,922   64.43%  Acceptable  Trivial.
          data2, data2    690,896,902   35.57%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        NonVolatileDCL<Singleton> factory = new NonVolatileDCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        The failures show up readily when the object is not surviving the races on its own. In this case,
        we can see the non-null singleton, which appears to carry null data!

        AArch64:
                    RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              data1, data1  1,470,891,960   63.17%   Acceptable  Trivial.
          data1, null-data      1,124,329    0.05%  Interesting  Data races.
              data2, data2    855,805,003   36.75%   Acceptable  Trivial.
          null-data, data2        665,052    0.03%  Interesting  Data races.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, null-data", "null-data, data2"}, expect = Expect.ACCEPTABLE_INTERESTING, desc = "Data races.")
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        NonVolatileDCL<Singleton> factory = new NonVolatileDCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
