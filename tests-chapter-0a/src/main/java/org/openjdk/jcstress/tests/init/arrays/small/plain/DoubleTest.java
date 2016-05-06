/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.tests.init.arrays.small.plain;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;

// -- This file was mechanically generated: Do not edit! -- //

/**
 * Tests if fields initialization is visible via racy publication.
 */
@JCStressTest
@Outcome(id = "[0]",  expect = Expect.ACCEPTABLE, desc = "Default value for the element. Allowed to see this: data race.")
@Outcome(id = "[-1]", expect = Expect.ACCEPTABLE, desc = "Have not seen the array yet.")
@Outcome(id = "[1]",  expect = Expect.ACCEPTABLE, desc = "Seen all elements set.")
@State
public class DoubleTest {

    double[] arr;

    @Actor
    public void actor1() {
        double[] a = new double[4];
        a[0] = a[1] = a[2] = a[3] = 1.39067116124321E-309;
        arr = a;
    }

    @Actor
    public void actor2(IntResult1 r) {
        double[] a = arr;
        if (a == null) {
            r.r1 = -1;
        } else {
            boolean allSet = true;
            for (double v : a) {
                allSet &= (v == 1.39067116124321E-309);
            }
            r.r1 = allSet ? 1 : 0;
        }
    }

}

