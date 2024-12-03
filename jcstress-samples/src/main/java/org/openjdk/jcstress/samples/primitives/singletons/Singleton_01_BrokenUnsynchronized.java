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

public class Singleton_01_BrokenUnsynchronized {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_01
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        This test starts as the base for more advanced Singleton examples.

        The implementation below is clearly incorrect in multithreaded code: there is no
        synchronization anywhere, so race conditions are abound.
     */

    public static class Unsynchronized<T> implements Factory<T> {
        private T instance;

        @Override
        public T get(Supplier<T> supplier) {
            if (instance == null) {
                instance = supplier.get();
            }
            return instance;
        }
    }

    /*
        The most basic test is trying to see what kind of failure modes this implementation experiences.

        This is not the architecture-specific problem, it is just a basic interleaving problem. We can clearly
        see the interesting outcome from two threads not seeing each others update, and leaving with their
        own installed singleton versions. This is a failure of singleton contract.

        AArch64, x86_64:
                    RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              data1, data1  1,529,743,128   52.28%   Acceptable  Trivial.
              data1, data2    203,941,377    6.97%  Interesting  Race condition.
              data2, data2  1,192,582,319   40.75%   Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition.")
    public static class Final {
        Unsynchronized<Singleton> factory = new Unsynchronized<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        It gets more interesting when Singleton itself carries data. Not only threads might not see
        the singleton version installed by another thread, they are also not guaranteed to see the
        singleton *contents*, as we see in the interesting example showing "null-data".

        See BasicJMM_06_Causality that shows there is no causality in the absence of synchronization.
        See BasicJMM_08_Finals that shows why the Final test above survives does not have "null-data".

        AArch64:
                            RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              data1, data1  1,042,687,796   44.37%   Acceptable  Trivial.
              data1, data2    193,299,702    8.23%  Interesting  Race condition.
          data1, null-data         80,883   <0.01%  Interesting  Race condition.
              data2, data2  1,113,547,718   47.39%   Acceptable  Trivial.
          null-data, data2        118,245   <0.01%  Interesting  Race condition.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition.")
    public static class NonFinal {
        Unsynchronized<Singleton> factory = new Unsynchronized<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }


}
