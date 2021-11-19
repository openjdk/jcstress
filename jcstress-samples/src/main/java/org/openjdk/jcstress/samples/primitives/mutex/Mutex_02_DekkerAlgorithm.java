/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.primitives.mutex;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = {"1, 2", "2, 1"}, expect = ACCEPTABLE, desc = "Mutex works")
@Outcome(id = "1, 1",           expect = FORBIDDEN,  desc = "Mutex failure")
@State
public class Mutex_02_DekkerAlgorithm {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Mutex_02_DekkerAlgorithm
    */

    /*
      ----------------------------------------------------------------------------------------------------------
        This example demonstrates the Dekker's algorithm for mutual exclusion.
            See: https://en.wikipedia.org/wiki/Dekker%27s_algorithm

        The core of this algorithm is to use the sequential consistency over flags and turn.
        "Flags" allows thread to proceed when other thread is known not to contend.
        "Turn" allows threads to coordinate their turns when they are contending.

        On x86_64, AArch64, PPC64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            1, 1            0    0.00%   Forbidden  Mutex failure
            1, 2  213,556,068   58.13%  Acceptable  Mutex works
            2, 1  153,830,556   41.87%  Acceptable  Mutex works
     */

    private volatile boolean want1, want2;
    private volatile int turn = 1;
    private int v;

    @Actor
    public void actor1(II_Result r) {
        want1 = true;
        while (want2) {
            if (turn != 1) {
                want1 = false;
                while (turn != 1); // wait
                want1 = true;
            }
        }
        { // critical section
            r.r1 = ++v;
        }
        turn = 2;
        want1 = false;
    }

    @Actor
    public void actor2(II_Result r) {
        want2 = true;
        while (want1) {
            if (turn != 2) {
                want2 = false;
                while (turn != 2); // wait
                want2 = true;
            }
        }
        { // critical section
            r.r2 = ++v;
        }
        turn = 1;
        want2 = false;
    }
}
