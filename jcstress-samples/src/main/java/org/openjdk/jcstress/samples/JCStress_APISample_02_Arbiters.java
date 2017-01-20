/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jcstress.samples;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

/*
    Another flavor of the same test as JCStress_APISample_01_Simple is using
    arbiters. Arbiters run after both actors, and therefore can observe the
    final result.

    This allows to directly observe the atomicity failure:

          [OK] org.openjdk.jcstress.samples.JCStress_APISample_02_Arbiters
        (JVM args: [-server])
      Observed state   Occurrences              Expectation  Interpretation
                   1       940,359   ACCEPTABLE_INTERESTING  One update lost: atomicity failure.
                   2   168,950,601               ACCEPTABLE  Actors updated independently.

    How to run this test:
       $ java -jar jcstress-samples/target/jcstress.jar -t JCStress_APISample_02_Arbiters
 */

@JCStressTest

// These are the test outcomes.
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Actors updated independently.")
@State
public class JCStress_APISample_02_Arbiters {

    int v;

    @Actor
    public void actor1() {
        v++;
    }

    @Actor
    public void actor2() {
        v++;
    }

    @Arbiter
    public void arbiter(IntResult1 r) {
        r.r1 = v;
    }

}
