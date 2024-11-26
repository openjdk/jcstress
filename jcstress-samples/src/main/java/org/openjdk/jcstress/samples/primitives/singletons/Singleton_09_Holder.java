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

public class Singleton_09_Holder {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_09
     */

    /*
        ----------------------------------------------------------------------------------------------------------

        This example is here for completeness.

        In some cases for one-shot singletons in the library code, it is more convenient to ride
        on class initialization guarantees by using the "class holder" pattern. JVM guarantees that `H` would
        get initialized on the first reference to it, and that initalization would be serialized by the JVM itself.

        The down-sides for this approach:
            1. It is static: you can only do it once per holder. This means tests below does not actually test
               it all that well.
            2. It is static (again): you need to know the instance you are putting into it ahead of time.
               Note that examples ignore `supplier`.

        The up-sides are:
            1. Simplicity: it is really hard to get wrong.
            2. Optimizeability: in hot code, the class initialization check would likely be elided, and the whole
               thing would be compiled to a single field read.

        This pattern sees limited use in some libraries, including JDK class library.
     */

    public static class FinalHolderHolder implements Factory<Singleton> {
        @Override
        public Singleton get(Supplier<Singleton> supplier) {
            return H.INSTANCE;
        }

        public static class H {
            public static final FinalSingleton INSTANCE = new FinalSingleton("data");
        }
    }

    public static class NonFinalHolderHolder implements Factory<Singleton> {
        @Override
        public Singleton get(Supplier<Singleton> supplier) {
            return H.INSTANCE;
        }

        public static class H {
            public static final NonFinalSingleton INSTANCE = new NonFinalSingleton("data");
        }
    }

    /*
        Not surprisingly, the results are consistent across all architectures.

        x86_64, AArch64:
              RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data, data  3,208,470,984  100.00%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        FinalHolderHolder factory = new FinalHolderHolder();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, null); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, null); }
    }

    /*
        Same with non-final singleton.

        x86_64, AArch64:
              RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data, data  3,491,187,144  100.00%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        NonFinalHolderHolder factory = new NonFinalHolderHolder();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, null); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, null); }
    }

}
