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
@Description("JSR 133 Causality Test 12")
@Ref("http://www.cs.umd.edu/~pugh/java/memoryModel/CausalityTestCases.html")
@Outcome(id = "1, 1, 1", expect = FORBIDDEN,
        desc = "Disallowed. After transformations, this code is same as in test 4.")
@Outcome(id = ".*",      expect = ACCEPTABLE, desc = "All other are acceptable.")
@State
public class Test12 {

    int x, y;

    int[] a = new int[2];

    {
        a[0] = 1;
        a[1] = 2;
    }

    @Actor
    public void thread1(IntResult3 r) {
        int r1 = x;
        a[r1] = 0;
        int r2 = a[0];
        y = r2;
        r.r1 = r1;
        r.r2 = r2;
    }

    @Actor
    public void thread2(IntResult3 r) {
        int r3 = y;
        x = r3;
        r.r3 = r3;
    }

}
