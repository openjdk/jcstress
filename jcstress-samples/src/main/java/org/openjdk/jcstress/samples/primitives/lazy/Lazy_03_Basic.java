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

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;
import org.openjdk.jcstress.infra.results.L_Result;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Holder;
import org.openjdk.jcstress.samples.primitives.lazy.shared.HolderFactory;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;
import org.openjdk.jcstress.samples.primitives.lazy.shared.NullHolderFactory;

import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

public class Lazy_03_Basic {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Lazy_03
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        Mindful of failures from the previous two examples, we can build a viable implementation like below.
        This is a double-checked locking on `set` field. `instance` rides on the release-acquire chain that
        stores and loads of `set` form.
     */

    static class BasicLazy<T> implements Lazy<T> {
        private final Supplier<T> factory;
        private volatile boolean set;
        private T instance;

        public BasicLazy(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T get() {
            if (set) {
                return instance;
            }

            synchronized (this) {
                if (!set) {
                    instance = factory.get();
                    set = true;
                }
                return instance;
            }
        }
    }

    /*
        As expected, this performs well on all platforms.

        x86_64, AArch64:
              RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          data, data  789,875,144  100.00%  Acceptable  Seeing the proper data.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data, data"}, expect = ACCEPTABLE, desc = "Seeing the proper data.")
    public static class Basic {
        Lazy<Holder> lazy = new BasicLazy<>(new HolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        It also handles nulls from the factory well.

        x86_64, AArch64:
                            RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          null-holder, null-holder  822,550,984  100.00%  Acceptable  Seeing a null holder.
     */

    @JCStressTest
    @State
    @Outcome(id = "null-holder, null-holder", expect = ACCEPTABLE, desc = "Seeing a null holder.")
    public static class NullHolder {
        Lazy<Holder> lazy = new BasicLazy<>(new NullHolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        And it survives races on `Lazy` itself well:

        x86_64, AArch64:
             RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
               data    786,159,357   35.38%  Acceptable  Trivial.
          null-lazy  1,435,800,267   64.62%  Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = "null-lazy", expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    @Outcome(id = "data",      expect = ACCEPTABLE, desc = "Trivial.")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1()           { lazy = new BasicLazy<>(new HolderFactory()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

    /*
        x86_64, AArch64:
                        RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
                    data, data    710,263,631   16.81%  Acceptable  Trivial.
               data, null-lazy    727,927,021   17.22%  Acceptable  Lazy instance not seen yet.
               null-lazy, data    793,749,671   18.78%  Acceptable  Lazy instance not seen yet.
          null-lazy, null-lazy  1,994,531,161   47.19%  Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data, data"}, expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy, data", "data, null-lazy", "null-lazy, null-lazy"}, expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyTwoWay {
        Lazy<Holder> lazy;
        @Actor public void actor1()            { lazy = new BasicLazy<>(new HolderFactory()); }
        @Actor public void actor2(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor3(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }
}