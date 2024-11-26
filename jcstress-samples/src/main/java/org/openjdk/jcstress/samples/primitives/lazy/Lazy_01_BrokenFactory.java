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
package org.openjdk.jcstress.samples.primitives.lazy;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LL_Result;
import org.openjdk.jcstress.infra.results.L_Result;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Holder;
import org.openjdk.jcstress.samples.primitives.lazy.shared.HolderFactory;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;

import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class Lazy_01_BrokenFactory {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Lazy_01
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        This test starts the discussion on more advanced topic: creating Lazy<T>, a lazy factory for the object.
        Lazy<T> looks deceptively like a singleton, and it is nearly that. Look through Singleton samples before
        continuing here.

        We will start building out the implementation that is based on double-checked locking from Singleton_05_DCL.
        This time we would give a supplier to a constructor.

        See Lazy.map(...) to understand the state that test verifies.
     */

    static class BrokenFactoryLazy<T> implements Lazy<T> {
        private Supplier<T> factory;
        private volatile T instance;

        public BrokenFactoryLazy(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T get() {
            T t = instance;
            if (t != null) {
                return t;
            }

            synchronized (this) {
                if (instance == null) {
                    instance = factory.get();
                }
                return instance;
            }
        }
    }

    /*
        As expected, this performs well on all platforms.

        x86_64, AArch64:
              RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          data, data  721,594,824  100.00%  Acceptable  Seeing the proper data.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data, data"}, expect = ACCEPTABLE, desc = "Seeing the proper data.")
    public static class Basic {
        Lazy<Holder> lazy = new BrokenFactoryLazy<>(new HolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        ...except, wait, it does not work well when we publish the _Lazy instance itself_ via race.
        Good implementations should survive this intact. This implementation does not!

        AArch64:
             RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
               data    721,802,082   29.74%   Acceptable  Trivial.
                dup              1   <0.01%  Interesting  Factory is called twice!
          exception            109   <0.01%  Interesting  Internal error!
          null-lazy  1,705,407,992   70.26%   Acceptable  Lazy instance not seen yet.

        There are two major problems with a common cause. Since `Lazy.factory` field is not final,
        publishing `Lazy` instance racily leaves that field in bad state. Sometimes we see `factory == null`,
        and this causes the internal exception. Sometimes we see `factory != null`, but the internal Supplier
        state is broken: it _thinks_ it was already called (`HolderSupplier.first == false`)!

        This further expands on BasicJMM_08_Finals. `final` fields are not only hygienic practice,
        they have concurrent safety implications.
     */

    @JCStressTest
    @State
    @Outcome(id = "null-lazy", expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    @Outcome(id = "data",      expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = "dup",       expect = ACCEPTABLE_INTERESTING, desc = "Supplier barfed.")
    @Outcome(id = "exception", expect = ACCEPTABLE_INTERESTING, desc = "Internal error!")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1()           { lazy = new BrokenFactoryLazy<>(new HolderFactory()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

}