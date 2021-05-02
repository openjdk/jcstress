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
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@Outcome(id = {"0, 0", "1, 1", "2, 2"}, expect = Expect.ACCEPTABLE, desc = "Nothing to see here")
@Outcome(id = {"2, 1", "1, 2"},         expect = Expect.ACCEPTABLE_INTERESTING, desc = "Hm?")
@Outcome(                               expect = Expect.FORBIDDEN,  desc = "Other cases are illegal")
@State
public class Sevcik_01_Redundant_WAR_Elimination {

    int x;
    int y;

    private final Object m1 = new Object();
    private final Object m2 = new Object();

    @Actor
    public void thread1() {
        synchronized (m1) {
            x = 2;
        }
    }

    @Actor
    public void thread2() {
        synchronized (m2) {
            x = 1;
        }
    }

    @Actor
    public void thread3(II_Result r) {
        synchronized (m1) {
            synchronized (m2) {
                int t = x;
                x = t;
                r.r1 = t;
                r.r2 = x;
            }
        }
    }

}
