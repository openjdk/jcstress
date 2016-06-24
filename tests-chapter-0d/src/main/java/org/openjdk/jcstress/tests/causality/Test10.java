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
package org.openjdk.jcstress.tests.causality;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult3;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Description("JSR 133 Causality Test 10")
@Ref("http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html")
@Outcome(id = "1, 1, 0", expect = FORBIDDEN,
         desc = "Forbidden. This is the same as test case 5, except using control " +
                 "dependences rather than data dependences.")
@Outcome(id = ".*",      expect = ACCEPTABLE, desc = "All other are acceptable.")
@State
public class Test10 {

    int x, y, z;

    @Actor
    public void thread1(IntResult3 r) {
        int r1 = x;
        if (r1 == 1) {
            y = 1;
        }
        r.r1 = r1;
    }

    @Actor
    public void thread2(IntResult3 r) {
        int r2 = y;
        if (r2 == 1) {
            x = 1;
        }
        r.r2 = r2;
    }

    @Actor
    public void thread3() {
        z = 1;
    }

    @Actor
    public void thread4(IntResult3 r) {
        int r3 = z;
        if (r3 == 1) {
            x = 1;
        }
        r.r3 = r3;
    }

}
