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
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

public class AdvancedJMM_14_BenignRaces {
    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_14_BenignRaces[.SubTestName]
     */

    /*
       ----------------------------------------------------------------------------------------------------------

        The well established semantics under the racy accesses allows to construct the examples where
        object are published without problems via the race. This condition is known as "benign race".
        It generally follows this form:

            T get() {
                T t = instance;      // 1: SINGLE racy read
                if (t == null) {
                    t = new T(...);  // 2: SAFE, IDEMPOTENT construction
                    instance = t;    // racy store
                }
                return t;
            }

        The mechanics of this benign race is understandable: if we read something, we read it in full,
        because final field semantics saves us (see BasicJMM_08_Finals example). If we read null, then
        we can recover by creating the instance ourselves and persist it for future callers. Since
        there might be a write-write race here, it is important that construction is idempotent, so
        regardless how many threads are initializing the object, all of them would store the semantically
        identical object.

        The additional wrinkle from the JMM is the need for the single read. If we read the instance field
        once, figure out it is not null, and then read it again, we might get null on second read!
        This is similar to the BasicJMM_05_Coherence example.

        In this test, we make such failures even more likely by introducing the "eraser" actor that
        drops the reference back to null. Correctly coded benign races should stay benign.

        Indeed, "Full" test that satisfies all this conditions only sees the values we want:

        x86_64, AArch64, PPC64:
          RESULT         SAMPLES     FREQ      EXPECT  DESCRIPTION
          42, 42  37,567,132,672  100.00%  Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = "42, 42", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(               expect = FORBIDDEN,  desc = "Cannot happen")
    public static class Full {

        @Actor
        public void actor1(II_Result r) {
            MyObject m = get();
            r.r1 = (m != null) ? m.x : -1;
        }

        @Actor
        public void actor2(II_Result r) {
            MyObject m = get();
            r.r2 = (m != null) ? m.x : -1;
        }

        @Actor
        public void eraser() {
            instance = null;
        }

        MyObject instance;

        MyObject get() {
            MyObject t = instance;
            if (t == null) {
                t = new MyObject(42);
                instance = t;
            }
            return t;
        }

        static class MyObject {
            final int x;
            MyObject(int x) {
                this.x = x;
            }
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        This changes if we relax either of the benign races requirements. For example, dropping "final"
        from the constructed object makes its construction unsafe, and thus we expose ourselves ot "0" outcomes.
        Indeed, that is seen on real hardware:

        AArch64:
          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
           0, 42       27,454   <0.01%  Interesting  Whoa
           42, 0       43,986   <0.01%  Interesting  Whoa
          42, 42  966,405,616   99.99%   Acceptable  Boring

        PPC64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
           0, 42        129,204   <0.01%  Interesting  Whoa
           42, 0         61,951   <0.01%  Interesting  Whoa
          42, 42  2,182,111,821   99.99%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = "42, 42", expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(               expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class NonFinal {

        @Actor
        public void actor1(II_Result r) {
            MyObject m = get();
            r.r1 = (m != null) ? m.x : -1;
        }

        @Actor
        public void actor2(II_Result r) {
            MyObject m = get();
            r.r2 = (m != null) ? m.x : -1;
        }

        @Actor
        public void eraser() {
            instance = null;
        }

        MyObject instance;

        MyObject get() {
            MyObject t = instance;
            if (t == null) {
                t = new MyObject(42);
                instance = t;
            }
            return t;
        }

        static class MyObject {
            int x;
            MyObject(int x) {
                this.x = x;
            }
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        Relaxing the double-read requirements exposes us to nulls, as explained above.

        x86_64:
          RESULT         SAMPLES     FREQ       EXPECT  DESCRIPTION
          -1, -1         294,812   <0.01%  Interesting  Whoa
          -1, 42       4,644,317    0.01%  Interesting  Whoa
          42, -1       4,736,261    0.01%  Interesting  Whoa
          42, 42  33,188,817,282   99.97%   Acceptable  Boring

        AArch64:
          RESULT         SAMPLES     FREQ       EXPECT  DESCRIPTION
          -1, -1         147,415   <0.01%  Interesting  Whoa
          -1, 42       1,076,573   <0.01%  Interesting  Whoa
          42, -1       1,106,320   <0.01%  Interesting  Whoa
          42, 42  12,180,344,380   99.98%   Acceptable  Boring

        PPC64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
           0, 42        129,204   <0.01%  Interesting  Whoa
           42, 0         61,951   <0.01%  Interesting  Whoa
          42, 42  2,182,111,821   99.99%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = "42, 42", expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(               expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class DoubleRead {

        @Actor
        public void actor1(II_Result r) {
            MyObject m = get();
            r.r1 = (m != null) ? m.x : -1;
        }

        @Actor
        public void actor2(II_Result r) {
            MyObject m = get();
            r.r2 = (m != null) ? m.x : -1;
        }

        @Actor
        public void eraser() {
            instance = null;
        }

        MyObject instance;

        MyObject get() {
            if (instance == null) {
                instance = new MyObject(42);
            }
            return instance;
        }

        static class MyObject {
            final int x;
            MyObject(int x) {
                this.x = x;
            }
        }
    }

}