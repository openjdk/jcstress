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

public class Singleton_10_ThreadLocalWitness {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_10
     */

    /*
        ----------------------------------------------------------------------------------------------------------

        Original DCL paper shows another quirky example:
          https://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html#ThreadLocal

        What if we were able to track whether the thread already visited the slow path, and thus guaranteed
        to have seen or installed the new instance? This works, at the expense of quite expensive ThreadLocal
        lookup on fast path.
     */

    static class ThreadLocalWitness<T> implements Factory<T> {
        private final ThreadLocal<String> witness;
        private T instance;

        public ThreadLocalWitness() {
            this.witness = new ThreadLocal<>();
        }

        @Override
        public T get(Supplier<T> supplier) {
            if (witness.get() != null) {
                return instance;
            }

            synchronized (this) {
                if (instance == null) {
                    instance = supplier.get();
                }

                // NOTE: Original example sets witness.set(witness), but that constructs a memory leak.
                // As the comments in the example correctly note, any non-null value would do as the argument
                // here, so we just put a String constant into it. This insulates us from putting anything
                // that references a thread local back into thread local itself.
                witness.set("seen");
                return instance;
            }
        }
    }

    /*
        This works on all architectures.

        x86_64, AArch64:
                RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  266,609,576   52.16%  Acceptable  Trivial.
          data2, data2  244,502,048   47.84%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        ThreadLocalWitness<Singleton> factory = new ThreadLocalWitness<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        Non-final singletons work well too:

        x86_64, AArch64:
                RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  225,094,945   45.91%  Acceptable  Trivial.
          data2, data2  265,188,519   54.09%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        ThreadLocalWitness<Singleton> factory = new ThreadLocalWitness<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}