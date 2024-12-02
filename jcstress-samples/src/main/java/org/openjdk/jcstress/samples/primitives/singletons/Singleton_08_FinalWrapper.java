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

public class Singleton_08_FinalWrapper {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_08
     */

    /*
        ----------------------------------------------------------------------------------------------------------

        You might have noticed that Singleton_07_BrokenNonVolatileDCL example still works well, as long
        as we store the safely constructed singleton instances. This allows us to survive races even without
        volatiles or acquires! We can exploit this by wrapping the singleton in the _final wrapper_, so that
        it allows us to survive the races, no matter if the singleton itself is safely constructed.

        See BasicJMM_08_Finals for more examples on finals. This construction is similar to BasicJMM_09_BenignRaces.

        The downside of this approach is another dereference when accessing the object, as well as a bit more
        memory spent for wrappers themselves.
     */

    public static class FinalWrapper<T> implements Factory<T> {
        private Wrapper<T> wrapper;

        @Override
        public T get(Supplier<T> supplier) {
            Wrapper<T> w = wrapper;
            if (w != null) {
                return w.instance;
            }

            synchronized (this) {
                if (wrapper == null) {
                    wrapper = new Wrapper<>(supplier.get());
                }
                return wrapper.instance;
            }
        }

        private static class Wrapper<T> {
            public final T instance;
            public Wrapper(T instance) {
                this.instance = instance;
            }
        }
    }

    /*
        Not surprisingly, this example works with final singletons.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,234,160,020   59.42%  Acceptable  Trivial.
          data2, data2    842,678,324   40.58%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        FinalWrapper<Singleton> factory = new FinalWrapper<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        What we gain here is proper behavior with non-final singletons!

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  1,092,867,901   58.10%  Acceptable  Trivial.
          data2, data2    788,130,443   41.90%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        FinalWrapper<Singleton> factory = new FinalWrapper<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
