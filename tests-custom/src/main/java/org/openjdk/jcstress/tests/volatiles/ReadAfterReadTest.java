/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

@JCStressTest
@Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Doing both reads early.")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Doing both reads late.")
@Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "Doing first read early, not surprising.")
@Outcome(id = "1, 0", expect = ACCEPTABLE_INTERESTING, desc = "First read seen racy value early, and the second one did not.")
@State
public class ReadAfterReadTest {

    private final Holder h1 = new Holder();
    private final Holder h2 = h1;

    private static class Holder {
        int a;
        int trap;
    }

    @Actor
    public void actor1() {
        h1.a = 1;
    }

    @Actor
    public void actor2(II_Result r) {
        Holder h1 = this.h1;
        Holder h2 = this.h2;

        // Spam null-pointer check folding: try to step on NPEs early.
        // Doing this early frees compiler from moving h1.a and h2.a loads
        // around, because it would not have to maintain exception order anymore.
        h1.trap = 0;
        h2.trap = 0;

        // Spam alias analysis: the code effectively reads the same field twice,
        // but compiler does not know (h1 == h2) (i.e. does not check it, as
        // this is not a profitable opt for real code), so it issues two independent
        // loads.
        r.r1 = h1.a;
        r.r2 = h2.a;
    }

}
