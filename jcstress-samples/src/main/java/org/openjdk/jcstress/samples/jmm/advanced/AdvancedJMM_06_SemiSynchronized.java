/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.jmm.advanced;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

public class AdvancedJMM_06_SemiSynchronized {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_06_SemiSynchronized[.SubTestName]
     */

    static class Composite {
        int x;
        public Composite(int v) {
            x = v;
        }
        public int get() {
            return x;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Somewhat similar to previous example, this test now publishes the Composite with the synchronized
        setter. But, the getter is deliberately non-synchronized. Unfortunately, synchronizing only the
        setter is not enough: the getter is still racy, and can observe surprising results.

        This can be seen on some platforms, for example with PPC64 (modern JDKs require -XX:+UseBiasedLocking):
          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  566,235,858   91.44%   Acceptable  Boring
               0          354   <0.01%  Interesting  Whoa
              42   53,010,764    8.56%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = "0",          expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class Racy {
        Holder<Composite> h = new Holder<>(new Composite(-1));

        @Actor
        void actor() {
            h.set(new Composite(42));
        }

        @Actor
        void observer(I_Result r) {
            r.r1 = h.get().get();
        }

        static class Holder<T> {
            T value;

            public Holder(T v) {
                value = v;
            }

            public synchronized void set(T v) {
                value = v;
            }

            public T get() { // Deliberately not synchronized
                return value;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        If we properly synchronize both getter and setter, the previously interesting example is now forbidden.

        PPC64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  229,147,333   43.33%  Acceptable  Boring
               0            0    0.00%   Forbidden  Now forbidden
              42  299,711,163   56.67%  Acceptable  Boring
     */
    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0",          expect = FORBIDDEN,  desc = "Now forbidden")
    public static class NonRacy {
        Holder<Composite> h = new Holder<>(new Composite(-1));

        @Actor
        void actor() {
            h.set(new Composite(42));
        }

        @Actor
        void observer(I_Result r) {
            r.r1 = h.get().get();
        }

        static class Holder<T> {
            T value;

            public Holder(T v) {
                value = v;
            }

            public synchronized void set(T v) {
                value = v;
            }

            public synchronized T get() {
                return value;
            }
        }
    }

}