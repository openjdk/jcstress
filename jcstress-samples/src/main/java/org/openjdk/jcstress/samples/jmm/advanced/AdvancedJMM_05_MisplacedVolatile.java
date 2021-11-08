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


public class AdvancedJMM_05_MisplacedVolatile {

     /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_05_MisplacedVolatile[.SubTestName]
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

        This test shows the common pitfall: the misplaced synchronization points. Here, the "volatile" is
        placed on the "h" field itself. But there are no releasing writes to "h" that gives us visibility
        of other updates! Note that test checks the value of dependent field, Composite.x -- the field of
        the object that we have passed between the threads unsafely.

        This outcome is possible on AArch64:
          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  388,488,952   81.30%   Acceptable  Boring
               0          553   <0.01%  Interesting  Whoa
              42   89,365,471   18.70%   Acceptable  Boring
     */
    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(id = "0",          expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class Racy {
        volatile Holder<Composite> h = new Holder<>(new Composite(-1));

        @Actor
        void actor() {
            h.set(new Composite(42));
        }

        @Actor
        void observer(I_Result r){
            r.r1 = h.get().get();
        }

        static class Holder<T> {
            T value;

            public Holder(T v) {
                value = v;
            }

            public void set(T v) {
                value = v;
            }

            public T get() {
                return value;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------
        Correctly synchronized program has "volatile" at the publication point: the write to "value" is now
        releasing write, and read from "value" is now acquiring write. Therefore, seeing zero in the composite
        data is illegal now.

        AArch64:
          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  309,220,995   72.04%  Acceptable  Boring
               0            0    0.00%   Forbidden  Illegal
              42  120,004,221   27.96%  Acceptable  Boring
     */
    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0",          expect = FORBIDDEN,  desc = "Illegal")
    public static class NonRacy {
        Holder<Composite> h = new Holder<>(new Composite(-1));

        @Actor
        void actor() {
            h.set(new Composite(42));
        }

        @Actor
        void observer(I_Result r){
            r.r1 = h.get().get();
        }

        static class Holder<T> {
            volatile T value; // volatile is now here

            public Holder(T v) {
                value = v;
            }

            public void set(T v) {
                value = v;
            }

            public T get() {
                return value;
            }
        }
    }


}