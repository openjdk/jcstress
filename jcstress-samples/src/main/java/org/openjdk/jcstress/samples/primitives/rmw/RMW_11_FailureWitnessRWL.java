/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.primitives.rmw;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LLLL_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

@JCStressTest
@Outcome(id = {"WRITE-BLOCKED, WRITE-BLOCKED, WRITE-BLOCKED, write-lock",
               "WRITE-BLOCKED, WRITE-BLOCKED, write-lock, WRITE-BLOCKED"},
        expect = ACCEPTABLE, desc = "One writer locked")
@Outcome(id = {"read-lock-1, read-lock-2, READ-BLOCKED, READ-BLOCKED",
               "read-lock-2, read-lock-1, READ-BLOCKED, READ-BLOCKED"},
        expect = ACCEPTABLE, desc = "Two readers locked")
@State
public class RMW_11_FailureWitnessRWL {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t RMW_11_FailureWitnessRWL[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Failure witness is helpful when the updates are not monotonic, and you would like to see
        what exact value you have failed against. Re-reading the value would not help, because it
        might be racy, as RMW_10_FailureWitness shows.

        Consider, for example, a reduced version of read-write lock. With failure witness, we can
        easily disambiguate whether we have conflicted with another reader or another writer.

        On x86_64:
                                                           RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
          WRITE-BLOCKED, WRITE-BLOCKED, WRITE-BLOCKED, write-lock  2,123,068,906   25.22%  Acceptable  One writer locked
          WRITE-BLOCKED, WRITE-BLOCKED, write-lock, WRITE-BLOCKED  1,992,233,099   23.67%  Acceptable  One writer locked
             read-lock-1, read-lock-2, READ-BLOCKED, READ-BLOCKED  2,258,993,329   26.84%  Acceptable  Two readers locked
             read-lock-2, read-lock-1, READ-BLOCKED, READ-BLOCKED  2,043,097,306   24.27%  Acceptable  Two readers locked
     */

    static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(RMW_11_FailureWitnessRWL.class, "state", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // RWL state:
    //   0..+inf: acquired for (multiple) readers
    //         0: free
    //        -1: acquired for single writer
    int state;

    private String takeForRead() {
        int witness = (int)VH.compareAndExchange(this, 0, 1);
        if (witness == 0) {
            // Read lock acquired.
            return "read-lock-1";
        } else if (witness < 0) {
            // Another thread acquired for write.
            return "WRITE-BLOCKED";
        } else {
            // Another thread acquired for read.
            return "read-lock-2";
        }
    }

    private String takeForWrite() {
        int witness = (int)VH.compareAndExchange(this, 0, -1);
        if (witness == 0) {
            // Write lock acquired.
            return "write-lock";
        } else if (witness < 0) {
            // Another thread acquired for write.
            return "WRITE-BLOCKED";
        } else {
            // Another thread acquired for read.
            return "READ-BLOCKED";
        }
    }

    @Actor
    public void actor1(LLLL_Result r) {
        r.r1 = takeForRead();
    }

    @Actor
    public void actor2(LLLL_Result r) {
        r.r2 = takeForRead();
    }

    @Actor
    public void actor3(LLLL_Result r) {
        r.r3 = takeForWrite();
    }

    @Actor
    public void actor4(LLLL_Result r) {
        r.r4 = takeForWrite();
    }

}
