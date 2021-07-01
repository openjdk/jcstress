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
package org.openjdk.jcstress.samples.high.race.condition;

import org.openjdk.jcstress.annotations.*;
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
@Outcome(id = {"150, 100, 150"}, expect = ACCEPTABLE, desc = "2:v=200, 2:newV=100, 2:v=newV, 1:v=100, 1:newV=150, 1:v=newV")
@Outcome(id = {"250, 150, 150"}, expect = ACCEPTABLE, desc = "1:v=200, 1:newV=250, 1:v=newV, 2:v=250, 2:newV=150, 2:v=newV")
@Outcome(id = {"250, 100, 250"}, expect = ACCEPTABLE_INTERESTING, desc = "1:v=200, 1:newV=250, 2:v=200, 2:newV=100, 2:v=newV, 1:v=newV")
@Outcome(id = {"250, 100, 100"}, expect = ACCEPTABLE_INTERESTING, desc = "1:v=200, 1:newV=250, 2:v=200, 2:newV=100, 1:v=newV, 2:v=newV")
@State
public class RaceCondition_01_ReadModifyWriteSequence {
    private volatile int v = 200;

    @Actor
    public void actor1(III_Result r) {
        int newV = v;
        newV += 50;
        v = newV;

        r.r1 = newV;
    }

    @Actor
    public void actor2(III_Result r) {
        int newV = v;
        newV -= 100;
        v = newV;

        r.r2 = newV;
    }

    @Arbiter
    public void arbiter(III_Result r) {
        r.r3 = v;
    }
}
