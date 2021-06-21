/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class Mutex_02_PetersonAlgorithm {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Mutex_02_PetersonAlgorithm
     */

    /**
     * Implemented according to https://en.wikipedia.org/wiki/Peterson%27s_algorithm
     */
    @JCStressTest
    @Outcome(id = {"1, 1"}, expect = ACCEPTABLE, desc = "Both actors could enter the critical section")
    @Outcome(id = {"1, 2", "2, 1", "2, 2"}, expect = FORBIDDEN, desc = "At least one actor couldn't enter the critical section")
    @Outcome(id = {"0, 0", "0, 1", "1, 0", "0, 2", "2, 0"}, expect = FORBIDDEN, desc = "At least one actor hang up in the loop")
    @State
    public static class PetersonAlgorithm {
        private final AtomicBoolean flagForActor1 = new AtomicBoolean();
        private final AtomicBoolean flagForActor2 = new AtomicBoolean();
        private final AtomicInteger turn = new AtomicInteger();
        private final AtomicBoolean isInCriticalSection = new AtomicBoolean();

        @Actor
        public void actor1(II_Result r) {
            flagForActor1.set(true);
            turn.set(2);
            while (flagForActor2.get() && turn.get() == 2) ;
            if (isInCriticalSection.compareAndSet(false, true)) {
                r.r1 = 1;
                isInCriticalSection.set(false);
            } else {
                r.r1 = 2;
            }
            flagForActor1.set(false);
        }

        @Actor
        public void actor2(II_Result r) {
            flagForActor2.set(true);
            turn.set(1);
            while (flagForActor1.get() && turn.get() == 1) ;
            if (isInCriticalSection.compareAndSet(false, true)) {
                r.r2 = 1;
                isInCriticalSection.set(false);
            } else {
                r.r2 = 2;
            }
            flagForActor2.set(false);
        }
    }
}
