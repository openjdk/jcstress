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
package org.openjdk.jcstress.samples.api;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

/*
    In many cases, tests share the outcomes and other metadata. To common them,
    there is a special @JCStressMeta annotation that says to look up the metadata
    at another class.

    How to run this test:
       $ java -jar jcstress-samples/target/jcstress.jar -t API_05_SharedMetadata

        ...

        .......... [OK] org.openjdk.jcstress.samples.api.APISample_05_SharedMetadata.PlainTest

          RESULT      SAMPLES    FREQ       EXPECT  DESCRIPTION
            1, 1    6,549,293    1.4%  Interesting  Both actors came up with the same value: atomicity failure.
            1, 2  414,490,076   90.0%   Acceptable  actor1 incremented, then actor2.
            2, 1   39,540,969    8.6%   Acceptable  actor2 incremented, then actor1.

        .......... [OK] org.openjdk.jcstress.samples.api.APISample_05_SharedMetadata.VolatileTest

          RESULT      SAMPLES    FREQ       EXPECT  DESCRIPTION
            1, 1   15,718,942    6.1%  Interesting  Both actors came up with the same value: atomicity failure.
            1, 2  120,855,601   47.2%   Acceptable  actor1 incremented, then actor2.
            2, 1  119,393,635   46.6%   Acceptable  actor2 incremented, then actor1.
 */

@Outcome(id = "1, 1", expect = ACCEPTABLE_INTERESTING, desc = "Both actors came up with the same value: atomicity failure.")
@Outcome(id = "1, 2", expect = ACCEPTABLE,             desc = "actor1 incremented, then actor2.")
@Outcome(id = "2, 1", expect = ACCEPTABLE,             desc = "actor2 incremented, then actor1.")
public class API_05_SharedMetadata {

    @JCStressTest
    @JCStressMeta(API_05_SharedMetadata.class)
    @State
    public static class PlainTest {
        int v;

        @Actor
        public void actor1(II_Result r) {
            r.r1 = ++v;
        }

        @Actor
        public void actor2(II_Result r) {
            r.r2 = ++v;
        }
    }

    @JCStressTest
    @JCStressMeta(API_05_SharedMetadata.class)
    @State
    public static class VolatileTest {
        volatile int v;

        @Actor
        public void actor1(II_Result r) {
            r.r1 = ++v;
        }

        @Actor
        public void actor2(II_Result r) {
            r.r2 = ++v;
        }
    }

}
