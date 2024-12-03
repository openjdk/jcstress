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

public class Singleton_05_DCL {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_05
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        All the observations and samples so far provide us with the building blocks for so called
        "Double-Checked Locking" pattern. This is the most common way to achieve singleton properties.
        It performs well in the majority of cases, and requires no deep analysis for correctness.

        You might know this pattern by this form:

            @Override
            public T get(Supplier<T> supplier) {
                if (instance == null) {                 // Check 1
                    synchronized (this) {               // Lock
                        if (instance == null) {         // Check 2
                            instance = supplier.get();
                        }
                    }
                }
                return instance;
            }

        See how two features work in tandem to solve all problems:
           1. Mutual exclusion to execute the supplier once is handled by synchronized block.
           2. Safe publication of `instance` is guaranteed by volatile. This is important because some
              readers might not enter the slow path.
           3. All interleaving is resolved by checking the `instance`, and going into slow path on failure.

        In this and the following examples, we are going to be using a variant of that code that is easier
        to modify for different primitives, and is arguably easier to reason about.

        2+ threads coming to slow path is the easiest case to think about: everything there happens under
        the lock. 2+ threads coming to fast path have no conflicts either: they only read instance. The only
        case we need to think through is what happens if 1 thread is in slow path, and another is in fast path.
     */

    public static class DCL<T> implements Factory<T> {
        private volatile T instance;

        @Override
        public T get(Supplier<T> supplier) {
            // Fast path: care about correctness+performance.
            T t = instance;
            if (t != null) {
                return t;
            }

            // Slow path: care about correctness only.
            // This is effectively the body of Singleton_04_InefficientSynchronized sample.
            synchronized (this) {
                if (instance == null) {
                    instance = supplier.get();
                }
                return instance;
            }
        }
    }

    /*
        Indeed, this works well on all architectures.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,259,722,777   57.88%  Acceptable  Trivial.
          data2, data2    916,873,647   42.12%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        DCL<Singleton> factory = new DCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        Since volatile provides us with publication guarantees, non-final singletons also work well.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,057,019,574   56.88%  Acceptable  Trivial.
          data2, data2    801,420,050   43.12%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        DCL<Singleton> factory = new DCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
