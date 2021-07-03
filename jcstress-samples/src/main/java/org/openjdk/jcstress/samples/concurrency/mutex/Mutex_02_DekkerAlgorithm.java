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
package org.openjdk.jcstress.samples.concurrency.mutex;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

/*
    How to run this test:
        $ java -jar jcstress-samples/target/jcstress.jar -t Mutex_02_DekkerAlgorithm
*/

/**
 * Implemented according to https://en.wikipedia.org/wiki/Dekker%27s_algorithm
 */
@JCStressTest
@Outcome(id = {"1, 2", "2, 1"}, expect = ACCEPTABLE, desc = "Sequential execution.")
@Outcome(id = "1, 1", expect = ACCEPTABLE_INTERESTING, desc = "Both actors came up with the same value: lock failure.")
@State
public class Mutex_02_DekkerAlgorithm {
    private volatile boolean actor1wantsToEnter;
    private volatile boolean actor2wantsToEnter;
    private volatile int turn = 1;
    private int v;

    @Actor
    public void actor1(II_Result r) {
        actor1wantsToEnter = true;
        while (actor2wantsToEnter) {
            if (turn != 1) {
                actor1wantsToEnter = false;
                while (turn != 1); // wait
                actor1wantsToEnter = true;
            }
        }
        { // critical section
            r.r1 = ++v;
        }
        turn = 2;
        actor1wantsToEnter = false;
    }

    @Actor
    public void actor2(II_Result r) {
        actor2wantsToEnter = true;
        while (actor1wantsToEnter) {
            if (turn != 2) {
                actor2wantsToEnter = false;
                while (turn != 2); // wait
                actor2wantsToEnter = true;
            }
        }
        { // critical section
            r.r2 = ++v;
        }
        turn = 1;
        actor2wantsToEnter = false;
    }
}
