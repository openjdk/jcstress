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
package org.openjdk.jcstress.samples.jmm.basic;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class BasicJMM_08_Finals {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_08_Finals[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Finals are another aspect of Java Memory Model. They allow surviving the publication
        via the race. In other words, they provide some basic inter-thread semantics, even in
        the absence of proper synchronization.

        x86_64:
                  RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          -1, -1, -1, -1  115,042,024   52.43%   Acceptable  Object is not seen yet.
              1, 0, 3, 0        3,892   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 3, 4       14,291   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 3, 0          123   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 3, 4  104,365,974   47.56%   Acceptable  Seen the complete object.

        AArch64:
                  RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          -1, -1, -1, -1  508,775,120   84.84%   Acceptable  Object is not seen yet.
              0, 0, 0, 0        1,725   <0.01%  Interesting  Seeing partially constructed object.
              0, 0, 0, 4          763   <0.01%  Interesting  Seeing partially constructed object.
              0, 0, 3, 0            9   <0.01%  Interesting  Seeing partially constructed object.
              0, 0, 3, 4           57   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 0, 0           21   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 0, 4        6,177   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 3, 0           17   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 3, 4        1,103   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 0, 0          101   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 0, 4           16   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 3, 0            1   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 3, 4          132   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 0, 0            7   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 0, 4          125   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 3, 0           91   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 3, 4   90,884,551   15.16%   Acceptable  Seen the complete object.
    */

    @JCStressTest
    @Outcome(id = "-1, -1, -1, -1", expect = ACCEPTABLE,             desc = "Object is not seen yet.")
    @Outcome(id = "1, 2, 3, 4",     expect = ACCEPTABLE,             desc = "Seen the complete object.")
    @Outcome(                       expect = ACCEPTABLE_INTERESTING, desc = "Seeing partially constructed object.")
    @State
    public static class PlainInit {
        int v = 1;

        MyObject o;

        @Actor
        public void actor1() {
            o = new MyObject(v);
        }

        @Actor
        public void actor2(IIII_Result r) {
            MyObject o = this.o;
            if (o != null) {
                r.r1 = o.x1;
                r.r2 = o.x2;
                r.r3 = o.x3;
                r.r4 = o.x4;
            } else {
                r.r1 = -1;
                r.r2 = -1;
                r.r3 = -1;
                r.r4 = -1;
            }
        }

        public static class MyObject {
            int x1, x2, x3, x4;
            public MyObject(int v) {
                x1 = v;
                x2 = x1 + v;
                x3 = x2 + v;
                x4 = x3 + v;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        One might think this is caused by compilers publishing the object reference before all field stores
        complete. This can be checked by switching the publication/acquisition accesses into "opaque" accesses.

        Indeed, the observed partial construction goes away for x86_64:

                  RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          -1, -1, -1, -1  208,243,420   65.50%   Acceptable  Object is not seen yet.
              1, 2, 3, 4  109,671,204   34.50%   Acceptable  Seen the complete object.

        ...but not for AArch64, since the memory ordering is still not there:

                  RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
          -1, -1, -1, -1  554,933,051   85.40%   Acceptable  Object is not seen yet.
              0, 0, 0, 0        1,701   <0.01%  Interesting  Seeing partially constructed object.
              0, 0, 0, 4          680   <0.01%  Interesting  Seeing partially constructed object.
              0, 0, 3, 0           26   <0.01%  Interesting  Seeing partially constructed object.
              0, 0, 3, 4           76   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 0, 0           12   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 0, 4           19   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 3, 0           17   <0.01%  Interesting  Seeing partially constructed object.
              0, 2, 3, 4          850   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 0, 0          124   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 0, 4            1   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 3, 0            6   <0.01%  Interesting  Seeing partially constructed object.
              1, 0, 3, 4            7   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 0, 0           17   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 0, 4           16   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 3, 0          106   <0.01%  Interesting  Seeing partially constructed object.
              1, 2, 3, 4   94,858,107   14.60%   Acceptable  Seen the complete object.
     */

    @JCStressTest
    @Outcome(id = "-1, -1, -1, -1", expect = ACCEPTABLE,             desc = "Object is not seen yet.")
    @Outcome(id = "1, 2, 3, 4",     expect = ACCEPTABLE,             desc = "Seen the complete object.")
    @Outcome(                       expect = ACCEPTABLE_INTERESTING, desc = "Seeing partially constructed object.")
    @State
    public static class OpaqueInit {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(OpaqueInit.class, "o", MyObject.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        int v = 1;

        MyObject o;

        @Actor
        public void actor1() {
            VH.setOpaque(this, new MyObject(v));
        }

        @Actor
        public void actor2(IIII_Result r) {
            MyObject o = (MyObject) VH.getOpaque(this);
            if (o != null) {
                r.r1 = o.x1;
                r.r2 = o.x2;
                r.r3 = o.x3;
                r.r4 = o.x4;
            } else {
                r.r1 = -1;
                r.r2 = -1;
                r.r3 = -1;
                r.r4 = -1;
            }
        }

        public static class MyObject {
            int x1, x2, x3, x4;
            public MyObject(int v) {
                x1 = v;
                x2 = x1 + v;
                x3 = x2 + v;
                x4 = x3 + v;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        The reliable way to see fully constructed object is synchronization. But there is another safety
        mechanism to survive the absense of proper synchronization: finals. If we put them on all critical
        fields, then the only observed state is full object.

        x86_64, AArch64
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  238,418,437   80.75%  Acceptable  Object is not seen yet.
               8   56,845,307   19.25%  Acceptable  Seen the complete object.
     */

    @JCStressTest
    @Outcome(id = "-1, -1, -1, -1", expect = ACCEPTABLE, desc = "Object is not seen yet.")
    @Outcome(id = "1, 2, 3, 4",     expect = ACCEPTABLE, desc = "Seen the complete object.")
    @Outcome(                       expect = FORBIDDEN,  desc = "Everything else is forbidden.")
    @State
    public static class FinalInit {
        int v = 1;

        MyObject o;

        @Actor
        public void actor1() {
            o = new MyObject(v);
        }

        @Actor
        public void actor2(IIII_Result r) {
            MyObject o = this.o;
            if (o != null) {
                r.r1 = o.x1;
                r.r2 = o.x2;
                r.r3 = o.x3;
                r.r4 = o.x4;
            } else {
                r.r1 = -1;
                r.r2 = -1;
                r.r3 = -1;
                r.r4 = -1;
            }
        }

        public static class MyObject {
            final int x1, x2, x3, x4;
            public MyObject(int v) {
                x1 = v;
                x2 = x1 + v;
                x3 = x2 + v;
                x4 = x3 + v;
            }
        }
    }

}
