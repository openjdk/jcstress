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
import org.openjdk.jcstress.samples.primitives.lazy.shared.NullHolderFactory;

import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

public class Lazy_06_FencedOneShot {

   /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Lazy_06
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        There is an alternative to Lazy_05_WrapperOneShot: emulating the final field semantics by using
        the explicit fence in constructor. Note this is a very sharp-edged tool, because it forces us to
        think outside the formal language guarantees. JDK sometimes do this trick in class libraries,
        where the code is under 100% JDK control and can be amended at any time, if broken.

        This example is provided for completeness. Reliable code should not use the constructions like this,
        unless there is a quick way to fix it when it is proven to be broken.
     */

    public static class FencedOneShot<T> implements Lazy<T> {
        private volatile Supplier<T> factory;
        private T instance;

        public FencedOneShot(Supplier<T> factory) {
            this.factory = factory;
            VarHandle.storeStoreFence();
        }

        @Override
        public T get() {
            if (factory == null) {
                return instance;
            }

            synchronized (this) {
                if (factory != null) {
                    instance = factory.get();
                    factory = null;
                }
                return instance;
            }
        }
    }

    /*
        This works well in basic tests.

        x86_64, AArch64:
              RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          data, data  722,475,464  100.00%  Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = ACCEPTABLE, desc = "Trivial.")
    public static class Basic {
        Lazy<Holder> lazy = new FencedOneShot<>(new HolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        This keeps handling null-s well.

        x86_64, AArch64:
                            RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          null-holder, null-holder  805,962,184  100.00%  Acceptable  Seeing a null holder.
     */

    @JCStressTest
    @State
    @Outcome(id = "null-holder, null-holder", expect = ACCEPTABLE, desc = "Seeing a null holder.")
    public static class NullHolder {
        Lazy<Holder> lazy = new FencedOneShot<>(new NullHolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        And, of course, it survives races on Lazy instance itself.

        x86_64, AArch64:
             RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
               data    645,079,245   30.00%  Acceptable  Trivial.
          null-lazy  1,505,528,059   70.00%  Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = "data",      expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = "null-lazy", expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1()           { lazy = new FencedOneShot<>(new HolderFactory()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

    /*
       x86_64, AArch64:
                    RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
                data, data    527,940,924   12.44%  Acceptable  Trivial.
           data, null-lazy    610,097,483   14.37%  Acceptable  Lazy instance not seen yet.
           null-lazy, data    731,296,132   17.23%  Acceptable  Lazy instance not seen yet.
      null-lazy, null-lazy  2,375,169,585   55.96%  Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy, data", "data, null-lazy", "null-lazy, null-lazy"}, expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    public static class RacyTwoWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new FencedOneShot<>(new HolderFactory()); }
        @Actor public void actor2(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor3(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

}