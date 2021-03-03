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
package org.openjdk.jcstress.tests.volatiles;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IIII_Result;

@JCStressTest
@Outcome(id = "0, 1, 1, 2", expect = Expect.FORBIDDEN,  desc = "Sequential consistency violation.")
@Outcome(                   expect = Expect.ACCEPTABLE, desc = "Accept everything else.")
@Ref("https://bugs.openjdk.java.net/browse/JDK-8262877")
@State
public class PowerSCViolation {

    // This is a draft, very narrow test to demonstrate JDK-8262877.
    // The regular jcstress tests do not catch the issue somehow, probably
    // due to inefficiencies in generated code that access IIII_Result.

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public volatile int x;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public volatile int y;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public int r1;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public int r2;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public int r3;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public int r4;

    @Actor
    public void actor1() {
        x = 2;
        r1 = y;
    }

    @Actor
    public void actor2() {
        y = 1;
    }

    @Actor
    public void actor3() {
        x = 1;
        r2 = y;
    }

    @Actor
    public void actor4() {
        r3 = x;
        r4 = x;
    }

    @Arbiter
    public void dump(IIII_Result r) {
        r.r1 = r1;
        r.r2 = r2;
        r.r3 = r3;
        r.r4 = r4;
    }

}
