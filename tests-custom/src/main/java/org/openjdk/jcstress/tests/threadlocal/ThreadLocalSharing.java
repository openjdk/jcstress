/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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
package org.openjdk.jcstress.tests.threadlocal;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

@JCStressTest
@Outcome(id = {"0, 0", "0, 1", "1, 1"}, expect = Expect.ACCEPTABLE,             desc = "All normal and racy results")
@Outcome(id = "1, 0",                   expect = Expect.ACCEPTABLE_INTERESTING, desc = "No memory effects")
@State
public class ThreadLocalSharing {

    private int x;
    private final Wrapper w = new Wrapper();
    private final ThreadLocal<Wrapper> tl = new ThreadLocal<>();

    public static class Wrapper {
        public int y;
    }

    @Actor
    public void actor1() {
        tl.set(w);
        x = 1;
        tl.get().y = 1;
    }

    @Actor
    public void actor2(II_Result r) {
        tl.set(w);
        r.r1 = tl.get().y;
        r.r2 = x;
    }

}