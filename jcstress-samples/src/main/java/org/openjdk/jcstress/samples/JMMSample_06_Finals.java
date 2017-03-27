/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jcstress.samples;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

public class JMMSample_06_Finals {

    /*
      ----------------------------------------------------------------------------------------------------------

        Finals are another aspect of Java Memory Model. They allow surviving the publication
        via the race. In other words, they provide some basic inter-thread semantics, even in
        the absence of proper synchronization.

              [OK] org.openjdk.jcstress.samples.JMMSample_06_Finals.PlainInit
            (JVM args: [-server, -XX:+UnlockDiagnosticVMOptions, -XX:+StressLCM, -XX:+StressGCM])
          Observed state   Occurrences              Expectation  Interpretation
                      -1    82,909,900               ACCEPTABLE  Object is not seen yet.
                       0             0   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       1             0   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       2             0   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       3             0   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       4             0   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       5           622   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       6         1,434   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       7           420   ACCEPTABLE_INTERESTING  Seeing partially constructed object.
                       8    16,973,344               ACCEPTABLE  Seen the complete object.
    */

    @JCStressTest
    @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Object is not seen yet.")
    @Outcome(id = {"0", "1", "2", "3", "4", "5", "6", "7"}, expect = ACCEPTABLE_INTERESTING, desc = "Seeing partially constructed object.")
    @Outcome(id = "8", expect = ACCEPTABLE,  desc = "Seen the complete object.")
    @State
    public static class PlainInit {
        int v = 1;

        MyObject o;

        @Actor
        public void actor1() {
            o = new MyObject(v);
        }

        @Actor
        public void actor2(I_Result r) {
            MyObject o = this.o;
            if (o != null) {
                r.r1 = o.x8 + o.x7 + o.x6 + o.x5 + o.x4 + o.x3 + o.x2 + o.x1;
            } else {
                r.r1 = -1;
            }
        }

        public static class MyObject {
            int x1, x2, x3, x4;
            int x5, x6, x7, x8;
            public MyObject(int v) {
                x1 = v;
                x2 = v;
                x3 = v;
                x4 = v;
                x5 = v;
                x6 = v;
                x7 = v;
                x8 = v;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Putting finals on the fields is enough to get some safety back.

               [OK] org.openjdk.jcstress.samples.JMMSample_06_Finals.FinalInit
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                      -1   112,755,834    ACCEPTABLE  Object is not seen yet.
                       8     3,766,026    ACCEPTABLE  Seen the complete object.
     */

    @JCStressTest
    @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Object is not seen yet.")
    @Outcome(id = "8", expect = ACCEPTABLE,  desc = "Seen the complete object.")
    @Outcome(expect = FORBIDDEN, desc = "Seeing partially constructed object.")
    @State
    public static class FinalInit {
        int v = 1;

        MyObject o;

        @Actor
        public void actor1() {
            o = new MyObject(v);
        }

        @Actor
        public void actor2(I_Result r) {
            MyObject o = this.o;
            if (o != null) {
                r.r1 = o.x8 + o.x7 + o.x6 + o.x5 + o.x4 + o.x3 + o.x2 + o.x1;
            } else {
                r.r1 = -1;
            }
        }

        public static class MyObject {
            final int x1, x2, x3, x4;
            final int x5, x6, x7, x8;
            public MyObject(int v) {
                x1 = v;
                x2 = v;
                x3 = v;
                x4 = v;
                x5 = v;
                x6 = v;
                x7 = v;
                x8 = v;
            }
        }
    }

}
