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

import org.openjdk.jcstress.annotations.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class BasicJMM_04_Progress {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_04_Progress[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        One naively can expect that writes to variables are eventually visible. However, under Java Memory Model,
        this does not apply to plain reads and writes. The usual example is the busy loop in plain field.
        The optimizing compiler is allowed to check the field once, and if it is "false", reduce the rest of
        the loop into "while(true)", infinite version.

        Indeed, running this on just about any platform yields:

              RESULT  SAMPLES     FREQ       EXPECT  DESCRIPTION
               STALE        4   50.00%  Interesting  Test is stuck
          TERMINATED        4   50.00%   Acceptable  Gracefully finished
      */

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE,             desc = "Gracefully finished")
    @Outcome(id = "STALE",      expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
    @State
    public static class PlainSpin {
        boolean ready;

        @Actor
        public void actor1() {
            while (!ready); // spin
        }

        @Signal
        public void signal() {
            ready = true;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Making the field "volatile" is the surefire way to achieve progress guarantees.
        All volatile writes are eventually visible, so the loop eventually terminates.

        Indeed, this is guaranteed to happen on all platforms:

              RESULT  SAMPLES    FREQ       EXPECT  DESCRIPTION
               STALE        0    0.0%  Interesting  Test is stuck
          TERMINATED   17,882  100.0%   Acceptable  Gracefully finished
     */

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE,             desc = "Gracefully finished")
    @Outcome(id = "STALE",      expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
    @State
    public static class VolatileSpin {
        volatile boolean ready;

        @Actor
        public void actor1() {
            while (!ready); // spin
        }

        @Signal
        public void signal() {
            ready = true;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        In fact, the overwhelming majority of hardware makes writes eventually visible, so what
        we minimally want is to make the accesses opaque to the optimizing compilers. Luckily,
        that is simple to do with VarHandles.{set|get}Opaque.

        Indeed, this is guaranteed to happen on all platforms:

              RESULT  SAMPLES     FREQ      EXPECT  DESCRIPTION
               STALE        0    0.00%   Forbidden  Test is stuck
          TERMINATED   17,902  100.00%  Acceptable  Gracefully finished
     */

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(id = "STALE",      expect = FORBIDDEN,  desc = "Test is stuck")
    @State
    public static class OpaqueSpin {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(OpaqueSpin.class, "ready", boolean.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        boolean ready;

        @Actor
        public void actor1() {
            while (!(boolean)VH.getOpaque(this)); // spin
        }

        @Signal
        public void signal() {
            VH.setOpaque(this, true);
        }
    }

}
