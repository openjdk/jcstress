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
public class AdvancedJMM_07_WrongReleaseOrder {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_07_WrongReleaseOrder
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        Remember, it is critically important that proper release-acquire chains follows the proper structure:
           A --before--> release --sees--> acquire --before--> B

        Only this way we can guarantee that B sees A. This test is one of the exploratory tests what bad
        things happen when that rule is violated. This example differs from BasicJMM_05_Coherence by doing
        the release in wrong order.

        This test yields:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
            0, 0  2,285,705,011   53.75%   Acceptable  Boring
            0, 1      3,023,487    0.07%   Acceptable  Plausible
            1, 0     17,424,594    0.41%  Interesting  Whoa
            1, 1  1,946,573,692   45.77%   Acceptable  Boring

        The "1, 0" outcome is now eminently possible and can be explained by a simple sequential execution.
     */

    int x;
    volatile int g;

    @Actor
    public void actor1() {
        g = 1;  // premature release
        x = 1;
    }

    @Actor
    public void actor2(II_Result r) {
        r.r1 = g;
        r.r2 = x;
    }
}