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

public class Singleton_04_InefficientSynchronized {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_04
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        CAS test fixes the versioning problem for us, but it still left us with a problem when calling to supplier
        multiple times is expensive.

        The common answer to these kinds of troubles is _mutual exclusion_ primitives, that only allow one thread
        to enter the mutual exclusion area. A common primitives for this are locks, for example intrinsic
         "synchronized" in Java.
     */

    public static class Synchronized<T> implements Factory<T> {
        private T instance;

        @Override
        public T get(Supplier<T> supplier) {
            synchronized (this) {
                if (instance == null) {
                    instance = supplier.get();
                }
                return instance;
            }
        }
    }

    /*
        As expected, this solves the versioning problem.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,267,833,868   59.29%  Acceptable  Trivial.
          data2, data2    870,567,356   40.71%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        Synchronized<Singleton> factory = new Synchronized<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        And, of course, it also solves the visibility problem: all threads agree on the contents of singletons.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,537,912,621   71.20%  Acceptable  Trivial.
          data2, data2    622,064,283   28.80%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        Synchronized<Singleton> factory = new Synchronized<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

    /*
        There is still a major performance problem: taking a lock every time we reach for singleton is expensive.
     */

}
