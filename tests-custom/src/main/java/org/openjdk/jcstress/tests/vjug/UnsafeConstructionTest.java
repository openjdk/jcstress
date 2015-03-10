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
package org.openjdk.jcstress.tests.vjug;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;

@JCStressTest
@Description("Tests if unsafe publication is unsafe.")
@Outcome(id = "[-1]", expect = Expect.ACCEPTABLE,             desc = "The object is not yet published")
@Outcome(id = "[0]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "The object is published, but all fields are 0.")
@Outcome(id = "[1]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "The object is published, 1 field is visible.")
@Outcome(id = "[2]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "The object is published, 2 fields are visible.")
@Outcome(id = "[3]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "The object is published, 3 fields are visible.")
@Outcome(id = "[4]",  expect = Expect.ACCEPTABLE,             desc = "The object is published, all fields are visible.")
@State
public class UnsafeConstructionTest {

    /*
       Implementation notes:
         * This showcases how compiler can move the publishing store past the field stores.
         * We need to provide constructor with some external value. If we put the constants in the
           constructor, then compiler can store all the fields with a single bulk store.
         * This test is best to be run with either 32-bit VM, or 64-bit VM with -XX:-UseCompressedOops:
           it seems the compressed references mechanics moves the reference store after the field
           stores, even though not required by JMM.
     */

    int x = 1;

    MyObject o;

    @Actor
    public void publish() {
        o = new MyObject(x);
    }

    @Actor
    public void consume(IntResult1 res) {
        MyObject lo = o;
        if (lo != null) {
            res.r1 = lo.x00 + lo.x01 + lo.x02 + lo.x03;
        } else {
            res.r1 = -1;
        }
    }

    static class MyObject {
        int x00, x01, x02, x03;
        public MyObject(int x) {
            x00 = x;
            x01 = x;
            x02 = x;
            x03 = x;
        }
    }

}
