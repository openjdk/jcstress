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
package org.openjdk.jcstress.tests.singletons;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;

/**
 * Tests the unsafe double-checked locking singleton.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@JCStressTest
@Description("Tests the unsafe publishing case.")
@Outcome(id = "[0]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "Singleton return the null instance. This is counter-intuitive, but there is the race on $instance, and second read in the return can indeed return the null reference.")
@Outcome(id = "[1]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "The reference field in singleton is null. This is the violation of singleton contract, but legal JMM behavior.")
@Outcome(id = "[42]", expect = Expect.ACCEPTABLE, desc = "The singleton is observed in fully-constructed way.")
public class UnsafeDCLSingletonTest {

    @Actor
    public final void actor1(UnsafeSingletonFactory s) {
        s.getInstance();
    }

    @Actor
    public final void actor2(UnsafeSingletonFactory s, IntResult1 r) {
        Singleton singleton = s.getInstance();
        if (singleton == null) {
            r.r1 = 0;
            return;
        }

        if (singleton.x == null) {
            r.r1 = 1;
            return;
        }

        r.r1 = singleton.x;
    }

    @State
    public static class UnsafeSingletonFactory {
        private Singleton instance; // specifically non-volatile

        public Singleton getInstance() {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        instance = new Singleton();
                    }
                }
            }
            return instance;
        }
    }
}
