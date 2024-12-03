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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class Singleton_03_InefficientCAS {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_03
    */

    /*
        ----------------------------------------------------------------------------------------------------------

        Volatile solves the publication problem for us, but it does not solve the multiple
        versions of a singleton installed. What if we used Compare-And-Set like this?
     */

    public static class CAS<T> implements Factory<T> {
        private final AtomicReference<T> ref = new AtomicReference<T>();

        @Override
        public T get(Supplier<T> supplier) {
            if (ref.get() == null) {
                ref.compareAndSet(null, supplier.get());
            }
            return ref.get();
        }
    }

    /*
        As expected, there are no problems with versioning anymore on any architecture.

        AArch64, x86_64:
                    RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              data1, data1  2,078,570,754   52.66%   Acceptable  Trivial.
              data2, data2  1,868,941,510   47.34%   Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition.")
    public static class Final {
        CAS<Singleton> factory = new CAS<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

    /*
        There are no problems with singleton publication either: CAS "releases" the singleton object,
        and get()-s acquire it.

        AArch64, x86_64:
                    RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              data1, data1  2,480,166,763   45.74%   Acceptable  Trivial.
              data2, data2  2,942,540,381   54.26%   Acceptable  Trivial.
     */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    @Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition.")
    public static class NonFinal {
        CAS<Singleton> factory = new CAS<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

    /*
        There is still a major performance problem here:

        Without better coordination, we can call to supplier multiple times, and depending on what happens there,
        we may incur a lot of overheads. In worst case, if supplier takes *seconds* to complete, *all* threads
        could ask for their instance, and all but one thread would be able to install that version!
     */

}
