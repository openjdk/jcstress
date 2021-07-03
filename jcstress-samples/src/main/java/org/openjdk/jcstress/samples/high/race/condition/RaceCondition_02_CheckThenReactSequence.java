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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.openjdk.jcstress.annotations.Expect.*;

/*
    How to run this test:
        $ java -jar jcstress-samples/target/jcstress.jar -t RaceCondition_02_CheckThenReactSequence
 */

/**
 * This sample demonstrates you how a check-then-react sequence can lead to surprising results.
 */
@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Only one actor got true for the flag in its if-clause")
@Outcome(id = {"true, true"}, expect = ACCEPTABLE_INTERESTING, desc = "Both actors got true for the flag in their if-clauses")
@State
public class RaceCondition_02_CheckThenReactSequence {
    private volatile boolean flag = true;

    @Actor
    public void actor1(ZZ_Result r) {
        if (flag) {
            flag = false;
            r.r1 = true;
        } else {
            r.r1 = false;
        }
    }

    @Actor
    public void actor2(ZZ_Result r) {
        if (flag) {
            flag = false;
            r.r2 = true;
        } else {
            r.r2 = false;
        }
    }
}
