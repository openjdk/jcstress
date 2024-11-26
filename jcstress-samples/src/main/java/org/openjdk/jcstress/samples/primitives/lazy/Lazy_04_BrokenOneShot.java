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
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class Lazy_04_BrokenOneShot {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Lazy_04
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        Lazy_03_Basic gets is a whirlwind of three fields: factory, instance, set. Could we make it better by
        signaling initialized state using `factory` field itself? For example, by `null`-ing it out when it
        was used? This looks like a great idea, and it also conserves memory by not holding to the factory
        once we are done with it.

        We can write DCL based on `factory` field then. The release-acquire between releasing factory=null store
        after first use and the acquire on fast path should give us the memory ordering we want.
     */

    static class BrokenOneShotLazy<T> implements Lazy<T> {
        private volatile Supplier<T> factory;
        private T instance;

        public BrokenOneShotLazy(Supplier<T> factory) {
            this.factory = factory;
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
        ...and it does! The example works well on all platforms.

        x86_64, AArch64:
              RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          data, data  745,085,384  100.00%  Acceptable  Seeing the proper data.
     */

    @JCStressTest
    @State
    @Outcome(id = "data, data", expect = ACCEPTABLE, desc = "Seeing the proper data.")
    public static class Basic {
        Lazy<Holder> lazy = new BrokenOneShotLazy<>(new HolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        ...and it handles nulls for us really well, since we do not depend on `instance` nullity.

        x86_64, AArch64:
                            RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
          null-holder, null-holder  842,580,424  100.00%  Acceptable  Seeing a null holder.
     */

    @JCStressTest
    @State
    @Outcome(id = "null-holder, null-holder", expect = ACCEPTABLE, desc = "Seeing a null holder.")
    public static class NullHolder {
        Lazy<Holder> lazy = new BrokenOneShotLazy<>(new NullHolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

    /*
        Except that this implementation is subtly broke under the racy publication of Lazy itself.
        Changing `final` to `volatile` for `factory` field give us a *weaker* semantics in constructors:
        the store to volatile field in constructor may not be visible under racy publication!
        See AdvancedJMM_13_VolatileVsFinal for the basic example of this. So we see this:

        AArch64:
           RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
             data    654,125,625   29.68%   Acceptable  Trivial.
      null-holder             32   <0.01%  Interesting  Seeing uninitialized holder!
        null-lazy  1,550,108,527   70.32%   Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = "data",        expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = "null-lazy",   expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    @Outcome(id = "null-holder", expect = ACCEPTABLE_INTERESTING, desc = "Seeing uninitialized holder!")
    public static class RacyOneWay {
        Lazy<Holder> lazy;
        @Actor public void actor1()           { lazy = new BrokenOneShotLazy<>(new HolderFactory()); }
        @Actor public void actor2(L_Result r) { r.r1 = Lazy.map(lazy); }
    }

    /*
        ...and also the two-way race is also failing:

        AArch64:
                          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
                      data, data    562,737,656   12.85%   Acceptable  Trivial.
               data, null-holder             12   <0.01%  Interesting  Seeing uninitialized holder!
                 data, null-lazy    695,112,068   15.88%   Acceptable  Lazy instance not seen yet.
               null-holder, data             18   <0.01%  Interesting  Seeing uninitialized holder!
          null-holder, null-lazy             24   <0.01%  Interesting  Seeing uninitialized holder!
                 null-lazy, data    736,499,631   16.82%   Acceptable  Lazy instance not seen yet.
          null-lazy, null-holder             34   <0.01%  Interesting  Seeing uninitialized holder!
            null-lazy, null-lazy  2,383,387,321   54.44%   Acceptable  Lazy instance not seen yet.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data, data"}, expect = ACCEPTABLE, desc = "Trivial.")
    @Outcome(id = {"null-lazy, data", "data, null-lazy", "null-lazy, null-lazy"}, expect = ACCEPTABLE, desc = "Lazy instance not seen yet.")
    @Outcome(id = {"null-holder, .*", ".*, null-holder"}, expect = ACCEPTABLE_INTERESTING, desc = "Seeing uninitialized holder!")
    public static class RacyTwoWay {
        Lazy<Holder> lazy;
        @Actor public void actor1() { lazy = new BrokenOneShotLazy<>(new HolderFactory()); }
        @Actor public void actor2(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor3(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

}