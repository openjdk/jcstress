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

/*
    Some concurrency tests are not following the "run continously" pattern. One
    of interesting test groups is that asserts if the code had terminated after
    a signal.

    Here, we use a single @Actor that busy-waits on a field, and a @Signal that
    sets that field. JCStress would start actor, and then deliver the signal.
    If it exits in reasonable time, it will record "TERMINATED" result, otherwise
    record "STALE".

    How to run this test:
       $ java -jar jcstress-samples/target/jcstress.jar -t JCStress_APISample_03_Termination
 */

@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE, desc = "Gracefully finished.")
@Outcome(id = "STALE", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Test hung up.")
@State
public class JCStress_APISample_03_Termination {

    int v;

    @Actor
    public void actor1() {
        while (v == 0) {
            // spin
        }
    }

    @Signal
    public void signal() {
        v = 1;
    }

}
