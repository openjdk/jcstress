/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.tests.scratch;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;
import sun.misc.Contended;

import java.util.concurrent.atomic.AtomicInteger;

public class LazySetOrderTest {

    @JCStressTest
    @State
    public static class Plain {

        @Contended
        private int x;
        private int g;

        @Actor
        public void act1() {
            x = 1;
            g = 1;
        }

        @Actor
        public void act2(IntResult2 r) {
            r.r1 = g;
            r.r2 = x;
        }
    }

    @JCStressTest
    @State
    public static class Lazy {
        @Contended
        private int x;

        private AtomicInteger g = new AtomicInteger();

        @Actor
        public void act1() {
            x = 1;
            g.lazySet(1);
        }

        @Actor
        public void act2(IntResult2 r) {
            r.r1 = g.get();
            r.r2 = x;
        }
    }

}
