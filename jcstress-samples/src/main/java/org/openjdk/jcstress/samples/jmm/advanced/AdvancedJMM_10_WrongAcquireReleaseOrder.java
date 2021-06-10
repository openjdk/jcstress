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
package org.openjdk.jcstress.samples.jmm.advanced;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@State
@Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Boring")
@Outcome(id = "0, 1",           expect = ACCEPTABLE,             desc = "Plausible")
@Outcome(id = "1, 0",           expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
public class AdvancedJMM_10_WrongAcquireReleaseOrder {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_10_WrongAcquireReleaseOrder
     */

    /*
       ----------------------------------------------------------------------------------------------------------

        This example effectively inverts the BasicJMM_06_Causality test: it "guards" the volatile "g" with
        plain "x". The interesting outcome (1, 0) cannot be explained by the sequential execution of this code,
        nevertheless it is allowed by JMM, because it is a data race.

        Note that even if we have seen "x = 1", we can still see "g = 1". That is, adding "volatile" to "g"
        does not guarantee the _promptness_ of publishing of "g".

        x86_64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0  2,694,178,631   58.36%   Acceptable  Boring
            0, 1     69,859,185    1.51%   Acceptable  Plausible
            1, 0        930,435    0.02%  Interesting  Whoa
            1, 1  1,851,647,173   40.11%   Acceptable  Boring
     */

    int x;
    volatile int g;

    @Actor
    public void actor1() {
        g = 1;
        x = 1;
    }

    @Actor
    public void actor2(II_Result r) {
        r.r1 = x;
        r.r2 = g;
    }
}