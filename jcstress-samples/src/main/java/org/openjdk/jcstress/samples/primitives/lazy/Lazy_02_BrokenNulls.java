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
import org.openjdk.jcstress.samples.primitives.lazy.shared.Holder;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;
import org.openjdk.jcstress.samples.primitives.lazy.shared.NullHolderFactory;

import java.util.function.Supplier;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class Lazy_02_BrokenNulls {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Lazy_02
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        Now that we know that `factory` should be `final`, consider additional requirement on Lazy<T>.

        There is no fundamental reason why factory is not allowed to return `null`. When it does, we need to
        pass it well.
     */

    static class BrokenNullsLazy<T> implements Lazy<T> {
        private final Supplier<T> factory;
        private volatile T instance;

        public BrokenNullsLazy(Supplier<T> factory) {
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
        The basic test shows that returning `null` from factory exposes a logic bug: since our
        "uninitialized" state is tracked as `null`, we cannot disambiguate the cases of legitimate `null`-s
        returned by factory, and the `null`-s as uninitialized state. So we end up calling the factory
        all the time. It would be okay if factory was fully idempotent, but it might not be, and calling
        it multiple times is certainly not lazy.

        x86_64, AArch64:
                    RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
          dup, null-holder  1,232,127,022   49.91%  Interesting  Factory is called twice!
          null-holder, dup  1,236,790,682   50.09%  Interesting  Factory is called twice!
     */

    @JCStressTest
    @State
    @Outcome(id = {"null-holder, dup", "dup, null-holder"}, expect = ACCEPTABLE_INTERESTING, desc = "Factory is called twice!")
    public static class NullHolder {
        Lazy<Holder> lazy = new BrokenNullsLazy<>(new NullHolderFactory());
        @Actor public void actor1(LL_Result r) { r.r1 = Lazy.map(lazy); }
        @Actor public void actor2(LL_Result r) { r.r2 = Lazy.map(lazy); }
    }

}