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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class AdvancedJMM_07_SemiVolatile {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_07_SemiVolatile[.SubTestName]
     */

    static class Composite {
        int x1, x2, x3, x4;
        public Composite(int v) {
            x1 = v; x2 = v; x3 = v; x4 = v;
        }
        public int get() {
            return x4 + x3 + x2 + x1;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Recasting the AdvancedJMM_06_SemiSynchronized example to volatiles, this test now exchanges the
        Composite directly between the threads. In this example, we provide strong memory semantics at the reader
        side. But the writer is left with weaker plain semantics. Like in AdvancedJMM_06_SemiSynchronized, doing
        the stronger semantics only on reader side does not solve the race. The writer can perform the writes in
        whatever order, thus revealing the surprising results even to the strong observer.

        Implementation note: this test uses VarHandles to get access to the mismatched volatile/plain ops.

        This can be seen on some platforms, for example AArch64, PPC64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -4  1,312,356,301   69.12%   Acceptable  Boring
               0            497   <0.01%  Interesting  Whoa
               1             41   <0.01%  Interesting  Whoa
               2             22   <0.01%  Interesting  Whoa
               3            128   <0.01%  Interesting  Whoa
               4    586,358,147   30.88%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "4"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(                  expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class RacyWrite {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(RacyWrite.class, "c", Composite.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        Composite c;

        @Actor
        void actor() {
            Composite lc = new Composite(1);
            VH.set(this, lc);
        }

        @Actor
        void observer(I_Result r) {
            Composite lc = (Composite) VH.getVolatile(this);
            r.r1 = (lc != null) ? lc.get() : -1;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Flipping the memory semantics, doing strong write and weak read does not fix the theoretical race,
        but most modern hardware would honor the data dependent ordering of reads. Here, reading the
        non-null Composite on most hardware would show the correct contents, provided it was published
        properly with a strong memory semantics.

        This is a corner-stone of final field semantics in Java and "Release-Consume Ordering" in C/C++.
        This does not extend to independent memory locations, as BasicJMM_06_Causality shows.

        If anyone has a DEC Alpha machine, try running this test there, as Alpha is one of a few
        known architectures that do not honor data-dependent loads, and might show Interesting outcomes
        here.

        Implementation note: this test uses VarHandles to get access to the mismatched volatile/plain ops.

        On x86, AArch64, PPC64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  2,461,528,799   81.86%   Acceptable  Boring
               4    545,287,457   18.14%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "4"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(                  expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class RacyRead {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(RacyRead.class, "c", Composite.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        Composite c;

        @Actor
        void actor() {
            Composite lc = new Composite(1);
            VH.setVolatile(this, lc);
        }

        @Actor
        void observer(I_Result r) {
            Composite lc = (Composite) VH.get(this);
            r.r1 = (lc != null) ? lc.get() : -1;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        If we use proper memory semantics at both reader and writer sides, the previously interesting examples
        are now forbidden.

        On x86, AArch64, PPC64:
          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  2,188,687,637   75.73%  Acceptable  Boring
               4    701,382,379   24.27%  Acceptable  Boring
     */
    @JCStressTest
    @State
    @Outcome(id = {"-1", "4"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(                  expect = FORBIDDEN,  desc = "Now forbidden")
    public static class NonRacy {
        volatile Composite c;

        @Actor
        void actor() {
            c = new Composite(1);
        }

        @Actor
        void observer(I_Result r) {
            Composite lc = c;
            r.r1 = (lc != null) ? lc.get() : -1;
        }
    }

}
