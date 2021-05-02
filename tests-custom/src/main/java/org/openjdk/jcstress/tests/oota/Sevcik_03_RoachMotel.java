/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.tests.oota;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@Outcome(id = ".*, 0, 0", expect = Expect.ACCEPTABLE, desc = "Nothing to see here")
@Outcome(id = ".*, 0, 1", expect = Expect.ACCEPTABLE, desc = "Nothing to see here")
@Outcome(id = "1, 1, 1",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "Out of thin air")
@Outcome(                 expect = Expect.ACCEPTABLE_INTERESTING, desc = "Interesting?")
@State
public class Sevcik_03_RoachMotel {

    int x;
    int y;
    int z;

    private final Object m = new Object();

    @Actor
    public void thread1() {
        synchronized (m) {
            x = 2;
        }
    }

    @Actor
    public void thread2() {
        synchronized (m) {
            x = 1;
        }
    }

    @Actor
    public void thread3(III_Result r) {
        int t1 = x;
        synchronized (m) {
            int t2 = z;
            if (t1 == 2) {
                y = 1;
            } else {
                y = t2;
            }
            r.r2 = t2;
        }
        r.r1 = t1;
    }

    @Actor
    public void thread4(III_Result r) {
        int t3 = y;
        z = t3;
        r.r3 = t3;
    }

}
