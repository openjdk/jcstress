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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

public class Singleton_06_AcquireReleaseDCL {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Singleton_06
     */

    /*
        ----------------------------------------------------------------------------------------------------------


        If one studies Singleton_05_DCL example more deeply, then one can ask whether the full-blown volatile
        is even needed. The answer is: it is not needed. We only need the release-acquire chain between the store
        of instance and the unsynchronized load of it. See BasicJMM_06_Causality example for basic example of this.

        Acquire-release is relatively easy to construct with VarHandles, see below. This might improve performance
        on weakly-ordered platforms, where the sequentially-consistent loads. are more heavy-weight than acquire loads.
        In overwhelming majority of cases optimizing this is not worth it. We will look at this example for the sake
        of completeness.
     */

    public static class AcquireReleaseDCL<T> implements Factory<T> {
        static final VarHandle VH;
        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(AcquireReleaseDCL.class, "instance", Object.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private T instance;

        @Override
        public T get(Supplier<T> supplier) {
            T res = (T) VH.getAcquire(this);
            if (res != null) {
                return res;
            }

            synchronized (this) {
                if (VH.get(this) == null) {
                    VH.setRelease(this, supplier.get());
                }
                return (T) VH.get(this);
            }
        }
    }

    /*
        This implementation works on all platforms.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  2,421,292,475   52.14%  Acceptable  Trivial.
          data2, data2  2,222,928,909   47.86%  Acceptable  Trivial.
       */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class Final {
        AcquireReleaseDCL<Singleton> factory = new AcquireReleaseDCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new FinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new FinalSingleton("data2")); }
    }

     /*
        This implementation works on all platforms.

        x86_64, AArch64:
                RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          data1, data1  2,166,322,137   57.64%  Acceptable  Trivial.
          data2, data2  1,591,873,007   42.36%  Acceptable  Trivial.
       */

    @JCStressTest
    @State
    @Outcome(id = {"data1, data1", "data2, data2" }, expect = Expect.ACCEPTABLE, desc = "Trivial.")
    public static class NonFinal {
        AcquireReleaseDCL<Singleton> factory = new AcquireReleaseDCL<>();
        @Actor public void actor1(LL_Result r) { r.r1 = MapResult.map(factory, () -> new NonFinalSingleton("data1")); }
        @Actor public void actor2(LL_Result r) { r.r2 = MapResult.map(factory, () -> new NonFinalSingleton("data2")); }
    }

}
