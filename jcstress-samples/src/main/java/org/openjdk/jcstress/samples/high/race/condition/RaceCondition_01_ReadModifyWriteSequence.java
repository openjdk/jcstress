/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.high.race.condition;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

/*
    How to run this test:
        $ java -jar jcstress-samples/target/jcstress.jar -t RaceCondition_01_ReadModifyWriteSequence
 */

/**
 * This sample demonstrates you how a read-modify-write sequence can lead to surprising results.
 */
@JCStressTest
@Outcome(id = {"150, 100, 150"}, expect = ACCEPTABLE, desc = "Actor1 considered actor2's result and wrote his right result")
@Outcome(id = {"250, 150, 150"}, expect = ACCEPTABLE, desc = "Actor2 considered actor1's result and wrote his right result")
@Outcome(id = {"250, 100, 250", "250, 150, 250"}, expect = ACCEPTABLE_INTERESTING, desc = "Actor1 ignored actor2's result and wrote his wrong result")
@Outcome(id = {"250, 100, 100", "150, 100, 100"}, expect = ACCEPTABLE_INTERESTING, desc = "Actor2 ignored actor1's result and wrote his wrong result")
@State
public class RaceCondition_01_ReadModifyWriteSequence {
    private volatile int value = 200;

    @Actor
    public void actor1(III_Result r) {
        int newValue = value;
        newValue += 50;
        value = newValue;

        r.r1 = newValue;
        r.r3 = value;
    }

    @Actor
    public void actor2(III_Result r) {
        int newValue = value;
        newValue -= 100;
        value = newValue;

        r.r2 = newValue;
        r.r3 = value;
    }
}
